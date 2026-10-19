import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    public LoginFrame() {
        setTitle("Computer Repair System - Login");
        setSize(420, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Computer Repair Service", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JButton login = new JButton("Login");

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        panel.add(title, c);
        c.gridwidth = 1;
        c.gridx = 0; c.gridy++;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        panel.add(username, c);
        c.gridx = 0; c.gridy++;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        panel.add(password, c);
        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        panel.add(login, c);

        login.addActionListener(e -> {
            dispose();
            new DashboardFrame().setVisible(true);
        });

        add(panel);
    }
}
