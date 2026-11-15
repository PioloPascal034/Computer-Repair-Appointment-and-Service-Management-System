package crsm.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

final class FXMLLoaderShim {
    private FXMLLoaderShim() {}
    static void loadLogin(Stage stage) throws Exception {
        FXMLLoader loader=new FXMLLoader(FXMLLoaderShim.class.getResource("/crsm/fx/login.fxml"));
        Scene scene=new Scene(loader.load(),1180,760);scene.getStylesheets().add(FXMLLoaderShim.class.getResource("/crsm/fx/crsm.css").toExternalForm());stage.setTitle("CRSM | Sign In");stage.setScene(scene);stage.show();
    }
}
