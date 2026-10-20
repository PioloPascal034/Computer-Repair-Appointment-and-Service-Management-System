package crsm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import javax.swing.Timer;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Customer onboarding and repair intake, submitted through JWT-authenticated RPCs. */
final class CustomerRequestPage extends JPanel {
    private final UserSession session;
    private final CustomerWorkflowService service = new CustomerWorkflowService();
    private final JTextField phone = new JTextField(), address = new JTextField(), serial = new JTextField(), specification = new JTextField();
    private final JTextField appointmentDate = new JTextField(nextWeekday().toString());
    private final JTextField appointmentTime = new JTextField("09:00");
    private final JComboBox<String> deviceType = new JComboBox<>();
    private final JComboBox<String> brand = new JComboBox<>();
    private final JComboBox<String> model = new JComboBox<>();
    private final ButtonGroup conditionGroup = new ButtonGroup();
    private final JPanel conditionChoices = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JComboBox<String> purpose = new JComboBox<>(new String[]{"Device drop-off", "Diagnostics", "Repair consultation", "Device collection"});
    private final JTextArea problem = area(3), work = area(2), accessories = area(2);
    private final JPanel photoNames = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final List<Path> photos = new ArrayList<>();
    private final JLabel selectedService = new JLabel("No service selected");
    private final JLabel selectedServiceDescription = new JLabel("Staff can recommend a service after diagnosis.");
    private final JLabel servicePrice = new JLabel("Choose a service and device to see a tailored estimate.");
    private final JButton browseServices = new JButton("Choose / Change");
    private final Timer estimateTimer = new Timer(300, e -> refreshServiceEstimate());
    private final List<Map<String,Object>> deviceCatalog = new ArrayList<>();
    private final Runnable onBrowseServices;
    private int selectedServiceId = -1;
    private String selectedServiceName = "";
    private int estimateGeneration;

