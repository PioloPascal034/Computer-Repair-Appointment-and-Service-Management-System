package crsm.fx;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

final class FxSupport {
    private FxSupport() {}
    static <T> void task(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return work.call(); } };
        task.setOnSucceeded(e -> success.accept(task.getValue()));
        task.setOnFailed(e -> failure.accept(task.getException()));
        Thread thread = new Thread(task, "crsm-background-work"); thread.setDaemon(true); thread.start();
    }
    static void error(javafx.scene.Node owner, String title, Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message=cause.getMessage()==null?cause.getClass().getSimpleName():cause.getMessage();
        Alert alert=new Alert(Alert.AlertType.ERROR);alert.setTitle(title);alert.setHeaderText("The operation could not be completed");
        alert.initOwner(owner.getScene()==null?null:owner.getScene().getWindow());
        javafx.scene.control.Label text=new javafx.scene.control.Label(message);text.setWrapText(true);text.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);text.setMaxWidth(650);
        javafx.scene.control.ScrollPane scroll=new javafx.scene.control.ScrollPane(text);scroll.setFitToWidth(true);scroll.setPrefViewportWidth(650);scroll.setPrefViewportHeight(180);
        alert.getDialogPane().setContent(scroll);alert.setResizable(true);alert.showAndWait();
    }
    static void info(javafx.scene.Node owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK); alert.setTitle(title); alert.initOwner(owner.getScene().getWindow()); alert.showAndWait();
    }
    static void runLater(Runnable action) { Platform.runLater(action); }
}
