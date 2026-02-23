package GUI;

import Entities.Reclamation;
import Services.ReclamationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.sql.SQLException;

public class ReponsePopupController {
    @FXML private TextArea txtReponse;

    private Reclamation selectedRec;
    private ReclamationService rs = new ReclamationService();

    // 1. Khalli esm wa7ed barka bech ma t-enghalatch
    private Runnable onSuccess;

    public void setData(Reclamation r, Runnable callback) {
        this.selectedRec = r;
        this.onSuccess = callback; // Hna t-3abbi el onSuccess s7i7
    }

    @FXML
    void handleEnvoyer(ActionEvent event) {
        if (txtReponse.getText().trim().isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "La réponse ne peut pas être vide").show();
            return;
        }
        try {

            rs.repondre(selectedRec.getIdReclamation(), txtReponse.getText());

            if (onSuccess != null) {
                onSuccess.run();
            }

            ((Stage)txtReponse.getScene().getWindow()).close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAnnuler() {
        ((Stage) txtReponse.getScene().getWindow()).close();
    }
}