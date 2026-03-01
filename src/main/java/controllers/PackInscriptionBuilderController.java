package controllers;

import Entities.Activite;
import Entities.Pack;
import GUI.utils.SceneUtils;
import Services.PackInscriptionMetierService;
import Services.PackInscriptionMetierService.PriceBreakdown;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class PackInscriptionBuilderController {

    public void handleBack(javafx.event.ActionEvent actionEvent) {
    }

    // ===== Flow state =====
    private enum BookingStatus { DRAFT, HELD, EN_ATTENTE_PAIEMENT, CONFIRME, EXPIRE }

    private BookingStatus bookingStatus = null;
    private Integer lastDraftId = null;
    private PriceBreakdown lastPb = null;
    private String lastPaymentLink = null;

    private LocalDateTime expiryAt = null;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> expiryTask;

    private final PackInscriptionMetierService service = new PackInscriptionMetierService();

    // ===== Debounce recalc =====
    private final PauseTransition debounce = new PauseTransition(Duration.millis(220));

    // ===== Activity rows =====
    private final List<ActiviteRow> allRows = new ArrayList<>();

    // ===== FXML =====
    @FXML private Label lblStatus;

    // Tab 1: Client
    @FXML private Spinner<Integer> spUserId;
    @FXML private Label lblClientSummary;
    @FXML private Label lblRisk;
    @FXML private Button btnLoadClient;
    @FXML private Button btnGoBuild;

    // Tab 2: Build
    @FXML private ComboBox<Pack> cbPack;
    @FXML private Spinner<Integer> spNbPersonnes;
    @FXML private TextField txtCoupon;

    @FXML private TextField txtSearch;
    @FXML private Button btnSelectMax;

    @FXML private VBox vbRows;
    @FXML private Label lblSelected;

    @FXML private Label lblMaxAct;

    @FXML private Button btnCalculer;
    @FXML private Button btnRiskGate;
    @FXML private Button btnGoDevis;

    // Tab 3: Devis
    @FXML private Label lblPackBase;
    @FXML private Label lblActTotal;
    @FXML private Label lblDiscountPack;
    @FXML private Label lblDiscountGroupe;
    @FXML private Label lblDiscountCoupon;
    @FXML private Label lblTotal;
    @FXML private Label lblDetails;

    @FXML private Label lblExpiry;
    @FXML private Button btnCreateDraft;
    @FXML private Button btnDevisPdf;
    @FXML private Button btnGoPayment;

    // Tab 4: Payment
    @FXML private TextField txtPaymentLink;
    @FXML private Button btnOpenPayment;
    @FXML private Button btnSendEmail;
    @FXML private Button btnConfirm;

    @FXML
    private void initialize() {

        setStatus("Prêt", "ok");

        // ---- spinners
        if (spUserId != null) {
            spUserId.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999999, 1));
            spUserId.setEditable(true);
        }
        if (spNbPersonnes != null) {
            spNbPersonnes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 1));
            spNbPersonnes.setEditable(true);
        }

        // ---- load packs
        try {
            if (cbPack != null) cbPack.getItems().setAll(service.getActivePacks());
        } catch (Exception e) {
            showError("Erreur DB", "Impossible de charger les packs.\n" + e.getMessage());
        }

        // ---- events: client tab
        if (btnLoadClient != null) btnLoadClient.setOnAction(e -> loadClientContext());
        if (btnGoBuild != null) btnGoBuild.setOnAction(e -> {
            // navigation between tabs is UI-level (TabPane), here we only validate
            if (spUserId == null) return;
            setStatus("Client OK → Build", "ok");
        });

        // ---- pack change => load activities
        if (cbPack != null) {
            cbPack.valueProperty().addListener((obs, oldV, newV) -> {
                resetBookingContext();
                clearPriceLabels();
                if (newV == null) {
                    clearRows();
                    lblMaxAct.setText("Max activités : --");
                    return;
                }
                loadActivitesForPack(newV);
                setStatus("Pack chargé", "ok");
            });
        }

        // ---- recalc triggers
        Runnable triggerRecalc = this::safeRecalcDebounced;
        if (spNbPersonnes != null) spNbPersonnes.valueProperty().addListener((o, a, b) -> triggerRecalc.run());
        if (txtCoupon != null) txtCoupon.textProperty().addListener((o, a, b) -> triggerRecalc.run());

        if (txtSearch != null) txtSearch.textProperty().addListener((o, a, b) -> applyFilter());

        if (btnSelectMax != null) btnSelectMax.setOnAction(e -> selectMaxAllowed());
        if (btnCalculer != null) btnCalculer.setOnAction(e -> safeRecalc());

        if (btnRiskGate != null) btnRiskGate.setOnAction(e -> runRiskGate(true));

        if (btnGoDevis != null) btnGoDevis.setOnAction(e -> {
            // silent gate (no popup unless blocked)
            boolean ok = runRiskGate(false);
            if (ok) setStatus("OK → Devis", "ok");
        });

        // ---- devis actions
        if (btnCreateDraft != null) btnCreateDraft.setOnAction(e -> createDraftAndHold());
        if (btnDevisPdf != null) btnDevisPdf.setOnAction(e -> onGenerateDevisPdf());
        if (btnGoPayment != null) btnGoPayment.setOnAction(e -> {
            if (lastDraftId == null) {
                showWarning("Draft requis", "Crée draft + HOLD avant paiement.");
                return;
            }
            setStatus("OK → Paiement", "ok");
        });

        // ---- payment actions
        if (btnOpenPayment != null) btnOpenPayment.setOnAction(e -> onOpenPayment());
        if (btnSendEmail != null) btnSendEmail.setOnAction(e -> onSendEmail());
        if (btnConfirm != null) btnConfirm.setOnAction(e -> confirmBooking());

        // ---- debounce
        debounce.setOnFinished(e -> safeRecalc());

        // ---- initial UI
        updateSelectedCount();
        clearPriceLabels();
        refreshPaymentLinkUI();
        refreshExpiryUI();
    }

    // =======================
    // 1) Client context
    // =======================
    private void loadClientContext() {
        int userId = safeInt(spUserId, 1);

        // TODO: brancher DB: historique, segment, etc.
        String summary =
                "Client #" + userId + "\n" +
                        "- Historique: (à brancher DB)\n" +
                        "- Segment: NEW\n" +
                        "- Notes: —";

        if (lblClientSummary != null) lblClientSummary.setText(summary);

        // risk preview resets
        if (lblRisk != null) lblRisk.setText("Risk: — (calcule après pricing)");

        setStatus("Client chargé", "ok");
    }

    // =======================
    // 2) Pack + Activities
    // =======================
    private void loadActivitesForPack(Pack pack) {
        try {
            List<Activite> list = service.getActivitesByPack(pack.getIdPack());
            clearRows();

            for (Activite a : list) {
                ActiviteRow row = new ActiviteRow(a);

                row.chk.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal && countSelected() > pack.getNbActivitesMax()) {
                        row.chk.setSelected(false);
                        showWarning("Limite atteinte", "Max activités pour ce pack = " + pack.getNbActivitesMax());
                        return;
                    }
                    updateSelectedCount();
                    safeRecalcDebounced();
                });

                allRows.add(row);
            }

            if (lblMaxAct != null) lblMaxAct.setText("Max activités : " + pack.getNbActivitesMax());

            applyFilter();
            updateSelectedCount();

        } catch (Exception e) {
            showError("Erreur DB", "Impossible de charger les activités.\n" + e.getMessage());
        }
    }

    private void applyFilter() {
        if (vbRows == null) return;

        String q = (txtSearch == null || txtSearch.getText() == null) ? "" : txtSearch.getText().trim().toLowerCase();

        vbRows.getChildren().clear();
        for (ActiviteRow r : allRows) {
            if (q.isEmpty() || r.matches(q)) {
                vbRows.getChildren().add(r.root);
            }
        }
    }

    private void selectMaxAllowed() {
        Pack pack = cbPack == null ? null : cbPack.getValue();
        if (pack == null) return;

        int max = pack.getNbActivitesMax();

        // unselect all
        allRows.forEach(r -> r.chk.setSelected(false));

        // select first visible max
        List<ActiviteRow> visible = getVisibleRows();
        for (int i = 0; i < visible.size() && i < max; i++) {
            visible.get(i).chk.setSelected(true);
        }

        updateSelectedCount();
        safeRecalcDebounced();
    }

    private List<ActiviteRow> getVisibleRows() {
        if (vbRows == null) return Collections.emptyList();
        Set<HBox> visibleRoots = vbRows.getChildren().stream()
                .filter(n -> n instanceof HBox)
                .map(n -> (HBox) n)
                .collect(Collectors.toSet());

        return allRows.stream()
                .filter(r -> visibleRoots.contains(r.root))
                .collect(Collectors.toList());
    }

    private void clearRows() {
        allRows.clear();
        if (vbRows != null) vbRows.getChildren().clear();
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        if (lblSelected != null) lblSelected.setText(countSelected() + " sélectionnée(s)");
    }

    private int countSelected() {
        int c = 0;
        for (ActiviteRow r : allRows) if (r.chk.isSelected()) c++;
        return c;
    }

    // =======================
    // 3) Pricing & Risk
    // =======================
    private void safeRecalcDebounced() {
        debounce.playFromStart();
    }

    private void safeRecalc() {
        try {
            recalc();
        } catch (Exception ex) {
            if (lblDetails != null) lblDetails.setText("⚠ " + ex.getMessage());
        }
    }

    private void recalc() throws Exception {
        Pack pack = cbPack == null ? null : cbPack.getValue();
        if (pack == null) return;

        int nb = safeInt(spNbPersonnes, 1);
        String coupon = (txtCoupon == null ? null : txtCoupon.getText());

        List<Integer> selectedActIds = allRows.stream()
                .filter(r -> r.chk.isSelected())
                .map(r -> r.activite.getIdActivite())
                .collect(Collectors.toList());

        PriceBreakdown pb = service.computePrice(pack.getIdPack(), selectedActIds, nb, coupon);
        lastPb = pb;

        if (lblPackBase != null) lblPackBase.setText(format(pb.packBase()));
        if (lblActTotal != null) lblActTotal.setText(format(pb.activitesTotal()));
        if (lblDiscountPack != null) lblDiscountPack.setText("- " + format(pb.discountPack()));
        if (lblDiscountGroupe != null) lblDiscountGroupe.setText("- " + format(pb.discountGroupe()));
        if (lblDiscountCoupon != null) lblDiscountCoupon.setText("- " + format(pb.discountCoupon()));
        if (lblTotal != null) lblTotal.setText(format(pb.total()));
        if (lblDetails != null) lblDetails.setText(pb.details());

        // Update risk label
        updateRiskLabel(pb, nb, selectedActIds.size(), coupon);
    }

    private boolean runRiskGate(boolean showPopup) {
        safeRecalc(); // ensure lastPb is updated

        int nb = safeInt(spNbPersonnes, 1);
        int acts = countSelected();
        String coupon = txtCoupon == null ? "" : txtCoupon.getText();
        BigDecimal total = (lastPb == null ? BigDecimal.ZERO : safeBd(lastPb.total()));

        int score = computeRiskScore(total, nb, acts, coupon, lastPb);

        String decision;
        if (score >= 70) decision = "HIGH → Deposit/Review required";
        else if (score >= 40) decision = "MEDIUM → Warning";
        else decision = "LOW → OK";

        if (lblRisk != null) lblRisk.setText("Risk: " + score + "/100 — " + decision);

        // silent mode: block only if extremely high
        if (!showPopup) {
            if (score >= 85) {
                setStatus("Bloqué (risk trop élevé)", "danger");
                showWarning("Risk Gate", "Score: " + score + "/100\n" + decision + "\n\nBlocage admin: nécessite review.");
                return false;
            }
            return true;
        }

        // popup mode
        if (score >= 70) {
            return confirm("Risk Gate", "Score: " + score + "/100\n" + decision + "\n\nContinuer quand même ?");
        } else {
            showInfo("Risk Gate", "Score: " + score + "/100\n" + decision);
            return true;
        }
    }

    private void updateRiskLabel(PriceBreakdown pb, int nb, int acts, String coupon) {
        BigDecimal total = pb == null ? BigDecimal.ZERO : safeBd(pb.total());
        int score = computeRiskScore(total, nb, acts, coupon, pb);

        String decision;
        if (score >= 70) decision = "HIGH";
        else if (score >= 40) decision = "MEDIUM";
        else decision = "LOW";

        if (lblRisk != null) lblRisk.setText("Risk: " + score + "/100 — " + decision);

        if (score >= 70) setStatus("Risque élevé (" + score + ")", "danger");
        else if (score >= 40) setStatus("Risque moyen (" + score + ")", "warn");
        else setStatus("Risque faible (" + score + ")", "ok");
    }

    private int computeRiskScore(BigDecimal total, int nbPersons, int nbActs, String coupon, PriceBreakdown pb) {
        int score = 0;

        if (total.compareTo(new BigDecimal("800")) >= 0) score += 40;
        else if (total.compareTo(new BigDecimal("400")) >= 0) score += 20;

        if (nbPersons >= 20) score += 25;
        else if (nbPersons >= 10) score += 14;

        if (nbActs >= 5) score += 16;
        else if (nbActs >= 3) score += 9;

        if (coupon != null && !coupon.trim().isEmpty()) score += 8;

        // discount anomaly
        if (pb != null) {
            BigDecimal disc = safeBd(pb.discountCoupon())
                    .add(safeBd(pb.discountGroupe()))
                    .add(safeBd(pb.discountPack()));
            if (disc.compareTo(new BigDecimal("200")) >= 0) score += 15;
        }

        return Math.min(100, score);
    }

    // =======================
    // 4) Draft + HOLD + Expiry
    // =======================
    private void createDraftAndHold() {
        Pack pack = cbPack == null ? null : cbPack.getValue();
        if (pack == null) {
            showWarning("Pack requis", "Choisis un pack.");
            return;
        }

        int userId = safeInt(spUserId, 1);
        int nb = safeInt(spNbPersonnes, 1);
        String coupon = txtCoupon == null ? "" : txtCoupon.getText();

        List<Integer> actIds = allRows.stream()
                .filter(r -> r.chk.isSelected())
                .map(r -> r.activite.getIdActivite())
                .collect(Collectors.toList());

        if (actIds.isEmpty()) {
            showWarning("Activité requise", "Sélectionne au moins une activité.");
            return;
        }
        if (actIds.size() > pack.getNbActivitesMax()) {
            showWarning("Limite atteinte", "Max activités = " + pack.getNbActivitesMax());
            return;
        }

        // Risk gate
        if (!runRiskGate(true)) return;

        try {
            int draftId = service.createInscriptionDraft(userId, pack.getIdPack(), actIds, nb, coupon);

            lastDraftId = draftId;
            bookingStatus = BookingStatus.HELD;

            expiryAt = LocalDateTime.now().plusMinutes(30);
            refreshExpiryUI();

            // Payment link (mock)
            lastPaymentLink = "https://pay.example/checkout?draftId=" + draftId;
            refreshPaymentLinkUI();

            scheduleExpiry();

            setStatus("HELD (#" + draftId + ")", "warn");
            showInfo("Draft + HOLD", "Draft #" + draftId + " créé.\nHOLD jusqu'à: " + expiryAt);

        } catch (Exception e) {
            showError("Erreur Draft", e.getMessage());
        }
    }

    private void scheduleExpiry() {
        if (expiryAt == null) return;

        try {
            if (scheduler == null) scheduler = Executors.newSingleThreadScheduledExecutor();

            if (expiryTask != null) expiryTask.cancel(true);

            long seconds = java.time.Duration.between(LocalDateTime.now(), expiryAt).getSeconds();
            if (seconds < 1) seconds = 1;

            expiryTask = scheduler.schedule(() -> Platform.runLater(() -> {
                if (bookingStatus == BookingStatus.HELD) {
                    bookingStatus = BookingStatus.EXPIRE;
                    setStatus("EXPIRE", "danger");
                    showWarning("Expired", "Le HOLD a expiré. Recrée un draft si لازم.");
                }
            }), seconds, TimeUnit.SECONDS);

        } catch (Exception ignored) { }
    }

    private void refreshExpiryUI() {
        if (lblExpiry != null) lblExpiry.setText(expiryAt == null ? "—" : expiryAt.toString());
    }

    private void refreshPaymentLinkUI() {
        if (txtPaymentLink != null) txtPaymentLink.setText(lastPaymentLink == null ? "" : lastPaymentLink);
    }

    private void resetBookingContext() {
        lastDraftId = null;
        bookingStatus = null;
        lastPaymentLink = null;
        expiryAt = null;
        refreshPaymentLinkUI();
        refreshExpiryUI();

        if (expiryTask != null) {
            try { expiryTask.cancel(true); } catch (Exception ignored) {}
        }
    }

    // =======================
    // 5) Payment actions
    // =======================
    private void onOpenPayment() {
        if (lastDraftId == null) {
            showWarning("Draft requis", "Crée draft + HOLD avant paiement.");
            return;
        }
        if (bookingStatus == BookingStatus.EXPIRE) {
            showWarning("Expired", "Draft expired. Recrée un draft.");
            return;
        }

        if (lastPaymentLink == null || lastPaymentLink.isBlank()) {
            lastPaymentLink = "https://pay.example/checkout?draftId=" + lastDraftId;
            refreshPaymentLinkUI();
        }

        try {
            // Desktop browse
            Class<?> desktopCls = Class.forName("java.awt.Desktop");
            Object desktop = desktopCls.getMethod("getDesktop").invoke(null);
            desktopCls.getMethod("browse", URI.class).invoke(desktop, URI.create(lastPaymentLink));
            showInfo("Paiement", "Lien ouvert:\n" + lastPaymentLink);
        } catch (Exception ex) {
            copyToClipboard(lastPaymentLink);
            showInfo("Paiement (copié)", "Impossible d'ouvrir navigateur.\nLien copié:\n" + lastPaymentLink);
        }
    }

    private void onSendEmail() {
        if (lastDraftId == null) {
            showWarning("Draft requis", "Crée draft + HOLD avant email.");
            return;
        }

        String subject = "EcoAdventure — Devis / Paiement — Draft #" + lastDraftId;
        String body = buildDevisText() + "\n\nPaiement: " + (lastPaymentLink == null ? "" : lastPaymentLink);

        // Placeholder (JavaMail not wired)
        copyToClipboard("SUBJECT: " + subject + "\n\n" + body);
        showInfo("Email (fallback)", "JavaMail غير مربوط.\n✅ محتوى الإيميل تـcopy في Clipboard.");
    }

    private void confirmBooking() {
        if (lastDraftId == null) {
            showWarning("Draft requis", "Crée draft + HOLD avant confirmation.");
            return;
        }
        if (bookingStatus == BookingStatus.EXPIRE) {
            showWarning("Expired", "Draft expired. Recrée un draft.");
            return;
        }

        boolean ok = confirm("Confirmer", "Confirmer Draft #" + lastDraftId + " ?");
        if (!ok) return;

        // TODO: if available in service:
        // service.confirmInscription(lastDraftId);

        bookingStatus = BookingStatus.CONFIRME;
        setStatus("CONFIRME (#" + lastDraftId + ")", "ok");
        showInfo("Confirmé", "Booking confirmé ✅\n(ربط DB ينجم يكون في service.confirmInscription)");
    }

    // =======================
    // 6) Devis PDF + QR (safe)
    // =======================
    private void onGenerateDevisPdf() {
        if (lastDraftId == null) {
            showWarning("Draft requis", "Crée draft + HOLD قبل Devis PDF.");
            return;
        }
        if (lastPb == null) safeRecalc();

        try {
            File out = new File(System.getProperty("user.home"), "Devis_Draft_" + lastDraftId + ".pdf");
            String qrText = (lastPaymentLink != null ? lastPaymentLink : ("DRAFT:" + lastDraftId));

            BufferedImage qr = generateQrImage(qrText, 260, 260);

            boolean pdfOk = tryCreatePdfWithPdfBox(out, qr, buildDevisText());

            if (!pdfOk) {
                File png = new File(System.getProperty("user.home"), "QR_Draft_" + lastDraftId + ".png");
                ImageIO.write(qr, "png", png);
                copyToClipboard(buildDevisText() + "\n\nQR: " + qrText);

                showInfo("Fallback (sans PDFBox)",
                        "PDFBox غير موجود.\n✅ QR: " + png.getAbsolutePath() +
                                "\n✅ Devis copied to clipboard.");
                return;
            }

            showInfo("✅ Devis PDF", "PDF créé:\n" + out.getAbsolutePath() + "\nQR:\n" + qrText);

        } catch (Exception e) {
            showError("PDF/QR erreur", e.getMessage());
        }
    }

    private BufferedImage generateQrImage(String text, int w, int h) throws Exception {
        try {
            Class<?> writerCls = Class.forName("com.google.zxing.qrcode.QRCodeWriter");
            Object writer = writerCls.getDeclaredConstructor().newInstance();

            Class<?> barcodeFormatCls = Class.forName("com.google.zxing.BarcodeFormat");
            Object qrFormat = Enum.valueOf((Class<Enum>) barcodeFormatCls, "QR_CODE");

            Object matrix = writerCls.getMethod("encode", String.class, barcodeFormatCls, int.class, int.class)
                    .invoke(writer, text, qrFormat, w, h);

            Class<?> mtwCls = Class.forName("com.google.zxing.client.j2se.MatrixToImageWriter");
            Class<?> bitMatrixCls = Class.forName("com.google.zxing.common.BitMatrix");
            return (BufferedImage) mtwCls.getMethod("toBufferedImage", bitMatrixCls).invoke(null, matrix);

        } catch (Exception noZxing) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(java.awt.Color.BLACK);
            g.drawRect(0, 0, w - 1, h - 1);
            g.drawString("QR (ZXing missing)", 10, 20);
            g.drawString(text.length() > 30 ? text.substring(0, 30) + "..." : text, 10, 45);
            g.dispose();
            return img;
        }
    }

    private boolean tryCreatePdfWithPdfBox(File out, BufferedImage qrImg, String text) {
        try {
            Class<?> pdDocumentCls = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Object doc = pdDocumentCls.getDeclaredConstructor().newInstance();

            Class<?> pdPageCls = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
            Object page = pdPageCls.getDeclaredConstructor().newInstance();
            pdDocumentCls.getMethod("addPage", pdPageCls).invoke(doc, page);

            Class<?> pdPageContentStreamCls = Class.forName("org.apache.pdfbox.pdmodel.PDPageContentStream");
            Object cs = pdPageContentStreamCls
                    .getDeclaredConstructor(pdDocumentCls, pdPageCls)
                    .newInstance(doc, page);

            Class<?> pdType1FontCls = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
            Object font = pdType1FontCls.getField("HELVETICA").get(null);

            pdPageContentStreamCls.getMethod("beginText").invoke(cs);
            pdPageContentStreamCls.getMethod("setFont", Class.forName("org.apache.pdfbox.pdmodel.font.PDFont"), float.class)
                    .invoke(cs, font, 12f);
            pdPageContentStreamCls.getMethod("newLineAtOffset", float.class, float.class).invoke(cs, 50f, 750f);

            String[] lines = text.split("\n");
            for (String line : lines) {
                pdPageContentStreamCls.getMethod("showText", String.class).invoke(cs, line);
                pdPageContentStreamCls.getMethod("newLineAtOffset", float.class, float.class).invoke(cs, 0f, -16f);
            }
            pdPageContentStreamCls.getMethod("endText").invoke(cs);

            Class<?> losslessFactoryCls = Class.forName("org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory");
            Object pdImage = losslessFactoryCls.getMethod("createFromImage", pdDocumentCls, BufferedImage.class)
                    .invoke(null, doc, qrImg);

            pdPageContentStreamCls.getMethod("drawImage",
                            Class.forName("org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject"),
                            float.class, float.class, float.class, float.class)
                    .invoke(cs, pdImage, 50f, 420f, 180f, 180f);

            pdPageContentStreamCls.getMethod("close").invoke(cs);

            pdDocumentCls.getMethod("save", File.class).invoke(doc, out);
            pdDocumentCls.getMethod("close").invoke(doc);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // =======================
    // 7) Devis text
    // =======================
    private String buildDevisText() {
        Pack pack = cbPack == null ? null : cbPack.getValue();
        int userId = safeInt(spUserId, 1);
        int nb = safeInt(spNbPersonnes, 1);

        String packName = (pack == null ? "" : String.valueOf(pack));
        String acts = allRows.stream().filter(r -> r.chk.isSelected())
                .map(r -> "- " + safeStr(r.activite.getNom()))
                .collect(Collectors.joining("\n"));

        String total = lblTotal == null ? "" : lblTotal.getText();

        return "EcoAdventure — Devis (Admin)\n"
                + "Date: " + LocalDateTime.now() + "\n"
                + "Client(UserID): " + userId + "\n"
                + "Pack: " + packName + "\n"
                + "Nb personnes: " + nb + "\n"
                + "Coupon: " + safeStr(txtCoupon == null ? "" : txtCoupon.getText()) + "\n\n"
                + "Activités:\n" + (acts.isEmpty() ? "- (aucune)" : acts) + "\n\n"
                + "TOTAL: " + total + "\n"
                + "Draft ID: " + (lastDraftId == null ? "-" : lastDraftId) + "\n"
                + "Paiement: " + (lastPaymentLink == null ? "-" : lastPaymentLink);
    }

    // =======================
    // 8) UI + helpers
    // =======================
    private void clearPriceLabels() {
        if (lblPackBase != null) lblPackBase.setText("--");
        if (lblActTotal != null) lblActTotal.setText("--");
        if (lblDiscountPack != null) lblDiscountPack.setText("--");
        if (lblDiscountGroupe != null) lblDiscountGroupe.setText("--");
        if (lblDiscountCoupon != null) lblDiscountCoupon.setText("--");
        if (lblTotal != null) lblTotal.setText("--");
        if (lblDetails != null) lblDetails.setText("");
    }

    private String format(BigDecimal v) {
        if (v == null) return "0.00 DT";
        return v.toPlainString() + " DT";
    }

    private int safeInt(Spinner<Integer> sp, int def) {
        if (sp == null) return def;
        try {
            Integer v = sp.getValue();
            if (v != null) return v;
        } catch (Exception ignored) {}
        try {
            String t = sp.getEditor().getText();
            if (t != null && !t.isBlank()) return Integer.parseInt(t.trim());
        } catch (Exception ignored) {}
        return def;
    }

    private BigDecimal safeBd(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private String safeStr(String s) { return s == null ? "" : s; }

    private void copyToClipboard(String s) {
        try {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(s);
            Clipboard.getSystemClipboard().setContent(cc);
        } catch (Exception ignored) {}
    }

    private void setStatus(String text, String level) {
        if (lblStatus == null) return;
        lblStatus.setText(text);

        lblStatus.getStyleClass().removeAll("status-ok", "status-warn", "status-danger");
        if ("danger".equalsIgnoreCase(level)) lblStatus.getStyleClass().add("status-danger");
        else if ("warn".equalsIgnoreCase(level)) lblStatus.getStyleClass().add("status-warn");
        else lblStatus.getStyleClass().add("status-ok");
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showWarning(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        // stop expiry scheduler to avoid background threads
        try {
            if (expiryTask != null) expiryTask.cancel(true);
            if (scheduler != null) scheduler.shutdownNow();
        } catch (Exception ignored) {}

        SceneUtils.loadScene("/GUI/MainLayout.fxml", (Node) event.getSource());
    }

    // =======================
    // Table widget row
    // =======================
    private static class ActiviteRow {
        final Activite activite;
        final CheckBox chk = new CheckBox();
        final HBox root = new HBox(12);

        private final String searchable;

        ActiviteRow(Activite a) {
            this.activite = a;

            root.getStyleClass().add("tw-row");
            root.setAlignment(Pos.CENTER_LEFT);

            Label nom = new Label(nvl(a.getNom()));
            nom.getStyleClass().add("tw-cell");
            nom.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nom, Priority.ALWAYS);

            Label cat = new Label(a.getCategorieAct() == null ? "" : a.getCategorieAct().name());
            cat.getStyleClass().addAll("tw-cell", "tw-chip");

            Label niv = new Label(a.getNiveauAct() == null ? "" : a.getNiveauAct().name());
            niv.getStyleClass().addAll("tw-cell", "tw-chip");

            Label prix = new Label(a.getPrix() == null ? "0 DT" : a.getPrix().toPlainString() + " DT");
            prix.getStyleClass().addAll("tw-cell", "tw-price");

            chk.getStyleClass().add("tw-check");

            root.getChildren().addAll(chk, nom, cat, niv, prix);

            searchable = (nvl(a.getNom()) + " " +
                    (a.getCategorieAct() == null ? "" : a.getCategorieAct().name()) + " " +
                    (a.getNiveauAct() == null ? "" : a.getNiveauAct().name()))
                    .toLowerCase();
        }

        boolean matches(String q) { return searchable.contains(q); }

        private static String nvl(String s) { return s == null ? "" : s; }
    }
}