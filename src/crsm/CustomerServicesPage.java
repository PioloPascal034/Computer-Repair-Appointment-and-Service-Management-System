package crsm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Searchable, database-backed service catalog for customers. */
final class CustomerServicesPage extends JPanel {
    private static final Color NAVY = new Color(25, 54, 116);
    private static final String ALL = "All Services";
    private static final String ALL_DEVICES = "All device types";
    private static final List<String> CATEGORY_ORDER = List.of("Diagnostics", "Hardware Repair", "Cleaning", "Display", "Battery and Power", "Keyboard and Input", "Software", "Storage and Data Recovery", "Network and Connectivity");
    private final UserSession session;
    private final JPanel rows = new JPanel();
    private final JTextField search = new JTextField();
    private final JComboBox<String> category = new JComboBox<>();
    private final JComboBox<String> deviceType = new JComboBox<>();
    private final JLabel status = new JLabel(" ");
    private final JLabel selectionSummary = new JLabel("No service selected");
    private final JLabel selectionDetails = new JLabel("Choose a service to continue with your repair request.");
    private final JButton continueButton = new JButton("Continue to request");
    private final Supplier<String> selectedDeviceTypeSupplier;
    private List<Map<String, Object>> allServices = List.of();
    private List<Map<String, Object>> serviceDevicePrices = List.of();
    private Map<String, Object> selectedService;

    CustomerServicesPage(UserSession session, Consumer<Map<String, Object>> onSelect) {
        this(session, () -> "", onSelect);
    }

