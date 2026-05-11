package controllers;

import Entities.Session;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;

public class AjouterReservationController {

    // ================= UI =================
    @FXML private ComboBox<String> statutRes;
    @FXML private TextField nbPersonnes;
    @FXML private Button btnAjouter;
    @FXML private Button btnAnnuler;

    // Label in your FXML (optional)
    @FXML private Label userInfoLabel;

    // ================= DB =================
    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    // ================= SESSION =================
    private UserApp connectedUser;
    private int userId;

    // ================= ANTI-BOT LOCK =================
    private int wrongAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_SECONDS = 30;

    // ================= CONTEXT =================
    private int activiteId = 0;

    public void setActiviteId(int activiteId) {
        this.activiteId = activiteId;
    }

    // =================================================
    // INITIALISATION
    // =================================================
    @FXML
    public void initialize() {

        connectedUser = Session.getConnectedUser();

        if (connectedUser == null) {
            DialogUtils.showError("Erreur", "Utilisateur non connecté.");
            disableForm();
            return;
        }

        userId = connectedUser.getIdUser();

        // ✅ عرض الاسم فقط (بلا ID)
        if (userInfoLabel != null) {
            userInfoLabel.setText(connectedUser.getNom() + " " + connectedUser.getPrenom());
        }

        initStatuts();
    }

    private void initStatuts() {
        if (statutRes == null) return;
        statutRes.getItems().clear();
        statutRes.getItems().addAll("EN_ATTENTE", "CONFIRMEE", "ANNULEE");
        statutRes.setValue("EN_ATTENTE");
    }

    private void disableForm() {
        if (btnAjouter != null) btnAjouter.setDisable(true);
        if (btnAnnuler != null) btnAnnuler.setDisable(true);
        if (nbPersonnes != null) nbPersonnes.setDisable(true);
        if (statutRes != null) statutRes.setDisable(true);
    }

