package GUI;

import Entities.Seance;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.GoogleCalendarService;
import Services.interfaces.ReservationSeanceService;
import Services.interfaces.SeanceService;
import Services.interfaces.UserService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDashboardController {

    // ================= UI =================
    @FXML private FlowPane planifieesContainer;
    @FXML private FlowPane termineesContainer;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final UserService userService = new UserService();
    private final ReservationSeanceService reservationService =
            new ReservationSeanceService();

    private final int USER_TEST_ID = 1;

    private final Map<Integer, String> coachMap = new HashMap<>();

    // =================================================
    // INITIALISATION
    // =================================================
    @FXML
    public void initialize() {

        loadCoachs();
        refreshCards();
    }

    // =================================================
    // REFRESH CARDS
    // =================================================
    private void refreshCards() {

        planifieesContainer.getChildren().clear();
        termineesContainer.getChildren().clear();

        try {

            List<Seance> all =
                    seanceService.getSeancesByUser(USER_TEST_ID);

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
    // CREATE PLANIFIEE CARD
    // =================================================
    private VBox createPlanifieeCard(Seance s) {

        VBox card = baseCard(s);

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.getStyleClass().add("btn-danger");

        btnAnnuler.setOnAction(e -> handleAnnuler(s));

        card.getChildren().add(btnAnnuler);

        return card;
    }

    // =================================================
    // CREATE TERMINEE CARD
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

        VBox card = new VBox(10);
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
    // ANNULER RESERVATION
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

            // 🔹 1️⃣ Récupérer l’ID Google AVANT suppression
            String googleEventId =
                    reservationService.getGoogleEventId(
                            USER_TEST_ID,
                            selected.getIdSeance()
                    );

            // 🔹 2️⃣ Supprimer en base
            reservationService.annuler(
                    USER_TEST_ID,
                    selected.getIdSeance()
            );

            // 🔹 3️⃣ Supprimer du Google Calendar si existant
            if (googleEventId != null && !googleEventId.isBlank()) {

                try {
                    GoogleCalendarService.deleteEvent(googleEventId);
                } catch (Exception e) {
                    System.out.println("Event Google non trouvé ou déjà supprimé.");
                }
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

            e.printStackTrace();
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
