import javax.swing.*;
import java.awt.*;

public class DataFrame extends JFrame {
    public DataFrame(String title) {
        setTitle(title);
        setSize(900, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(heading, BorderLayout.NORTH);

        String[] columns = {"ID", "Name", "Contact", "Status"};
        Object[][] data = {
            {"001", "Juan Dela Cruz", "09123456789", "Active"},
            {"002", "Maria Santos", "09987654321", "Active"},
            {"003", "Pedro Reyes", "09001112233", "Active"},
            {"004", "Ana Garcia", "09887776655", "Active"}
        };

        panel.add(new JScrollPane(new JTable(data, columns)), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(new JButton("Add"));
        buttons.add(new JButton("Edit"));
        buttons.add(new JButton("Delete"));
        buttons.add(new JButton("Refresh"));
        panel.add(buttons, BorderLayout.SOUTH);

        add(panel);
    }
}
