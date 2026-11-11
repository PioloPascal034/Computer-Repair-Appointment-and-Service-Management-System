package crsm;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.UIManager;

/** Colors transcribed from the supplied color_palette.jpg swatches. */
public final class CrsmTheme {
    public static final Color WHITE = Color.decode("#FFFFFF");
    // Shared tokens transcribed from the supplied CRSM screens.
    public static final Color LAVENDER = Color.decode("#1769E8");
    public static final Color INDIGO = Color.decode("#0B3C91");
    public static final Color NAVY = Color.decode("#0A1855");
    public static final Color PEACH = Color.decode("#FFB83D");
    public static final Color SIDEBAR = Color.decode("#071C42");
    public static final Color SIDEBAR_HOVER = Color.decode("#0E4A9D");
    public static final Color CANVAS = Color.decode("#EEF6FF");
    public static final Color BORDER = new Color(211, 226, 247);
    public static final Color TEXT = new Color(18, 38, 91);
    public static final Color MUTED = new Color(80, 108, 160);
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
        UIManager.put("Table.selectionBackground", new Color(207, 226, 255));
        UIManager.put("Table.selectionForeground", NAVY);
        UIManager.put("Component.focusedBorderColor", LAVENDER);
        UIManager.put("TextField.caretForeground", NAVY);
        UIManager.put("TextArea.caretForeground", NAVY);
        UIManager.put("TableHeader.background", INDIGO);
        UIManager.put("TableHeader.foreground", WHITE);
        UIManager.put("TabbedPane.selected", LAVENDER);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 11, 8, 11)));
    }
}
