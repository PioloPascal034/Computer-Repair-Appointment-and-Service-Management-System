package crsm;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/** Native Swing application shell. Design PNGs remain visual references only. */
public class CrsmWindow extends JFrame implements CrsmHost {
    private UserSession session;
    private final String role;
    private final RepairRepository repository = new RepairRepository();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, CrsmListing> listings = new LinkedHashMap<>();
    private final JLabel headerUser = new JLabel();
    private final JLabel headerClock = new JLabel();
    private final JTextField headerSearch = new JTextField();
    private final Preferences prefs = Preferences.userNodeForPackage(CrsmApplication.class);
    private CrsmPages.DashboardPage dashboard;
    private CrsmPages.ReportsPage reports;
    private CrsmPages.UserAccountsPage accounts;
    private CrsmPages.ProfilePage profile;
    private CrsmPages.SettingsPage settings;
    private String currentPage = "Dashboard";
    private TechnicianRealtimeClient technicianRealtime;
    private long lastActivity = System.currentTimeMillis();

    public CrsmWindow(UserSession session) {
        super("CRSM | Computer Repair Service Management");
        this.session = session;
        this.role = switch (session.role()) {
            case "ADMIN" -> "Admin";
            case "STAFF" -> "Staff";
            default -> "Customer";
        };
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1500, 950);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);
        registerViews();
        showPage(prefs.get(CrsmApplication.DEFAULT_PAGE_KEY, "Dashboard"));
        if (role.equals("Admin") || role.equals("Staff")) startTechnicianRealtime();
        startClock();
        startIdleWatch();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                if (technicianRealtime != null) technicianRealtime.close();
            }
        });
    }

    @Override public UserSession session() { return session; }
    @Override public void replaceSession(UserSession next) { this.session = next; refreshHeader(); }
    @Override public String roleName() { return role; }
    @Override public RepairRepository repository() { return repository; }
    @Override public int pageSize() { return CrsmApplication.pageSize(); }
    @Override public boolean customerRecordsBlocked() { return "Customer".equals(role); }

    @Override public boolean canMutate(RepairDataModel.Entity entity) {
        if ("Admin".equals(role)) return true;
        if ("Staff".equals(role)) {
            return entity == RepairDataModel.Entity.CUSTOMERS
                    || entity == RepairDataModel.Entity.COMPUTERS
                    || entity == RepairDataModel.Entity.APPOINTMENTS
                    || entity == RepairDataModel.Entity.REPAIR_REQUESTS
                    || entity == RepairDataModel.Entity.PAYMENTS;
        }
        return false;
    }

    @Override public void applySearch(String query) {
        CrsmListing listing = listings.get(currentPage);
        if (listing != null) listing.applySearch(query);
        else if ("User Accounts".equals(currentPage) && accounts != null) accounts.reload();
    }

    @Override public void refreshHeader() {
        headerUser.setText(session.fullName() + "  ·  " + role);
    }

    @Override public void showDatabaseError(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, cause.getMessage(), "Database operation unavailable", JOptionPane.ERROR_MESSAGE);
    }

    @Override public void signOut() { signOut(true); }

    private void signOut(boolean confirm) {
        if (confirm) {
            int answer = JOptionPane.showConfirmDialog(this, "Sign out of your CRSM account?", "Sign out", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                new SupabaseAuthService().signOut(session);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    if (technicianRealtime != null) technicianRealtime.close();
                    dispose();
                    new CrsmApplication().showLogin();
                } catch (Exception ex) { CrsmApplication.showError(CrsmWindow.this, ex); }
            }
        }.execute();
    }

    private void startTechnicianRealtime() {
        try {
            technicianRealtime = new TechnicianRealtimeClient(session, () -> javax.swing.SwingUtilities.invokeLater(() -> {
                CrsmListing panel = listings.get("Technicians");
                if (panel != null && "Technicians".equals(currentPage)) panel.reload();
            }), state -> javax.swing.SwingUtilities.invokeLater(() -> {
                CrsmListing panel = listings.get("Technicians");
                if (panel != null) panel.setRealtimeState(state);
            }));
            technicianRealtime.start();
        } catch (RuntimeException ex) {
            CrsmListing panel = listings.get("Technicians");
            if (panel != null) panel.setRealtimeState("Live updates unavailable; use Refresh.");
        }
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(236, 0));
        side.setBackground(CrsmTheme.NAVY);
        side.setBorder(BorderFactory.createEmptyBorder(18, 12, 12, 12));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setOpaque(false);
        brand.add(CrsmUi.logoMark(46, true));
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        JLabel mark = new JLabel("CRSM");
        mark.setForeground(CrsmTheme.WHITE);
        mark.setFont(CrsmTheme.font(Font.BOLD, 22));
        JLabel sub = new JLabel("COMPUTER REPAIR");
        sub.setForeground(CrsmTheme.WHITE);
        sub.setFont(CrsmTheme.font(Font.BOLD, 9));
        JLabel sub2 = new JLabel("SERVICE MANAGEMENT");
        sub2.setForeground(CrsmTheme.WHITE);
        sub2.setFont(CrsmTheme.font(Font.BOLD, 9));
        names.add(mark);
        names.add(sub);
        names.add(sub2);
        brand.add(names);
        side.add(brand, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(22, 0, 12, 0));
        String group = "";
        for (String item : navigationForRole()) {
            String nextGroup = groupFor(item);
            if (!nextGroup.equals(group)) {
                group = nextGroup;
                if (!group.isBlank()) {
                    JLabel label = new JLabel(group.toUpperCase());
                    label.setForeground(CrsmTheme.LAVENDER);
                    label.setFont(CrsmTheme.font(Font.BOLD, 10));
                    label.setBorder(BorderFactory.createEmptyBorder(16, 12, 7, 4));
                    label.setAlignmentX(LEFT_ALIGNMENT);
                    nav.add(label);
                }
            }
            JButton button = CrsmUi.navItem(item, false);
            button.addActionListener(e -> showPage(item));
            navButtons.put(item, button);
            nav.add(button);
            nav.add(Box.createVerticalStrut(4));
        }
        JScrollPane scroll = new JScrollPane(nav, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        side.add(scroll, BorderLayout.CENTER);
        JButton logout = CrsmUi.navy("Sign out");
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        logout.addActionListener(e -> signOut());
        side.add(logout, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(CrsmTheme.CANVAS);
        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setBackground(CrsmTheme.NAVY);
        top.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        CrsmUi.styleField(headerSearch);
        headerSearch.putClientProperty("JTextField.placeholderText", "Search…");
        headerSearch.addActionListener(e -> applySearch(headerSearch.getText()));
        headerClock.setForeground(CrsmTheme.WHITE);
        headerClock.setFont(CrsmTheme.font(Font.PLAIN, 12));
        headerUser.setForeground(CrsmTheme.WHITE);
        headerUser.setFont(CrsmTheme.font(Font.BOLD, 13));
        refreshHeader();
        JButton bell = CrsmUi.link("Alerts");
        bell.setForeground(CrsmTheme.WHITE);
        bell.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "There is no notifications table in the current schema. Operational lists on Dashboard and each module are the live data source.",
                "Alerts", JOptionPane.INFORMATION_MESSAGE));
        JButton profileButton = CrsmUi.link("Account ▾");
        profileButton.setForeground(CrsmTheme.WHITE);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem myProfile = new JMenuItem("My Profile");
        myProfile.addActionListener(e -> showPage("My Profile"));
        menu.add(myProfile);
        if ("Admin".equals(role)) {
            JMenuItem settingsItem = new JMenuItem("Settings");
            settingsItem.addActionListener(e -> showPage("Settings"));
            menu.add(settingsItem);
        }
        JMenuItem signOutItem = new JMenuItem("Sign out");
        signOutItem.addActionListener(e -> signOut());
        menu.add(signOutItem);
        profileButton.addActionListener(e -> menu.show(profileButton, 0, profileButton.getHeight()));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(headerClock);
        right.add(bell);
        right.add(headerUser);
        right.add(profileButton);
        top.add(headerSearch, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        body.add(content, BorderLayout.CENTER);
        main.add(top, BorderLayout.NORTH);
        main.add(body, BorderLayout.CENTER);
        return main;
    }

    private void registerViews() {
        dashboard = CrsmPages.dashboard(this);
        content.add(dashboard, "Dashboard");
        for (RepairDataModel.Entity entity : RepairDataModel.Entity.values()) {
            if ("Customer".equals(role) && entity != RepairDataModel.Entity.COMPUTERS
                    && entity != RepairDataModel.Entity.APPOINTMENTS
                    && entity != RepairDataModel.Entity.REPAIR_REQUESTS
                    && entity != RepairDataModel.Entity.REPAIR_SERVICES
                    && entity != RepairDataModel.Entity.PAYMENTS) continue;
            if ("Staff".equals(role) && entity == RepairDataModel.Entity.REPAIR_SERVICES) {
                // Staff may view the catalog when assigning a service on a repair request.
            }
            CrsmListing panel = new CrsmListing(this, entity);
            listings.put(entity.label, panel);
            content.add(panel, entity.label);
        }
        if ("Admin".equals(role)) {
            reports = CrsmPages.reports(this);
            content.add(reports, "Reports");
            accounts = CrsmPages.accounts(this);
            content.add(accounts, "User Accounts");
            settings = CrsmPages.settings(this);
            content.add(settings, "Settings");
        }
        profile = CrsmPages.profile(this);
        content.add(profile, "My Profile");
    }

    private List<String> navigationForRole() {
        return switch (role) {
            case "Customer" -> List.of("Dashboard", "Computers", "Appointments", "Repair Requests", "Services", "Payments", "My Profile");
            case "Staff" -> List.of("Dashboard", "Customers", "Computers", "Appointments", "Repair Requests", "Technicians", "Services", "Payments", "My Profile");
            default -> List.of("Dashboard", "Customers", "Computers", "Appointments", "Repair Requests", "Technicians", "Services", "Payments", "Reports", "User Accounts", "Settings", "My Profile");
        };
    }

    private String groupFor(String item) {
        if (item.equals("Dashboard")) return "";
        if (item.equals("Reports")) return "Reports";
        if (List.of("User Accounts", "Settings", "My Profile").contains(item)) return "System";
        return "Customer".equals(role) ? "My Service" : "Management";
    }

    @Override public void showPage(String name) {
        if (!navigationForRole().contains(name)) name = "Dashboard";
        currentPage = name;
        navButtons.forEach((key, button) -> CrsmUi.selectNav(button, key.equals(name)));
        cards.show(content, name);
        CrsmListing listing = listings.get(name);
        if (listing != null) listing.reload();
        if ("Dashboard".equals(name) && dashboard != null) dashboard.reload();
        if ("Reports".equals(name) && reports != null) reports.reload();
        if ("User Accounts".equals(name) && accounts != null) accounts.reload();
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy  HH:mm");
        headerClock.setText(LocalDateTime.now().format(formatter));
        Timer timer = new Timer(15_000, e -> headerClock.setText(LocalDateTime.now().format(formatter)));
        timer.start();
    }

    private void startIdleWatch() {
        int minutes = prefs.getInt(CrsmApplication.AUTO_LOGOUT_KEY, 0);
        if (minutes <= 0) return;
        Toolkit.getDefaultToolkit().addAWTEventListener((AWTEventListener) event -> lastActivity = System.currentTimeMillis(),
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        Timer timer = new Timer(30_000, e -> {
            if (System.currentTimeMillis() - lastActivity > minutes * 60_000L) {
                JOptionPane.showMessageDialog(this, "You were signed out after " + minutes + " minutes of inactivity.");
                signOut(false);
            }
        });
        timer.start();
    }
}
