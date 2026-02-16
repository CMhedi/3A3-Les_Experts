package GUI;

import Entities.Planning;
import Entities.Seance;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.PlanningService;
import Services.interfaces.SeanceService;

import enums.StatutSeance;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoachDashboardController {

    // ================= TABLE =================
    @FXML private TableView<Seance> seanceTable;

    @FXML private TableColumn<Seance, LocalDate> colDate;
    @FXML private TableColumn<Seance, LocalTime> colDebut;
    @FXML private TableColumn<Seance, LocalTime> colFin;
    @FXML private TableColumn<Seance, Integer> colCapacite;
    @FXML private TableColumn<Seance, String> colStatut;
    @FXML private TableColumn<Seance, String> colPlanning;
    @FXML private TableColumn<Seance, String> colNom;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;

    // ================= STATS =================
    @FXML private Label lblTotal;
    @FXML private Label lblToday;
    @FXML private Label lblTerminee;

    // ================= DATA =================
    private ObservableList<Seance> masterData = FXCollections.observableArrayList();
    private FilteredList<Seance> filteredData;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final PlanningService planningService = new PlanningService();

    private final Map<Integer, String> planningMap = new HashMap<>();

    // ⚠️ Temporaire (login plus tard)
    private final int coachId = 1;

    // =================================================
    // INITIALISATION
    // =================================================
    @FXML
    public void initialize() {

        configureFilters();
        configureTable();
        configureSearch();
        loadPlannings();
        loadData();
    }

    // =================================================
    // CONFIGURATION
    // =================================================
    private void configureFilters() {

        statutFilter.getItems().addAll(
                "Tous",
                "PLANIFIEE",
                "TERMINEE",
                "ANNULEE"
        );

        statutFilter.setValue("Tous");

        statutFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void configureTable() {

        seanceTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
        colNom.setCellValueFactory(d->
                new SimpleStringProperty(d.getValue().getNom()));
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #1f4d36;");
            }
        });


        colDate.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getDateSeance()));

        colDebut.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getHeureDebut()));

        colFin.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getHeureFin()));

        colCapacite.setCellValueFactory(d ->
                new SimpleIntegerProperty(
                        d.getValue().getCapacite()).asObject());

        // ========= STATUT BADGE =========
        colStatut.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getStatutSeance().name()));

        colStatut.setCellFactory(col -> new TableCell<>() {

            private final Label badge = new Label();

            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                if (empty || statut == null) {
                    setGraphic(null);
                    return;
                }

                badge.setText(statut);

                badge.getStyleClass().removeAll(
                        "badge-planifiee",
                        "badge-terminee",
                        "badge-annulee"
                );

                switch (statut) {
                    case "PLANIFIEE" -> badge.getStyleClass().add("badge-planifiee");
                    case "TERMINEE" -> badge.getStyleClass().add("badge-terminee");
                    case "ANNULEE" -> badge.getStyleClass().add("badge-annulee");
                }

                setGraphic(badge);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

        colPlanning.setCellValueFactory(d ->
                new SimpleStringProperty(
                        planningMap.getOrDefault(
                                d.getValue().getIdPlanning(),
                                "Inconnu"
                        )
                )
        );

        filteredData = new FilteredList<>(masterData, s -> true);

        SortedList<Seance> sortedData =
                new SortedList<>(filteredData);

        sortedData.comparatorProperty()
                .bind(seanceTable.comparatorProperty());

        seanceTable.setItems(sortedData);
    }

    private void configureSearch() {

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    // =================================================
    // FILTER LOGIC (CENTRALIZED)
    // =================================================
    private void applyFilters() {

        filteredData.setPredicate(seance -> {

            String search = searchField.getText();
            String statut = statutFilter.getValue();

            boolean matchesSearch = true;
            boolean matchesStatut = true;

            if (search != null && !search.isBlank()) {

                String keyword = search.toLowerCase();

                String planningName =
                        planningMap.getOrDefault(
                                seance.getIdPlanning(),
                                ""
                        ).toLowerCase();

                matchesSearch =
                        seance.getNom().toLowerCase().contains(keyword)
                                || seance.getDateSeance().toString().contains(keyword)
                                || seance.getHeureDebut().toString().contains(keyword)
                                || seance.getStatutSeance().name().toLowerCase().contains(keyword)
                                || planningName.contains(keyword);

            }

            if (statut != null && !statut.equals("Tous")) {
                matchesStatut =
                        seance.getStatutSeance().name().equals(statut);
            }

            return matchesSearch && matchesStatut;
        });
    }

    // =================================================
    // LOAD DATA
    // =================================================
    private void loadData() {

        try {

            List<Seance> list =
                    seanceService.getByCoach(coachId);

            masterData.setAll(list);

            updateStats(list);

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les séances."
            );
        }
    }

    private void loadPlannings() {

        try {

            List<Planning> list =
                    planningService.getAll();

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
    // STATS
    // =================================================
    private void updateStats(List<Seance> list) {

        lblTotal.setText(String.valueOf(list.size()));

        long today = list.stream()
                .filter(s -> s.getDateSeance()
                        .equals(LocalDate.now()))
                .count();

        lblToday.setText(String.valueOf(today));

        long terminees = list.stream()
                .filter(s ->
                        s.getStatutSeance()
                                == StatutSeance.TERMINEE)
                .count();

        lblTerminee.setText(String.valueOf(terminees));
    }

    // =================================================
    // RETOUR MENU
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
