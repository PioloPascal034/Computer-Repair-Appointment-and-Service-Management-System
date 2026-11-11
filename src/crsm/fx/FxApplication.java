package crsm.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** JavaFX entry point. The existing Swing implementation remains available as a legacy fallback. */
public final class FxApplication extends Application {
    private static javafx.application.HostServices links;
    static void openLink(String url){if(links==null)throw new IllegalStateException("Open the JavaFX app first.");links.showDocument(url);}
    @Override public void start(Stage stage) throws Exception {
        links=getHostServices();
        FXMLLoader loader = new FXMLLoader(FxApplication.class.getResource("/crsm/fx/login.fxml"));
        Scene scene = new Scene(loader.load(), 1180, 760);
        scene.getStylesheets().add(FxApplication.class.getResource("/crsm/fx/crsm.css").toExternalForm());
        stage.setTitle("CRSM | Sign In");
        stage.setMinWidth(1020); stage.setMinHeight(680);
        stage.setScene(scene); stage.show();
    }
}
