package controllers;

import Entities.Evenement;
import Services.EvenementService;
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

public class EvenementCrudController {

    @FXML private TableView<Evenement> tableEvents;
    @FXML private TableColumn<Evenement, Number> colId;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colCategorie;
    @FXML private TableColumn<Evenement, String> colDate;
    @FXML private TableColumn<Evenement, String> colLieu;
    @FXML private TableColumn<Evenement, Number> colPlaces;
    @FXML private TableColumn<Evenement, String> colStatut;

    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    private final EvenementService service = new EvenementService();
    private final ObservableList<Evenement> data = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getIdEvenement()));
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getTitre())));
        colCategorie.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategorieEvt() == null ? "" : c.getValue().getCategorieEvt().name()
        ));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateEvent() == null ? "" : c.getValue().getDateEvent().format(dtf)
        ));
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getLieu())));
        colPlaces.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getNbPlaces()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getStatut())));

        tableEvents.setItems(data);
        loadAll();
    }

    private void loadAll() {
        try {
            List<Evenement> list = service.getAll();
            data.setAll(list);
            lblInfo.setText("Total: " + data.size());
        } catch (Exception e) {
            lblInfo.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        loadAll();
    }

    @FXML
    private void onSearch() {
        String q = nvl(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) { loadAll(); return; }

        try {
            List<Evenement> list = service.getAll();
            data.setAll(list.stream()
                    .filter(e -> nvl(e.getTitre()).toLowerCase(Locale.ROOT).contains(q)
                            || nvl(e.getLieu()).toLowerCase(Locale.ROOT).contains(q)
                            || (e.getCategorieEvt() != null && e.getCategorieEvt().name().toLowerCase(Locale.ROOT).contains(q))
                            || nvl(e.getStatut()).toLowerCase(Locale.ROOT).contains(q))
                    .collect(Collectors.toList()));
            lblInfo.setText("Résultats: " + data.size());
        } catch (Exception e) {
            lblInfo.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onEdit() {
        Evenement selected = tableEvents.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Info", "Sélection requise", "Sélectionne un événement.");
            return;
        }
        openForm(selected);
    }

    @FXML
    private void onDelete() {
        Evenement selected = tableEvents.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Info", "Sélection requise", "Sélectionne un événement.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'événement ?");
        confirm.setContentText("ID: " + selected.getIdEvenement() + " | " + selected.getTitre());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.delete(selected.getIdEvenement());
            loadAll();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible", e.getMessage());
        }
    }

    private void openForm(Evenement existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/evenement_form.fxml"));
            Scene scene = new Scene(loader.load());

            EvenementFormController ctrl = loader.getController();
            ctrl.setData(existing);
            ctrl.setOnSaved(this::loadAll);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(existing == null ? "Ajouter un événement" : "Modifier un événement");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire", e.getMessage());
        }
    }

    // =========================
    // NAVIGATION (HOME / RESERVATIONS)
    // =========================

    @FXML
    private void goHome(ActionEvent event) {
        switchScene(event, "/views/Home.fxml");
    }

    @FXML
    private void goToReservations(ActionEvent event) {
        switchScene(event, "/views/reservation_list.fxml");
    }

    @FXML
    private void goToEvenements(ActionEvent event) {
        switchScene(event, "/views/evenement_list.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            lblInfo.setText("Erreur navigation: " + e.getMessage());
        }
    }

    private void alert(Alert.AlertType type, String title, String header, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
