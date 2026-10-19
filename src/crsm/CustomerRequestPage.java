package crsm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Customer onboarding and repair intake, submitted through JWT-authenticated RPCs. */
final class CustomerRequestPage extends JPanel {
    private final UserSession session;
    private final CustomerWorkflowService service = new CustomerWorkflowService();
    private final JTextField phone = new JTextField(), address = new JTextField(), serial = new JTextField();
    private final JTextField appointmentDate = new JTextField(nextWeekday().toString());
    private final JTextField appointmentTime = new JTextField("09:00");
    private final JComboBox<String> deviceType = new JComboBox<>(new String[]{"Laptop", "Desktop", "Tablet", "All-in-one", "Gaming PC", "Monitor", "Other"});
    private final JComboBox<String> brand = new JComboBox<>(new String[]{"Acer", "Apple", "ASUS", "Dell", "HP", "Lenovo", "Microsoft", "MSI", "Samsung", "Other"});
    private final JComboBox<String> model = new JComboBox<>();
    private final ButtonGroup conditionGroup = new ButtonGroup();
    private final JPanel conditionChoices = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JComboBox<String> purpose = new JComboBox<>(new String[]{"Device drop-off", "Diagnostics", "Repair consultation", "Device collection"});
    private final JTextArea problem = area(3), work = area(2), accessories = area(2);
    private final JPanel photoNames = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final List<Path> photos = new ArrayList<>();
    private final JLabel selectedService = new JLabel("No service selected");
    private final JLabel servicePrice = new JLabel(" ");
    private final List<Map<String,Object>> deviceCatalog = new ArrayList<>();
    private int selectedServiceId = -1;

