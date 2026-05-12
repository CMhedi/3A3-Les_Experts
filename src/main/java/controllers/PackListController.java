package controllers;

import Entities.Pack;
import Services.PackService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PackListController {

    @FXML private VBox rowsBox;
    @FXML private TextField txtSearch;
    @FXML private Label lblTotal;

    private final PackService service = new PackService();
    private final ObservableList<Pack> master = FXCollections.observableArrayList();

    private Pack selectedPack = null;
    private GridPane selectedRow = null;

    // must match FXML header widths
    private static final double W_NOM = 240;
    private static final double W_TYPE = 170;
    private static final double W_PRIX = 120;
    private static final double W_MAX = 140;
    private static final double W_STATUT = 140;
    private static final double W_ACTIONS = 120;

    @FXML
    private void initialize() {

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, o, n) -> render());
            txtSearch.focusedProperty().addListener((obs, was, isNow) ->
                    toggleClass(txtSearch, "is-focused", isNow)
            );
        }

        Platform.runLater(() -> {
            playPageEntrance();
            enableButtonHoverPressClasses();
        });

        refresh();
    }

    @FXML private void onAdd() { openForm(null); }

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
            service.delete(selectedPack.getIdPack());
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML private void onRefresh() { refresh(); }

    private void refresh() {
        try {
            master.setAll(service.getAll());
            clearSelection();
            render();
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void onOpenQrPdfDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QrPdfDialog.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm()); }
            catch (Exception ignored) {}
            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Export PDF & QR Code");
            st.setScene(scene);
            st.setResizable(true);
            st.showAndWait();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void render() {
        if (rowsBox == null) return;

        rowsBox.getChildren().clear();

        String q = (txtSearch == null || txtSearch.getText() == null)
                ? ""
                : txtSearch.getText().trim().toLowerCase();

        List<Pack> visible = new ArrayList<>();
        for (Pack p : master) {
            if (q.isEmpty() || matchPack(p, q)) visible.add(p);
        }
        visible.sort(Comparator.comparing(p -> safe(p.getNom()).toLowerCase()));

        int i = 0;
        for (Pack p : visible) {
            GridPane row = buildRow(p, i);

            row.setOpacity(0);
            row.setTranslateY(10);
            rowsBox.getChildren().add(row);

            PauseTransition delay = new PauseTransition(Duration.millis(i * 25L));
            delay.setOnFinished(e -> playRowAppear(row));
            delay.play();

            i++;
        }

        if (lblTotal != null) lblTotal.setText(visible.size() + " / " + master.size());
    }

    private boolean matchPack(Pack p, String q) {
        String nom = safe(p.getNom()).toLowerCase();
        String type = p.getTypePack() == null ? "" : p.getTypePack().name().toLowerCase();
        String statut = p.getStatutPack() == null ? "" : p.getStatutPack().name().toLowerCase();
        return nom.contains(q) || type.contains(q) || statut.contains(q);
    }

    private GridPane buildRow(Pack p, int index) {

        // cells
        Label cNom = cell(safe(p.getNom()));
        Label cType = cell(p.getTypePack() == null ? "" : p.getTypePack().name());

        String prix = (p.getPrixBase() == null ? "0" : p.getPrixBase().toPlainString()) + " DT";
        Label cPrix = cell(prix); cPrix.getStyleClass().add("num");

        Label cMax = cell(String.valueOf(p.getNbActivitesMax())); cMax.getStyleClass().add("num");

        String stTxt = p.getStatutPack() == null ? "" : p.getStatutPack().name();
        boolean actif = stTxt.toUpperCase().contains("ACTIF");
        Label cStatut = new Label(stTxt);
        cStatut.getStyleClass().addAll("pack-cell","badge", actif ? "badge-active" : "badge-inactive");

        // actions
        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("icon-btn");
        Button btnDel = new Button("🗑");
        btnDel.getStyleClass().addAll("icon-btn","danger");

        btnEdit.setOnMouseEntered(e -> toggleClass(btnEdit, "is-hover", true));
        btnEdit.setOnMouseExited(e -> toggleClass(btnEdit, "is-hover", false));
        btnDel.setOnMouseEntered(e -> toggleClass(btnDel, "is-hover", true));
        btnDel.setOnMouseExited(e -> toggleClass(btnDel, "is-hover", false));

        HBox actions = new HBox(btnEdit, btnDel);
        actions.getStyleClass().addAll("row-actions","cell-last"); // ✅ no right border last column

        // row grid
        GridPane row = new GridPane();
        row.getStyleClass().add("pack-row");

        row.getColumnConstraints().addAll(
                cc(W_NOM), cc(W_TYPE), cc(W_PRIX), cc(W_MAX), cc(W_STATUT), cc(W_ACTIONS)
        );

        row.add(cNom, 0, 0);
        row.add(cType, 1, 0);
        row.add(cPrix, 2, 0);
        row.add(cMax, 3, 0);
        row.add(cStatut, 4, 0);
        row.add(actions, 5, 0);

        // ✅ remove right border on last text cells if needed
        // We keep separators for first 5 cells (pack-cell). Last is actions (cell-last).

        if (index % 2 == 1) row.getStyleClass().add("is-alt");

        row.setOnMouseEntered(e -> toggleClass(row, "is-hover", true));
        row.setOnMouseExited(e -> toggleClass(row, "is-hover", false));
        row.setOnMouseClicked(e -> {
            selectRow(row, p);
            if (e.getClickCount() == 2) onEdit();
        });

        btnEdit.setOnAction(e -> { selectRow(row, p); onEdit(); });
        btnDel.setOnAction(e -> { selectRow(row, p); onDelete(); });

        return row;
    }

    private ColumnConstraints cc(double w) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPrefWidth(w);
        c.setMinWidth(w);
        c.setMaxWidth(w);
        return c;
    }

    private Label cell(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("pack-cell");
        return l;
    }

    private void selectRow(GridPane row, Pack p) {
        if (selectedRow != null) selectedRow.getStyleClass().remove("is-selected");
        selectedRow = row;
        selectedPack = p;

        if (!row.getStyleClass().contains("is-selected")) row.getStyleClass().add("is-selected");

        ScaleTransition st1 = new ScaleTransition(Duration.millis(120), row);
        st1.setToX(1.015); st1.setToY(1.015);
        st1.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition st2 = new ScaleTransition(Duration.millis(140), row);
        st2.setToX(1.0); st2.setToY(1.0);
        st2.setInterpolator(Interpolator.EASE_BOTH);

        new SequentialTransition(st1, st2).play();
    }

    private void clearSelection() {
        selectedPack = null;
        if (selectedRow != null) selectedRow.getStyleClass().remove("is-selected");
        selectedRow = null;
    }

    private void playRowAppear(GridPane row) {
        FadeTransition ft = new FadeTransition(Duration.millis(180), row);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), row);
        tt.setToY(0);
        new ParallelTransition(ft, tt).play();
    }

    private void playPageEntrance() {
        if (rowsBox == null || rowsBox.getScene() == null) return;
        Parent root = rowsBox.getScene().getRoot();
        root.setOpacity(0);
        root.setTranslateY(10);
        FadeTransition ft = new FadeTransition(Duration.millis(240), root);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(240), root);
        tt.setToY(0);
        new ParallelTransition(ft, tt).play();
    }

    private void enableButtonHoverPressClasses() {
        if (rowsBox == null || rowsBox.getScene() == null) return;
        Parent root = rowsBox.getScene().getRoot();
        for (Node n : root.lookupAll(".tool-btn")) {
            if (n instanceof Button b) {
                b.setOnMouseEntered(e -> toggleClass(b, "is-hover", true));
                b.setOnMouseExited(e -> { toggleClass(b, "is-hover", false); toggleClass(b, "is-pressed", false); });
                b.setOnMousePressed(e -> toggleClass(b, "is-pressed", true));
                b.setOnMouseReleased(e -> toggleClass(b, "is-pressed", false));
            }
        }
    }

    private static void toggleClass(Node node, String cls, boolean on) {
        if (node == null) return;
        if (on) {
            if (!node.getStyleClass().contains(cls)) node.getStyleClass().add(cls);
        } else {
            node.getStyleClass().remove(cls);
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void openForm(Pack pack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PackForm.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            try { scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm()); }
            catch (Exception ignored) {}
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