package crsm;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.UIManager;

/** Colors transcribed from the supplied color_palette.jpg swatches. */
public final class CrsmTheme {
    public static final Color WHITE = Color.decode("#FFFFFF");
    public static final Color LAVENDER = Color.decode("#4A709C");
    public static final Color INDIGO = Color.decode("#3B5998");
    public static final Color NAVY = Color.decode("#1E2640");
    public static final Color PEACH = Color.decode("#4A709C");
    public static final Color SIDEBAR = Color.decode("#1E2640");
    public static final Color SIDEBAR_HOVER = Color.decode("#232A45");
    public static final Color CANVAS = Color.decode("#F4F6F9");
    public static final Color BORDER = new Color(225, 230, 238);
    public static final Color TEXT = new Color(38, 47, 65);
    public static final Color MUTED = new Color(104, 116, 137);
    public static final Color SUCCESS = new Color(29, 132, 98);
    public static final Color WARNING = new Color(180, 112, 28);

    private CrsmTheme() {}

    static Font font(int style, int size) { return new Font("Raleway", style, size); }

    public static void install() {
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("TextField.font", base);
        UIManager.put("PasswordField.font", base);
        UIManager.put("ComboBox.font", base);
        UIManager.put("Table.font", base);
        UIManager.put("TableHeader.font", base.deriveFont(Font.BOLD));
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Button.background", WHITE);
        UIManager.put("Button.foreground", NAVY);
        UIManager.put("ComboBox.background", WHITE);
        UIManager.put("TextArea.background", WHITE);
        UIManager.put("Table.selectionBackground", LAVENDER);
        UIManager.put("Table.selectionForeground", WHITE);
        UIManager.put("Component.focusedBorderColor", LAVENDER);
        UIManager.put("TextField.caretForeground", NAVY);
        UIManager.put("TextArea.caretForeground", NAVY);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 11, 8, 11)));
    }
}
