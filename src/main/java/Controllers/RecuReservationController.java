package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class RecuReservationController {

    @FXML private Label lblId;
  //  @FXML private Label lblDate;
    @FXML private Label lblStatut;
    @FXML private Label lblNb;
    @FXML private Label lblUser;
    @FXML private Label lblActivite;

    public void setData(int id, String statut,
                        int nb, int user, int activite) {

        lblId.setText("Numero De Réservation : " + id);
       // lblDate.setText("Date : " + date);
        lblStatut.setText("Statut : " + statut);
        lblNb.setText("Nombre de personnes : " + nb);
        lblUser.setText("NUM Compte : " + user);
        lblActivite.setText("Num Recu Activité : " + activite);
    }

    @FXML
    private void retour(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }


    @FXML
    private void fermer(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/UserSeances.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }


