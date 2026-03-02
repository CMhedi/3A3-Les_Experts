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
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDashboardController {

    @FXML private FlowPane planifieesContainer;
    @FXML private FlowPane termineesContainer;

    private final SeanceService seanceService = new SeanceService();
    private final Map<Integer, UserApp> coachCache = new HashMap<>();
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

        String googleLink = null;

        try {
            googleLink = reservationService.getGoogleEventLink(
                    userId,
                    s.getIdSeance()
            );
        } catch (Exception ignored) {}

        Button btnGoogle = new Button();
        btnGoogle.setPrefHeight(36);
        btnGoogle.setMinWidth(230);
        btnGoogle.getStyleClass().add("btn-google");

        ImageView googleIcon = new ImageView(
                new Image(getClass()
                        .getResourceAsStream("/images/icons8-calendrier-google-144.png"))
        );
        googleIcon.setFitWidth(16);
        googleIcon.setFitHeight(16);

        btnGoogle.setGraphic(googleIcon);
        btnGoogle.setContentDisplay(ContentDisplay.LEFT);
        btnGoogle.setGraphicTextGap(8);

        if (googleLink == null || googleLink.isBlank()) {
            btnGoogle.setText(" Ajouter au Google Calendar");
            btnGoogle.setOnAction(e -> ajouterAuCalendrier(s));
        } else {
            btnGoogle.setText(" Voir dans Google Calendar");
            btnGoogle.setOnAction(e -> handleVoirGoogle(s));
        }

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setPrefHeight(36);
        btnAnnuler.setMinWidth(120);
        btnAnnuler.getStyleClass().add("btn-danger");
        btnAnnuler.setOnAction(e -> handleAnnuler(s));

        actions.getChildren().addAll(btnGoogle, btnAnnuler);

        card.getChildren().add(actions);

        return card;
    }

    // =================================================
    // TERMINEE CARD
    // =================================================
    private VBox createTermineeCard(Seance s) {

        VBox card = baseCard(s);

        Label badge = new Label("Séance terminée");
        badge.getStyleClass().add("badge-terminee");

        card.getChildren().add(badge);

        return card;
    }
    // =================================================
    // BASE CARD STRUCTURE
    // =================================================
    private VBox baseCard(Seance s) {

        VBox card = new VBox(12);
        card.getStyleClass().add("coach-card");
        card.setPrefWidth(320);

        // ================= TITLE =================
        Label title = new Label(s.getNom());
        title.getStyleClass().add("coach-card-title");

        // ================= DATE / TIME =================
        Label date = new Label("📅 " + s.getDateSeance());
        date.getStyleClass().add("coach-card-info");

        Label time = new Label("⏰ " +
                s.getHeureDebut() + " - " + s.getHeureFin());
        time.getStyleClass().add("coach-card-info");

        // ================= COACH AVEC IMAGE =================
        HBox coachRow = new HBox(8);
        coachRow.setAlignment(Pos.CENTER_LEFT);

        UserApp coachUser = coachCache.computeIfAbsent(
                s.getIdCoach(),
                id -> {
                    try {
                        return userService.getById(id);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        String imagePath = coachUser != null
                ? coachUser.getImageUrl()
                : null;

        Image image;

        try {
            if (imagePath != null && !imagePath.isBlank()) {
                image = new Image(imagePath, true);
            } else {
                image = new Image(getClass()
                        .getResource("/gui/default-user.png")
                        .toExternalForm());
            }
        } catch (Exception e) {
            image = new Image(getClass()
                    .getResource("/gui/default-user.png")
                    .toExternalForm());
        }

        ImageView coachImage = new ImageView(image);
        coachImage.setFitWidth(30);
        coachImage.setFitHeight(30);

        Circle clip = new Circle(15, 15, 15);
        coachImage.setClip(clip);

        coachImage.getStyleClass().add("coach-avatar");

        Label coachName = new Label(
                coachUser != null
                        ? coachUser.getNom() + " " + coachUser.getPrenom()
                        : "Inconnu"
        );
        coachName.getStyleClass().add("coach-card-info");

        coachRow.getChildren().addAll(coachImage, coachName);

        // ================= BUILD =================
        card.getChildren().addAll(
                title,
                date,
                time,
                coachRow
        );

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
    private void ajouterAuCalendrier(Seance s) {

        try {

            var start = java.time.LocalDateTime.of(
                    s.getDateSeance(),
                    s.getHeureDebut()
            );

            var end = java.time.LocalDateTime.of(
                    s.getDateSeance(),
                    s.getHeureFin()
            );

            GoogleCalendarService.GoogleEventData data =
                    GoogleCalendarService.addEvent(
                            userId,
                            "Séance : " + s.getNom(),
                            "Séance EcoAdventure",
                            start,
                            end
                    );

            reservationService.saveGoogleEventId(
                    userId,
                    s.getIdSeance(),
                    data.id
            );

            reservationService.saveGoogleEventLink(
                    userId,
                    s.getIdSeance(),
                    data.htmlLink
            );

            DialogUtils.showInfo(
                    "Google Calendar",
                    "Événement ajouté au calendrier."
            );

            refreshCards();

        } catch (Exception e) {

            DialogUtils.showError(
                    "Google Calendar",
                    "Impossible d'ajouter au calendrier."
            );
        }
    }
}