    CustomerRequestPage(UserSession session) {
        this.session = session;
        setLayout(new BorderLayout(16, 16));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        JPanel top = new JPanel(new BorderLayout(4, 5)); top.setOpaque(false);
        JLabel title = new JLabel("Tell us about your device"); title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        top.add(title, BorderLayout.NORTH);
        top.add(new JLabel("Register your device, describe the issue, and choose a drop-off time."), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 18, 14)); form.setOpaque(false);
        style(deviceType); style(brand); style(model); style(purpose);
        brand.setEditable(true); model.setEditable(true);
        conditionChoices.setOpaque(false);
        for (String option : new String[]{"Working", "Intermittent", "No power", "Physical damage"}) {
            JRadioButton radio = new JRadioButton(option); radio.setOpaque(false); conditionGroup.add(radio); conditionChoices.add(radio);
            if (option.equals("Working")) radio.setSelected(true);
        }
        deviceType.addActionListener(e -> { refreshBrands(); refreshModels(); refreshServicePrice(); }); brand.addActionListener(e -> refreshModels());
        refreshModels();
        addRow(form, "Contact number *", phone); addRow(form, "Address", address);
        addRow(form, "Device category *", deviceType); addRow(form, "Brand *", brand);
        addRow(form, "Model", model); addRow(form, "Serial number (optional)", serial);
        addRow(form, "Device condition *", conditionChoices); addRow(form, "Items handed in", accessories);
        addRow(form, "What is wrong? *", problem); addRow(form, "What would you like us to do?", work);
        JPanel photosRow = new JPanel(new BorderLayout(4, 4)); photosRow.setOpaque(false);
        JButton attach = new JButton("Add photos (up to 3)"); attach.addActionListener(e -> choosePhotos());
        photosRow.add(attach, BorderLayout.NORTH); photosRow.add(photoNames, BorderLayout.CENTER);
        addRow(form, "Damage photos", photosRow);
        JPanel appt = new JPanel(new GridLayout(1, 2, 8, 0)); appt.setOpaque(false); appt.add(appointmentDate); appt.add(appointmentTime);
        style(appointmentDate); style(appointmentTime);
        addRow(form, "Drop-off date / time (YYYY-MM-DD / HH:MM)", appt);
        addRow(form, "Appointment type", purpose);
        JPanel servicePanel = new JPanel(new BorderLayout(6, 4)); servicePanel.setOpaque(false);
        selectedService.setText("No service selected — staff can recommend one after diagnosis.");
        servicePanel.add(selectedService, BorderLayout.CENTER);
        servicePanel.add(servicePrice, BorderLayout.SOUTH);
        addRow(form, "Selected service", servicePanel);
        JScrollPane scroll = new JScrollPane(form); scroll.setBorder(null); scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
        JButton submit = new JButton("Submit repair request"); submit.setPreferredSize(new Dimension(0, 44));
        submit.setBackground(new Color(45, 50, 80)); submit.setForeground(Color.WHITE); submit.setFocusPainted(false);
        submit.addActionListener(e -> submit(submit)); add(submit, BorderLayout.SOUTH);
        loadDeviceCatalog();
    }

    void selectService(int serviceId, String name, String description) {
        selectedServiceId = serviceId;
        selectedService.setText("Selected: " + name + (description == null || description.isBlank() ? "" : " — " + description));
        work.setText(name);
        refreshServicePrice();
    }

    void applySearch(String query) {
        String q = query == null ? "" : query.trim();
        if (!q.isBlank()) for (Map<String,Object> item : deviceCatalog) {
            String make = String.valueOf(item.get("brand")), deviceModel = String.valueOf(item.get("model"));
            if ((make + " " + deviceModel).toLowerCase().contains(q.toLowerCase())) {
                deviceType.setSelectedItem(item.get("device_type")); refreshBrands(); brand.setSelectedItem(make); refreshModels(); model.setSelectedItem(deviceModel); return;
            }
        }
        for (int i = 0; i < brand.getItemCount(); i++) if (brand.getItemAt(i).equalsIgnoreCase(q)) { brand.setSelectedIndex(i); refreshModels(); return; }
        if (!q.isBlank()) { problem.requestFocusInWindow(); problem.setText(q); }
    }

    private void refreshModels() {
        String b = String.valueOf(brand.getEditor().getItem()).trim().toLowerCase();
        String t = String.valueOf(deviceType.getSelectedItem());
        List<String> models = new ArrayList<>();
        if (!deviceCatalog.isEmpty()) {
            for (Map<String,Object> row : deviceCatalog) if (t.equals(String.valueOf(row.get("device_type"))) && b.equals(String.valueOf(row.get("brand")).toLowerCase())) models.add(String.valueOf(row.get("model")));
            if (!models.isEmpty()) { model.removeAllItems(); models.forEach(model::addItem); model.setEditable(true); return; }
        }
        if (b.contains("apple")) models.addAll(t.equals("Laptop") ? List.of("MacBook Air 13-inch", "MacBook Air 15-inch", "MacBook Pro 14-inch", "MacBook Pro 16-inch") : List.of("iMac 24-inch", "Mac mini", "Mac Studio"));
        else if (b.contains("dell")) models.addAll(List.of("Inspiron 14", "Inspiron 15", "XPS 13", "XPS 15", "Latitude 5440", "Precision 5680", "OptiPlex"));
        else if (b.contains("hp")) models.addAll(List.of("Pavilion 14", "Pavilion 15", "Envy x360", "Spectre x360", "EliteBook 840", "ProBook 450", "ProDesk"));
        else if (b.contains("lenovo")) models.addAll(List.of("ThinkPad E14", "ThinkPad T14", "ThinkPad X1 Carbon", "IdeaPad Slim 3", "Yoga 7", "Legion 5", "ThinkCentre"));
        else if (b.contains("asus")) models.addAll(List.of("Vivobook 15", "Zenbook 14", "ROG Zephyrus G14", "TUF Gaming A15", "ExpertBook B1"));
        else if (b.contains("acer")) models.addAll(List.of("Aspire 5", "Swift Go 14", "Swift X", "Predator Helios Neo 16", "Nitro V 15"));
        else if (b.contains("microsoft")) models.addAll(List.of("Surface Laptop 5", "Surface Laptop 6", "Surface Pro 9", "Surface Pro 10", "Surface Laptop Studio 2"));
        else if (b.contains("msi")) models.addAll(List.of("Modern 14", "Prestige 14", "Katana 15", "Cyborg 15", "Raider GE78"));
        else if (b.contains("samsung")) models.addAll(List.of("Galaxy Book4", "Galaxy Book4 Pro", "Galaxy Book4 Ultra", "Galaxy Book3 360"));
        if (models.isEmpty()) models.add("Type model name");
        model.removeAllItems(); models.forEach(model::addItem); model.setEditable(true);
    }

    private void loadDeviceCatalog() {
        new SwingWorker<List<Map<String,Object>>,Void>() {
            protected List<Map<String,Object>> doInBackground() throws Exception { return service.deviceCatalog(session); }
            protected void done() { try { deviceCatalog.clear(); deviceCatalog.addAll(get()); if (!deviceCatalog.isEmpty()) {
                    Object priorType=deviceType.getSelectedItem(); deviceType.removeAllItems();
                    deviceCatalog.stream().map(r->String.valueOf(r.get("device_type"))).distinct().forEach(deviceType::addItem);
                    if (priorType!=null) deviceType.setSelectedItem(priorType);
                    refreshBrands(); refreshModels();
                } } catch (Exception ex) { /* retain editable common-brand/model suggestions when the catalog migration is not installed */ } }
        }.execute();
    }
    private void refreshBrands() {
        String t=String.valueOf(deviceType.getSelectedItem()); Object old=brand.getEditor().getItem();
        List<String> brands=deviceCatalog.stream().filter(r->t.equals(String.valueOf(r.get("device_type")))).map(r->String.valueOf(r.get("brand"))).distinct().toList();
        if(brands.isEmpty())return; brand.removeAllItems(); brands.forEach(brand::addItem); brand.setEditable(true); if(old!=null)brand.setSelectedItem(old);
    }
    private void refreshServicePrice() {
        if(selectedServiceId<0)return; String type=String.valueOf(deviceType.getSelectedItem()); servicePrice.setText("Checking price for "+type+"…");
        new SwingWorker<String,Void>() { protected String doInBackground() throws Exception { return service.devicePrice(session,selectedServiceId,type); }
            protected void done(){try{String price=get();servicePrice.setText(price.isBlank()?"Device-specific price not set; staff will confirm.":"Estimated catalog price for "+type+": ₱"+price);}catch(Exception ignored){servicePrice.setText("Staff will confirm the device-specific price.");}} }.execute();
    }

    private void choosePhotos() {
        int remaining = 3 - photos.size();
        if (remaining <= 0) { JOptionPane.showMessageDialog(this, "You can attach up to 3 photos."); return; }
        JFileChooser chooser = new JFileChooser(); chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Images (PNG, JPG)", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File[] chosen=chooser.getSelectedFiles(); if(chosen.length==0&&chooser.getSelectedFile()!=null)chosen=new java.io.File[]{chooser.getSelectedFile()};
        for (java.io.File file : chosen) {
            if (photos.size() >= 3) break;
            try { java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file); if (image == null) throw new IllegalArgumentException("Not a readable image."); if (file.length() > 2L * 1024 * 1024) throw new IllegalArgumentException("Each image must be 2 MB or smaller."); photos.add(file.toPath()); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, file.getName() + ": " + ex.getMessage(), "Photo not added", JOptionPane.WARNING_MESSAGE); }
        }
        photoNames.removeAll(); for (Path photo : photos) { JButton chip = new JButton("× " + photo.getFileName()); chip.addActionListener(e -> { photos.remove(photo); updatePhotoNames(); }); photoNames.add(chip); }
        photoNames.revalidate(); photoNames.repaint();
    }
    private void updatePhotoNames() { photoNames.removeAll(); for (Path photo : photos) { JButton chip = new JButton("× " + photo.getFileName()); chip.addActionListener(e -> { photos.remove(photo); updatePhotoNames(); }); photoNames.add(chip); } photoNames.revalidate(); photoNames.repaint(); }

    private void submit(JButton button) {
        if (phone.getText().isBlank() || String.valueOf(brand.getEditor().getItem()).isBlank() || problem.getText().isBlank()) { JOptionPane.showMessageDialog(this, "Enter your contact number, device brand, and a description of the problem.", "Required information", JOptionPane.WARNING_MESSAGE); return; }
        try { LocalDate.parse(appointmentDate.getText().trim()); java.time.LocalTime.parse(appointmentTime.getText().trim()); }
        catch (RuntimeException ex) { JOptionPane.showMessageDialog(this, "Enter a valid date (YYYY-MM-DD) and time (HH:MM).", "Appointment time", JOptionPane.WARNING_MESSAGE); return; }
        Map<String,Object> values = new LinkedHashMap<>();
        values.put("device_type", deviceType.getSelectedItem()); values.put("brand", String.valueOf(brand.getEditor().getItem()).trim());
        values.put("model", String.valueOf(model.getEditor().getItem()).trim()); values.put("serial", serial.getText().trim());
        values.put("problem", problem.getText().trim()); values.put("work", work.getText().trim()); values.put("condition", selectedCondition());
        values.put("accessories", accessories.getText().trim()); values.put("appointment_date", appointmentDate.getText().trim());
        values.put("appointment_time", appointmentTime.getText().trim()); values.put("purpose", purpose.getSelectedItem());
        values.put("service_id", selectedServiceId < 0 ? null : selectedServiceId);
        String phoneValue=phone.getText().trim(), addressValue=address.getText().trim();
        List<Path> selectedPhotos=List.copyOf(photos);
        button.setEnabled(false);
        new SwingWorker<SubmitResult,Void>() {
            protected SubmitResult doInBackground() throws Exception {
                int cid = service.customerId(session);
                if (cid < 0) cid = service.completeProfile(session, phoneValue, addressValue);
                int id=service.submit(session, cid, values);
                List<String> failures=new ArrayList<>();
                for(Path photo:selectedPhotos)try{
                    String path=new SupabaseStorageService().uploadRepairPhoto(session,id,photo);
                    service.registerPhoto(session,id,path);
                }catch(Exception uploadError){failures.add(photo.getFileName()+": "+uploadError.getMessage());}
                return new SubmitResult(id,failures,selectedPhotos.size());
            }
            protected void done() {
                button.setEnabled(true);
                try {
                    SubmitResult result=get();
                    String message = "Request #" + result.id + " submitted. Your appointment is pending staff confirmation.";
                    if (!result.failures.isEmpty()) message += "\n\nRequest saved, but these photos could not be uploaded:\n" + String.join("\n", result.failures);
                    else if (result.photoCount > 0) message += "\n" + result.photoCount + " photo(s) uploaded.";
                    JOptionPane.showMessageDialog(CustomerRequestPage.this, message, "Request submitted", result.failures.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                    photos.clear(); updatePhotoNames();
                } catch (Exception ex) { Throwable cause = ex.getCause() == null ? ex : ex.getCause(); JOptionPane.showMessageDialog(CustomerRequestPage.this, cause.getMessage(), "Unable to submit request", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private static LocalDate nextWeekday() { LocalDate d=LocalDate.now().plusDays(1); while (d.getDayOfWeek().getValue()>5) d=d.plusDays(1); return d; }
    private String selectedCondition() { for (java.awt.Component c : conditionChoices.getComponents()) if (c instanceof JRadioButton radio && radio.isSelected()) return radio.getText(); return "Other"; }
    private static JTextArea area(int rows) { JTextArea a=new JTextArea(rows,22); a.setLineWrap(true); a.setWrapStyleWord(true); a.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(190,195,215)),BorderFactory.createEmptyBorder(7,8,7,8))); return a; }
    private static void style(java.awt.Component c) { c.setPreferredSize(new Dimension(220,34)); }
    private static void addRow(JPanel panel,String label,java.awt.Component field) { panel.add(new JLabel(label)); panel.add(field); }
    private static final class SubmitResult { final int id; final List<String> failures; final int photoCount; SubmitResult(int id,List<String> failures,int photoCount){this.id=id;this.failures=failures;this.photoCount=photoCount;} }
}
