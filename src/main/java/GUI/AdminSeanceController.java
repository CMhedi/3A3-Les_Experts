package GUI;

import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.SeanceService;
import Services.UserService;

import javafx.beans.property.*;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Entities.Session;
import enums.RoleUser;
public class AdminSeanceController {

    // ================= UI =================
    @FXML private Label planningLabel;
    @FXML private TextField searchField;
    @FXML private TableColumn<Seance, String> colNom;

    @FXML private TableView<Seance> seanceTable;
    @FXML private TableColumn<Seance, LocalDate> colDate;
    @FXML private TableColumn<Seance, LocalTime> colDebut;
    @FXML private TableColumn<Seance, LocalTime> colFin;
    @FXML private TableColumn<Seance, Integer> colCapacite;
    @FXML private TableColumn<Seance, String> colStatut;
    @FXML private TableColumn<Seance, String> colCoach;

    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    // ================= DATA =================
    private ObservableList<Seance> masterData = FXCollections.observableArrayList();
    private FilteredList<Seance> filteredData;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final UserService userService = new UserService();
    private UserApp connectedUser;
    private int planningId;

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
        loadCoachs();
    }
    private void closeWindow() {
        if (seanceTable != null && seanceTable.getScene() != null) {
            Stage stage = (Stage) seanceTable.getScene().getWindow();
            stage.close();
        }
    }
    // =================================================
    // CONFIGURATION
    // =================================================
    private void configureTable() {

        seanceTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
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
        colNom.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getNom()
                )
        );

        colDate.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getDateSeance()));

        colDebut.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getHeureDebut()));

        colFin.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getHeureFin()));

        colCapacite.setCellValueFactory(d ->
                new SimpleIntegerProperty(
                        d.getValue().getCapacite()).asObject());

        colCoach.setCellValueFactory(d ->
                new SimpleStringProperty(
                        coachMap.getOrDefault(
                                d.getValue().getIdCoach(),
                                "Inconnu"
                        )
                )
        );

        colStatut.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getStatutSeance().name()
                )
        );

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

        filteredData = new FilteredList<>(masterData, s -> true);

        SortedList<Seance> sortedData =
                new SortedList<>(filteredData);

        sortedData.comparatorProperty()
                .bind(seanceTable.comparatorProperty());

        seanceTable.setItems(sortedData);
    }

    private void configureSearch() {

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {

            filteredData.setPredicate(seance -> {

                if (newVal == null || newVal.isBlank())
                    return true;

                String keyword = newVal.toLowerCase();

                String coachName =
                        coachMap.getOrDefault(
                                seance.getIdCoach(), ""
                        ).toLowerCase();

                return seance.getNom().toLowerCase().contains(keyword)
                        || seance.getDateSeance().toString().contains(keyword)
                        || seance.getStatutSeance().name().toLowerCase().contains(keyword)
                        || coachName.contains(keyword)
                        || String.valueOf(seance.getCapacite()).contains(keyword);
            });
        });
    }

    private void configureButtons() {

        btnModifier.disableProperty().bind(
                seanceTable.getSelectionModel()
                        .selectedItemProperty().isNull()
        );

        btnSupprimer.disableProperty().bind(
                seanceTable.getSelectionModel()
                        .selectedItemProperty().isNull()
        );
    }

    // =================================================
    // LOAD DATA
    // =================================================
    public void setPlanning(int planningId, String periode) {

        this.planningId = planningId;
        planningLabel.setText("Séances du planning : " + periode);

        loadData();
    }

    private void loadData() {

        try {

            List<Seance> list =
                    seanceService.getByPlanning(planningId);

            masterData.setAll(list);

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les séances."
            );
        }
    }

    private void loadCoachs() {

        try {

            List<UserApp> coachs =
                    userService.getAllCoachs();

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
    // ADD / UPDATE
    // =================================================
    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleUpdate() {

        Seance selected =
                seanceTable.getSelectionModel().getSelectedItem();

        if (selected != null)
            openForm(selected);
    }

    private void openForm(Seance seance) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/SeanceFormView.fxml")
            );

            Parent root = loader.load();

            loader.<SeanceFormController>getController()
                    .setSeance(seance, planningId);

            Stage stage = new Stage();
            stage.setTitle("Formulaire Séance");

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

    // =================================================
    // DELETE
    // =================================================
    @FXML
    private void handleDelete() {

        Seance selected =
                seanceTable.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        boolean confirm =
                DialogUtils.showConfirmation(
                        "Suppression séance",
                        "Voulez-vous supprimer cette séance ?"
                );

        if (!confirm) return;

        try {

            seanceService.delete(selected.getIdSeance());
            loadData();

            DialogUtils.showInfo(
                    "Succès",
                    "Séance supprimée avec succès."
            );

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Suppression impossible."
            );
        }
    }

    // =================================================
    // RETOUR
    // =================================================
    @FXML
    private void handleRetour(javafx.event.ActionEvent e) {

        SceneUtils.loadScene(
                "/AdminPlanningView.fxml",
                "/admin.css",
                (javafx.scene.Node) e.getSource()
        );
    }
}
