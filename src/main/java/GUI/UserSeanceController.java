package GUI;

import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.GoogleCalendarService;
import Services.ReservationSeanceService;
import Services.SeanceService;
import Services.UserService;
import exceptions.ValidationException;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ContentDisplay;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Entities.Session;
import enums.RoleUser;
import javafx.stage.Stage;

public class UserSeanceController {

    // ================= UI =================
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final ReservationSeanceService reservationService =
            new ReservationSeanceService();
    private final UserService userService = new UserService();

    private UserApp connectedUser;
    private int userId;

    private List<Seance> masterData;
    private final Map<Integer, String> coachMap = new HashMap<>();

    // =================================================
    // INITIALISATION
    // =================================================
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

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            refreshCards();
        });

        loadData();
    }
    private void closeWindow() {
        if (cardContainer != null && cardContainer.getScene() != null) {
            Stage stage = (Stage) cardContainer.getScene().getWindow();
            stage.close();
        }
    }

    // =================================================
    // LOAD DATA
    // =================================================
    private void loadData() {

        try {
            masterData = seanceService.getAll();
            refreshCards();
        } catch (Exception e) {
            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les séances."
            );
        }
    }

    private void loadCoachs() {

        try {
            List<UserApp> coachs = userService.getAllCoachs();

            for (UserApp c : coachs) {
                coachMap.put(
                        c.getIdUser(),
                        c.getNom() + " " + c.getPrenom()
                );
            }

        } catch (Exception e) {
            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les coachs."
            );
        }
    }

    // =================================================
    // REFRESH CARDS
    // =================================================
    private void refreshCards() {

        cardContainer.getChildren().clear();

        if (masterData == null) return;

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase();

        for (Seance s : masterData) {

            boolean matches =
                    s.getNom().toLowerCase().contains(search)
                            || s.getDateSeance().toString().contains(search)
                            || coachMap.getOrDefault(
                            s.getIdCoach(), ""
                    ).toLowerCase().contains(search);

            if (matches) {
                cardContainer.getChildren().add(createCard(s));
            }
        }
    }

    // =================================================
    // CREATE CARD
    // =================================================
    private VBox createCard(Seance s) {

        VBox card = new VBox(15);
        card.getStyleClass().add("seance-card-pro");

        // ================= TITLE =================
        Label title = new Label(s.getNom());
        title.getStyleClass().add("seance-title-pro");

        // ================= INFOS =================
        VBox infoBox = new VBox(6);

        Label date = new Label("📅  " + s.getDateSeance());
        Label time = new Label("⏰  "
                + s.getHeureDebut() + " - " + s.getHeureFin());
        Label coach = new Label("👤  "
                + coachMap.getOrDefault(
                s.getIdCoach(), "Inconnu"));

        date.getStyleClass().add("seance-info-pro");
        time.getStyleClass().add("seance-info-pro");
        coach.getStyleClass().add("seance-info-pro");

        infoBox.getChildren().addAll(date, time, coach);

        // ================= CAPACITE =================
        int reserved = 0;
        try {
            reserved = reservationService.countReservations(s.getIdSeance());
        } catch (Exception ignored) {}

        int restantes = s.getCapacite() - reserved;

        ProgressBar progress =
                new ProgressBar((double) reserved / s.getCapacite());
        progress.setPrefHeight(8);
        progress.getStyleClass().add("places-progress");

        Label places =
                new Label(restantes + " places restantes");
        places.getStyleClass().add("places-label");

        // ================= FOOTER =================
        VBox footer = new VBox(8);

        Label badge = new Label();
        badge.getStyleClass().add("badge-pro");

        Button actionBtn = new Button();
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setPrefHeight(38);

        // ================= LOGIQUE =================
        if (s.getDateSeance().isBefore(LocalDate.now())) {

            badge.setText("Terminée");
            badge.getStyleClass().add("badge-grey");

            actionBtn.setText("Terminée");
            actionBtn.setDisable(true);
        }
        else if (restantes <= 0) {

            badge.setText("Complet");
            badge.getStyleClass().add("badge-red");

            actionBtn.setText("Complet");
            actionBtn.setDisable(true);
        }
        else {

            boolean dejaReserve =
                    reservationService.exists(
                            userId,
                            s.getIdSeance()
                    );

            if (dejaReserve) {

                badge.setText("Réservée");
                badge.getStyleClass().add("badge-blue");

                actionBtn.setText(" Voir dans Google Calendar");
                actionBtn.getStyleClass().add("btn-google");

                ImageView googleIcon = new ImageView(
                        new Image(getClass()
                                .getResource("/images/google.png")
                                .toExternalForm())
                );
                googleIcon.setFitWidth(16);
                googleIcon.setFitHeight(16);

                actionBtn.setGraphic(googleIcon);
                actionBtn.setContentDisplay(ContentDisplay.LEFT);
                actionBtn.setGraphicTextGap(8);

                actionBtn.setOnAction(e -> ouvrirGoogleCalendar(s));

            } else {

                badge.setText("Disponible");
                badge.getStyleClass().add("badge-green");

                actionBtn.setText("Réserver");
                actionBtn.getStyleClass().add("btn-primary");

                actionBtn.setOnAction(e -> reserver(s));
            }
        }

        footer.getChildren().addAll(badge, actionBtn);

        card.getChildren().addAll(
                title,
                infoBox,
                progress,
                places,
                footer
        );

        return card;
    }

    // =================================================
    // GOOGLE
    // =================================================
    private void ouvrirGoogleCalendar(Seance s) {

        try {

            String link =
                    reservationService.getGoogleEventLink(
                            userId,
                            s.getIdSeance()
                    );

            if (link == null || link.isBlank()) {
                DialogUtils.showWarning(
                        "Google Calendar",
                        "Aucun événement associé."
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
    // RESERVER
    // =================================================
    private void reserver(Seance s) {

        try {

            boolean confirmed =
                    DialogUtils.showConfirmation(
                            "Réserver la séance",
                            "Confirmez-vous la réservation ?"
                    );

            if (!confirmed) return;

            reservationService.reserver(
                    userId,
                    s.getIdSeance()
            );
            LocalDateTime start =
                    LocalDateTime.of(
                            s.getDateSeance(),
                            s.getHeureDebut()
                    );

            LocalDateTime end =
                    LocalDateTime.of(
                            s.getDateSeance(),
                            s.getHeureFin()
                    );

            GoogleCalendarService.GoogleEventData data =
                    GoogleCalendarService.addEvent(
                            Session.getConnectedUser().getIdUser(),   // 🔥 IMPORTANT
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
                    "Succès",
                    "Séance réservée."
            );

            loadData();

        } catch (ValidationException e) {

            DialogUtils.showWarning(
                    "Validation",
                    e.getMessage()
            );

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de réserver."
            );
        }
    }

    // =================================================
    // NAVIGATION
    // =================================================
    @FXML
    private void handleRetour(javafx.event.ActionEvent event) {

        SceneUtils.loadScene(
                "/gui/MainLayoutUser.fxml",
                (javafx.scene.Node) event.getSource()
        );
    }

    @FXML
    private void handleDashboard(javafx.event.ActionEvent event) {

        SceneUtils.loadScene(
                "/UserDashboardView.fxml",
                "/admin.css",
                (javafx.scene.Node) event.getSource()
        );
    }
}