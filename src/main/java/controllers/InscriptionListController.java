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

    @FXML private VBox rowsBox;
    @FXML private TextField txtSearch;
    @FXML private Label lblTotal;

    private final InscriptionService service = new InscriptionService();
    private final UserService userService = new UserService();
    private final PackService packService = new PackService();

    private final WhatsAppService wa = new WhatsAppService();

    private final ObservableList<Inscription> master = FXCollections.observableArrayList();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // selection
    private Inscription selectedInscription = null;
    private HBox selectedRow = null;

    // lookups (no IDs displayed)
    private final Map<Integer, String> userLabelById = new HashMap<>();
    private final Map<Integer, String> packLabelById = new HashMap<>();

    // for WhatsApp context
    private final Map<Integer, UserApp> userById = new HashMap<>();
    private final Map<Integer, Pack> packById = new HashMap<>();

    @FXML
    private void initialize() {
        txtSearch.textProperty().addListener((obs, o, n) -> render());
        refresh();
    }

    @FXML
    private void onAdd() { openForm(null); }

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
        confirm.setContentText(
                "Utilisateur: " + user +
                        "\nPack: " + pack +
                        "\nMontant: " + selectedInscription.getMontantTotal() + " DT"
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        // ✅ CAPTCHA step
        boolean okCaptcha = CaptchaDialog.confirmDeletion(
                "Captcha de suppression",
                "Pour confirmer la suppression, tape le code affiché."
        );
        if (!okCaptcha) return;

        try {
            service.delete(selectedInscription.getIdInscription());
            refresh();
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void onRefresh() { refresh(); }

    // ✅ New: WhatsApp manual send from list
    @FXML
    private void onWhatsApp() {
        if (selectedInscription == null) {
            showInfo("Sélection requise", "Sélectionne une inscription puis clique WhatsApp.");
            return;
        }

        if (!wa.isConfigured()) {
            showInfo("WhatsApp non configuré",
                    "Ajoute les variables d'environnement:\nWHATSAPP_TOKEN\nWHATSAPP_PHONE_NUMBER_ID\n(WHATSAPP_API_VERSION optionnel)");
            return;
        }

        try {
            int uid = selectedInscription.getIdUser();
            int pid = selectedInscription.getIdPack();

            UserApp user = userById.get(uid);
            Pack pack = packById.get(pid);

            // choose template type
            ChoiceDialog<EcoWhatsAppTemplates.Type> dlgType =
                    new ChoiceDialog<>(EcoWhatsAppTemplates.Type.UPDATE, EcoWhatsAppTemplates.Type.values());
            dlgType.setTitle("WhatsApp");
            dlgType.setHeaderText("Type de message WhatsApp");
            dlgType.setContentText("Choisir :");
            Optional<EcoWhatsAppTemplates.Type> typeOpt = dlgType.showAndWait();
            if (typeOpt.isEmpty()) return;

            EcoWhatsAppTemplates.Type type = typeOpt.get();
            String msg = EcoWhatsAppTemplates.build(type, user, pack, selectedInscription);

            // phone
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

            if (res.ok) {
                showInfo("WhatsApp envoyé ✅", "Message envoyé (HTTP " + res.statusCode + ").");
            } else {
                showInfo("Erreur WhatsApp ❌", "HTTP " + res.statusCode + "\n" + res.responseBody);
            }

        } catch (Exception e) {
            showError(e);
        }
    }

    private void refresh() {
        try {
            preloadLookups();
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

        List<Inscription> visible = new ArrayList<>();
        for (Inscription i : master) {
            if (q.isEmpty() || matchInscription(i, q)) visible.add(i);
        }

        // tri: plus récent d'abord
        visible.sort((a, b) -> {
            LocalDateTime da = toLocalDateTime(a.getDateInscription());
            LocalDateTime db = toLocalDateTime(b.getDateInscription());
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        for (Inscription i : visible) {
            rowsBox.getChildren().add(buildRow(i));
        }

        lblTotal.setText("Affiché / Total : " + visible.size() + " / " + master.size());
    }

    private boolean matchInscription(Inscription i, String q) {
        String u = getUserLabel(i.getIdUser()).toLowerCase();
        String p = getPackLabel(i.getIdPack()).toLowerCase();
        String st = i.getStatutInscr() == null ? "" : i.getStatutInscr().toLowerCase();
        String dt = formatDate(i.getDateInscription()).toLowerCase();
        String mt = String.valueOf(i.getMontantTotal()).toLowerCase();
        return u.contains(q) || p.contains(q) || st.contains(q) || dt.contains(q) || mt.contains(q);
    }

    private HBox buildRow(Inscription i) {
        Label lUser = cell(getUserLabel(i.getIdUser()), 240);
        Label lPack = cell(getPackLabel(i.getIdPack()), 240);
        Label lDate = cell(formatDate(i.getDateInscription()), 160);
        Label lMontant = cell(i.getMontantTotal() + " DT", 120);
        Label lStatut = cell(i.getStatutInscr() == null ? "" : i.getStatutInscr(), 160);

        HBox row = new HBox(lUser, lPack, lDate, lMontant, lStatut);
        row.setSpacing(0);

        // ✅ CSS handles hover + base style (#rowsBox > .hbox ...)
        row.setOnMouseClicked(e -> {
            selectRow(row, i);
            if (e.getClickCount() == 2) onEdit();
        });

        return row;
    }

    private void selectRow(HBox row, Inscription i) {
        if (selectedRow != null) {
            selectedRow.getStyleClass().remove("row-selected");
        }
        selectedRow = row;
        selectedInscription = i;
        row.getStyleClass().add("row-selected");
    }

    private void clearSelection() {
        selectedInscription = null;
        selectedRow = null;
    }

    private Label cell(String text, double w) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        return l;
    }

    // =========================
    // Lookups user/pack (labels)
    // =========================
    private void preloadLookups() {
        userLabelById.clear();
        packLabelById.clear();
        userById.clear();
        packById.clear();

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

    private String getPackLabel(int idPack) { return packLabelById.getOrDefault(idPack, "Pack"); }
    private String getUserLabel(int idUser) { return userLabelById.getOrDefault(idUser, "Utilisateur"); }

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
            if (v instanceof Integer i) return i;
            if (v instanceof Number n) return n.intValue();
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String buildUserLabel(UserApp u) {
        String prenom = tryStringGetter(u, "getPrenom");
        String nom = tryStringGetter(u, "getNom");
        String email = tryStringGetter(u, "getEmail");
        String username = tryStringGetter(u, "getUsername");

        String full = (safe(prenom) + " " + safe(nom)).trim();
        if (!full.isBlank()) return full;
        if (email != null && !email.isBlank()) return email;
        if (username != null && !username.isBlank()) return username;
        return String.valueOf(u);
    }

    private String tryStringGetter(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String guessPhoneFromUser(UserApp u) {
        if (u == null) return null;
        try { return String.valueOf(u.getClass().getMethod("getTelephone").invoke(u)); } catch (Exception ignored) {}
        try { return String.valueOf(u.getClass().getMethod("getTel").invoke(u)); } catch (Exception ignored) {}
        try { return String.valueOf(u.getClass().getMethod("getPhone").invoke(u)); } catch (Exception ignored) {}
        return null;
    }

    // =========================
    // Form dialog + date format
    // =========================
    private void openForm(Inscription inscription) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/InscriptionForm.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

            InscriptionFormController controller = loader.getController();
            controller.setInscription(inscription);
            controller.setOnSaved(this::refresh);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle(inscription == null ? "Ajouter une inscription" : "Modifier une inscription");
            st.setScene(scene);
            st.showAndWait();
        } catch (Exception e) {
            showError(e);
        }
    }

    private String formatDate(Object dateObj) {
        LocalDateTime ldt = toLocalDateTime(dateObj);
        if (ldt == null) return dateObj == null ? "" : String.valueOf(dateObj);
        return ldt.format(fmt);
    }

    private LocalDateTime toLocalDateTime(Object dateObj) {
        if (dateObj == null) return null;
        try {
            if (dateObj instanceof LocalDateTime ldt) return ldt;
            if (dateObj instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

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