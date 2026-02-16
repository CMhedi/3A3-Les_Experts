package gui;

import Entities.Reclamation;
import Services.interfaces.ReclamationService;
import enums.StatutReclamation;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.sql.SQLException;

public class ReponsePopupController {
    @FXML private TextArea txtReponse;

    private Reclamation selectedRec;
    private ReclamationService rs = new ReclamationService();
    private Runnable onRefresh; // Bach na3mlou refresh lel table ba3d el reponse

    public void setData(Reclamation r, Runnable callback) {
        this.selectedRec = r;
        this.onRefresh = callback;
    }

    @FXML
    void handleEnvoyer() {
        if (txtReponse.getText().trim().isEmpty()) return;
        try {
            // ✅ On utilise la méthode repondre du service
            rs.repondre(selectedRec.getIdReclamation(), txtReponse.getText());

            onRefresh.run(); // Refreshes the TableView in Admin Controller
            handleAnnuler(); // Closes the popup
        } catch (SQLException e) { e.printStackTrace(); }
    }
    @FXML
    void handleAnnuler() {
        ((Stage) txtReponse.getScene().getWindow()).close();
    }
}