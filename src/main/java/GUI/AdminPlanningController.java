package GUI;
import Entities.Session;
import Entities.UserApp;
import enums.RoleUser;
import Entities.Planning;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.PlanningService;
import Services.SeanceService;
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
import java.util.List;

public class AdminPlanningController {

    // ================= UI =================
    @FXML private Label lblTotalPlanning;
    @FXML private TextField searchField;

    @FXML private TableView<Planning> planningTable;
    @FXML private TableColumn<Planning, String> colPeriode;
    @FXML private TableColumn<Planning, String> colDescription;

    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    // ================= DATA =================
    private ObservableList<Planning> masterData = FXCollections.observableArrayList();
    private FilteredList<Planning> filteredData;

    // ================= SERVICES =================
    private final PlanningService planningService = new PlanningService();
    private final SeanceService seanceService = new SeanceService();
    private UserApp connectedUser;
    // ==================================================
    // INITIALISATION
    // ==================================================
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

        if (connectedUser.getRole() != RoleUser.ADMIN) {
            DialogUtils.showError(
                    "Accès refusé",
                    "Cette page est réservée aux administrateurs."
            );
            closeWindow();
            return;
        }

        configureTable();
        configureSearch();
        configureButtons();
        loadData();
    }
    private void closeWindow() {
        if (planningTable != null && planningTable.getScene() != null) {
            Stage stage = (Stage) planningTable.getScene().getWindow();
            stage.close();
        }
    }
    // ==================================================
    // CONFIGURATION
    // ==================================================
    private void configureTable() {

        colPeriode.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPeriode()));

        colDescription.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescription()));

        planningTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        filteredData = new FilteredList<>(masterData, p -> true);

        SortedList<Planning> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty()
                .bind(planningTable.comparatorProperty());

        planningTable.setItems(sortedData);
    }

    private void configureSearch() {

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {

            filteredData.setPredicate(planning -> {

                if (newVal == null || newVal.isBlank())
                    return true;

                String keyword = newVal.toLowerCase();

                return planning.getPeriode().toLowerCase().contains(keyword)
                        || planning.getDescription().toLowerCase().contains(keyword);
            });
        });
    }

    private void configureButtons() {

        btnModifier.disableProperty().bind(
                planningTable.getSelectionModel()
                        .selectedItemProperty().isNull()
        );

        btnSupprimer.disableProperty().bind(
                planningTable.getSelectionModel()
                        .selectedItemProperty().isNull()
        );
    }

    // ==================================================
    // LOAD DATA
    // ==================================================
    private void loadData() {

        try {

            List<Planning> plannings = planningService.getAll();
            masterData.setAll(plannings);

            lblTotalPlanning.setText(
                    String.valueOf(masterData.size())
            );

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les plannings."
            );
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

    // ==================================================
    // FORM
    // ==================================================
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

    // ==================================================
    // GÉRER SÉANCES
    // ==================================================
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
                            selected.getPeriode()
                    );
        }
    }

    // ==================================================
    // RETOUR
    // ==================================================
    @FXML
    private void handleRetour(javafx.event.ActionEvent event) {

        SceneUtils.loadScene(
                "/GUI/AdminDashboard.fxml",
                (javafx.scene.Node) event.getSource()
        );
    }
}
