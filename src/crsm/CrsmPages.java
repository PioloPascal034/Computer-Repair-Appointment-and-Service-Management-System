package crsm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Role-specific dashboard, reports, profile, and administration pages. */
final class CrsmPages {
    private static final String[] REPORTS = {
            "Active_Repair_Status", "Appointment_Schedule", "Repair_History",
            "Technician_Workload", "Repair_Revenue", "Pending_Repairs", "Most_Requested_Services"
    };

    private CrsmPages() {}

    static DashboardPage dashboard(CrsmHost host) { return new DashboardPage(host); }
    static ReportsPage reports(CrsmHost host) { return new ReportsPage(host); }
    static UserAccountsPage accounts(CrsmHost host) { return new UserAccountsPage(host); }
    static ProfilePage profile(CrsmHost host) { return new ProfilePage(host); }
    static SettingsPage settings(CrsmHost host) { return new SettingsPage(host); }

    private static class MessagePage extends JPanel {
        MessagePage(String title, String detail) {
            super(new BorderLayout(16, 16));
            setBackground(CrsmTheme.CANVAS);
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            add(CrsmUi.pageHead(title, detail, null), BorderLayout.NORTH);
            add(CrsmUi.notice(title, detail), BorderLayout.CENTER);
        }
    }

    static final class DashboardPage extends JPanel {
        private final CrsmHost host;
        private final JPanel metrics = new JPanel(new GridLayout(1, 5, 12, 12));
        private final JPanel charts = new JPanel(new GridLayout(1, 2, 14, 14));
        private final DefaultTableModel repairRows = tableModel("Request ID", "Status", "Reported issue", "Estimate");
        private final DefaultTableModel appointmentRows = tableModel("Appointment", "Date", "Time", "Status");
        private final JLabel status = CrsmUi.muted("Loading current business data…");

        DashboardPage(CrsmHost host) {
            super(new BorderLayout(16, 16));
            this.host = host;
            setBackground(CrsmTheme.CANVAS);
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            add(CrsmUi.pageHead("Good to see you, " + host.session().fullName(),
                    "Current service activity, based on your configured business database.", null), BorderLayout.NORTH);

            metrics.setOpaque(false);
            charts.setOpaque(false);
            for (String label : List.of("Customers", "Registered devices", "Active repairs", "Today's appointments", "Recorded payments")) {
                metrics.add(CrsmUi.metric(label, "—", "Live database total"));
            }
            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            JPanel hero = CrsmUi.card();
            hero.setBorder(BorderFactory.createCompoundBorder(new CrsmUi.RoundedBorder(CrsmTheme.NAVY, 16), BorderFactory.createEmptyBorder(16, 18, 16, 18)));
            JPanel welcome = new JPanel();
            welcome.setOpaque(false);
            welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
            JLabel title = CrsmUi.heading("Keep every repair moving.", 22);
            title.setForeground(CrsmTheme.WHITE);
            welcome.add(title);
            welcome.add(Box.createVerticalStrut(5));
            welcome.add(CrsmUi.html("Review active requests, update repair estimates, and keep service records organized.", new java.awt.Color(220, 228, 242), 13));
            welcome.add(Box.createVerticalStrut(14));
            JPanel shortcuts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            shortcuts.setOpaque(false);
            JButton workflow = CrsmUi.primary("Open repair workflow");
            workflow.addActionListener(event -> host.showPage("Repair Workflow"));
            JButton requests = CrsmUi.ghost("Repair requests");
            requests.addActionListener(event -> host.showPage("Repair Requests"));
            JButton appointments = CrsmUi.ghost("Appointments");
            appointments.addActionListener(event -> host.showPage("Appointments"));
            shortcuts.add(workflow); shortcuts.add(requests); shortcuts.add(appointments);
            welcome.add(shortcuts);
            hero.add(welcome, BorderLayout.CENTER);

            JTable recentRepairs = styledTable(repairRows);
            JTable recentAppointments = styledTable(appointmentRows);
            JPanel recent = new JPanel(new GridLayout(1, 2, 14, 14));
            recent.setOpaque(false);
            recent.add(sectionWithTable("Latest repair requests", recentRepairs));
            recent.add(sectionWithTable("Latest appointments", recentAppointments));
            content.add(metrics);
            content.add(Box.createVerticalStrut(14));
            content.add(hero);
            content.add(Box.createVerticalStrut(14));
            content.add(charts);
            content.add(Box.createVerticalStrut(14));
            content.add(recent);
            content.add(Box.createVerticalStrut(8));
            content.add(status);
            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(null);
            scroll.getViewport().setOpaque(false);
            scroll.getViewport().setBackground(CrsmTheme.CANVAS);
            add(scroll, BorderLayout.CENTER);
            reload();
        }

