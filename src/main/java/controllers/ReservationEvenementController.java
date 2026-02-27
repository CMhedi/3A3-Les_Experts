package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import Utiles.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDateTime;

public class ReservationEvenementController {

    @FXML private TableView<ReservationEvenement> table;
    @FXML private TableColumn<ReservationEvenement, Integer> colIdRes;
    @FXML private TableColumn<ReservationEvenement, Integer> colIdEvent;
    @FXML private TableColumn<ReservationEvenement, Integer> colNbBillets;
    @FXML private TableColumn<ReservationEvenement, String> colStatut;
    @FXML private TableColumn<ReservationEvenement, LocalDateTime> colDateRes;
    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private ObservableList<ReservationEvenement> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colIdRes.setCellValueFactory(new PropertyValueFactory<>("idResEvt"));
        colIdEvent.setCellValueFactory(new PropertyValueFactory<>("idEvenement"));
        colNbBillets.setCellValueFactory(new PropertyValueFactory<>("nbBillets"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutRes"));
        colDateRes.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));
        loadAll();
    }

    private void loadAll() {
        try {
            masterData.setAll(service.getAll());
            table.setItems(masterData);
            if (lblInfo != null) lblInfo.setText(masterData.size() + " réservation(s)");
        } catch (Exception e) {
            System.err.println("Erreur chargement: " + e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onDelete() {
        try {
            ReservationEvenement selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la réservation ?", ButtonType.YES, ButtonType.NO);
                if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    service.delete(selected.getIdResEvt()); // Lezemha try-catch
                    loadAll();
                }
            } else {
                new Alert(Alert.AlertType.WARNING, "Sélectionnez une ligne !").show();
            }
        } catch (Exception e) {
            System.err.println("Erreur suppression: " + e.getMessage());
        }
    }

    private void openForm(ReservationEvenement res) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservation_form_popup.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            ReservationEvenementFormController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setData(res);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();
                if (ctrl.isSaved()) loadAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goHome(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            SceneNavigator.go(stage, "/views/Home.fxml", "EcoAdventure - Accueil");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText().toLowerCase().trim();
        table.setItems(masterData.filtered(r -> String.valueOf(r.getIdEvenement()).contains(q)));
    }

    @FXML private void onRefresh() { searchField.clear(); loadAll(); }
}