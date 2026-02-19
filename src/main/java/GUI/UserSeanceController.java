package GUI;

import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.GoogleCalendarService;
import Services.interfaces.ReservationSeanceService;
import Services.interfaces.SeanceService;
import Services.interfaces.UserService;
import exceptions.ValidationException;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSeanceController {

    // ================= UI =================
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final ReservationSeanceService reservationService =
            new ReservationSeanceService();
    private final UserService userService = new UserService();

    private final int USER_TEST_ID = 1; // ⚠️ login plus tard

    private List<Seance> masterData;

    private final Map<Integer, String> coachMap = new HashMap<>();

    // =================================================
    // INITIALISATION
    // =================================================
    @FXML
    public void initialize() {

        loadCoachs();

        searchField.textProperty().addListener((obs, o, n) -> refreshCards());

        loadData();
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
    // CARD SYSTEM
    // =================================================
    private void refreshCards() {

        cardContainer.getChildren().clear();

        if (masterData == null) return;

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase();

        for (Seance s : masterData) {

            boolean matchesSearch =
                    s.getNom().toLowerCase().contains(search)
                            || s.getDateSeance().toString().contains(search)
                            || coachMap.getOrDefault(
                            s.getIdCoach(), ""
                    ).toLowerCase().contains(search);

            if (matchesSearch) {
                cardContainer.getChildren().add(createCard(s));
            }
        }
    }

    private VBox createCard(Seance s) {

        VBox card = new VBox(18);
        card.getStyleClass().add("seance-card-pro");

        // ================= TITLE =================
        Label title = new Label(s.getNom());
        title.getStyleClass().add("seance-title-pro");

        // ================= DATE =================
        Label date = new Label("📅  " + s.getDateSeance());
        date.getStyleClass().add("seance-info-pro");

        Label time = new Label("⏰  "
                + s.getHeureDebut() + " - " + s.getHeureFin());
        time.getStyleClass().add("seance-info-pro");

        Label coach = new Label("👤  "
                + coachMap.getOrDefault(
                s.getIdCoach(), "Inconnu"));
        coach.getStyleClass().add("seance-info-pro");

        // ================= PLACES =================
        int reserved = 0;
        try {
            reserved = reservationService
                    .countReservations(s.getIdSeance());
        } catch (Exception ignored) {}

        int capacite = s.getCapacite();
        int restantes = capacite - reserved;

        double progressValue =
                (double) reserved / capacite;

        ProgressBar progressBar = new ProgressBar(progressValue);
        progressBar.getStyleClass().add("places-progress");

        Label placesLabel = new Label(
                restantes + " places restantes"
        );
        placesLabel.getStyleClass().add("places-label");

        // ================= BADGE + BUTTON =================
        HBox footer = new HBox(15);
        footer.setStyle("-fx-alignment: CENTER_LEFT;");

        Label badge = new Label();
        badge.getStyleClass().add("badge-pro");

        Button actionBtn = new Button();
        actionBtn.getStyleClass().add("btn-pro");

        if (s.getDateSeance().isBefore(LocalDate.now())) {

            badge.setText("Terminée");
            badge.getStyleClass().add("badge-grey");

            actionBtn.setText("Terminée");
            actionBtn.setDisable(true);

            card.getStyleClass().add("border-grey");
        }
        else if (restantes <= 0) {

            badge.setText("Complet");
            badge.getStyleClass().add("badge-red");

            actionBtn.setText("Complet");
            actionBtn.setDisable(true);

            card.getStyleClass().add("border-red");
        }
        else {

            badge.setText("Disponible");
            badge.getStyleClass().add("badge-green");

            actionBtn.setText("Réserver");
            actionBtn.setOnAction(e -> reserver(s));

            card.getStyleClass().add("border-green");
        }

        footer.getChildren().addAll(badge, actionBtn);

        card.getChildren().addAll(
                title,
                date,
                time,
                coach,
                progressBar,
                placesLabel,
                footer
        );

        return card;
    }


    // =================================================
    // RESERVATION
    // =================================================
    // =================================================
// RESERVATION
// =================================================
    private void reserver(Seance s) {

        try {

            boolean confirmed =
                    DialogUtils.showConfirmation(
                            "Réserver la séance",
                            "Confirmez-vous la réservation ?"
                    );

            if (!confirmed) return;

            // 🔎 Vérifier si déjà réservé
            if (reservationService.exists(
                    USER_TEST_ID,
                    s.getIdSeance()
            )) {
                DialogUtils.showWarning(
                        "Déjà réservé",
                        "Vous avez déjà réservé cette séance."
                );
                return;
            }

            // ✅ Réservation en base
            reservationService.reserver(
                    USER_TEST_ID,
                    s.getIdSeance()
            );

            // ================= GOOGLE CALENDAR =================
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

                // 🔥 Google génère l'ID
                String googleEventId =
                        GoogleCalendarService.addEvent(
                                "Séance : " + s.getNom(),
                                "Séance EcoAdventure avec votre coach",
                                start,
                                end
                        );

                // ✅ Sauvegarder l'eventId en base
                reservationService.saveGoogleEventId(
                        USER_TEST_ID,
                        s.getIdSeance(),
                        googleEventId
                );

            } catch (Exception googleError) {

                System.out.println("Erreur Google Calendar : "
                        + googleError.getMessage());

                // On ne bloque pas la réservation
            }

            DialogUtils.showInfo(
                    "Succès",
                    "Séance réservée avec succès."
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

            e.printStackTrace();
        }
    }


    // =================================================
    // NAVIGATION
    // =================================================
    @FXML
    private void handleRetour(javafx.event.ActionEvent event) {

        SceneUtils.loadScene(
                "/Menu.fxml",
                "/menu.css",
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
