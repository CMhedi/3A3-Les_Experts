package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ReservationEvenementController {

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

    private void loadAll() {
        try {
            data.setAll(service.getAll());
            lblInfo.setText("Total: " + data.size());
        } catch (Exception e) {
            lblInfo.setText("Erreur chargement: " + e.getMessage());
        }
    }

    @FXML private void onRefresh() {
        searchField.clear();
        loadAll();
    }

    @FXML private void onSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) { loadAll(); return; }

        try {
            List<ReservationEvenement> list = service.getAll();
            data.setAll(list.stream()
                    .filter(r -> (r.getStatutRes() != null && r.getStatutRes().name().toLowerCase(Locale.ROOT).contains(q))
                            || String.valueOf(r.getIdEvenement()).contains(q))
                    .collect(Collectors.toList()));
            lblInfo.setText("Résultats: " + data.size());
        } catch (Exception e) {
            lblInfo.setText("Erreur recherche: " + e.getMessage());
        }
    }

    @FXML private void onAdd() { openPopup(null); }

    @FXML private void onEdit() {
        ReservationEvenement selected = tableReservations.getSelectionModel().getSelectedItem();
        if (selected == null) { lblInfo.setText("Sélectionne une réservation."); return; }
        openPopup(selected);
    }

    @FXML private void onDelete() {
        ReservationEvenement selected = tableReservations.getSelectionModel().getSelectedItem();
        if (selected == null) { lblInfo.setText("Sélectionne une réservation."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la réservation ?");
        confirm.setContentText("ID: " + selected.getIdResEvt());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.delete(selected.getIdResEvt());
            loadAll();
        } catch (Exception e) {
            lblInfo.setText("Erreur suppression: " + e.getMessage());
        }
    }

    private void openPopup(ReservationEvenement existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservation_form_popup.fxml"));
            Scene scene = new Scene(loader.load());

            ReservationEvenementFormController ctrl = loader.getController();
            ctrl.setData(existing);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(existing == null ? "Nouvelle réservation" : "Modifier réservation");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

            if (ctrl.isSaved()) loadAll();

        } catch (Exception e) {
            lblInfo.setText("Erreur popup: " + e.getMessage());
        }
    }

    @FXML
    public void goHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Home.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            lblInfo.setText("Erreur navigation: " + e.getMessage());
        }
    }
}
