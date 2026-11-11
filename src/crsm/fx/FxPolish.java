package crsm.fx;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/** Small, finite animations: no timers or background animation loops. */
final class FxPolish {
    private FxPolish() {}
    static Node icon(String page) {
        String key=page.startsWith("Refresh")?"Refresh":page.startsWith("Save")||page.startsWith("Confirm")?"Save":page.startsWith("View")?"View":page.startsWith("Add")||page.startsWith("Register")?"Add":page.startsWith("Record payment")||page.contains("receipt")?"Payments":page.startsWith("Edit")?"Edit":page.startsWith("Delete")?"Delete":page;
        String path=switch(key){
            case "Dashboard" -> "M3 3H10V10H3Z M14 3H21V10H14Z M3 14H10V21H3Z M14 14H21V21H14Z";
            case "Customers", "User Accounts" -> "M10 7A3 3 0 1 1 4 7A3 3 0 1 1 10 7 M1 21V18A6 6 0 0 1 13 18V21 M17 4A3 3 0 0 1 17 10 M17 13A6 6 0 0 1 23 19V21";
            case "Profile", "My Profile", "Account Settings" -> "M16 7A4 4 0 1 1 8 7A4 4 0 1 1 16 7 M4 21V19A8 8 0 0 1 20 19V21";
            case "Computers", "My Computers" -> "M3 4H21V16H3Z M8 21H16 M12 16V21";
            case "Appointments", "My Schedule" -> "M3 5H21V21H3Z M7 2V8 M17 2V8 M3 11H21 M7 15H10 M14 15H17";
            case "Payments", "Payments & Receipts" -> "M5 2H19V22L16 20L13 22L10 20L7 22L5 20Z M8 7H16 M8 11H16 M8 15H12";
            case "Reports" -> "M4 3V21H22 M8 16V11 M13 16V6 M18 16V9";
            case "Technicians", "Repair Workflow", "My Assigned Repairs" -> "M14 3A6 6 0 0 0 8 11L2 17L7 22L13 16A6 6 0 0 0 21 8L17 12L12 7L16 3Z";
            case "Parts & Prices" -> "M3 7L12 2L21 7V17L12 22L3 17Z M3 7L12 12L21 7 M12 12V22 M7 4L17 9";
            case "Services", "Services & Prices" -> "M4 4H20V20H4Z M8 8H16 M8 12H16 M8 16H13";
            case "Settings" -> "M9 2H15L16 6L20 7L22 12L19 15L19 19L14 22L11 19L6 20L2 15L5 11L4 6L9 5Z M16 12A4 4 0 1 1 8 12A4 4 0 1 1 16 12";
            case "Refresh" -> "M20 7V2 M20 7H15 M20 7A9 9 0 1 0 21 15";
            case "Save" -> "M4 4H18L21 7V21H3V4Z M7 4V10H16V4 M7 21V14H17V21";
            case "Add", "New Repair Request" -> "M12 3V21 M3 12H21";
            case "View" -> "M2 12Q12 -2 22 12Q12 26 2 12 M16 12A4 4 0 1 1 8 12A4 4 0 1 1 16 12";
            case "Edit" -> "M4 16L16 4L20 8L8 20L3 21Z M13 7L17 11";
            case "Delete" -> "M3 6H21 M9 6V3H15V6 M6 6L7 21H17L18 6 M10 10V17 M14 10V17";
            case "My Repairs", "Repair Requests" -> "M6 3H18V21H6Z M9 8H15 M9 12H15 M9 16H13";
            default -> "M4 12H20 M14 6L20 12L14 18";
        };
        SVGPath glyph=new SVGPath();glyph.setContent(path);glyph.getStyleClass().add("nav-glyph");
        StackPane box=new StackPane(glyph);box.setMinSize(26,26);box.setPrefSize(26,26);box.setMaxSize(26,26);return box;
    }
    static void navigation(Button button){
        button.setGraphic(icon(button.getText()));button.setGraphicTextGap(11);button.setAccessibleText(button.getText());button.setTooltip(new Tooltip(button.getText()));
        TranslateTransition movement=new TranslateTransition(Duration.millis(140),button);
        button.setOnMouseEntered(e->{movement.stop();movement.setToX(3);movement.playFromStart();});
        button.setOnMouseExited(e->{movement.stop();movement.setToX(0);movement.playFromStart();});
    }
    static void decorate(Node node){
        if(node instanceof Button button){button.setAccessibleText(button.getText());if(button.getGraphic()==null&&!button.getText().isBlank()){button.setGraphic(icon(button.getText()));button.setGraphicTextGap(8);}if(button.getTooltip()==null)button.setTooltip(new Tooltip(button.getText()));}
        if(node instanceof Parent parent)for(Node child:parent.getChildrenUnmodifiable())decorate(child);
    }
    static void reveal(Node node){
        node.setOpacity(0);node.setTranslateY(9);
        FadeTransition fade=new FadeTransition(Duration.millis(220),node);fade.setToValue(1);
        TranslateTransition rise=new TranslateTransition(Duration.millis(220),node);rise.setToY(0);rise.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade,rise).play();
    }
}
