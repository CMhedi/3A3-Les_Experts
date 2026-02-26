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

    // ===== anti-bot lock =====
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
        // ✅ EXACT enum DB
        statutRes.getItems().addAll("EN_ATTENTE", "CONFIRMEE", "ANNULEE");
        statutRes.setValue("EN_ATTENTE");
    }

    @FXML
    private void ajouterReservation(ActionEvent event) {

        if (activiteId <= 0) {
            alertErr("ID activité manquant (setActiviteId non appelé).");
            return;
        }

        String statut = statutRes.getValue();
        String nbTxt = nbPersonnes.getText();
        String userTxt = idUser.getText();

        if (statut == null || nbTxt == null || nbTxt.isBlank() || userTxt == null || userTxt.isBlank()) {
            alertWarn("Veuillez remplir tous les champs !");
            return;
        }

        int nbPers, idUserInt;
        try {
            nbPers = Integer.parseInt(nbTxt.trim());
            idUserInt = Integer.parseInt(userTxt.trim());
        } catch (NumberFormatException e) {
            alertErr("Veuillez entrer des nombres valides !");
            return;
        }

        if (nbPers <= 0) {
            alertWarn("Le nombre de personnes doit être > 0.");
            return;
        }

        if (btnAjouter.isDisabled()) return;

        // ✅ éviter erreur FK user
        if (!userExists(idUserInt)) {
            alertErr("ID user inexistant dans user_app.\nAjoute cet user dans user_app ou mets un ID valide.");
            return;
        }

        // ✅ vérifier capacité avant CAPTCHA
        try {
            CapacityCheck check = checkCapacityByCategory(activiteId, nbPers);
            if (!check.ok) {
                alertErr("Capacité dépassée pour la catégorie: " + check.categorie +
                        "\nCapacité: " + check.capacite +
                        "\nRéservées: " + check.reservees +
                        "\nVotre demande: " + nbPers +
                        "\nRestantes: " + (check.capacite - check.reservees));
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            alertErr("Erreur vérification capacité: " + e.getMessage());
            return;
        }

        openVerificationThenInsert(statut, nbPers, idUserInt, activiteId);
    }

    // ===================== CAPACITY CHECK =====================

    private static class CapacityCheck {
        boolean ok;
        String categorie;
        int capacite;
        int reservees;
    }

    // ✅ capacité/réservées sur toute la catégorie de l'activité
    // ✅ ignore ANNULEE
    private CapacityCheck checkCapacityByCategory(int idActivite, int nbPersWanted) throws SQLException {

        String sql =
                "SELECT " +
                        "  a.categorie_act AS categorie, " +
                        "  cp.capacite_totale AS capacite, " +
                        "  COALESCE(SUM(CASE WHEN r.statut_res IN ('EN_ATTENTE','CONFIRMEE') THEN r.nb_personnes ELSE 0 END), 0) AS reservees " +
                        "FROM activite a " +
                        "JOIN capacity_policy cp ON cp.categorie_act = a.categorie_act " +
                        "LEFT JOIN activite a2 ON a2.categorie_act = a.categorie_act " +
                        "LEFT JOIN reservation_activite r ON r.id_activite = a2.id_activite " +
                        "WHERE a.id_activite = ? " +
                        "GROUP BY a.categorie_act, cp.capacite_totale";

        CapacityCheck out = new CapacityCheck();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idActivite);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    out.ok = false;
                    out.categorie = "UNKNOWN";
                    out.capacite = 0;
                    out.reservees = 0;
                    return out;
                }

                out.categorie = rs.getString("categorie");
                out.capacite = rs.getInt("capacite");
                out.reservees = rs.getInt("reservees");

                int restantes = out.capacite - out.reservees;
                out.ok = (nbPersWanted <= restantes);
                return out;
            }
        }
    }

    // ===================== CAPTCHA + INSERT =====================

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

                    // ✅ re-check juste avant insert (si autre user a réservé entre temps)
                    try {
                        CapacityCheck check = checkCapacityByCategory(idActiviteInt, nbPers);
                        if (!check.ok) {
                            alertErr("Capacité dépassée (après vérification).\n" +
                                    "Catégorie: " + check.categorie +
                                    "\nCapacité: " + check.capacite +
                                    "\nRéservées: " + check.reservees +
                                    "\nVotre demande: " + nbPers);
                            return;
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        alertErr("Erreur vérification capacité: " + e.getMessage());
                        return;
                    }

                    insertReservationAndGoSeatPicker(statut, nbPers, idUserInt, idActiviteInt);
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
            alertErr("Erreur d'ouverture de la page de vérification.");
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

    // ===================== INSERT -> SEAT PICKER =====================

    private void insertReservationAndGoSeatPicker(String statut, int nbPers, int idUserInt, int idActiviteInt) {

        String sql = "INSERT INTO reservation_activite (statut_res, nb_personnes, id_user, id_activite) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/SetPicker.fxml"));
                Parent root = loader.load();

                SeatPickerController controller = loader.getController();
                controller.setContext(generatedId, statut, nbPers, idUserInt, idActiviteInt);

                Stage stage = (Stage) btnAjouter.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            alertErr("Erreur ajout réservation: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            alertErr("Erreur ouverture seatPicker.fxml");
        }
    }

    // ===================== UTILS =====================

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

    private void alertErr(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void alertWarn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
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