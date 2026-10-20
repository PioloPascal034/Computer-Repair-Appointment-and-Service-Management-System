package crsm;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.UIManager;

/** Colors transcribed from the supplied color_palette.jpg swatches. */
public final class CrsmTheme {
    public static final Color WHITE = Color.decode("#FFFFFF");
    public static final Color LAVENDER = Color.decode("#676F9D");
    public static final Color INDIGO = Color.decode("#424769");
    public static final Color NAVY = Color.decode("#2D3250");
    public static final Color PEACH = Color.decode("#F9B17A");
    public static final Color CANVAS = new Color(246, 247, 251);
    public static final Color BORDER = new Color(224, 227, 238);
    public static final Color TEXT = new Color(38, 43, 68);

    private CrsmTheme() {}

    static Font font(int style, int size) { return new Font("Raleway", style, size); }

    public static void install() {
        Font base = new Font("Raleway", Font.PLAIN, 13);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("TextField.font", base);
        UIManager.put("PasswordField.font", base);
        UIManager.put("ComboBox.font", base);
        UIManager.put("Table.font", base);
        UIManager.put("TableHeader.font", base.deriveFont(Font.BOLD));
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Table.selectionBackground", LAVENDER);
        UIManager.put("Table.selectionForeground", WHITE);
        UIManager.put("Component.focusedBorderColor", LAVENDER);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(7, 9, 7, 9)));
    }
}
