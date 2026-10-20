package crsm;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

/** Lightweight charts for values returned by the configured database. */
final class CrsmCharts {
    private static final Color[] COLORS = {
            CrsmTheme.INDIGO, CrsmTheme.SUCCESS, CrsmTheme.WARNING,
            new Color(139, 101, 201), new Color(49, 161, 177), new Color(213, 90, 90)
    };

    private CrsmCharts() {}

    static JPanel bars(String title, Map<?, ?> values) {
        return chart(title, values, ChartType.BARS);
    }

    static JPanel line(String title, Map<?, ?> values) {
        return chart(title, values, ChartType.LINE);
    }

    static JPanel pie(String title, Map<?, ?> values) {
        return chart(title, values, ChartType.PIE);
    }

    private static JPanel chart(String title, Map<?, ?> values, ChartType type) {
        JPanel panel = CrsmUi.card();
        panel.setLayout(new BorderLayout(0, 10));
        panel.add(CrsmUi.heading(title, 15), BorderLayout.NORTH);
        panel.add(new Plot(values, type), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(340, 210));
        return panel;
    }

    private enum ChartType { BARS, LINE, PIE }

    private static final class Plot extends JPanel {
        private final Map<String, Double> values = new LinkedHashMap<>();
        private final ChartType type;

        Plot(Map<?, ?> source, ChartType type) {
            this.type = type;
            setOpaque(false);
            if (source != null) source.forEach((key, value) -> {
                if (key == null || value == null) return;
                try {
                    double number = value instanceof Number n ? n.doubleValue() : Double.parseDouble(value.toString());
                    if (Double.isFinite(number) && number >= 0) values.put(String.valueOf(key), number);
                } catch (NumberFormatException ignored) {
                    // Skip values that cannot be charted; the underlying table/report still shows the raw data.
                }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (values.isEmpty()) {
                g.setColor(CrsmTheme.MUTED);
                g.drawString("No records to chart", 12, Math.max(20, getHeight() / 2));
                g.dispose();
                return;
            }
            if (type == ChartType.PIE) drawPie(g);
            else drawAxes(g);
            g.dispose();
        }

        private void drawAxes(Graphics2D g) {
            int left = 38, right = 10, top = 12, bottom = 34;
            int width = Math.max(1, getWidth() - left - right);
            int height = Math.max(1, getHeight() - top - bottom);
            double maximum = values.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            if (maximum == 0) maximum = 1;
            g.setColor(CrsmTheme.BORDER);
            g.drawLine(left, top, left, top + height);
            g.drawLine(left, top + height, left + width, top + height);
            int count = values.size();
            int slot = Math.max(1, width / count);
            int index = 0;
            Path2D line = new Path2D.Double();
            for (Map.Entry<String, Double> entry : values.entrySet()) {
                int centerX = left + index * slot + slot / 2;
                int barHeight = (int) Math.round(entry.getValue() / maximum * Math.max(1, height - 4));
                int y = top + height - barHeight;
                if (type == ChartType.BARS) {
                    int barWidth = Math.max(8, Math.min(36, slot - 8));
                    g.setColor(COLORS[index % COLORS.length]);
                    g.fillRoundRect(centerX - barWidth / 2, y, barWidth, barHeight, 7, 7);
                } else {
                    if (index == 0) line.moveTo(centerX, y);
                    else line.lineTo(centerX, y);
                    g.setColor(COLORS[0]);
                    g.fillOval(centerX - 4, y - 4, 8, 8);
                }
                g.setColor(CrsmTheme.MUTED);
                String label = entry.getKey();
                FontMetrics metrics = g.getFontMetrics();
                if (label.length() > 10) label = label.substring(0, 9) + "…";
                g.drawString(label, centerX - metrics.stringWidth(label) / 2, top + height + 17);
                index++;
            }
            if (type == ChartType.LINE) {
                g.setColor(COLORS[0]);
                g.setStroke(new BasicStroke(2f));
                g.draw(line);
            }
        }

        private void drawPie(Graphics2D g) {
            double total = values.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total <= 0) {
                g.setColor(CrsmTheme.MUTED);
                g.drawString("No values to chart", 12, Math.max(20, getHeight() / 2));
                return;
            }
            int diameter = Math.min(getHeight() - 16, Math.max(60, getWidth() / 3));
            int x = 8, y = Math.max(8, (getHeight() - diameter) / 2);
            int start = 0, index = 0;
            List<String> labels = new ArrayList<>(values.keySet());
            for (String label : labels) {
                int angle = (int) Math.round(values.get(label) / total * 360);
                g.setColor(COLORS[index % COLORS.length]);
                g.fillArc(x, y, diameter, diameter, start, angle);
                start += angle;
                index++;
            }
            int legendX = x + diameter + 14;
            int legendY = y + 12;
            index = 0;
            for (String label : labels) {
                if (legendY > getHeight() - 8) break;
                g.setColor(COLORS[index % COLORS.length]);
                g.fillRoundRect(legendX, legendY - 9, 9, 9, 3, 3);
                g.setColor(CrsmTheme.TEXT);
                String text = label + "  " + values.get(label).longValue();
                if (text.length() > 22) text = text.substring(0, 21) + "…";
                g.drawString(text, legendX + 14, legendY);
                legendY += 19;
                index++;
            }
        }
    }
}
