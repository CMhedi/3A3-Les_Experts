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
    @FXML private TextField idActivite;
    @FXML private Button btnAjouter;
    @FXML private Button btnAnnuler;

    // cnx de bd
    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    // ===== Anti-bot settings =====
    private int wrongAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_SECONDS = 30;

    @FXML
    public void initialize() {
        statutRes.getItems().addAll("Confirmée", "EN_ATTENTE", "Annulée");
    }

    // ✅ Branche ce handler sur ton bouton Ajouter (onAction="#ajouterReservation")
    @FXML
    private void ajouterReservation() {
        // 1) Vérifier champs minimum (tu avais commenté, je garde soft)
        String statut = statutRes.getValue();
        String nb = nbPersonnes.getText();
        String userId = idUser.getText();
        String activiteId = idActivite.getText();

        if (statut == null || nb == null || nb.isBlank() || userId == null || userId.isBlank() || activiteId == null || activiteId.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs !", ButtonType.OK).showAndWait();
            return;
        }

        // 2) Vérif nombres
        int nbPers, idUserInt, idActiviteInt;
        try {
            nbPers = Integer.parseInt(nb.trim());
            idUserInt = Integer.parseInt(userId.trim());
            idActiviteInt = Integer.parseInt(activiteId.trim());
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Veuillez entrer des nombres valides pour le nombre de personnes, l'ID utilisateur et l'ID activité !",
                    ButtonType.OK).showAndWait();
            return;
        }

        // 3) Si déjà bloqué -> ignore
        if (btnAjouter.isDisabled()) return;

        // 4) Ouvrir vérification anti-bot avant insert
        openVerificationThenInsert(statut, nbPers, idUserInt, idActiviteInt);
    }

    private void openVerificationThenInsert(String statut, int nbPers, int idUserInt, int idActiviteInt) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/Verification.fxml"));
            Parent root = loader.load();

            VerificationController vc = loader.getController();

            // ✅ à chaque erreur captcha
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

            // ✅ si validé
            vc.setOnResult(ok -> {
                if (ok) {
                    // reset compteur après succès
                    wrongAttempts = 0;

                    // insert + reçu
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
                    controller.setData(
                            generatedId,
                            statut,
                            nbPers,
                            idUserInt,
                            idActiviteInt
                    );

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
        statutRes.setValue(null);
        nbPersonnes.clear();
        idUser.clear();
        idActivite.clear();
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