    CustomerRequestPage(UserSession session, Runnable onBrowseServices) {
        this.session = session;
        this.onBrowseServices = onBrowseServices;
        estimateTimer.setRepeats(false);
        setLayout(new BorderLayout(16, 16));
        setBackground(CrsmTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        add(CrsmUi.pageHead("New repair request",
                "Tell us about your device and choose a convenient drop-off time.", null), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(4, 2, 10, 2));
        style(deviceType); style(brand); style(model); style(purpose);
        CrsmUi.styleField(phone); CrsmUi.styleField(address); CrsmUi.styleField(serial);
        CrsmUi.styleField(specification); CrsmUi.styleField(appointmentDate); CrsmUi.styleField(appointmentTime);
        styleArea(problem); styleArea(work); styleArea(accessories);
        brand.setEnabled(false); model.setEnabled(false); model.setEditable(true);
        conditionChoices.setOpaque(false);
        for (String option : new String[]{"Working", "Intermittent", "No power", "Physical damage"}) {
            JRadioButton radio = new JRadioButton(option); radio.setOpaque(false); conditionGroup.add(radio); conditionChoices.add(radio);
            if (option.equals("Working")) radio.setSelected(true);
        }
        deviceType.addActionListener(e -> { refreshBrands(); refreshModels(); clearSpecification(); scheduleServiceEstimate(); });
        brand.addActionListener(e -> { refreshModels(); clearSpecification(); scheduleServiceEstimate(); });
        model.addActionListener(e -> { refreshSpecificationSuggestions(); scheduleServiceEstimate(); });
        JPanel contact = fieldGrid();
        contact.add(CrsmUi.labeled("Contact number *", phone));
        contact.add(CrsmUi.labeled("Address", address));
        JPanel appointment = new JPanel(new GridLayout(1, 2, 12, 0)); appointment.setOpaque(false);
        appointment.add(CrsmUi.labeled("Date (YYYY-MM-DD)", appointmentDate));
        appointment.add(CrsmUi.labeled("Time (HH:MM)", appointmentTime));
        contact.add(CrsmUi.labeled("Drop-off appointment", appointment));
        contact.add(CrsmUi.labeled("Appointment type", purpose));
        form.add(CrsmUi.section("Contact & drop-off", "How we can reach you and when you plan to bring in the device.", contact));
        form.add(Box.createVerticalStrut(14));

        JPanel device = fieldGrid();
        device.add(CrsmUi.labeled("Device category *", deviceType));
        device.add(CrsmUi.labeled("Brand *", brand));
        device.add(CrsmUi.labeled("Model *", model));
        device.add(CrsmUi.labeled("Specifications (optional)", specification));
        device.add(CrsmUi.labeled("Serial number (optional)", serial));
        device.add(CrsmUi.labeled("Device condition *", conditionChoices));
        form.add(CrsmUi.section("Your device", "Choose the closest category, brand, and model so the estimate can use matching device data.", device));
        form.add(Box.createVerticalStrut(14));

        JPanel issue = fieldGrid();
        issue.add(CrsmUi.labeled("What is wrong? *", areaScroll(problem)));
        issue.add(CrsmUi.labeled("What would you like us to do?", areaScroll(work)));
        issue.add(CrsmUi.labeled("Items handed in with the device", areaScroll(accessories)));
        form.add(CrsmUi.section("Repair details", "Describe the symptoms and any work you would like us to consider.", issue));
        form.add(Box.createVerticalStrut(14));

        JPanel photosRow = new JPanel(new BorderLayout(4, 4)); photosRow.setOpaque(false);
        JButton attach = CrsmUi.ghost("Add photos (up to 3)"); attach.addActionListener(e -> choosePhotos());
        photosRow.add(attach, BorderLayout.NORTH); photosRow.add(photoNames, BorderLayout.CENTER);
        JPanel servicePanel = new JPanel(new BorderLayout(8, 8)); servicePanel.setOpaque(false);
        JPanel serviceText = new JPanel(); serviceText.setOpaque(false); serviceText.setLayout(new BoxLayout(serviceText, BoxLayout.Y_AXIS));
        selectedService.setFont(CrsmTheme.font(Font.BOLD, 15)); selectedService.setForeground(CrsmTheme.NAVY);
        selectedServiceDescription.setForeground(CrsmTheme.MUTED);
        servicePrice.setFont(CrsmTheme.font(Font.BOLD, 14)); servicePrice.setForeground(CrsmTheme.INDIGO);
        serviceText.add(selectedService); serviceText.add(Box.createVerticalStrut(4)); serviceText.add(selectedServiceDescription);
        serviceText.add(Box.createVerticalStrut(10)); serviceText.add(servicePrice);
        servicePanel.add(serviceText, BorderLayout.CENTER);
        browseServices.addActionListener(e -> onBrowseServices.run());
        servicePanel.add(browseServices, BorderLayout.EAST);
        JLabel estimateNote = CrsmUi.muted("Estimates update dynamically when the service or device details change. Final pricing is confirmed after physical technician inspection.");
        estimateNote.setFont(CrsmTheme.font(Font.PLAIN, 12));
        JPanel estimate = new JPanel(new BorderLayout(0, 10)); estimate.setOpaque(false);
        estimate.add(servicePanel, BorderLayout.CENTER); estimate.add(estimateNote, BorderLayout.SOUTH);
        form.add(CrsmUi.section("Service & estimate", "Select a repair service to calculate a device-aware estimate.", estimate));
        form.add(Box.createVerticalStrut(14));
        form.add(CrsmUi.section("Damage photos", "Optional: attach up to three clear PNG or JPG images (2 MB each).", photosRow));
        JScrollPane scroll = new JScrollPane(form); scroll.setBorder(null); scroll.getViewport().setOpaque(false); scroll.getViewport().setBackground(CrsmTheme.CANVAS);
        add(scroll, BorderLayout.CENTER);
        JButton submit = CrsmUi.primary("Submit repair request"); submit.setPreferredSize(new Dimension(0, 46));
        submit.addActionListener(e -> submit(submit)); add(submit, BorderLayout.SOUTH);
        loadDeviceCatalog();
        installSpecificationAutocomplete();
        specification.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleServiceEstimate(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleServiceEstimate(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { scheduleServiceEstimate(); }
        });
    }

    void selectService(Map<String,Object> serviceRow) {
        selectedServiceId = CustomerWorkflowService.integerId(serviceRow.get("service_id"), "repair_services.service_id");
        selectedServiceName = text(serviceRow.get("service_name"));
        selectedService.setText(selectedServiceName);
        selectedServiceDescription.setText(text(serviceRow.get("description")).isBlank()
                ? "The repair scope will be confirmed by a technician." : text(serviceRow.get("description")));
        work.setText(selectedServiceName);
        estimateTimer.stop();
        refreshServiceEstimate();
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
        String b = selectedItem(brand).toLowerCase();
        String t = selectedDeviceType();
        List<String> models = deviceCatalog.stream().filter(r -> t.equals(String.valueOf(r.get("device_type"))) && b.equals(String.valueOf(r.get("brand")).toLowerCase())).map(r -> String.valueOf(r.get("model"))).distinct().sorted().toList();
        model.removeAllItems(); models.forEach(model::addItem);
        model.setEnabled(!b.isBlank() && !t.isBlank());
        refreshSpecificationSuggestions();
    }

    private void loadDeviceCatalog() {
        new SwingWorker<List<Map<String,Object>>,Void>() {
            protected List<Map<String,Object>> doInBackground() throws Exception { return DeviceCatalogLoader.load(); }
            protected void done() { try { deviceCatalog.clear(); deviceCatalog.addAll(get()); deviceType.removeAllItems();
                    deviceCatalog.stream().map(r->String.valueOf(r.get("device_type"))).distinct().sorted().forEach(deviceType::addItem);
                    refreshBrands(); refreshModels();
                } catch (Exception ex) { JOptionPane.showMessageDialog(CustomerRequestPage.this, ex.getMessage(), "Device catalog unavailable", JOptionPane.ERROR_MESSAGE); } }
        }.execute();
    }
    private void refreshBrands() {
        String t=String.valueOf(deviceType.getSelectedItem());
        List<String> brands=deviceCatalog.stream().filter(r->t.equals(String.valueOf(r.get("device_type")))).map(r->String.valueOf(r.get("brand"))).distinct().toList();
        brand.removeAllItems(); brands.forEach(brand::addItem); brand.setEnabled(!brands.isEmpty());
    }

    String selectedDeviceType() {
        Object value = deviceType.getSelectedItem();
        return value == null ? "" : String.valueOf(value).trim();
    }

    private void clearSpecification() { specification.setText(""); refreshSpecificationSuggestions(); }
    private void refreshSpecificationSuggestions() { /* suggestions are rebuilt by the document listener */ }
    private void installSpecificationAutocomplete() {
        JPopupMenu popup = new JPopupMenu(); JList<String> suggestions = new JList<>(); suggestions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        popup.add(new JScrollPane(suggestions));
        specification.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); } public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); } public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() {
                String t=String.valueOf(deviceType.getSelectedItem()), b=String.valueOf(brand.getSelectedItem()), m=String.valueOf(model.getSelectedItem()), q=specification.getText().trim().toLowerCase();
                List<String> values=deviceCatalog.stream().filter(r->t.equals(String.valueOf(r.get("device_type"))) && b.equals(String.valueOf(r.get("brand"))) && m.equals(String.valueOf(r.get("model")))).map(r->String.valueOf(r.get("specification"))).filter(s->!s.isBlank() && s.toLowerCase().contains(q)).distinct().toList();
                suggestions.setModel(new javax.swing.AbstractListModel<>() { public int getSize(){return values.size();} public String getElementAt(int i){return values.get(i);} });
                if (!values.isEmpty() && specification.isFocusOwner()) { popup.pack(); popup.show(specification, 0, specification.getHeight()); } else popup.setVisible(false);
            }
        });
        suggestions.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && suggestions.getSelectedValue()!=null) { specification.setText(suggestions.getSelectedValue()); popup.setVisible(false); } });
    }
    private void refreshServiceEstimate() {
        int generation = ++estimateGeneration;
        if (selectedServiceId < 0) {
            setServicePrice("No service selected. Staff can recommend one after diagnosis.");
            return;
        }
        String type = selectedDeviceType();
        String make = selectedItem(brand), deviceModel = selectedItem(model), spec = specification.getText().trim();
        if (type.isBlank() || make.isBlank() || deviceModel.isBlank()) {
            setServicePrice("Select the device type, brand, and model to calculate an estimate. Final price is confirmed after inspection.");
            return;
        }
        int serviceId = selectedServiceId;
        setServicePrice("Calculating an estimate for " + make + " " + deviceModel + "…");
        new SwingWorker<Map<String,Object>,Void>() {
            protected Map<String,Object> doInBackground() throws Exception {
                return service.quoteService(session, serviceId, type, make, deviceModel, spec);
            }
            protected void done() {
                if (generation != estimateGeneration) return;
                try {
                    Map<String,Object> quote = get();
                    BigDecimal baseFee = CustomerWorkflowService.moneyValue(quote.get("base_fee"));
                    BigDecimal amount = CustomerWorkflowService.moneyValue(quote.get("amount"));
                    BigDecimal partPrice = CustomerWorkflowService.moneyValue(quote.get("part_price"));
                    String message = text(quote.get("message"));
                    if ("estimated".equalsIgnoreCase(text(quote.get("status"))) && amount != null) {
                        if (partPrice != null && baseFee != null) {
                            String part = text(quote.get("part_name")).isBlank() ? "matched part" : text(quote.get("part_name"));
                            setServicePrice("Current estimate range: " + money(baseFee) + " – " + money(amount)
                                    + " (service fee through service fee + " + part + ").");
                        } else {
                            setServicePrice("Current device-specific estimate: " + money(amount) + ".");
                        }
                    } else {
                        String base = baseFee == null ? "" : "Known service fee: " + money(baseFee) + ". ";
                        setServicePrice(base + (message.isBlank() ? "A complete estimate requires technician inspection." : message));
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    setServicePrice("Pricing could not be confirmed (" + cause.getMessage() + "). Final estimate requires technician inspection.");
                }
            }
        }.execute();
    }

    private void scheduleServiceEstimate() {
        estimateGeneration++;
        if (selectedServiceId >= 0) setServicePrice("Updating the device-aware estimate…");
        estimateTimer.restart();
    }

    private static String selectedItem(JComboBox<String> combo) {
        Object value = combo.getSelectedItem();
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "not available" : "PHP " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void setServicePrice(String message) {
        servicePrice.setText("<html><div style='width:520px'>" + message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</div></html>");
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }

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
        if (phone.getText().isBlank() || deviceType.getSelectedItem()==null || brand.getSelectedItem()==null || model.getSelectedItem()==null || problem.getText().isBlank()) { JOptionPane.showMessageDialog(this, "Enter your contact number, device type, brand, model, and a description of the problem.", "Required information", JOptionPane.WARNING_MESSAGE); return; }
        try { LocalDate.parse(appointmentDate.getText().trim()); java.time.LocalTime.parse(appointmentTime.getText().trim()); }
        catch (RuntimeException ex) { JOptionPane.showMessageDialog(this, "Enter a valid date (YYYY-MM-DD) and time (HH:MM).", "Appointment time", JOptionPane.WARNING_MESSAGE); return; }
        Map<String,Object> values = new LinkedHashMap<>();
        values.put("device_type", deviceType.getSelectedItem()); values.put("brand", String.valueOf(brand.getSelectedItem()).trim());
        values.put("model", String.valueOf(model.getSelectedItem()).trim()); values.put("specification", specification.getText().trim()); values.put("serial", serial.getText().trim());
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
    private static void style(java.awt.Component c) {
        if (c instanceof JComboBox<?> combo) CrsmUi.styleCombo(combo);
        else c.setPreferredSize(new Dimension(220,42));
    }
    private static JPanel fieldGrid() { JPanel panel = new JPanel(new GridLayout(0, 2, 14, 14)); panel.setOpaque(false); return panel; }
    private static void styleArea(JTextArea area) {
        area.setFont(CrsmTheme.font(Font.PLAIN, 13));
        area.setForeground(CrsmTheme.NAVY);
        area.setBackground(CrsmTheme.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        area.setPreferredSize(new Dimension(220, 78));
    }
    private static JScrollPane areaScroll(JTextArea area) {
        JScrollPane scroll=new JScrollPane(area);
        scroll.setBorder(new CrsmUi.RoundedBorder(CrsmTheme.BORDER,12));
        scroll.getViewport().setBackground(CrsmTheme.WHITE);
        area.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { scroll.setBorder(new CrsmUi.RoundedBorder(CrsmTheme.LAVENDER,12)); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { scroll.setBorder(new CrsmUi.RoundedBorder(CrsmTheme.BORDER,12)); }
        });
        return scroll;
    }
    private static final class SubmitResult { final int id; final List<String> failures; final int photoCount; SubmitResult(int id,List<String> failures,int photoCount){this.id=id;this.failures=failures;this.photoCount=photoCount;} }
}