    CustomerServicesPage(UserSession session, Supplier<String> selectedDeviceTypeSupplier, Consumer<Map<String, Object>> onSelect) {
        this.session = session;
        this.selectedDeviceTypeSupplier = selectedDeviceTypeSupplier;
        setLayout(new BorderLayout(12, 12)); setBackground(Color.WHITE);
        JPanel header = new JPanel(); header.setOpaque(false); header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Repair Services"); title.setFont(title.getFont().deriveFont(Font.BOLD, 24f)); title.setForeground(NAVY); header.add(title);
        header.add(Box.createVerticalStrut(4)); header.add(new JLabel("Browse practical repair services. Final pricing is confirmed after device inspection.")); header.add(Box.createVerticalStrut(10));
        JPanel filters = new JPanel(new GridBagLayout()); filters.setOpaque(false);
        search.putClientProperty("JTextField.placeholderText", "Search service, problem, category, or description"); category.addItem(ALL); deviceType.addItem(ALL_DEVICES);
        addFilter(filters, search, 0, 0, 1.0); addFilter(filters, category, 1, 0, 0.28); addFilter(filters, deviceType, 2, 0, 0.28);
        JButton reset = new JButton("Clear filters"); reset.addActionListener(e -> { search.setText(""); category.setSelectedItem(ALL); deviceType.setSelectedItem(ALL_DEVICES); }); addFilter(filters, reset, 3, 0, 0.0);
        header.add(filters); header.add(status); add(header, BorderLayout.NORTH);
        rows.setOpaque(false); rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS)); rows.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        JScrollPane scroll = new JScrollPane(rows); scroll.setBorder(null); scroll.getViewport().setOpaque(false); add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(12, 4)); bottom.setBackground(new Color(246, 249, 255)); bottom.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(210, 222, 241)), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JPanel summary = new JPanel(); summary.setOpaque(false); summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS)); selectionSummary.setFont(selectionSummary.getFont().deriveFont(Font.BOLD, 14f)); selectionSummary.setForeground(NAVY); summary.add(selectionSummary); summary.add(Box.createVerticalStrut(3)); summary.add(selectionDetails); bottom.add(summary, BorderLayout.CENTER);
        continueButton.setEnabled(false); continueButton.addActionListener(e -> { if (selectedService != null) onSelect.accept(selectedService); }); bottom.add(continueButton, BorderLayout.EAST); add(bottom, BorderLayout.SOUTH);
        DocumentListener listener = new DocumentListener() { public void insertUpdate(DocumentEvent e) { render(); } public void removeUpdate(DocumentEvent e) { render(); } public void changedUpdate(DocumentEvent e) { render(); } };
        search.getDocument().addDocumentListener(listener); category.addActionListener(e -> render()); deviceType.addActionListener(e -> render()); reload();
    }

    void applySearch(String query) { search.setText(query == null ? "" : query); }

    private void addFilter(JPanel panel, java.awt.Component component, int x, int y, double weight) { GridBagConstraints c = new GridBagConstraints(); c.gridx = x; c.gridy = y; c.weightx = weight; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, x == 0 ? 0 : 6, 0, 0); panel.add(component, c); }

    private void reload() {
        status.setText("Loading the current service catalog...");
        new SwingWorker<CatalogData, Void>() {
            protected CatalogData doInBackground() throws Exception { CustomerWorkflowService service = new CustomerWorkflowService(); return new CatalogData(service.services(session), service.serviceDevicePrices(session)); }
            protected void done() {
                try { CatalogData data = get(); allServices = data.services; serviceDevicePrices = data.compatibility; populateFilters(); status.setText(allServices.size() + " service(s) available. Listed prices may change after inspection."); render(); }
                catch (Exception ex) { Throwable cause = ex.getCause() == null ? ex : ex.getCause(); status.setText("Could not load the service catalog: " + safeMessage(cause)); rows.removeAll(); rows.add(message("Services are temporarily unavailable. Please try again later.")); rows.revalidate(); rows.repaint(); }
            }
        }.execute();
    }

    private void populateFilters() {
        String oldCategory = text(category.getSelectedItem()); String oldDevice = text(selectedDeviceTypeSupplier.get()); if (oldDevice.isBlank()) oldDevice = text(deviceType.getSelectedItem());
        Set<String> categories = new LinkedHashSet<>(CATEGORY_ORDER); allServices.stream().map(this::categoryFor).filter(s -> !s.isBlank()).forEach(categories::add);
        category.removeAllItems(); category.addItem(ALL); categories.stream().sorted(Comparator.comparingInt(this::categoryRank)).forEach(category::addItem); category.setSelectedItem(categories.contains(oldCategory) ? oldCategory : ALL);
        Set<String> types = new LinkedHashSet<>(); serviceDevicePrices.stream().map(r -> text(r.get("device_type"))).filter(s -> !s.isBlank()).forEach(types::add);
        deviceType.removeAllItems(); deviceType.addItem(ALL_DEVICES); types.stream().sorted(String.CASE_INSENSITIVE_ORDER).forEach(deviceType::addItem); deviceType.setSelectedItem(types.contains(oldDevice) ? oldDevice : ALL_DEVICES);
    }

    private int categoryRank(String value) { int i = CATEGORY_ORDER.indexOf(value); return i < 0 ? 100 : i; }

    private void render() {
        rows.removeAll(); String query = search.getText().trim().toLowerCase(); String chosenCategory = text(category.getSelectedItem()); String chosenDevice = text(deviceType.getSelectedItem()); int count = 0;
        for (Map<String, Object> service : allServices) {
            String name = text(service.get("service_name")); String description = text(service.get("description")); String categoryName = categoryFor(service); String problems = text(service.get("common_problems")); String searchable = (name + " " + description + " " + categoryName + " " + problems).toLowerCase();
            if (!query.isBlank() && !searchable.contains(query)) continue; if (!ALL.equals(chosenCategory) && !categoryName.equalsIgnoreCase(chosenCategory)) continue; if (!ALL_DEVICES.equals(chosenDevice) && !isCompatible(service, chosenDevice)) continue;
            rows.add(serviceRow(service, name, description, categoryName)); rows.add(Box.createVerticalStrut(8)); count++;
        }
        if (count == 0) rows.add(message(allServices.isEmpty() ? "No services are listed yet." : "No services match the current filters.")); rows.revalidate(); rows.repaint();
    }

    private JPanel serviceRow(Map<String, Object> service, String name, String description, String categoryName) {
        int id = intValue(service.get("service_id")); boolean selected = selectedService != null && intValue(selectedService.get("service_id")) == id;
        JPanel row = new JPanel(new BorderLayout(12, 4)); row.setAlignmentX(LEFT_ALIGNMENT); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118)); row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selected ? new Color(28, 114, 220) : new Color(210, 220, 235), selected ? 2 : 1), BorderFactory.createEmptyBorder(10, 12, 10, 12))); row.setBackground(Color.WHITE);
        JPanel details = new JPanel(); details.setOpaque(false); details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS)); JLabel title = new JLabel(escape(name)); title.setFont(title.getFont().deriveFont(Font.BOLD, 15f)); title.setForeground(NAVY); details.add(title); details.add(Box.createVerticalStrut(4)); details.add(htmlLabel(description.isBlank() ? "Description will be confirmed by staff." : description, 680)); details.add(Box.createVerticalStrut(5)); String compatible = compatibleTypes(id); details.add(new JLabel(categoryName + "  |  Compatible: " + (compatible.isBlank() ? "General service" : compatible))); row.add(details, BorderLayout.CENTER);
        JPanel action = new JPanel(); action.setOpaque(false); action.setLayout(new BoxLayout(action, BoxLayout.Y_AXIS)); JLabel price = new JLabel("Starting at " + price(service.get("service_price"))); price.setFont(price.getFont().deriveFont(Font.BOLD, 14f)); price.setForeground(NAVY); action.add(price); action.add(Box.createVerticalStrut(8)); JButton choose = new JButton(selected ? "Selected" : "Choose service"); choose.addActionListener(e -> { selectedService = service; updateSummary(); render(); }); action.add(choose); row.add(action, BorderLayout.EAST); return row;
    }

    private boolean isCompatible(Map<String, Object> service, String selectedType) { int id = intValue(service.get("service_id")); boolean hasRows = false; for (Map<String, Object> row : serviceDevicePrices) { if (intValue(row.get("service_id")) != id) continue; hasRows = true; if (selectedType.equalsIgnoreCase(text(row.get("device_type")))) return true; } return !hasRows; }
    private String compatibleTypes(int serviceId) { Set<String> values = new LinkedHashSet<>(); for (Map<String, Object> row : serviceDevicePrices) if (intValue(row.get("service_id")) == serviceId) values.add(text(row.get("device_type"))); return String.join(", ", values); }
    private void updateSummary() { if (selectedService == null) { selectionSummary.setText("No service selected"); selectionDetails.setText("Choose a service to continue with your repair request."); continueButton.setEnabled(false); return; } selectionSummary.setText("Selected: " + text(selectedService.get("service_name")) + "  |  " + price(selectedService.get("service_price"))); String description = text(selectedService.get("description")); selectionDetails.setText(description.isBlank() ? "Final pricing is confirmed after device inspection." : description); continueButton.setEnabled(true); }
    private JPanel message(String value) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT)); p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT); p.add(new JLabel(value)); return p; }
    private JLabel htmlLabel(String value, int width) { return new JLabel("<html><div style='width:" + width + "px'>" + escape(value) + "</div></html>"); }
    private String categoryFor(Map<String, Object> service) { String explicit = text(service.get("category")); if (!explicit.isBlank()) return explicit; String name = text(service.get("service_name")).toLowerCase(); if (name.contains("diagnos")) return "Diagnostics"; if (name.contains("clean")) return "Cleaning"; if (name.contains("screen") || name.contains("display")) return "Display"; if (name.contains("battery") || name.contains("power")) return "Battery and Power"; if (name.contains("keyboard") || name.contains("input")) return "Keyboard and Input"; if (name.contains("storage") || name.contains("data")) return "Storage and Data Recovery"; if (name.contains("network") || name.contains("wifi") || name.contains("internet")) return "Network and Connectivity"; if (name.contains("hardware") || name.contains("replacement")) return "Hardware Repair"; return "Software"; }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static int intValue(Object value) { try { return Integer.parseInt(text(value)); } catch (RuntimeException ex) { return -1; } }
    private static String price(Object value) { String v = text(value); return v.isBlank() ? "Price to be confirmed" : "PHP " + v; }
    private static String safeMessage(Throwable t) { return t.getMessage() == null || t.getMessage().isBlank() ? t.getClass().getSimpleName() : t.getMessage(); }
    private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private record CatalogData(List<Map<String, Object>> services, List<Map<String, Object>> compatibility) { }
}
