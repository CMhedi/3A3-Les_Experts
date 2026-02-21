package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage; // <--- C'EST CET IMPORT QUI EST CRUCIAL

public class CallController {
    @FXML private Label callerName;
    @FXML private Label callStatus;
    private Stage stage;

    // Assure-toi que la signature est exactement celle-là
    public void setCallerData(String name, Stage stage) {
        this.callerName.setText(name);
        this.stage = stage;
    }

    @FXML
    private void handleAccept() {
        callStatus.setText("Appel en cours...");
        callStatus.setStyle("-fx-text-fill: #34c759;");
    }

    @FXML
    private void handleReject() {
        if (stage != null) {
            stage.close();
        }
    }
}