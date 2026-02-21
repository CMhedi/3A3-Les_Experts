package controllers;

import Entities.Pack;
import Services.PackService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PackListController {

    // ✅ TableWidget nodes
    @FXML private VBox rowsBox;

    @FXML private TextField txtSearch;
    @FXML private Label lblTotal;

    private final PackService service = new PackService();
    private final ObservableList<Pack> master = FXCollections.observableArrayList();

    // selection (remplace TableView selection)
    private Pack selectedPack = null;
    private HBox selectedRow = null;

    @FXML
    private void initialize() {
        txtSearch.textProperty().addListener((obs, o, n) -> render());
        refresh();
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onEdit() {
        if (selectedPack == null) {
            showInfo("Sélection requise", "Veuillez sélectionner un pack à modifier.");
            return;
        }
        openForm(selectedPack);
    }

    @FXML
    private void onDelete() {
        if (selectedPack == null) {
            showInfo("Sélection requise", "Veuillez sélectionner un pack à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer le pack « " + safe(selectedPack.getNom()) + " » ?");
        confirm.setContentText("Cette action est irréversible.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            // ✅ suppression par ID (interne), mais on ne l'affiche jamais
            service.delete(selectedPack.getIdPack());
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        try {
            master.setAll(service.getAll());
            clearSelection();
            render();
        } catch (Exception e) {
            showError(e);
        }
    }

    // =========================
    // TableWidget rendering
    // =========================
    private void render() {
        rowsBox.getChildren().clear();

        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        List<Pack> visible = new ArrayList<>();
        for (Pack p : master) {
            if (q.isEmpty() || matchPack(p, q)) visible.add(p);
        }

        // tri simple
        visible.sort(Comparator.comparing(p -> safe(p.getNom()).toLowerCase()));

        for (Pack p : visible) {
            rowsBox.getChildren().add(buildRow(p));
        }

        lblTotal.setText(visible.size() + " / " + master.size());
    }

    private boolean matchPack(Pack p, String q) {
        String nom = safe(p.getNom()).toLowerCase();
        String type = p.getTypePack() == null ? "" : p.getTypePack().name().toLowerCase();
        String statut = p.getStatutPack() == null ? "" : p.getStatutPack().name().toLowerCase();
        return nom.contains(q) || type.contains(q) || statut.contains(q);
    }

    private HBox buildRow(Pack p) {
        Label lNom = cell(safe(p.getNom()), 240);
        Label lType = cell(p.getTypePack() == null ? "" : p.getTypePack().name(), 170);
        Label lPrix = cell(p.getPrixBase() + " DT", 120);
        Label lMax = cell(String.valueOf(p.getNbActivitesMax()), 140);
        Label lStatut = cell(p.getStatutPack() == null ? "" : p.getStatutPack().name(), 140);

        HBox row = new HBox(lNom, lType, lPrix, lMax, lStatut);
        row.setSpacing(0);
        row.setStyle("""
                -fx-background-color: rgba(255,255,255,0.45);
                -fx-border-color: rgba(0,0,0,0.08);
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-padding: 10;
                """);

        row.setOnMouseClicked(e -> selectRow(row, p));
        row.setOnMouseEntered(e -> {
            if (row != selectedRow) row.setStyle(row.getStyle() + "-fx-background-color: rgba(255,255,255,0.65);");
        });
        row.setOnMouseExited(e -> {
            if (row != selectedRow) row.setStyle("""
                -fx-background-color: rgba(255,255,255,0.45);
                -fx-border-color: rgba(0,0,0,0.08);
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-padding: 10;
                """);
        });

        // double click = edit
        row.setOnMouseClicked(e -> {
            selectRow(row, p);
            if (e.getClickCount() == 2) onEdit();
        });

        return row;
    }

    private void selectRow(HBox row, Pack p) {
        if (selectedRow != null) {
            selectedRow.setStyle("""
                -fx-background-color: rgba(255,255,255,0.45);
                -fx-border-color: rgba(0,0,0,0.08);
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-padding: 10;
                """);
        }
        selectedRow = row;
        selectedPack = p;

        row.setStyle("""
                -fx-background-color: rgba(120, 72, 255, 0.18);
                -fx-border-color: rgba(120, 72, 255, 0.55);
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-padding: 10;
                """);
    }

    private void clearSelection() {
        selectedPack = null;
        selectedRow = null;
    }

    private Label cell(String text, double w) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        return l;
    }

    // =========================
    // Form dialog
    // =========================
    private void openForm(Pack pack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PackForm.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

            PackFormController controller = loader.getController();
            controller.setPack(pack);
            controller.setOnSaved(this::refresh);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle(pack == null ? "Ajouter un pack" : "Modifier un pack");
            st.setScene(scene);
            st.showAndWait();
        } catch (Exception e) {
            showError(e);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText("Une erreur est survenue");
        a.setContentText(e.getMessage());
        a.showAndWait();
    }
}
