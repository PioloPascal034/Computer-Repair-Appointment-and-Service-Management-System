package crsm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** Searchable, role-aware management view for one database entity. */
final class CrsmListing extends JPanel {
    private final CrsmHost host;
    private final RepairDataModel.Entity entity;
    private final JTextField search = new JTextField();
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton view = CrsmUi.ghost("View details");
    private final JButton edit = CrsmUi.ghost("Edit");
    private final JButton delete = CrsmUi.ghost("Delete");
    private final JButton create = CrsmUi.primary("Add record");
    private final JButton refresh = CrsmUi.ghost("Refresh");
    private final Timer searchTimer = new Timer(300, event -> reload());
    private List<RepairDataModel.Row> rows = List.of();
    private int generation;
    private boolean busy;

    CrsmListing(CrsmHost host, RepairDataModel.Entity entity) {
        super(new BorderLayout(16, 16));
        this.host = host;
        this.entity = entity;
        setBackground(CrsmTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        searchTimer.setRepeats(false);

        refresh.addActionListener(event -> reload());
        create.setText("Add " + singular(entity.label));
        create.setEnabled(host.canMutate(entity));
        create.setToolTipText(host.canMutate(entity) ? "Create a record" : "Your role cannot create this record");
        create.addActionListener(event -> editRecord(null));
        add(CrsmUi.pageHead(entity.label, "Search and manage " + entity.label.toLowerCase() + " records.", refresh), BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        CrsmUi.styleField(search);
        search.putClientProperty("JTextField.placeholderText", "Search " + entity.label.toLowerCase() + "…");
        toolbar.add(search, BorderLayout.CENTER);
        toolbar.add(create, BorderLayout.EAST);

        model.setColumnIdentifiers(columns());
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        CrsmUi.styleTable(table);
        table.setRowHeight(40);
        table.getSelectionModel().addListSelectionListener(event -> updateActions());
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "viewRecord");
        table.getActionMap().put("viewRecord", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { viewRecord(); }
        });
        JScrollPane tableScroll = CrsmUi.scroll(table);
        tableScroll.setPreferredSize(new Dimension(800, 420));

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(view); actions.add(edit); actions.add(delete);
        view.addActionListener(event -> viewRecord());
        edit.addActionListener(event -> editSelected());
        delete.addActionListener(event -> deleteSelected());

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.add(status, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(tableScroll, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { searchTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { searchTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { searchTimer.restart(); }
        });
        updateActions();
        reload();
    }

    void reload() {
        int request = ++generation;
        status.setText("Loading " + entity.label.toLowerCase() + "…");
        setBusy(true);
        new SwingWorker<List<RepairDataModel.Row>, Void>() {
            @Override protected List<RepairDataModel.Row> doInBackground() throws SQLException {
                return host.repository().findAll(entity, search.getText());
            }
            @Override protected void done() {
                if (request != generation) return;
                setBusy(false);
                try {
                    rows = get();
                    renderRows();
                    status.setText(rows.size() + (rows.size() == 1 ? " record" : " records"));
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    status.setText("Could not load " + entity.label.toLowerCase() + ".");
                    host.showDatabaseError(cause instanceof Exception ex ? ex : new IllegalStateException(cause));
                }
            }
        }.execute();
    }

    void applySearch(String query) {
        search.setText(query == null ? "" : query);
    }

    private void renderRows() {
        model.setRowCount(0);
        for (RepairDataModel.Row row : rows) {
            Object[] values = new Object[entity.fields.size() + 1];
            values[0] = row.id();
            for (int i = 0; i < entity.fields.size(); i++) values[i + 1] = row.values().get(entity.fields.get(i));
            model.addRow(values);
        }
        updateActions();
    }

    private Object[] columns() {
        List<String> columns = new ArrayList<>();
        columns.add(entity.idColumn);
        columns.addAll(entity.fields);
        return columns.stream().map(CrsmUi::titleCase).toArray();
    }

    private RepairDataModel.Row selectedRow() {
        int selected = table.getSelectedRow();
        if (selected < 0) return null;
        int modelIndex = table.convertRowIndexToModel(selected);
        if (modelIndex < 0 || modelIndex >= rows.size()) return null;
        return rows.get(modelIndex);
    }

    private void updateActions() {
        boolean selected = selectedRow() != null;
        view.setEnabled(selected && !busy);
        edit.setEnabled(selected && !busy && host.canMutate(entity));
        delete.setEnabled(selected && !busy && host.canMutate(entity));
        if (!host.canMutate(entity)) {
            edit.setToolTipText("Your role cannot edit " + entity.label.toLowerCase());
            delete.setToolTipText("Your role cannot delete " + entity.label.toLowerCase());
        }
    }

    private void viewRecord() {
        RepairDataModel.Row row = selectedRow();
        if (row == null) return;
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        details.add(CrsmUi.heading(entity.label + " #" + row.id(), 17));
        details.add(Box.createVerticalStrut(12));
        for (String field : entity.fields) {
            JPanel line = new JPanel(new BorderLayout(12, 0));
            line.setOpaque(false);
            JLabel label = CrsmUi.muted(CrsmUi.titleCase(field));
            JLabel value = new JLabel(row.value(field).isBlank() ? "—" : row.value(field));
            value.setFont(CrsmTheme.font(Font.PLAIN, 13));
            line.add(label, BorderLayout.WEST);
            line.add(value, BorderLayout.CENTER);
            line.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            details.add(line);
        }
        JOptionPane.showMessageDialog(this, new JScrollPane(details), entity.label + " details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void editSelected() {
        RepairDataModel.Row row = selectedRow();
        if (row != null) editRecord(row);
    }

    private void editRecord(RepairDataModel.Row existing) {
        if (!host.canMutate(entity)) {
            JOptionPane.showMessageDialog(this, "Your role does not have permission to change " + entity.label.toLowerCase() + ".", "Permission denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Map<String, JComponent> inputs = new LinkedHashMap<>();
        JPanel fields = new JPanel(new GridLayout(0, 2, 14, 12));
        fields.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        for (String field : entity.fields) {
            JComponent input;
            if (isLongText(field)) {
                JTextArea area = CrsmUi.multiline(3);
                if (existing != null) area.setText(existing.value(field));
                input = new JScrollPane(area);
            } else {
                JTextField text = new JTextField(existing == null ? "" : existing.value(field));
                CrsmUi.styleField(text);
                input = text;
            }
            inputs.put(field, input);
            fields.add(CrsmUi.labeled(CrsmUi.titleCase(field) + (entity.requiredFields.contains(field) ? " *" : ""), input));
        }
        JScrollPane formScroll = new JScrollPane(fields);
        formScroll.setBorder(null);
        formScroll.setPreferredSize(new Dimension(720, Math.min(500, Math.max(240, entity.fields.size() * 70))));
        String title = (existing == null ? "Add " : "Edit ") + singular(entity.label);
        if (JOptionPane.showConfirmDialog(this, formScroll, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String value;
            if (entry.getValue() instanceof JScrollPane scroll && scroll.getViewport().getView() instanceof JTextArea area) value = area.getText().trim();
            else if (entry.getValue() instanceof JTextField text) value = text.getText().trim();
            else value = "";
            values.put(entry.getKey(), value);
        }
        String validation = validate(values);
        if (validation != null) {
            JOptionPane.showMessageDialog(this, validation, "Check record details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Integer id = existing == null ? null : ((Number) existing.id()).intValue();
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                host.repository().save(entity, id, values);
                return null;
            }
            @Override protected void done() {
                setBusy(false);
                try {
                    get();
                    reload();
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    JOptionPane.showMessageDialog(CrsmListing.this, cause.getMessage(), "Could not save record", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String validate(Map<String, String> values) {
        for (String required : entity.requiredFields) {
            if (values.getOrDefault(required, "").isBlank()) return CrsmUi.titleCase(required) + " is required.";
        }
        for (Map.Entry<String, String> value : values.entrySet()) {
            String field = value.getKey();
            String raw = value.getValue();
            if (raw.isBlank()) continue;
            try {
                if (field.endsWith("_id") || field.equals("customer_id") || field.equals("computer_id") || field.equals("technician_id") || field.equals("service_id") || field.equals("repair_request_id")) Integer.parseInt(raw);
                else if (field.endsWith("_cost") || field.equals("service_price") || field.equals("amount_paid")) new java.math.BigDecimal(raw);
                else if (field.endsWith("_date")) LocalDate.parse(raw);
                else if (field.endsWith("_time")) LocalTime.parse(raw);
            } catch (RuntimeException exception) {
                return CrsmUi.titleCase(field) + " has an invalid value.";
            }
        }
        return null;
    }

    private void deleteSelected() {
        RepairDataModel.Row row = selectedRow();
        if (row == null || !host.canMutate(entity)) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + singular(entity.label).toLowerCase() + " #" + row.id() + "? This cannot be undone.",
                "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                host.repository().delete(entity, ((Number) row.id()).intValue());
                return null;
            }
            @Override protected void done() {
                setBusy(false);
                try {
                    get();
                    reload();
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    JOptionPane.showMessageDialog(CrsmListing.this, cause.getMessage(), "Could not delete record", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        table.setEnabled(!busy);
        search.setEnabled(!busy);
        create.setEnabled(!busy && host.canMutate(entity));
        refresh.setEnabled(!busy);
        updateActions();
    }

    private static boolean isLongText(String field) {
        return field.contains("problem") || field.equals("diagnosis") || field.equals("address") || field.equals("description") || field.equals("specification") || field.equals("verified_specification");
    }

    private static String singular(String label) {
        return label.equals("Services") ? "Service" : label.equals("Repair Requests") ? "Repair Request" : label.endsWith("s") ? label.substring(0, label.length() - 1) : label;
    }
}
