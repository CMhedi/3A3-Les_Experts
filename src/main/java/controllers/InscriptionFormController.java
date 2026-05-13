package controllers;

import Entities.Inscription;
import Entities.Pack;
import Entities.UserApp;
import Services.InscriptionService;
import Services.PackService;
import Services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InscriptionFormController {

    // ─── FXML ─────────────────────────────────────────────
    @FXML private Label     lblFormTitle;
    @FXML private Label     lblInscrId;

    @FXML private ComboBox<String> cbUser;
    @FXML private ComboBox<String> cbPack;
    @FXML private Label     lblNomUser;
    @FXML private Label     lblNomPack;
    @FXML private Label     lblPrixBase;
    @FXML private Label     lblDiscount;
    @FXML private TextField txtMontant;

    @FXML private ComboBox<String> cbStatut;
    @FXML private Label     lblStatutBadge;

    @FXML private RadioButton   rbCard;
    @FXML private RadioButton   rbPaypal;
    @FXML private RadioButton   rbVirement;
    @FXML private VBox          boxCardForm;
    @FXML private TextField     txtCardNumber;
    @FXML private TextField     txtCardName;
    @FXML private TextField     txtCardExpiry;
    @FXML private PasswordField txtCardCvv;
    @FXML private Button        btnScanOcr;
    @FXML private Button        btnPayNow;
    @FXML private ProgressIndicator piPay;
    @FXML private Label         lblPayStatus;
    @FXML private Label         lblCardHint;
    @FXML private VBox          boxPayRecap;
    @FXML private Label         lblPayRef;
    @FXML private Label         lblOrderId;
    @FXML private Label         lblPaidAtInfo;

    // ─── Services ─────────────────────────────────────────
    private final InscriptionService inscService = new InscriptionService();
    private final PackService        packService = new PackService();
    private final UserService        userService = new UserService();

    // ─── État ─────────────────────────────────────────────
    private Inscription editingInscription = null;
    private Runnable    onSavedCallback    = null;

    private final Map<Integer, String>  userLabelById = new HashMap<>();
    private final Map<Integer, UserApp> userById      = new HashMap<>();
    private final Map<Integer, String>  packLabelById = new HashMap<>();
    private final Map<Integer, Pack>    packById      = new HashMap<>();

    // Pack actuellement sélectionné — requis pour add(insc, pack) / update(insc, pack)
    private Pack selectedPack = null;

    // Paiement sandbox
    private boolean       paymentDone      = false;
    private String        paymentReference = null;
    private String        paymentOrderId   = null;
    private String        paymentGateway   = null;
    private String        paymentStatus    = null;
    private LocalDateTime paidAt           = null;

    // ════════════════════════════════════════════════════════
    //  INITIALISATION
    // ════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        // ── Statuts depuis la BDD ──
        List<String> statuts = inscService.getAllowedStatutsFromDB();
        cbStatut.setItems(FXCollections.observableArrayList(statuts));
        cbStatut.getSelectionModel().selectFirst();
        cbStatut.setOnAction(e -> refreshStatutBadge());

        // ── Groupe radio paiement ──
        ToggleGroup tg = new ToggleGroup();
        rbCard.setToggleGroup(tg);
        rbPaypal.setToggleGroup(tg);
        rbVirement.setToggleGroup(tg);
        tg.selectedToggleProperty().addListener((obs, o, n) -> refreshPaymentSection());
        rbCard.setSelected(true);
        refreshPaymentSection();

        // ── Auto-format numéro carte (groupes de 4) ──
        txtCardNumber.textProperty().addListener((obs, o, n) -> {
            String digits = n.replaceAll("[^0-9]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            String fmt = digits.replaceAll("(.{4})", "$1 ").trim();
            if (!fmt.equals(n)) { String f = fmt; Platform.runLater(() -> txtCardNumber.setText(f)); }
            detectCardBrand(digits);
        });

        // ── Auto-format expiry MM/AA ──
        txtCardExpiry.textProperty().addListener((obs, o, n) -> {
            String d = n.replaceAll("[^0-9]", "");
            if (d.length() > 4) d = d.substring(0, 4);
            String f = d.length() > 2 ? d.substring(0, 2) + "/" + d.substring(2) : d;
            if (!f.equals(n)) { String ff = f; Platform.runLater(() -> txtCardExpiry.setText(ff)); }
        });

        loadLookups();
        refreshStatutBadge();
    }

    // ════════════════════════════════════════════════════════
    //  API PUBLIQUE
    // ════════════════════════════════════════════════════════

    public void setInscription(Inscription insc) {
        this.editingInscription = insc;
        if (insc == null) {
            lblFormTitle.setText("Nouvelle Inscription");
            lblInscrId.setText("#—");
            return;
        }
        lblFormTitle.setText("Modifier l'Inscription");
        lblInscrId.setText("#" + insc.getIdInscription());

        // Utilisateur
        String uLabel = userLabelById.get(insc.getIdUser());
        if (uLabel != null) cbUser.getSelectionModel().select(uLabel);
        refreshUserInfo(insc.getIdUser());

        // Pack
        String pLabel = packLabelById.get(insc.getIdPack());
        if (pLabel != null) {
            cbPack.getSelectionModel().select(pLabel);
            selectedPack = packById.get(insc.getIdPack());
        }
        refreshPackInfo(insc.getIdPack());

        // Statut
        if (insc.getStatutInscr() != null)
            cbStatut.getSelectionModel().select(insc.getStatutInscr());
        refreshStatutBadge();

        // Montant
        if (insc.getMontantTotal() != null)
            txtMontant.setText(insc.getMontantTotal().toPlainString());

        // Paiement existant
        paymentGateway   = insc.getPaymentGateway();
        paymentReference = insc.getPaymentReference();
        paymentOrderId   = insc.getPaymentOrderId();
        paymentStatus    = insc.getPaymentStatus();
        paidAt           = toLocalDateTime(insc.getPaidAt());

        if ("paid".equalsIgnoreCase(paymentStatus)) {
            paymentDone = true;
            showPayRecap();
        }
    }

    public void setOnSaved(Runnable cb) { this.onSavedCallback = cb; }

    // ════════════════════════════════════════════════════════
    //  STATUT — badge coloré
    // ════════════════════════════════════════════════════════

    private void refreshStatutBadge() {
        String s = cbStatut.getValue();
        if (s == null) return;
        String style = switch (s.toUpperCase()) {
            case "CONFIRMEE"  -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534;";
            case "EN_ATTENTE" -> "-fx-background-color: #fef9c3; -fx-text-fill: #92400e;";
            case "ANNULEE"    -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            default           -> "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;";
        };
        lblStatutBadge.setStyle(style
                + "-fx-background-radius: 20; -fx-padding: 4 14 4 14; "
                + "-fx-font-size: 12; -fx-font-weight: bold;");
        lblStatutBadge.setText(s);
    }

    // ════════════════════════════════════════════════════════
    //  PAIEMENT
    // ════════════════════════════════════════════════════════

    private void refreshPaymentSection() {
        boolean isCard = rbCard.isSelected();
        boxCardForm.setVisible(isCard);
        boxCardForm.setManaged(isCard);
    }

    private void detectCardBrand(String digits) {
        String brand;
        if      (digits.startsWith("4"))          brand = "💙 Visa";
        else if (digits.matches("^5[1-5].*"))     brand = "🔴 Mastercard";
        else if (digits.matches("^3[47].*"))      brand = "🟢 American Express";
        else if (digits.matches("^6(?:011|5).*")) brand = "🟡 Discover";
        else if (digits.isEmpty())                brand = "";
        else                                      brand = "💳 Carte bancaire";
        lblCardHint.setText(brand.isBlank() ? "" : "Détectée : " + brand);
    }

    // ── OCR ───────────────────────────────────────────────

    @FXML
    private void onScanOcr() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une image de carte");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.bmp","*.tiff"));
        File file = fc.showOpenDialog((Stage) btnScanOcr.getScene().getWindow());
        if (file == null) return;

        lblCardHint.setText("⏳ Scan en cours…");
        btnScanOcr.setDisable(true);

        new Thread(() -> {
            try {
                String text = runOcr(file);
                Platform.runLater(() -> { fillCardFromOcr(text); btnScanOcr.setDisable(false); });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    lblCardHint.setText("❌ OCR échoué : " + ex.getMessage());
                    btnScanOcr.setDisable(false);
                });
            }
        }).start();
    }

    private String runOcr(File imageFile) throws TesseractException {
        Tesseract tess = new Tesseract();
        String tessPath = System.getenv("TESSDATA_PREFIX");
        if (tessPath == null || tessPath.isBlank()) tessPath = "src/main/resources/tessdata";
        tess.setDatapath(tessPath);
        tess.setLanguage("eng");
        tess.setPageSegMode(6);
        return tess.doOCR(imageFile);
    }

    private void fillCardFromOcr(String raw) {
        if (raw == null || raw.isBlank()) {
            lblCardHint.setText("⚠ Aucun texte détecté.");
            return;
        }
        String text = raw.replaceAll("\\s+", " ").trim();
        boolean found = false;

        // Numéro : 16 chiffres
        Matcher mNum = Pattern.compile("\\b(\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4})\\b")
                .matcher(text);
        if (mNum.find()) {
            txtCardNumber.setText(mNum.group(1).replaceAll("[^0-9]","").replaceAll("(.{4})","$1 ").trim());
            found = true;
        }

        // Expiry MM/YY
        Matcher mExp = Pattern.compile("\\b(0[1-9]|1[0-2])[/\\-](\\d{2}(?:\\d{2})?)\\b").matcher(text);
        if (mExp.find()) {
            String yr = mExp.group(2).length() == 4 ? mExp.group(2).substring(2) : mExp.group(2);
            txtCardExpiry.setText(mExp.group(1) + "/" + yr);
            found = true;
        }

        // Nom titulaire (lignes en MAJUSCULES)
        Matcher mName = Pattern.compile("\\b([A-Z]{2,}(?:\\s[A-Z]{2,})+)\\b").matcher(text);
        String bestName = "";
        while (mName.find()) {
            String c = mName.group(1);
            if (c.matches("VISA|MASTERCARD|AMERICAN EXPRESS|DISCOVER|CARD")) continue;
            if (c.length() > bestName.length()) bestName = c;
        }
        if (!bestName.isBlank()) { txtCardName.setText(bestName); found = true; }

        lblCardHint.setText(found
                ? "✅ Carte scannée — vérifiez les champs."
                : "⚠ Scan partiel — complétez manuellement.");
    }

    // ── Bouton Payer ──────────────────────────────────────

    @FXML
    private void onPayNow() {
        String num    = txtCardNumber.getText().replaceAll("\\s","");
        String name   = txtCardName.getText().trim();
        String expiry = txtCardExpiry.getText().trim();
        String cvv    = txtCardCvv.getText().trim();

        if (num.length() < 16)           { showPayError("Numéro de carte invalide (16 chiffres)."); return; }
        if (name.isBlank())              { showPayError("Saisissez le nom sur la carte."); return; }
        if (!expiry.matches("\\d{2}/\\d{2}")) { showPayError("Format date invalide (MM/AA)."); return; }
        if (cvv.length() < 3)            { showPayError("CVV invalide."); return; }

        piPay.setVisible(true);  piPay.setManaged(true);
        btnPayNow.setDisable(true);
        lblPayStatus.setText("⏳ Traitement en cours…");
        lblPayStatus.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13;");

        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            String last4   = num.substring(num.length() - 4);
            String brand   = num.startsWith("4") ? "VISA" : num.startsWith("5") ? "MC" : "CARD";
            String ref     = "DEMO-" + last4 + "-" + UUID.randomUUID().toString()
                    .replace("-","").substring(0, 8).toUpperCase();
            String orderId = "PACK-" + System.currentTimeMillis();

            Platform.runLater(() -> {
                piPay.setVisible(false); piPay.setManaged(false);
                btnPayNow.setDisable(false);

                paymentDone      = true;
                paymentGateway   = "card_demo";
                paymentReference = ref;
                paymentOrderId   = orderId;
                paymentStatus    = "paid";
                paidAt           = LocalDateTime.now();

                // Passer le statut à CONFIRMEE automatiquement
                cbStatut.getSelectionModel().select("CONFIRMEE");
                refreshStatutBadge();

                lblPayStatus.setText("✅ Paiement accepté (" + brand + " •••• " + last4 + ")");
                lblPayStatus.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 13; -fx-font-weight: bold;");
                showPayRecap();
            });
        }).start();
    }

    private void showPayError(String msg) {
        lblPayStatus.setText("❌ " + msg);
        lblPayStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13;");
    }

    private void showPayRecap() {
        boxPayRecap.setVisible(true); boxPayRecap.setManaged(true);
        lblPayRef.setText("Référence : "   + safe(paymentReference));
        lblOrderId.setText("Order ID : "   + safe(paymentOrderId));
        String d = paidAt != null
                ? paidAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        lblPaidAtInfo.setText("Payé le : " + d);
    }

    @FXML private void onFillVisa() {
        txtCardNumber.setText("4539 1488 0343 6467");
        txtCardName.setText("TEST VISA GOLD");
        txtCardExpiry.setText("12/26");
        txtCardCvv.setText("737");
    }

    @FXML private void onFillMaster() {
        txtCardNumber.setText("5425 2334 3010 9903");
        txtCardName.setText("TEST MASTERCARD");
        txtCardExpiry.setText("09/27");
        txtCardCvv.setText("123");
    }

    @FXML private void onClearCard() {
        txtCardNumber.clear(); txtCardName.clear();
        txtCardExpiry.clear(); txtCardCvv.clear();
        lblCardHint.setText("");
    }

    // ════════════════════════════════════════════════════════
    //  ENREGISTRER — utilise add(insc, pack) / update(insc, pack)
    // ════════════════════════════════════════════════════════

    @FXML
    private void onSave() {
        if (cbUser.getValue() == null) { showInfo("Champ requis", "Sélectionnez un utilisateur."); return; }
        if (cbPack.getValue() == null) { showInfo("Champ requis", "Sélectionnez un pack.");        return; }
        if (selectedPack     == null)  { showInfo("Champ requis", "Pack introuvable.");             return; }

        try {
            Inscription insc = editingInscription != null ? editingInscription : new Inscription();

            // Utilisateur
            int userId = getUserIdFromLabel(cbUser.getValue());
            insc.setIdUser(userId);
            UserApp u = userById.get(userId);
            insc.setNomUser(u != null ? buildUserLabel(u) : cbUser.getValue());

            // Pack
            insc.setIdPack(selectedPack.getIdPack());
            insc.setNomPack(safe(selectedPack.getNom()));

            // Statut
            insc.setStatutInscr(cbStatut.getValue());

            // Paiement — injecter dans l'objet avant save
            if (paymentDone) {
                insc.setPaymentGateway(paymentGateway);
                insc.setPaymentReference(paymentReference);
                insc.setPaymentOrderId(paymentOrderId);
                insc.setPaymentStatus(paymentStatus);
                insc.setPaidAt(paidAt);
            } else if (rbPaypal.isSelected()) {
                insc.setPaymentGateway("paypal");
                insc.setPaymentStatus("initiated");
            } else if (rbVirement.isSelected()) {
                insc.setPaymentGateway("virement");
                insc.setPaymentStatus("initiated");
            }

            if (editingInscription == null) {
                // ── ADD : le service calcule montant + loyalty ──
                insc.setDateInscription(LocalDateTime.now());
                int newId = inscService.add(insc, selectedPack);

                // Si paiement déjà fait, persister les colonnes paiement séparément
                if (paymentDone && newId > 0) {
                    inscService.updatePayment(newId,
                            paymentGateway, paymentReference, paymentOrderId,
                            paymentStatus, paidAt, null);
                }

            } else {
                // ── UPDATE : le service recalcule montant + loyalty ──
                inscService.update(insc, selectedPack);

                // Mettre à jour les colonnes paiement si paiement vient d'être effectué
                if (paymentDone) {
                    inscService.updatePayment(insc.getIdInscription(),
                            paymentGateway, paymentReference, paymentOrderId,
                            paymentStatus, paidAt, null);
                }
            }

            if (onSavedCallback != null) onSavedCallback.run();
            closeWindow();

        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void onCancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) cbUser.getScene().getWindow()).close();
    }

    // ════════════════════════════════════════════════════════
    //  CHARGEMENT DONNÉES
    // ════════════════════════════════════════════════════════

    private void loadLookups() {
        // Packs
        try {
            for (Pack p : packService.getAll()) {
                packById.put(p.getIdPack(), p);
                packLabelById.put(p.getIdPack(), safe(p.getNom()));
            }
            cbPack.setItems(FXCollections.observableArrayList(
                    packById.values().stream().map(p -> safe(p.getNom())).toList()));
            cbPack.setOnAction(e -> {
                String lbl = cbPack.getValue();
                if (lbl == null) return;
                packById.values().stream()
                        .filter(p -> safe(p.getNom()).equals(lbl))
                        .findFirst().ifPresent(p -> {
                            selectedPack = p;
                            refreshPackInfo(p.getIdPack());
                        });
            });
        } catch (Exception ignored) {}

        // Utilisateurs
        try {
            for (UserApp u : userService.getAll()) {
                Integer id = extractUserId(u);
                if (id != null) {
                    userById.put(id, u);
                    userLabelById.put(id, buildUserLabel(u));
                }
            }
            cbUser.setItems(FXCollections.observableArrayList(
                    userLabelById.values().stream().toList()));
            cbUser.setOnAction(e -> {
                int uid = getUserIdFromLabel(cbUser.getValue());
                refreshUserInfo(uid);
            });
        } catch (Exception ignored) {}
    }

    private void refreshUserInfo(int userId) {
        UserApp u = userById.get(userId);
        lblNomUser.setText(u != null ? buildUserLabel(u) : "—");
    }

    private void refreshPackInfo(int packId) {
        Pack p = packById.get(packId);
        if (p == null) { lblNomPack.setText("—"); return; }
        lblNomPack.setText(safe(p.getNom()));
        lblPrixBase.setText(p.getPrixBase()  != null ? p.getPrixBase().toPlainString()  + " DT" : "— DT");
        lblDiscount.setText(p.getReduction() != null ? p.getReduction().toPlainString() + " %"  : "0 %");

        // Montant indicatif (le service recalculera avec loyalty)
        try {
            BigDecimal prix = p.getPrixBase()  != null ? p.getPrixBase()  : BigDecimal.ZERO;
            BigDecimal red  = p.getReduction() != null ? p.getReduction() : BigDecimal.ZERO;
            BigDecimal total = prix.multiply(BigDecimal.ONE.subtract(
                    red.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));
            txtMontant.setText(total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRES LOOKUPS
    // ════════════════════════════════════════════════════════

    private int getUserIdFromLabel(String label) {
        if (label == null) return -1;
        return userLabelById.entrySet().stream()
                .filter(e -> e.getValue().equals(label))
                .mapToInt(Map.Entry::getKey).findFirst().orElse(-1);
    }

    private Integer extractUserId(UserApp u) {
        Integer id = tryIntGetter(u, "getIdUser");
        if (id != null) return id;
        id = tryIntGetter(u, "getIdUtilisateur");
        if (id != null) return id;
        return tryIntGetter(u, "getId");
    }

    private Integer tryIntGetter(Object obj, String name) {
        try {
            Method m = obj.getClass().getMethod(name);
            Object v = m.invoke(obj);
            if (v == null) return null;
            if (v instanceof Integer i) return i;
            if (v instanceof Number n)  return n.intValue();
            return Integer.parseInt(v.toString());
        } catch (Exception e) { return null; }
    }

    private String buildUserLabel(UserApp u) {
        String prenom = tryStringGetter(u, "getPrenom");
        String nom    = tryStringGetter(u, "getNom");
        String email  = tryStringGetter(u, "getEmail");
        String full   = (safe(prenom) + " " + safe(nom)).trim();
        if (!full.isBlank()) return full;
        if (email != null && !email.isBlank()) return email;
        return String.valueOf(u);
    }

    private String tryStringGetter(Object obj, String name) {
        try {
            Method m = obj.getClass().getMethod(name);
            Object v = m.invoke(obj);
            return v == null ? null : v.toString();
        } catch (Exception e) { return null; }
    }

    private LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime ldt)       return ldt;
        if (o instanceof java.sql.Timestamp ts)   return ts.toLocalDateTime();
        return null;
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showError(Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText("Une erreur est survenue");
        a.setContentText(e.getMessage()); a.showAndWait();
    }
}