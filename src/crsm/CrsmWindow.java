package crsm;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Main role-aware application shell. */
public final class CrsmWindow extends JFrame implements CrsmHost {
    private UserSession session;
    private final String role;
    private final RepairRepository repository = new RepairRepository();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, CrsmListing> listings = new LinkedHashMap<>();
    private final JLabel headerUser = new JLabel();
    private final JTextField headerSearch = new JTextField();
    private CrsmPages.DashboardPage dashboard;
    private CustomerRequestPage customerRequest;
    private String currentPage = "Dashboard";

    public CrsmWindow(UserSession session) {
        super("CRSM | Computer Repair Service Management"); this.session = session;
        this.role = "ADMIN".equals(session.role()) ? "Admin" : "STAFF".equals(session.role()) ? "Staff" : "Customer";
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setMinimumSize(new Dimension(1180, 760)); setSize(1500, 950); setLocationRelativeTo(null); setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST); add(buildMainArea(), BorderLayout.CENTER); registerViews(); showPage("Dashboard");
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout()); side.setPreferredSize(new Dimension(232, 0)); side.setBackground(CrsmTheme.NAVY); side.setBorder(BorderFactory.createEmptyBorder(18, 12, 12, 12));
        JPanel nav = new JPanel(); nav.setOpaque(false); nav.setLayout(new javax.swing.BoxLayout(nav, javax.swing.BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("CRSM"); logo.setForeground(CrsmTheme.WHITE); nav.add(logo);
        for (String item : navigationForRole()) { JButton button = new JButton(item); button.setHorizontalAlignment(JButton.LEFT); button.setMaximumSize(new Dimension(220, 42)); button.setForeground(CrsmTheme.WHITE); button.setBackground(CrsmTheme.NAVY); button.setFocusPainted(false); button.addActionListener(e -> showPage(item)); navButtons.put(item, button); nav.add(button); }
        side.add(nav, BorderLayout.CENTER); JButton logout = new JButton("Sign out"); logout.addActionListener(e -> signOut()); side.add(logout, BorderLayout.SOUTH); return side;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout()); main.setBackground(CrsmTheme.CANVAS); JPanel top = new JPanel(new BorderLayout(12, 0)); top.setBackground(CrsmTheme.NAVY); top.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        CrsmUi.styleField(headerSearch); headerSearch.setPreferredSize(new Dimension(420, 40)); headerSearch.addActionListener(e -> applySearch(headerSearch.getText())); JButton search = CrsmUi.ghost("Search"); search.addActionListener(e -> applySearch(headerSearch.getText()));
        JPanel searchSlot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); searchSlot.setOpaque(false); searchSlot.add(headerSearch); searchSlot.add(search); headerUser.setForeground(CrsmTheme.WHITE); top.add(searchSlot, BorderLayout.CENTER); top.add(headerUser, BorderLayout.EAST); refreshHeader();
        JPanel body = new JPanel(new BorderLayout()); body.setOpaque(false); body.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16)); body.add(content, BorderLayout.CENTER); main.add(top, BorderLayout.NORTH); main.add(body, BorderLayout.CENTER); return main;
    }

    private void registerViews() {
        dashboard = CrsmPages.dashboard(this); content.add(dashboard, "Dashboard");
        if ("Customer".equals(role)) {
            customerRequest = new CustomerRequestPage(session); content.add(customerRequest, "New Repair Request"); content.add(new CustomerTrackingPage(session), "My Repairs");
            content.add(new CustomerServicesPage(session, customerRequest::selectedDeviceType, service -> { customerRequest.selectService(Integer.parseInt(String.valueOf(service.get("service_id"))), String.valueOf(service.get("service_name")), String.valueOf(service.get("description"))); showPage("New Repair Request"); }), "Services");
        } else {
            content.add(new RepairOperationsPage(session), "Repair Workflow"); for (RepairDataModel.Entity entity : RepairDataModel.Entity.values()) { CrsmListing listing = new CrsmListing(this, entity); listings.put(entity.label, listing); content.add(listing, entity.label); }
            if ("Admin".equals(role)) { content.add(CrsmPages.reports(this), "Reports"); content.add(CrsmPages.accounts(this), "User Accounts"); content.add(CrsmPages.settings(this), "Settings"); }
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
    @Override public void applySearch(String query) { CrsmListing listing = listings.get(currentPage); if (listing != null) listing.applySearch(query); else if (customerRequest != null && "New Repair Request".equals(currentPage)) customerRequest.applySearch(query); }
    @Override public void refreshHeader() { headerUser.setText(session.fullName() + "  ·  " + role); }
    @Override public void showDatabaseError(Exception exception) { CrsmApplication.showError(this, exception); }
    @Override public void signOut() { dispose(); new CrsmApplication().showLogin(); }
    @Override public void showPage(String name) { if (!navigationForRole().contains(name)) name = "Dashboard"; currentPage = name; cards.show(content, name); CrsmListing listing = listings.get(name); if (listing != null) listing.reload(); if ("Dashboard".equals(name)) dashboard.reload(); }
}
