package crsm;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/** Shared application shell and schema-backed management views. */
public class CrsmWindow extends JFrame {
    private final UserSession session;
    private final String role;
    private final RepairRepository repository = new RepairRepository();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final Map<String, EntityPanel> entityPanels = new LinkedHashMap<>();
    private final Map<String, DashboardPanel> dashboardPanels = new LinkedHashMap<>();
    private TechnicianRealtimeClient technicianRealtime;

    public CrsmWindow(UserSession session) {
        super("CRSM | Computer Repair Service Management");
        this.session = session;
        this.role = switch (session.role()) { case "ADMIN" -> "Admin"; case "STAFF" -> "Staff"; default -> "Customer"; };
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1500, 950);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);
        registerViews();
        showPage("Dashboard");
        if (role.equals("Admin") || role.equals("Staff")) startTechnicianRealtime();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                if (technicianRealtime != null) technicianRealtime.close();
            }
        });
    }

    private void startTechnicianRealtime() {
        try {
            technicianRealtime = new TechnicianRealtimeClient(session, () -> javax.swing.SwingUtilities.invokeLater(() -> {
                if ("Technicians".equals(pageTitle.getText())) {
                    EntityPanel panel = entityPanels.get("Technicians");
                    if (panel != null) panel.reload();
                }
            }), state -> javax.swing.SwingUtilities.invokeLater(() -> {
                EntityPanel panel = entityPanels.get("Technicians");
                if (panel != null) panel.setRealtimeState(state);
            }));
            technicianRealtime.start();
        } catch (RuntimeException ex) {
            EntityPanel panel = entityPanels.get("Technicians");
            if (panel != null) panel.setRealtimeState("Live updates unavailable; use Refresh.");
        }
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(232, 0));
        side.setBackground(CrsmTheme.NAVY);
        side.setBorder(BorderFactory.createEmptyBorder(18, 12, 12, 12));
        JPanel brand = new JPanel(new GridLayout(2, 1, 2, 0)); brand.setOpaque(false);
        JLabel logo = new JLabel("CRSM", SwingConstants.CENTER); logo.setForeground(CrsmTheme.WHITE); logo.setFont(new Font("Raleway", Font.BOLD, 27));
        JLabel brandSub = new JLabel("COMPUTER REPAIR SERVICE", SwingConstants.CENTER); brandSub.setForeground(CrsmTheme.WHITE); brandSub.setFont(new Font("Raleway", Font.BOLD, 10));
        brand.add(logo); brand.add(brandSub); side.add(brand, BorderLayout.NORTH);
        JPanel nav = new JPanel(); nav.setOpaque(false); nav.setLayout(new javax.swing.BoxLayout(nav, javax.swing.BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(24, 0, 12, 0));
        List<String> pages = navigationForRole();
        String group = "";
        for (String item : pages) {
            String nextGroup = groupFor(item);
            if (!nextGroup.equals(group)) {
                group = nextGroup;
                JLabel label = new JLabel(group.toUpperCase()); label.setForeground(new Color(190, 194, 218));
                label.setFont(new Font("Raleway", Font.BOLD, 10)); label.setBorder(BorderFactory.createEmptyBorder(16, 12, 7, 4));
                nav.add(label);
            }
            JButton button = new JButton(iconFor(item) + "   " + item);
            button.setHorizontalAlignment(JButton.LEFT); button.setForeground(CrsmTheme.WHITE); button.setBackground(CrsmTheme.NAVY);
            button.setBorder(BorderFactory.createEmptyBorder(11, 10, 11, 8)); button.setFocusPainted(false); button.setMaximumSize(new Dimension(220, 42));
            button.addActionListener(e -> showPage(item)); nav.add(button);
        }
        side.add(nav, BorderLayout.CENTER);
        JButton logout = new JButton("↪   Sign out"); logout.setForeground(CrsmTheme.WHITE); logout.setBackground(CrsmTheme.INDIGO); logout.setFocusPainted(false);
        logout.addActionListener(e -> {
            int answer = JOptionPane.showConfirmDialog(this, "Sign out of your CRSM account?", "Sign out", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
            logout.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception { new SupabaseAuthService().signOut(session); return null; }
                @Override protected void done() {
                    logout.setEnabled(true);
                    try { get(); dispose(); new CrsmApplication().showLogin(); }
                    catch (Exception ex) { CrsmApplication.showError(CrsmWindow.this, ex); }
                }
            }.execute();
        }); side.add(logout, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout(0, 12)); main.setBackground(CrsmTheme.CANVAS);
        JPanel top = new JPanel(new BorderLayout()); top.setBackground(CrsmTheme.NAVY); top.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        pageTitle.setForeground(CrsmTheme.WHITE); pageTitle.setFont(new Font("Raleway", Font.BOLD, 21));
        JLabel user = new JLabel(session.fullName() + "  |  " + role + "  ▼"); user.setForeground(CrsmTheme.WHITE);
        top.add(pageTitle, BorderLayout.WEST); top.add(user, BorderLayout.EAST);
        JPanel body = new JPanel(new BorderLayout()); body.setOpaque(false); body.setBorder(BorderFactory.createEmptyBorder(4, 18, 18, 18)); body.add(content, BorderLayout.CENTER);
        main.add(top, BorderLayout.NORTH); main.add(body, BorderLayout.CENTER);
        return main;
    }

    private void registerViews() {
        for (RepairDataModel.Entity entity : RepairDataModel.Entity.values()) {
            if (role.equals("Customer") && entity != RepairDataModel.Entity.COMPUTERS && entity != RepairDataModel.Entity.APPOINTMENTS && entity != RepairDataModel.Entity.REPAIR_REQUESTS && entity != RepairDataModel.Entity.REPAIR_SERVICES && entity != RepairDataModel.Entity.PAYMENTS) continue;
            EntityPanel panel = new EntityPanel(entity);
            entityPanels.put(entity.label, panel); content.add(panel, entity.label);
        }
        DashboardPanel dashboard = new DashboardPanel(); dashboardPanels.put("Dashboard", dashboard); content.add(dashboard, "Dashboard");
        content.add(buildReports(), "Reports");
        if (role.equals("Admin")) content.add(new UserAccountsPanel(), "User Accounts");
        content.add(new ProfilePanel(), "My Profile");
        if (role.equals("Admin")) content.add(new SettingsPanel(), "Settings");
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
        if (List.of("Reports").contains(item)) return "Reports";
        if (List.of("User Accounts", "Settings", "My Profile").contains(item)) return "System";
        return role.equals("Customer") ? "My Service" : "Management";
    }

    private String iconFor(String label) {
        return switch (label) {
            case "Dashboard" -> "⌂"; case "Customers" -> "♙"; case "Computers" -> "▣";
            case "Appointments" -> "▦"; case "Repair Requests" -> "⚒"; case "Technicians" -> "♟";
            case "Services" -> "⚙"; case "Payments" -> "₱"; case "Reports" -> "▤";
            case "User Accounts" -> "♧"; case "My Profile" -> "♙"; default -> "⚙";
        };
    }

    private void showPage(String name) {
        if (role.equals("Customer") && List.of("Computers", "Appointments", "Repair Requests", "Payments").contains(name)) {
            cards.show(content, name); pageTitle.setText(name);
            EntityPanel panel = entityPanels.get(name);
            if (panel != null) panel.showCustomerScopeGap();
            else { content.add(infoPanel(name, "Customer records are not connected to an authenticated customer profile. The UCD limits customer access to the signed-in customer's own records. This screen is blocked until account identity and row-level scope are defined."), name); cards.show(content, name); }
            return;
        }
        pageTitle.setText(name); cards.show(content, name);
        EntityPanel entity = entityPanels.get(name); if (entity != null) entity.reload();
        DashboardPanel dashboard = dashboardPanels.get(name); if (dashboard != null) dashboard.reload();
    }

    private JPanel infoPanel(String heading, String detail) {
        JPanel panel = new JPanel(new BorderLayout(0, 14)); panel.setBackground(CrsmTheme.CANVAS);
        JLabel title = new JLabel(heading); title.setFont(new Font("Raleway", Font.BOLD, 26)); title.setForeground(CrsmTheme.NAVY);
        JLabel text = new JLabel("<html><div style='width:650px'>" + detail + "</div></html>"); text.setForeground(CrsmTheme.INDIGO);
        JPanel card = new JPanel(new BorderLayout()); card.setBackground(CrsmTheme.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER), BorderFactory.createEmptyBorder(24, 24, 24, 24))); card.add(text);
        panel.add(title, BorderLayout.NORTH); panel.add(card, BorderLayout.CENTER); return panel;
    }

    private JPanel buildReports() {
        JPanel panel = new JPanel(new BorderLayout(0, 14)); panel.setBackground(CrsmTheme.CANVAS);
        JLabel title = new JLabel("Reports"); title.setFont(new Font("Raleway", Font.BOLD, 28)); title.setForeground(CrsmTheme.NAVY);
        JLabel note = new JLabel("Operational reports from the SQL views defined in docs/Repairs_&_Views.sql."); note.setForeground(CrsmTheme.LAVENDER);
        JPanel heading = new JPanel(new GridLayout(2, 1)); heading.setOpaque(false); heading.add(title); heading.add(note); panel.add(heading, BorderLayout.NORTH);
        String[][] reports = {{"Active Repair Status", "Active_Repair_Status"}, {"Appointment Schedule", "Appointment_Schedule"}, {"Repair History", "Repair_History"}, {"Technician Workload", "Technician_Workload"}, {"Repair Revenue", "Repair_Revenue"}, {"Pending Repairs", "Pending_Repairs"}, {"Most Requested Services", "Most_Requested_Services"}};
        JComboBox<String> choice = new JComboBox<>(); Map<String, String> reportNames = new LinkedHashMap<>();
        for (String[] report : reports) { choice.addItem(report[0]); reportNames.put(report[0], report[1]); }
        DefaultTableModel model = new DefaultTableModel(); JTable resultTable = new JTable(model); resultTable.setRowHeight(30);
        resultTable.getTableHeader().setBackground(CrsmTheme.INDIGO); resultTable.getTableHeader().setForeground(CrsmTheme.WHITE);
        JLabel resultStatus = new JLabel("Choose a report and select Run report.");
        JButton run = new JButton("Run report"); CrsmApplication.stylePrimary(run);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT)); controls.setBackground(CrsmTheme.WHITE); controls.add(new JLabel("Report type")); controls.add(choice); controls.add(run);
        JPanel center = new JPanel(new BorderLayout(0, 10)); center.setOpaque(false); center.add(controls, BorderLayout.NORTH); center.add(new JScrollPane(resultTable), BorderLayout.CENTER); center.add(resultStatus, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);
        run.addActionListener(e -> {
            run.setEnabled(false); resultStatus.setText("Loading report...");
            String view = reportNames.get(choice.getSelectedItem());
            new SwingWorker<List<Map<String, Object>>, Void>() {
                protected List<Map<String, Object>> doInBackground() throws Exception { return repository.findReport(view); }
                protected void done() {
                    run.setEnabled(true);
                    try {
                        List<Map<String, Object>> rows = get(); model.setRowCount(0); model.setColumnCount(0);
                        if (!rows.isEmpty()) {
                            for (String column : rows.get(0).keySet()) model.addColumn(column);
                            for (Map<String, Object> row : rows) model.addRow(row.values().toArray());
                        }
                        resultStatus.setText(rows.size() + " rows");
                    } catch (Exception ex) { resultStatus.setText("Report unavailable. Confirm the SQL views are installed in PostgreSQL."); showDatabaseError(ex); }
                }
            }.execute();
        });
        return panel;
    }

    private final class UserAccountsPanel extends JPanel {
        private final javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel();
        private final JTable users = new JTable(model);
        private final JComboBox<String> targetRole = new JComboBox<>(new String[]{"STAFF", "ADMIN"});
        private final JLabel state = new JLabel("New accounts receive the Customer role by default.");
        UserAccountsPanel() {
            setLayout(new BorderLayout(0, 12)); setBackground(CrsmTheme.CANVAS);
            JLabel heading = new JLabel("User Accounts"); heading.setFont(new Font("Raleway", Font.BOLD, 28)); heading.setForeground(CrsmTheme.NAVY);
            JButton refresh = new JButton("Refresh"); JButton promote = new JButton("Promote selected account"); CrsmApplication.stylePrimary(promote);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT)); actions.setBackground(CrsmTheme.WHITE); actions.add(new JLabel("Promote to")); actions.add(targetRole); actions.add(promote); actions.add(refresh);
            users.setRowHeight(34); users.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); users.getTableHeader().setBackground(CrsmTheme.INDIGO); users.getTableHeader().setForeground(CrsmTheme.WHITE);
            add(heading, BorderLayout.NORTH); add(new JScrollPane(users), BorderLayout.CENTER);
            JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false); bottom.add(actions, BorderLayout.NORTH); bottom.add(state, BorderLayout.SOUTH); add(bottom, BorderLayout.SOUTH);
            refresh.addActionListener(e -> load());
            promote.addActionListener(e -> {
                int row = users.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this, "Select an account first."); return; }
                String id = String.valueOf(users.getValueAt(row, 0));
                if (id.equals(session.userId())) { JOptionPane.showMessageDialog(this, "Choose another account to promote."); return; }
                int answer = JOptionPane.showConfirmDialog(this, "Promote this account to " + targetRole.getSelectedItem() + "?", "Confirm role promotion", JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) return;
                promote.setEnabled(false);
                new SwingWorker<Void,Void>() {
                    protected Void doInBackground() throws Exception { new SupabaseAuthService().promote(session, id, (String)targetRole.getSelectedItem()); return null; }
                    protected void done() { promote.setEnabled(true); try { get(); state.setText("Account promoted."); load(); } catch (Exception ex) { CrsmApplication.showError(UserAccountsPanel.this, ex); } }
                }.execute();
            });
            load();
        }
        private void load() {
            state.setText("Loading accounts...");
            new SwingWorker<List<Map<String,Object>>,Void>() {
                protected List<Map<String,Object>> doInBackground() throws Exception { return new SupabaseAuthService().listProfiles(session); }
                protected void done() {
                    try {
                        List<Map<String,Object>> rows = get(); model.setRowCount(0); model.setColumnCount(0);
                        String[] columns = {"id", "full_name", "username", "email", "role", "created_at"}; for (String col : columns) model.addColumn(col);
                        for (Map<String,Object> row : rows) model.addRow(java.util.Arrays.stream(columns).map(row::get).toArray());
                        state.setText(rows.size() + " accounts. New signups start as CUSTOMER; only an administrator can promote them.");
                    } catch (Exception ex) { state.setText("Unable to load accounts."); CrsmApplication.showError(UserAccountsPanel.this, ex); }
                }
            }.execute();
        }
    }

    private final class ProfilePanel extends JPanel {
        private java.io.File selectedImage;
        private final JLabel preview = new JLabel("No profile photo selected", SwingConstants.CENTER);
        private final JLabel state = new JLabel(" ");
        ProfilePanel() {
            setLayout(new BorderLayout(0, 14)); setBackground(CrsmTheme.CANVAS);
            JLabel heading = new JLabel("My Profile"); heading.setFont(new Font("Raleway", Font.BOLD, 28)); heading.setForeground(CrsmTheme.NAVY);
            JPanel card = new JPanel(new BorderLayout(20, 12)); card.setBackground(CrsmTheme.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER), BorderFactory.createEmptyBorder(22, 24, 22, 24)));
            JPanel details = new JPanel(new GridLayout(0, 1, 0, 10)); details.setOpaque(false);
            details.add(new JLabel("Name: " + session.fullName())); details.add(new JLabel("Username: " + session.username())); details.add(new JLabel("Email: " + session.email())); details.add(new JLabel("Role: " + role));
            preview.setPreferredSize(new Dimension(200, 200)); preview.setBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER));
            JPanel photoColumn = new JPanel(new BorderLayout(0, 10)); photoColumn.setOpaque(false); photoColumn.add(preview, BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT)); buttons.setOpaque(false); JButton choose = new JButton("Choose photo"); JButton upload = new JButton("Upload profile photo"); CrsmApplication.stylePrimary(upload); buttons.add(choose); buttons.add(upload); photoColumn.add(buttons, BorderLayout.SOUTH);
            choose.addActionListener(e -> selectImage(preview));
            upload.addActionListener(e -> {
                if (selectedImage == null) { JOptionPane.showMessageDialog(this, "Choose a PNG or JPEG image first."); return; }
                upload.setEnabled(false); state.setText("Uploading profile photo...");
                new SwingWorker<String,Void>() {
                    protected String doInBackground() throws Exception {
                        String path = new SupabaseStorageService().uploadAvatar(session, selectedImage.toPath());
                        new SupabaseAuthService().updateAvatar(session, path); return path;
                    }
                    protected void done() { upload.setEnabled(true); try { String path = get(); state.setText("Profile photo uploaded."); JOptionPane.showMessageDialog(ProfilePanel.this, "Profile photo saved to private Supabase Storage."); } catch (Exception ex) { state.setText("Upload failed."); CrsmApplication.showError(ProfilePanel.this, ex); } }
                }.execute();
            });
            card.add(details, BorderLayout.CENTER); card.add(photoColumn, BorderLayout.EAST);
            add(heading, BorderLayout.NORTH); add(card, BorderLayout.CENTER); add(state, BorderLayout.SOUTH);
        }
        private void selectImage(JLabel target) {
            java.io.File file = CrsmApplication.chooseImage(this); if (file == null) return;
            try {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
                if (image == null) throw new IllegalArgumentException("The selected image could not be opened.");
                selectedImage = file; target.setText(""); target.setIcon(new javax.swing.ImageIcon(image.getScaledInstance(190, 190, java.awt.Image.SCALE_SMOOTH)));
            } catch (Exception ex) { CrsmApplication.showError(this, ex); }
        }
    }

    private final class SettingsPanel extends JPanel {
        private java.io.File selectedLogo;
        private final JLabel preview = new JLabel("No business logo selected", SwingConstants.CENTER);
        private final JLabel state = new JLabel("Business logo upload is restricted to authenticated administrators.");
        SettingsPanel() {
            setLayout(new BorderLayout(0, 14)); setBackground(CrsmTheme.CANVAS);
            JLabel heading = new JLabel("Settings"); heading.setFont(new Font("Raleway", Font.BOLD, 28)); heading.setForeground(CrsmTheme.NAVY);
            JPanel card = new JPanel(new BorderLayout(16, 12)); card.setBackground(CrsmTheme.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER), BorderFactory.createEmptyBorder(22, 24, 22, 24)));
            JPanel description = new JPanel(new GridLayout(0, 1, 0, 8)); description.setOpaque(false);
            JLabel title = new JLabel("Business logo"); title.setFont(new Font("Raleway", Font.BOLD, 19)); title.setForeground(CrsmTheme.INDIGO);
            description.add(title); description.add(new JLabel("Choose a PNG or JPEG image up to 2 MB. It will be stored in the private brand-assets bucket."));
            JButton choose = new JButton("Choose logo"); JButton upload = new JButton("Upload and save logo"); CrsmApplication.stylePrimary(upload); description.add(choose); description.add(upload);
            preview.setPreferredSize(new Dimension(300, 200)); preview.setBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER));
            choose.addActionListener(e -> selectLogo());
            upload.addActionListener(e -> {
                if (selectedLogo == null) { JOptionPane.showMessageDialog(this, "Choose a PNG or JPEG image first."); return; }
                upload.setEnabled(false); state.setText("Uploading business logo...");
                new SwingWorker<String,Void>() {
                    protected String doInBackground() throws Exception {
                        String path = new SupabaseStorageService().uploadLogo(session, selectedLogo.toPath());
                        new SupabaseAuthService().updateBusinessLogo(session, path); return path;
                    }
                    protected void done() { upload.setEnabled(true); try { get(); state.setText("Business logo uploaded and saved."); } catch (Exception ex) { state.setText("Upload failed."); CrsmApplication.showError(SettingsPanel.this, ex); } }
                }.execute();
            });
            card.add(description, BorderLayout.CENTER); card.add(preview, BorderLayout.EAST);
            add(heading, BorderLayout.NORTH); add(card, BorderLayout.CENTER); add(state, BorderLayout.SOUTH);
        }
        private void selectLogo() {
            java.io.File file = CrsmApplication.chooseImage(this); if (file == null) return;
            try { java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file); if (image == null) throw new IllegalArgumentException("The selected image could not be opened."); selectedLogo = file; preview.setText(""); preview.setIcon(new javax.swing.ImageIcon(image.getScaledInstance(290, 190, java.awt.Image.SCALE_SMOOTH))); }
            catch (Exception ex) { CrsmApplication.showError(this, ex); }
        }
    }

    private final class DashboardPanel extends JPanel {
        private final JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 12));
        private final JLabel connectionState = new JLabel("Connecting to configured database...");
        DashboardPanel() {
            setLayout(new BorderLayout(0, 16)); setBackground(CrsmTheme.CANVAS);
            JPanel hero = new JPanel(new BorderLayout()); hero.setBackground(CrsmTheme.WHITE); hero.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER), BorderFactory.createEmptyBorder(22, 24, 22, 24)));
            JPanel titles = new JPanel(new GridLayout(2, 1)); titles.setOpaque(false);
            JLabel welcome = new JLabel("Welcome to CRSM"); welcome.setFont(new Font("Raleway", Font.BOLD, 27)); welcome.setForeground(CrsmTheme.NAVY);
            JLabel sub = new JLabel("Manage computer repair service information."); sub.setForeground(CrsmTheme.LAVENDER); titles.add(welcome); titles.add(sub);
            hero.add(titles, BorderLayout.CENTER); hero.add(connectionState, BorderLayout.EAST);
            metrics.setBackground(CrsmTheme.CANVAS);
            JPanel center = new JPanel(new BorderLayout(0, 16)); center.setOpaque(false); center.add(metrics, BorderLayout.NORTH);
            center.add(infoPanel("Operational data", "Dashboard totals are loaded from the database. No sample counts are supplied when the database is unavailable."), BorderLayout.CENTER);
            add(hero, BorderLayout.NORTH); add(center, BorderLayout.CENTER);
        }
        void reload() {
            if (role.equals("Customer")) {
                metrics.removeAll();
                connectionState.setText("Customer profile not connected");
                connectionState.setForeground(CrsmTheme.INDIGO);
                metrics.revalidate(); metrics.repaint();
                return;
            }
            new SwingWorker<List<Integer>, Void>() {
                protected List<Integer> doInBackground() throws Exception {
                    List<Integer> counts = new ArrayList<>();
                    for (RepairDataModel.Entity e : List.of(RepairDataModel.Entity.CUSTOMERS, RepairDataModel.Entity.COMPUTERS, RepairDataModel.Entity.REPAIR_REQUESTS, RepairDataModel.Entity.APPOINTMENTS)) counts.add(repository.count(e));
                    return counts;
                }
                protected void done() {
                    try {
                        List<Integer> counts = get(); metrics.removeAll();
                        String[] names = {"Total Customers", "Registered Devices", "Repair Requests", "Appointments"};
                        for (int i = 0; i < counts.size(); i++) metrics.add(metricCard(names[i], String.valueOf(counts.get(i)), i));
                        connectionState.setText("Database connected"); connectionState.setForeground(new Color(35, 132, 93)); metrics.revalidate(); metrics.repaint();
                    } catch (Exception ex) { connectionState.setText("Database unavailable"); connectionState.setForeground(new Color(170, 68, 68)); showDatabaseError(ex); }
                }
            }.execute();
        }
    }

    private JPanel metricCard(String title, String value, int index) {
        JPanel card = new JPanel(new BorderLayout(0, 10)); card.setBackground(CrsmTheme.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER), BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel name = new JLabel(title); name.setForeground(CrsmTheme.INDIGO); JLabel number = new JLabel(value); number.setForeground(index == 2 ? CrsmTheme.LAVENDER : CrsmTheme.NAVY); number.setFont(new Font("Raleway", Font.BOLD, 29));
        card.add(name, BorderLayout.NORTH); card.add(number, BorderLayout.CENTER); return card;
    }

    private final class EntityPanel extends JPanel {
        private final RepairDataModel.Entity entity;
        private final DefaultTableModel tableModel = new DefaultTableModel();
        private final JTable table = new JTable(tableModel);
        private final JTextField search = new JTextField(24);
        private final JLabel status = new JLabel(" ");
        private final JLabel realtimeStatus = new JLabel(" ");
        private final Timer searchTimer;
        private boolean customerScopeGap;

        EntityPanel(RepairDataModel.Entity entity) {
            this.entity = entity;
            setLayout(new BorderLayout(0, 12)); setBackground(CrsmTheme.CANVAS);
            JPanel head = new JPanel(new BorderLayout(0, 8)); head.setOpaque(false);
            JLabel heading = new JLabel(entity.label); heading.setFont(new Font("Raleway", Font.BOLD, 27)); heading.setForeground(CrsmTheme.NAVY);
            JLabel subtitle = new JLabel("View and manage " + entity.label.toLowerCase() + " records."); subtitle.setForeground(CrsmTheme.LAVENDER);
            JPanel titles = new JPanel(new GridLayout(2, 1)); titles.setOpaque(false); titles.add(heading); titles.add(subtitle); head.add(titles, BorderLayout.NORTH);
            JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)); tools.setBackground(CrsmTheme.WHITE); tools.setBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER));
            search.setToolTipText("Search " + entity.label); search.setPreferredSize(new Dimension(360, 36)); tools.add(search);
            JButton refresh = new JButton("Refresh"); tools.add(refresh);
            JButton add = new JButton("＋ Add"); CrsmApplication.stylePrimary(add); tools.add(add);
            JButton edit = new JButton("Edit"); tools.add(edit);
            JButton delete = new JButton("Delete"); tools.add(delete);
            head.add(tools, BorderLayout.SOUTH);
            table.setRowHeight(34); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.getTableHeader().setBackground(CrsmTheme.INDIGO); table.getTableHeader().setForeground(CrsmTheme.WHITE); table.getTableHeader().setReorderingAllowed(false);
            JScrollPane scroll = new JScrollPane(table); scroll.getViewport().setBackground(CrsmTheme.WHITE); scroll.setBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER));
            JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false); bottom.add(status, BorderLayout.WEST);
            if (entity == RepairDataModel.Entity.TECHNICIANS && !role.equals("Customer")) {
                realtimeStatus.setForeground(CrsmTheme.LAVENDER); bottom.add(realtimeStatus, BorderLayout.EAST);
            }
            add(head, BorderLayout.NORTH); add(scroll, BorderLayout.CENTER); add(bottom, BorderLayout.SOUTH);
            searchTimer = new Timer(350, e -> reload()); searchTimer.setRepeats(false);
            search.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { searchTimer.restart(); } });
            refresh.addActionListener(e -> reload()); add.addActionListener(e -> editRow(null));
            edit.addActionListener(e -> { int row = table.getSelectedRow(); if (row >= 0) editRow(row); else JOptionPane.showMessageDialog(this, "Select a row first."); });
            delete.addActionListener(e -> deleteSelected());
        }

        void showCustomerScopeGap() {
            customerScopeGap = true; tableModel.setRowCount(0); tableModel.setColumnCount(0);
            tableModel.addColumn("Customer data access unavailable"); tableModel.addRow(new Object[]{"The current schema has no authenticated customer identity or ownership scope. No customer records were loaded."});
            status.setText("Own-record access is blocked until authentication and row scoping are defined.");
        }

        void setRealtimeState(String message) {
            if (entity == RepairDataModel.Entity.TECHNICIANS) realtimeStatus.setText(message);
        }

        void reload() {
            if (customerScopeGap) { showCustomerScopeGap(); return; }
            setControlsEnabled(false);
            new SwingWorker<List<RepairDataModel.Row>, Void>() {
                protected List<RepairDataModel.Row> doInBackground() throws Exception { return repository.findAll(entity, search.getText()); }
                protected void done() {
                    setControlsEnabled(true);
                    try {
                        List<RepairDataModel.Row> rows = get();
                        tableModel.setRowCount(0); tableModel.setColumnCount(0);
                        tableModel.addColumn(entity.idColumn); for (String f : entity.fields) tableModel.addColumn(f);
                        for (RepairDataModel.Row row : rows) {
                            Object[] values = new Object[entity.fields.size() + 1]; values[0] = row.id();
                            for (int i = 0; i < entity.fields.size(); i++) values[i + 1] = row.values().get(entity.fields.get(i));
                            tableModel.addRow(values);
                        }
                        status.setText(rows.size() + " records");
                    } catch (Exception ex) { status.setText("Could not load records"); showDatabaseError(ex); }
                }
            }.execute();
        }

        private void setControlsEnabled(boolean enabled) {
            search.setEnabled(enabled); for (Component c : ((JPanel)((JPanel)getComponent(0)).getComponent(1)).getComponents()) if (c instanceof JButton b) b.setEnabled(enabled);
        }

        private void editRow(Integer rowIndex) {
            if (customerScopeGap) return;
            Map<String, String> initial = new LinkedHashMap<>(); Integer id = null;
            if (rowIndex != null) {
                id = Integer.valueOf(table.getValueAt(rowIndex, 0).toString());
                for (int i = 0; i < entity.fields.size(); i++) initial.put(entity.fields.get(i), String.valueOf(table.getValueAt(rowIndex, i + 1) == null ? "" : table.getValueAt(rowIndex, i + 1)));
            }
            RecordDialog dialog = new RecordDialog(this, entity, initial, id != null);
            dialog.setVisible(true);
            if (!dialog.isSaved()) return;
            String validation = CrsmWindow.this.validate(entity, dialog.values());
            if (validation != null) { JOptionPane.showMessageDialog(this, validation, "Check the record", JOptionPane.WARNING_MESSAGE); return; }
            Integer recordId = id;
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception { repository.save(entity, recordId, dialog.values()); return null; }
                protected void done() { try { get(); status.setText("Record saved"); reload(); } catch (Exception ex) { showDatabaseError(ex); } }
            }.execute();
        }

        private void deleteSelected() {
            if (customerScopeGap) return;
            int row = table.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
            int id = Integer.parseInt(table.getValueAt(row, 0).toString());
            int answer = JOptionPane.showConfirmDialog(this, "Delete " + entity.label + " record " + id + "?", "Confirm deletion", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception { repository.delete(entity, id); return null; }
                protected void done() { try { get(); reload(); } catch (Exception ex) { showDatabaseError(ex); } }
            }.execute();
        }
    }

    private static final class RecordDialog extends JDialog {
        private final Map<String, Component> fields = new LinkedHashMap<>();
        private boolean saved;
        RecordDialog(Component owner, RepairDataModel.Entity entity, Map<String, String> initial, boolean editing) {
            super(javax.swing.SwingUtilities.getWindowAncestor(owner), "", ModalityType.APPLICATION_MODAL);
            setTitle((editing ? "Edit " : "Add ") + entity.label);
            JPanel form = new JPanel(new GridLayout(0, 2, 10, 9)); form.setBorder(BorderFactory.createEmptyBorder(18, 18, 12, 18)); form.setBackground(CrsmTheme.WHITE);
            for (String field : entity.fields) {
                form.add(new JLabel(labelFor(field)));
                Component input;
                List<String> choices = statusChoices(entity, field);
                if (!choices.isEmpty()) {
                    JComboBox<String> selector = new JComboBox<>(choices.toArray(String[]::new));
                    String selected = initial.get(field); if (selected != null) selector.setSelectedItem(selected);
                    input = selector;
                } else {
                    JTextField text = new JTextField(initial.getOrDefault(field, ""), 25);
                    if (field.endsWith("_date")) text.setToolTipText("Use yyyy-MM-dd");
                    if (field.endsWith("_time")) text.setToolTipText("Use HH:mm or HH:mm:ss");
                    if (field.endsWith("_id")) text.setToolTipText("Enter an existing related record ID");
                    if (field.endsWith("_cost") || field.equals("service_price") || field.equals("amount_paid")) text.setToolTipText("Enter a non-negative amount");
                    input = text;
                }
                fields.put(field, input); form.add(input);
            }
            JButton save = new JButton("Save"); CrsmApplication.stylePrimary(save); JButton cancel = new JButton("Cancel");
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.setBackground(CrsmTheme.WHITE); buttons.add(cancel); buttons.add(save);
            save.addActionListener(e -> { saved = true; setVisible(false); }); cancel.addActionListener(e -> setVisible(false));
            setLayout(new BorderLayout()); add(form, BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH); pack(); setLocationRelativeTo(owner);
        }
        Map<String, String> values() {
            Map<String, String> result = new LinkedHashMap<>();
            fields.forEach((k, v) -> result.put(k, v instanceof JTextField text ? text.getText() : String.valueOf(((JComboBox<?>)v).getSelectedItem())));
            return result;
        }
        boolean isSaved() { return saved; }

        private static List<String> statusChoices(RepairDataModel.Entity entity, String field) {
            if (entity == RepairDataModel.Entity.REPAIR_REQUESTS && field.equals("repair_status")) return List.of("Pending", "Confirmed", "Under Diagnosis", "Under Repair", "Waiting for Parts", "Ready for Pickup", "Completed");
            if (entity == RepairDataModel.Entity.APPOINTMENTS && field.equals("appointment_status")) return List.of("Pending", "Confirmed", "Completed", "Cancelled");
            if (entity == RepairDataModel.Entity.PAYMENTS && field.equals("payment_status")) return List.of("Unpaid", "Partial", "Paid");
            return List.of();
        }

        private static String labelFor(String field) {
            String[] parts = field.split("_"); StringBuilder label = new StringBuilder();
            for (String part : parts) { if (!label.isEmpty()) label.append(' '); label.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)); }
            return label.toString();
        }
    }

    private String validate(RepairDataModel.Entity entity, Map<String, String> values) {
        for (String field : entity.requiredFields) if (values.getOrDefault(field, "").isBlank()) return field + " is required.";
        try {
            for (String field : entity.fields) {
                String value = values.getOrDefault(field, "").trim(); if (value.isEmpty()) continue;
                if (field.endsWith("_id")) Integer.parseInt(value);
                if (field.endsWith("_cost") || field.equals("service_price") || field.equals("amount_paid")) {
                    if (new BigDecimal(value).signum() < 0) return field + " must be zero or greater.";
                }
                if (field.endsWith("_date")) LocalDate.parse(value);
                if (field.endsWith("_time")) LocalTime.parse(value.length() == 5 ? value + ":00" : value);
            }
        } catch (Exception ex) { return "Check number, amount, date (yyyy-MM-dd), or time (HH:mm) fields."; }
        if (entity == RepairDataModel.Entity.REPAIR_REQUESTS && !List.of("Pending", "Confirmed", "Under Diagnosis", "Under Repair", "Waiting for Parts", "Ready for Pickup", "Completed").contains(values.get("repair_status"))) return "Repair status must match the current database constraint.";
        if (entity == RepairDataModel.Entity.APPOINTMENTS && !List.of("Pending", "Confirmed", "Completed", "Cancelled").contains(values.get("appointment_status"))) return "Appointment status must match the current database constraint.";
        if (entity == RepairDataModel.Entity.PAYMENTS && !List.of("Unpaid", "Partial", "Paid").contains(values.get("payment_status"))) return "Payment status must match the current database constraint.";
        return null;
    }

    private void showDatabaseError(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(this, cause.getMessage(), "Database operation unavailable", JOptionPane.ERROR_MESSAGE);
    }
}