        void reload() {
            status.setText("Loading current business data…");
            new SwingWorker<DashboardData, Void>() {
                @Override protected DashboardData doInBackground() throws Exception {
                    RepairRepository repository = host.repository();
                    List<RepairDataModel.Row> requests = repository.findAll(RepairDataModel.Entity.REPAIR_REQUESTS, "");
                    List<RepairDataModel.Row> appointments = repository.findAll(RepairDataModel.Entity.APPOINTMENTS, "");
                    List<RepairDataModel.Row> payments = repository.findAll(RepairDataModel.Entity.PAYMENTS, "");
                    int activeRepairs = (int) requests.stream().filter(row -> !List.of("Completed", "Cancelled").contains(row.value("repair_status"))).count();
                    int today = (int) appointments.stream().filter(row -> LocalDate.now().toString().equals(row.value("appointment_date"))).count();
                    Map<String, Integer> repairsByStatus = countBy(requests, "repair_status");
                    Map<String, Integer> appointmentsByStatus = countBy(appointments, "appointment_status");
                    java.math.BigDecimal received = payments.stream()
                            .map(row -> RepairDataModelValue.money(row.value("amount_paid")))
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    return new DashboardData(repository.count(RepairDataModel.Entity.CUSTOMERS),
                            repository.count(RepairDataModel.Entity.COMPUTERS), activeRepairs, today, received,
                            requests.stream().limit(6).toList(),
                            appointments.stream().filter(row -> LocalDate.now().toString().equals(row.value("appointment_date"))).limit(6).toList(),
                            repairsByStatus, appointmentsByStatus);
                }
                @Override protected void done() {
                    try {
                        DashboardData data = get();
                        String[] values = {Integer.toString(data.customers), Integer.toString(data.devices),
                                Integer.toString(data.activeRepairs), Integer.toString(data.todayAppointments),
                                CrsmUi.money(data.payments)};
                        for (int i = 0; i < metrics.getComponentCount(); i++) {
                            JPanel metric = (JPanel) metrics.getComponent(i);
                            setMetricValue(metric, values[i]);
                        }
                        repairRows.setRowCount(0);
                        for (RepairDataModel.Row row : data.recentRepairs) repairRows.addRow(new Object[]{
                                row.id(), row.value("repair_status"), row.value("reported_problem"), CrsmUi.money(row.values().get("estimated_cost"))});
                        appointmentRows.setRowCount(0);
                        for (RepairDataModel.Row row : data.appointmentsToday) appointmentRows.addRow(new Object[]{
                                row.id(), row.value("appointment_date"), row.value("appointment_time"), row.value("appointment_status")});
                        if (data.appointmentsToday.isEmpty()) appointmentRows.addRow(new Object[]{"—", "No appointments today", "—", "—"});
                        charts.removeAll();
                        charts.add(CrsmCharts.bars("Repair status overview", data.repairsByStatus));
                        charts.add(CrsmCharts.pie("Appointment status", data.appointmentsByStatus));
                        charts.revalidate();
                        charts.repaint();
                        status.setText("Data loaded from the configured repair database.");
                    } catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("Dashboard data could not be loaded.");
                        host.showDatabaseError(cause instanceof Exception ex ? ex : new IllegalStateException(cause));
                    }
                }
            }.execute();
        }

        private static void setMetricValue(JPanel card, String value) {
            setMetricValue((java.awt.Container) card, value);
        }

        private static void setMetricValue(java.awt.Container container, String value) {
            for (Component component : container.getComponents()) {
                if (component instanceof JLabel label && label.getFont().getSize() >= 24) label.setText(value);
                if (component instanceof java.awt.Container nested) setMetricValue(nested, value);
            }
        }

        private static JPanel sectionWithTable(String title, JTable table) {
            JPanel section = CrsmUi.card();
            section.setLayout(new BorderLayout(0, 10));
            section.add(CrsmUi.heading(title, 15), BorderLayout.NORTH);
            section.add(CrsmUi.scroll(table), BorderLayout.CENTER);
            return section;
        }

