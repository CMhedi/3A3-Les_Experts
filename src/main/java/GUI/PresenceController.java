package GUI;

import Entities.ReservationSeance;
import Entities.Seance;
import Services.interfaces.ReservationSeanceService;
import enums.StatutPresence;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class PresenceController {

    @FXML private Label lblSeanceTitle;
    @FXML private Label lblStats;
    @FXML private Label lblTaux;

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

        this.currentSeance = s;

        lblSeanceTitle.setText("Appel de présence - " + s.getNom());

        if (s.getDateSeance().isAfter(LocalDate.now())) {
            tablePresence.setDisable(true);
        }

        loadData();
    }
    @FXML
    public void initialize() {

        configureRowStyle();
    }
    private void configureRowStyle() {

        tablePresence.setRowFactory(tv -> new TableRow<>() {

            @Override
            protected void updateItem(ReservationSeance item, boolean empty) {

                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                    return;
                }

                switch (item.getStatutPresence()) {
                    case PRESENT ->
                            setStyle("-fx-background-color: #e8f5e9;");
                    case ABSENT ->
                            setStyle("-fx-background-color: #ffebee;");
                    case NON_MARQUE ->
                            setStyle("");
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

        // ================= STATUT =================
        colStatut.setCellValueFactory(
                new PropertyValueFactory<>("statutPresence")
        );

        colStatut.setCellFactory(
                ComboBoxTableCell.forTableColumn(
                        StatutPresence.values()
                )
        );

        colStatut.setOnEditCommit(event -> {

            ReservationSeance r = event.getRowValue();

            r.setStatutPresence(event.getNewValue());

            service.updatePresence(
                    r.getIdReservation(),
                    r.getStatutPresence()
            );

            tablePresence.refresh(); // 🔥 مهم
            updateStats();
        });

        tablePresence.setEditable(true);

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

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Présences sauvegardées !");
        alert.showAndWait();
    }

    @FXML
    private void handleClose() {

        Stage stage = (Stage) tablePresence.getScene().getWindow();
        stage.close();
    }
}