    // =================================================
    // ACTION : AJOUTER
    // =================================================
    @FXML
    private void ajouterReservation(ActionEvent event) {

        if (!validateContext()) return;

        FormData data = readAndValidateForm();
        if (data == null) return;

        if (btnAjouter != null && btnAjouter.isDisabled()) return;

        // ✅ éviter erreur FK user (même si session OK)
        if (!userExists(userId)) {
            DialogUtils.showError(
                    "Utilisateur introuvable",
                    "Votre compte n'existe pas dans user_app."
            );
            return;
        }

        // ✅ vérifier capacité avant CAPTCHA
        try {
            CapacityCheck check = checkCapacityByCategory(activiteId, data.nbPersonnes);
            if (!check.ok) {
                DialogUtils.showError(
                        "Capacité dépassée",
                        "Catégorie: " + check.categorie +
                                "\nCapacité: " + check.capacite +
                                "\nRéservées: " + check.reservees +
                                "\nVotre demande: " + data.nbPersonnes +
                                "\nRestantes: " + (check.capacite - check.reservees)
                );
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Erreur vérification capacité: " + e.getMessage());
            return;
        }

        openVerificationThenInsert(data);
    }

    private boolean validateContext() {
        if (activiteId <= 0) {
            DialogUtils.showError("Erreur", "ID activité manquant (setActiviteId non appelé).");
            return false;
        }
        return true;
    }

    // =================================================
    // FORM VALIDATION
    // =================================================
    private static class FormData {
        String statut;
        int nbPersonnes;
    }

    private FormData readAndValidateForm() {

        String statut = (statutRes == null) ? null : statutRes.getValue();
        String nbTxt = (nbPersonnes == null) ? null : nbPersonnes.getText();

        if (statut == null || nbTxt == null || nbTxt.isBlank()) {
            DialogUtils.showWarning("Validation", "Veuillez remplir tous les champs !");
            return null;
        }

        int nbPers;
        try {
            nbPers = Integer.parseInt(nbTxt.trim());
        } catch (NumberFormatException e) {
            DialogUtils.showError("Validation", "Veuillez entrer un nombre valide !");
            return null;
        }

        if (nbPers <= 0) {
            DialogUtils.showWarning("Validation", "Le nombre de personnes doit être > 0.");
            return null;
        }

        FormData out = new FormData();
        out.statut = statut;
        out.nbPersonnes = nbPers;
        return out;
    }

    // =================================================
    // CAPACITY CHECK
    // =================================================
    private static class CapacityCheck {
        boolean ok;
        String categorie;
        int capacite;
        int reservees;
    }

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
                out.ok = nbPersWanted <= restantes;
                return out;
            }
        }
    }

    // =================================================
    // CAPTCHA POPUP + INSERT
    // =================================================
    private void openVerificationThenInsert(FormData data) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/Verification.fxml"));
            Parent root = loader.load();

            VerificationController vc = loader.getController();

            vc.setOnWrongAttempt(this::handleWrongAttempt);
            vc.setOnResult(ok -> {
                if (ok) {
                    wrongAttempts = 0;
                    recheckThenInsert(data);
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
            DialogUtils.showError("Erreur", "Erreur d'ouverture de la page de vérification.");
        }
    }

    private void handleWrongAttempt() {

        wrongAttempts++;

        if (wrongAttempts >= MAX_ATTEMPTS) {
            lockAdding();
            return;
        }

        int remaining = MAX_ATTEMPTS - wrongAttempts;
        DialogUtils.showWarning("Code incorrect", "Il vous reste " + remaining + " tentative(s).");
    }

    private void recheckThenInsert(FormData data) {

        try {
            CapacityCheck check = checkCapacityByCategory(activiteId, data.nbPersonnes);
            if (!check.ok) {
                DialogUtils.showError(
                        "Capacité dépassée",
                        "Capacité dépassée (après vérification).\n" +
                                "Catégorie: " + check.categorie +
                                "\nCapacité: " + check.capacite +
                                "\nRéservées: " + check.reservees +
                                "\nVotre demande: " + data.nbPersonnes
                );
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Erreur vérification capacité: " + e.getMessage());
            return;
        }

        insertReservationAndGoSeatPicker(data);
    }

    private void lockAdding() {

        btnAjouter.setDisable(true);

        DialogUtils.showError(
                "Blocage sécurité",
                "Trop d'erreurs ! Ajout bloqué pendant " + LOCK_SECONDS + " secondes."
        );

        PauseTransition pause = new PauseTransition(Duration.seconds(LOCK_SECONDS));
        pause.setOnFinished(ev -> {
            wrongAttempts = 0;
            btnAjouter.setDisable(false);
        });
        pause.play();
    }

    // =================================================
    // INSERT -> SEAT PICKER
    // =================================================
    private void insertReservationAndGoSeatPicker(FormData data) {

        String sql =
                "INSERT INTO reservation_activite " +
                        "(statut_res, nb_personnes, id_user, id_activite) " +
                        "VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setString(1, data.statut);
            pst.setInt(2, data.nbPersonnes);
            pst.setInt(3, userId);
            pst.setInt(4, activiteId);

            int result = pst.executeUpdate();

            if (result <= 0) {
                DialogUtils.showError("Erreur", "Insertion échouée.");
                return;
            }

            int generatedId = 0;
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) generatedId = rs.getInt(1);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/SetPicker.fxml"));
            Parent root = loader.load();

            SeatPickerController controller = loader.getController();
            controller.setContext(generatedId, data.statut, data.nbPersonnes, userId, activiteId);

            Stage stage = (Stage) btnAjouter.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (SQLException e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Erreur ajout réservation: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Erreur ouverture SetPicker.fxml");
        }
    }

    // =================================================
    // DB UTILS
    // =================================================
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

    // =================================================
    // UI : ANNULER
    // =================================================
    @FXML
    private void annuler() {
        if (statutRes != null) statutRes.setValue("EN_ATTENTE");
        if (nbPersonnes != null) nbPersonnes.clear();
    }

    // =================================================
    // NAVIGATION
    // =================================================
    @FXML
    public void goUserSeances(ActionEvent event) {
        SceneUtils.loadScene("/GUI/UserSeances.fxml", (javafx.scene.Node) event.getSource());
    }

    @FXML
    public void btnswitch(ActionEvent event) {
        SceneUtils.loadScene("/GUI/reservation.fxml", (javafx.scene.Node) event.getSource());
    }
}