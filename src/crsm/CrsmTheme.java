package crsm;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.UIManager;

/**
 * Colors taken from the supplied color_palette.jpg swatches only:
 * #ffffff, #676f9d, #424769, #2d3250, #f9b17a.
 */
public final class CrsmTheme {
    public static final Color WHITE = Color.decode("#FFFFFF");
    public static final Color LAVENDER = Color.decode("#676F9D");
    public static final Color INDIGO = Color.decode("#424769");
    public static final Color NAVY = Color.decode("#2D3250");
    public static final Color PEACH = Color.decode("#F9B17A");
    public static final Color CANVAS = WHITE;
    public static final Color BORDER = LAVENDER;
    public static final Color TEXT = NAVY;
    public static final String FONT_FAMILY = resolveFamily();

    private CrsmTheme() {}

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static void install() {
        Font base = font(Font.PLAIN, 13);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("TextField.font", base);
        UIManager.put("PasswordField.font", base);
        UIManager.put("ComboBox.font", base);
        UIManager.put("Table.font", base);
        UIManager.put("TableHeader.font", font(Font.BOLD, 13));
        UIManager.put("CheckBox.font", base);
        UIManager.put("TabbedPane.font", base);
        UIManager.put("Panel.background", WHITE);
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("Table.selectionBackground", LAVENDER);
        UIManager.put("Table.selectionForeground", WHITE);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LAVENDER), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        UIManager.put("PasswordField.border", UIManager.get("TextField.border"));
    }

    private static String resolveFamily() {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase("Raleway")) return name;
        }
        return "Segoe UI";
    }
}
