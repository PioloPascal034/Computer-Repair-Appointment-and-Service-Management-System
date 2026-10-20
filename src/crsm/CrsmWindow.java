package crsm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Main role-aware application shell. */
public final class CrsmWindow extends JFrame implements CrsmHost {
    private UserSession session;
    private final String role;
    private final RepairRepository repository = new RepairRepository();
    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, CrsmListing> listings = new LinkedHashMap<>();
    private final JLabel headerUser = new JLabel();
    private final JLabel pageTitle = CrsmUi.heading("Dashboard", 19);
    private final JTextField headerSearch = new JTextField();
    private CrsmPages.DashboardPage dashboard;
    private CrsmPages.ReportsPage reportsPage;
    private CrsmPages.UserAccountsPage accountsPage;
    private CustomerServicesPage customerServices;
    private CustomerRequestPage customerRequest;
    private String currentPage = "Dashboard";
    private boolean signingOut;

    public CrsmWindow(UserSession session) {
        super("CRSM | Computer Repair Service Management"); this.session = session;
        this.role = "ADMIN".equals(session.role()) ? "Admin" : "STAFF".equals(session.role()) ? "Staff" : "Customer";
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setMinimumSize(new Dimension(1080, 680)); setSize(1440, 900); setLocationRelativeTo(null); setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST); add(buildMainArea(), BorderLayout.CENTER); registerViews(); showPage("Dashboard");
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 12)); side.setPreferredSize(new Dimension(252, 0)); side.setBackground(CrsmTheme.SIDEBAR); side.setBorder(BorderFactory.createEmptyBorder(22, 14, 16, 14));
        JPanel top = new JPanel(); top.setOpaque(false); top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4)); brand.setOpaque(false);
        brand.add(CrsmUi.logoMark(42, true)); JLabel logo = new JLabel("CRSM"); logo.setForeground(CrsmTheme.WHITE); logo.setFont(CrsmTheme.font(Font.BOLD, 22)); brand.add(logo); top.add(brand);
        JLabel subtitle = new JLabel("  COMPUTER REPAIR SERVICES"); subtitle.setForeground(new java.awt.Color(177, 191, 216)); subtitle.setFont(CrsmTheme.font(Font.BOLD, 9)); top.add(subtitle); top.add(javax.swing.Box.createVerticalStrut(28));
        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setLayout(new javax.swing.BoxLayout(navigation, javax.swing.BoxLayout.Y_AXIS));
        for (String item : navigationForRole()) {
            JButton button = CrsmUi.navItem(item, item.equals(currentPage));
            button.addActionListener(e -> showPage(item)); navButtons.put(item, button); navigation.add(button); navigation.add(javax.swing.Box.createVerticalStrut(5));
        }
        JScrollPane navScroll = new JScrollPane(navigation);
        navScroll.setBorder(null);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        side.add(top, BorderLayout.NORTH);
        side.add(navScroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 12)); bottom.setOpaque(false);
        JLabel help = CrsmUi.html("Need help with a repair?<br><b>Our team is ready to assist.</b>", new java.awt.Color(205, 215, 231), 11); help.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); help.setOpaque(true); help.setBackground(CrsmTheme.SIDEBAR_HOVER); bottom.add(help, BorderLayout.NORTH);
        JButton logout = CrsmUi.navItem("Sign out", false); logout.addActionListener(e -> signOut()); bottom.add(logout, BorderLayout.SOUTH); side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout(0, 0)); main.setBackground(CrsmTheme.CANVAS);
        JPanel top = new JPanel(new BorderLayout(22, 0)); top.setBackground(CrsmTheme.WHITE); top.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CrsmTheme.BORDER), BorderFactory.createEmptyBorder(14, 26, 14, 26)));
        JPanel searchSlot = new JPanel(new BorderLayout(8, 0)); searchSlot.setOpaque(false); searchSlot.setPreferredSize(new Dimension(420, 42));
        CrsmUi.styleField(headerSearch); headerSearch.putClientProperty("JTextField.placeholderText", "Search services, devices, or repair requests"); headerSearch.addActionListener(e -> applySearch(headerSearch.getText()));
        JButton search = CrsmUi.primary("Search"); search.addActionListener(e -> applySearch(headerSearch.getText())); searchSlot.add(headerSearch, BorderLayout.CENTER); searchSlot.add(search, BorderLayout.EAST);
        JPanel user = new JPanel(new BorderLayout(10, 0)); user.setOpaque(false); user.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 2));
        JPanel avatar = CrsmUi.logoMark(34, false);
        JPanel userText = new JPanel(); userText.setOpaque(false); userText.setLayout(new javax.swing.BoxLayout(userText, javax.swing.BoxLayout.Y_AXIS));
        headerUser.setFont(CrsmTheme.font(Font.BOLD, 13)); headerUser.setForeground(CrsmTheme.NAVY); JLabel roleLabel = CrsmUi.muted(role + " account"); roleLabel.setFont(CrsmTheme.font(Font.PLAIN, 11));
        userText.add(headerUser); userText.add(roleLabel); user.add(avatar, BorderLayout.WEST); user.add(userText, BorderLayout.CENTER);
        JPanel accountActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); accountActions.setOpaque(false);
        JButton notifications = CrsmUi.ghost("Notifications");
        notifications.setEnabled(false);
        notifications.setToolTipText("Notifications are unavailable because no notification feed is configured.");
        JButton accountMenu = CrsmUi.ghost("Account ▾");
        JPopupMenu menu = new JPopupMenu();
        JMenuItem profile = new JMenuItem("My Profile");
        profile.addActionListener(event -> showPage("My Profile"));
        menu.add(profile);
        if ("Admin".equals(role)) {
            JMenuItem settings = new JMenuItem("Settings");
            settings.addActionListener(event -> showPage("Settings"));
            menu.add(settings);
        }
        menu.addSeparator();
        JMenuItem signOut = new JMenuItem("Sign out");
        signOut.addActionListener(event -> signOut());
        menu.add(signOut);
        accountMenu.addActionListener(event -> menu.show(accountMenu, 0, accountMenu.getHeight()));
        accountActions.add(notifications); accountActions.add(accountMenu);
        user.add(accountActions, BorderLayout.EAST);
        top.add(pageTitle, BorderLayout.WEST); top.add(searchSlot, BorderLayout.CENTER); top.add(user, BorderLayout.EAST); refreshHeader();
        JPanel body = new JPanel(new BorderLayout()); body.setOpaque(false); body.setBorder(BorderFactory.createEmptyBorder(22, 26, 26, 26)); body.add(content, BorderLayout.CENTER);
        main.add(top, BorderLayout.NORTH); main.add(body, BorderLayout.CENTER); return main;
    }

    private void registerViews() {
        dashboard = CrsmPages.dashboard(this); content.add(dashboard, "Dashboard");
        if ("Customer".equals(role)) {
            customerRequest = new CustomerRequestPage(session, () -> showPage("Services")); content.add(customerRequest, "New Repair Request"); content.add(new CustomerTrackingPage(session), "My Repairs");
            customerServices = new CustomerServicesPage(session, customerRequest::selectedDeviceType,
                    service -> { customerRequest.selectService(service); showPage("New Repair Request"); });
            content.add(customerServices, "Services");
        } else {
            content.add(new RepairOperationsPage(session), "Repair Workflow"); for (RepairDataModel.Entity entity : RepairDataModel.Entity.values()) { CrsmListing listing = new CrsmListing(this, entity); listings.put(entity.label, listing); content.add(listing, entity.label); }
            if ("Admin".equals(role)) {
                reportsPage = CrsmPages.reports(this);
                accountsPage = CrsmPages.accounts(this);
                content.add(reportsPage, "Reports");
                content.add(accountsPage, "User Accounts");
                content.add(CrsmPages.settings(this), "Settings");
            }
        }
        content.add(CrsmPages.profile(this), "My Profile");
    }

    private List<String> navigationForRole() { if ("Customer".equals(role)) return List.of("Dashboard", "New Repair Request", "My Repairs", "Services", "My Profile"); List<String> values = new java.util.ArrayList<>(List.of("Dashboard", "Repair Workflow", "Customers", "Computers", "Appointments", "Repair Requests", "Technicians", "Services", "Payments", "My Profile")); if ("Admin".equals(role)) values.addAll(List.of("Reports", "User Accounts", "Settings")); return values; }
    @Override public UserSession session() { return session; }
    @Override public void replaceSession(UserSession next) { session = next; refreshHeader(); }
    @Override public String roleName() { return role; }
    @Override public RepairRepository repository() { return repository; }
    @Override public int pageSize() { return 25; }
    @Override public boolean customerRecordsBlocked() { return "Customer".equals(role); }
    @Override public boolean canMutate(RepairDataModel.Entity entity) { return "Admin".equals(role) || ("Staff".equals(role) && entity != RepairDataModel.Entity.REPAIR_SERVICES); }
    @Override public void applySearch(String query) {
        String value = query == null ? "" : query;
        headerSearch.setToolTipText(null);
        CrsmListing listing = listings.get(currentPage);
        if (listing != null) listing.applySearch(value);
        else if (customerRequest != null && "New Repair Request".equals(currentPage)) customerRequest.applySearch(value);
        else if (customerServices != null && "Services".equals(currentPage)) customerServices.applySearch(value);
        else if (reportsPage != null && "Reports".equals(currentPage)) reportsPage.applySearch(value);
        else if (accountsPage != null && "User Accounts".equals(currentPage)) accountsPage.applySearch(value);
        else if ("Dashboard".equals(currentPage) && !value.isBlank() && listings.containsKey("Repair Requests")) {
            showPage("Repair Requests");
            listings.get("Repair Requests").applySearch(value);
        } else {
            headerSearch.setToolTipText("Search is not available on this page. Use the page's supported controls.");
        }
    }
    @Override public void refreshHeader() { headerUser.setText(session.fullName() + "  ·  " + role); }
    @Override public void showDatabaseError(Exception exception) { CrsmApplication.showError(this, exception); }
    @Override public void signOut() {
        if (signingOut) return;
        signingOut = true;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                new SupabaseAuthService().signOut(session);
                return null;
            }
            @Override protected void done() {
                try { get(); }
                catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    CrsmApplication.showError(CrsmWindow.this, cause instanceof Exception ex ? ex : new IllegalStateException(cause));
                }
                signingOut = false;
                dispose();
                new CrsmApplication().showLogin();
            }
        }.execute();
    }
    @Override public void showPage(String name) {
        String page = navigationForRole().contains(name) ? name : "Dashboard";
        currentPage = page;
        pageTitle.setText(page);
        cards.show(content, page);
        navButtons.forEach((label, button) -> CrsmUi.selectNav(button, label.equals(page)));
        CrsmListing listing = listings.get(page);
        if (listing != null) listing.reload();
        if ("Dashboard".equals(page)) dashboard.reload();
    }
}
