package GUI;

import Entities.Seance;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.ReservationSeanceService;
import Services.interfaces.SeanceService;
import Services.interfaces.UserService;

import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class UserDashboardController {

    // ================= TABLES =================
    @FXML private TableView<Seance> tablePlanifiees;
    @FXML private TableView<Seance> tableTerminees;
    @FXML private Button btnAnnuler;
    @FXML private TableColumn<Seance, String> colNomP;
    @FXML private TableColumn<Seance, String> colNomT;

    @FXML private TableColumn<Seance, LocalDate> colDateP;
    @FXML private TableColumn<Seance, LocalTime> colDebutP;
    @FXML private TableColumn<Seance, LocalTime> colFinP;
    @FXML private TableColumn<Seance, String> colCoachP;

    @FXML private TableColumn<Seance, LocalDate> colDateT;
    @FXML private TableColumn<Seance, LocalTime> colDebutT;
    @FXML private TableColumn<Seance, LocalTime> colFinT;
    @FXML private TableColumn<Seance, String> colCoachT;

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

        btnAnnuler.disableProperty().bind(
                tablePlanifiees.getSelectionModel()
                        .selectedItemProperty().isNull()
        );

        tablePlanifiees.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        tableTerminees.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        loadCoachs();
        setupColumns();
        refreshTables();
    }

    // =================================================
    // CONFIG TABLE COLUMNS
    // =================================================
    private void setupColumns() {

        // 🔥 OBLIGATOIRE
        colNomP.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNom())
        );

        colNomP.setCellFactory(col -> new TableCell<>() {
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


        colNomT.setCellFactory(colNomP.getCellFactory());

        colNomT.setCellValueFactory(colNomP.getCellValueFactory());

        colDateP.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getDateSeance()));

        colDebutP.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getHeureDebut()));

        colFinP.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getHeureFin()));

        colCoachP.setCellValueFactory(d ->
                new SimpleStringProperty(
                        coachMap.getOrDefault(
                                d.getValue().getIdCoach(),
                                "Inconnu"
                        )
                )
        );

        // Réutilisation pour table terminées
        colDateT.setCellValueFactory(colDateP.getCellValueFactory());
        colDebutT.setCellValueFactory(colDebutP.getCellValueFactory());
        colFinT.setCellValueFactory(colFinP.getCellValueFactory());
        colCoachT.setCellValueFactory(colCoachP.getCellValueFactory());
    }

    // =================================================
    // REFRESH TABLES
    // =================================================
    private void refreshTables() {

        try {

            List<Seance> all =
                    seanceService.getSeancesByUser(USER_TEST_ID);

            List<Seance> planifiees = all.stream()
                    .filter(s ->
                            !s.getDateSeance().isBefore(LocalDate.now())
                    )
                    .toList();

            List<Seance> terminees = all.stream()
                    .filter(s ->
                            s.getDateSeance().isBefore(LocalDate.now())
                    )
                    .toList();

            tablePlanifiees.getItems().setAll(planifiees);
            tableTerminees.getItems().setAll(terminees);

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger vos séances."
            );
        }
    }

    // =================================================
    // LOAD COACHS CACHE
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
    // ANNULER RESERVATION
    // =================================================
    @FXML
    private void handleAnnuler() {

        Seance selected =
                tablePlanifiees.getSelectionModel()
                        .getSelectedItem();

        if (selected == null) return;

        boolean confirmed =
                DialogUtils.showConfirmation(
                        "Annulation de réservation",
                        "Voulez-vous vraiment annuler cette réservation ?"
                );

        if (!confirmed) return;

        try {

            reservationService.annuler(
                    USER_TEST_ID,
                    selected.getIdSeance()
            );

            DialogUtils.showInfo(
                    "Succès",
                    "Réservation annulée avec succès."
            );

            refreshTables();

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible d'annuler la réservation."
            );
        }
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
