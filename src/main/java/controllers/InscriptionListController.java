package controllers;

import Entities.Inscription;
import Entities.Pack;
import Entities.UserApp;
import Services.InscriptionService;
import Services.PackService;
import Services.UserService;
import Services.WhatsAppService;
import Services.EcoWhatsAppTemplates;
import Utiles.CaptchaDialog;
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

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class InscriptionListController {

    @FXML private VBox       rowsBox;
    @FXML private TextField  txtSearch;
    @FXML private Label      lblTotal;

    private final InscriptionService service     = new InscriptionService();
    private final UserService        userService = new UserService();
    private final PackService        packService = new PackService();
    private final WhatsAppService    wa          = new WhatsAppService();

    private final ObservableList<Inscription> master = FXCollections.observableArrayList();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // sélection
    private Inscription selectedInscription = null;
    private HBox        selectedRow         = null;
    private String      selectedRowBaseStyle = "";

    // lookups
    private final Map<Integer, String>  userLabelById = new HashMap<>();
    private final Map<Integer, String>  packLabelById = new HashMap<>();
    private final Map<Integer, UserApp> userById      = new HashMap<>();
    private final Map<Integer, Pack>    packById      = new HashMap<>();

    // ─── Palette lignes (cohérente avec PackListController) ───
    private static final String BG_EVEN     = "-fx-background-color: white;";
    private static final String BG_ODD      = "-fx-background-color: #f8fafc;";
    private static final String BG_HOVER    = "-fx-background-color: #eff6ff;";
    private static final String BG_SELECTED = "-fx-background-color: #dbeafe; "
            + "-fx-border-color: #3b82f6 transparent transparent transparent; "
            + "-fx-border-width: 1;";
    private static final String ROW_BORDER  = "-fx-border-color: transparent transparent #e2e8f0 transparent; "
            + "-fx-border-width: 1;";

    // largeurs colonnes — DOIVENT correspondre au FXML header
    private static final double W_USER    = 200;
    private static final double W_PACK    = 200;
    private static final double W_DATE    = 150;
    private static final double W_MONTANT = 120;
    private static final double W_STATUT  = 140;
    private static final double W_GW      = 110;
    private static final double W_PAYST   = 110;

    @FXML
    private void initialize() {
        txtSearch.textProperty().addListener((obs, o, n) -> render());
        refresh();
    }

    @FXML private void onAdd()     { openForm(null); }
    @FXML private void onRefresh() { refresh(); }

    @FXML
    private void onEdit() {
        if (selectedInscription == null) {
            showInfo("Sélection requise", "Veuillez sélectionner une inscription à modifier.");
            return;
        }
        openForm(selectedInscription);
    }

    @FXML
    private void onDelete() {
        if (selectedInscription == null) {
            showInfo("Sélection requise", "Veuillez sélectionner une inscription à supprimer.");
            return;
        }
        String user = getUserLabel(selectedInscription.getIdUser());
        String pack = getPackLabel(selectedInscription.getIdPack());

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer cette inscription ?");
        confirm.setContentText("Utilisateur: " + user + "\nPack: " + pack
                + "\nMontant: " + selectedInscription.getMontantTotal() + " DT");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        boolean okCaptcha = CaptchaDialog.confirmDeletion(
                "Captcha de suppression",
                "Pour confirmer la suppression, tape le code affiché.");
        if (!okCaptcha) return;

        try {
            service.delete(selectedInscription.getIdInscription());
            refresh();
        } catch (Exception e) { showError(e); }
    }

    @FXML
    private void onWhatsApp() {
        if (selectedInscription == null) {
            showInfo("Sélection requise", "Sélectionne une inscription puis clique WhatsApp.");
            return;
        }
        if (!wa.isConfigured()) {
            showInfo("WhatsApp non configuré",
                    "Ajoute les variables d'environnement:\nWHATSAPP_TOKEN\nWHATSAPP_PHONE_NUMBER_ID");
            return;
        }
        try {
            int uid = selectedInscription.getIdUser();
            int pid = selectedInscription.getIdPack();
            UserApp user = userById.get(uid);
            Pack pack    = packById.get(pid);

            ChoiceDialog<EcoWhatsAppTemplates.Type> dlgType =
                    new ChoiceDialog<>(EcoWhatsAppTemplates.Type.UPDATE, EcoWhatsAppTemplates.Type.values());
            dlgType.setTitle("WhatsApp");
            dlgType.setHeaderText("Type de message WhatsApp");
            dlgType.setContentText("Choisir :");
            Optional<EcoWhatsAppTemplates.Type> typeOpt = dlgType.showAndWait();
            if (typeOpt.isEmpty()) return;

            String msg   = EcoWhatsAppTemplates.build(typeOpt.get(), user, pack, selectedInscription);
            String phone = guessPhoneFromUser(user);
            if (phone == null || phone.isBlank() || !phone.trim().startsWith("+")) {
                TextInputDialog d = new TextInputDialog(phone == null ? "+216" : phone);
                d.setTitle("Numéro WhatsApp");
                d.setHeaderText("Entrer le numéro WhatsApp (format E.164)");
                d.setContentText("Ex: +216XXXXXXXX");
                Optional<String> in = d.showAndWait();
                if (in.isEmpty()) return;
                phone = in.get().trim();
            }

            WhatsAppService.SendResult res = wa.sendTextMessage(phone, msg);
            if (res.ok) showInfo("WhatsApp envoyé ✅", "Message envoyé (HTTP " + res.statusCode + ").");
            else        showInfo("Erreur WhatsApp ❌", "HTTP " + res.statusCode + "\n" + res.responseBody);
        } catch (Exception e) { showError(e); }
    }

    private void refresh() {
        try {
            preloadLookups();
            master.setAll(service.getAll());
            clearSelection();
            render();
        } catch (Exception e) { showError(e); }
    }

    // ═══════════════════════════════════════════════════════
    //  RENDU DU TABLEAU
    // ═══════════════════════════════════════════════════════

    private void render() {
        rowsBox.getChildren().clear();

        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        List<Inscription> visible = new ArrayList<>();
        for (Inscription i : master)
            if (q.isEmpty() || matchInscription(i, q)) visible.add(i);

        // tri : plus récent d'abord
        visible.sort((a, b) -> {
            LocalDateTime da = toLocalDateTime(a.getDateInscription());
            LocalDateTime db = toLocalDateTime(b.getDateInscription());
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        int idx = 0;
        for (Inscription i : visible) {
            rowsBox.getChildren().add(buildRow(i, idx++));
        }

        lblTotal.setText("Affiché / Total : " + visible.size() + " / " + master.size());
    }

    private boolean matchInscription(Inscription i, String q) {
        String u  = (i.getNomUser() != null && !i.getNomUser().isBlank())
                ? i.getNomUser().toLowerCase() : getUserLabel(i.getIdUser()).toLowerCase();
        String p  = (i.getNomPack() != null && !i.getNomPack().isBlank())
                ? i.getNomPack().toLowerCase() : getPackLabel(i.getIdPack()).toLowerCase();
        String st = i.getStatutInscr()    == null ? "" : i.getStatutInscr().toLowerCase();
        String dt = formatDate(i.getDateInscription()).toLowerCase();
        String mt = String.valueOf(i.getMontantTotal()).toLowerCase();
        String gw = i.getPaymentGateway() == null ? "" : i.getPaymentGateway().toLowerCase();
        String ps = i.getPaymentStatus()  == null ? "" : i.getPaymentStatus().toLowerCase();
        return u.contains(q) || p.contains(q) || st.contains(q)
                || dt.contains(q) || mt.contains(q) || gw.contains(q) || ps.contains(q);
    }

    // ═══════════════════════════════════════════════════════
    //  CONSTRUCTION D'UNE LIGNE — styles 100 % inline
    // ═══════════════════════════════════════════════════════

    private HBox buildRow(Inscription i, int index) {

        String userTxt = (i.getNomUser() != null && !i.getNomUser().isBlank())
                ? i.getNomUser() : getUserLabel(i.getIdUser());
        String packTxt = (i.getNomPack() != null && !i.getNomPack().isBlank())
                ? i.getNomPack() : getPackLabel(i.getIdPack());

        Label lUser    = cell(userTxt,                                            W_USER);
        Label lPack    = cell(packTxt,                                            W_PACK);
        Label lDate    = cell(formatDate(i.getDateInscription()),                 W_DATE);
        Label lMontant = cell(i.getMontantTotal() + " DT",                       W_MONTANT);

        // Badge statut inscription
        String stTxt = i.getStatutInscr() == null ? "" : i.getStatutInscr();
        Label lStatut = badgeStatutInscription(stTxt, W_STATUT);

        Label lGateway = cell(i.getPaymentGateway() == null ? "—" : i.getPaymentGateway(), W_GW);

        // Badge statut paiement
        String psTxt = i.getPaymentStatus() == null ? "—" : i.getPaymentStatus();
        Label lPaySt   = badgePaymentStatus(psTxt, W_PAYST);

        HBox row = new HBox(lUser, lPack, lDate, lMontant, lStatut, lGateway, lPaySt);
        row.setSpacing(0);

        String baseStyle = (index % 2 == 0 ? BG_EVEN : BG_ODD) + ROW_BORDER;
        row.setStyle(baseStyle);

        // Hover
        row.setOnMouseEntered(e -> {
            if (row != selectedRow) row.setStyle(BG_HOVER + ROW_BORDER);
        });
        row.setOnMouseExited(e -> {
            if (row != selectedRow) row.setStyle(baseStyle);
        });

        // Clic / double-clic
        row.setOnMouseClicked(e -> {
            selectRow(row, i, baseStyle);
            if (e.getClickCount() == 2) onEdit();
        });

        return row;
    }

    /** Cellule texte standard — fond transparent, texte sombre */
    private Label cell(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 13; "
                + "-fx-padding: 10 14 10 8; -fx-background-color: transparent;");
        return l;
    }

    /** Badge coloré pour statut_inscr */
    private Label badgeStatutInscription(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        String color;
        switch (text.toUpperCase()) {
            case "CONFIRMEE"  -> color = "-fx-background-color: #dcfce7; -fx-text-fill: #166534;";
            case "EN_ATTENTE" -> color = "-fx-background-color: #fef9c3; -fx-text-fill: #92400e;";
            case "ANNULEE"    -> color = "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            default           -> color = "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;";
        }
        l.setStyle(color + " -fx-background-radius: 20; -fx-padding: 3 10 3 10; "
                + "-fx-font-size: 11; -fx-font-weight: bold;");
        return l;
    }

    /** Badge coloré pour payment_status */
    private Label badgePaymentStatus(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        String color;
        switch (text.toLowerCase()) {
            case "paid"     -> color = "-fx-background-color: #dcfce7; -fx-text-fill: #166534;";
            case "initiated"-> color = "-fx-background-color: #fef9c3; -fx-text-fill: #92400e;";
            case "failed"   -> color = "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            case "refunded" -> color = "-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3;";
            default         -> color = "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;";
        }
        l.setStyle(color + " -fx-background-radius: 20; -fx-padding: 3 10 3 10; "
                + "-fx-font-size: 11; -fx-font-weight: bold;");
        return l;
    }

    // ═══════════════════════════════════════════════════════
    //  SÉLECTION
    // ═══════════════════════════════════════════════════════

    private void selectRow(HBox row, Inscription i, String baseStyle) {
        if (selectedRow != null) selectedRow.setStyle(selectedRowBaseStyle);
        selectedRow          = row;
        selectedInscription  = i;
        selectedRowBaseStyle = baseStyle;
        row.setStyle(BG_SELECTED);
    }

    private void clearSelection() {
        if (selectedRow != null) selectedRow.setStyle(selectedRowBaseStyle);
        selectedInscription  = null;
        selectedRow          = null;
        selectedRowBaseStyle = "";
    }

    // ═══════════════════════════════════════════════════════
    //  LOOKUPS user / pack
    // ═══════════════════════════════════════════════════════

    private void preloadLookups() {
        userLabelById.clear(); packLabelById.clear();
        userById.clear();      packById.clear();
        try {
            for (Pack p : packService.getAll()) {
                packById.put(p.getIdPack(), p);
                packLabelById.put(p.getIdPack(), safe(p.getNom()));
            }
        } catch (Exception ignored) {}
        try {
            for (UserApp u : userService.getAll()) {
                Integer id = extractUserId(u);
                if (id != null) {
                    userById.put(id, u);
                    userLabelById.put(id, buildUserLabel(u));
                }
            }
        } catch (Exception ignored) {}
    }

    private String getPackLabel(int idPack) { return packLabelById.getOrDefault(idPack, "Pack #" + idPack); }
    private String getUserLabel(int idUser) { return userLabelById.getOrDefault(idUser, "User #" + idUser); }

    private Integer extractUserId(UserApp u) {
        Integer id = tryIntGetter(u, "getIdUser");
        if (id != null) return id;
        id = tryIntGetter(u, "getIdUtilisateur");
        if (id != null) return id;
        return tryIntGetter(u, "getId");
    }

    private Integer tryIntGetter(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            if (v == null) return null;
            if (v instanceof Integer ii) return ii;
            if (v instanceof Number n)   return n.intValue();
            return Integer.parseInt(v.toString());
        } catch (Exception e) { return null; }
    }

    private String buildUserLabel(UserApp u) {
        String prenom   = tryStringGetter(u, "getPrenom");
        String nom      = tryStringGetter(u, "getNom");
        String email    = tryStringGetter(u, "getEmail");
        String username = tryStringGetter(u, "getUsername");
        String full = (safe(prenom) + " " + safe(nom)).trim();
        if (!full.isBlank()) return full;
        if (email    != null && !email.isBlank())    return email;
        if (username != null && !username.isBlank()) return username;
        return String.valueOf(u);
    }

    private String tryStringGetter(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            return v == null ? null : v.toString();
        } catch (Exception e) { return null; }
    }

    private static String guessPhoneFromUser(UserApp u) {
        if (u == null) return null;
        try { return String.valueOf(u.getClass().getMethod("getTelephone").invoke(u)); } catch (Exception ignored) {}
        try { return String.valueOf(u.getClass().getMethod("getTel").invoke(u)); }       catch (Exception ignored) {}
        try { return String.valueOf(u.getClass().getMethod("getPhone").invoke(u)); }     catch (Exception ignored) {}
        return null;
    }

    // ═══════════════════════════════════════════════════════
    //  FORMULAIRE + DATES
    // ═══════════════════════════════════════════════════════

    private void openForm(Inscription inscription) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/InscriptionForm.fxml"));
            Scene scene = new Scene(loader.load());
            try { scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm()); }
            catch (Exception ignored) {}
            InscriptionFormController controller = loader.getController();
            controller.setInscription(inscription);
            controller.setOnSaved(this::refresh);
            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle(inscription == null ? "Ajouter une inscription" : "Modifier une inscription");
            st.setScene(scene);
            st.showAndWait();
        } catch (Exception e) { showError(e); }
    }

    private String formatDate(Object dateObj) {
        LocalDateTime ldt = toLocalDateTime(dateObj);
        if (ldt == null) return dateObj == null ? "" : String.valueOf(dateObj);
        return ldt.format(fmt);
    }

    private LocalDateTime toLocalDateTime(Object dateObj) {
        if (dateObj == null) return null;
        try {
            if (dateObj instanceof LocalDateTime ldt)   return ldt;
            if (dateObj instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
            return null;
        } catch (Exception e) { return null; }
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

    // ═══════════════════════════════════════════════════════
    //  NAVIGATION (conservée pour compatibilité)
    // ═══════════════════════════════════════════════════════

    @FXML private void onDashboard()    { navigateSafe("/fxml/Dashboard.fxml"); }
    @FXML private void onPacks()        { navigateSafe("/fxml/PackList.fxml"); }
    @FXML private void onInscriptions() { onRefresh(); }
    @FXML private void onRetour()       { navigateSafe("/Menu.fxml"); }

    private void navigateSafe(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) lblTotal.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { showError(e); }
    }
}