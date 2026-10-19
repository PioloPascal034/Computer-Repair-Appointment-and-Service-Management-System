package crsm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/** Schema-backed listing pages that follow the supplied management-screen layout. */
final class CrsmListing extends JPanel {
    private final CrsmHost host;
    private final RepairDataModel.Entity entity;
    private final JTextField search = new JTextField();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 12));
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return column == getColumnCount() - 1; }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");
    private final JLabel realtime = new JLabel(" ");
    private final JButton previous = CrsmUi.ghost("‹");
    private final JButton next = CrsmUi.ghost("›");
    private final JLabel pageLabel = CrsmUi.muted(" ");
    private final Timer searchTimer;
    private int page = 1;
    private int total = 0;
    private List<Map<String, Object>> rows = List.of();

    CrsmListing(CrsmHost host, RepairDataModel.Entity entity) {
        this.host = host;
        this.entity = entity;
        setLayout(new BorderLayout(0, 12));
        setBackground(CrsmTheme.CANVAS);
        setBorder(new javax.swing.border.EmptyBorder(8, 4, 4, 4));

        JPanel head = new JPanel(new BorderLayout(0, 12));
        head.setOpaque(false);
        head.add(CrsmUi.pageHead(entity.label, subtitle(), clock()), BorderLayout.NORTH);
        metrics.setOpaque(false);
        head.add(metrics, BorderLayout.CENTER);
        head.add(tools(), BorderLayout.SOUTH);
        add(head, BorderLayout.NORTH);

        CrsmUi.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(40);
        add(CrsmUi.scroll(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(status);
        left.add(realtime);
        JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pager.setOpaque(false);
        previous.addActionListener(e -> { if (page > 1) { page--; reload(); } });
        next.addActionListener(e -> { if (page * host.pageSize() < total) { page++; reload(); } });
        pager.add(pageLabel);
        pager.add(previous);
        pager.add(next);
        bottom.add(left, BorderLayout.WEST);
        bottom.add(pager, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        searchTimer = new Timer(350, e -> { page = 1; reload(); });
        searchTimer.setRepeats(false);
        search.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent event) { searchTimer.restart(); }
        });
        if (entity == RepairDataModel.Entity.TECHNICIANS) realtime.setForeground(CrsmTheme.LAVENDER);
    }

    private String subtitle() {
        return switch (entity) {
            case CUSTOMERS -> "Manage customer information and computer records.";
            case COMPUTERS -> "Manage computer and device records with their assigned customers.";
            case APPOINTMENTS -> "Schedule and manage customer appointments for computer repair services.";
            case REPAIR_REQUESTS -> "Manage and track all computer repair requests and their status.";
            case TECHNICIANS -> "Manage technician records, skills, availability, and assigned repairs.";
            case REPAIR_SERVICES -> "Manage computer repair services, pricing, and details.";
            case PAYMENTS -> "Manage and track customer payments for computer repair services.";
        };
    }

    private JLabel clock() {
        JLabel label = CrsmUi.muted(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy  HH:mm")));
        label.setFont(CrsmTheme.font(Font.BOLD, 12));
        return label;
    }

    private JPanel tools() {
        JPanel tools = CrsmUi.card();
        tools.setLayout(new BorderLayout(8, 0));
        CrsmUi.styleField(search);
        search.setToolTipText(searchHint());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        search.setPreferredSize(new Dimension(360, 40));
        filters.add(search);
        statusFilter.removeAllItems();
        statusFilter.addItem("All statuses");
        for (String value : statuses()) statusFilter.addItem(value);
        statusFilter.setPreferredSize(new Dimension(160, 36));
        statusFilter.addActionListener(e -> { page = 1; reload(); });
        if (!statuses().isEmpty()) filters.add(statusFilter);
        JButton refresh = CrsmUi.ghost("Refresh");
        refresh.addActionListener(e -> reload());
        filters.add(refresh);
        tools.add(filters, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        if (host.canMutate(entity)) {
            JButton add = CrsmUi.navy("+ Add");
            add.addActionListener(e -> edit(null));
            actions.add(add);
        }
        tools.add(actions, BorderLayout.EAST);
        return tools;
    }

    private String searchHint() {
        return switch (entity) {
            case CUSTOMERS -> "Search by ID, name, contact number, email address";
            case COMPUTERS -> "Search by ID, device type, brand, model, customer name";
            case APPOINTMENTS -> "Search by ID, customer name, purpose, or status";
            case REPAIR_REQUESTS -> "Search by ID, customer, service, technician, or status";
            case TECHNICIANS -> "Search by ID, name, or specialization";
            case REPAIR_SERVICES -> "Search by service name or description";
            case PAYMENTS -> "Search by payment ID, customer name, repair ID, or status";
        };
    }

    private List<String> statuses() {
        return switch (entity) {
            case APPOINTMENTS -> List.of("Pending", "Confirmed", "Completed", "Cancelled");
            case REPAIR_REQUESTS -> List.of("Pending", "Confirmed", "Under Diagnosis", "Under Repair", "Waiting for Parts", "Ready for Pickup", "Completed");
            case PAYMENTS -> List.of("Unpaid", "Partial", "Paid");
            case COMPUTERS -> List.of("Pending", "Confirmed", "Under Diagnosis", "Under Repair", "Waiting for Parts", "Ready for Pickup", "Completed");
            default -> List.of();
        };
    }

    void applySearch(String query) {
        search.setText(query == null ? "" : query);
        page = 1;
        reload();
    }

    void setRealtimeState(String message) {
        realtime.setText(message == null ? "" : message);
    }

    void reload() {
        if (host.customerRecordsBlocked() && entity != RepairDataModel.Entity.REPAIR_SERVICES) {
            showCustomerGap();
            return;
        }
        String query = search.getText();
        String selected = String.valueOf(statusFilter.getSelectedItem());
        String statusValue = selected == null || selected.startsWith("All") ? null : selected;
        int pageSize = host.pageSize();
        int requestedPage = page;
        new SwingWorker<Load, Void>() {
            @Override protected Load doInBackground() throws Exception {
                RepairRepository.DisplayPage display = host.repository().findDisplay(entity, query, statusValue, requestedPage, pageSize);
                Map<String, String> cards = metricsFromDatabase();
                return new Load(display, cards);
            }
            @Override protected void done() {
                try {
                    Load load = get();
                    rows = load.display.rows();
                    total = load.display.total();
                    renderMetrics(load.metrics);
                    renderTable(load.display);
                    int from = total == 0 ? 0 : ((requestedPage - 1) * pageSize) + 1;
                    int to = Math.min(requestedPage * pageSize, total);
                    status.setText("Showing " + from + " to " + to + " of " + total + " records");
                    pageLabel.setText("Page " + requestedPage);
                    previous.setEnabled(requestedPage > 1);
                    next.setEnabled(requestedPage * pageSize < total);
                } catch (Exception ex) {
                    status.setText("Could not load records.");
                    host.showDatabaseError(ex);
                }
            }
        }.execute();
    }

    private Map<String, String> metricsFromDatabase() throws Exception {
        RepairRepository repo = host.repository();
        Map<String, String> cards = new LinkedHashMap<>();
        switch (entity) {
            case CUSTOMERS -> {
                cards.put("Total Customers", String.valueOf(repo.count(entity)));
                cards.put("Registered Devices", String.valueOf(repo.count(RepairDataModel.Entity.COMPUTERS)));
                cards.put("Repair Requests", String.valueOf(repo.count(RepairDataModel.Entity.REPAIR_REQUESTS)));
                cards.put("Appointments", String.valueOf(repo.count(RepairDataModel.Entity.APPOINTMENTS)));
            }
            case COMPUTERS -> {
                cards.put("Total Devices", String.valueOf(repo.count(entity)));
                Map<String, Integer> statuses = repo.countGroup(RepairDataModel.Entity.REPAIR_REQUESTS, "repair_status");
                int active = statuses.entrySet().stream().filter(e -> !"Completed".equals(e.getKey())).mapToInt(Map.Entry::getValue).sum();
                cards.put("Active Repairs", String.valueOf(active));
                cards.put("Completed Repairs", String.valueOf(statuses.getOrDefault("Completed", 0)));
                cards.put("Waiting for Parts", String.valueOf(statuses.getOrDefault("Waiting for Parts", 0)));
            }
            case APPOINTMENTS -> {
                cards.put("Appointments This Month", String.valueOf(repo.countAppointmentsInMonth(LocalDate.now().withDayOfMonth(1))));
                Map<String, Integer> statuses = repo.countGroup(entity, "appointment_status");
                cards.put("Confirmed", String.valueOf(statuses.getOrDefault("Confirmed", 0)));
                cards.put("Pending", String.valueOf(statuses.getOrDefault("Pending", 0)));
                cards.put("Cancelled", String.valueOf(statuses.getOrDefault("Cancelled", 0)));
            }
            case REPAIR_REQUESTS -> {
                cards.put("Total Repair Requests", String.valueOf(repo.count(entity)));
                Map<String, Integer> statuses = repo.countGroup(entity, "repair_status");
                cards.put("Under Diagnosis", String.valueOf(statuses.getOrDefault("Under Diagnosis", 0)));
                cards.put("Under Repair", String.valueOf(statuses.getOrDefault("Under Repair", 0)));
                cards.put("Completed", String.valueOf(statuses.getOrDefault("Completed", 0)));
            }
            case TECHNICIANS -> {
                cards.put("Total Technicians", String.valueOf(repo.count(entity)));
                cards.put("Repair Assignments", String.valueOf(repo.count(RepairDataModel.Entity.REPAIR_REQUESTS)));
                cards.put("Active Repairs", String.valueOf(repo.countActiveRepairs()));
                cards.put("Availability values", "Stored as text on each technician record");
            }
            case REPAIR_SERVICES -> {
                cards.put("Total Services", String.valueOf(repo.count(entity)));
                cards.put("Catalog note", "The schema has no active/inactive service flag.");
                cards.put("Linked Requests", String.valueOf(repo.count(RepairDataModel.Entity.REPAIR_REQUESTS)));
                cards.put("Payments", CrsmUi.money(repo.sumPayments()));
            }
            case PAYMENTS -> {
                cards.put("Total Payments", CrsmUi.money(repo.sumPayments()));
                cards.put("Paid", CrsmUi.money(repo.sumPaymentsByStatus("Paid")));
                cards.put("Unpaid", CrsmUi.money(repo.sumPaymentsByStatus("Unpaid")));
                cards.put("Partial", CrsmUi.money(repo.sumPaymentsByStatus("Partial")));
            }
        }
        return cards;
    }

    private void renderMetrics(Map<String, String> cards) {
        metrics.removeAll();
        metrics.setLayout(new GridLayout(1, Math.max(1, cards.size()), 12, 12));
        cards.forEach((title, value) -> metrics.add(CrsmUi.metric(title, value, null)));
        metrics.revalidate();
        metrics.repaint();
    }

    private void showCustomerGap() {
        metrics.removeAll();
        metrics.add(CrsmUi.notice("Own-record access is blocked",
                "The UCD limits a customer to their own computers, appointments, repair requests, and payments. The current business schema has no authenticated customer identity or ownership column, so those records are not loaded."));
        model.setRowCount(0);
        model.setColumnCount(0);
        model.addColumn("Access");
        model.addRow(new Object[]{"No customer-owned rows were requested from the database."});
        status.setText("Blocked until account-to-customer mapping and row scope exist.");
        previous.setEnabled(false);
        next.setEnabled(false);
        metrics.revalidate();
        metrics.repaint();
    }

    private void renderTable(RepairRepository.DisplayPage display) {
        model.setRowCount(0);
        model.setColumnCount(0);
        List<String> columns = new ArrayList<>(display.columns());
        columns.add("actions");
        for (String column : columns) model.addColumn(CrsmUi.titleCase(column));
        for (Map<String, Object> row : display.rows()) {
            Object[] values = new Object[columns.size()];
            for (int i = 0; i < display.columns().size(); i++) values[i] = row.get(display.columns().get(i));
            values[columns.size() - 1] = row;
            model.addRow(values);
        }
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(table.getColumnCount() - 1).setCellRenderer(new ActionRenderer());
            table.getColumnModel().getColumn(table.getColumnCount() - 1).setCellEditor(new ActionEditor());
            table.getColumnModel().getColumn(table.getColumnCount() - 1).setPreferredWidth(140);
        }
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                String name = table.getColumnName(column);
                if (name.toLowerCase().contains("status")) return CrsmUi.badge(CrsmUi.text(value));
                if (name.toLowerCase().contains("cost") || name.toLowerCase().contains("price") || name.toLowerCase().contains("amount")) {
                    value = CrsmUi.money(value);
                }
                Component component = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setForeground(CrsmTheme.NAVY);
                return component;
            }
        });
    }

    private int rowId(int viewRow) {
        if (viewRow < 0 || viewRow >= rows.size()) return -1;
        Object value = rows.get(viewRow).get(entity.idColumn);
        return value == null ? -1 : Integer.parseInt(String.valueOf(value));
    }

    private void view(int viewRow) {
        int id = rowId(viewRow);
        if (id < 0) return;
        new SwingWorker<RepairDataModel.Row, Void>() {
            @Override protected RepairDataModel.Row doInBackground() throws Exception { return host.repository().findById(entity, id); }
            @Override protected void done() {
                try {
                    RepairDataModel.Row row = get();
                    if (row == null) { JOptionPane.showMessageDialog(CrsmListing.this, "The record is no longer available."); return; }
                    StringBuilder detail = new StringBuilder("<html><div style='width:420px'>");
                    detail.append("<b>").append(entity.label).append(" ").append(id).append("</b><br><br>");
                    row.values().forEach((key, value) -> detail.append(CrsmUi.titleCase(key)).append(": ").append(value == null ? "—" : value).append("<br>"));
                    detail.append("</div></html>");
                    JOptionPane.showMessageDialog(CrsmListing.this, new JLabel(detail.toString()), entity.label, JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) { host.showDatabaseError(ex); }
            }
        }.execute();
    }

    private void edit(Integer viewRow) {
        if (!host.canMutate(entity) && viewRow != null) {
            view(viewRow);
            return;
        }
        if (!host.canMutate(entity)) {
            JOptionPane.showMessageDialog(this, "Your role can view this catalog but cannot change it.");
            return;
        }
        Map<String, String> initial = new LinkedHashMap<>();
        Integer id = null;
        if (viewRow != null) {
            id = rowId(viewRow);
            if (id < 0) return;
            try {
                RepairDataModel.Row row = host.repository().findById(entity, id);
                if (row == null) { JOptionPane.showMessageDialog(this, "The record is no longer available."); return; }
                for (String field : entity.fields) initial.put(field, row.value(field));
            } catch (Exception ex) { host.showDatabaseError(ex); return; }
        }
        RecordForm form = new RecordForm(this, host, entity, initial, id != null);
        form.setVisible(true);
        if (!form.saved()) return;
        Map<String, String> values = form.values();
        String validation = RecordForm.validate(entity, values);
        if (validation != null) {
            JOptionPane.showMessageDialog(this, validation, "Check the record", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Integer recordId = id;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { host.repository().save(entity, recordId, values); return null; }
            @Override protected void done() {
                try { get(); reload(); }
                catch (Exception ex) { host.showDatabaseError(ex); }
            }
        }.execute();
    }

    private void delete(int viewRow) {
        if (!host.canMutate(entity)) {
            JOptionPane.showMessageDialog(this, "Your role cannot delete this record.");
            return;
        }
        int id = rowId(viewRow);
        if (id < 0) return;
        int answer = JOptionPane.showConfirmDialog(this, "Delete " + entity.label + " record " + id + "?", "Confirm deletion", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { host.repository().delete(entity, id); return null; }
            @Override protected void done() {
                try { get(); reload(); }
                catch (Exception ex) { host.showDatabaseError(ex); }
            }
        }.execute();
    }

    private JPanel actionButtons(int row, boolean editor) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        panel.setOpaque(false);
        JButton view = CrsmUi.iconAction("◉");
        JButton edit = CrsmUi.iconAction("✎");
        JButton delete = CrsmUi.iconAction("⌫");
        view.setToolTipText("View details");
        edit.setToolTipText("Edit record");
        delete.setToolTipText("Delete record");
        view.setForeground(CrsmTheme.INDIGO);
        edit.setForeground(CrsmTheme.NAVY);
        delete.setForeground(CrsmTheme.PEACH);
        if (editor) {
            view.addActionListener(e -> { table.getCellEditor().stopCellEditing(); view(row); });
            edit.addActionListener(e -> { table.getCellEditor().stopCellEditing(); edit(row); });
            delete.addActionListener(e -> { table.getCellEditor().stopCellEditing(); delete(row); });
        }
        panel.add(view);
        panel.add(edit);
        panel.add(delete);
        return panel;
    }

    private final class ActionRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            return actionButtons(row, false);
        }
    }

    private final class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private int editingRow;
        @Override public Object getCellEditorValue() { return rows.isEmpty() ? null : rows.get(Math.min(editingRow, rows.size() - 1)); }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
            editingRow = row;
            return actionButtons(row, true);
        }
    }

    private record Load(RepairRepository.DisplayPage display, Map<String, String> metrics) {}

    static final class RecordForm extends JDialog {
        private final Map<String, Component> fields = new LinkedHashMap<>();
        private boolean saved;

        RecordForm(Component owner, CrsmHost host, RepairDataModel.Entity entity, Map<String, String> initial, boolean editing) {
            super(javax.swing.SwingUtilities.getWindowAncestor(owner), (editing ? "Edit " : "Add ") + entity.label, ModalityType.APPLICATION_MODAL);
            JPanel form = new JPanel(new GridLayout(0, 2, 10, 9));
            form.setBorder(new javax.swing.border.EmptyBorder(18, 18, 12, 18));
            form.setBackground(CrsmTheme.WHITE);
            Map<String, List<RepairRepository.Lookup>> lookups = new LinkedHashMap<>();
            try {
                if (entity.fields.contains("customer_id")) lookups.put("customer_id", host.repository().lookup(RepairDataModel.Entity.CUSTOMERS));
                if (entity.fields.contains("computer_id")) lookups.put("computer_id", host.repository().lookup(RepairDataModel.Entity.COMPUTERS));
                if (entity.fields.contains("technician_id")) lookups.put("technician_id", host.repository().lookup(RepairDataModel.Entity.TECHNICIANS));
                if (entity.fields.contains("service_id")) lookups.put("service_id", host.repository().lookup(RepairDataModel.Entity.REPAIR_SERVICES));
                if (entity.fields.contains("repair_request_id")) lookups.put("repair_request_id", host.repository().lookup(RepairDataModel.Entity.REPAIR_REQUESTS));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, "Related records could not be loaded for the form. You can still enter numeric IDs if the connection recovers.");
            }
            for (String field : entity.fields) {
                form.add(new JLabel(CrsmUi.titleCase(field)));
                List<String> choices = statusChoices(entity, field);
                Component input;
                if (!choices.isEmpty()) {
                    JComboBox<String> selector = new JComboBox<>(choices.toArray(String[]::new));
                    if (initial.get(field) != null) selector.setSelectedItem(initial.get(field));
                    input = selector;
                } else if (lookups.containsKey(field)) {
                    JComboBox<RepairRepository.Lookup> selector = new JComboBox<>();
                    selector.addItem(new RepairRepository.Lookup(0, "(none)"));
                    for (RepairRepository.Lookup lookup : lookups.get(field)) selector.addItem(lookup);
                    String current = initial.get(field);
                    if (current != null && !current.isBlank()) {
                        try {
                            int selectedId = Integer.parseInt(current.trim());
                            for (int i = 0; i < selector.getItemCount(); i++) {
                                if (selector.getItemAt(i).id() == selectedId) selector.setSelectedIndex(i);
                            }
                        } catch (NumberFormatException ignored) { }
                    }
                    input = selector;
                } else if (field.equals("reported_problem") || field.equals("diagnosis") || field.equals("description") || field.equals("address") || field.equals("purpose")) {
                    JTextArea area = CrsmUi.multiline(3);
                    area.setText(initial.getOrDefault(field, ""));
                    input = new JScrollPane(area);
                } else {
                    JTextField text = new JTextField(initial.getOrDefault(field, ""), 24);
                    CrsmUi.styleField(text);
                    input = text;
                }
                fields.put(field, input);
                form.add(input);
            }
            JButton save = CrsmUi.primary("Save");
            JButton cancel = CrsmUi.ghost("Cancel");
            save.addActionListener(e -> { saved = true; setVisible(false); });
            cancel.addActionListener(e -> setVisible(false));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.setBackground(CrsmTheme.WHITE);
            buttons.add(cancel);
            buttons.add(save);
            setLayout(new BorderLayout());
            add(form, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);
            pack();
            setLocationRelativeTo(owner);
        }

        Map<String, String> values() {
            Map<String, String> result = new LinkedHashMap<>();
            fields.forEach((key, component) -> result.put(key, read(component)));
            return result;
        }

        boolean saved() { return saved; }

        private static String read(Component component) {
            if (component instanceof JScrollPane scroll && scroll.getViewport().getView() instanceof JTextArea area) return area.getText();
            if (component instanceof JTextField text) return text.getText();
            if (component instanceof JComboBox<?> combo) {
                Object selected = combo.getSelectedItem();
                if (selected instanceof RepairRepository.Lookup lookup) return lookup.id() == 0 ? "" : String.valueOf(lookup.id());
                return selected == null ? "" : String.valueOf(selected);
            }
            return "";
        }

        static List<String> statusChoices(RepairDataModel.Entity entity, String field) {
            if (entity == RepairDataModel.Entity.REPAIR_REQUESTS && field.equals("repair_status")) {
                return List.of("Pending", "Confirmed", "Under Diagnosis", "Under Repair", "Waiting for Parts", "Ready for Pickup", "Completed");
            }
            if (entity == RepairDataModel.Entity.APPOINTMENTS && field.equals("appointment_status")) {
                return List.of("Pending", "Confirmed", "Completed", "Cancelled");
            }
            if (entity == RepairDataModel.Entity.PAYMENTS && field.equals("payment_status")) {
                return List.of("Unpaid", "Partial", "Paid");
            }
            return List.of();
        }

        static String validate(RepairDataModel.Entity entity, Map<String, String> values) {
            for (String field : entity.requiredFields) if (values.getOrDefault(field, "").isBlank()) return CrsmUi.titleCase(field) + " is required.";
            try {
                for (String field : entity.fields) {
                    String value = values.getOrDefault(field, "").trim();
                    if (value.isEmpty()) continue;
                    if (field.endsWith("_id")) Integer.parseInt(value);
                    if (field.endsWith("_cost") || field.equals("service_price") || field.equals("amount_paid")) {
                        if (new BigDecimal(value).signum() < 0) return CrsmUi.titleCase(field) + " must be zero or greater.";
                    }
                    if (field.endsWith("_date")) LocalDate.parse(value);
                    if (field.endsWith("_time")) LocalTime.parse(value.length() == 5 ? value + ":00" : value);
                }
            } catch (Exception ex) {
                return "Check number, amount, date (yyyy-MM-dd), or time (HH:mm) fields.";
            }
            return null;
        }
    }
}
