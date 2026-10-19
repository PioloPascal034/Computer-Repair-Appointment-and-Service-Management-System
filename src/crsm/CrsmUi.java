package crsm;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;

/** Native Swing building blocks that follow the supplied palette and layouts. */
final class CrsmUi {
    private CrsmUi() {}

    static JPanel card() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CrsmTheme.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(CrsmTheme.LAVENDER, 16),
                new EmptyBorder(18, 18, 18, 18)));
        return panel;
    }

    static JPanel padded(Color background, int pad) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(background);
        panel.setBorder(new EmptyBorder(pad, pad, pad, pad));
        return panel;
    }

    static JLabel heading(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(CrsmTheme.font(Font.BOLD, size));
        label.setForeground(CrsmTheme.NAVY);
        return label;
    }

    static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(CrsmTheme.font(Font.PLAIN, 13));
        label.setForeground(CrsmTheme.LAVENDER);
        return label;
    }

    static JLabel html(String text, Color color, int size) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setFont(CrsmTheme.font(Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    static JButton primary(String text) {
        return filled(text, CrsmTheme.PEACH, CrsmTheme.NAVY, Font.BOLD, 14);
    }

    static JButton navy(String text) {
        return filled(text, CrsmTheme.INDIGO, CrsmTheme.WHITE, Font.BOLD, 13);
    }

    static JButton danger(String text) {
        return filled(text, CrsmTheme.NAVY, CrsmTheme.WHITE, Font.BOLD, 12);
    }

    static JButton ghost(String text) {
        JButton button = new JButton(text);
        button.setFont(CrsmTheme.font(Font.PLAIN, 13));
        button.setForeground(CrsmTheme.INDIGO);
        button.setBackground(CrsmTheme.WHITE);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(CrsmTheme.LAVENDER, 12),
                new EmptyBorder(8, 12, 8, 12)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static JButton iconAction(String text) {
        JButton button = link(text);
        button.setFont(CrsmTheme.font(Font.BOLD, 14));
        button.setForeground(CrsmTheme.INDIGO);
        return button;
    }

    private static JButton filled(String text, Color background, Color foreground, int style, int size) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = background;
                if (getModel().isPressed()) fill = CrsmTheme.INDIGO;
                else if (getModel().isRollover()) fill = CrsmTheme.LAVENDER;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(CrsmTheme.font(style, size));
        button.setForeground(foreground);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static JButton link(String text) {
        JButton button = new JButton(text);
        button.setFont(CrsmTheme.font(Font.BOLD, 12));
        button.setForeground(CrsmTheme.INDIGO);
        button.setBackground(CrsmTheme.WHITE);
        button.setBorder(new EmptyBorder(4, 4, 4, 4));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static JButton navItem(String text, boolean selected) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean on = Boolean.TRUE.equals(getClientProperty("selected"));
                g2.setColor(on ? CrsmTheme.INDIGO : CrsmTheme.NAVY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.putClientProperty("selected", selected);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIcon(new SymbolIcon(text, 20, CrsmTheme.WHITE));
        button.setIconTextGap(12);
        button.setFont(CrsmTheme.font(selected ? Font.BOLD : Font.PLAIN, 13));
        button.setForeground(CrsmTheme.WHITE);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(10, 14, 10, 14));
        button.setFocusPainted(false);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    static void selectNav(JButton button, boolean selected) {
        button.putClientProperty("selected", selected);
        button.setFont(CrsmTheme.font(selected ? Font.BOLD : Font.PLAIN, 13));
        button.repaint();
    }

    static JLabel badge(String text) {
        JLabel label = new JLabel(text == null || text.isBlank() ? "—" : text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setFont(CrsmTheme.font(Font.BOLD, 11));
        label.setBorder(new EmptyBorder(4, 10, 4, 10));
        String value = text == null ? "" : text;
        if (List.of("Paid", "Completed", "Confirmed", "Ready for Pickup").contains(value)) {
            label.setBackground(CrsmTheme.PEACH);
            label.setForeground(CrsmTheme.NAVY);
        } else if (List.of("Pending", "Unpaid", "Partial", "Waiting for Parts").contains(value)) {
            label.setBackground(CrsmTheme.LAVENDER);
            label.setForeground(CrsmTheme.WHITE);
        } else if (List.of("ADMIN", "Admin", "Under Repair", "Under Diagnosis").contains(value)) {
            label.setBackground(CrsmTheme.INDIGO);
            label.setForeground(CrsmTheme.WHITE);
        } else {
            label.setBackground(CrsmTheme.NAVY);
            label.setForeground(CrsmTheme.WHITE);
        }
        return label;
    }

    static String money(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return "—";
        try {
            return "₱" + new java.math.BigDecimal(String.valueOf(value)).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (RuntimeException ex) {
            return String.valueOf(value);
        }
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String titleCase(String field) {
        if (field == null || field.isBlank()) return "";
        String[] parts = field.split("_");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!label.isEmpty()) label.append(' ');
            label.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return label.toString();
    }

    static void styleField(JTextField field) {
        field.setFont(CrsmTheme.font(Font.PLAIN, 14));
        field.setForeground(CrsmTheme.NAVY);
        field.setBackground(CrsmTheme.WHITE);
        field.setCaretColor(CrsmTheme.NAVY);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(CrsmTheme.LAVENDER, 12),
                new EmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(100, 42));
    }

    static JPanel labeled(String caption, JComponent field) {
        JPanel block = new JPanel(new BorderLayout(0, 6));
        block.setOpaque(false);
        JLabel label = new JLabel(caption);
        label.setFont(CrsmTheme.font(Font.BOLD, 13));
        label.setForeground(CrsmTheme.NAVY);
        block.add(label, BorderLayout.NORTH);
        block.add(field, BorderLayout.CENTER);
        return block;
    }

    static JPanel passwordRow(JPasswordField password) {
        styleField(password);
        JButton toggle = link("Show");
        toggle.addActionListener(e -> {
            boolean reveal = password.getEchoChar() != 0;
            password.setEchoChar(reveal ? (char) 0 : '\u2022');
            toggle.setText(reveal ? "Hide" : "Show");
        });
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(password, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);
        return row;
    }

    /** A native input row matching the sign-in/sign-up field treatment in the supplied designs. */
    static JPanel authInput(JTextField field, String symbol, String placeholder) {
        field.setFont(CrsmTheme.font(Font.PLAIN, 14));
        field.setForeground(CrsmTheme.NAVY);
        field.setBackground(CrsmTheme.WHITE);
        field.setCaretColor(CrsmTheme.NAVY);
        field.setBorder(new EmptyBorder(9, 5, 9, 10));
        field.setPreferredSize(new Dimension(100, 44));
        field.putClientProperty("JTextField.placeholderText", placeholder);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(CrsmTheme.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(CrsmTheme.LAVENDER, 12),
                new EmptyBorder(2, 12, 2, 8)));
        JLabel glyph = new JLabel(new SymbolIcon(symbol, 20, CrsmTheme.NAVY));
        glyph.setPreferredSize(new Dimension(24, 36));
        row.add(glyph, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    static JPanel authPassword(JPasswordField field, String placeholder) {
        field.setFont(CrsmTheme.font(Font.PLAIN, 14));
        field.setForeground(CrsmTheme.NAVY);
        field.setBackground(CrsmTheme.WHITE);
        field.setCaretColor(CrsmTheme.NAVY);
        field.setBorder(new EmptyBorder(9, 5, 9, 5));
        field.setPreferredSize(new Dimension(100, 44));
        field.putClientProperty("JTextField.placeholderText", placeholder);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(CrsmTheme.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(CrsmTheme.LAVENDER, 12),
                new EmptyBorder(2, 12, 2, 8)));
        JLabel glyph = new JLabel(new SymbolIcon("lock", 20, CrsmTheme.NAVY));
        glyph.setPreferredSize(new Dimension(24, 36));
        JButton toggle = link("Show");
        toggle.setToolTipText("Show password");
        toggle.addActionListener(e -> {
            boolean reveal = field.getEchoChar() != 0;
            field.setEchoChar(reveal ? (char) 0 : '\u2022');
            toggle.setText(reveal ? "Hide" : "Show");
            toggle.setToolTipText(reveal ? "Hide password" : "Show password");
        });
        row.add(glyph, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);
        return row;
    }

    static JPanel metric(String title, String value, String note) {
        JPanel card = card();
        JLabel name = muted(title);
        JLabel number = heading(value, 28);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        number.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(name);
        body.add(Box.createVerticalStrut(8));
        body.add(number);
        if (note != null && !note.isBlank()) {
            JLabel extra = muted(note);
            extra.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(Box.createVerticalStrut(6));
            body.add(extra);
        }
        JLabel symbol = new JLabel(new SymbolIcon(title, 24, CrsmTheme.NAVY));
        symbol.setOpaque(true);
        symbol.setBackground(title.toLowerCase().contains("repair") ? CrsmTheme.PEACH : CrsmTheme.LAVENDER);
        symbol.setPreferredSize(new Dimension(46, 46));
        symbol.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel leading = new JPanel(new BorderLayout(10, 0));
        leading.setOpaque(false);
        leading.add(symbol, BorderLayout.WEST);
        leading.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
        card.add(body, BorderLayout.CENTER);
        card.add(leading, BorderLayout.WEST);
        return card;
    }

    static JPanel notice(String title, String detail) {
        JPanel card = card();
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel heading = heading(title, 16);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel text = html("<div style='width:620px'>" + detail + "</div>", CrsmTheme.INDIGO, 13);
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(heading);
        body.add(Box.createVerticalStrut(8));
        body.add(text);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    static void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(CrsmTheme.LAVENDER);
        table.setBackground(CrsmTheme.WHITE);
        table.setForeground(CrsmTheme.NAVY);
        table.setSelectionBackground(CrsmTheme.LAVENDER);
        table.setSelectionForeground(CrsmTheme.WHITE);
        table.getTableHeader().setBackground(CrsmTheme.NAVY);
        table.getTableHeader().setForeground(CrsmTheme.WHITE);
        table.getTableHeader().setFont(CrsmTheme.font(Font.BOLD, 12));
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    static JScrollPane scroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(new RoundedBorder(CrsmTheme.LAVENDER, 12));
        scroll.getViewport().setBackground(CrsmTheme.WHITE);
        scroll.setBackground(CrsmTheme.WHITE);
        return scroll;
    }

    static JPanel pageHead(String title, String subtitle, JComponent trailing) {
        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel heading = heading(title, 28);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = muted(subtitle);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(heading);
        titles.add(Box.createVerticalStrut(4));
        titles.add(sub);
        head.add(titles, BorderLayout.WEST);
        if (trailing != null) head.add(trailing, BorderLayout.EAST);
        return head;
    }

    static JPanel metricsRow(JPanel... cards) {
        JPanel row = new JPanel(new GridLayout(1, Math.max(1, cards.length), 12, 12));
        row.setOpaque(false);
        for (JPanel card : cards) row.add(card);
        return row;
    }

    static JPanel logoMark(int size, boolean light) {
        return new LogoPanel(size, light);
    }

    static JPanel workshopScene() {
        return new WorkshopPanel();
    }

    static JTextArea multiline(int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(CrsmTheme.font(Font.PLAIN, 13));
        area.setForeground(CrsmTheme.NAVY);
        area.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(CrsmTheme.LAVENDER, 10),
                new EmptyBorder(8, 8, 8, 8)));
        return area;
    }

    static JPanel flowLeft(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panel.setOpaque(false);
        for (Component component : components) panel.add(component);
        return panel;
    }

    private static final class SymbolIcon implements Icon {
        private final String key;
        private final int size;
        private final Color color;

        SymbolIcon(String key, int size, Color color) {
            this.key = key == null ? "" : key.toLowerCase();
            this.size = size;
            this.color = color;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.scale(size / 20.0, size / 20.0);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if (key.contains("user") || key.contains("customer") || key.contains("profile") || key.contains("staff") || key.contains("admin")) {
                g.drawOval(7, 2, 6, 6);
                g.drawArc(3, 10, 14, 10, 0, 180);
            } else if (key.contains("computer") || key.contains("device")) {
                g.drawRoundRect(2, 3, 16, 11, 2, 2);
                g.drawLine(10, 14, 10, 17);
                g.drawLine(6, 18, 14, 18);
            } else if (key.contains("appointment") || key.contains("calendar")) {
                g.drawRoundRect(3, 4, 14, 14, 2, 2);
                g.drawLine(3, 8, 17, 8);
                g.drawLine(7, 2, 7, 6);
                g.drawLine(13, 2, 13, 6);
                g.drawLine(7, 11, 8, 11);
                g.drawLine(12, 11, 13, 11);
                g.drawLine(7, 14, 8, 14);
            } else if (key.contains("repair") || key.contains("service") || key.contains("technician") || key.contains("tool")) {
                g.drawLine(4, 16, 14, 6);
                g.drawArc(10, 1, 8, 8, 35, 225);
                g.drawOval(2, 14, 5, 5);
            } else if (key.contains("payment") || key.contains("revenue")) {
                g.drawOval(2, 2, 16, 16);
                g.setFont(CrsmTheme.font(Font.BOLD, 13));
                g.drawString("₱", 6, 14);
            } else if (key.contains("report")) {
                g.drawLine(3, 18, 18, 18);
                g.drawLine(4, 16, 4, 10);
                g.drawLine(9, 16, 9, 6);
                g.drawLine(14, 16, 14, 3);
            } else if (key.contains("setting") || key.contains("gear")) {
                g.drawOval(5, 5, 10, 10);
                g.drawOval(8, 8, 4, 4);
                g.drawLine(10, 1, 10, 4);
                g.drawLine(10, 16, 10, 19);
                g.drawLine(1, 10, 4, 10);
                g.drawLine(16, 10, 19, 10);
                g.drawLine(3, 3, 6, 6);
                g.drawLine(14, 14, 17, 17);
                g.drawLine(14, 6, 17, 3);
                g.drawLine(3, 17, 6, 14);
            } else if (key.contains("email")) {
                g.drawRoundRect(2, 4, 16, 12, 2, 2);
                g.drawLine(3, 5, 10, 11);
                g.drawLine(17, 5, 10, 11);
            } else if (key.contains("lock")) {
                g.drawRoundRect(4, 9, 12, 9, 2, 2);
                g.drawArc(6, 2, 8, 12, 0, 180);
                g.drawOval(9, 12, 2, 2);
            } else {
                g.drawRoundRect(3, 3, 6, 6, 1, 1);
                g.drawRoundRect(11, 3, 6, 6, 1, 1);
                g.drawRoundRect(3, 11, 6, 6, 1, 1);
                g.drawRoundRect(11, 11, 6, 6, 1, 1);
            }
            g.dispose();
        }
    }

    static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        RoundedBorder(Color color, int radius) { this.color = color; this.radius = radius; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(x + 1, y + 1, width - 3, height - 3, radius, radius));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(4, 4, 4, 4); }
    }

    private static final class LogoPanel extends JPanel {
        private final int size;
        private final boolean light;
        LogoPanel(int size, boolean light) {
            this.size = size; this.light = light;
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color ink = light ? CrsmTheme.WHITE : CrsmTheme.NAVY;
            g2.setColor(ink);
            int s = Math.min(getWidth(), getHeight()) - 4;
            g2.scale(s / 48.0, s / 48.0);
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(2, 12, 29, 23, 3, 3);
            g2.drawLine(12, 35, 12, 41);
            g2.drawLine(7, 42, 22, 42);
            g2.drawOval(29, 3, 14, 14);
            g2.drawOval(34, 8, 4, 4);
            g2.drawLine(36, 0, 36, 3);
            g2.drawLine(36, 17, 36, 20);
            g2.drawLine(26, 10, 29, 10);
            g2.drawLine(43, 10, 46, 10);
            g2.drawRoundRect(29, 23, 15, 20, 2, 2);
            g2.drawLine(32, 32, 36, 36);
            g2.drawLine(36, 36, 42, 29);
            g2.dispose();
        }
    }

    private static final class WorkshopPanel extends JPanel {
        WorkshopPanel() {
            setBackground(CrsmTheme.NAVY);
            setPreferredSize(new Dimension(520, 0));
            setLayout(new BorderLayout());
            JPanel copy = new JPanel();
            copy.setOpaque(false);
            copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
            copy.setBorder(new EmptyBorder(48, 42, 36, 36));
            JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            brand.setOpaque(false);
            brand.setAlignmentX(Component.LEFT_ALIGNMENT);
            brand.add(logoMark(54, true));
            JPanel names = new JPanel();
            names.setOpaque(false);
            names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
            JLabel mark = new JLabel("CRSM");
            mark.setFont(CrsmTheme.font(Font.BOLD, 36));
            mark.setForeground(CrsmTheme.WHITE);
            JLabel name = new JLabel("COMPUTER REPAIR SERVICE MANAGEMENT");
            name.setFont(CrsmTheme.font(Font.BOLD, 12));
            name.setForeground(CrsmTheme.WHITE);
            names.add(mark); names.add(name);
            brand.add(names);
            JPanel accent = new JPanel();
            accent.setBackground(CrsmTheme.PEACH);
            accent.setMaximumSize(new Dimension(72, 4));
            accent.setPreferredSize(new Dimension(72, 4));
            accent.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel tag = new JLabel("Manage Devices. Deliver Solutions.");
            tag.setFont(CrsmTheme.font(Font.PLAIN, 20));
            tag.setForeground(CrsmTheme.WHITE);
            tag.setAlignmentX(Component.LEFT_ALIGNMENT);
            copy.add(brand);
            copy.add(Box.createVerticalStrut(18));
            copy.add(accent);
            copy.add(Box.createVerticalStrut(16));
            copy.add(tag);
            add(copy, BorderLayout.NORTH);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(CrsmTheme.INDIGO);
            g2.fillRoundRect(48, h - 280, 210, 140, 16, 16);
            g2.setColor(CrsmTheme.LAVENDER);
            g2.fillRoundRect(68, h - 262, 170, 100, 10, 10);
            g2.setColor(CrsmTheme.WHITE);
            g2.fillRoundRect(280, h - 210, 150, 95, 14, 14);
            g2.setColor(CrsmTheme.PEACH);
            g2.fillOval(w - 120, 80, 70, 70);
            g2.setColor(CrsmTheme.INDIGO);
            g2.fillRoundRect(40, h - 90, w - 80, 18, 8, 8);
            g2.dispose();
        }
    }

    static void drawCentered(Graphics2D g2, String text, int cx, int cy) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, cx - metrics.stringWidth(text) / 2, cy + metrics.getAscent() / 2);
    }
}
