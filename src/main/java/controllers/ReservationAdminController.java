package controllers;

import Entities.ReservationEvenement;
import enums.StatutReservation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class ReservationAdminController implements Initializable {

    @FXML private TableView<ReservationEvenement> reservationTable;
    @FXML private TableColumn<ReservationEvenement, Integer> colId;
    @FXML private TableColumn<ReservationEvenement, LocalDateTime> colDate;
    @FXML private TableColumn<ReservationEvenement, StatutReservation> colStatus;
    @FXML private TableColumn<ReservationEvenement, Integer> colBillets;
    @FXML private TableColumn<ReservationEvenement, Integer> colEventId;
    @FXML private TextField searchReservationField;

    private ObservableList<ReservationEvenement> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mapping m3a el getters mta3 l'entité mte3ek
        colId.setCellValueFactory(new PropertyValueFactory<>("idResEvt"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statutRes"));
        colBillets.setCellValueFactory(new PropertyValueFactory<>("nbBillets"));
        colEventId.setCellValueFactory(new PropertyValueFactory<>("idEvenement"));

        loadDataFromDatabase();
    }

    private void loadDataFromDatabase() {
        // Houni t3ayet lel Service mte3ek (CRUD)
        // Exemple mock data bech tjarreb bih tawa:
        /*
        ReservationEvenement r1 = new ReservationEvenement();
        r1.setIdResEvt(1);
        r1.setDateReservation(LocalDateTime.now());
        r1.setStatutRes(StatutReservation.CONFIRMEE); // Thabbet f esm el enum
        r1.setNbBillets(3);
        r1.setIdEvenement(101);
        masterData.add(r1);
        */

        reservationTable.setItems(masterData);
    }

    @FXML
    private void onSearchReservation() {
        // Recherche simple par ID Event
        String filter = searchReservationField.getText();
        if (filter.isEmpty()) {
            reservationTable.setItems(masterData);
        } else {
            ObservableList<ReservationEvenement> filtered = masterData.filtered(r ->
                    String.valueOf(r.getIdEvenement()).contains(filter)
            );
            reservationTable.setItems(filtered);
        }
    }

    @FXML
    private void onDeleteReservation() {
        ReservationEvenement selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            masterData.remove(selected);
            // Service.delete(selected.getIdResEvt());
        }
    }

    @FXML private void goHome() { /* Navigation */ }
    @FXML private void goToEvents() { /* Navigation */ }
}