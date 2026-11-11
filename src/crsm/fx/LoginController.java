package crsm.fx;

import crsm.SupabaseAuthService;
import crsm.UserSession;
import crsm.AuthRateLimitException;
import java.time.Instant;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public final class LoginController {
    @FXML private TextField email;
    @FXML private PasswordField password;
    @FXML private CheckBox remember;
    @FXML private Label status;
    private static Instant emailRetryNotBefore=Instant.EPOCH;
    private boolean emailRequestInProgress;
    private static long retrySeconds(){return Math.max(SupabaseAuthService.emailRetrySeconds(),Math.max(0,java.time.Duration.between(Instant.now(),emailRetryNotBefore).getSeconds()+1));}
    private static void pauseEmailRequests(Throwable error){if(error instanceof AuthRateLimitException limited)emailRetryNotBefore=Instant.now().plusSeconds(limited.appPauseSeconds());}
    private static String emailError(Throwable error){pauseEmailRequests(error);return message(error)+(error instanceof AuthRateLimitException?" App retries are paused temporarily; the Supabase limit may last longer. No account or email delivery was confirmed.":"");}

    @FXML private void signIn(ActionEvent event) {
        String address=email.getText().trim(), secret=password.getText();
        if(address.isBlank() || secret.isBlank()){status.setText("Enter your email address and password.");return;}
        status.setText("Signing in…");
        FxSupport.task(() -> new SupabaseAuthService().signIn(address,secret), session -> openWorkspace((UserSession)session), error -> status.setText(message(error)));
    }

    @FXML private void signUp(ActionEvent event) {
        if(emailRequestInProgress){status.setText("An email request is already in progress.");return;}
        Dialog<ButtonType> dialog=new Dialog<>();dialog.setTitle("Create a Customer account");dialog.initOwner(email.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("crsm.css").toExternalForm());
        GridPane grid=new GridPane();grid.setHgap(12);grid.setVgap(12);
        TextField name=new TextField(),username=new TextField(),address=new TextField(email.getText().strip());PasswordField secret=new PasswordField(),confirm=new PasswordField();
        address.setPromptText("An email address you own");username.setPromptText("3-50 letters, numbers or underscores");secret.setPromptText("At least 8 characters");
        grid.add(new Label("Full name"),0,0);grid.add(name,1,0);grid.add(new Label("Username"),0,1);grid.add(username,1,1);grid.add(new Label("Email address"),0,2);grid.add(address,1,2);grid.add(new Label("Password"),0,3);grid.add(secret,1,3);grid.add(new Label("Confirm password"),0,4);grid.add(confirm,1,4);
        Label feedback=new Label("Use your own email address. Email confirmation may be required before sign-in.");feedback.setWrapText(true);feedback.setMaxWidth(530);
        dialog.getDialogPane().setContent(new javafx.scene.layout.VBox(14,grid,feedback));dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        Button create=(Button)dialog.getDialogPane().lookupButton(ButtonType.OK);create.setText("Create account");create.getStyleClass().add("primary-button");
        final boolean[] saving={false};dialog.setOnCloseRequest(e->{if(saving[0])e.consume();});
        Runnable updatePause=()->{if(!saving[0]){long seconds=retrySeconds();create.setDisable(seconds>0);create.setText(seconds>0?"Retry available in "+seconds+"s":"Create account");}};
        Timeline pauseTimer=new Timeline(new KeyFrame(Duration.seconds(1),e->updatePause.run()));pauseTimer.setCycleCount(Timeline.INDEFINITE);dialog.setOnHidden(e->pauseTimer.stop());updatePause.run();pauseTimer.play();
        create.addEventFilter(ActionEvent.ACTION,e->{e.consume();
            if(emailRequestInProgress||retrySeconds()>0){feedback.setText("Email requests are temporarily paused. Check your inbox before retrying.");return;}
            if(!secret.getText().equals(confirm.getText())){feedback.setText("Passwords do not match. Please re-enter the confirmation.");return;}
            String n=name.getText(),u=username.getText(),mail=address.getText(),p=secret.getText();saving[0]=true;emailRequestInProgress=true;create.setDisable(true);dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(true);feedback.setText("Creating your Customer account...");
            FxSupport.task(()->new SupabaseAuthService().signUp(n,u,mail,p),ready->{saving[0]=false;emailRequestInProgress=false;emailRetryNotBefore=Instant.now().plusSeconds(60);dialog.close();email.setText(mail.strip());password.clear();status.setText(Boolean.TRUE.equals(ready)?"Account created. You can now sign in.":"Check your email for a confirmation link, then sign in. If you already registered, use Sign In or Forgot password.");},error->{saving[0]=false;emailRequestInProgress=false;dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(false);feedback.setText(emailError(error));updatePause.run();});
        });dialog.show();
    }

    @FXML private void forgotPassword(ActionEvent event) {
        if(emailRequestInProgress){status.setText("An email request is already in progress.");return;}
        if(retrySeconds()>0){status.setText("Email requests are paused in this app for "+retrySeconds()+" seconds. The provider may require longer; check your inbox first.");return;}
        String address=email.getText().trim();if(address.isBlank()){status.setText("Enter your email address first.");return;}
        emailRequestInProgress=true;status.setText("Sending reset instructions...");
        FxSupport.task(()->{new SupabaseAuthService().sendPasswordReset(address);return null;},ignored->{emailRequestInProgress=false;emailRetryNotBefore=Instant.now().plusSeconds(60);status.setText("If the account exists, reset instructions were sent. Check your inbox before requesting another email.");},error->{emailRequestInProgress=false;status.setText(emailError(error));});
    }

    private void openWorkspace(UserSession session) {
        try { FXMLLoader loader=new FXMLLoader(getClass().getResource("/crsm/fx/workspace.fxml")); loader.setController(new WorkspaceController(session)); Scene scene=new Scene(loader.load(),1440,900); scene.getStylesheets().add(getClass().getResource("/crsm/fx/crsm.css").toExternalForm()); Stage stage=(Stage)email.getScene().getWindow(); stage.setTitle("CRSM | Computer Repair Service Management"); stage.setScene(scene); stage.setMinWidth(1180);stage.setMinHeight(720); stage.centerOnScreen(); }
        catch(Exception error){status.setText(message(error));}
    }
    private static String message(Throwable error){Throwable t=error.getCause()==null?error:error.getCause();return t.getMessage()==null?t.getClass().getSimpleName():t.getMessage();}
}
