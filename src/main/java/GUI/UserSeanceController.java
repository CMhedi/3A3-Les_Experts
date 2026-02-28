package GUI;

import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.GoogleCalendarService;
import Services.interfaces.ReservationSeanceService;
import Services.interfaces.SeanceService;
import Services.interfaces.UserService;
import enums.StatutSeance;
import exceptions.ValidationException;
import Services.interfaces.RecommendationServiceUser;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
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
import java.util.Set;

import Entities.Session;
import enums.RoleUser;
import javafx.stage.Stage;

public class UserSeanceController {

    // ================= UI =================
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;

    // ================= SERVICES =================
    private final RecommendationServiceUser recommendationService =
            new RecommendationServiceUser();
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

            for (Seance s : masterData) {
                seanceService.updateSeanceToTermineeIfNeeded(s);
            }

            masterData = seanceService.getAll();

            refreshCards();

        } catch (Exception e) {
            e.printStackTrace();   // 🔥 AJOUTE ÇA
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

        if (masterData == null || masterData.isEmpty()) {

            Label empty =
                    new Label("📭 Aucune séance disponible pour le moment.");
            empty.getStyleClass().add("reco-empty");

            cardContainer.getChildren().add(empty);
            return;
        }

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase();

        try {

            // 🔥 1️⃣ Récupérer recommandations IA
            List<Seance> recommended =
                    recommendationService.recommendForUser(userId);

            if (recommended == null)
                recommended = List.of();

            // 🔥 2️⃣ Garder uniquement les séances valides
            List<Seance> validRecommended = recommended.stream()
                    .filter(s -> s.getStatutSeance() == StatutSeance.PLANIFIEE)
                    .filter(s -> matchesSearch(s, search))
                    .collect(java.util.stream.Collectors.toList());

            Set<Integer> recommendedIds = validRecommended.stream()
                    .map(Seance::getIdSeance)
                    .collect(java.util.stream.Collectors.toSet());

            // ⭐ SECTION RECOMMANDÉES
            if (!validRecommended.isEmpty()) {

                Label recoTitle =
                        new Label("⭐ Recommandées pour vous");
                recoTitle.getStyleClass().add("reco-title");

                cardContainer.getChildren().add(recoTitle);

                for (Seance s : validRecommended) {
                    cardContainer.getChildren()
                            .add(createCard(s, true));
                }

                cardContainer.getChildren().add(new Separator());

            } else {

                // 🔵 OPTION UX : afficher message intelligent
                Label noReco =
                        new Label("🤖 Aucune recommandation personnalisée pour le moment.\n"
                                + "Réservez des séances pour obtenir des suggestions adaptées !");
                noReco.getStyleClass().add("reco-empty");

                cardContainer.getChildren().add(noReco);
                cardContainer.getChildren().add(new Separator());
            }

            // 📋 AUTRES SÉANCES DISPONIBLES
            for (Seance s : masterData) {

                if (recommendedIds.contains(s.getIdSeance()))
                    continue;

                if (s.getStatutSeance() != StatutSeance.PLANIFIEE)
                    continue;

                if (matchesSearch(s, search)) {
                    cardContainer.getChildren()
                            .add(createCard(s, false));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private boolean matchesSearch(Seance s, String search) {

        return s.getNom().toLowerCase().contains(search)
                || s.getDateSeance().toString().contains(search)
                || coachMap.getOrDefault(
                s.getIdCoach(), ""
        ).toLowerCase().contains(search);
    }
    // =================================================
    // CREATE CARD
    // =================================================
    private VBox createCard(Seance s, boolean recommended) {

        VBox card = new VBox(15);
        card.getStyleClass().add("seance-card-pro");

        if (recommended) {
            card.getStyleClass().add("recommended-card");
        }

        // ================= HEADER (Title + Reco badge) =================
        VBox headerBox = new VBox(5);

        Label title = new Label(s.getNom());
        title.getStyleClass().add("seance-title-pro");

        headerBox.getChildren().add(title);

        if (recommended) {
            Label recoBadge = new Label("⭐ Recommandé pour vous");
            recoBadge.getStyleClass().add("reco-badge");
            headerBox.getChildren().add(recoBadge);
        }

        // ================= INFOS =================
        VBox infoBox = new VBox(6);

        Label date = new Label("📅  " + s.getDateSeance());
        Label time = new Label("⏰  " + s.getHeureDebut() + " - " + s.getHeureFin());
        Label coach = new Label("👤  " + coachMap.getOrDefault(
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

        Label places = new Label(restantes + " places restantes");
        places.getStyleClass().add("places-label");

        // ================= FOOTER =================
        VBox footer = new VBox(8);

        Label badge = new Label();
        badge.getStyleClass().add("badge-pro");

        Button actionBtn = new Button();
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setPrefHeight(38);

        // ================= LOGIQUE METIER =================
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime =
                LocalDateTime.of(
                        s.getDateSeance(),
                        s.getHeureDebut()
                );

        boolean dejaReserve =
                reservationService.exists(
                        userId,
                        s.getIdSeance()
                );

        // 🔴 TERMINÉE
        if (s.getStatutSeance() == StatutSeance.TERMINEE) {

            badge.setText("Terminée");
            badge.getStyleClass().add("badge-grey");

            actionBtn.setText("Terminée");
            actionBtn.setDisable(true);
        }

        // 🔴 ANNULÉE
        else if (s.getStatutSeance() == StatutSeance.ANNULEE) {

            badge.setText("Annulée");
            badge.getStyleClass().add("badge-red");

            actionBtn.setText("Annulée");
            actionBtn.setDisable(true);
        }

        // 🔴 Déjà commencée
        else if (!now.isBefore(startDateTime)) {

            badge.setText("Déjà commencée");
            badge.getStyleClass().add("badge-grey");

            actionBtn.setText("Indisponible");
            actionBtn.setDisable(true);
        }

        // 🔴 Complet
        else if (restantes <= 0) {

            badge.setText("Complet");
            badge.getStyleClass().add("badge-red");

            actionBtn.setText("Complet");
            actionBtn.setDisable(true);
        }

        // 🔵 Déjà réservé
        else if (dejaReserve) {

            badge.setText("Réservée");
            badge.getStyleClass().add("badge-blue");

            String googleLink = null;

            try {
                googleLink = reservationService.getGoogleEventLink(
                        userId,
                        s.getIdSeance()
                );
            } catch (Exception ignored) {}

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
            actionBtn.getStyleClass().add("btn-google");

            if (googleLink == null || googleLink.isBlank()) {

                actionBtn.setText(" Ajouter au Google Calendar");
                actionBtn.setOnAction(e -> ajouterAuCalendrier(s));

            } else {

                actionBtn.setText(" Voir dans Google Calendar");
                actionBtn.setOnAction(e -> ouvrirGoogleCalendar(s));
            }
        }

        // 🟢 Disponible
        else {

            badge.setText("Disponible");
            badge.getStyleClass().add("badge-green");

            actionBtn.setText("Réserver");
            actionBtn.getStyleClass().add("btn-primary");

            actionBtn.setOnAction(e -> reserver(s));
        }

        footer.getChildren().addAll(badge, actionBtn);

        // ================= BUILD CARD =================
        card.getChildren().addAll(
                headerBox,
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

            LocalDateTime now = LocalDateTime.now();

            LocalDateTime startDateTime =
                    LocalDateTime.of(
                            s.getDateSeance(),
                            s.getHeureDebut()
                    );

            // 🔒 1️⃣ Bloquer si statut non PLANIFIEE
            if (s.getStatutSeance() != StatutSeance.PLANIFIEE) {

                DialogUtils.showWarning(
                        "Réservation impossible",
                        "Cette séance n'est plus disponible."
                );
                return;
            }

            // 🔒 2️⃣ Bloquer si déjà commencée
            if (!now.isBefore(startDateTime)) {

                DialogUtils.showWarning(
                        "Réservation impossible",
                        "La séance a déjà commencé."
                );
                return;
            }

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
    private void ajouterAuCalendrier(Seance s) {

        try {

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

            loadData();

        } catch (Exception e) {

            DialogUtils.showError(
                    "Google Calendar",
                    "Impossible d'ajouter au calendrier."
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