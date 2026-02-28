package GUI;

import Entities.Pack;
import Services.PackServiceUser;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class UserPackListController {

    @FXML private ListView<Pack> lvPacks;
    @FXML private TextField txtSearch;

    private final PackServiceUser packService = new PackServiceUser();
    private FilteredList<Pack> filtered;

    @FXML
    private void initialize() {
        lvPacks.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Pack p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    setText("📦 " + p.getNom()
                            + "\nType: " + p.getTypePack()
                            + " | Prix: " + p.getPrixBase() + " DT"
                            + " | Réduction: " + p.getReduction() + " DT"
                            + "\nCapacité: " + p.getNbActivitesMax());
                }
            }
        });

        loadPacks();

        txtSearch.textProperty().addListener((obs, o, n) -> {
            if (filtered == null) return;
            String q = (n == null) ? "" : n.trim().toLowerCase();
            filtered.setPredicate(p ->
                    q.isEmpty()
                            || p.getNom().toLowerCase().contains(q)
                            || String.valueOf(p.getTypePack()).toLowerCase().contains(q));
        });
    }

    private void loadPacks() {
        try {
            List<Pack> packs = packService.getActivePacks();
            filtered = new FilteredList<>(FXCollections.observableArrayList(packs), x -> true);
            lvPacks.setItems(filtered);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de charger les packs.").showAndWait();
        }
    }

    @FXML
    private void refresh() { loadPacks(); }

    @FXML
    private void continueToConfirm() {
        Pack selected = lvPacks.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Choisis un pack d'abord 🙂").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/UserPackInscription.fxml"));
            Parent root = loader.load();
            UserPackInscriptionController ctrl = loader.getController();
            ctrl.setSelectedPack(selected);

            BorderPane pane = (BorderPane) lvPacks.getScene().lookup("#mainPaneUser");
            if (pane != null) pane.setCenter(root);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la confirmation.").showAndWait();
        }
    }
}