package crsm.fx;

import crsm.PaymentReceipt;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;

final class PaymentDialogs {
    private PaymentDialogs() {}
    private static Dialog<Void> dialog(Node owner,String title){
        Dialog<Void> dialog=new Dialog<>();dialog.setTitle(title);dialog.initOwner(owner.getScene().getWindow());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(PaymentDialogs.class.getResource("crsm.css").toExternalForm());return dialog;
    }
    static void receipt(Node owner,String customerName,Map<String,Object> bill,Map<String,Object> payment){
        String text=PaymentReceipt.text(customerName,bill,payment);
        Dialog<Void> dialog=dialog(owner,"Digital payment receipt");
        TextArea receipt=new TextArea(text);receipt.setEditable(false);receipt.setWrapText(true);receipt.setPrefSize(510,460);receipt.getStyleClass().add("receipt-text");
        Button download=new Button("Save receipt (.txt)");download.getStyleClass().add("primary-button");
        download.setOnAction(event->{FileChooser chooser=new FileChooser();chooser.setTitle("Save digital receipt");chooser.setInitialFileName("CRSM-receipt-"+String.valueOf(payment.get("payment_id")).replaceAll("[^0-9]","")+".txt");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text receipt","*.txt"));java.io.File file=chooser.showSaveDialog(dialog.getDialogPane().getScene().getWindow());if(file!=null)try{Files.writeString(file.toPath(),text,StandardCharsets.UTF_8);}catch(java.io.IOException error){FxSupport.error(receipt,"Could not save receipt",error);}});
        VBox body=new VBox(14,new Label("CRSM | PAYMENT RECEIPT"),receipt,download);body.getStyleClass().add("card");dialog.getDialogPane().setContent(body);dialog.show();
    }
    static void demoQr(Node owner,Map<String,Object> bill){
        Dialog<Void> dialog=dialog(owner,"Demo payment QR - no real payments");
        Label title=new Label("DEMO PAYMENT QR");title.getStyleClass().add("card-title");
        Label demo=new Label("DEMO ONLY");demo.getStyleClass().add("qr-demo-overlay");
        StackPane qr=new StackPane(mockPattern(),demo);qr.setMaxSize(232,232);
        Label notice=new Label("Mock visual only. This is not a scannable payment code. No funds are transferred and no payment is recorded.");notice.setWrapText(true);notice.setMaxWidth(350);notice.getStyleClass().add("demo-notice");
        Label instruction=new Label("Pay at the shop. Your assigned Technician or Admin must verify the money received and issue a receipt.");instruction.setWrapText(true);instruction.setMaxWidth(350);
        VBox body=new VBox(14,title,new Label("Repair #"+bill.get("repair_request_id")),new Label("Current balance: PHP "+bill.get("balance_due")),qr,notice,instruction);body.setAlignment(Pos.CENTER);body.getStyleClass().add("card");dialog.getDialogPane().setContent(body);dialog.show();
    }
    /** Decorative QR-like pattern, deliberately not an encoded payment payload. */
    private static Canvas mockPattern(){
        Canvas canvas=new Canvas(232,232);GraphicsContext graphics=canvas.getGraphicsContext2D();graphics.setFill(Color.WHITE);graphics.fillRect(0,0,232,232);
        Random random=new Random(20261111);graphics.setFill(Color.web("#071F3B"));
        for(int y=0;y<21;y++)for(int x=0;x<21;x++){
            boolean finder=(x<7&&y<7)||(x>=14&&y<7)||(x<7&&y>=14);
            int dx=x>=14?x-14:x,dy=y>=14?y-14:y;
            boolean dark=finder?(dx==0||dx==6||dy==0||dy==6||(dx>=2&&dx<=4&&dy>=2&&dy<=4)):random.nextBoolean();
            if(dark)graphics.fillRect(32+x*8,32+y*8,8,8);
        }
        return canvas;
    }
}
