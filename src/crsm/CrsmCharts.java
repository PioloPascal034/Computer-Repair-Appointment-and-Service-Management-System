package crsm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

/** Simple palette-only charts drawn with Java2D. */
final class CrsmCharts {
    private static final Color[] SLICES = {
            CrsmTheme.PEACH, CrsmTheme.LAVENDER, CrsmTheme.INDIGO, CrsmTheme.NAVY, CrsmTheme.WHITE
    };

    private CrsmCharts() {}

    static JPanel pie(String title, Map<String, ? extends Number> values) {
        return new PiePanel(title, values);
    }

    static JPanel bars(String title, Map<String, ? extends Number> values) {
        return new BarPanel(title, values);
    }

    static JPanel line(String title, Map<String, ? extends Number> values) {
        return new LinePanel(title, values);
    }

    private static List<Slice> slices(Map<String, ? extends Number> values) {
        List<Slice> list = new ArrayList<>();
        if (values == null) return list;
        int i = 0;
        for (Map.Entry<String, ? extends Number> entry : values.entrySet()) {
            double amount = entry.getValue() == null ? 0 : entry.getValue().doubleValue();
            if (amount < 0) amount = 0;
            Color color = SLICES[i % (SLICES.length - 1)];
            if (color.equals(CrsmTheme.WHITE)) color = CrsmTheme.LAVENDER;
            list.add(new Slice(entry.getKey(), amount, color));
            i++;
        }
        return list;
    }

    private record Slice(String label, double value, Color color) {}

    private static final class PiePanel extends JPanel {
        private final String title;
        private final List<Slice> slices;
        PiePanel(String title, Map<String, ? extends Number> values) {
            this.title = title; this.slices = slices(values);
            setBackground(CrsmTheme.WHITE);
            setPreferredSize(new Dimension(320, 240));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(CrsmTheme.font(Font.BOLD, 14));
            g2.setColor(CrsmTheme.NAVY);
            g2.drawString(title, 12, 22);
            double total = slices.stream().mapToDouble(s -> s.value).sum();
            int size = Math.min(getWidth() / 2, getHeight() - 50);
            int x = 16, y = 36;
            double angle = 90;
            if (total <= 0 || slices.isEmpty()) {
                g2.setColor(CrsmTheme.LAVENDER);
                g2.setFont(CrsmTheme.font(Font.PLAIN, 12));
                g2.drawString("No values available from the current data.", 16, y + 24);
                g2.dispose();
                return;
            }
            for (Slice slice : slices) {
                double extent = -(slice.value / total) * 360.0;
                g2.setColor(slice.color);
                g2.fill(new Arc2D.Double(x, y, size, size, angle, extent, Arc2D.PIE));
                angle += extent;
            }
            g2.setColor(CrsmTheme.WHITE);
            g2.fillOval(x + size / 4, y + size / 4, size / 2, size / 2);
            g2.setColor(CrsmTheme.NAVY);
            g2.setFont(CrsmTheme.font(Font.BOLD, 16));
            String center = String.valueOf((int) Math.round(total));
            CrsmUi.drawCentered(g2, center, x + size / 2, y + size / 2 - 4);
            int legendY = 40;
            g2.setFont(CrsmTheme.font(Font.PLAIN, 12));
            for (Slice slice : slices) {
                g2.setColor(slice.color);
                g2.fillRoundRect(x + size + 18, legendY, 12, 12, 4, 4);
                g2.setColor(CrsmTheme.NAVY);
                g2.drawString(slice.label + "  " + (int) Math.round(slice.value), x + size + 36, legendY + 11);
                legendY += 20;
            }
            g2.dispose();
        }
    }

    private static final class BarPanel extends JPanel {
        private final String title;
        private final List<Slice> slices;
        BarPanel(String title, Map<String, ? extends Number> values) {
            this.title = title; this.slices = slices(values);
            setBackground(CrsmTheme.WHITE);
            setPreferredSize(new Dimension(360, 220));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(CrsmTheme.font(Font.BOLD, 14));
            g2.setColor(CrsmTheme.NAVY);
            g2.drawString(title, 12, 22);
            double max = slices.stream().mapToDouble(s -> s.value).max().orElse(0);
            int base = getHeight() - 36;
            int left = 16;
            int width = slices.isEmpty() ? 20 : Math.max(18, (getWidth() - 32) / slices.size() - 8);
            int i = 0;
            if (max <= 0) {
                g2.setColor(CrsmTheme.LAVENDER);
                g2.setFont(CrsmTheme.font(Font.PLAIN, 12));
                g2.drawString("No values available from the current data.", 16, 50);
                g2.dispose();
                return;
            }
            for (Slice slice : slices) {
                int height = (int) Math.round((slice.value / max) * (getHeight() - 80));
                int x = left + i * (width + 8);
                g2.setColor(slice.color);
                g2.fillRoundRect(x, base - height, width, height, 8, 8);
                g2.setColor(CrsmTheme.NAVY);
                g2.setFont(CrsmTheme.font(Font.PLAIN, 10));
                String label = slice.label.length() > 8 ? slice.label.substring(0, 8) : slice.label;
                g2.drawString(label, x, base + 14);
                i++;
            }
            g2.dispose();
        }
    }

    private static final class LinePanel extends JPanel {
        private final String title;
        private final List<Slice> slices;
        LinePanel(String title, Map<String, ? extends Number> values) {
            this.title = title; this.slices = slices(values);
            setBackground(CrsmTheme.WHITE);
            setPreferredSize(new Dimension(360, 220));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(CrsmTheme.font(Font.BOLD, 14));
            g2.setColor(CrsmTheme.NAVY);
            g2.drawString(title, 12, 22);
            if (slices.size() < 2) {
                g2.setColor(CrsmTheme.LAVENDER);
                g2.setFont(CrsmTheme.font(Font.PLAIN, 12));
                g2.drawString("Need at least two dated payment totals to draw this chart.", 16, 50);
                g2.dispose();
                return;
            }
            double max = slices.stream().mapToDouble(s -> s.value).max().orElse(1);
            int left = 36, right = getWidth() - 16, top = 40, bottom = getHeight() - 36;
            g2.setColor(CrsmTheme.LAVENDER);
            g2.drawLine(left, bottom, right, bottom);
            g2.setColor(CrsmTheme.PEACH);
            g2.setStroke(new BasicStroke(3f));
            int prevX = 0, prevY = 0;
            for (int i = 0; i < slices.size(); i++) {
                int x = left + i * (right - left) / Math.max(1, slices.size() - 1);
                int y = bottom - (int) Math.round((slices.get(i).value / Math.max(max, 1)) * (bottom - top));
                if (i > 0) g2.drawLine(prevX, prevY, x, y);
                prevX = x; prevY = y;
            }
            g2.setColor(CrsmTheme.NAVY);
            for (int i = 0; i < slices.size(); i++) {
                int x = left + i * (right - left) / Math.max(1, slices.size() - 1);
                int y = bottom - (int) Math.round((slices.get(i).value / Math.max(max, 1)) * (bottom - top));
                g2.fillOval(x - 4, y - 4, 8, 8);
                g2.setFont(CrsmTheme.font(Font.PLAIN, 10));
                g2.drawString(slices.get(i).label, x - 10, bottom + 14);
            }
            g2.dispose();
        }
    }
}
