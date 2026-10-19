import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {
    public DashboardFrame() {
        setTitle("Computer Repair Appointment and Service Management System");
        setSize(1050, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        main.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));
        cards.add(card("Appointments", "8"));
        cards.add(card("Repair Requests", "12"));
        cards.add(card("Technicians", "5"));
        cards.add(card("Pending Repairs", "6"));

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(cards, BorderLayout.NORTH);

        String[] columns = {"Request ID", "Customer", "Computer", "Technician", "Status"};
        Object[][] data = {
            {"RR-001", "Juan Dela Cruz", "Laptop", "Mark Santos", "Under Repair"},
            {"RR-002", "Maria Santos", "Desktop", "Alex Cruz", "Under Diagnosis"},
            {"RR-003", "Pedro Reyes", "Laptop", "Mark Santos", "Waiting for Parts"},
            {"RR-004", "Ana Garcia", "Desktop", "Alex Cruz", "Ready for Pickup"}
        };
        center.add(new JScrollPane(new JTable(data, columns)), BorderLayout.CENTER);

        main.add(center, BorderLayout.CENTER);

        JPanel menu = new JPanel(new GridLayout(2, 4, 8, 8));
        addButton(menu, "Customers", () -> new DataFrame("Customers").setVisible(true));
        addButton(menu, "Computers", () -> new DataFrame("Computers").setVisible(true));
        addButton(menu, "Appointments", () -> new DataFrame("Appointments").setVisible(true));
        addButton(menu, "Repair Requests", () -> new RepairRequestFrame().setVisible(true));
        addButton(menu, "Technicians", () -> new DataFrame("Technicians").setVisible(true));
        addButton(menu, "Repair Services", () -> new DataFrame("Repair Services").setVisible(true));
        addButton(menu, "Payments", () -> new DataFrame("Payments").setVisible(true));
        addButton(menu, "Logout", () -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        main.add(menu, BorderLayout.SOUTH);
        add(main);
    }

    private JPanel card(String name, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(name));
        JLabel label = new JLabel(value, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 30));
        p.add(label, BorderLayout.CENTER);
        return p;
    }

    private void addButton(JPanel panel, String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        panel.add(b);
    }
}
