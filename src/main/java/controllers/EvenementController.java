package controllers;

import Entities.Evenement;
import Services.EvenementService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class EvenementController {

    // ===== Table =====
    @FXML private TableView<Evenement> tableEvents;

    @FXML private TableColumn<Evenement, Number> colId;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colCategorie;
    @FXML private TableColumn<Evenement, String> colDate;
    @FXML private TableColumn<Evenement, String> colLieu;
    @FXML private TableColumn<Evenement, Number> colPlaces;
    @FXML private TableColumn<Evenement, String> colStatut;

    @FXML private Label lblInfo;

    private final EvenementService evenementService = new EvenementService();
    private final ObservableList<Evenement> data = FXCollections.observableArrayList();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getIdEvenement()));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getTitre())));
        colCategorie.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getCategorieEvt() == null ? "" : c.getValue().getCategorieEvt().name())
        );
        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getDateEvent() == null ? "" : c.getValue().getDateEvent().format(dtf))
        );
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getLieu())));
        colPlaces.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getNbPlaces()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getStatut())));

        tableEvents.setItems(data);

        loadEvents();
    }

    // ===== Load =====
    public void loadEvents() {
        try {
            data.setAll(evenementService.getAll());
            if (lblInfo != null)
                lblInfo.setText("Total événements : " + data.size());
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Erreur", "Chargement impossible", e.getMessage());
        }
    }

    // ===== Buttons =====
    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onEdit() {
        Evenement selected = tableEvents.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Info", "Sélection requise", "Choisis un événement.");
            return;
        }
        openForm(selected);
    }

    @FXML
    private void onDelete() {
        Evenement selected = tableEvents.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Info", "Sélection requise", "Choisis un événement.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cet événement ?");
        confirm.setContentText(selected.getTitre());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            evenementService.delete(selected.getIdEvenement());
            loadEvents();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible", e.getMessage());
        }
    }

    // ===== Popup =====
    private void openForm(Evenement existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/evenement_form.fxml"));
            Parent root = loader.load();

            EvenementFormController formCtrl = loader.getController();
            formCtrl.setData(existing);
            formCtrl.setOnSaved(this::loadEvents);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(existing == null ? "Ajouter événement" : "Modifier événement");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d’ouvrir le formulaire", e.getMessage());
        }
    }

    // ===== Utils =====
    private void alert(Alert.AlertType type, String title, String header, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
