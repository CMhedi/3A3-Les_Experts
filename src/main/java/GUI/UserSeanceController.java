package GUI;

import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.ReservationSeanceService;
import Services.interfaces.SeanceService;
import Services.interfaces.UserService;
import exceptions.ValidationException;

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

public class UserSeanceController {

    // ================= TABLE =================
    @FXML private TableView<Seance> seanceTable;
    @FXML private TableColumn<Seance, LocalDate> colDate;
    @FXML private TableColumn<Seance, LocalTime> colDebut;
    @FXML private TableColumn<Seance, LocalTime> colFin;
    @FXML private TableColumn<Seance, String> colCoach;
    @FXML private TableColumn<Seance, Integer> colPlaces;
    @FXML private TableColumn<Seance, String> colStatut;
    @FXML private TextField searchField;
    @FXML private Button btnReserver;
    @FXML private TableColumn<Seance, String> colNom;

    private final ObservableList<Seance> masterData =
            FXCollections.observableArrayList();

    private FilteredList<Seance> filteredData;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final ReservationSeanceService reservationService =
            new ReservationSeanceService();
    private final UserService userService = new UserService();

    private final int USER_TEST_ID = 1;

    private final Map<Integer, String> coachMap = new HashMap<>();

    // =================================================
    // INITIALISATION
    // =================================================
    @FXML
    public void initialize() {

        seanceTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        loadCoachs();
        configureColumns();
        configureSearch();

        btnReserver.disableProperty().bind(
                seanceTable.getSelectionModel()
                        .selectedItemProperty().isNull()
        );

        refreshTable();
    }

    // =================================================
    // CONFIG COLUMNS
    // =================================================
    private void configureColumns() {
        colNom.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNom())
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


        colDate.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getDateSeance()));

        colDebut.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getHeureDebut()));

        colFin.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getHeureFin()));

        colCoach.setCellValueFactory(d ->
                new SimpleStringProperty(
                        coachMap.getOrDefault(
                                d.getValue().getIdCoach(),
                                "Inconnu"
                        )
                )
        );

        colPlaces.setCellValueFactory(d -> {
            try {
                int reserved = reservationService
                        .countReservations(d.getValue().getIdSeance());
                return new SimpleIntegerProperty(
                        d.getValue().getCapacite() - reserved
                ).asObject();
            } catch (Exception e) {
                return new SimpleIntegerProperty(0).asObject();
            }
        });

        // ===== STATUT BADGE
        colStatut.setCellValueFactory(data -> {

            Seance s = data.getValue();

            if (s.getDateSeance().isBefore(LocalDate.now()))
                return new SimpleStringProperty("Terminée");

            try {
                int reserved = reservationService
                        .countReservations(s.getIdSeance());

                if (s.getCapacite() - reserved <= 0)
                    return new SimpleStringProperty("Complète");

            } catch (Exception e) {
                return new SimpleStringProperty("Indisponible");
            }

            return new SimpleStringProperty("Disponible");
        });

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
                        "badge-disponible",
                        "badge-complete",
                        "badge-terminee"
                );

                switch (statut) {
                    case "Disponible" ->
                            badge.getStyleClass().add("badge-disponible");
                    case "Complète" ->
                            badge.getStyleClass().add("badge-complete");
                    case "Terminée" ->
                            badge.getStyleClass().add("badge-terminee");
                }

                setGraphic(badge);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
    }

    // =================================================
    // SEARCH SYSTEM PRO
    // =================================================
    private void configureSearch() {

        filteredData = new FilteredList<>(masterData, s -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {

            filteredData.setPredicate(seance -> {

                if (newVal == null || newVal.isBlank())
                    return true;

                String keyword = newVal.toLowerCase();

                return seance.getNom().toLowerCase().contains(keyword)
                        || seance.getDateSeance().toString().contains(keyword)
                        || seance.getHeureDebut().toString().contains(keyword)
                        || seance.getHeureFin().toString().contains(keyword)
                        || coachMap.getOrDefault(
                        seance.getIdCoach(), ""
                ).toLowerCase().contains(keyword);

            });
        });

        SortedList<Seance> sorted =
                new SortedList<>(filteredData);

        sorted.comparatorProperty()
                .bind(seanceTable.comparatorProperty());

        seanceTable.setItems(sorted);
    }

    // =================================================
    // LOAD COACHS
    // =================================================
    private void loadCoachs() {
        try {
            List<UserApp> coachs = userService.getAllCoachs();
            coachs.forEach(c ->
                    coachMap.put(
                            c.getIdUser(),
                            c.getNom() + " " + c.getPrenom()
                    )
            );
        } catch (Exception e) {
            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les coachs."
            );
        }
    }

    // =================================================
    // REFRESH TABLE
    // =================================================
    private void refreshTable() {

        try {
            masterData.setAll(seanceService.getAll());

            seanceTable.setPlaceholder(
                    new Label("Aucune séance disponible")
            );

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les séances."
            );
        }
    }

    // =================================================
    // RESERVER
    // =================================================
    @FXML
    private void handleReserver() {

        Seance selected =
                seanceTable.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        try {

            if (selected.getDateSeance().isBefore(LocalDate.now())) {
                DialogUtils.showWarning(
                        "Séance terminée",
                        "Vous ne pouvez pas réserver une séance passée."
                );
                return;
            }

            int reserved =
                    reservationService.countReservations(
                            selected.getIdSeance()
                    );

            if (selected.getCapacite() - reserved <= 0) {
                DialogUtils.showWarning(
                        "Séance complète",
                        "Il n'y a plus de places disponibles."
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
                    USER_TEST_ID,
                    selected.getIdSeance()
            );

            DialogUtils.showInfo(
                    "Succès",
                    "Séance réservée avec succès."
            );

            refreshTable();

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
