package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import Services.LocalTicketServer;
import Services.TicketApiClient;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class RecuReservationController {

    @FXML private Label lblId;
    @FXML private Label lblStatut;
    @FXML private Label lblNb;
    @FXML private Label lblUser;
    @FXML private Label lblActivite;

    // NEW
    @FXML private Label lblToken;
    @FXML private ImageView qrView;

    private int reservationId;

    public void setData(int id, String statut,
                        int nb, int user, int activite) {

        this.reservationId = id;

        lblId.setText("Numero De Réservation : " + id);
        lblStatut.setText("Statut : " + statut);
        lblNb.setText("Nombre de personnes : " + nb);
        lblUser.setText("NUM Compte : " + user);
        lblActivite.setText("Num Recu Activité : " + activite);

        // NEW default
        if (lblToken != null) lblToken.setText("Ticket : (non généré)");
        if (qrView != null) qrView.setImage(null);

        // Démarre serveur ticket local (au cas où)
        LocalTicketServer.startOnce();
    }

    // NEW
    @FXML
    private void genererQr(ActionEvent event) {
        try {
            TicketApiClient api = new TicketApiClient();
            TicketApiClient.GenerateResp resp = api.generate(reservationId);

            lblToken.setText("Ticket Token : " + resp.token);

            byte[] png = Base64.getDecoder().decode(resp.pngBase64);
            qrView.setImage(new Image(new ByteArrayInputStream(png)));

            new Alert(Alert.AlertType.INFORMATION,
                    "QR Ticket généré ✅\nScanne-le à l'entrée.",
                    ButtonType.OK).showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Erreur génération QR : " + e.getMessage()
                            + "\n⚠️ Vérifie que le statut est CONFIRMEE.",
                    ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void retour(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }
    @FXML
    private void openCheckIn(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/checkin.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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