package crsm;

import java.awt.BorderLayout;
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

/** Native Swing sign-in and registration. Design PNGs remain visual references only. */
public class CrsmApplication {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(CrsmApplication.class);
    static final String REMEMBER_EMAIL_KEY = "remembered_email";
    static final String PAGE_SIZE_KEY = "items_per_page";
    static final String DEFAULT_PAGE_KEY = "default_module";
    static final String AUTO_LOGOUT_KEY = "auto_logout_minutes";
    static final String DATE_FORMAT_KEY = "date_format";
    static final String TIME_FORMAT_KEY = "time_format";

    public void showLogin() {
        CrsmTheme.install();
        showAuthPage(false);
    }

    private void showAuthPage(boolean signup) {
        JFrame frame = new JFrame(signup ? "CRSM | Create an Account" : "CRSM | Sign In");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980, 680));
        frame.setSize(1280, 800);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CrsmTheme.NAVY);
        root.add(CrsmUi.workshopScene(), BorderLayout.WEST);

        JPanel formHost = new JPanel(new GridBagLayout());
        formHost.setBackground(CrsmTheme.WHITE);
        formHost.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        JPanel formCard = signup ? buildSignupForm(frame) : buildSigninForm(frame);
        formCard.setPreferredSize(new Dimension(520, signup ? 680 : 560));
        formHost.add(formCard);
        root.add(new JScrollPane(formHost, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private JPanel buildSigninForm(JFrame frame) {
        JPanel card = formCard();
        addFormHeader(card, "Welcome Back!", "Sign in to your account to continue.");
        JTextField email = new JTextField(PREFERENCES.get(REMEMBER_EMAIL_KEY, ""));
        CrsmUi.styleField(email);
        email.putClientProperty("JTextField.placeholderText", "Enter your email address");
        JPasswordField password = new JPasswordField();
        JCheckBox remember = new JCheckBox("Remember me");
        remember.setOpaque(false);
        remember.setForeground(CrsmTheme.NAVY);
        remember.setSelected(!email.getText().isBlank());
        JButton forgot = CrsmUi.link("Forgot Password?");
        JButton signIn = CrsmUi.primary("Sign In");
        signIn.setPreferredSize(new Dimension(100, 46));
        JButton switchSignup = CrsmUi.ghost("Don't have an account?  Sign up");

        addField(card, "Email Address", email, 0);
        addField(card, "Password", CrsmUi.passwordRow(password), 1);
        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        options.add(remember, BorderLayout.WEST);
        options.add(forgot, BorderLayout.EAST);
        addRow(card, options, 2, new Insets(2, 0, 12, 0));
        addRow(card, signIn, 3, new Insets(0, 0, 10, 0));
        addRow(card, orSeparator(), 4, new Insets(4, 0, 8, 0));
        addRow(card, switchSignup, 5, new Insets(0, 0, 0, 0));
        JLabel authNote = CrsmUi.muted("Sign-in uses the email from registration. Username is stored on the profile and is not an Auth login.");
        addRow(card, authNote, 6, new Insets(14, 0, 0, 0));

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
                        frame.dispose();
                        new CrsmWindow(session).setVisible(true);
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
            String resetAddress = address.trim();
            forgot.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception { new SupabaseAuthService().sendPasswordReset(resetAddress); return null; }
                @Override protected void done() {
                    forgot.setEnabled(true);
                    try {
                        get();
                        JOptionPane.showMessageDialog(frame, "If the address is registered, Supabase Auth will send password reset instructions.");
                    } catch (Exception ex) { showError(frame, ex); }
                }
            }.execute();
        });
        return card;
    }

    private JPanel buildSignupForm(JFrame frame) {
        JPanel card = formCard();
        addFormHeader(card, "Create an Account", "Join now to get started with your computer repair service management.");
        JTextField fullName = new JTextField(); CrsmUi.styleField(fullName);
        JTextField username = new JTextField(); CrsmUi.styleField(username);
        JTextField email = new JTextField(); CrsmUi.styleField(email);
        JPasswordField password = new JPasswordField();
        JPasswordField confirm = new JPasswordField();
        JCheckBox terms = new JCheckBox("I agree to the Terms of Service and Privacy Policy.");
        terms.setOpaque(false);
        terms.setForeground(CrsmTheme.NAVY);
        JButton submit = CrsmUi.primary("Sign Up");
        submit.setPreferredSize(new Dimension(100, 46));
        JButton switchSignin = CrsmUi.ghost("Already have an account?  Sign in");

        addField(card, "Full Name", fullName, 0);
        addField(card, "Username", username, 1);
        addField(card, "Email Address", email, 2);
        addField(card, "Password", CrsmUi.passwordRow(password), 3);
        addField(card, "Confirm Password", CrsmUi.passwordRow(confirm), 4);
        addRow(card, terms, 5, new Insets(4, 0, 8, 0));
        addRow(card, submit, 6, new Insets(0, 0, 8, 0));
        addRow(card, orSeparator(), 7, new Insets(2, 0, 8, 0));
        addRow(card, switchSignin, 8, new Insets(0, 0, 0, 0));
        addRow(card, CrsmUi.muted("New accounts are created as Customer. There is no role selector. Only an administrator can promote an account."), 9, new Insets(12, 0, 0, 0));

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
            if (secret.length() < 8) { JOptionPane.showMessageDialog(frame, "Use a password with at least 8 characters.", "Create an account", JOptionPane.WARNING_MESSAGE); return; }
            if (!terms.isSelected()) {
                JOptionPane.showMessageDialog(frame, "No approved Terms of Service document is in the project files. Check the box to acknowledge that you want to continue signup.", "Create an account", JOptionPane.WARNING_MESSAGE);
                return;
            }
            submit.setEnabled(false);
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception { return new SupabaseAuthService().signUp(name, handle, address, secret); }
                @Override protected void done() {
                    submit.setEnabled(true);
                    try {
                        boolean signedIn = get();
                        if (signedIn) {
                            UserSession session = new SupabaseAuthService().signIn(address, secret);
                            frame.dispose();
                            new CrsmWindow(session).setVisible(true);
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
        card.setBorder(BorderFactory.createCompoundBorder(
                new CrsmUi.RoundedBorder(CrsmTheme.LAVENDER, 18),
                BorderFactory.createEmptyBorder(28, 32, 28, 32)));
        return card;
    }

    private void addFormHeader(JPanel card, String title, String subtitle) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.add(CrsmUi.logoMark(42, false));
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        JLabel mark = new JLabel("CRSM");
        mark.setFont(CrsmTheme.font(Font.BOLD, 28));
        mark.setForeground(CrsmTheme.NAVY);
        JLabel name = new JLabel("COMPUTER REPAIR SERVICE MANAGEMENT");
        name.setFont(CrsmTheme.font(Font.BOLD, 11));
        name.setForeground(CrsmTheme.NAVY);
        names.add(mark); names.add(name);
        brand.add(Box.createHorizontalStrut(8));
        brand.add(names);
        JPanel accent = new JPanel();
        accent.setBackground(CrsmTheme.PEACH);
        accent.setMaximumSize(new Dimension(48, 4));
        accent.setPreferredSize(new Dimension(48, 4));
        accent.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel heading = new JLabel(title);
        heading.setFont(CrsmTheme.font(Font.BOLD, 26));
        heading.setForeground(CrsmTheme.NAVY);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel(subtitle);
        sub.setFont(CrsmTheme.font(Font.PLAIN, 13));
        sub.setForeground(CrsmTheme.LAVENDER);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(brand);
        header.add(Box.createVerticalStrut(12));
        header.add(accent);
        header.add(Box.createVerticalStrut(16));
        header.add(heading);
        header.add(Box.createVerticalStrut(6));
        header.add(sub);
        GridBagConstraints c = constraints(0);
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 22, 0);
        card.add(header, c);
    }

    private void addField(JPanel card, String label, Component input, int row) {
        GridBagConstraints c = constraints(row + 1);
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 12, 0);
        card.add(CrsmUi.labeled(label, (javax.swing.JComponent) input), c);
    }

    private void addRow(JPanel card, Component component, int row, Insets insets) {
        GridBagConstraints c = constraints(row + 1);
        c.gridwidth = 2;
        c.insets = insets;
        card.add(component, c);
    }

    private static GridBagConstraints constraints(int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }

    private static JPanel orSeparator() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        JSeparator left = new JSeparator(); left.setForeground(CrsmTheme.LAVENDER);
        JSeparator right = new JSeparator(); right.setForeground(CrsmTheme.LAVENDER);
        JLabel label = new JLabel("OR", SwingConstants.CENTER);
        label.setForeground(CrsmTheme.LAVENDER);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0; c.weightx = 1; panel.add(left, c);
        c.gridx = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE; panel.add(label, c);
        c.gridx = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 10, 0, 0); panel.add(right, c);
        return panel;
    }

    static void showError(Component parent, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(parent, cause.getMessage(), "Unable to continue", JOptionPane.ERROR_MESSAGE);
    }

    static File chooseImage(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG or JPEG images", "png", "jpg", "jpeg"));
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    static int pageSize() {
        return Math.max(5, PREFERENCES.getInt(PAGE_SIZE_KEY, 10));
    }
}
