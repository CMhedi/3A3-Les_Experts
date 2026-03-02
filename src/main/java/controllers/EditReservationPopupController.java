package controllers;

import Models.Reservation;
import Services.interfaces.SendGridEmailService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EditReservationPopupController {

    @FXML private ComboBox<String> cbStatut;
    @FXML private Spinner<Integer> spNbPersonnes;
    @FXML private TextField tfIdReservation;
    @FXML private TextField tfIdActivite;

    private Reservation reservation;
    private ReservationController parentController;

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    private final SendGridEmailService emailService = new SendGridEmailService();

    @FXML
    public void initialize() {
        cbStatut.getItems().setAll("EN_ATTENTE", "CONFIRMEE", "ANNULEE");
        SpinnerValueFactory<Integer> vf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1);
        spNbPersonnes.setValueFactory(vf);
        spNbPersonnes.setEditable(true);
    }

    public void setReservation(Reservation r) {
        this.reservation = r;
        tfIdReservation.setText(String.valueOf(r.getId()));
        tfIdActivite.setText(String.valueOf(r.getIdActivite()));
        cbStatut.setValue(r.getStatut());
        spNbPersonnes.getValueFactory().setValue(r.getNbPersonnes());
    }

    public void setParentController(ReservationController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    @FXML
    private void onSave() {

        if (reservation == null) {
            new Alert(Alert.AlertType.ERROR, "No reservation loaded.").show();
            return;
        }

        String newStatut = cbStatut.getValue();
        int newNb = spNbPersonnes.getValue();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            String sql = "UPDATE reservation_activite SET statut_res=?, nb_personnes=? WHERE id_res_act=?";

            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, newStatut);
                pst.setInt(2, newNb);
                pst.setInt(3, reservation.getId());
                pst.executeUpdate();
            }

            if (parentController != null)
                parentController.refreshTable();

            new Thread(() -> {
                try {
                    UserInfo user;

                    try (Connection newConn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                        user = fetchUserInfo(newConn, reservation.getIdUser());
                    }

                    if (user == null)
                        throw new RuntimeException("User not found");

                    String subject = "EcoAdventure - Reservation Updated";

                    String html = buildEmailHtml(
                            user.prenom,
                            user.nom,
                            reservation.getId(),
                            newStatut,
                            newNb
                    );

                    emailService.sendHtml(user.email, subject, html);

                    Platform.runLater(this::closeWindow);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Update error.").show();
        }
    }

    private String buildEmailHtml(String prenom, String nom, int id, String statut, int nb) {

        String badgeColor = statut.equals("CONFIRMEE") ? "#16a34a" :
                statut.equals("ANNULEE") ? "#dc2626" : "#f59e0b";

        return """
        <html>
        <body style="font-family:Arial;background:#f5f7fa;padding:20px;">
          <div style="max-width:600px;margin:auto;background:white;border-radius:12px;padding:20px;box-shadow:0 6px 20px rgba(0,0,0,0.1);">
            
            <h2 style="margin-top:0;">EcoAdventure</h2>
            
            <p>Bonjour <strong>%s %s</strong>,</p>
            
            <p>Votre réservation a été mise à jour.</p>
            
            <div style="margin:15px 0;padding:10px;border-radius:8px;background:%s20;color:%s;font-weight:bold;">
                Statut : %s
            </div>
            
            <table style="width:100%%;border-collapse:collapse;">
                <tr>
                    <td style="padding:8px;border-bottom:1px solid #eee;">Réservation ID</td>
                    <td style="padding:8px;border-bottom:1px solid #eee;"><strong>#%d</strong></td>
                </tr>
                <tr>
                    <td style="padding:8px;">Participants</td>
                    <td style="padding:8px;"><strong>%d</strong></td>
                </tr>
            </table>
            
            <p style="margin-top:20px;font-size:12px;color:#888;">
                Ceci est un email automatique.
            </p>
          </div>
        </body>
        </html>
        """.formatted(prenom, nom, badgeColor, badgeColor, statut, id, nb);
    }

    private UserInfo fetchUserInfo(Connection conn, int idUser) throws Exception {
        String sql = "SELECT nom, prenom, email FROM user_app WHERE id_user = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserInfo(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email")
                    );
                }
            }
        }
        return null;
    }

    private static class UserInfo {
        final String nom, prenom, email;
        UserInfo(String nom, String prenom, String email) {
            this.nom = nom;
            this.prenom = prenom;
            this.email = email;
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) tfIdReservation.getScene().getWindow();
        stage.close();
    }
}