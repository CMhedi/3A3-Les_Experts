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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class InscriptionListController {

    @FXML private FlowPane rowsBox;
    @FXML private TextField txtSearch;
    @FXML private Label lblTotal;
    @FXML private Label lblKpiTotal;
    @FXML private Label lblKpiAttente;
    @FXML private Label lblKpiConfirme;
    @FXML private Label lblKpiMontant;
    @FXML private VBox chartsHost;

    private PieChart insPieStatut;
    private PieChart insPiePayment;
    private BarChart<String, Number> insBarTopPacks;
    private LineChart<String, Number> insLineMonth;
    private BarChart<String, Number> insBarMontantStatut;
    private BarChart<String, Number> insBarGateway;

    private final InscriptionService service = new InscriptionService();
    private final UserService userService = new UserService();
    private final PackService packService = new PackService();
    private final WhatsAppService wa = new WhatsAppService();

    private final ObservableList<Inscription> master = FXCollections.observableArrayList();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Inscription selectedInscription = null;
    private VBox selectedCard = null;

    private final Map<Integer, String> userLabelById = new HashMap<>();
    private final Map<Integer, String> packLabelById = new HashMap<>();
    private final Map<Integer, UserApp> userById = new HashMap<>();
    private final Map<Integer, Pack> packById = new HashMap<>();

    @FXML
    private void initialize() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, o, n) -> render());
        }
        if (rowsBox != null) {
            rowsBox.setPrefWrapLength(1200);
        }
        setupInscriptionChartsUi();
        refresh();
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

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
        } catch (Exception e) {
            showError(e);
        }
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
            Pack pack = packById.get(pid);

            ChoiceDialog<EcoWhatsAppTemplates.Type> dlgType =
                    new ChoiceDialog<>(EcoWhatsAppTemplates.Type.UPDATE, EcoWhatsAppTemplates.Type.values());
            dlgType.setTitle("WhatsApp");
            dlgType.setHeaderText("Type de message WhatsApp");
            dlgType.setContentText("Choisir :");
            Optional<EcoWhatsAppTemplates.Type> typeOpt = dlgType.showAndWait();
            if (typeOpt.isEmpty()) return;

            String msg = EcoWhatsAppTemplates.build(typeOpt.get(), user, pack, selectedInscription);
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
            else showInfo("Erreur WhatsApp ❌", "HTTP " + res.statusCode + "\n" + res.responseBody);
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
            updateInscriptionChartsData();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void render() {
        if (rowsBox == null) return;
        rowsBox.getChildren().clear();

        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        List<Inscription> visible = new ArrayList<>();
        for (Inscription i : master) {
            if (q.isEmpty() || matchInscription(i, q)) visible.add(i);
        }

        visible.sort((a, b) -> {
            LocalDateTime da = toLocalDateTime(a.getDateInscription());
            LocalDateTime db = toLocalDateTime(b.getDateInscription());
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        updateKpis();

        for (Inscription i : visible) {
            rowsBox.getChildren().add(buildCard(i));
        }

        if (lblTotal != null) {
            lblTotal.setText("Affiché / Total : " + visible.size() + " / " + master.size());
        }
    }

    private void updateKpis() {
        int n = master.size();
        long att = master.stream().filter(x -> "EN_ATTENTE".equalsIgnoreCase(safe(x.getStatutInscr()))).count();
        long conf = master.stream().filter(x -> "CONFIRMEE".equalsIgnoreCase(safe(x.getStatutInscr()))).count();
        BigDecimal sum = master.stream()
                .map(Inscription::getMontantTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lblKpiTotal != null) lblKpiTotal.setText(String.valueOf(n));
        if (lblKpiAttente != null) lblKpiAttente.setText(String.valueOf(att));
        if (lblKpiConfirme != null) lblKpiConfirme.setText(String.valueOf(conf));
        if (lblKpiMontant != null) {
            lblKpiMontant.setText(sum.compareTo(BigDecimal.ZERO) == 0 ? "—"
                    : String.format(Locale.FRANCE, "%.0f", sum.setScale(0, RoundingMode.HALF_UP)));
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private boolean matchInscription(Inscription i, String q) {
        String u = (i.getNomUser() != null && !i.getNomUser().isBlank())
                ? i.getNomUser().toLowerCase() : getUserLabel(i.getIdUser()).toLowerCase();
        String p = (i.getNomPack() != null && !i.getNomPack().isBlank())
                ? i.getNomPack().toLowerCase() : getPackLabel(i.getIdPack()).toLowerCase();
        String st = i.getStatutInscr() == null ? "" : i.getStatutInscr().toLowerCase();
        String dt = formatDate(i.getDateInscription()).toLowerCase();
        String mt = String.valueOf(i.getMontantTotal()).toLowerCase();
        String gw = i.getPaymentGateway() == null ? "" : i.getPaymentGateway().toLowerCase();
        String ps = i.getPaymentStatus() == null ? "" : i.getPaymentStatus().toLowerCase();
        return u.contains(q) || p.contains(q) || st.contains(q)
                || dt.contains(q) || mt.contains(q) || gw.contains(q) || ps.contains(q);
    }

    private VBox buildCard(Inscription i) {
        VBox card = new VBox(8);
        card.getStyleClass().add("adi-card");

        String userTxt = (i.getNomUser() != null && !i.getNomUser().isBlank())
                ? i.getNomUser() : getUserLabel(i.getIdUser());
        String packTxt = (i.getNomPack() != null && !i.getNomPack().isBlank())
                ? i.getNomPack() : getPackLabel(i.getIdPack());

        Label lUser = new Label(userTxt);
        lUser.getStyleClass().add("adi-user");
        lUser.setWrapText(true);

        Label lPack = new Label("📦  " + packTxt);
        lPack.getStyleClass().add("adi-pack");

        Label lDate = new Label("🕐  " + formatDate(i.getDateInscription()));
        lDate.getStyleClass().add("adi-date");

        String mt = i.getMontantTotal() == null ? "—" : i.getMontantTotal().toPlainString() + " DT";
        Label lMontant = new Label(mt);
        lMontant.getStyleClass().add("adi-montant");

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        Label lStatut = badgeInscription(i.getStatutInscr());
        Label lPaySt = badgePayment(i.getPaymentStatus());
        badges.getChildren().addAll(lStatut, lPaySt);

        Label lGw = new Label("Passerelle : " + (i.getPaymentGateway() == null ? "—" : i.getPaymentGateway()));
        lGw.getStyleClass().add("adi-date");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        spacer.setMinHeight(4);

        card.getChildren().addAll(lUser, lPack, lDate, lMontant, badges, lGw, spacer);

        card.setOnMouseClicked(e -> {
            selectCard(card, i);
            if (e.getClickCount() == 2) onEdit();
        });

        return card;
    }

    private Label badgeInscription(String text) {
        Label l = new Label(text == null ? "" : text);
        String t = text == null ? "" : text.toUpperCase(Locale.ROOT);
        l.getStyleClass().clear();
        switch (t) {
            case "CONFIRMEE" -> l.getStyleClass().add("badge-inscr-conf");
            case "EN_ATTENTE" -> l.getStyleClass().add("badge-inscr-wait");
            case "ANNULEE" -> l.getStyleClass().add("badge-inscr-cancel");
            default -> l.getStyleClass().add("badge-inscr-default");
        }
        return l;
    }

    private Label badgePayment(String text) {
        String raw = text == null ? "—" : text;
        Label l = new Label(raw);
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        l.getStyleClass().clear();
        switch (t) {
            case "paid" -> l.getStyleClass().add("badge-pay-ok");
            case "initiated", "pending" -> l.getStyleClass().add("badge-pay-wait");
            case "failed", "refunded" -> l.getStyleClass().add("badge-pay-bad");
            default -> l.getStyleClass().add("badge-pay-default");
        }
        return l;
    }

    private void selectCard(VBox card, Inscription ins) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().removeAll("adi-card-selected");
        }
        selectedCard = card;
        selectedInscription = ins;
        card.getStyleClass().removeAll("adi-card-selected");
        card.getStyleClass().add("adi-card-selected");
    }

    private void clearSelection() {
        if (selectedCard != null) {
            selectedCard.getStyleClass().removeAll("adi-card-selected");
        }
        selectedInscription = null;
        selectedCard = null;
    }

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
        } catch (Exception ignored) {
        }
        try {
            for (UserApp u : userService.getAll()) {
                Integer id = extractUserId(u);
                if (id != null) {
                    userById.put(id, u);
                    userLabelById.put(id, buildUserLabel(u));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String getPackLabel(int idPack) {
        return packLabelById.getOrDefault(idPack, "Pack #" + idPack);
    }

    private String getUserLabel(int idUser) {
        return userLabelById.getOrDefault(idUser, "User #" + idUser);
    }

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
        try {
            return String.valueOf(u.getClass().getMethod("getTelephone").invoke(u));
        } catch (Exception ignored) {
        }
        try {
            return String.valueOf(u.getClass().getMethod("getTel").invoke(u));
        } catch (Exception ignored) {
        }
        try {
            return String.valueOf(u.getClass().getMethod("getPhone").invoke(u));
        } catch (Exception ignored) {
        }
        return null;
    }

    private void setupInscriptionChartsUi() {
        if (chartsHost == null) return;
        chartsHost.getChildren().clear();

        Label head = new Label("📊 Analytiques (toutes les inscriptions en base)");
        head.getStyleClass().add("adi-charts-head");

        insPieStatut = new PieChart();
        insPieStatut.setTitle("Statuts d'inscription");
        insPieStatut.setLabelsVisible(true);
        insStyleChart(insPieStatut);

        insPiePayment = new PieChart();
        insPiePayment.setTitle("Statuts / passerelles de paiement");
        insPiePayment.setLabelsVisible(true);
        insStyleChart(insPiePayment);

        insBarTopPacks = insNewBar("Packs les plus réservés (volume)", "Pack", "Inscriptions");
        CategoryAxis axM = new CategoryAxis();
        axM.setLabel("Mois");
        NumberAxis ayM = new NumberAxis();
        ayM.setLabel("Nombre");
        insLineMonth = new LineChart<>(axM, ayM);
        insLineMonth.setTitle("Inscriptions sur 12 mois");
        insLineMonth.setLegendVisible(false);
        insLineMonth.setCreateSymbols(true);
        insStyleChart(insLineMonth);

        insBarMontantStatut = insNewBar("Montant total (DT) par statut d'inscription", "Statut", "Montant (DT)");
        insBarGateway = insNewBar("Répartition par passerelle", "Passerelle", "Nombre");

        HBox r1 = new HBox(14, insPieStatut, insPiePayment);
        r1.getStyleClass().add("adi-chart-row");
        HBox.setHgrow(insPieStatut, Priority.ALWAYS);
        HBox.setHgrow(insPiePayment, Priority.ALWAYS);

        HBox r2 = new HBox(14, insBarTopPacks, insLineMonth);
        r2.getStyleClass().add("adi-chart-row");
        HBox.setHgrow(insBarTopPacks, Priority.ALWAYS);
        HBox.setHgrow(insLineMonth, Priority.ALWAYS);

        HBox r3 = new HBox(14, insBarMontantStatut, insBarGateway);
        r3.getStyleClass().add("adi-chart-row");
        HBox.setHgrow(insBarMontantStatut, Priority.ALWAYS);
        HBox.setHgrow(insBarGateway, Priority.ALWAYS);

        chartsHost.getChildren().addAll(head, r1, r2, r3);
    }

    private BarChart<String, Number> insNewBar(String title, String xLabel, String yLabel) {
        CategoryAxis x = new CategoryAxis();
        x.setLabel(xLabel);
        NumberAxis y = new NumberAxis();
        y.setLabel(yLabel);
        BarChart<String, Number> b = new BarChart<>(x, y);
        b.setTitle(title);
        b.setLegendVisible(false);
        b.setAnimated(false);
        insStyleChart(b);
        return b;
    }

    private void insStyleChart(Chart c) {
        c.getStyleClass().add("adi-chart");
        c.setMinHeight(260);
        c.setPrefHeight(280);
    }

    private void updateInscriptionChartsData() {
        if (chartsHost == null || insPieStatut == null) return;

        List<Inscription> data = new ArrayList<>(master);

        Map<String, Long> stCount = new LinkedHashMap<>();
        for (Inscription i : data) {
            String k = safe(i.getStatutInscr());
            if (k.isBlank()) k = "Non renseigné";
            stCount.merge(k, 1L, Long::sum);
        }
        ObservableList<PieChart.Data> pieSt = FXCollections.observableArrayList();
        for (var e : stCount.entrySet()) {
            if (e.getValue() > 0) pieSt.add(new PieChart.Data(e.getKey(), e.getValue()));
        }
        if (pieSt.isEmpty()) pieSt.add(new PieChart.Data("Aucune donnée", 1));
        insPieStatut.setData(pieSt);

        Map<String, Long> payCount = new LinkedHashMap<>();
        for (Inscription i : data) {
            String ps = i.getPaymentStatus();
            String gw = i.getPaymentGateway();
            String k = (ps != null && !ps.isBlank()) ? ps : (gw != null && !gw.isBlank() ? gw : "—");
            payCount.merge(k, 1L, Long::sum);
        }
        ObservableList<PieChart.Data> piePay = FXCollections.observableArrayList();
        for (var e : payCount.entrySet()) {
            if (e.getValue() > 0) piePay.add(new PieChart.Data(e.getKey(), e.getValue()));
        }
        if (piePay.isEmpty()) piePay.add(new PieChart.Data("Aucune donnée", 1));
        insPiePayment.setData(piePay);

        Map<Integer, Long> byPackId = new HashMap<>();
        for (Inscription i : data) {
            byPackId.merge(i.getIdPack(), 1L, Long::sum);
        }
        List<Map.Entry<Integer, Long>> packEntries = new ArrayList<>(byPackId.entrySet());
        packEntries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        XYChart.Series<String, Number> sTop = new XYChart.Series<>();
        sTop.setName("Volume");
        int limit = Math.min(10, packEntries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<Integer, Long> e = packEntries.get(i);
            sTop.getData().add(new XYChart.Data<>(
                    trunc(getPackLabel(e.getKey()), 18),
                    e.getValue()));
        }
        if (sTop.getData().isEmpty()) {
            sTop.getData().add(new XYChart.Data<>("—", 0));
        }
        insBarTopPacks.getData().clear();
        insBarTopPacks.getData().add(sTop);

        XYChart.Series<String, Number> sMonth = new XYChart.Series<>();
        sMonth.setName("Inscriptions");
        YearMonth end = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = end.minusMonths(i);
            String key = ym.getYear() + "-" + String.format(Locale.FRANCE, "%02d", ym.getMonthValue());
            long cnt = data.stream()
                    .map(this::toLocalDateTime)
                    .filter(Objects::nonNull)
                    .filter(dt -> YearMonth.from(dt).equals(ym))
                    .count();
            sMonth.getData().add(new XYChart.Data<>(key, cnt));
        }
        insLineMonth.getData().clear();
        insLineMonth.getData().add(sMonth);

        Map<String, BigDecimal> sumByStat = new HashMap<>();
        for (Inscription i : data) {
            String k = safe(i.getStatutInscr());
            if (k.isBlank()) k = "—";
            BigDecimal m = i.getMontantTotal() == null ? BigDecimal.ZERO : i.getMontantTotal();
            sumByStat.merge(k, m, BigDecimal::add);
        }
        XYChart.Series<String, Number> sMont = new XYChart.Series<>();
        for (var e : sumByStat.entrySet()) {
            sMont.getData().add(new XYChart.Data<>(e.getKey(), e.getValue().doubleValue()));
        }
        if (sMont.getData().isEmpty()) {
            sMont.getData().add(new XYChart.Data<>("—", 0));
        }
        insBarMontantStatut.getData().clear();
        insBarMontantStatut.getData().add(sMont);

        Map<String, Long> gwCount = new LinkedHashMap<>();
        for (Inscription i : data) {
            String g = i.getPaymentGateway();
            String k = (g == null || g.isBlank()) ? "—" : g;
            gwCount.merge(k, 1L, Long::sum);
        }
        XYChart.Series<String, Number> sGw = new XYChart.Series<>();
        for (var e : gwCount.entrySet()) {
            sGw.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        if (sGw.getData().isEmpty()) {
            sGw.getData().add(new XYChart.Data<>("—", 0));
        }
        insBarGateway.getData().clear();
        insBarGateway.getData().add(sGw);
    }

    private LocalDateTime toLocalDateTime(Inscription i) {
        return toLocalDateTime((Object) i.getDateInscription());
    }

    private static String trunc(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(1, max - 1)) + "…";
    }

    private void openForm(Inscription inscription) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/InscriptionForm.fxml"));
            Scene scene = new Scene(loader.load());
            try {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            } catch (Exception ignored) {
            }
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

    @FXML
    private void onDashboard() {
        navigateSafe("/fxml/Dashboard.fxml");
    }

    @FXML
    private void onPacks() {
        navigateSafe("/fxml/PackList.fxml");
    }

    @FXML
    private void onInscriptions() {
        onRefresh();
    }

    @FXML
    private void onRetour() {
        navigateSafe("/Menu.fxml");
    }

    private void navigateSafe(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) lblTotal.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError(e);
        }
    }
}
