package GUI;

import Entities.ReservationSeance;
import Entities.Seance;
import Services.interfaces.ReservationSeanceService;
import enums.StatutPresence;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import GUI.utils.DialogUtils;
import java.time.LocalDate;
import java.util.List;

public class PresenceController {

    @FXML private Label lblSeanceTitle;
    @FXML private Label lblStats;
    @FXML private Label lblTaux;
    @FXML private TableColumn<ReservationSeance, Void> colAction;
    @FXML private TableView<ReservationSeance> tablePresence;
    @FXML private TableColumn<ReservationSeance, String> colNom;
    @FXML private TableColumn<ReservationSeance, String> colEmail;
    @FXML private TableColumn<ReservationSeance, StatutPresence> colStatut;

    private Seance currentSeance;
    private final ReservationSeanceService service =
            new ReservationSeanceService();

    private ObservableList<ReservationSeance> data =
            FXCollections.observableArrayList();

    public void setSeance(Seance s) {
        if (s.getDateSeance().isAfter(LocalDate.now())) {

            lblStats.setText("Séance non commencée");
            lblTaux.setText("Appel indisponible");

            tablePresence.setDisable(true);
        }
        lblSeanceTitle.getStyleClass().add("presence-title");
        lblStats.getStyleClass().add("presence-stats");
        lblTaux.getStyleClass().add("presence-taux");
        this.currentSeance = s;

        lblSeanceTitle.setText("Appel de présence - " + s.getNom());

        if (s.getDateSeance().isAfter(LocalDate.now())) {
            tablePresence.getStyleClass().add("table-disabled");
            tablePresence.setDisable(true);
        }

        loadData();
    }
    @FXML
    public void initialize() {

        tablePresence.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
        tablePresence.setPlaceholder(
                new Label("Aucun participant inscrit")
        );

        configureRowStyle();
    }
    private void configureRowStyle() {

        tablePresence.setRowFactory(tv -> new TableRow<>() {

            @Override
            protected void updateItem(ReservationSeance item, boolean empty) {

                super.updateItem(item, empty);

                getStyleClass().removeAll(
                        "row-present",
                        "row-absent",
                        "row-nonmarque"
                );

                if (item == null || empty) return;

                switch (item.getStatutPresence()) {
                    case PRESENT ->
                            getStyleClass().add("row-present");
                    case ABSENT ->
                            getStyleClass().add("row-absent");
                    case NON_MARQUE ->
                            getStyleClass().add("row-nonmarque");
                }
            }
        });
    }

