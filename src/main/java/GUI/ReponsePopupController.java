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
    private final ReclamationService rs = new ReclamationService();

    private Runnable onSuccess;

    public void setData(Reclamation r, Runnable callback) {
        this.selectedRec = r;
        this.onSuccess = callback;
    }

    @FXML
    void handleEnvoyer(ActionEvent event) {
        String rep = (txtReponse.getText() == null) ? "" : txtReponse.getText().trim();

        if (rep.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Champs vide");
            a.setHeaderText(null);
            a.setContentText("La réponse ne peut pas être vide");
            a.show();
            return;
        }

        if (selectedRec == null) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText(null);
            a.setContentText("Aucune réclamation sélectionnée.");
            a.show();
            return;
        }

        try {
            rs.repondre(selectedRec.getIdReclamation(), rep);

            if (onSuccess != null) onSuccess.run();

            ((Stage) txtReponse.getScene().getWindow()).close();

        } catch (SQLException e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur SQL");
            a.setHeaderText(null);
            a.setContentText("Impossible d'envoyer la réponse.");
            a.show();
        }
    }

    @FXML
    void handleAnnuler() {
        ((Stage) txtReponse.getScene().getWindow()).close();
    }
}