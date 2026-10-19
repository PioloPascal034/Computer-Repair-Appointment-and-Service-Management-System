import javax.swing.*;
import java.awt.*;

public class RepairRequestFrame extends JFrame {
    public RepairRequestFrame() {
        setTitle("Repair Requests");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel heading = new JLabel("Repair Requests");
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(heading, BorderLayout.NORTH);

        String[] columns = {
            "Request ID", "Computer", "Technician", "Service",
            "Problem", "Status", "Est. Cost", "Final Cost"
        };

        Object[][] data = {
            {"RR-001", "Laptop - Dell", "Mark Santos", "Hardware Diagnostics",
             "Not turning on", "Under Diagnosis", "300.00", "-"},
            {"RR-002", "Desktop - HP", "Alex Cruz", "Screen Replacement",
             "No display", "Under Repair", "3500.00", "-"},
            {"RR-003", "Laptop - Acer", "Mark Santos", "OS Installation",
             "Slow system", "Ready for Pickup", "800.00", "800.00"}
        };

        JTable table = new JTable(data, columns);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("New Repair Request");
        JButton update = new JButton("Update Status");
        JButton view = new JButton("View Details");

        add.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Prototype only: repair request form will be connected to Supabase later."));
        update.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Prototype only: status update will be connected to Supabase later."));
        view.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Repair details screen will be added during database integration."));

        bottom.add(add);
        bottom.add(update);
        bottom.add(view);
        panel.add(bottom, BorderLayout.SOUTH);

        add(panel);
    }
}
