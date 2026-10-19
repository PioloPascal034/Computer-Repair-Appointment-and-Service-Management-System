package crsm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Entry UI. Authentication remains an explicit integration gap in the current schema. */
public class CrsmApplication {
    public void showLogin() {
        CrsmTheme.install();
        JFrame frame = new JFrame("CRSM | Computer Repair Service Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setSize(1240, 780);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new java.awt.GridLayout(1, 2));
        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(CrsmTheme.NAVY);
        brand.setBorder(BorderFactory.createEmptyBorder(64, 54, 54, 54));
        JLabel logo = new JLabel("CRSM", SwingConstants.CENTER);
        logo.setForeground(CrsmTheme.WHITE);
        logo.setFont(new Font("Raleway", Font.BOLD, 48));
        JLabel tagline = new JLabel("COMPUTER REPAIR SERVICE MANAGEMENT", SwingConstants.CENTER);
        tagline.setForeground(CrsmTheme.WHITE);
        tagline.setFont(new Font("Raleway", Font.BOLD, 18));
        JLabel promise = new JLabel("Manage devices. Deliver solutions.", SwingConstants.CENTER);
        promise.setForeground(CrsmTheme.PEACH);
        promise.setFont(new Font("Raleway", Font.PLAIN, 19));
        JPanel brandCenter = new JPanel(new BorderLayout(8, 12));
        brandCenter.setOpaque(false);
        brandCenter.add(logo, BorderLayout.NORTH);
        brandCenter.add(tagline, BorderLayout.CENTER);
        brandCenter.add(promise, BorderLayout.SOUTH);
        brand.add(brandCenter, BorderLayout.CENTER);

        JPanel formSide = new JPanel(new GridBagLayout());
        formSide.setBackground(CrsmTheme.CANVAS);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CrsmTheme.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(CrsmTheme.BORDER), BorderFactory.createEmptyBorder(34, 38, 34, 38)));
        form.setPreferredSize(new Dimension(440, 510));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(8, 0, 8, 0);
        JLabel title = new JLabel("Welcome Back", SwingConstants.CENTER);
        title.setFont(new Font("Raleway", Font.BOLD, 30)); title.setForeground(CrsmTheme.NAVY); form.add(title, c);
        c.gridy++;
        JLabel subtitle = new JLabel("Sign in to your account to continue", SwingConstants.CENTER);
        subtitle.setForeground(CrsmTheme.LAVENDER); form.add(subtitle, c);
        c.gridwidth = 1; c.gridy++; form.add(new JLabel("Username"), c);
        JTextField username = new JTextField(); c.gridx = 1; username.setColumns(20); form.add(username, c);
        c.gridx = 0; c.gridy++; form.add(new JLabel("Password"), c);
        JPasswordField password = new JPasswordField(); c.gridx = 1; form.add(password, c);
        c.gridx = 0; c.gridy++; form.add(new JLabel("Preview role"), c);
        JComboBox<String> role = new JComboBox<>(new String[]{"Customer", "Staff", "Admin"}); c.gridx = 1; form.add(role, c);
        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        JLabel warning = new JLabel("Role preview only. The project schema has no authentication or user-role tables.");
        warning.setForeground(CrsmTheme.INDIGO); warning.setFont(warning.getFont().deriveFont(11f)); form.add(warning, c);
        c.gridy++;
        JButton signIn = new JButton("Open UI Preview"); stylePrimary(signIn); form.add(signIn, c);
        c.gridy++;
        JButton signUp = new JButton("Create Customer Account"); styleSecondary(signUp); form.add(signUp, c);
        signUp.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                "Customer account creation is specified in the UCD, but no account/authentication table or approved auth provider is present in the current schema.",
                "Authentication requirement", JOptionPane.INFORMATION_MESSAGE));
        signIn.addActionListener(e -> {
            if (username.getText().isBlank() || password.getPassword().length == 0) {
                JOptionPane.showMessageDialog(frame, "Enter a username and password to open the UI preview.", "Required fields", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String selected = (String) role.getSelectedItem();
            frame.dispose();
            new CrsmWindow(selected).setVisible(true);
        });
        formSide.add(form);
        root.add(brand); root.add(formSide);
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    static void stylePrimary(JButton button) {
        button.setBackground(CrsmTheme.PEACH); button.setForeground(CrsmTheme.NAVY);
        button.setFocusPainted(false); button.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
    }
    static void styleSecondary(JButton button) {
        button.setBackground(CrsmTheme.WHITE); button.setForeground(CrsmTheme.INDIGO);
        button.setFocusPainted(false); button.setBorder(BorderFactory.createLineBorder(CrsmTheme.LAVENDER));
    }
}
