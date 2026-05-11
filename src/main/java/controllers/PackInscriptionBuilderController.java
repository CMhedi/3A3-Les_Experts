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
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.google.gson.Gson;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PackInscriptionBuilderController {

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

    // ===== Payment PRO state =====
    private boolean paymentApproved = false;
    private String paymentRef = null;
    private final ToggleGroup payGroup = new ToggleGroup();

    // ===== Regex =====
    private static final Pattern EXPIRY_RGX = Pattern.compile("(0[1-9]|1[0-2])/[0-9]{2}");

    // ===== OCR API =====
    private static final String OCR_API_URL = "http://localhost:8088/api/ocr/card";
    private static final String TESSDATA_PATH = "C:\\Program Files\\Tesseract-OCR\\tessdata";

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

    // Tab 4: Payment (old ids)
    @FXML private TextField txtPaymentLink;
    @FXML private Button btnOpenPayment;
    @FXML private Button btnSendEmail;
    @FXML private Button btnConfirm;

    // Tab 4: Payment PRO
    @FXML private Label lblPayBadge;
    @FXML private RadioButton rbCard;
    @FXML private RadioButton rbPaypal;
    @FXML private RadioButton rbLink;

    @FXML private VBox boxCardForm;
    @FXML private VBox boxLink;

    @FXML private TextField txtCardName;
    @FXML private TextField txtCardNumber;
    @FXML private TextField txtCardExpiry;
    @FXML private PasswordField txtCardCvv;

    @FXML private Button btnPayNow;
    @FXML private Button btnFake3ds;
    @FXML private ProgressIndicator piPay;
    @FXML private Label lblPayStatus;

    @FXML private TextArea txtReceipt;
    @FXML private Button btnCopyReceipt;

    // OCR button + hint
    @FXML private Button btnScanOcr;
    @FXML private Label lblCardHint;

    // (Optional) test presets – تنجم تحيّدهم من FXML لو ما تحبهمش
    @FXML private Button btnFillVisaGold;
    @FXML private Button btnFillMaster;
    @FXML private Button btnFillAmex;
    @FXML private Button btnClearCard;

    @FXML
    private void initialize() {

        setStatus("Prêt", "ok");

        // spinners
        if (spUserId != null) {
            spUserId.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999999, 1));
            spUserId.setEditable(true);
        }
        if (spNbPersonnes != null) {
            spNbPersonnes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 1));
            spNbPersonnes.setEditable(true);
        }

        // load packs
        try {
            if (cbPack != null) cbPack.getItems().setAll(service.getActivePacks());
        } catch (Exception e) {
            showError("Erreur DB", "Impossible de charger les packs.\n" + e.getMessage());
        }

        // client actions
        if (btnLoadClient != null) btnLoadClient.setOnAction(e -> loadClientContext());
        if (btnGoBuild != null) btnGoBuild.setOnAction(e -> setStatus("Client OK → Build", "ok"));

        // pack change => load activities
        if (cbPack != null) {
            cbPack.valueProperty().addListener((obs, oldV, newV) -> {
                resetBookingContext();
                clearPriceLabels();
                clearRows();
                clearPaymentContext();

                if (newV == null) {
                    if (lblMaxAct != null) lblMaxAct.setText("Max activités : --");
                    return;
                }
                loadActivitesForPack(newV);
                setStatus("Pack chargé", "ok");
            });
        }

        // recalc triggers
        Runnable triggerRecalc = this::safeRecalcDebounced;
        if (spNbPersonnes != null) spNbPersonnes.valueProperty().addListener((o, a, b) -> triggerRecalc.run());
        if (txtCoupon != null) txtCoupon.textProperty().addListener((o, a, b) -> triggerRecalc.run());
        if (txtSearch != null) txtSearch.textProperty().addListener((o, a, b) -> applyFilter());

        if (btnSelectMax != null) btnSelectMax.setOnAction(e -> selectMaxAllowed());
        if (btnCalculer != null) btnCalculer.setOnAction(e -> safeRecalc());
        if (btnRiskGate != null) btnRiskGate.setOnAction(e -> runRiskGate(true));

        if (btnGoDevis != null) btnGoDevis.setOnAction(e -> {
            boolean ok = runRiskGate(false);
            if (ok) setStatus("OK → Devis", "ok");
        });

        // devis actions
        if (btnCreateDraft != null) btnCreateDraft.setOnAction(e -> createDraftAndHold());
        if (btnDevisPdf != null) btnDevisPdf.setOnAction(e -> onGenerateDevisPdf());
        if (btnGoPayment != null) btnGoPayment.setOnAction(e -> {
            if (lastDraftId == null) {
                showWarning("Draft requis", "Crée draft + HOLD avant paiement.");
                return;
            }
            setStatus("OK → Paiement", "ok");
        });

        // payment actions
        if (btnOpenPayment != null) btnOpenPayment.setOnAction(e -> onOpenPayment());
        if (btnSendEmail != null) btnSendEmail.setOnAction(e -> onSendEmail());
        if (btnConfirm != null) btnConfirm.setOnAction(e -> confirmBooking());

        // debounce
        debounce.setOnFinished(e -> safeRecalc());

        // initial UI
        updateSelectedCount();
        clearPriceLabels();
        refreshPaymentLinkUI();
        refreshExpiryUI();

        // Payment PRO wiring
        if (rbCard != null) rbCard.setToggleGroup(payGroup);
        if (rbPaypal != null) rbPaypal.setToggleGroup(payGroup);
        if (rbLink != null) rbLink.setToggleGroup(payGroup);
        if (rbCard != null) rbCard.setSelected(true);

        payGroup.selectedToggleProperty().addListener((obs, o, n) -> refreshPaymentModeUI());
        refreshPaymentModeUI();

        installCardInputMasks();
        if (txtCardNumber != null) txtCardNumber.textProperty().addListener((o, a, b) -> updateBrandHint());
        if (txtCardCvv != null) txtCardCvv.textProperty().addListener((o, a, b) -> updateBrandHint());

        if (btnPayNow != null) btnPayNow.setOnAction(e -> onPayNow());
        if (btnFake3ds != null) btnFake3ds.setOnAction(e -> simulate3ds());

        if (btnCopyReceipt != null) btnCopyReceipt.setOnAction(e -> {
            if (txtReceipt != null) copyToClipboard(txtReceipt.getText());
            showInfo("Copié", "Reçu copié ✅");
        });

        // OCR
        if (btnScanOcr != null) btnScanOcr.setOnAction(e -> onScanCardOcr());
        if (lblCardHint != null) lblCardHint.setText("OCR: يعبي Name/Number/Expiry من صورة الكارت. CVV يدوي.");

        // optional test presets
        if (btnFillVisaGold != null) btnFillVisaGold.setOnAction(e -> fillCardAll("EISHA KHANNA", "4000123456789017", "12/30", "123"));
        if (btnFillMaster != null) btnFillMaster.setOnAction(e -> fillCardAll("HEDI CHEIKH", "5555555555554444", "11/29", "321"));
        if (btnFillAmex != null) btnFillAmex.setOnAction(e -> fillCardAll("ECO ADVENTURE", "378282246310005", "10/30", "1234"));
        if (btnClearCard != null) btnClearCard.setOnAction(e -> clearCardFields());

        setPayStatus("—", false);
        setReceiptText("");
        updateBrandHint();
    }

    // =======================
    // 1) Client
    // =======================
    private void loadClientContext() {
        int userId = safeInt(spUserId, 1);

        String summary =
                "Client #" + userId + "\n" +
                        "- Historique: (à brancher DB)\n" +
                        "- Segment: NEW\n" +
                        "- Notes: —";

        if (lblClientSummary != null) lblClientSummary.setText(summary);
        if (lblRisk != null) lblRisk.setText("Risk: — (calcule après pricing)");
        setStatus("Client chargé", "ok");
    }

    // =======================
    // 2) Activities
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
            if (q.isEmpty() || r.matches(q)) vbRows.getChildren().add(r.root);
        }
    }

    private void selectMaxAllowed() {
        Pack pack = cbPack == null ? null : cbPack.getValue();
        if (pack == null) return;

        int max = pack.getNbActivitesMax();
        allRows.forEach(r -> r.chk.setSelected(false));

        List<ActiviteRow> visible = getVisibleRows();
        for (int i = 0; i < visible.size() && i < max; i++) visible.get(i).chk.setSelected(true);

        updateSelectedCount();
        safeRecalcDebounced();
    }

    private List<ActiviteRow> getVisibleRows() {
        if (vbRows == null) return Collections.emptyList();
        Set<HBox> visibleRoots = vbRows.getChildren().stream()
                .filter(n -> n instanceof HBox)
                .map(n -> (HBox) n)
                .collect(Collectors.toSet());

        return allRows.stream().filter(r -> visibleRoots.contains(r.root)).collect(Collectors.toList());
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
    private void safeRecalcDebounced() { debounce.playFromStart(); }

    private void safeRecalc() {
        try { recalc(); }
        catch (Exception ex) { if (lblDetails != null) lblDetails.setText("⚠ " + ex.getMessage()); }
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

        updateRiskLabel(pb, nb, selectedActIds.size(), coupon);
    }

    private boolean runRiskGate(boolean showPopup) {
        safeRecalc();

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

        if (!showPopup) {
            if (score >= 85) {
                setStatus("Bloqué (risk trop élevé)", "danger");
                showWarning("Risk Gate", "Score: " + score + "/100\n" + decision + "\n\nBlocage admin: nécessite review.");
                return false;
            }
            return true;
        }

        if (score >= 70) return confirm("Risk Gate", "Score: " + score + "/100\n" + decision + "\n\nContinuer quand même ?");
        showInfo("Risk Gate", "Score: " + score + "/100\n" + decision);
        return true;
    }

    private void updateRiskLabel(PriceBreakdown pb, int nb, int acts, String coupon) {
        BigDecimal total = pb == null ? BigDecimal.ZERO : safeBd(pb.total());
        int score = computeRiskScore(total, nb, acts, coupon, pb);

        String decision = (score >= 70) ? "HIGH" : (score >= 40) ? "MEDIUM" : "LOW";
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

        if (pb != null) {
            BigDecimal disc = safeBd(pb.discountCoupon()).add(safeBd(pb.discountGroupe())).add(safeBd(pb.discountPack()));
            if (disc.compareTo(new BigDecimal("200")) >= 0) score += 15;
        }

        return Math.min(100, score);
    }

    private int currentRiskScore() {
        int nb = safeInt(spNbPersonnes, 1);
        int acts = countSelected();
        String coupon = txtCoupon == null ? "" : txtCoupon.getText();
        BigDecimal total = (lastPb == null ? BigDecimal.ZERO : safeBd(lastPb.total()));
        return computeRiskScore(total, nb, acts, coupon, lastPb);
    }

    // =======================
    // 4) Draft + HOLD
    // =======================
    private void createDraftAndHold() {
        Pack pack = cbPack == null ? null : cbPack.getValue();
        if (pack == null) { showWarning("Pack requis", "Choisis un pack."); return; }

        int userId = safeInt(spUserId, 1);
        int nb = safeInt(spNbPersonnes, 1);
        String coupon = txtCoupon == null ? "" : txtCoupon.getText();

        List<Integer> actIds = allRows.stream()
                .filter(r -> r.chk.isSelected())
                .map(r -> r.activite.getIdActivite())
                .collect(Collectors.toList());

        if (actIds.isEmpty()) { showWarning("Activité requise", "Sélectionne au moins une activité."); return; }
        if (actIds.size() > pack.getNbActivitesMax()) { showWarning("Limite atteinte", "Max activités = " + pack.getNbActivitesMax()); return; }
        if (!runRiskGate(true)) return;

        try {
            int draftId = service.createInscriptionDraft(userId, pack.getIdPack(), actIds, nb, coupon);

            lastDraftId = draftId;
            bookingStatus = BookingStatus.HELD;

            expiryAt = LocalDateTime.now().plusMinutes(30);
            refreshExpiryUI();

            lastPaymentLink = "https://pay.example/checkout?draftId=" + draftId;
            refreshPaymentLinkUI();

            scheduleExpiry();
            clearPaymentContext();

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

    private void refreshExpiryUI() { if (lblExpiry != null) lblExpiry.setText(expiryAt == null ? "—" : expiryAt.toString()); }
    private void refreshPaymentLinkUI() { if (txtPaymentLink != null) txtPaymentLink.setText(lastPaymentLink == null ? "" : lastPaymentLink); }

    private void resetBookingContext() {
        lastDraftId = null;
        bookingStatus = null;
        lastPaymentLink = null;
        expiryAt = null;
        refreshPaymentLinkUI();
        refreshExpiryUI();
        clearPaymentContext();

        if (expiryTask != null) {
            try { expiryTask.cancel(true); } catch (Exception ignored) {}
        }
    }

    private void clearPaymentContext() {
        paymentApproved = false;
        paymentRef = null;
        setPayStatus("—", false);
        setReceiptText("");
        updateBrandHint();
    }

    // =======================
    // 5) Payment PRO
    // =======================
    private void refreshPaymentModeUI() {
        boolean card = rbCard != null && rbCard.isSelected();
        boolean link = rbLink != null && rbLink.isSelected();
        boolean paypal = rbPaypal != null && rbPaypal.isSelected();

        if (boxCardForm != null) { boxCardForm.setVisible(card); boxCardForm.setManaged(card); }
        if (boxLink != null) { boxLink.setVisible(link || paypal); boxLink.setManaged(link || paypal); }

        if (lblPayBadge != null) lblPayBadge.setText(paypal ? "PAYPAL SANDBOX" : (link ? "CHECKOUT" : "CARTE (LOCAL)"));
        if (btnOpenPayment != null) btnOpenPayment.setDisable(!(link || paypal));
        if (btnSendEmail != null) btnSendEmail.setDisable(!(link || paypal));
    }

    private void installCardInputMasks() {
        if (txtCardNumber != null) {
            txtCardNumber.textProperty().addListener((o, old, val) -> {
                if (val == null) return;
                String digits = val.replaceAll("\\D", "");
                if (digits.length() > 19) digits = digits.substring(0, 19);

                String formatted = formatCardNumber(digits);
                if (!formatted.equals(val)) {
                    int caret = Math.min(formatted.length(), txtCardNumber.getCaretPosition());
                    txtCardNumber.setText(formatted);
                    txtCardNumber.positionCaret(caret);
                }
            });
        }

        if (txtCardExpiry != null) {
            txtCardExpiry.textProperty().addListener((o, old, val) -> {
                if (val == null) return;
                String d = val.replaceAll("\\D", "");
                if (d.length() > 4) d = d.substring(0, 4);
                String formatted = (d.length() <= 2) ? d : d.substring(0, 2) + "/" + d.substring(2);
                if (!formatted.equals(val)) {
                    int caret = Math.min(formatted.length(), txtCardExpiry.getCaretPosition());
                    txtCardExpiry.setText(formatted);
                    txtCardExpiry.positionCaret(caret);
                }
            });
        }

        if (txtCardCvv != null) {
            txtCardCvv.textProperty().addListener((o, old, val) -> {
                if (val == null) return;
                String d = val.replaceAll("\\D", "");
                if (d.length() > 4) d = d.substring(0, 4);
                if (!d.equals(val)) txtCardCvv.setText(d);
            });
        }
    }

    private void onPayNow() {
        if (lastDraftId == null) { showWarning("Draft requis", "Crée draft + HOLD avant paiement."); return; }
        if (bookingStatus == BookingStatus.EXPIRE) { showWarning("Expired", "Draft expired. Recrée un draft."); return; }
        if (lastPb == null) safeRecalc();

        if (rbPaypal != null && rbPaypal.isSelected()) { ensurePaymentLink("paypal"); onOpenPayment(); return; }
        if (rbLink != null && rbLink.isSelected()) { ensurePaymentLink("checkout"); onOpenPayment(); return; }

        String name = txtCardName == null ? "" : txtCardName.getText().trim();
        String numDigits = txtCardNumber == null ? "" : txtCardNumber.getText().replaceAll("\\D", "");
        String exp = txtCardExpiry == null ? "" : txtCardExpiry.getText().trim();
        String cvv = txtCardCvv == null ? "" : txtCardCvv.getText().trim();

        if (name.isEmpty() || numDigits.length() < 13 || !EXPIRY_RGX.matcher(exp).matches()) {
            showWarning("Champs invalides", "Vérifie (Nom / Numéro / MM/YY).");
            return;
        }
        if (!luhnValid(numDigits)) {
            showWarning("Carte invalide", "Le numéro de carte n'est pas valide (Luhn).");
            return;
        }

        String brand = detectBrand(numDigits);
        int expectedCvv = brand.equals("AMEX") ? 4 : 3;
        if (cvv.length() < expectedCvv) {
            showWarning("CVV invalide", "CVV لازم " + expectedCvv + " chiffres pour " + brand + ".");
            return;
        }

        int risk = currentRiskScore();
        if (risk >= 85) {
            showWarning("Risk Policy", "Risk شديد (" + risk + "/100)\n➡ Card local ممنوع. استعمل Checkout.");
            if (rbLink != null) rbLink.setSelected(true);
            ensurePaymentLink("checkout");
            return;
        }

        bookingStatus = BookingStatus.EN_ATTENTE_PAIEMENT;
        paymentApproved = false;
        paymentRef = null;

        setPayStatus("Vérification…", true);
        setReceiptText("");

        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

        ex.schedule(() -> Platform.runLater(() -> setPayStatus("3D Secure…", true)),
                900, TimeUnit.MILLISECONDS);

        ex.schedule(() -> Platform.runLater(() -> {
            boolean ok = new Random().nextInt(100) >= 8;

            if (!ok) {
                bookingStatus = BookingStatus.HELD;
                paymentApproved = false;
                setPayStatus("Paiement refusé / échec", false);
                setReceiptText(buildReceiptPro(false));
                showWarning("Paiement échoué", "Transaction refusée. Réessaie أو استعمل Checkout.");
            } else {
                paymentApproved = true;
                bookingStatus = BookingStatus.HELD;
                paymentRef = "PAY-" + lastDraftId + "-" + System.currentTimeMillis();
                setPayStatus("Paiement approuvé ✅", false);
                setReceiptText(buildReceiptPro(true));
            }
            ex.shutdownNow();
        }), 2200, TimeUnit.MILLISECONDS);
    }

    private void simulate3ds() {
        if (lastDraftId == null) { showWarning("Draft requis", "Crée draft + HOLD avant simulation."); return; }
        setPayStatus("3D Secure (simulé)…", true);

        PauseTransition p = new PauseTransition(Duration.seconds(1.3));
        p.setOnFinished(e -> setPayStatus("3D Secure terminé ✅", false));
        p.playFromStart();
    }

    private void ensurePaymentLink(String mode) {
        if (lastPaymentLink != null && !lastPaymentLink.isBlank()) return;
        lastPaymentLink = "https://pay.example/" + mode
                + "?draftId=" + lastDraftId
                + "&amount=" + (lastPb == null ? "0" : lastPb.total().toPlainString());
        refreshPaymentLinkUI();
    }

    private void setPayStatus(String msg, boolean loading) {
        if (lblPayStatus != null) lblPayStatus.setText(msg);
        if (piPay != null) { piPay.setVisible(loading); piPay.setManaged(loading); }
    }

    private void setReceiptText(String txt) { if (txtReceipt != null) txtReceipt.setText(txt == null ? "" : txt); }

    private String buildReceiptPro(boolean ok) {
        String total = (lastPb == null ? "--" : format(lastPb.total()));
        String methodLabel =
                (rbPaypal != null && rbPaypal.isSelected()) ? "PayPal (Sandbox)" :
                        (rbLink != null && rbLink.isSelected()) ? "Checkout Link" :
                                "Carte bancaire (Local – Simulation)";

        String num = txtCardNumber == null ? "" : txtCardNumber.getText().replaceAll("\\D", "");
        String brand = detectBrand(num);
        String last4 = num.length() >= 4 ? num.substring(num.length() - 4) : "";

        String statusLine = ok ? "✅ APPROUVÉ" : "❌ REFUSÉ";
        String when = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .format(LocalDateTime.now());

        String ref = (paymentRef == null ? "-" : paymentRef);
        String cardLine = last4.isBlank() ? "" : (brand + " • **** **** **** " + last4);

        return "ECOADVENTURE • Reçu de Paiement (Sandbox)\n"
                + "────────────────────────────────────────\n"
                + "Statut           : " + statusLine + "\n"
                + "Date & heure     : " + when + " (Africa/Tunis)\n\n"
                + "Commande / Draft : #" + (lastDraftId == null ? "-" : lastDraftId) + "\n"
                + "Référence        : " + ref + "\n"
                + "Méthode          : " + methodLabel + "\n"
                + (cardLine.isEmpty() ? "" : "Carte            : " + cardLine + "\n\n")
                + "Montant          : " + total + "\n"
                + "Frais            : 0.00 DT\n"
                + "Total payé       : " + total + "\n\n"
                + "Sécurité         : 3D Secure (simulé) • Aucune donnée sensible stockée\n"
                + "Prochaine étape  : Confirmer la réservation (Admin)\n"
                + "────────────────────────────────────────\n"
                + "Merci pour votre confiance ✨";
    }

    // =======================
    // 6) OCR INTELLIGENT (Dynamic)
    // =======================
    private void onScanCardOcr() {
        if (lastDraftId == null) {
            showWarning("Draft requis", "Crée draft + HOLD avant OCR.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir صورة الكارت (Front)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File file = fc.showOpenDialog(btnScanOcr.getScene().getWindow());
        if (file == null) return;

        setPayStatus("OCR…", true);

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        BufferedImage original = ImageIO.read(file);
                        if (original == null) throw new RuntimeException("Image invalide.");
                        BufferedImage pre = preprocessForOcr(original);
                        return callOcrApiFront(pre); // Map result
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .whenComplete((res, err) -> Platform.runLater(() -> {
                    setPayStatus("—", false);

                    if (err != null || res == null) {
                        showWarning("OCR", "Échec OCR: " + (err != null ? err.getMessage() : "unknown"));
                        return;
                    }

                    String name  = safeStr((String) res.get("name"));
                    String expiry= safeStr((String) res.get("expiry"));
                    String pan   = safeStr((String) res.get("pan"));
                    String brand = safeStr((String) res.get("brand"));
                    String last4 = safeStr((String) res.get("last4"));

                    // ✅ FILL FIELDS مباشرة
                    if (txtCardName != null)   txtCardName.setText(name);
                    if (txtCardExpiry != null) txtCardExpiry.setText(expiry);
                    if (txtCardNumber != null) txtCardNumber.setText(formatCardNumber(pan));

                    // ✅ Hint
                    if (lblCardHint != null) lblCardHint.setText("OCR OK ✅ " + brand + " • **** " + last4 + " | CVV يدوي");

                    // ✅ update receipt preview (اختياري)
                    setReceiptText("OCR Filled ✅\nName: " + name + "\nExpiry: " + expiry + "\nCard: **** " + last4);

                    updateBrandHint();
                }));
    }

    private BufferedImage preprocessForOcr(BufferedImage src) {
        // 1) resize (bigger helps OCR)
        int targetW = 1400;
        double scale = (src.getWidth() >= targetW) ? 1.0 : (targetW / (double) src.getWidth());
        int newW = (int) Math.round(src.getWidth() * scale);
        int newH = (int) Math.round(src.getHeight() * scale);

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();

        // 2) grayscale + contrast
        BufferedImage gray = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = gray.createGraphics();
        g2.drawImage(resized, 0, 0, null);
        g2.dispose();

        // simple contrast stretch
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);
        int min = 255, max = 0;
        for (int y = 0; y < newH; y++) {
            for (int x = 0; x < newW; x++) {
                int p = gray.getRaster().getSample(x, y, 0);
                if (p < min) min = p;
                if (p > max) max = p;
            }
        }
        int range = Math.max(1, (max - min));
        for (int y = 0; y < newH; y++) {
            for (int x = 0; x < newW; x++) {
                int p = gray.getRaster().getSample(x, y, 0);
                int v = (p - min) * 255 / range;
                out.getRaster().setSample(x, y, 0, v);
            }
        }
        return out;
    }

    private Map<String, Object> callOcrApiFront(BufferedImage image) {
        try {
            byte[] pngBytes = bufferedImageToPng(image);
            String b64 = Base64.getEncoder().encodeToString(pngBytes);

            Map<String, String> payload = new HashMap<>();
            payload.put("front_image_base64", b64);
            payload.put("tessdata_path", TESSDATA_PATH);

            Gson g = new Gson();
            String json = g.toJson(payload);

            URL url = new URL(OCR_API_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = con.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
            String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            Map<String, Object> map = g.fromJson(resp, Map.class);
            if (map == null) throw new RuntimeException("OCR response vide");

            // ✅ debug سريع (اختياري)
            System.out.println("OCR API RESPONSE: " + resp);

            if (map.containsKey("error")) throw new RuntimeException(String.valueOf(map.get("error")));
            return map;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] bufferedImageToPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    // =======================
    // 7) Link/Email/Confirm
    // =======================
    private void onOpenPayment() {
        if (lastDraftId == null) { showWarning("Draft requis", "Crée draft + HOLD avant paiement."); return; }
        if (bookingStatus == BookingStatus.EXPIRE) { showWarning("Expired", "Draft expired. Recrée un draft."); return; }

        if (lastPaymentLink == null || lastPaymentLink.isBlank()) {
            lastPaymentLink = "https://pay.example/checkout?draftId=" + lastDraftId;
            refreshPaymentLinkUI();
        }

        try {
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
        if (lastDraftId == null) { showWarning("Draft requis", "Crée draft + HOLD avant email."); return; }

        String subject = "EcoAdventure — Devis / Paiement — Draft #" + lastDraftId;
        String body = buildDevisText() + "\n\nPaiement: " + (lastPaymentLink == null ? "" : lastPaymentLink);

        copyToClipboard("SUBJECT: " + subject + "\n\n" + body);
        showInfo("Email (fallback)", "JavaMail غير مربوط.\n✅ محتوى الإيميل تـcopy في Clipboard.");
    }

    private void confirmBooking() {
        if (lastDraftId == null) { showWarning("Draft requis", "Crée draft + HOLD avant confirmation."); return; }
        if (bookingStatus == BookingStatus.EXPIRE) { showWarning("Expired", "Draft expired. Recrée un draft."); return; }

        boolean needsApproval = (rbCard != null && rbCard.isSelected());
        if (needsApproval && !paymentApproved) {
            showWarning("Paiement requis", "Il faut payer d'abord (Paiement approuvé ✅) قبل confirmation.");
            return;
        }

        boolean ok = confirm("Confirmer", "Confirmer Draft #" + lastDraftId + " ?");
        if (!ok) return;

        bookingStatus = BookingStatus.CONFIRME;
        setStatus("CONFIRME (#" + lastDraftId + ")", "ok");

        if (txtReceipt != null && (txtReceipt.getText() == null || txtReceipt.getText().isBlank())) {
            setReceiptText(buildReceiptPro(true));
        }

        showInfo("Confirmé", "Booking confirmé ✅");
    }

    // =======================
    // 8) Devis PDF + QR (kept minimal fallback)
    // =======================
    private void onGenerateDevisPdf() {
        if (lastDraftId == null) { showWarning("Draft requis", "Crée draft + HOLD قبل Devis PDF."); return; }
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
                showInfo("Fallback", "PDFBox غير موجود.\nQR saved: " + png.getAbsolutePath() + "\nDevis copied.");
                return;
            }

            showInfo("✅ Devis PDF", "PDF créé:\n" + out.getAbsolutePath());

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
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);
            g.setColor(Color.BLACK); g.drawRect(0, 0, w - 1, h - 1);
            g.drawString("QR (ZXing missing)", 10, 20);
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
            Object cs = pdPageContentStreamCls.getDeclaredConstructor(pdDocumentCls, pdPageCls).newInstance(doc, page);

            Class<?> pdType1FontCls = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
            Object font = pdType1FontCls.getField("HELVETICA").get(null);

            pdPageContentStreamCls.getMethod("beginText").invoke(cs);
            pdPageContentStreamCls.getMethod("setFont", Class.forName("org.apache.pdfbox.pdmodel.font.PDFont"), float.class)
                    .invoke(cs, font, 12f);
            pdPageContentStreamCls.getMethod("newLineAtOffset", float.class, float.class).invoke(cs, 50f, 750f);

            for (String line : text.split("\n")) {
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
    // 9) Devis text
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
    // 10) Helpers
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

    private String format(BigDecimal v) { return (v == null ? "0.00" : v.toPlainString()) + " DT"; }
    private BigDecimal safeBd(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private String safeStr(String s) { return s == null ? "" : s; }

    private int safeInt(Spinner<Integer> sp, int def) {
        if (sp == null) return def;
        try { Integer v = sp.getValue(); if (v != null) return v; } catch (Exception ignored) {}
        try {
            String t = sp.getEditor().getText();
            if (t != null && !t.isBlank()) return Integer.parseInt(t.trim());
        } catch (Exception ignored) {}
        return def;
    }

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
    public void handleBack(javafx.event.ActionEvent event) {
        try {
            if (expiryTask != null) expiryTask.cancel(true);
            if (scheduler != null) scheduler.shutdownNow();
        } catch (Exception ignored) {}
        SceneUtils.loadScene("/GUI/MainLayout.fxml", (Node) event.getSource());
    }

    private void clearCardFields() {
        if (txtCardName != null) txtCardName.clear();
        if (txtCardNumber != null) txtCardNumber.clear();
        if (txtCardExpiry != null) txtCardExpiry.clear();
        if (txtCardCvv != null) txtCardCvv.clear();
        updateBrandHint();
    }

    private void fillCardAll(String name, String digits, String expiry, String cvv) {
        if (txtCardName != null) txtCardName.setText(name);
        if (txtCardNumber != null) txtCardNumber.setText(formatCardNumber(digits));
        if (txtCardExpiry != null) txtCardExpiry.setText(expiry);
        if (txtCardCvv != null) txtCardCvv.setText(cvv);
        updateBrandHint();
    }

    private String formatCardNumber(String digits) {
        String d = digits == null ? "" : digits.replaceAll("\\D", "");
        if (d.length() == 15) return d.substring(0,4) + " " + d.substring(4,10) + " " + d.substring(10); // AMEX 4-6-5
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(' ');
            sb.append(d.charAt(i));
        }
        return sb.toString();
    }

    private void updateBrandHint() {
        if (lblCardHint == null || txtCardNumber == null) return;

        String d = txtCardNumber.getText() == null ? "" : txtCardNumber.getText().replaceAll("\\D", "");
        if (d.isEmpty()) { lblCardHint.setText("—"); return; }

        String brand = detectBrand(d);
        int expectedCvv = brand.equals("AMEX") ? 4 : 3;

        String last4 = d.length() >= 4 ? d.substring(d.length() - 4) : d;
        String cvvVal = (txtCardCvv == null || txtCardCvv.getText() == null) ? "" : txtCardCvv.getText().trim();

        lblCardHint.setText("Détecté: " + brand + " • **** " + last4
                + " • CVV attendu: " + expectedCvv
                + (cvvVal.isEmpty() ? "" : (" • saisi: " + cvvVal.length() + " chiffres")));
    }

    private String detectBrand(String digits) {
        if (digits.startsWith("4")) return "VISA";
        if (digits.matches("^5[1-5].*")) return "MASTERCARD";
        if (digits.matches("^2(2[2-9]\\d|[3-6]\\d\\d|7[01]\\d|720).*")) return "MASTERCARD";
        if (digits.startsWith("34") || digits.startsWith("37")) return "AMEX";
        if (digits.startsWith("6")) return "DISCOVER";
        return "CARD";
    }

    private boolean luhnValid(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            char c = digits.charAt(i);
            if (c < '0' || c > '9') return false;
            int n = c - '0';
            if (alt) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
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
    private static BufferedImage preprocess(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();

        // resize to width ~1400
        int targetW = 1400;
        double scale = w >= targetW ? 1.0 : (targetW / (double) w);
        int nw = (int) Math.round(w * scale);
        int nh = (int) Math.round(h * scale);

        BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();

        BufferedImage gray = new BufferedImage(nw, nh, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = gray.createGraphics();
        g2.drawImage(resized, 0, 0, null);
        g2.dispose();

        // simple contrast stretch
        int min = 255, max = 0;
        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
                int p = gray.getRaster().getSample(x, y, 0);
                if (p < min) min = p;
                if (p > max) max = p;
            }
        }
        int range = Math.max(1, max - min);

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
                int p = gray.getRaster().getSample(x, y, 0);
                int v = (p - min) * 255 / range;
                out.getRaster().setSample(x, y, 0, v);
            }
        }
        return out;
    }
}