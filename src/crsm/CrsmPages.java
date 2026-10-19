package crsm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

final class CrsmPages {
    private CrsmPages() {}

    static DashboardPage dashboard(CrsmHost host) { return new DashboardPage(host); }
    static ReportsPage reports(CrsmHost host) { return new ReportsPage(host); }
    static UserAccountsPage accounts(CrsmHost host) { return new UserAccountsPage(host); }
    static ProfilePage profile(CrsmHost host) { return new ProfilePage(host); }
    static SettingsPage settings(CrsmHost host) { return new SettingsPage(host); }

    static final class DashboardPage extends JPanel {
        private final CrsmHost host;
        private final JPanel metrics = new JPanel(new GridLayout(1, 5, 10, 10));
        private final JPanel charts = new JPanel(new GridLayout(1, 3, 10, 10));
        private final JPanel tables = new JPanel(new GridLayout(1, 2, 10, 10));
        private final JPanel notes = new JPanel(new GridLayout(1, 2, 10, 10));
        private final JLabel state = CrsmUi.muted("Loading operational totals from the database.");

        DashboardPage(CrsmHost host) {
            this.host = host;
            setLayout(new BorderLayout(0, 12));
            setBackground(CrsmTheme.CANVAS);
            setBorder(new javax.swing.border.EmptyBorder(8, 4, 4, 4));
            JPanel hero = CrsmUi.card();
            JPanel titles = new JPanel();
            titles.setOpaque(false);
            titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
            titles.add(CrsmUi.heading("Welcome Back, " + host.roleName() + "!", 26));
            titles.add(Box.createVerticalStrut(4));
            titles.add(CrsmUi.muted("Manage your computer repair service efficiently."));
            titles.add(Box.createVerticalStrut(8));
            titles.add(state);
            hero.add(titles, BorderLayout.CENTER);
            JPanel overview = new JPanel();
            overview.setOpaque(false);
            overview.setLayout(new BoxLayout(overview, BoxLayout.Y_AXIS));
            overview.add(CrsmUi.heading("System Overview", 14));
            overview.add(CrsmUi.muted("Appointments • Repairs • Payments"));
            hero.add(overview, BorderLayout.EAST);
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            metrics.setOpaque(false);
            charts.setOpaque(false);
            tables.setOpaque(false);
            notes.setOpaque(false);
            body.add(metrics);
            body.add(Box.createVerticalStrut(12));
            body.add(charts);
            body.add(Box.createVerticalStrut(12));
            body.add(tables);
            body.add(Box.createVerticalStrut(12));
            body.add(notes);
            JPanel north = new JPanel(new BorderLayout(0, 12));
            north.setOpaque(false);
            north.add(CrsmUi.pageHead("Dashboard", "Operational snapshot from the current schema.", null), BorderLayout.NORTH);
            north.add(hero, BorderLayout.SOUTH);
            add(north, BorderLayout.NORTH);
            add(new JScrollPane(body, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        }

        void reload() {
            if ("Customer".equals(host.roleName())) {
                metrics.removeAll();
                charts.removeAll();
                tables.removeAll();
                notes.removeAll();
                state.setText("Shop-wide dashboard totals are for Staff and Admin. Customer records remain scoped to the signed-in customer once ownership exists.");
                notes.add(CrsmUi.notice("Customer workspace", "Use My Profile for account details. Computers, appointments, repair requests, and payments stay blocked until the schema maps your Auth account to a Customers row."));
                notes.add(CrsmUi.notice("Services", "You can still open Services to browse catalog prices stored in Repair_Services."));
                revalidate();
                repaint();
                return;
            }
            new SwingWorker<Dash, Void>() {
                @Override protected Dash doInBackground() throws Exception {
                    RepairRepository repo = host.repository();
                    Dash dash = new Dash();
                    dash.customers = repo.count(RepairDataModel.Entity.CUSTOMERS);
                    dash.devices = repo.count(RepairDataModel.Entity.COMPUTERS);
                    dash.activeRepairs = repo.countActiveRepairs();
                    dash.todayAppointments = repo.countTodayAppointments();
                    dash.revenue = repo.sumPayments();
                    dash.repairStatus = repo.countGroup(RepairDataModel.Entity.REPAIR_REQUESTS, "repair_status");
                    dash.monthly = repo.monthlyPaymentTotals();
                    dash.services = toNumberMap(repo.findReport("Most_Requested_Services"), "service_name", "times_requested");
                    dash.workload = toNumberMap(repo.findReport("Technician_Workload"), "technician_name", "total_repairs");
                    dash.recentRepairs = repo.recentRepairs(8);
                    dash.today = repo.todayAppointments();
                    dash.recentPayments = repo.recentPayments(6);
                    return dash;
                }
                @Override protected void done() {
                    try {
                        Dash dash = get();
                        state.setText("Database connected.");
                        metrics.removeAll();
                        metrics.add(CrsmUi.metric("Total Customers", String.valueOf(dash.customers), null));
                        metrics.add(CrsmUi.metric("Registered Devices", String.valueOf(dash.devices), null));
                        metrics.add(CrsmUi.metric("Active Repairs", String.valueOf(dash.activeRepairs), "Not completed"));
                        metrics.add(CrsmUi.metric("Today's Appointments", String.valueOf(dash.todayAppointments), LocalDate.now().toString()));
                        metrics.add(CrsmUi.metric("Total Revenue", CrsmUi.money(dash.revenue), "Sum of amount_paid"));
                        charts.removeAll();
                        charts.add(wrap(CrsmCharts.pie("Repair Status Overview", dash.repairStatus)));
                        charts.add(wrap(CrsmCharts.line("Monthly Revenue", dash.monthly)));
                        charts.add(wrap(CrsmCharts.bars("Most Requested Services", dash.services)));
                        tables.removeAll();
                        tables.add(tableCard("Recent Repair Requests", dash.recentRepairs, "View All", "Repair Requests"));
                        tables.add(tableCard("Today's Appointments", dash.today, "View All", "Appointments"));
                        notes.removeAll();
                        notes.add(wrap(CrsmCharts.bars("Technician Workload", dash.workload)));
                        notes.add(tableCard("Recent Payments", dash.recentPayments, "View All", "Payments"));
                        revalidate();
                        repaint();
                    } catch (Exception ex) {
                        state.setText("Database unavailable.");
                        host.showDatabaseError(ex);
                    }
                }
            }.execute();
        }

        private JPanel wrap(Component component) {
            JPanel card = CrsmUi.card();
            card.add(component, BorderLayout.CENTER);
            return card;
        }

        private JPanel tableCard(String title, List<Map<String, Object>> rows, String action, String page) {
            JPanel card = CrsmUi.card();
            JButton viewAll = CrsmUi.link(action);
            viewAll.addActionListener(e -> host.showPage(page));
            card.add(CrsmUi.pageHead(title, rows.size() + " rows from the current database.", viewAll), BorderLayout.NORTH);
            DefaultTableModel model = new DefaultTableModel();
            JTable table = new JTable(model);
            CrsmUi.styleTable(table);
            if (!rows.isEmpty()) {
                for (String column : rows.get(0).keySet()) model.addColumn(CrsmUi.titleCase(column));
                for (Map<String, Object> row : rows) model.addRow(row.values().toArray());
            }
            card.add(CrsmUi.scroll(table), BorderLayout.CENTER);
            return card;
        }

        static Map<String, Number> toNumberMap(List<Map<String, Object>> rows, String labelKey, String valueKey) {
            Map<String, Number> values = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Object label = first(row, labelKey);
                Object value = first(row, valueKey);
                if (label == null || value == null) continue;
                try { values.put(String.valueOf(label), new BigDecimal(String.valueOf(value))); }
                catch (RuntimeException ignored) { }
            }
            return values;
        }

        private static Object first(Map<String, Object> row, String key) {
            if (row.containsKey(key)) return row.get(key);
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) return entry.getValue();
            }
            return null;
        }

