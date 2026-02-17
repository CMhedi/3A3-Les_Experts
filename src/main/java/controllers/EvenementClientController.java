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
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EvenementClientController {

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

        tableEvents.setRowFactory(tv -> {
            TableRow<Evenement> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Evenement ev = row.getItem();
                    openReservationsClient(ev.getIdEvenement());
                }
            });
            return row;
        });
    }

    private void loadAll() {
        try {
            List<Evenement> list = service.getAll();
            data.setAll(list);
            if (lblInfo != null) lblInfo.setText("Total: " + data.size());
        } catch (Exception e) {
            if (lblInfo != null) lblInfo.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        if (searchField != null) searchField.clear();
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
            if (lblInfo != null) lblInfo.setText("Résultats: " + data.size());
        } catch (Exception e) {
            if (lblInfo != null) lblInfo.setText("Erreur: " + e.getMessage());
        }
    }

    private void openReservationsClient(int eventId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservation_list_client.fxml"));
            Parent root = loader.load();

            ReservationClientController ctrl = loader.getController();
            ctrl.loadReservationsByEventId(eventId);

            Stage stage = (Stage) tableEvents.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            if (lblInfo != null) lblInfo.setText("Erreur ouverture réservations: " + e.getMessage());
        }
    }

    @FXML
    private void goHome() { switchScene("/views/HomeClient.fxml"); }

    @FXML
    private void goToReservations() { switchScene("/views/reservation_list_client.fxml"); }

    @FXML
    private void goToEvenements() { switchScene("/views/evenement_list_client.fxml"); }

    @FXML
    private void logout() { switchScene("/views/Login.fxml"); }

    private void switchScene(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) tableEvents.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            if (lblInfo != null) lblInfo.setText("Erreur navigation: " + e.getMessage());
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
