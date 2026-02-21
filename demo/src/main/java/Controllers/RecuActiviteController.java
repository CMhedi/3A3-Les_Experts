package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class RecuActiviteController {

    @FXML private Label lblType;
    @FXML private Label lblNom;
    @FXML private Label lblCategorie;
    @FXML private Label lblNiveau;
    @FXML private Label lblPrix;
    @FXML private Label lblStatut;

    public void setData(String Type, String nom, String categorie,
                        String niveau, String prix, String statut) {

        lblType.setText("Type : " + Type);
        lblNom.setText("Nom : " + nom);
        lblCategorie.setText("Catégorie : " + categorie);
        lblNiveau.setText("Niveau : " + niveau);
        lblPrix.setText("Prix : " + prix + " DT");
        lblStatut.setText("Statut : " + statut);
    }

    @FXML
    private void fermer(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }
}
