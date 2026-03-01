package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * QrPdfDialogController
 * - PRO button motion (hover glow + lift, click pop, shine sweep)
 * - Dialog open animation (fade + scale)
 * - QR loading from API (/api/qr?kind=...)
 * - Export PDF opens browser (/api/export/pdf?kind=...)
 */
public class QrPdfDialogController {

    @FXML private ImageView imgQr;
    @FXML private Label lblHint;

    // Buttons (as in the improved FXML)
    @FXML private Button btnExport, btnQrIns, btnQrPacks, btnQrBoth;

    // Shine overlays (Labels in StackPane)
    @FXML private Label shineExport, shineIns, shinePacks, shineBoth;

    private final String API_BASE = System.getenv().getOrDefault("EA_QR_API_BASE", "http://localhost:8086");
    private final HttpClient http = HttpClient.newHttpClient();

    private String currentKind = "both";
    private String currentPdfUrl = API_BASE + "/api/export/pdf?kind=" + currentKind;

    @FXML
    private void initialize() {
        // ✅ dialog open animation after scene is ready
        Platform.runLater(() -> {
            if (btnExport != null && btnExport.getScene() != null) {
                animateDialogOpen(btnExport.getScene().getRoot());
            }
        });

        // ✅ pro button animations
        bindProButtonMotion(btnExport,  shineExport, true);
        bindProButtonMotion(btnQrIns,   shineIns,   false);
        bindProButtonMotion(btnQrPacks, shinePacks, false);
        bindProButtonMotion(btnQrBoth,  shineBoth,  false);

        // default QR
        loadQr("both");
    }

    /* =========================
       Actions
       ========================= */

    @FXML
    private void onExportPdf() {
        try {
            Desktop.getDesktop().browse(URI.create(currentPdfUrl));
        } catch (Exception e) {
            showHint("Impossible d’ouvrir le lien PDF: " + e.getMessage(), true);
        }
    }

    @FXML
    private void onQrInscriptions() { loadQr("inscriptions"); }

    @FXML
    private void onQrPacks() { loadQr("packs"); }

    @FXML
    private void onQrBoth() { loadQr("both"); }

    /* =========================
       QR loading
       ========================= */

    private void loadQr(String kind) {
        this.currentKind = kind;
        this.currentPdfUrl = API_BASE + "/api/export/pdf?kind=" + kind;

        showHint("Génération QR (" + kind + ")...", false);

        String qrUrl = API_BASE + "/api/qr?kind=" + kind;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(qrUrl))
                .GET()
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(res -> {
                    if (res.statusCode() != 200) {
                        Platform.runLater(() -> showHint("QR API error: HTTP " + res.statusCode(), true));
                        return;
                    }

                    Image img = new Image(new ByteArrayInputStream(res.body()));

                    Platform.runLater(() -> {
                        imgQr.setImage(img);
                        // small pop when QR updates
                        popNode(imgQr);

                        showHint("✅ Scan QR (" + kind + ") → PDF download", false);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showHint(
                            "Impossible de contacter l’API QR.\n" +
                                    "Vérifie que le serveur tourne sur: " + API_BASE + "\n" +
                                    ex.getMessage(),
                            true
                    ));
                    return null;
                });
    }

    /* =========================
       PRO Animations
       ========================= */

    private void bindProButtonMotion(Button btn, Label shine, boolean primary) {
        if (btn == null) return;

        // base shadow
        DropShadow base = new DropShadow();
        base.setRadius(primary ? 18 : 12);
        base.setSpread(0.12);
        base.setOffsetY(primary ? 6 : 4);
        base.setColor(Color.rgb(0, 0, 0, 0.35));
        btn.setEffect(base);

        // Hover in
        btn.setOnMouseEntered(e -> {
            // lift + scale
            ScaleTransition st = new ScaleTransition(Duration.millis(140), btn);
            st.setToX(1.03);
            st.setToY(1.03);

            TranslateTransition tt = new TranslateTransition(Duration.millis(140), btn);
            tt.setToY(-1.6);

            // glow
            DropShadow glow = new DropShadow();
            glow.setRadius(primary ? 22 : 16);
            glow.setSpread(0.22);
            glow.setOffsetY(primary ? 8 : 6);
            glow.setColor(primary
                    ? Color.rgb(95, 233, 140, 0.28)   // green glow
                    : Color.rgb(56, 189, 248, 0.20)); // cyan glow
            btn.setEffect(glow);

            new ParallelTransition(st, tt).play();

            // shine sweep
            if (shine != null) {
                playShine(shine);
            }
        });

        // Hover out
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), btn);
            st.setToX(1.0);
            st.setToY(1.0);

            TranslateTransition tt = new TranslateTransition(Duration.millis(160), btn);
            tt.setToY(0);

            new ParallelTransition(st, tt).play();
            btn.setEffect(base);

            if (shine != null) shine.setOpacity(0);
        });

        // Pressed pop
        btn.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), btn);
            st.setToX(0.985);
            st.setToY(0.985);
            st.play();
        });

        btn.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(90), btn);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });
    }

    private void playShine(Label shine) {
        shine.setOpacity(0);
        shine.setTranslateX(-40);

        FadeTransition ftIn = new FadeTransition(Duration.millis(120), shine);
        ftIn.setToValue(1.0);

        TranslateTransition sweep = new TranslateTransition(Duration.millis(520), shine);
        sweep.setToX(40);

        FadeTransition ftOut = new FadeTransition(Duration.millis(160), shine);
        ftOut.setToValue(0.0);

        SequentialTransition seq = new SequentialTransition(ftIn, sweep, ftOut);
        seq.play();
    }

    private void popNode(Node node) {
        if (node == null) return;

        ScaleTransition in = new ScaleTransition(Duration.millis(120), node);
        in.setFromX(0.98);
        in.setFromY(0.98);
        in.setToX(1.02);
        in.setToY(1.02);

        ScaleTransition out = new ScaleTransition(Duration.millis(120), node);
        out.setToX(1.0);
        out.setToY(1.0);

        new SequentialTransition(in, out).play();
    }

    private void animateDialogOpen(Node root) {
        if (root == null) return;

        root.setOpacity(0);
        root.setScaleX(0.98);
        root.setScaleY(0.98);

        FadeTransition ft = new FadeTransition(Duration.millis(160), root);
        ft.setToValue(1.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(180), root);
        st.setToX(1.0);
        st.setToY(1.0);

        new ParallelTransition(ft, st).play();
    }

    /* =========================
       Hint helper
       ========================= */

    private void showHint(String msg, boolean isError) {
        if (lblHint == null) return;
        lblHint.setText(msg);

        // soft attention animation
        FadeTransition ft = new FadeTransition(Duration.millis(140), lblHint);
        ft.setFromValue(0.55);
        ft.setToValue(1.0);
        ft.play();

        // if error, small red shadow (without touching CSS)
        if (isError) {
            DropShadow ds = new DropShadow();
            ds.setRadius(14);
            ds.setSpread(0.18);
            ds.setColor(Color.rgb(255, 79, 79, 0.28));
            lblHint.setEffect(ds);

            PauseTransition p = new PauseTransition(Duration.seconds(2));
            p.setOnFinished(e -> lblHint.setEffect(null));
            p.play();
        } else {
            lblHint.setEffect(null);
        }
    }
}