        private static Map<String, Integer> countBy(List<RepairDataModel.Row> rows, String field) {
            Map<String, Integer> values = new LinkedHashMap<>();
            for (RepairDataModel.Row row : rows) values.merge(row.value(field).isBlank() ? "Unspecified" : row.value(field), 1, Integer::sum);
            return values;
        }
    }

    private record DashboardData(int customers, int devices, int activeRepairs, int todayAppointments,
                                 java.math.BigDecimal payments, List<RepairDataModel.Row> recentRepairs,
                                 List<RepairDataModel.Row> appointmentsToday, Map<String, Integer> repairsByStatus,
                                 Map<String, Integer> appointmentsByStatus) {}

    static final class ReportsPage extends JPanel {
        private final CrsmHost host;
        private final JComboBox<String> report = new JComboBox<>(REPORTS);
        private final JLabel status = CrsmUi.muted("Choose a supported database report and generate it.");
        private final DefaultTableModel model = tableModel();
        private final JTable table = styledTable(model);
        private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        private final JTextField search = new JTextField();
        private List<Map<String, Object>> data = List.of();

        ReportsPage(CrsmHost host) {
            super(new BorderLayout(16, 16));
            this.host = host;
            setBackground(CrsmTheme.CANVAS);
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            table.setRowSorter(sorter);
            CrsmUi.styleField(search);
            search.putClientProperty("JTextField.placeholderText", "Filter current report");
            search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void update() {
                    String query = search.getText().trim();
                    sorter.setRowFilter(query.isBlank() ? null : javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
                }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { update(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { update(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { update(); }
            });
            JButton generate = CrsmUi.primary("Generate report");
            generate.addActionListener(event -> generate(generate));
            JPanel filter = new JPanel(new BorderLayout(12, 0));
            filter.setOpaque(false);
            CrsmUi.styleCombo(report);
            filter.add(search, BorderLayout.WEST);
            filter.add(CrsmUi.labeled("Available report", report), BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            JButton export = CrsmUi.ghost("Export CSV");
            export.setEnabled(false);
            export.setToolTipText("Generate a report before exporting it.");
            export.addActionListener(event -> exportCsv());
            buttons.add(export); buttons.add(generate);
            filter.add(buttons, BorderLayout.EAST);
            JPanel content = new JPanel(new BorderLayout(0, 12));
            content.setOpaque(false);
            content.add(filter, BorderLayout.NORTH);
            content.add(CrsmUi.scroll(table), BorderLayout.CENTER);
            content.add(status, BorderLayout.SOUTH);
            add(CrsmUi.pageHead("Reports", "Generate and export reports backed by the configured database views.", null), BorderLayout.NORTH);
            add(CrsmUi.section("Operational reports", "Only reports exposed by the existing database views are listed.", content), BorderLayout.CENTER);
            export.putClientProperty("export", Boolean.TRUE);
            this.putClientProperty("exportButton", export);
            generate();
        }

        private void generate() { generate(null); }
        void applySearch(String query) { search.setText(query == null ? "" : query); }
        private void generate(JButton button) {
            if (button != null) button.setEnabled(false);
            status.setText("Generating " + report.getSelectedItem() + "…");
            new SwingWorker<List<Map<String, Object>>, Void>() {
                @Override protected List<Map<String, Object>> doInBackground() throws Exception {
                    return host.repository().findReport(String.valueOf(report.getSelectedItem()));
                }
                @Override protected void done() {
                    if (button != null) button.setEnabled(true);
                    try {
                        data = get();
                        renderRows(data);
                        status.setText(data.size() + " report rows loaded.");
                        JButton export = (JButton) ReportsPage.this.getClientProperty("exportButton");
                        export.setEnabled(!data.isEmpty());
                        export.setToolTipText(data.isEmpty() ? "There are no report rows to export." : "Export the current report as CSV.");
                    } catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("The report could not be generated.");
                        host.showDatabaseError(cause instanceof Exception ex ? ex : new IllegalStateException(cause));
                    }
                }
            }.execute();
        }

        private void renderRows(List<Map<String, Object>> rows) {
            model.setRowCount(0);
            model.setColumnCount(0);
            if (rows.isEmpty()) return;
            List<String> columns = new ArrayList<>(rows.get(0).keySet());
            for (String column : columns) model.addColumn(CrsmUi.titleCase(column));
            for (Map<String, Object> row : rows) model.addRow(columns.stream().map(row::get).toArray());
        }

        private void exportCsv() {
            if (data.isEmpty()) return;
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File(String.valueOf(report.getSelectedItem()) + ".csv"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path path = chooser.getSelectedFile().toPath();
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                List<String> columns = new ArrayList<>(data.get(0).keySet());
                writer.write(columns.stream().map(ReportsPage::csv).collect(java.util.stream.Collectors.joining(",")));
                writer.newLine();
                for (Map<String, Object> row : data) {
                    writer.write(columns.stream().map(column -> csv(row.get(column))).collect(java.util.stream.Collectors.joining(",")));
                    writer.newLine();
                }
                status.setText("Report exported to " + path.getFileName() + ".");
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, exception.getMessage(), "Could not export report", JOptionPane.ERROR_MESSAGE);
            }
        }

        private static String csv(Object value) {
            String text = value == null ? "" : String.valueOf(value);
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
    }

    static final class UserAccountsPage extends JPanel {
        private final CrsmHost host;
        private final JLabel status = CrsmUi.muted("Loading account profiles…");
        private final DefaultTableModel model = tableModel("User ID", "Full name", "Username", "Email", "Role", "Active", "Created", "Last login");
        private final JTable table = styledTable(model);
        private final JTextField search = new JTextField();
        private final JButton promote = CrsmUi.ghost("Change role");
        private final JButton active = CrsmUi.ghost("Deactivate account");
        private final JButton activity = CrsmUi.ghost("Login activity");
        private final JButton add = CrsmUi.primary("Add user");
        private List<Map<String, Object>> profiles = List.of();

        UserAccountsPage(CrsmHost host) {
            super(new BorderLayout(16, 16));
            this.host = host;
            setBackground(CrsmTheme.CANVAS);
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            table.setAutoCreateRowSorter(true);
            table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(event -> updateActions());
            CrsmUi.styleField(search);
            search.putClientProperty("JTextField.placeholderText", "Filter by name, username, email, or role");
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void update() {
                    String query = search.getText().trim();
                    sorter.setRowFilter(query.isBlank() ? null : javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
                }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { update(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { update(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { update(); }
            });

            JButton refresh = CrsmUi.ghost("Refresh");
            refresh.addActionListener(event -> reload());
            add.setEnabled(false);
            add.setToolTipText("Admin invitations are unavailable; new Customer accounts use the sign-up flow.");
            add.addActionListener(event -> JOptionPane.showMessageDialog(this,
                    "Admin account invitations are not available in the configured backend. New Customer accounts can register through the sign-up screen.",
                    "User creation unavailable", JOptionPane.INFORMATION_MESSAGE));
            JPanel toolbar = new JPanel(new BorderLayout(10, 0));
            toolbar.setOpaque(false);
            toolbar.add(search, BorderLayout.CENTER);
            JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            topButtons.setOpaque(false);
            topButtons.add(add); topButtons.add(refresh);
            toolbar.add(topButtons, BorderLayout.EAST);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            actions.setOpaque(false);
            actions.add(promote); actions.add(active); actions.add(activity);
            promote.addActionListener(event -> changeRole());
            active.addActionListener(event -> toggleActive());
            activity.addActionListener(event -> showActivity());
            JPanel body = new JPanel(new BorderLayout(0, 12));
            body.setOpaque(false);
            body.add(toolbar, BorderLayout.NORTH);
            body.add(CrsmUi.scroll(table), BorderLayout.CENTER);
            JPanel footer = new JPanel(new BorderLayout(8, 0));
            footer.setOpaque(false);
            footer.add(status, BorderLayout.CENTER);
            footer.add(actions, BorderLayout.EAST);
            body.add(footer, BorderLayout.SOUTH);
            add(CrsmUi.pageHead("User accounts", "Review account status, manage staff roles, and inspect sign-in activity.", null), BorderLayout.NORTH);
            add(CrsmUi.section("Accounts and permissions", "Account creation remains on the Customer sign-up flow; staff invitation is not supported by the configured API.", body), BorderLayout.CENTER);
            updateActions();
            reload();
        }

        private void reload() {
            status.setText("Loading account profiles…");
            new SwingWorker<List<Map<String, Object>>, Void>() {
                @Override protected List<Map<String, Object>> doInBackground() throws Exception {
                    return new SupabaseAuthService().listProfiles(host.session());
                }
                @Override protected void done() {
                    try {
                        profiles = get();
                        model.setRowCount(0);
                        for (Map<String, Object> profile : profiles) model.addRow(new Object[]{
                                profile.get("id"), profile.get("full_name"), profile.get("username"), profile.get("email"),
                                profile.get("role"), Boolean.TRUE.equals(profile.get("is_active")) ? "Active" : "Inactive",
                                profile.get("created_at"), profile.get("last_login_at")});
                        status.setText(profiles.size() + (profiles.size() == 1 ? " account" : " accounts"));
                        updateActions();
                    } catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("Account profiles could not be loaded.");
                        showError(UserAccountsPage.this, cause);
                    }
                }
            }.execute();
        }

        void applySearch(String query) { search.setText(query == null ? "" : query); }

        private Map<String, Object> selectedProfile() {
            int selected = table.getSelectedRow();
            if (selected < 0) return null;
            int index = table.convertRowIndexToModel(selected);
            return index >= 0 && index < profiles.size() ? profiles.get(index) : null;
        }

        private void updateActions() {
            Map<String, Object> profile = selectedProfile();
            boolean selected = profile != null;
            promote.setEnabled(selected && !host.session().userId().equals(String.valueOf(profile.get("id"))));
            active.setEnabled(selected && !host.session().userId().equals(String.valueOf(profile.get("id"))));
            activity.setEnabled(selected);
            if (selected) active.setText(Boolean.TRUE.equals(profile.get("is_active")) ? "Deactivate account" : "Reactivate account");
            else active.setText("Deactivate account");
        }

        private void changeRole() {
            Map<String, Object> profile = selectedProfile();
            if (profile == null) return;
            String current = String.valueOf(profile.get("role"));
            String[] roles = {"STAFF", "ADMIN"};
            String next = (String) JOptionPane.showInputDialog(this, "Choose a supported elevated role for " + profile.get("email") + ".",
                    "Change account role", JOptionPane.QUESTION_MESSAGE, null, roles, current.equals("STAFF") ? "ADMIN" : "STAFF");
            if (next == null || next.equals(current)) return;
            if (JOptionPane.showConfirmDialog(this, "Change this account role to " + next + "?", "Confirm role change", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            runAccountAction(() -> new SupabaseAuthService().promote(host.session(), String.valueOf(profile.get("id")), next), "Account role updated.");
        }

        private void toggleActive() {
            Map<String, Object> profile = selectedProfile();
            if (profile == null || host.session().userId().equals(String.valueOf(profile.get("id")))) return;
            boolean next = !Boolean.TRUE.equals(profile.get("is_active"));
            if (JOptionPane.showConfirmDialog(this, (next ? "Reactivate" : "Deactivate") + " " + profile.get("email") + "?",
                    "Confirm account status", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            runAccountAction(() -> new SupabaseAuthService().setAccountActive(host.session(), String.valueOf(profile.get("id")), next),
                    next ? "Account reactivated." : "Account deactivated.");
        }

        private void showActivity() {
            Map<String, Object> profile = selectedProfile();
            if (profile == null) return;
            status.setText("Loading login activity…");
            new SwingWorker<List<Map<String, Object>>, Void>() {
                @Override protected List<Map<String, Object>> doInBackground() throws Exception {
                    return new SupabaseAuthService().loginActivity(host.session(), String.valueOf(profile.get("id")));
                }
                @Override protected void done() {
                    try {
                        List<Map<String, Object>> events = get();
                        DefaultTableModel eventsModel = tableModel("Occurred at", "Client");
                        for (Map<String, Object> event : events) eventsModel.addRow(new Object[]{event.get("occurred_at"), event.get("client_label")});
                        JTable eventsTable = styledTable(eventsModel);
                        if (events.isEmpty()) status.setText("No login events were recorded for this account.");
                        else status.setText(events.size() + " recent login events loaded.");
                        JOptionPane.showMessageDialog(UserAccountsPage.this, CrsmUi.scroll(eventsTable),
                                "Login activity · " + profile.get("email"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("Login activity could not be loaded.");
                        showError(UserAccountsPage.this, cause);
                    }
                }
            }.execute();
        }

        private void runAccountAction(AccountAction action, String success) {
            setActionsEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception { action.run(); return null; }
                @Override protected void done() {
                    setActionsEnabled(true);
                    try { get(); status.setText(success); reload(); }
                    catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("Account update failed.");
                        showError(UserAccountsPage.this, cause);
                    }
                }
            }.execute();
        }

        private void setActionsEnabled(boolean enabled) {
            table.setEnabled(enabled);
            promote.setEnabled(enabled);
            active.setEnabled(enabled);
            activity.setEnabled(enabled);
        }
    }

    @FunctionalInterface private interface AccountAction { void run() throws Exception; }

    static final class ProfilePage extends JPanel {
        private final CrsmHost host;
        private final JTextField fullName = new JTextField();
        private final JTextField username = new JTextField();
        private final JTextField email = new JTextField();
        private final JLabel status = new JLabel(" ");

        ProfilePage(CrsmHost host) {
            super(new BorderLayout(16, 16));
            this.host = host;
            setBackground(CrsmTheme.CANVAS);
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            add(CrsmUi.pageHead("My profile", "Manage your personal account details and review sign-in information.", null), BorderLayout.NORTH);
            CrsmUi.styleField(fullName); CrsmUi.styleField(username); CrsmUi.styleField(email); email.setEditable(false); email.setBackground(CrsmTheme.CANVAS);
            JPanel identity = new JPanel(new GridLayout(0, 2, 14, 14));
            identity.setOpaque(false);
            identity.add(CrsmUi.labeled("Full name", fullName));
            identity.add(CrsmUi.labeled("Username", username));
            identity.add(CrsmUi.labeled("Email address · managed by sign-in", email));
            JTextField role = new JTextField(host.roleName());
            CrsmUi.styleField(role); role.setEditable(false); role.setBackground(CrsmTheme.CANVAS);
            identity.add(CrsmUi.labeled("Account role", role));
            JPanel account = CrsmUi.section("Personal information", "Update the name and username shown in your CRSM account.", identity);
            JButton save = CrsmUi.primary("Save profile changes");
            JPanel actions = new JPanel(new BorderLayout(8, 8));
            actions.setOpaque(false);
            actions.add(status, BorderLayout.CENTER);
            actions.add(save, BorderLayout.EAST);
            JPanel body = new JPanel(new BorderLayout(0, 14));
            body.setOpaque(false);
            body.add(account, BorderLayout.NORTH);
            body.add(actions, BorderLayout.SOUTH);
            add(body, BorderLayout.CENTER);
            save.addActionListener(event -> save(save));
            refresh();
        }

        private void refresh() {
            UserSession session = host.session();
            fullName.setText(session.fullName());
            username.setText(session.username());
            email.setText(session.email());
        }

        private void save(JButton button) {
            String name = fullName.getText().trim(), handle = username.getText().trim();
            if (name.isBlank() || handle.isBlank()) { status.setText("Name and username are required."); return; }
            button.setEnabled(false);
            status.setText("Saving profile…");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    new SupabaseAuthService().updateProfile(host.session(), name, handle);
                    return null;
                }
                @Override protected void done() {
                    button.setEnabled(true);
                    try { get(); host.replaceSession(host.session().withIdentity(name, handle)); status.setText("Profile saved."); }
                    catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("Could not save profile: " + message(cause));
                    }
                }
            }.execute();
        }
    }

    static final class SettingsPage extends JPanel {
        private final CrsmHost host;
        private final JTextField shopName = new JTextField();
        private final JTextField shopAddress = new JTextField();
        private final JTextField contact = new JTextField();
        private final JTextField businessEmail = new JTextField();
        private final JTextField openTime = new JTextField("09:00");
        private final JTextField closeTime = new JTextField("17:00");
        private final JTextField advanceDays = new JTextField("90");
        private final JTextField dailyLimit = new JTextField("0");
        private final JCheckBox[] workingDays = new JCheckBox[7];
        private final JLabel status = CrsmUi.muted("Loading saved settings…");
        private final JPanel sections = new JPanel();

        SettingsPage(CrsmHost host) {
            super(new BorderLayout(14, 14));
            this.host = host;
            setBackground(CrsmTheme.CANVAS);
            setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
            add(CrsmUi.pageHead("Settings", "Manage supported shop details and appointment availability.", null), BorderLayout.NORTH);

            for (JTextField field : List.of(shopName, shopAddress, contact, businessEmail, openTime, closeTime, advanceDays, dailyLimit)) CrsmUi.styleField(field);
            GridLayout business = new GridLayout(0, 2, 14, 14);
            JPanel businessFields = new JPanel(business);
            businessFields.setOpaque(false);
            businessFields.add(CrsmUi.labeled("Shop name", shopName));
            businessFields.add(CrsmUi.labeled("Business email", businessEmail));
            businessFields.add(CrsmUi.labeled("Address", shopAddress));
            businessFields.add(CrsmUi.labeled("Contact number", contact));
            JButton saveBusiness = CrsmUi.primary("Save business details");
            saveBusiness.addActionListener(event -> saveBusiness(saveBusiness));
            JButton uploadLogo = CrsmUi.ghost("Upload business logo");
            uploadLogo.addActionListener(event -> uploadLogo(uploadLogo));
            JPanel businessPanel = new JPanel(new BorderLayout(0, 12));
            businessPanel.setOpaque(false);
            businessPanel.add(businessFields, BorderLayout.CENTER);
            JPanel businessActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            businessActions.setOpaque(false);
            businessActions.add(uploadLogo);
            businessActions.add(saveBusiness);
            businessPanel.add(businessActions, BorderLayout.SOUTH);
            sections.add(CrsmUi.section("Business information", "These values are saved in the existing authenticated settings store.", businessPanel));

            GridLayout appointment = new GridLayout(0, 2, 14, 14);
            JPanel appointmentFields = new JPanel(appointment);
            appointmentFields.setOpaque(false);
            appointmentFields.add(CrsmUi.labeled("Opening time (HH:MM)", openTime));
            appointmentFields.add(CrsmUi.labeled("Closing time (HH:MM)", closeTime));
            appointmentFields.add(CrsmUi.labeled("Advance booking limit (days)", advanceDays));
            appointmentFields.add(CrsmUi.labeled("Daily appointment limit (0 = unlimited)", dailyLimit));
            JPanel days = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            days.setOpaque(false);
            String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (int i = 0; i < dayNames.length; i++) {
                workingDays[i] = new JCheckBox(dayNames[i]);
                workingDays[i].setOpaque(false);
                days.add(workingDays[i]);
            }
            appointmentFields.add(CrsmUi.labeled("Working days", days));
            JButton saveAppointments = CrsmUi.primary("Save appointment settings");
            saveAppointments.addActionListener(event -> saveAppointmentSettings(saveAppointments));
            JPanel appointmentPanel = new JPanel(new BorderLayout(0, 12));
            appointmentPanel.setOpaque(false);
            appointmentPanel.add(appointmentFields, BorderLayout.CENTER);
            appointmentPanel.add(saveAppointments, BorderLayout.SOUTH);
            sections.add(CrsmUi.section("Appointment settings", "These settings are used by the existing customer booking workflow.", appointmentPanel));

            JButton services = CrsmUi.ghost("Open service catalog");
            services.addActionListener(event -> host.showPage("Services"));
            sections.add(CrsmUi.section("Service & pricing", "Service prices and device-specific pricing are managed in the existing Services catalog.", services));

            sections.add(unavailable("Notifications", "No notification preferences or delivery service is configured."));
            sections.add(unavailable("Backup & data", "No backup or restore service is configured; backup actions are unavailable."));
            sections.add(unavailable("System security", "Session timeout and multi-login controls are not exposed by the configured authentication backend."));
            sections.setOpaque(false);
            sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
            JScrollPane scroll = new JScrollPane(sections);
            scroll.setBorder(null);
            scroll.getViewport().setOpaque(false);
            scroll.getViewport().setBackground(CrsmTheme.CANVAS);
            JButton refresh = CrsmUi.ghost("Refresh saved settings");
            refresh.addActionListener(event -> reload());
            JPanel body = new JPanel(new BorderLayout(0, 10));
            body.setOpaque(false);
            body.add(refresh, BorderLayout.NORTH);
            body.add(scroll, BorderLayout.CENTER);
            body.add(status, BorderLayout.SOUTH);
            add(body, BorderLayout.CENTER);
            reload();
        }

        private void reload() {
            new SwingWorker<Map<String, String>, Void>() {
                @Override protected Map<String, String> doInBackground() throws Exception {
                    return new SupabaseAuthService().settings(host.session());
                }
                @Override protected void done() {
                    try {
                        Map<String, String> values = get();
                        shopName.setText(values.getOrDefault("shop_name", ""));
                        shopAddress.setText(values.getOrDefault("shop_address", ""));
                        contact.setText(values.getOrDefault("business_contact_number", ""));
                        businessEmail.setText(values.getOrDefault("business_email", ""));
                        openTime.setText(values.getOrDefault("appointment_open_time", "09:00"));
                        closeTime.setText(values.getOrDefault("appointment_close_time", "17:00"));
                        advanceDays.setText(values.getOrDefault("appointment_advance_days", "90"));
                        dailyLimit.setText(values.getOrDefault("appointment_daily_limit", "0"));
                        String days = values.getOrDefault("appointment_working_days", "1,2,3,4,5");
                        for (int day = 1; day <= 7; day++) workingDays[day - 1].setSelected(List.of(days.split(",")).contains(Integer.toString(day)));
                        status.setText("Settings loaded from Supabase.");
                    } catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("Saved settings could not be loaded: " + message(cause));
                    }
                }
            }.execute();
        }

        private void saveBusiness(JButton button) {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("shop_name", shopName.getText().trim());
            values.put("shop_address", shopAddress.getText().trim());
            values.put("business_contact_number", contact.getText().trim());
            values.put("business_email", businessEmail.getText().trim());
            save(values, button, "Business details saved.");
        }

        private void uploadLogo(JButton button) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG or JPEG image", "png", "jpg", "jpeg"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path image = chooser.getSelectedFile().toPath();
            button.setEnabled(false);
            status.setText("Uploading business logo…");
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception {
                    String path = new SupabaseStorageService().uploadLogo(host.session(), image);
                    new SupabaseAuthService().updateBusinessLogo(host.session(), path);
                    return path;
                }
                @Override protected void done() {
                    button.setEnabled(true);
                    try {
                        String path = get();
                        status.setText("Business logo uploaded and saved: " + path);
                    } catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("Business logo could not be saved: " + message(cause));
                    }
                }
            }.execute();
        }

        private void saveAppointmentSettings(JButton button) {
            LocalTime opens, closes;
            int advance, daily;
            try {
                opens = LocalTime.parse(openTime.getText().trim());
                closes = LocalTime.parse(closeTime.getText().trim());
                if (!closes.isAfter(opens)) throw new IllegalArgumentException("Closing time must be after opening time.");
                advance = Integer.parseInt(advanceDays.getText().trim());
                daily = Integer.parseInt(dailyLimit.getText().trim());
                if (advance < 1 || daily < 0) throw new IllegalArgumentException("Advance days must be positive and daily limit cannot be negative.");
            } catch (RuntimeException exception) {
                status.setText("Check appointment settings: " + message(exception));
                return;
            }
            List<String> selectedDays = new ArrayList<>();
            for (int i = 0; i < workingDays.length; i++) if (workingDays[i].isSelected()) selectedDays.add(Integer.toString(i + 1));
            if (selectedDays.isEmpty()) { status.setText("Select at least one working day."); return; }
            Map<String, String> values = new LinkedHashMap<>();
            values.put("appointment_open_time", opens.toString());
            values.put("appointment_close_time", closes.toString());
            values.put("appointment_advance_days", Integer.toString(advance));
            values.put("appointment_daily_limit", Integer.toString(daily));
            values.put("appointment_working_days", String.join(",", selectedDays));
            save(values, button, "Appointment settings saved.");
        }

        private void save(Map<String, String> values, JButton button, String success) {
            button.setEnabled(false);
            status.setText("Saving settings…");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    new SupabaseAuthService().saveSettings(host.session(), values);
                    return null;
                }
                @Override protected void done() {
                    button.setEnabled(true);
                    try { get(); status.setText(success); }
                    catch (Exception exception) {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        status.setText("Settings were not saved: " + message(cause));
                    }
                }
            }.execute();
        }

        private static JPanel unavailable(String title, String detail) {
            JPanel note = new JPanel(new BorderLayout());
            note.setOpaque(false);
            JLabel message = CrsmUi.muted(detail);
            JButton unavailable = CrsmUi.ghost(title + " unavailable");
            unavailable.setEnabled(false);
            unavailable.setToolTipText(detail);
            note.add(message, BorderLayout.CENTER);
            note.add(unavailable, BorderLayout.EAST);
            return CrsmUi.section(title, "Not connected to a supported backend action.", note);
        }
    }

    private static JPanel section(String title, Component content) {
        JPanel card = CrsmUi.card();
        card.add(CrsmUi.heading(title, 15), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        CrsmUi.styleTable(table);
        table.setRowHeight(38);
        table.setFillsViewportHeight(true);
        return table;
    }

    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static void showError(Component parent, Throwable cause) {
        JOptionPane.showMessageDialog(parent, message(cause), "Unable to complete action", JOptionPane.ERROR_MESSAGE);
    }

    private static String message(Throwable cause) {
        return cause.getMessage() == null || cause.getMessage().isBlank() ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private record RepairDataModelValue() {
        static java.math.BigDecimal money(String value) {
            if (value == null || value.isBlank()) return java.math.BigDecimal.ZERO;
            return new java.math.BigDecimal(value);
        }
    }
}
