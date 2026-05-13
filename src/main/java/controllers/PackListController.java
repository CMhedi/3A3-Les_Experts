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

    private Pack    selectedPack = null;
    private GridPane selectedRow  = null;
    private String   selectedRowBaseStyle = ""; // pour restaurer après déselection

    // ─── Largeurs colonnes — DOIVENT correspondre au FXML header ───
    private static final double W_NOM       = 200;
    private static final double W_TYPE      = 130;
    private static final double W_PRIX      = 120;
    private static final double W_REDUCTION = 110;
    private static final double W_MAX       = 120;
    private static final double W_STATUT    = 120;
    private static final double W_ACTIONS   = 150;

    // ─── Palette de couleurs lignes ───
    private static final String BG_EVEN     = "-fx-background-color: white;";
    private static final String BG_ODD      = "-fx-background-color: #f8fafc;";
    private static final String BG_HOVER    = "-fx-background-color: #eff6ff;";
    private static final String BG_SELECTED = "-fx-background-color: #dbeafe; "
            + "-fx-border-color: #3b82f6 transparent transparent transparent; "
            + "-fx-border-width: 1;";
    private static final String CELL_BASE   = "-fx-text-fill: #1e293b; "
            + "-fx-font-size: 13; "
            + "-fx-padding: 10 14 10 8;";
    private static final String ROW_BORDER  = "-fx-border-color: transparent transparent #e2e8f0 transparent; "
            + "-fx-border-width: 1;";

    @FXML
    private void initialize() {
        if (txtSearch != null)
            txtSearch.textProperty().addListener((obs, o, n) -> render());

        Platform.runLater(() -> {
            playPageEntrance();
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
        } catch (Exception e) { showError(e); }
    }

    @FXML private void onRefresh() { refresh(); }

    private void refresh() {
        try {
            master.setAll(service.getAll());
            clearSelection();
            render();
        } catch (Exception e) { showError(e); }
    }

    @FXML
    private void onOpenQrPdfDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QrPdfDialog.fxml"));
            Parent root = loader.load();
            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Export PDF & QR Code");
            st.setScene(new Scene(root));
            st.setResizable(true);
            st.showAndWait();
        } catch (Exception e) { showError(e); }
    }

    // ═══════════════════════════════════════════════════════
    //  RENDU DU TABLEAU
    // ═══════════════════════════════════════════════════════

    private void render() {
        if (rowsBox == null) return;
        rowsBox.getChildren().clear();

        String q = (txtSearch == null || txtSearch.getText() == null)
                ? "" : txtSearch.getText().trim().toLowerCase();

        List<Pack> visible = new ArrayList<>();
        for (Pack p : master)
            if (q.isEmpty() || matchPack(p, q)) visible.add(p);

        visible.sort(Comparator.comparing(p -> safe(p.getNom()).toLowerCase()));

        int i = 0;
        for (Pack p : visible) {
            GridPane row = buildRow(p, i);
            row.setOpacity(0);
            row.setTranslateY(8);
            rowsBox.getChildren().add(row);

            PauseTransition delay = new PauseTransition(Duration.millis(i * 22L));
            delay.setOnFinished(e -> playRowAppear(row));
            delay.play();
            i++;
        }

        if (lblTotal != null) lblTotal.setText(visible.size() + " / " + master.size());
    }

    private boolean matchPack(Pack p, String q) {
        String nom    = safe(p.getNom()).toLowerCase();
        String type   = p.getTypePack()   == null ? "" : p.getTypePack().name().toLowerCase();
        String statut = p.getStatutPack() == null ? "" : p.getStatutPack().name().toLowerCase();
        return nom.contains(q) || type.contains(q) || statut.contains(q);
    }

    // ═══════════════════════════════════════════════════════
    //  CONSTRUCTION D'UNE LIGNE — styles 100 % inline
    // ═══════════════════════════════════════════════════════

    private GridPane buildRow(Pack p, int index) {

        // ─── Cellules texte ───
        Label cNom       = cell(safe(p.getNom()));
        Label cType      = cell(p.getTypePack() == null ? "" : p.getTypePack().name());

        String prixTxt = (p.getPrixBase() == null ? "0" : p.getPrixBase().toPlainString()) + " DT";
        Label cPrix = cell(prixTxt);
        cPrix.setStyle(CELL_BASE + "-fx-font-weight: bold;");

        String redTxt = (p.getReduction() == null ? "0" : p.getReduction().toPlainString()) + " %";
        Label cReduction = cell(redTxt);
        cReduction.setStyle(CELL_BASE + "-fx-text-fill: #16a34a; -fx-font-weight: bold;");

        Label cMax = cell(String.valueOf(p.getNbActivitesMax()));

        // ─── Badge statut ───
        String stTxt = p.getStatutPack() == null ? "" : p.getStatutPack().name();
        boolean actif = stTxt.toUpperCase().contains("ACTIF");
        Label cStatut = new Label(stTxt);
        if (actif) {
            cStatut.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; "
                    + "-fx-background-radius: 20; -fx-padding: 3 10 3 10; "
                    + "-fx-font-size: 11; -fx-font-weight: bold;");
        } else {
            cStatut.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; "
                    + "-fx-background-radius: 20; -fx-padding: 3 10 3 10; "
                    + "-fx-font-size: 11;");
        }

        // ─── Boutons action ───
        Button btnEdit = new Button("✎ Modifier");
        btnEdit.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-padding: 4 10 4 10; "
                + "-fx-cursor: hand;");
        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: white; "
                        + "-fx-background-radius: 6; -fx-font-size: 11; -fx-padding: 4 10 4 10; "
                        + "-fx-cursor: hand;"));
        btnEdit.setOnMouseExited(e -> btnEdit.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; "
                        + "-fx-background-radius: 6; -fx-font-size: 11; -fx-padding: 4 10 4 10; "
                        + "-fx-cursor: hand;"));

        Button btnDel = new Button("🗑");
        btnDel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-padding: 4 8 4 8; "
                + "-fx-cursor: hand;");
        btnDel.setOnMouseEntered(e -> btnDel.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; "
                        + "-fx-background-radius: 6; -fx-font-size: 11; -fx-padding: 4 8 4 8; "
                        + "-fx-cursor: hand;"));
        btnDel.setOnMouseExited(e -> btnDel.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; "
                        + "-fx-background-radius: 6; -fx-font-size: 11; -fx-padding: 4 8 4 8; "
                        + "-fx-cursor: hand;"));

        HBox actions = new HBox(6, btnEdit, btnDel);
        actions.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 6 0 6 8;");

        // ─── Ligne GridPane ───
        GridPane row = new GridPane();
        String baseStyle = (index % 2 == 0 ? BG_EVEN : BG_ODD) + ROW_BORDER;
        row.setStyle(baseStyle);

        row.getColumnConstraints().addAll(
                cc(W_NOM), cc(W_TYPE), cc(W_PRIX), cc(W_REDUCTION),
                cc(W_MAX), cc(W_STATUT), cc(W_ACTIONS)
        );

        row.add(cNom,       0, 0);
        row.add(cType,      1, 0);
        row.add(cPrix,      2, 0);
        row.add(cReduction, 3, 0);
        row.add(cMax,       4, 0);
        row.add(cStatut,    5, 0);
        row.add(actions,    6, 0);

        // ─── Hover ───
        row.setOnMouseEntered(e -> {
            if (row != selectedRow)
                row.setStyle(BG_HOVER + ROW_BORDER);
        });
        row.setOnMouseExited(e -> {
            if (row != selectedRow)
                row.setStyle(baseStyle);
        });

        // ─── Clic / double-clic ───
        row.setOnMouseClicked(e -> {
            selectRow(row, p, baseStyle);
            if (e.getClickCount() == 2) onEdit();
        });

        btnEdit.setOnAction(e -> { selectRow(row, p, baseStyle); onEdit(); });
        btnDel.setOnAction(e  -> { selectRow(row, p, baseStyle); onDelete(); });

        return row;
    }

    private ColumnConstraints cc(double w) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPrefWidth(w);
        c.setMinWidth(w);
        c.setMaxWidth(w * 2);
        return c;
    }

    /** Label de cellule avec style inline (texte sombre sur fond transparent) */
    private Label cell(String text) {
        Label l = new Label(text);
        l.setStyle(CELL_BASE);
        return l;
    }

    // ═══════════════════════════════════════════════════════
    //  SÉLECTION
    // ═══════════════════════════════════════════════════════

    private void selectRow(GridPane row, Pack p, String baseStyle) {
        // Restaurer l'ancienne ligne sélectionnée
        if (selectedRow != null)
            selectedRow.setStyle(selectedRowBaseStyle);

        selectedRow          = row;
        selectedPack         = p;
        selectedRowBaseStyle = baseStyle;

        row.setStyle(BG_SELECTED);

        ScaleTransition s1 = new ScaleTransition(Duration.millis(110), row);
        s1.setToX(1.013); s1.setToY(1.013);
        s1.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition s2 = new ScaleTransition(Duration.millis(130), row);
        s2.setToX(1.0); s2.setToY(1.0);
        s2.setInterpolator(Interpolator.EASE_BOTH);
        new SequentialTransition(s1, s2).play();
    }

    private void clearSelection() {
        if (selectedRow != null)
            selectedRow.setStyle(selectedRowBaseStyle);
        selectedPack         = null;
        selectedRow          = null;
        selectedRowBaseStyle = "";
    }

    // ═══════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════

    private void playRowAppear(GridPane row) {
        FadeTransition ft = new FadeTransition(Duration.millis(160), row);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(160), row);
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

    // ═══════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════

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
        } catch (Exception e) { showError(e); }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText("Une erreur est survenue");
        a.setContentText(e.getMessage()); a.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════

    @FXML private void onDashboard()    { navigateSafe("/fxml/Dashboard.fxml"); }
    @FXML private void onPacks()        { refresh(); }
    @FXML private void onInscriptions() { navigateSafe("/fxml/InscriptionList.fxml"); }
    @FXML private void onRetour()       { navigateSafe("/Menu.fxml"); }

    private void navigateSafe(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) lblTotal.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { showError(e); }
    }
}