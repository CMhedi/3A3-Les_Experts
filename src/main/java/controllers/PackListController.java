package controllers;

import Entities.Pack;
import Services.PackService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PackListController {

    // ✅ TableWidget nodes
    @FXML private VBox rowsBox;

    @FXML private TextField txtSearch;
    @FXML private Label lblTotal;

    // ✅ (OPTIONNEL) QR/PDF nodes (ajoute-les dans FXML si tu veux l’affichage)
    @FXML private ImageView imgQr;
    @FXML private Label lblQrHint;

    private final PackService service = new PackService();
    private final ObservableList<Pack> master = FXCollections.observableArrayList();

    // selection (remplace TableView selection)
    private Pack selectedPack = null;
    private HBox selectedRow = null;

    /* =========================================================
       QR/PDF API CONFIG
       =========================================================
       - EA_QR_API_BASE : base URL pour appeler ton QrPdfApiServer depuis PC
         ex: http://localhost:8086
       - IMPORTANT: dans QrPdfApiServer, utilise EA_PUBLIC_BASE_URL = http://IP_PC:8086
         pour que le téléphone puisse télécharger le PDF après scan.
       ========================================================= */
    private final String API_BASE = System.getenv().getOrDefault("EA_QR_API_BASE", "http://localhost:8086");
    private final HttpClient http = HttpClient.newHttpClient();
    private String currentKind = "both";
    private String currentPdfUrl = API_BASE + "/api/export/pdf?kind=" + currentKind;

    @FXML
    private void initialize() {
        txtSearch.textProperty().addListener((obs, o, n) -> render());
        refresh();

        // ✅ si QR bloc موجود في FXML، نولّد QR افتراضياً
        if (imgQr != null) {
            loadQr("both");
        }
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

        // hover
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

        // click + double click
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

    /* =========================================================
       QR/PDF ACTIONS (bind in FXML)
       ========================================================= */
    @FXML
    private void onQrPacks() {
        loadQr("packs");
    }

    @FXML
    private void onQrInscriptions() {
        loadQr("inscriptions");
    }

    @FXML
    private void onQrBoth() {
        loadQr("both");
    }

    @FXML
    private void onOpenPdfLink() {
        try {
            Desktop.getDesktop().browse(URI.create(currentPdfUrl));
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void onCopyPdfLink() {
        ClipboardContent cc = new ClipboardContent();
        cc.putString(currentPdfUrl);
        Clipboard.getSystemClipboard().setContent(cc);

        if (lblQrHint != null) {
            lblQrHint.setText("✅ Lien copié: " + currentPdfUrl);
        }
    }

    private void loadQr(String kind) {
        this.currentKind = kind;
        this.currentPdfUrl = API_BASE + "/api/export/pdf?kind=" + kind;

        if (lblQrHint != null) {
            lblQrHint.setText("Génération du QR (" + kind + ") ...");
        }

        // si imgQr مش موجودة في FXML، ما نعمل شيء
        if (imgQr == null) return;

        String qrUrl = API_BASE + "/api/qr?kind=" + kind;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(qrUrl))
                .GET()
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(res -> {
                    if (res.statusCode() != 200) {
                        Platform.runLater(() -> showInfo("QR/PDF", "QR API error: HTTP " + res.statusCode()));
                        return;
                    }

                    Image img = new Image(new ByteArrayInputStream(res.body()));
                    Platform.runLater(() -> {
                        imgQr.setImage(img);
                        if (lblQrHint != null) {
                            lblQrHint.setText("✅ Scan QR avec ton téléphone → PDF download (" + kind + ")");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showInfo(
                            "QR/PDF",
                            "Impossible de contacter l’API QR.\n" +
                                    "Vérifie que QrPdfApiServer tourne sur: " + API_BASE + "\n\n" +
                                    ex.getMessage()
                    ));
                    return null;
                });
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