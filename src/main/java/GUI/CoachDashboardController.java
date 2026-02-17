package GUI;

import Entities.Planning;
import Entities.Seance;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.PlanningService;
import Services.interfaces.SeanceService;
import enums.StatutSeance;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoachDashboardController {

    // ================= UI =================
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;

    @FXML private Label lblTotal;
    @FXML private Label lblToday;
    @FXML private Label lblTerminee;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final PlanningService planningService = new PlanningService();

    private final Map<Integer, String> planningMap = new HashMap<>();

    private final int coachId = 1; // ⚠️ login plus tard

    private List<Seance> masterData;

    // =================================================
    // INITIALISATION
    // =================================================
    @FXML
    public void initialize() {

        configureFilters();

        searchField.textProperty().addListener((obs, o, n) -> refreshCards());
        statutFilter.valueProperty().addListener((obs, o, n) -> refreshCards());

        loadPlannings();
        loadData();
    }

    // =================================================
    // CONFIG FILTER
    // =================================================
    private void configureFilters() {

        statutFilter.getItems().addAll(
                "Tous",
                "PLANIFIEE",
                "TERMINEE",
                "ANNULEE"
        );

        statutFilter.setValue("Tous");
        statutFilter.getStyleClass().add("combo-pro");

        searchField.getStyleClass().add("search-field-pro");
        searchField.setPromptText("🔍 Rechercher une séance...");
    }

    // =================================================
    // LOAD DATA
    // =================================================
    private void loadData() {

        try {

            masterData = seanceService.getByCoach(coachId);

            updateStats(masterData);
            refreshCards();

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les séances."
            );
        }
    }

    private void loadPlannings() {

        try {

            List<Planning> list = planningService.getAll();

            for (Planning p : list) {
                planningMap.put(
                        p.getIdPlanning(),
                        p.getPeriode()
                );
            }

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les plannings."
            );
        }
    }

    // =================================================
    // CARD SYSTEM (PRO VERSION CLEAN)
    // =================================================
    private void refreshCards() {

        cardContainer.getChildren().clear();

        if (masterData == null) return;

        String search = searchField.getText() == null ?
                "" : searchField.getText().toLowerCase();

        String statutSelected = statutFilter.getValue();

        for (Seance s : masterData) {

            boolean matchesSearch =
                    s.getNom().toLowerCase().contains(search)
                            || s.getDateSeance().toString().contains(search);

            boolean matchesStatut =
                    statutSelected.equals("Tous")
                            || s.getStatutSeance().name().equals(statutSelected);

            if (matchesSearch && matchesStatut) {
                cardContainer.getChildren().add(createCard(s));
            }
        }
    }

    private VBox createCard(Seance s) {

        VBox card = new VBox(14);
        card.getStyleClass().add("coach-card");
        card.setStyle("-fx-padding: 24;");

        // ===== TITLE
        Label title = new Label(s.getNom());
        title.getStyleClass().add("coach-card-title");
        title.setStyle("-fx-wrap-text: true;");

        // ===== INFO CONTAINER
        VBox infoContainer = new VBox(10);
        infoContainer.setStyle("-fx-spacing: 10;");

        // Date info
        HBox dateRow = new HBox(10);
        dateRow.setStyle("-fx-alignment: CENTER_LEFT;");
        Label dateLabel = new Label("📅  " + s.getDateSeance());
        dateLabel.getStyleClass().add("coach-card-info");
        dateRow.getChildren().add(dateLabel);

        // Time info
        HBox timeRow = new HBox(10);
        timeRow.setStyle("-fx-alignment: CENTER_LEFT;");
        Label timeLabel = new Label("⏰  " + s.getHeureDebut() + " - " + s.getHeureFin());
        timeLabel.getStyleClass().add("coach-card-info");
        timeRow.getChildren().add(timeLabel);

        // Planning info
        HBox planningRow = new HBox(10);
        planningRow.setStyle("-fx-alignment: CENTER_LEFT;");
        Label planningLabel = new Label("📁  " + planningMap.getOrDefault(
                s.getIdPlanning(), "Inconnu"));
        planningLabel.getStyleClass().add("coach-card-info");
        planningRow.getChildren().add(planningLabel);

        infoContainer.getChildren().addAll(dateRow, timeRow, planningRow);

        // ===== FOOTER WITH BADGE
        VBox footer = new VBox();
        footer.getStyleClass().add("coach-card-footer");
        footer.setStyle("-fx-padding: 16 0 0 0; -fx-alignment: CENTER_LEFT;");

        Label badge = new Label(s.getStatutSeance().name());
        badge.setStyle("-fx-padding: 8 18; -fx-font-weight: 700; -fx-font-size: 11;");

        switch (s.getStatutSeance()) {
            case PLANIFIEE -> badge.getStyleClass().add("badge-planifiee");
            case TERMINEE -> badge.getStyleClass().add("badge-terminee");
            case ANNULEE -> badge.getStyleClass().add("badge-annulee");
        }

        footer.getChildren().add(badge);

        card.getChildren().addAll(title, infoContainer, footer);

        return card;
    }

    // =================================================
    // STATS
    // =================================================
    private void updateStats(List<Seance> list) {

        lblTotal.setText(String.valueOf(list.size()));

        long today = list.stream()
                .filter(s -> s.getDateSeance().equals(LocalDate.now()))
                .count();

        lblToday.setText(String.valueOf(today));

        long terminees = list.stream()
                .filter(s -> s.getStatutSeance() == StatutSeance.TERMINEE)
                .count();

        lblTerminee.setText(String.valueOf(terminees));
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
}
