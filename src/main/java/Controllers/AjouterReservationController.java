// ===== 2) AjouterReservationController.java (COMPLET) =====
package Controllers;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;

public class AjouterReservationController {

    @FXML private ComboBox<String> statutRes;
    @FXML private TextField nbPersonnes;
    @FXML private TextField idUser;
    @FXML private Button btnAjouter;
    @FXML private Button btnAnnuler;

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    private int wrongAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_SECONDS = 30;

    // ✅ ID activité caché (obligatoire)
    private int activiteId = 0;

    public void setActiviteId(int activiteId) {
        this.activiteId = activiteId;
    }

    @FXML
    public void initialize() {
        statutRes.getItems().clear();
        statutRes.getItems().addAll("Confirmée", "EN_ATTENTE", "Annulée");
    }

    @FXML
    private void ajouterReservation(ActionEvent event) {

        // ✅ BLOQUER si ID activité n’a pas été injecté
        if (activiteId <= 0) {
            new Alert(Alert.AlertType.ERROR, "ID activité manquant (setActiviteId non appelé).", ButtonType.OK).showAndWait();
            return;
        }

        String statut = statutRes.getValue();
        String nbTxt = nbPersonnes.getText();
        String userTxt = idUser.getText();

        if (statut == null || nbTxt == null || nbTxt.isBlank() || userTxt == null || userTxt.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs !", ButtonType.OK).showAndWait();
            return;
        }

        int nbPers, idUserInt;
        try {
            nbPers = Integer.parseInt(nbTxt.trim());
            idUserInt = Integer.parseInt(userTxt.trim());
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Veuillez entrer des nombres valides !", ButtonType.OK).showAndWait();
            return;
        }

        if (nbPers <= 0) {
            new Alert(Alert.AlertType.WARNING, "Le nombre de personnes doit être > 0.", ButtonType.OK).showAndWait();
            return;
        }

        if (btnAjouter.isDisabled()) return;

        // ✅ éviter l'erreur FK
        if (!userExists(idUserInt)) {
            new Alert(Alert.AlertType.ERROR,
                    "ID user inexistant dans user_app.\n" +
                            "Ajoute cet user dans la table user_app ou mets un ID valide.",
                    ButtonType.OK).showAndWait();
            return;
        }

        openVerificationThenInsert(statut, nbPers, idUserInt, activiteId);
    }

    private boolean userExists(int idUserInt) {
        String sql = "SELECT 1 FROM user_app WHERE id_user = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idUserInt);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openVerificationThenInsert(String statut, int nbPers, int idUserInt, int idActiviteInt) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/Verification.fxml"));
            Parent root = loader.load();

            VerificationController vc = loader.getController();

            vc.setOnWrongAttempt(() -> {
                wrongAttempts++;
                if (wrongAttempts >= MAX_ATTEMPTS) {
                    lockAdding();
                } else {
                    int remaining = MAX_ATTEMPTS - wrongAttempts;
                    new Alert(Alert.AlertType.WARNING,
                            "Code incorrect. Il vous reste " + remaining + " tentative(s).",
                            ButtonType.OK).showAndWait();
                }
            });

            vc.setOnResult(ok -> {
                if (ok) {
                    wrongAttempts = 0;
                    insertReservationAndShowReceipt(statut, nbPers, idUserInt, idActiviteInt);
                }
            });

            Stage popup = new Stage();
            popup.setTitle("Vérification Anti-Bot");
            popup.initOwner(btnAjouter.getScene().getWindow());
            popup.setScene(new Scene(root));
            popup.setResizable(false);
            popup.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur d'ouverture de la page de vérification.", ButtonType.OK).showAndWait();
        }
    }

    private void lockAdding() {
        btnAjouter.setDisable(true);

        Alert alert = new Alert(Alert.AlertType.ERROR,
                "Trop d'erreurs ! Ajout bloqué pendant " + LOCK_SECONDS + " secondes.",
                ButtonType.OK);
        alert.showAndWait();

        PauseTransition pause = new PauseTransition(Duration.seconds(LOCK_SECONDS));
        pause.setOnFinished(ev -> {
            wrongAttempts = 0;
            btnAjouter.setDisable(false);
        });
        pause.play();
    }

    private void insertReservationAndShowReceipt(String statut, int nbPers, int idUserInt, int idActiviteInt) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            String sql = "INSERT INTO reservation_activite (statut_res, nb_personnes, id_user, id_activite) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pst.setString(1, statut);
                pst.setInt(2, nbPers);
                pst.setInt(3, idUserInt);
                pst.setInt(4, idActiviteInt);

                int result = pst.executeUpdate();

                if (result > 0) {
                    int generatedId = 0;
                    try (ResultSet rs = pst.getGeneratedKeys()) {
                        if (rs.next()) generatedId = rs.getInt(1);
                    }

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/recuReservation.fxml"));
                    Parent root = loader.load();

                    RecuReservationController controller = loader.getController();
                    controller.setData(generatedId, statut, nbPers, idUserInt, idActiviteInt);

                    Stage stage = new Stage();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Reçu de Réservation");
                    stage.show();

                    annuler();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'ajout dans la base : " + e.getMessage(), ButtonType.OK).showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'ouverture du reçu.", ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void annuler() {
        if (statutRes != null) statutRes.setValue(null);
        if (nbPersonnes != null) nbPersonnes.clear();
        if (idUser != null) idUser.clear();
    }

    @FXML
    public void btnswitch(ActionEvent event) {
        switchScene(event, "/GUI/reservation.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de retour vers : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void goUserSeances(ActionEvent event) {
        switchScene(event, "/GUI/UserSeances.fxml");
    }
}