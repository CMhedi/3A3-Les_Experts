package Controllers;

import Models.Reservation;
import Services.SendGridEmailService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;

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

        if (r.getStatut() != null) cbStatut.setValue(r.getStatut().trim().toUpperCase());
        if (spNbPersonnes.getValueFactory() != null) spNbPersonnes.getValueFactory().setValue(r.getNbPersonnes());
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
        Integer newNb = spNbPersonnes.getValue();

        if (newStatut == null || newStatut.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Please choose a status.").show();
            return;
        }
        if (newNb == null || newNb <= 0) {
            new Alert(Alert.AlertType.WARNING, "Invalid participants number.").show();
            return;
        }

        newStatut = newStatut.trim().toUpperCase();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            String sql = "UPDATE reservation_activite SET statut_res=?, nb_personnes=? WHERE id_res_act=?";

            int rows;
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, newStatut);
                pst.setInt(2, newNb);
                pst.setInt(3, reservation.getId());
                rows = pst.executeUpdate();
            }

            if (rows == 0) {
                new Alert(Alert.AlertType.ERROR, "No row updated.").show();
                return;
            }

            if (parentController != null) parentController.refreshTable();

            new Alert(Alert.AlertType.INFORMATION, "Reservation updated. Email sending...").show();

            String finalNewStatut = newStatut;
            int finalNewNb = newNb;

            // ✅ send email in background
            new Thread(() -> {
                try {
                    int idUser = reservation.getIdUser();

                    UserInfo user;

                    // ✅ nouvelle connexion
                    try (Connection newConn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                        user = fetchUserInfo(newConn, idUser);
                    }

                    if (user == null)
                        throw new RuntimeException("User not found (id_user=" + idUser + ")");

                    String subject = "EcoAdventure - Reservation update";
                    String body =
                            "Hello " + user.prenom + " " + user.nom + ",\n\n" +
                                    "Your reservation #" + reservation.getId() + " has been updated:\n" +
                                    "- Status: " + finalNewStatut + "\n" +
                                    "- Participants: " + finalNewNb + "\n\n" +
                                    "EcoAdventure Team";

                    emailService.sendPlainText(user.email, subject, body);

                    Platform.runLater(this::closeWindow);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.WARNING,
                                "Updated, but email failed:\n" + ex.getMessage()).show();
                        closeWindow();
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Update error.").show();
        }
    }

    private UserInfo fetchUserInfo(Connection conn, int idUser) throws Exception {
        String sql = "SELECT nom, prenom, email FROM user_app WHERE id_user = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserInfo(rs.getString("nom"), rs.getString("prenom"), rs.getString("email"));
                }
            }
        }
        return null;
    }

    private static class UserInfo {
        final String nom, prenom, email;
        UserInfo(String nom, String prenom, String email) {
            this.nom = nom; this.prenom = prenom; this.email = email;
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) tfIdReservation.getScene().getWindow();
        stage.close();
    }
}