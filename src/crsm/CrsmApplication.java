package crsm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Native Swing sign-in and registration flows. Design PNGs remain references only. */
public class CrsmApplication {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(CrsmApplication.class);
    private static final String REMEMBER_EMAIL_KEY = "remembered_email";
    private static final Color AUTH_CANVAS = new Color(246, 248, 253);
    private static final Color AUTH_BORDER = new Color(218, 225, 239);

    public void showLogin() {
        CrsmTheme.install();
        showAuthPage(false);
    }

    private void showAuthPage(boolean signup) {
        JFrame frame = new JFrame(signup ? "CRSM | Create an Account" : "CRSM | Sign In");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 650));
        frame.setSize(1180, 760);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AUTH_CANVAS);
        root.add(buildBrandPanel(), BorderLayout.WEST);
        JPanel formHost = new JPanel(new GridBagLayout());
        formHost.setBackground(AUTH_CANVAS);
        formHost.setBorder(BorderFactory.createEmptyBorder(34, 34, 34, 34));
        JPanel formCard = signup ? buildSignupForm(frame) : buildSigninForm(frame);
        formCard.setPreferredSize(new Dimension(510, signup ? 650 : 590));
        formHost.add(formCard);
        GridBagConstraints formConstraints = new GridBagConstraints();
        formConstraints.gridx = 1;
        formConstraints.gridy = 0;
        formConstraints.weightx = 0.46;
        formConstraints.weighty = 1;
        formConstraints.fill = GridBagConstraints.BOTH;
        root.add(new JScrollPane(formHost, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private JPanel buildBrandPanel() {
        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBackground(CrsmTheme.NAVY);
        brand.setPreferredSize(new Dimension(390, 0));
        brand.setBorder(BorderFactory.createEmptyBorder(58, 42, 58, 38));

        JLabel mark = new JLabel("CRSM");
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);
        mark.setForeground(CrsmTheme.WHITE);
        mark.setFont(new Font("Segoe UI", Font.BOLD, 48));
        JLabel name = new JLabel("COMPUTER REPAIR SERVICE MANAGEMENT");
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        name.setForeground(CrsmTheme.WHITE);
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));
        name.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        JPanel accent = new JPanel();
        accent.setAlignmentX(Component.LEFT_ALIGNMENT);
        accent.setBackground(CrsmTheme.PEACH);
        accent.setMaximumSize(new Dimension(76, 5));
        accent.setPreferredSize(new Dimension(76, 5));
        JLabel tagline = new JLabel("Manage Devices. Deliver Solutions.");
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagline.setForeground(new Color(220, 226, 246));
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        JLabel description = new JLabel("Appointments, repair requests, and service updates in one place.");
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setForeground(new Color(193, 203, 230));
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        brand.add(mark);
        brand.add(name);
        brand.add(Box.createVerticalStrut(24));
        brand.add(accent);
        brand.add(Box.createVerticalStrut(28));
        brand.add(tagline);
        brand.add(Box.createVerticalStrut(10));
        brand.add(description);
        brand.add(Box.createVerticalGlue());
        JLabel replaceableAsset = new JLabel("Logo and workshop artwork can be added here later.");
        replaceableAsset.setAlignmentX(Component.LEFT_ALIGNMENT);
        replaceableAsset.setForeground(new Color(166, 180, 215));
        replaceableAsset.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        brand.add(replaceableAsset);
        return brand;
    }

    private JPanel buildSigninForm(JFrame frame) {
        JPanel card = formCard();
        JLabel title = heading("Welcome Back");
        JLabel subtitle = subtitle("Sign in to your account to continue.");
        JTextField email = new JTextField(PREFERENCES.get(REMEMBER_EMAIL_KEY, ""));
        styleField(email);
        JPasswordField password = new JPasswordField();
        styleField(password);
        JCheckBox remember = new JCheckBox("Remember my email on this device");
        remember.setOpaque(false);
        remember.setSelected(!email.getText().isBlank());
        JButton showPassword = linkButton("Show");
        showPassword.addActionListener(e -> togglePassword(password, showPassword));

        JButton forgot = linkButton("Forgot password?");
        JButton signIn = authPrimaryButton("Sign In");
        JButton switchSignup = secondaryButton("Create a Customer account");

        addFormHeader(card, title, subtitle);
        addField(card, "Email Address", email, 0);
        JPanel passwordField = new JPanel(new BorderLayout(8, 0));
        passwordField.setOpaque(false); passwordField.add(password, BorderLayout.CENTER); passwordField.add(showPassword, BorderLayout.EAST);
        addField(card, "Password", passwordField, 1);
        JPanel options = new JPanel(new BorderLayout()); options.setOpaque(false);
        options.add(remember, BorderLayout.WEST); options.add(forgot, BorderLayout.EAST);
        addRow(card, options, 2, new Insets(4, 0, 12, 0));
        addRow(card, signIn, 3, new Insets(0, 0, 10, 0));
        addRow(card, separator("OR"), 4, new Insets(5, 0, 5, 0));
        addRow(card, switchSignup, 5, new Insets(4, 0, 0, 0));

        signIn.addActionListener(e -> {
            if (email.getText().isBlank() || password.getPassword().length == 0) {
                JOptionPane.showMessageDialog(frame, "Enter your email address and password.", "Sign in", JOptionPane.WARNING_MESSAGE);
                return;
            }
            signIn.setEnabled(false);
            new SwingWorker<UserSession, Void>() {
                @Override protected UserSession doInBackground() throws Exception {
                    return new SupabaseAuthService().signIn(email.getText().trim(), new String(password.getPassword()));
                }
                @Override protected void done() {
                    signIn.setEnabled(true);
                    try {
                        UserSession session = get();
                        if (remember.isSelected()) PREFERENCES.put(REMEMBER_EMAIL_KEY, email.getText().trim());
                        else PREFERENCES.remove(REMEMBER_EMAIL_KEY);
                        frame.dispose(); new CrsmWindow(session).setVisible(true);
                    } catch (Exception ex) { showError(frame, ex); }
                }
            }.execute();
        });
        email.addActionListener(e -> password.requestFocusInWindow());
        password.addActionListener(e -> signIn.doClick());
        switchSignup.addActionListener(e -> { frame.dispose(); showAuthPage(true); });
        forgot.addActionListener(e -> {
            String address = email.getText().trim();
            if (address.isBlank()) address = JOptionPane.showInputDialog(frame, "Enter the email address on your account:", "Password reset", JOptionPane.QUESTION_MESSAGE);
            if (address == null || address.isBlank()) return;
            String resetAddress = address.trim(); forgot.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception { new SupabaseAuthService().sendPasswordReset(resetAddress); return null; }
                @Override protected void done() {
                    forgot.setEnabled(true);
                    try { get(); JOptionPane.showMessageDialog(frame, "If the address is registered, Supabase Auth will send password reset instructions."); }
                    catch (Exception ex) { showError(frame, ex); }
                }
            }.execute();
        });
        return card;
    }

    private JPanel buildSignupForm(JFrame frame) {
        JPanel card = formCard();
        JLabel title = heading("Create an Account");
        JLabel subtitle = subtitle("Sign up as a customer. An administrator may change account access later.");
        JTextField fullName = new JTextField(); styleField(fullName);
        JTextField username = new JTextField(); styleField(username);
        JTextField email = new JTextField(); styleField(email);
        JPasswordField password = new JPasswordField(); styleField(password);
        JPasswordField confirm = new JPasswordField(); styleField(confirm);
        char passwordEcho = password.getEchoChar();
        JCheckBox showPasswords = new JCheckBox("Show passwords"); showPasswords.setOpaque(false);
        showPasswords.addActionListener(e -> {
            char echo = showPasswords.isSelected() ? (char) 0 : passwordEcho;
            password.setEchoChar(echo); confirm.setEchoChar(echo);
        });
        JCheckBox terms = new JCheckBox("I agree to the Terms of Service and Privacy Policy"); terms.setOpaque(false);
        JButton submit = authPrimaryButton("Sign Up");
        JButton switchSignin = secondaryButton("Already have an account?  Sign in");

        addFormHeader(card, title, subtitle);
        addField(card, "Full Name", fullName, 0);
        addField(card, "Username", username, 1);
        addField(card, "Email Address", email, 2);
        addField(card, "Password", password, 3);
        addField(card, "Confirm Password", confirm, 4);
        addRow(card, showPasswords, 5, new Insets(1, 0, 0, 0));
        addRow(card, terms, 6, new Insets(4, 0, 4, 0));
        addRow(card, submit, 7, new Insets(2, 0, 8, 0));
        addRow(card, separator("OR"), 8, new Insets(3, 0, 3, 0));
        addRow(card, switchSignin, 9, new Insets(2, 0, 0, 0));

        submit.addActionListener(e -> {
            String name = fullName.getText().trim(), handle = username.getText().trim(), address = email.getText().trim();
            String secret = new String(password.getPassword()), repeat = new String(confirm.getPassword());
            if (name.isBlank() || handle.isBlank() || address.isBlank() || secret.isBlank() || repeat.isBlank()) {
                JOptionPane.showMessageDialog(frame, "Complete all required fields.", "Create an account", JOptionPane.WARNING_MESSAGE); return;
            }
            if (!address.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                JOptionPane.showMessageDialog(frame, "Enter a valid email address.", "Create an account", JOptionPane.WARNING_MESSAGE); return;
            }
            if (!secret.equals(repeat)) { JOptionPane.showMessageDialog(frame, "The passwords do not match.", "Create an account", JOptionPane.WARNING_MESSAGE); return; }
            if (!terms.isSelected()) { JOptionPane.showMessageDialog(frame, "Accept the Terms of Service and Privacy Policy to continue.", "Create an account", JOptionPane.WARNING_MESSAGE); return; }
            submit.setEnabled(false);
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception { return new SupabaseAuthService().signUp(name, handle, address, secret); }
                @Override protected void done() {
                    submit.setEnabled(true);
                    try {
                        boolean signedIn = get();
                        if (signedIn) {
                            UserSession session = new SupabaseAuthService().signIn(address, secret);
                            frame.dispose(); new CrsmWindow(session).setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(frame, "Account created as Customer. Check your email to confirm the account, then sign in.", "Account created", JOptionPane.INFORMATION_MESSAGE);
                            frame.dispose(); showAuthPage(false);
                        }
                    } catch (Exception ex) { showError(frame, ex); }
                }
            }.execute();
        });
        switchSignin.addActionListener(e -> { frame.dispose(); showAuthPage(false); });
        return card;
    }

    private JPanel formCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CrsmTheme.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AUTH_BORDER), BorderFactory.createEmptyBorder(30, 34, 30, 34)));
        return card;
    }

    private void addFormHeader(JPanel card, JLabel title, JLabel subtitle) {
        JPanel header = new JPanel(); header.setOpaque(false); header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel logoText = new JLabel("CRSM  |  COMPUTER REPAIR SERVICE MANAGEMENT");
        logoText.setForeground(CrsmTheme.INDIGO); logoText.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JPanel accent = new JPanel(); accent.setBackground(CrsmTheme.PEACH); accent.setMaximumSize(new Dimension(52, 4)); accent.setPreferredSize(new Dimension(52, 4)); accent.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(logoText); header.add(Box.createVerticalStrut(14)); header.add(accent); header.add(Box.createVerticalStrut(17)); header.add(title); header.add(Box.createVerticalStrut(5)); header.add(subtitle);
        GridBagConstraints c = constraints(0); c.gridwidth = 2; c.insets = new Insets(0, 0, 24, 0); card.add(header, c);
    }

    private void addField(JPanel card, String label, Component input, int row) {
        JPanel block = new JPanel(new BorderLayout(0, 6)); block.setOpaque(false);
        JLabel labelComponent = new JLabel(label); labelComponent.setForeground(CrsmTheme.NAVY); labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        block.add(labelComponent, BorderLayout.NORTH); block.add(input, BorderLayout.CENTER);
        GridBagConstraints c = constraints(row + 1); c.gridwidth = 2; c.insets = new Insets(0, 0, 13, 0); card.add(block, c);
    }

    private void addRow(JPanel card, Component component, int row, Insets insets) {
        GridBagConstraints c = constraints(row + 1); c.gridwidth = 2; c.insets = insets; card.add(component, c);
    }

    private static GridBagConstraints constraints(int row) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST; c.insets = new Insets(0, 0, 0, 0); return c;
    }

    private JLabel heading(String text) {
        JLabel label = new JLabel(text); label.setForeground(CrsmTheme.NAVY); label.setFont(new Font("Segoe UI", Font.BOLD, 26)); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }

    private JLabel subtitle(String text) {
        JLabel label = new JLabel("<html><div style='width:400px'>" + text + "</div></html>"); label.setForeground(CrsmTheme.LAVENDER); label.setFont(new Font("Segoe UI", Font.PLAIN, 13)); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }

    private static JPanel separator(String text) {
        JPanel panel = new JPanel(new java.awt.GridBagLayout()); panel.setOpaque(false);
        JSeparator left = new JSeparator(); left.setForeground(AUTH_BORDER);
        JSeparator right = new JSeparator(); right.setForeground(AUTH_BORDER);
        JLabel label = new JLabel(text, SwingConstants.CENTER); label.setForeground(CrsmTheme.LAVENDER);
        GridBagConstraints c = new GridBagConstraints(); c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0; c.weightx = 1; panel.add(left, c);
        c.gridx = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(0, 0, 0, 10); panel.add(label, c);
        c.gridx = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 0, 0); panel.add(right, c);
        return panel;
    }

    private static void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14)); field.setForeground(CrsmTheme.TEXT); field.setBackground(CrsmTheme.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AUTH_BORDER), BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        field.setPreferredSize(new Dimension(100, 43));
    }

    private static JButton authPrimaryButton(String text) {
        JButton button = new JButton(text); button.setFont(new Font("Segoe UI", Font.BOLD, 15)); button.setForeground(CrsmTheme.NAVY); button.setBackground(CrsmTheme.PEACH);
        button.setOpaque(true); button.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18)); button.setFocusPainted(false); button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 48)); return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text); button.setForeground(CrsmTheme.INDIGO); button.setBackground(CrsmTheme.WHITE); button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AUTH_BORDER), BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        button.setFocusPainted(false); button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)); return button;
    }

    private static JButton linkButton(String text) {
        JButton button = new JButton(text); button.setForeground(CrsmTheme.INDIGO); button.setBackground(CrsmTheme.WHITE); button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        button.setFocusPainted(false); button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)); return button;
    }

    private static void togglePassword(JPasswordField password, JButton button) {
        boolean reveal = password.getEchoChar() != 0;
        password.setEchoChar(reveal ? (char) 0 : '\u2022'); button.setText(reveal ? "Hide" : "Show");
    }

    static void stylePrimary(JButton button) {
        button.setForeground(CrsmTheme.WHITE); button.setBackground(CrsmTheme.INDIGO); button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15)); button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    static void showError(Component parent, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(parent, cause.getMessage(), "Unable to continue", JOptionPane.ERROR_MESSAGE);
    }

    static File chooseImage(Component parent) {
        JFileChooser chooser = new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter("PNG or JPEG images", "png", "jpg", "jpeg"));
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }
}
