package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class RecuReservationController {

    @FXML private Label lblId;
    @FXML private Label lblDate;
    @FXML private Label lblStatut;
    @FXML private Label lblNb;
    @FXML private Label lblUser;
    @FXML private Label lblActivite;

    public void setData(int id, String date, String statut,
                        int nb, int user, int activite) {

        lblId.setText("ID Réservation : " + id);
        lblDate.setText("Date : " + date);
        lblStatut.setText("Statut : " + statut);
        lblNb.setText("Nombre de personnes : " + nb);
        lblUser.setText("ID User : " + user);
        lblActivite.setText("ID Activité : " + activite);
    }

    @FXML
    private void retour(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }
}
