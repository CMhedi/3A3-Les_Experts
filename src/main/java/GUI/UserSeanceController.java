package GUI;

import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.GoogleCalendarService;
import Services.ReservationSeanceService;
import Services.SeanceService;
import Services.UserService;
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

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Entities.Session;
import enums.RoleUser;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class UserSeanceController {

    // ================= UI =================
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;
    @FXML
    private ImageView userImageView;

    @FXML
    private Label userNameLabel;
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
    private final Map<Integer, UserApp> coachCache = new HashMap<>();
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
        loadUserHeader();
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
            empty.getStyleClass().add("empty-state");

            cardContainer.getChildren().add(empty);
            return;
        }

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase();

        try {

            // 🔥 1️⃣ Récupérer recommandations IA (ScoredSeance)
            List<RecommendationServiceUser.ScoredSeance> recommended =
                    recommendationService.recommendForUser(userId);

            if (recommended == null)
                recommended = List.of();

            // 🔥 2️⃣ Garder uniquement séances valides + recherche
            List<RecommendationServiceUser.ScoredSeance> validRecommended =
                    recommended.stream()
                            .filter(r -> r.seance.getStatutSeance() == StatutSeance.PLANIFIEE)
                            .filter(r -> matchesSearch(r.seance, search))
                            .filter(r -> !reservationService.exists(userId, r.seance.getIdSeance()))
                            .toList();

            Set<Integer> recommendedIds = validRecommended.stream()
                    .map(r -> r.seance.getIdSeance())
                    .collect(java.util.stream.Collectors.toSet());

            // =====================================================
            // 1️⃣ SECTION RECOMMANDÉES
            // =====================================================
            if (!validRecommended.isEmpty()) {

                VBox recoSection = new VBox(20);
                recoSection.getStyleClass().add("reco-section");

                Label recoTitle = new Label("⭐ Recommandées pour vous");
                recoTitle.getStyleClass().add("reco-section-title");

                FlowPane recoCards = new FlowPane();
                recoCards.setHgap(25);
                recoCards.setVgap(25);

                int rank = 1;

                for (RecommendationServiceUser.ScoredSeance scored : validRecommended) {

                    recoCards.getChildren().add(
                            createCard(
                                    scored.seance,
                                    true,
                                    rank++,
                                    scored.reason
                            )
                    );
                }

                recoSection.getChildren().addAll(recoTitle, recoCards);
                cardContainer.getChildren().add(recoSection);

            } else {

                VBox emptyReco = new VBox(10);
                emptyReco.getStyleClass().add("empty-reco-box");

                Label noRecoTitle =
                        new Label("🤖 Aucune recommandation personnalisée");
                noRecoTitle.getStyleClass().add("empty-title");

                Label noRecoSub =
                        new Label("Réservez des séances pour obtenir des suggestions adaptées.");
                noRecoSub.getStyleClass().add("empty-subtitle");

                emptyReco.getChildren().addAll(noRecoTitle, noRecoSub);

                cardContainer.getChildren().add(emptyReco);
            }

            // 🔥 ESPACE VISUEL ENTRE SECTIONS
            Region divider = new Region();
            divider.setPrefHeight(40);
            cardContainer.getChildren().add(divider);

            // =====================================================
            // 2️⃣ AUTRES SÉANCES
            // =====================================================
            VBox allSection = new VBox(20);

            Label allTitle =
                    new Label("📋 Toutes les séances disponibles");
            allTitle.getStyleClass().add("section-title");

            FlowPane allCards = new FlowPane();
            allCards.setHgap(25);
            allCards.setVgap(25);

            for (Seance s : masterData) {

                if (recommendedIds.contains(s.getIdSeance()))
                    continue;

                if (s.getStatutSeance() != StatutSeance.PLANIFIEE)
                    continue;
                if (reservationService.exists(userId, s.getIdSeance()))
                    continue;

                if (matchesSearch(s, search)) {

                    allCards.getChildren().add(
                            createCard(
                                    s,
                                    false,
                                    0,
                                    null
                            )
                    );
                }
            }

            allSection.getChildren().addAll(allTitle, allCards);
            cardContainer.getChildren().add(allSection);

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
    private VBox createCard(Seance s,
                            boolean recommended,
                            int rank,
                            String reason) {

        VBox card = new VBox(18);
        card.getStyleClass().add("seance-card-pro");
        card.setPrefWidth(300);

        if (recommended) {
            card.getStyleClass().add("recommended-card");
        }

        // =================================================
        // HEADER
        // =================================================
        VBox headerBox = new VBox(6);

        if (recommended && rank > 0) {
            Label rankBadge = new Label("TOP " + rank);
            rankBadge.getStyleClass().add("rank-badge");
            headerBox.getChildren().add(rankBadge);
        }

        if (recommended && reason != null && !reason.isBlank()) {

            Label reasonLabel = new Label("💡 " + reason);
            reasonLabel.getStyleClass().add("reco-reason");
            reasonLabel.setWrapText(true);
            reasonLabel.setMaxWidth(Double.MAX_VALUE);

            headerBox.getChildren().add(reasonLabel);
        }

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setSpacing(10);

        Label title = new Label(s.getNom());
        title.getStyleClass().add("seance-title-pro");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(title, spacer);
        headerBox.getChildren().add(topRow);

        if (recommended) {
            Label recoBadge = new Label("⭐ Recommandé pour vous");
            recoBadge.getStyleClass().add("reco-badge");
            headerBox.getChildren().add(recoBadge);
        }

        // =================================================
        // INFO SECTION
        // =================================================
        VBox infoBox = new VBox(8);

        Label date = new Label("📅  " + s.getDateSeance());
        Label time = new Label("⏰  " + s.getHeureDebut() + " - " + s.getHeureFin());

        date.getStyleClass().add("seance-info-pro");
        time.getStyleClass().add("seance-info-pro");

        // 🔥 COACH AVEC IMAGE
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
        coachImage.setFitWidth(28);
        coachImage.setFitHeight(28);

        // Rendre image ronde
        Circle clip = new Circle(14, 14, 14);
        coachImage.setClip(clip);

        coachImage.getStyleClass().add("coach-avatar");

        Label coachName = new Label(
                coachUser != null
                        ? coachUser.getNom() + " " + coachUser.getPrenom()
                        : "Inconnu"
        );
        coachName.getStyleClass().add("seance-info-pro");

        coachRow.getChildren().addAll(coachImage, coachName);

        infoBox.getChildren().addAll(date, time, coachRow);

        // =================================================
        // COMPTEUR "DANS X JOURS"
        // =================================================
        LocalDate today = LocalDate.now();
        long daysLeft = ChronoUnit.DAYS.between(today, s.getDateSeance());

        if (daysLeft >= 0) {

            Label countdown = new Label();

            if (daysLeft == 0)
                countdown.setText("🚀 Aujourd'hui");
            else if (daysLeft == 1)
                countdown.setText("⏳ Demain");
            else
                countdown.setText("⏳ Dans " + daysLeft + " jours");

            countdown.getStyleClass().add("countdown-label");
            infoBox.getChildren().add(countdown);
        }

        // =================================================
        // CAPACITÉ
        // =================================================
        int reserved = 0;
        try {
            reserved = reservationService.countReservations(s.getIdSeance());
        } catch (Exception ignored) {}

        int restantes = s.getCapacite() - reserved;
        double progressValue = (double) reserved / s.getCapacite();

        ProgressBar progress = new ProgressBar(progressValue);
        progress.setPrefHeight(6);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("places-progress");

        Label places = new Label(restantes + " places restantes");
        places.getStyleClass().add("places-label");

        VBox capacityBox = new VBox(6, progress, places);

        // =================================================
        // FOOTER
        // =================================================
        VBox footer = new VBox(10);

        Label badge = new Label();
        badge.getStyleClass().add("badge-pro");

        Button actionBtn = new Button();
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setPrefHeight(42);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime =
                LocalDateTime.of(s.getDateSeance(), s.getHeureDebut());

        boolean dejaReserve =
                reservationService.exists(userId, s.getIdSeance());

        if (s.getStatutSeance() == StatutSeance.TERMINEE) {

            badge.setText("Terminée");
            badge.getStyleClass().add("badge-grey");

            actionBtn.setText("Séance terminée");
            actionBtn.setDisable(true);
        }

        else if (s.getStatutSeance() == StatutSeance.ANNULEE) {

            badge.setText("Annulée");
            badge.getStyleClass().add("badge-red");

            actionBtn.setText("Séance annulée");
            actionBtn.setDisable(true);
        }

        else if (!now.isBefore(startDateTime)) {

            badge.setText("Déjà commencée");
            badge.getStyleClass().add("badge-grey");

            actionBtn.setText("Indisponible");
            actionBtn.setDisable(true);
        }

        else if (restantes <= 0) {

            badge.setText("Complet");
            badge.getStyleClass().add("badge-red");

            actionBtn.setText("Complet");
            actionBtn.setDisable(true);
        }

        else if (dejaReserve) {

            badge.setText("Réservée");
            badge.getStyleClass().add("badge-blue");

            actionBtn.setText("Voir réservation");
            actionBtn.getStyleClass().add("btn-google");
            actionBtn.setOnAction(e -> ouvrirGoogleCalendar(s));
        }

        else {

            badge.setText("Disponible");
            badge.getStyleClass().add("badge-green");

            actionBtn.setText("Réserver");
            actionBtn.getStyleClass().add("btn-primary");
            actionBtn.setOnAction(e -> reserver(s));
        }

        footer.getChildren().addAll(badge, actionBtn);

        // =================================================
        // HOVER EFFECT
        // =================================================
        card.setOnMouseEntered(e -> card.getStyleClass().add("card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("card-hover"));

        // =================================================
        // BUILD
        // =================================================
        card.getChildren().addAll(
                headerBox,
                infoBox,
                capacityBox,
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
    private void loadUserHeader() {

        UserApp user = Session.getConnectedUser();

        if (user == null)
            return;

        // ✅ Nom complet
        userNameLabel.setText(user.getNom() + " " + user.getPrenom());

        Image image;

        try {
            if (user.getImageUrl() != null && !user.getImageUrl().isBlank()) {
                image = new Image(user.getImageUrl(), true);
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

        userImageView.setImage(image);

        // 🔵 Rendre l'image ronde
        Circle clip = new Circle(15, 15, 15);
        userImageView.setClip(clip);
    }
}