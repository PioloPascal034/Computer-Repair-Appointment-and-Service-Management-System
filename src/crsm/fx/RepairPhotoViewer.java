package crsm.fx;

import crsm.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import java.io.ByteArrayInputStream;
import java.util.*;

/** Private attachments are fetched with the signed-in JWT, never with public URLs. */
final class RepairPhotoViewer {
    static void show(Node owner, UserSession session, CustomerWorkflowService workflow, int repairId) {
        Dialog<Void> dialog=new Dialog<>();dialog.setTitle("Repair #"+repairId+" details");
        dialog.initOwner(owner.getScene().getWindow());dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(RepairPhotoViewer.class.getResource("crsm.css").toExternalForm());
        VBox body=new VBox(14);body.getStyleClass().add("card");Label status=new Label("Loading repair and attached damage photos?");
        FlowPane thumbnails=new FlowPane(12,12);Button retry=new Button("Reload attachments");retry.getStyleClass().add("ghost-button");
        body.getChildren().addAll(status,thumbnails,retry);dialog.getDialogPane().setContent(body);dialog.getDialogPane().setPrefWidth(650);
        Runnable load=()->{thumbnails.getChildren().clear();status.setText("Loading?");retry.setDisable(true);
            FxSupport.task(()->{Map<String,Object> repair=workflow.repairs(session).stream().filter(r->String.valueOf(r.get("repair_request_id")).equals(String.valueOf(repairId))).findFirst().orElseThrow(()->new SecurityException("Repair unavailable or access denied."));
                return new AbstractMap.SimpleEntry<>(repair,workflow.repairPhotos(session,repairId));},result->{
                retry.setDisable(false);List<Map<String,Object>> photos=result.getValue();
                status.setText(String.valueOf(result.getKey().get("repair_status"))+" ? "+String.valueOf(result.getKey().get("reported_problem"))+"\n"+(photos.isEmpty()?"No photos attached.":"Attached damage photos ? select a thumbnail to preview."));status.setWrapText(true);
                for(int i=0;i<photos.size();i++){final int index=i;String path=String.valueOf(photos.get(i).get("object_path"));Button thumbnail=new Button("Loading photo "+(i+1)+"?");thumbnail.getStyleClass().add("photo-thumbnail");thumbnail.setPrefSize(170,130);thumbnails.getChildren().add(thumbnail);
                    thumbnail.setOnAction(e->preview(dialog,session,workflow,repairId,photos,index));
                    FxSupport.task(()->image(workflow.repairPhoto(session,repairId,path),160,110),img->{thumbnail.setGraphic(new ImageView(img));thumbnail.setText("");thumbnail.setAccessibleText("Preview damage photo "+(index+1));},err->thumbnail.setText("Photo "+(index+1)+" unavailable\nClick to retry preview"));
                }
            },err->{retry.setDisable(false);status.setText("Unable to load repair attachments: "+err.getMessage());});};
        retry.setOnAction(e->load.run());load.run();dialog.show();
    }
    private static Image image(byte[] data,int width,int height){Image image=new Image(new ByteArrayInputStream(data),width,height,true,true);if(image.isError()||image.getWidth()==0)throw new IllegalStateException("The stored photo is not a readable image.");return image;}
    private static void preview(Dialog<?> parent,UserSession session,CustomerWorkflowService workflow,int repairId,List<Map<String,Object>> photos,int initial){
        Dialog<Void> preview=new Dialog<>();preview.setTitle("Damage photo preview");preview.initOwner(parent.getDialogPane().getScene().getWindow());preview.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        preview.getDialogPane().getStylesheets().add(RepairPhotoViewer.class.getResource("crsm.css").toExternalForm());
        ImageView image=new ImageView();image.setPreserveRatio(true);image.setFitWidth(760);image.setFitHeight(500);
        Label status=new Label();Button previous=new Button("Previous"),next=new Button("Next"),retry=new Button("Retry");for(Button b:List.of(previous,next,retry))b.getStyleClass().add("ghost-button");
        VBox body=new VBox(12,new StackPane(image),status,new HBox(10,previous,next,retry));body.getStyleClass().add("card");preview.getDialogPane().setContent(body);
        int[] index={initial},generation={0};Runnable load=()->{int ticket=++generation[0],position=index[0];image.setImage(null);status.setText("Loading photo "+(position+1)+" of "+photos.size()+"?");previous.setDisable(position==0);next.setDisable(position==photos.size()-1);
            FxSupport.task(()->image(workflow.repairPhoto(session,repairId,String.valueOf(photos.get(position).get("object_path"))),1520,1000),img->{if(ticket==generation[0]){image.setImage(img);status.setText("Photo "+(position+1)+" of "+photos.size());}},err->{if(ticket==generation[0])status.setText("Photo unavailable. Retry or choose another photo. "+err.getMessage());});};
        previous.setOnAction(e->{index[0]--;load.run();});next.setOnAction(e->{index[0]++;load.run();});retry.setOnAction(e->load.run());load.run();preview.show();
    }
}
