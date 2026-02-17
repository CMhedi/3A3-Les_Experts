package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ReservationClientController {

    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    @FXML private TableView<ReservationEvenement> tableReservations;
    @FXML private TableColumn<ReservationEvenement, Number> colId;
    @FXML private TableColumn<ReservationEvenement, String> colDate;
    @FXML private TableColumn<ReservationEvenement, String> colStatut;
    @FXML private TableColumn<ReservationEvenement, Number> colBillets;
    @FXML private TableColumn<ReservationEvenement, Number> colEvent;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private final ObservableList<ReservationEvenement> data = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Integer currentEventId = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getIdResEvt()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateReservation() == null ? "" : c.getValue().getDateReservation().format(dtf)
        ));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatutRes() == null ? "" : c.getValue().getStatutRes().name()
        ));
        colBillets.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getNbBillets()));
        colEvent.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getIdEvenement()));

        tableReservations.setItems(data);
        loadAll();
    }

    public void loadReservationsByEventId(int eventId) {
        this.currentEventId = eventId;
        if (searchField != null) searchField.clear();
        loadAll();
        if (lblInfo != null) lblInfo.setText("Événement ID: " + eventId + " | Total: " + data.size());
    }

    private void loadAll() {
        try {
            List<ReservationEvenement> list = service.getAll();
            if (currentEventId != null) {
                list = list.stream().filter(r -> r.getIdEvenement() == currentEventId).collect(Collectors.toList());
            }
            data.setAll(list);
            if (lblInfo != null && currentEventId == null) lblInfo.setText("Total: " + data.size());
        } catch (Exception e) {
            if (lblInfo != null) lblInfo.setText("Erreur chargement: " + e.getMessage());
        }
    }
    @FXML private void onRefresh() {
        if (searchField != null) searchField.clear();
        loadAll();
    }

    @FXML private void onSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) { loadAll(); return; }

        try {
            List<ReservationEvenement> list = service.getAll();
            if (currentEventId != null) {
                list = list.stream().filter(r -> r.getIdEvenement() == currentEventId).collect(Collectors.toList());
            }

            data.setAll(list.stream()
                    .filter(r -> (r.getStatutRes() != null && r.getStatutRes().name().toLowerCase(Locale.ROOT).contains(q))
                            || String.valueOf(r.getIdEvenement()).contains(q))
                    .collect(Collectors.toList()));
            if (lblInfo != null) lblInfo.setText("Résultats: " + data.size());
        } catch (Exception e) {
            if (lblInfo != null) lblInfo.setText("Erreur recherche: " + e.getMessage());
        }
    }

    @FXML private void goHome() { switchScene("/views/HomeClient.fxml"); }
    @FXML private void goToReservations() { switchScene("/views/reservation_list_client.fxml"); }
    @FXML private void goToEvenements() { switchScene("/views/evenement_list_client.fxml"); }
    @FXML private void logout() { switchScene("/views/Login.fxml"); }

    private void switchScene(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) tableReservations.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            if (lblInfo != null) lblInfo.setText("Erreur navigation: " + e.getMessage());
        }
    }
}