        private static final class Dash {
            int customers, devices, activeRepairs, todayAppointments;
            BigDecimal revenue = BigDecimal.ZERO;
            Map<String, Integer> repairStatus = Map.of();
            Map<String, BigDecimal> monthly = Map.of();
            Map<String, Number> services = Map.of();
            Map<String, Number> workload = Map.of();
            List<Map<String, Object>> recentRepairs = List.of();
            List<Map<String, Object>> today = List.of();
            List<Map<String, Object>> recentPayments = List.of();
        }
    }

    static final class ReportsPage extends JPanel {
        private final CrsmHost host;
        private final JPanel metrics = new JPanel(new GridLayout(1, 4, 10, 10));
        private final JPanel charts = new JPanel(new GridLayout(1, 3, 10, 10));
        private final DefaultTableModel model = new DefaultTableModel();
        private final JTable table = new JTable(model);
        private final JComboBox<String> choice = new JComboBox<>();
        private final JLabel state = CrsmUi.muted("Choose a SQL view and generate the report.");
        private final Map<String, String> views = new LinkedHashMap<>();

        ReportsPage(CrsmHost host) {
            this.host = host;
            setLayout(new BorderLayout(0, 12));
            setBackground(CrsmTheme.CANVAS);
            setBorder(new javax.swing.border.EmptyBorder(8, 4, 4, 4));
            views.put("Active Repair Status", "Active_Repair_Status");
            views.put("Appointment Schedule", "Appointment_Schedule");
            views.put("Repair History", "Repair_History");
            views.put("Technician Workload", "Technician_Workload");
            views.put("Repair Revenue", "Repair_Revenue");
            views.put("Pending Repairs", "Pending_Repairs");
            views.put("Most Requested Services", "Most_Requested_Services");
            views.keySet().forEach(choice::addItem);
            JButton run = CrsmUi.navy("Generate Report");
            run.addActionListener(e -> runReport());
            JPanel controls = CrsmUi.card();
            controls.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
            controls.add(new JLabel("Report type"));
            choice.setPreferredSize(new Dimension(240, 34));
            controls.add(choice);
            controls.add(run);
            controls.add(CrsmUi.muted("Date-range filters are not in the current views. Payment method is not a schema column."));
            CrsmUi.styleTable(table);
            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.setOpaque(false);
            metrics.setOpaque(false);
            charts.setOpaque(false);
            center.add(metrics, BorderLayout.NORTH);
            JPanel mid = new JPanel(new BorderLayout(0, 12));
            mid.setOpaque(false);
            mid.add(charts, BorderLayout.NORTH);
            mid.add(CrsmUi.scroll(table), BorderLayout.CENTER);
            mid.add(state, BorderLayout.SOUTH);
            center.add(mid, BorderLayout.CENTER);
            add(CrsmUi.pageHead("Reports", "View and analyze key data from the SQL views in docs/Repairs_&_Views.sql.", controls), BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);
        }

        void reload() {
            new SwingWorker<Dash, Void>() {
                @Override protected Dash doInBackground() throws Exception {
                    RepairRepository repo = host.repository();
                    Dash dash = new Dash();
                    dash.revenue = repo.sumPayments();
                    dash.repairs = repo.count(RepairDataModel.Entity.REPAIR_REQUESTS);
                    dash.customers = repo.count(RepairDataModel.Entity.CUSTOMERS);
                    dash.services = repo.count(RepairDataModel.Entity.REPAIR_SERVICES);
                    dash.monthly = repo.monthlyPaymentTotals();
                    dash.serviceCounts = DashboardPage.toNumberMap(repo.findReport("Most_Requested_Services"), "service_name", "times_requested");
                    dash.workload = DashboardPage.toNumberMap(repo.findReport("Technician_Workload"), "technician_name", "total_repairs");
                    return dash;
                }
                @Override protected void done() {
                    try {
                        Dash dash = get();
                        metrics.removeAll();
                        metrics.add(CrsmUi.metric("Total Revenue", CrsmUi.money(dash.revenue), "Payments.amount_paid"));
                        metrics.add(CrsmUi.metric("Total Repair Requests", String.valueOf(dash.repairs), null));
                        metrics.add(CrsmUi.metric("Total Customers", String.valueOf(dash.customers), null));
                        metrics.add(CrsmUi.metric("Services in Catalog", String.valueOf(dash.services), null));
                        charts.removeAll();
                        charts.add(wrap(CrsmCharts.bars("Monthly Revenue", dash.monthly)));
                        charts.add(wrap(CrsmCharts.pie("Service Request Distribution", dash.serviceCounts)));
                        charts.add(wrap(CrsmCharts.bars("Technician Workload", dash.workload)));
                        revalidate();
                        repaint();
                    } catch (Exception ex) { host.showDatabaseError(ex); }
                }
            }.execute();
        }

        private JPanel wrap(Component component) {
            JPanel card = CrsmUi.card();
            card.add(component, BorderLayout.CENTER);
            return card;
        }

        private void runReport() {
            String view = views.get(String.valueOf(choice.getSelectedItem()));
            state.setText("Loading " + view + "…");
            new SwingWorker<List<Map<String, Object>>, Void>() {
                @Override protected List<Map<String, Object>> doInBackground() throws Exception { return host.repository().findReport(view); }
                @Override protected void done() {
                    try {
                        List<Map<String, Object>> rows = get();
                        model.setRowCount(0);
                        model.setColumnCount(0);
                        if (!rows.isEmpty()) {
                            for (String column : rows.get(0).keySet()) model.addColumn(column);
                            for (Map<String, Object> row : rows) model.addRow(row.values().toArray());
                        }
                        state.setText(rows.size() + " rows from " + view + ".");
                    } catch (Exception ex) {
                        state.setText("Report unavailable. Confirm the SQL views are installed.");
                        host.showDatabaseError(ex);
                    }
                }
            }.execute();
        }

        private static final class Dash {
            BigDecimal revenue = BigDecimal.ZERO;
            int repairs, customers, services;
            Map<String, BigDecimal> monthly = Map.of();
            Map<String, Number> serviceCounts = Map.of();
            Map<String, Number> workload = Map.of();
        }
    }

    static final class UserAccountsPage extends JPanel {
        private final CrsmHost host;
        private final DefaultTableModel model = new DefaultTableModel();
        private final JTable table = new JTable(model);
        private final JTextField search = new JTextField();
        private final JComboBox<String> roleFilter = new JComboBox<>(new String[]{"All roles", "CUSTOMER", "STAFF", "ADMIN"});
        private final JComboBox<String> targetRole = new JComboBox<>(new String[]{"STAFF", "ADMIN"});
        private final JLabel state = CrsmUi.muted("New accounts receive the Customer role by default.");
        private final JPanel metrics = new JPanel(new GridLayout(1, 4, 10, 10));
        private List<Map<String, Object>> rows = List.of();

        UserAccountsPage(CrsmHost host) {
            this.host = host;
            setLayout(new BorderLayout(0, 12));
            setBackground(CrsmTheme.CANVAS);
            setBorder(new javax.swing.border.EmptyBorder(8, 4, 4, 4));
            CrsmUi.styleField(search);
            search.setPreferredSize(new Dimension(280, 40));
            JButton refresh = CrsmUi.ghost("Refresh");
            JButton promote = CrsmUi.primary("Promote selected account");
            JButton add = CrsmUi.navy("+ Add User");
            JButton view = CrsmUi.ghost("View");
            refresh.addActionListener(e -> reload());
            search.addActionListener(e -> render());
            roleFilter.addActionListener(e -> render());
            promote.addActionListener(e -> promote());
            view.addActionListener(e -> viewSelected());
            add.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    "There is no admin create-user RPC. A person signs up as Customer, then an administrator promotes the profile to Staff or Admin. Deactivate, last-login, and Technician-as-a-login-role are not in crsm_profiles.",
                    "Add user", JOptionPane.INFORMATION_MESSAGE));
            JPanel tools = CrsmUi.card();
            tools.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
            tools.add(search);
            tools.add(roleFilter);
            tools.add(new JLabel("Promote to"));
            tools.add(targetRole);
            tools.add(promote);
            tools.add(view);
            tools.add(refresh);
            tools.add(add);
            CrsmUi.styleTable(table);
            metrics.setOpaque(false);
            JPanel north = new JPanel(new BorderLayout(0, 12));
            north.setOpaque(false);
            north.add(CrsmUi.pageHead("User Accounts", "Manage Auth profiles, roles, and administrator promotion.", null), BorderLayout.NORTH);
            north.add(metrics, BorderLayout.CENTER);
            north.add(tools, BorderLayout.SOUTH);
            add(north, BorderLayout.NORTH);
            add(CrsmUi.scroll(table), BorderLayout.CENTER);
            add(state, BorderLayout.SOUTH);
        }

        void reload() {
            state.setText("Loading accounts…");
            new SwingWorker<List<Map<String, Object>>, Void>() {
                @Override protected List<Map<String, Object>> doInBackground() throws Exception {
                    return new SupabaseAuthService().listProfiles(host.session());
                }
                @Override protected void done() {
                    try {
                        rows = get();
                        render();
                    } catch (Exception ex) {
                        state.setText("Unable to load accounts.");
                        CrsmApplication.showError(UserAccountsPage.this, ex);
                    }
                }
            }.execute();
        }

        private void render() {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String role = String.valueOf(roleFilter.getSelectedItem());
            List<Map<String, Object>> visible = rows.stream().filter(row -> {
                String rowRole = String.valueOf(row.get("role"));
                if (!"All roles".equals(role) && !role.equals(rowRole)) return false;
                if (query.isBlank()) return true;
                return String.valueOf(row).toLowerCase().contains(query);
            }).toList();
            model.setRowCount(0);
            model.setColumnCount(0);
            String[] columns = {"id", "full_name", "username", "email", "role", "created_at"};
            for (String column : columns) model.addColumn(CrsmUi.titleCase(column));
            for (Map<String, Object> row : visible) model.addRow(java.util.Arrays.stream(columns).map(row::get).toArray());
            long customers = rows.stream().filter(r -> "CUSTOMER".equals(String.valueOf(r.get("role")))).count();
            long staff = rows.stream().filter(r -> "STAFF".equals(String.valueOf(r.get("role")))).count();
            long admins = rows.stream().filter(r -> "ADMIN".equals(String.valueOf(r.get("role")))).count();
            metrics.removeAll();
            metrics.add(CrsmUi.metric("Total Users", String.valueOf(rows.size()), "crsm_profiles"));
            metrics.add(CrsmUi.metric("Customers", String.valueOf(customers), "Default signup role"));
            metrics.add(CrsmUi.metric("Staff", String.valueOf(staff), null));
            metrics.add(CrsmUi.metric("Administrators", String.valueOf(admins), null));
            metrics.revalidate();
            metrics.repaint();
            state.setText(visible.size() + " accounts shown. Status, last login, and deactivate are not in the profile table.");
        }

        private void viewSelected() {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select an account first."); return; }
            StringBuilder detail = new StringBuilder("<html><div style='width:360px'>");
            for (int i = 0; i < table.getColumnCount(); i++) {
                detail.append(table.getColumnName(i)).append(": ").append(table.getValueAt(row, i)).append("<br>");
            }
            detail.append("</div></html>");
            JOptionPane.showMessageDialog(this, new JLabel(detail.toString()), "Account", JOptionPane.INFORMATION_MESSAGE);
        }

        private void promote() {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select an account first."); return; }
            String id = String.valueOf(table.getValueAt(row, 0));
            if (id.equals(host.session().userId())) {
                JOptionPane.showMessageDialog(this, "Choose another account to promote.");
                return;
            }
            String role = String.valueOf(targetRole.getSelectedItem());
            int answer = JOptionPane.showConfirmDialog(this, "Promote this account to " + role + "?", "Confirm role promotion", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    new SupabaseAuthService().promote(host.session(), id, role);
                    return null;
                }
                @Override protected void done() {
                    try { get(); state.setText("Account promoted."); reload(); }
                    catch (Exception ex) { CrsmApplication.showError(UserAccountsPage.this, ex); }
                }
            }.execute();
        }
    }

    static final class ProfilePage extends JPanel {
        private final CrsmHost host;
        private final JTextField fullName = new JTextField();
        private final JTextField username = new JTextField();
        private final JLabel preview = new JLabel("No profile photo", SwingConstants.CENTER);
        private final JLabel state = CrsmUi.muted(" ");
        private java.io.File selectedImage;

        ProfilePage(CrsmHost host) {
            this.host = host;
            setLayout(new BorderLayout(0, 12));
            setBackground(CrsmTheme.CANVAS);
            setBorder(new javax.swing.border.EmptyBorder(8, 4, 4, 4));
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Profile Info", infoTab());
            tabs.addTab("Change Password", passwordTab());
            tabs.addTab("Login Activity", CrsmUi.notice("Login activity is unavailable",
                    "crsm_profiles has created_at but no last-login or session-history table. This tab has no data to show."));
            tabs.addTab("Account", accountTab());
            add(CrsmUi.pageHead("My Profile", "Manage your signed-in account.", null), BorderLayout.NORTH);
            add(tabs, BorderLayout.CENTER);
            add(state, BorderLayout.SOUTH);
            loadPhoto();
        }

        private JPanel infoTab() {
            UserSession session = host.session();
            CrsmUi.styleField(fullName);
            CrsmUi.styleField(username);
            fullName.setText(session.fullName());
            username.setText(session.username());
            JTextField email = new JTextField(session.email());
            CrsmUi.styleField(email);
            email.setEditable(false);
            JTextField role = new JTextField(host.roleName());
            CrsmUi.styleField(role);
            role.setEditable(false);
            JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
            form.setOpaque(false);
            form.add(CrsmUi.labeled("Full Name", fullName));
            form.add(CrsmUi.labeled("Username", username));
            form.add(CrsmUi.labeled("Email Address (Auth login)", email));
            form.add(CrsmUi.labeled("Role", role));
            JButton save = CrsmUi.primary("Save profile");
            save.addActionListener(e -> saveProfile());
            form.add(save);
            preview.setPreferredSize(new Dimension(180, 180));
            preview.setBorder(BorderFactory.createLineBorder(CrsmTheme.LAVENDER));
            JButton choose = CrsmUi.ghost("Choose photo");
            JButton upload = CrsmUi.navy("Upload photo");
            JButton clear = CrsmUi.ghost("Remove photo path");
            choose.addActionListener(e -> choosePhoto());
            upload.addActionListener(e -> uploadPhoto());
            clear.addActionListener(e -> clearPhoto());
            JPanel photo = new JPanel(new BorderLayout(0, 8));
            photo.setOpaque(false);
            photo.add(preview, BorderLayout.CENTER);
            photo.add(CrsmUi.flowLeft(choose, upload, clear), BorderLayout.SOUTH);
            JPanel card = CrsmUi.card();
            card.add(form, BorderLayout.CENTER);
            card.add(photo, BorderLayout.EAST);
            return card;
        }

        private JPanel passwordTab() {
            JPasswordField current = new JPasswordField();
            JPasswordField next = new JPasswordField();
            JPasswordField confirm = new JPasswordField();
            JButton save = CrsmUi.primary("Update password");
            save.addActionListener(e -> {
                String a = new String(current.getPassword()), b = new String(next.getPassword()), c = new String(confirm.getPassword());
                if (a.isBlank() || b.isBlank()) {
                    JOptionPane.showMessageDialog(this, "Enter your current password and a new password.");
                    return;
                }
                if (!b.equals(c)) { JOptionPane.showMessageDialog(this, "The new passwords do not match."); return; }
                if (b.length() < 8) { JOptionPane.showMessageDialog(this, "Use at least 8 characters."); return; }
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception {
                        new SupabaseAuthService().updatePassword(host.session(), a, b);
                        return null;
                    }
                    @Override protected void done() {
                        try { get(); state.setText("Password updated."); JOptionPane.showMessageDialog(ProfilePage.this, "Password updated."); }
                        catch (Exception ex) { CrsmApplication.showError(ProfilePage.this, ex); }
                    }
                }.execute();
            });
            JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
            form.setOpaque(false);
            form.add(CrsmUi.labeled("Current Password", CrsmUi.passwordRow(current)));
            form.add(CrsmUi.labeled("New Password", CrsmUi.passwordRow(next)));
            form.add(CrsmUi.labeled("Confirm Password", CrsmUi.passwordRow(confirm)));
            form.add(save);
            JPanel card = CrsmUi.card();
            card.add(form, BorderLayout.NORTH);
            return card;
        }

        private JPanel accountTab() {
            JPanel card = CrsmUi.card();
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.add(CrsmUi.heading("Signed-in account", 16));
            body.add(CrsmUi.muted("User ID: " + host.session().userId()));
            body.add(Box.createVerticalStrut(8));
            body.add(CrsmUi.muted("Email is the Auth login. Username is stored on the profile only."));
            body.add(Box.createVerticalStrut(12));
            JButton signOut = CrsmUi.navy("Sign out");
            signOut.addActionListener(e -> host.signOut());
            body.add(signOut);
            body.add(Box.createVerticalStrut(12));
            body.add(CrsmUi.muted("Account deletion is not provided by the current RPCs."));
            card.add(body, BorderLayout.NORTH);
            return card;
        }

        private void saveProfile() {
            String name = fullName.getText().trim(), handle = username.getText().trim();
            if (name.isBlank() || handle.isBlank()) {
                JOptionPane.showMessageDialog(this, "Full name and username are required.");
                return;
            }
            new SwingWorker<UserSession, Void>() {
                @Override protected UserSession doInBackground() throws Exception {
                    return new SupabaseAuthService().updateProfile(host.session(), name, handle);
                }
                @Override protected void done() {
                    try {
                        host.replaceSession(get());
                        state.setText("Profile saved.");
                        host.refreshHeader();
                    } catch (Exception ex) { CrsmApplication.showError(ProfilePage.this, ex); }
                }
            }.execute();
        }

        private void choosePhoto() {
            java.io.File file = CrsmApplication.chooseImage(this);
            if (file == null) return;
            try {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
                if (image == null) throw new IllegalArgumentException("The selected image could not be opened.");
                selectedImage = file;
                preview.setText("");
                preview.setIcon(new javax.swing.ImageIcon(image.getScaledInstance(170, 170, java.awt.Image.SCALE_SMOOTH)));
            } catch (Exception ex) { CrsmApplication.showError(this, ex); }
        }

        private void uploadPhoto() {
            if (selectedImage == null) { JOptionPane.showMessageDialog(this, "Choose a PNG or JPEG image first."); return; }
            new SwingWorker<UserSession, Void>() {
                @Override protected UserSession doInBackground() throws Exception {
                    String path = new SupabaseStorageService().uploadAvatar(host.session(), selectedImage.toPath());
                    new SupabaseAuthService().updateAvatar(host.session(), path);
                    return host.session().withAvatarPath(path);
                }
                @Override protected void done() {
                    try {
                        host.replaceSession(get());
                        state.setText("Profile photo uploaded.");
                        host.refreshHeader();
                    } catch (Exception ex) { CrsmApplication.showError(ProfilePage.this, ex); }
                }
            }.execute();
        }

        private void clearPhoto() {
            new SwingWorker<UserSession, Void>() {
                @Override protected UserSession doInBackground() throws Exception {
                    new SupabaseAuthService().clearAvatar(host.session());
                    return host.session().withAvatarPath("");
                }
                @Override protected void done() {
                    try {
                        host.replaceSession(get());
                        preview.setIcon(null);
                        preview.setText("No profile photo");
                        state.setText("Profile photo path cleared.");
                        host.refreshHeader();
                    } catch (Exception ex) { CrsmApplication.showError(ProfilePage.this, ex); }
                }
            }.execute();
        }

        private void loadPhoto() {
            String path = host.session().avatarPath();
            if (path == null || path.isBlank()) return;
            new SwingWorker<byte[], Void>() {
                @Override protected byte[] doInBackground() throws Exception {
                    return new SupabaseStorageService().download(host.session(), "crsm-profile-images", path);
                }
                @Override protected void done() {
                    try {
                        byte[] bytes = get();
                        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                        if (image == null) return;
                        preview.setText("");
                        preview.setIcon(new javax.swing.ImageIcon(image.getScaledInstance(170, 170, java.awt.Image.SCALE_SMOOTH)));
                    } catch (Exception ignored) {
                        state.setText("Profile photo could not be downloaded.");
                    }
                }
            }.execute();
        }
    }

    static final class SettingsPage extends JPanel {
        private final CrsmHost host;
        private final Preferences prefs = Preferences.userNodeForPackage(CrsmApplication.class);
        private final JComboBox<Integer> pageSize = new JComboBox<>(new Integer[]{5, 8, 10, 15, 20});
        private final JComboBox<String> dateFormat = new JComboBox<>(new String[]{"yyyy-MM-dd", "MMM d, yyyy"});
        private final JComboBox<String> timeFormat = new JComboBox<>(new String[]{"HH:mm", "hh:mm a"});
        private final JComboBox<Integer> autoLogout = new JComboBox<>(new Integer[]{0, 15, 30, 60});
        private final JLabel preview = new JLabel("No business logo selected", SwingConstants.CENTER);
        private final JLabel state = CrsmUi.muted("Business logo upload is restricted to authenticated administrators.");
        private java.io.File selectedLogo;

        SettingsPage(CrsmHost host) {
            this.host = host;
            setLayout(new BorderLayout(0, 12));
            setBackground(CrsmTheme.CANVAS);
            setBorder(new javax.swing.border.EmptyBorder(8, 4, 4, 4));
            pageSize.setSelectedItem(host.pageSize());
            dateFormat.setSelectedItem(prefs.get(CrsmApplication.DATE_FORMAT_KEY, "yyyy-MM-dd"));
            timeFormat.setSelectedItem(prefs.get(CrsmApplication.TIME_FORMAT_KEY, "HH:mm"));
            autoLogout.setSelectedItem(prefs.getInt(CrsmApplication.AUTO_LOGOUT_KEY, 0));
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("General", generalTab());
            tabs.addTab("Business Information", logoTab());
            tabs.addTab("Service & Pricing", CrsmUi.notice("Not in the current schema",
                    "Default labor rate, diagnostic fee, and tax are not columns on Repair_Services. Service prices are edited on the Services page."));
            tabs.addTab("Appointment", CrsmUi.notice("Not in the current schema",
                    "Operating hours, working days, and booking limits are not stored. Appointments use appointment_date, appointment_time, purpose, and appointment_status."));
            tabs.addTab("Notifications", CrsmUi.notice("Not in the current schema",
                    "There is no notifications table or delivery service. The header bell reports this same gap."));
            tabs.addTab("Backup & Data", CrsmUi.notice("Not in the current application",
                    "Backup frequency and retention are database-administration tasks, not a CRSM table."));
            tabs.addTab("System", CrsmUi.notice("Authentication",
                    "Sign-in already requires Supabase Auth. Multiple-session policy is controlled by Supabase, not this client."));
            add(CrsmUi.pageHead("Settings", "Manage local display preferences and the administrator business logo.", null), BorderLayout.NORTH);
            add(tabs, BorderLayout.CENTER);
            add(state, BorderLayout.SOUTH);
            loadLogo();
        }

        private JPanel generalTab() {
            JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
            form.setOpaque(false);
            form.add(CrsmUi.labeled("Items per page", pageSize));
            form.add(CrsmUi.labeled("Date format (display)", dateFormat));
            form.add(CrsmUi.labeled("Time format (display)", timeFormat));
            form.add(CrsmUi.labeled("Auto-logout minutes (0 = off)", autoLogout));
            JButton save = CrsmUi.primary("Save Changes");
            save.addActionListener(e -> {
                prefs.putInt(CrsmApplication.PAGE_SIZE_KEY, (Integer) pageSize.getSelectedItem());
                prefs.put(CrsmApplication.DATE_FORMAT_KEY, String.valueOf(dateFormat.getSelectedItem()));
                prefs.put(CrsmApplication.TIME_FORMAT_KEY, String.valueOf(timeFormat.getSelectedItem()));
                prefs.putInt(CrsmApplication.AUTO_LOGOUT_KEY, (Integer) autoLogout.getSelectedItem());
                state.setText("Local display preferences saved on this computer. Listing pages use the new page size after Refresh.");
            });
            JPanel card = CrsmUi.card();
            card.add(form, BorderLayout.CENTER);
            card.add(save, BorderLayout.SOUTH);
            return card;
        }

        private JPanel logoTab() {
            preview.setPreferredSize(new Dimension(280, 160));
            preview.setBorder(BorderFactory.createLineBorder(CrsmTheme.LAVENDER));
            JButton choose = CrsmUi.ghost("Change Logo");
            JButton upload = CrsmUi.primary("Upload and save logo");
            choose.addActionListener(e -> chooseLogo());
            upload.addActionListener(e -> uploadLogo());
            JPanel copy = new JPanel();
            copy.setOpaque(false);
            copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
            copy.add(CrsmUi.heading("Business logo", 16));
            copy.add(Box.createVerticalStrut(6));
            copy.add(CrsmUi.muted("PNG or JPEG, up to 2 MB, stored in the private crsm-brand-assets bucket."));
            copy.add(Box.createVerticalStrut(8));
            copy.add(CrsmUi.flowLeft(choose, upload));
            copy.add(Box.createVerticalStrut(8));
            copy.add(CrsmUi.muted("Shop name, address, and contact fields in the mockup are not columns in the seven business tables. They are not saved as organization facts."));
            JPanel card = CrsmUi.card();
            card.add(copy, BorderLayout.CENTER);
            card.add(preview, BorderLayout.EAST);
            return card;
        }

        private void chooseLogo() {
            java.io.File file = CrsmApplication.chooseImage(this);
            if (file == null) return;
            try {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
                if (image == null) throw new IllegalArgumentException("The selected image could not be opened.");
                selectedLogo = file;
                preview.setText("");
                preview.setIcon(new javax.swing.ImageIcon(image.getScaledInstance(260, 150, java.awt.Image.SCALE_SMOOTH)));
            } catch (Exception ex) { CrsmApplication.showError(this, ex); }
        }

        private void uploadLogo() {
            if (selectedLogo == null) { JOptionPane.showMessageDialog(this, "Choose a PNG or JPEG image first."); return; }
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception {
                    String path = new SupabaseStorageService().uploadLogo(host.session(), selectedLogo.toPath());
                    new SupabaseAuthService().updateBusinessLogo(host.session(), path);
                    return path;
                }
                @Override protected void done() {
                    try { get(); state.setText("Business logo uploaded and saved."); }
                    catch (Exception ex) { CrsmApplication.showError(SettingsPage.this, ex); }
                }
            }.execute();
        }

        private void loadLogo() {
            new SwingWorker<byte[], Void>() {
                @Override protected byte[] doInBackground() throws Exception {
                    Map<String, String> settings = new SupabaseAuthService().loadSettings(host.session());
                    String path = settings.get("business_logo_path");
                    if (path == null || path.isBlank()) return new byte[0];
                    return new SupabaseStorageService().download(host.session(), "crsm-brand-assets", path);
                }
                @Override protected void done() {
                    try {
                        byte[] bytes = get();
                        if (bytes.length == 0) return;
                        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                        if (image == null) return;
                        preview.setText("");
                        preview.setIcon(new javax.swing.ImageIcon(image.getScaledInstance(260, 150, java.awt.Image.SCALE_SMOOTH)));
                    } catch (Exception ignored) {
                        state.setText("Business logo could not be downloaded.");
                    }
                }
            }.execute();
        }
    }
}
