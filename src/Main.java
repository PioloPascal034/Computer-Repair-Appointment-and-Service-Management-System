import javax.swing.SwingUtilities;
import crsm.CrsmApplication;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CrsmApplication().showLogin());
    }
}