    private void loadData() {

        List<ReservationSeance> list =
                service.getReservationsBySeance(
                        currentSeance.getIdSeance()
                );

        data.setAll(list);

        // 🔥 Sécuriser NON_MARQUE
        for (ReservationSeance r : data) {
            if (r.getStatutPresence() == null) {
                r.setStatutPresence(StatutPresence.NON_MARQUE);
            }
        }

        tablePresence.setItems(data);

        // ================= NOM =================
        colNom.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getUser() != null
                                ? c.getValue().getUser().getNom()
                                : "Inconnu"
                )
        );

        // ================= EMAIL =================
        colEmail.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getUser() != null
                                ? c.getValue().getUser().getEmail()
                                : "-"
                )
        );

        // ================= STATUT BADGE =================
        colStatut.setCellFactory(col -> new TableCell<>() {

            private final Label badge = new Label();

            {
                badge.getStyleClass().add("status-badge");
            }

            @Override
            protected void updateItem(StatutPresence statut, boolean empty) {
                super.updateItem(statut, empty);

                if (empty || statut == null) {
                    setGraphic(null);
                    return;
                }

                badge.getStyleClass().removeAll(
                        "badge-present",
                        "badge-absent",
                        "badge-neutral"
                );

                switch (statut) {
                    case PRESENT -> {
                        badge.setText("✔ Présent");
                        badge.getStyleClass().add("badge-present");
                    }
                    case ABSENT -> {
                        badge.setText("✖ Absent");
                        badge.getStyleClass().add("badge-absent");
                    }
                    case NON_MARQUE -> {
                        badge.setText("• Non marqué");
                        badge.getStyleClass().add("badge-neutral");
                    }
                }

                setGraphic(badge);
            }
        });

        colStatut.setCellValueFactory(
                new PropertyValueFactory<>("statutPresence")
        );

        // ================= ACTION BUTTONS =================
        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button btnPresent = new Button("✔");
            private final Button btnAbsent = new Button("✖");
            private final HBox pane = new HBox(8, btnPresent, btnAbsent);

            {
                btnPresent.getStyleClass().add("btn-present");
                btnAbsent.getStyleClass().add("btn-absent");
                pane.setAlignment(Pos.CENTER);

                btnPresent.setOnAction(e -> {
                    ReservationSeance r = getTableView().getItems().get(getIndex());
                    updatePresence(r, StatutPresence.PRESENT);
                });

                btnAbsent.setOnAction(e -> {
                    ReservationSeance r = getTableView().getItems().get(getIndex());
                    updatePresence(r, StatutPresence.ABSENT);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                ReservationSeance r =
                        getTableView().getItems().get(getIndex());

                btnPresent.setDisable(
                        r.getStatutPresence() == StatutPresence.PRESENT
                );

                btnAbsent.setDisable(
                        r.getStatutPresence() == StatutPresence.ABSENT
                );

                setGraphic(pane);
            }
        });

        updateStats();
    }
    private void updateStats() {

        long present = data.stream()
                .filter(r -> r.getStatutPresence() == StatutPresence.PRESENT)
                .count();

        long absent = data.stream()
                .filter(r -> r.getStatutPresence() == StatutPresence.ABSENT)
                .count();

        long total = data.size();

        double taux = total == 0 ? 0 :
                (double) present / total * 100;

        lblStats.setText(
                "Présents: " + present +
                        " | Absents: " + absent +
                        " | Total: " + total
        );

        lblTaux.setText(
                "Taux de présence: "
                        + String.format("%.1f %%", taux)
        );
    }

    @FXML
    private void handleSave() {

        for (ReservationSeance r : data) {
            service.updatePresence(
                    r.getIdReservation(),
                    r.getStatutPresence()
            );
        }

        updateStats();
        DialogUtils.showInfo(
                "Succès",
                "Présences sauvegardées avec succès."
        );
    }

    @FXML
    private void handleClose() {

        Stage stage = (Stage) tablePresence.getScene().getWindow();
        stage.close();
    }
    private void updatePresence(ReservationSeance r, StatutPresence statut) {

        try {

            r.setStatutPresence(statut);

            service.updatePresence(
                    r.getIdReservation(),
                    statut
            );

            tablePresence.refresh();
            updateStats();
            animateSingleRow(r);

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de mettre à jour la présence."
            );

            e.printStackTrace();
        }
    }
    private void animateSingleRow(ReservationSeance r) {

        for (TableRow<ReservationSeance> row : tablePresence.lookupAll(".table-row-cell")
                .stream()
                .map(n -> (TableRow<ReservationSeance>) n)
                .toList()) {

            if (row.getItem() == r) {

                FadeTransition ft = new FadeTransition(
                        Duration.millis(250),
                        row
                );

                ft.setFromValue(0.5);
                ft.setToValue(1);
                ft.play();

                break;
            }
        }
    }
    @FXML
    private void handleCloturer() {


        boolean confirmed = DialogUtils.showConfirmation(
                "Clôturer séance",
                "Clôturer définitivement cette séance ?"
        );

        if (confirmed) {

            tablePresence.setDisable(true);

            DialogUtils.showInfo(
                    "Séance clôturée",
                    "La séance a été clôturée avec succès."
            );
        }
        }

    }
