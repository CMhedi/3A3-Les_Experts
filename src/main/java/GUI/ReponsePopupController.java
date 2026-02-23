package GUI;

import Entities.Reclamation;
<<<<<<< HEAD
import Services.ReclamationService;
=======
import GUI.utils.DialogUtils;
import Services.interfaces.ReclamationService;
import enums.StatutReclamation;
>>>>>>> origin/salma_integration
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

    private Runnable onSuccess;

    public void setData(Reclamation r, Runnable callback) {
        this.selectedRec = r;
        this.onSuccess = callback;
    }

    @FXML
    void handleEnvoyer(ActionEvent event) {
        if (txtReponse.getText().trim().isEmpty()) {
            DialogUtils.showError("Champs vide", "La réponse ne peut pas être vide");
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