package crsm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Lightweight chart placeholders used by the dashboard until chart rendering is added. */
final class CrsmCharts {
    private CrsmCharts() {}
    static JPanel bars(String title, Map<?, ?> values) { return panel(title, values); }
    static JPanel line(String title, Map<?, ?> values) { return panel(title, values); }
    static JPanel pie(String title, Map<?, ?> values) { return panel(title, values); }
    private static JPanel panel(String title, Map<?, ?> values) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(260, 150));
        panel.add(new JLabel(title + (values == null || values.isEmpty() ? "" : " (" + values.size() + ")")), BorderLayout.NORTH);
        return panel;
    }
}
