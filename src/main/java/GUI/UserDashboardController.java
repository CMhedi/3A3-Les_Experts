package GUI;

import Entities.Seance;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.GoogleCalendarService;
import Services.ReservationSeanceService;
import Services.SeanceService;
import Services.UserService;
import Entities.Session;
import Entities.UserApp;
import enums.RoleUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDashboardController {

    @FXML private FlowPane planifieesContainer;
    @FXML private FlowPane termineesContainer;

    private final SeanceService seanceService = new SeanceService();
    private final UserService userService = new UserService();
    private final ReservationSeanceService reservationService =
            new ReservationSeanceService();

    private UserApp connectedUser;
    private int userId;

    private final Map<Integer, String> coachMap = new HashMap<>();

    @FXML
    public void initialize() {

        connectedUser = Session.getConnectedUser();

        if (connectedUser == null) {
            DialogUtils.showError(
                    "Erreur",
                    "Utilisateur non connecté."
            );
            closeWindow();
            return;
        }

        if (connectedUser.getRole() != RoleUser.USER_SIMPLE) {
            DialogUtils.showError(
                    "Accès refusé",
                    "Cette page est réservée aux utilisateurs."
            );
            closeWindow();
            return;
        }

        userId = connectedUser.getIdUser();

        loadCoachs();
        refreshCards();
    }
    private void closeWindow() {
        if (planifieesContainer != null &&
                planifieesContainer.getScene() != null) {

            Stage stage = (Stage)
                    planifieesContainer.getScene().getWindow();

            stage.close();
        }
    }
    // =================================================
    // REFRESH
    // =================================================
    private void refreshCards() {

        planifieesContainer.getChildren().clear();
        termineesContainer.getChildren().clear();

        try {

            List<Seance> all =
                    seanceService.getSeancesByUser(userId);

            for (Seance s : all) {

                if (!s.getDateSeance().isBefore(LocalDate.now())) {
                    planifieesContainer.getChildren()
                            .add(createPlanifieeCard(s));
                } else {
                    termineesContainer.getChildren()
                            .add(createTermineeCard(s));
                }
            }

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger vos séances."
            );
        }
    }

    // =================================================
    // PLANIFIEE CARD (Design amélioré)
    // =================================================
    private VBox createPlanifieeCard(Seance s) {

        VBox card = baseCard(s);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        // 🔥 Bouton Google
        Button btnVoir = new Button(" Voir dans Google Calendar");
        btnVoir.setPrefHeight(36);
        btnVoir.setMinWidth(230);
        btnVoir.getStyleClass().add("btn-google");

        ImageView googleIcon = new ImageView(
                new Image(getClass()
                        .getResourceAsStream("/images/google.png"))
        );
        googleIcon.setFitWidth(16);
        googleIcon.setFitHeight(16);

        btnVoir.setGraphic(googleIcon);
        btnVoir.setContentDisplay(ContentDisplay.LEFT);
        btnVoir.setGraphicTextGap(8);

        btnVoir.setOnAction(e -> handleVoirGoogle(s));

        // 🔥 Bouton Annuler
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setPrefHeight(36);
        btnAnnuler.setMinWidth(120);
        btnAnnuler.getStyleClass().add("btn-danger");

        btnAnnuler.setOnAction(e -> handleAnnuler(s));

        actions.getChildren().addAll(btnVoir, btnAnnuler);

        card.getChildren().add(actions);

        return card;
    }

    // =================================================
    // TERMINEE CARD
    // =================================================
    private VBox createTermineeCard(Seance s) {

        VBox card = baseCard(s);

        Label badge = new Label("Terminée");
        badge.getStyleClass().add("badge-terminee");

        card.getChildren().add(badge);

        return card;
    }

    // =================================================
    // BASE CARD STRUCTURE
    // =================================================
    private VBox baseCard(Seance s) {

        VBox card = new VBox(8);
        card.getStyleClass().add("coach-card");

        Label title = new Label(s.getNom());
        title.getStyleClass().add("coach-card-title");

        Label date = new Label("📅 " + s.getDateSeance());
        date.getStyleClass().add("coach-card-info");

        Label time = new Label("⏰ " +
                s.getHeureDebut() + " - " + s.getHeureFin());
        time.getStyleClass().add("coach-card-info");

        Label coach = new Label("👤 " +
                coachMap.getOrDefault(
                        s.getIdCoach(),
                        "Inconnu"
                ));
        coach.getStyleClass().add("coach-card-info");

        card.getChildren().addAll(title, date, time, coach);

        return card;
    }

    // =================================================
    // VOIR GOOGLE
    // =================================================
    private void handleVoirGoogle(Seance s) {

        try {

            String link = reservationService.getGoogleEventLink(
                    userId,
                    s.getIdSeance()
            );

            if (link == null || link.isBlank()) {

                DialogUtils.showWarning(
                        "Google Calendar",
                        "Aucun événement Google associé."
                );
                return;
            }

            java.awt.Desktop.getDesktop().browse(
                    new java.net.URI(link)
            );

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible d’ouvrir Google Calendar."
            );
        }
    }

    // =================================================
    // ANNULER
    // =================================================
    private void handleAnnuler(Seance selected) {

        if (selected == null) return;

        boolean confirmed =
                DialogUtils.showConfirmation(
                        "Annulation",
                        "Voulez-vous vraiment annuler cette réservation ?"
                );

        if (!confirmed) return;

        try {

            String googleEventId =
                    reservationService.getGoogleEventId(
                            userId,
                            selected.getIdSeance()
                    );

            reservationService.annuler(
                    userId,
                    selected.getIdSeance()
            );

            if (googleEventId != null && !googleEventId.isBlank()) {

                try {
                    GoogleCalendarService.deleteEvent(
                            Session.getConnectedUser().getIdUser(),
                            googleEventId
                    );
                } catch (Exception ignored) {}
            }

            DialogUtils.showInfo(
                    "Succès",
                    "Réservation annulée et supprimée du calendrier."
            );

            refreshCards();

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible d'annuler la réservation."
            );
        }
    }

    // =================================================
    // LOAD COACHS
    // =================================================
    private void loadCoachs() {

        try {
            userService.getAllCoachs().forEach(c ->
                    coachMap.put(
                            c.getIdUser(),
                            c.getNom() + " " + c.getPrenom()
                    )
            );
        } catch (Exception ignored) {}
    }

    // =================================================
    // NAVIGATION
    // =================================================
    @FXML
    private void handleRetour(ActionEvent e) {

        SceneUtils.loadScene(
                "/UserSeanceView.fxml",
                "/admin.css",
                (Node) e.getSource()
        );
    }

    @FXML
    private void handleSeances(ActionEvent event) {

        SceneUtils.loadScene(
                "/UserSeanceView.fxml",
                "/admin.css",
                (Node) event.getSource()
        );
    }
}