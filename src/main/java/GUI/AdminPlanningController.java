package GUI;

import Entities.Planning;
import Entities.Session;
import Entities.UserApp;
import Services.interfaces.RecommendationServiceAdmin;
import enums.RoleUser;
import enums.StatutPlanning;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.PlanningService;
import Services.interfaces.SeanceService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminPlanningController {

    // ================= UI =================
    @FXML private Label lblTotalPlanning;
    @FXML private Label lblActifs;
    @FXML private Label lblArchives;
    @FXML private Label lblBrouillons;

    @FXML private TextField searchField;
    @FXML private ComboBox<Integer> yearFilter;
    @FXML private ComboBox<String> statutFilter;
    @FXML private Label lblTopCoach1;
    @FXML private Label lblTopCoach2;
    @FXML private Label lblTopCoach3;
    @FXML private TableView<Planning> planningTable;
    @FXML private TableColumn<Planning, String> colPeriode;
    @FXML private TableColumn<Planning, String> colDescription;
    @FXML private TableColumn<Planning, String> colStatut;
    @FXML private Label lblRecommendationDay;
    @FXML private Label lblRecommendationCoach;
    @FXML private Label lblRecommendationMonth;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    // ================= DATA =================
    private ObservableList<Planning> masterData = FXCollections.observableArrayList();
    private FilteredList<Planning> filteredData;

    // ================= SERVICES =================
    private final PlanningService planningService = new PlanningService();
    private final SeanceService seanceService = new SeanceService();
    private final RecommendationServiceAdmin recommendationServiceAdmin = new RecommendationServiceAdmin();
    private UserApp connectedUser;
    @FXML private TableColumn<Planning, String> colDateDebut;
    @FXML private TableColumn<Planning, String> colDateFin;
    @FXML private TableColumn<Planning, String> colDuree;
    // ==================================================
    // INITIALISATION
    // ==================================================
    @FXML
    public void initialize() {

        connectedUser = Session.getConnectedUser();

        if (connectedUser == null || connectedUser.getRole() != RoleUser.ADMIN) {
            DialogUtils.showError("Erreur", "Accès refusé.");
            closeWindow();
            return;
        }

        configureTable();
        configureFilters();
        configureSearch();
        configureButtons();
        loadData();
    }

    private void closeWindow() {
        Stage stage = (Stage) planningTable.getScene().getWindow();
        stage.close();
    }

    // ==================================================
    // CONFIG TABLE
    // ==================================================


    private void configureTable() {
        planningTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");

        colPeriode.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getTitre()
                ));

        colDateDebut.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDateDebut() != null ?
                                data.getValue().getDateDebut().format(formatter)
                                : ""
                ));

        colDateFin.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDateFin() != null ?
                                data.getValue().getDateFin().format(formatter)
                                : ""
                ));

        colDuree.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.valueOf(data.getValue().getDuree())
                ));

        colDescription.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDescription()));

        colStatut.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getStatut().name()));

        filteredData = new FilteredList<>(masterData, p -> true);

        SortedList<Planning> sortedData =
                new SortedList<>(filteredData);

        sortedData.comparatorProperty()
                .bind(planningTable.comparatorProperty());

        planningTable.setItems(sortedData);
    }

    // ==================================================
    // FILTERS
    // ==================================================
    private void configureFilters() {

        statutFilter.setItems(FXCollections.observableArrayList(
                "TOUS", "ACTIF", "BROUILLON", "ARCHIVE"
        ));
        statutFilter.setValue("TOUS");

        statutFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        yearFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {

        filteredData.setPredicate(planning -> {

            // Filtre année
            if (yearFilter.getValue() != null) {
                if (planning.getDateDebut().getYear() != yearFilter.getValue())
                    return false;
            }

            // Filtre statut
            if (!statutFilter.getValue().equals("TOUS")) {
                if (!planning.getStatut().name().equals(statutFilter.getValue()))
                    return false;
            }

            return true;
        });
    }

    // ==================================================
    // SEARCH
    // ==================================================
    private void configureSearch() {

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {

            filteredData.setPredicate(planning -> {

                if (newVal == null || newVal.isBlank())
                    return true;

                String keyword = newVal.toLowerCase();

                return planning.getTitre().toLowerCase().contains(keyword)
                        || planning.getPeriode().toLowerCase().contains(keyword)
                        || planning.getDescription().toLowerCase().contains(keyword)
                        || planning.getStatut().name().toLowerCase().contains(keyword);
            });
        });
        planningTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Planning planning, boolean empty) {
                super.updateItem(planning, empty);

                if (planning == null || empty) {
                    setStyle("");
                } else {
                    switch (planning.getStatut()) {
                        case ACTIF -> setStyle("-fx-background-color: #d4edda;");      // vert clair
                        case BROUILLON -> setStyle("-fx-background-color: #fff3cd;");  // jaune clair
                        case ARCHIVE -> setStyle("-fx-background-color: #f8d7da;");    // rouge clair
                    }
                }
            }
        });
    }

    // ==================================================
    // BUTTONS
    // ==================================================
    private void configureButtons() {
        btnModifier.disableProperty().bind(
                planningTable.getSelectionModel().selectedItemProperty().isNull());
        btnSupprimer.disableProperty().bind(
                planningTable.getSelectionModel().selectedItemProperty().isNull());
    }
    private void loadTop3Coaches() {

        try {

            List<String> top3 =
                    seanceService.getTop3CoachesDetailed();
            lblTopCoach1.setText(top3.size() > 0 ? top3.get(0) : "—");
            lblTopCoach2.setText(top3.size() > 1 ? top3.get(1) : "—");
            lblTopCoach3.setText(top3.size() > 2 ? top3.get(2) : "—");

        } catch (Exception e) {

            lblTopCoach1.setText("Erreur");
            lblTopCoach2.setText("");
            lblTopCoach3.setText("");
        }
    }

    // ==================================================
    // LOAD DATA + DASHBOARD
    // ==================================================
    private void loadData() {

        try {
            List<Planning> plannings = planningService.getAll();
            masterData.setAll(plannings);

            lblTotalPlanning.setText(String.valueOf(masterData.size()));
            loadTop3Coaches();
            // Dashboard
            lblRecommendationDay.setText(
                    recommendationServiceAdmin.getBestDayRecommendation()
            );

            lblRecommendationCoach.setText(
                    recommendationServiceAdmin.getBestCoachRecommendation()
            );

            lblRecommendationMonth.setText(
                    recommendationServiceAdmin.getBestMonthRecommendation()
            );
            long actifs = masterData.stream()
                    .filter(p -> p.getStatut() == StatutPlanning.ACTIF).count();

            long archives = masterData.stream()
                    .filter(p -> p.getStatut() == StatutPlanning.ARCHIVE).count();

            long brouillons = masterData.stream()
                    .filter(p -> p.getStatut() == StatutPlanning.BROUILLON).count();

            lblActifs.setText(String.valueOf(actifs));
            lblArchives.setText(String.valueOf(archives));
            lblBrouillons.setText(String.valueOf(brouillons));

            // Remplir filtre année automatiquement
            Set<Integer> years = masterData.stream()
                    .map(p -> p.getDateDebut().getYear())
                    .collect(Collectors.toSet());

            yearFilter.setItems(FXCollections.observableArrayList(years));

        } catch (Exception e) {
            DialogUtils.showError("Erreur", "Impossible de charger les plannings.");
        }
    }
    // ==================================================
