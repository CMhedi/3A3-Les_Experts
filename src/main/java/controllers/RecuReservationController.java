package controllers;

import Services.interfaces.LocalTicketServer;
import Services.interfaces.TicketApiClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class RecuReservationController {

    // ================= UI =================
    @FXML private Label lblId;
    @FXML private Label lblStatut;
    @FXML private Label lblNb;

    // Ticket
    @FXML private Label lblToken;
    @FXML private ImageView qrView;

    // ================= STATE =================
    private int reservationId;

    // Keep same signature (even if we don't display user/activite)
    public void setData(int id, String statut, int nb, int user, int activite) {

        this.reservationId = id;

        if (lblId != null) lblId.setText("Numéro de réservation : " + id);
        if (lblStatut != null) lblStatut.setText("Statut : " + statut);
        if (lblNb != null) lblNb.setText("Nombre de personnes : " + nb);

        // Default
        if (lblToken != null) lblToken.setText("Ticket : (non généré)");
        if (qrView != null) qrView.setImage(null);

        // ✅ Start local ticket server safely (no crash if already started)
        try {
            LocalTicketServer.startOnce();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.WARNING,
                    "Serveur ticket déjà démarré ou port occupé.\n" +
                            "Le QR peut quand même être généré si l'API fonctionne.\n\n" +
                            "Détail: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    // =================================================
    // GENERER QR
    // =================================================
    @FXML
    private void genererQr(ActionEvent event) {
        try {
            TicketApiClient api = new TicketApiClient();
            TicketApiClient.GenerateResp resp = api.generate(reservationId);

            if (lblToken != null) lblToken.setText("Ticket Token : " + resp.token);

            byte[] png = Base64.getDecoder().decode(resp.pngBase64);
            if (qrView != null) qrView.setImage(new Image(new ByteArrayInputStream(png)));

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

    // =================================================
    // NAVIGATION
    // =================================================
    @FXML
    private void retour(ActionEvent event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
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