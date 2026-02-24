package Controllers;

import Models.TopActivite;
import Services.ActiviteService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class TopActiviteController {

    @FXML private VBox boxTop1;
    @FXML private VBox boxTop2;
    @FXML private VBox boxTop3;

    @FXML private Label t1Nom, t1Type, t1Categorie, t1Niveau, t1Nb;
    @FXML private Label t2Nom, t2Type, t2Categorie, t2Niveau, t2Nb;
    @FXML private Label t3Nom, t3Type, t3Categorie, t3Niveau, t3Nb;

    private final ActiviteService service = new ActiviteService();

    @FXML
    public void initialize() {
        try {
            List<TopActivite> top = service.getTop3Activites();

            // Hide all by default
            boxTop1.setVisible(false); boxTop1.setManaged(false);
            boxTop2.setVisible(false); boxTop2.setManaged(false);
            boxTop3.setVisible(false); boxTop3.setManaged(false);

            if (top.size() > 0) fillTop1(top.get(0));
            if (top.size() > 1) fillTop2(top.get(1));
            if (top.size() > 2) fillTop3(top.get(2));

            if (top.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Aucune réservation trouvée pour calculer le Top 3.").show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur chargement Top Clients").show();
        }
    }

    private void fillTop1(TopActivite a) {
        boxTop1.setVisible(true); boxTop1.setManaged(true);
        t1Nom.setText(a.getNom());
        t1Type.setText("Type : " + a.getTypeActivite());
        t1Categorie.setText("Catégorie : " + a.getCategorieAct());
        t1Niveau.setText("Niveau : " + a.getNiveauAct());
        t1Nb.setText("⭐ Réservations : " + a.getNbReservations());
    }

    private void fillTop2(TopActivite a) {
        boxTop2.setVisible(true); boxTop2.setManaged(true);
        t2Nom.setText(a.getNom());
        t2Type.setText("Type : " + a.getTypeActivite());
        t2Categorie.setText("Catégorie : " + a.getCategorieAct());
        t2Niveau.setText("Niveau : " + a.getNiveauAct());
        t2Nb.setText("⭐ Réservations : " + a.getNbReservations());
    }

    private void fillTop3(TopActivite a) {
        boxTop3.setVisible(true); boxTop3.setManaged(true);
        t3Nom.setText(a.getNom());
        t3Type.setText("Type : " + a.getTypeActivite());
        t3Categorie.setText("Catégorie : " + a.getCategorieAct());
        t3Niveau.setText("Niveau : " + a.getNiveauAct());
        t3Nb.setText("⭐ Réservations : " + a.getNbReservations());
    }

    @FXML
    public void goUserSeances(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/UserSeances.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}