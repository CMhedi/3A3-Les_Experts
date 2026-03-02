package GUI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class DetailsController {
    @FXML private Label lblUser, lblType;
    @FXML private TextArea txtMessage;

    public void setData(String user, String type, String message) {
        lblUser.setText(user);
        lblType.setText(type);
        txtMessage.setText(message);
    }

    @FXML
    private void closePopup() {
        Stage stage = (Stage) lblUser.getScene().getWindow();
        stage.close();
    }
}