// AJOUT
// ==================================================
    @FXML
    private void handleAdd() {
        openForm(null);
    }

    // ==================================================
// MODIFIER
// ==================================================
    @FXML
    private void handleUpdate() {

        Planning selected =
                planningTable.getSelectionModel().getSelectedItem();

        if (selected != null)
            openForm(selected);
    }

    // ==================================================
// DELETE
// ==================================================
    @FXML
    private void handleDelete() {

        Planning selected =
                planningTable.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        try {

            if (seanceService.hasSeanceInPlanning(
                    selected.getIdPlanning())) {

                DialogUtils.showError(
                        "Suppression impossible",
                        "Ce planning contient des séances."
                );
                return;
            }

            boolean confirm =
                    DialogUtils.showConfirmation(
                            "Suppression du planning",
                            "Voulez-vous vraiment supprimer ce planning ?"
                    );

            if (!confirm) return;

            planningService.delete(selected.getIdPlanning());
            loadData();

            DialogUtils.showInfo(
                    "Succès",
                    "Planning supprimé avec succès."
            );

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de supprimer."
            );
        }
    }
    private void openForm(Planning planning) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/PlanningFormView.fxml")
            );

            Parent root = loader.load();

            loader.<PlanningFormController>getController()
                    .setPlanning(planning);

            Stage stage = new Stage();
            stage.setTitle("Formulaire Planning");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/admin.css")
                            .toExternalForm()
            );

            stage.setScene(scene);
            stage.showAndWait();

            loadData();

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible d’ouvrir le formulaire."
            );
        }
    }
    @FXML
    private void handleSeances(javafx.event.ActionEvent event) {

        Planning selected =
                planningTable.getSelectionModel().getSelectedItem();

        if (selected == null) {

            DialogUtils.showWarning(
                    "Attention",
                    "Sélectionnez un planning."
            );
            return;
        }

        FXMLLoader loader =
                SceneUtils.loadScene(
                        "/AdminSeanceView.fxml",
                        "/admin.css",
                        (javafx.scene.Node) event.getSource()
                );

        if (loader != null) {

            loader.<AdminSeanceController>getController()
                    .setPlanning(
                            selected.getIdPlanning(),
                            selected.getTitre()
                    );
        }
    }
    @FXML
    private void handleRetour(javafx.event.ActionEvent event) {

        SceneUtils.loadScene(
                "/gui/MainLayout.fxml",
                (javafx.scene.Node) event.getSource()
        );
    }
}