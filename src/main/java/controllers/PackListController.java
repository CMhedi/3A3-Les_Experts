package controllers;

import Entities.Pack;
import Services.PackService;
import enums.StatutPack;
import enums.TypePack;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PackListController {

    @FXML private FlowPane rowsBox;
    @FXML private VBox chartsHost;
    @FXML private TextField txtSearch;
    @FXML private Label lblTotal;
    @FXML private Label lblActifs;
    @FXML private Label lblInactifs;
    @FXML private Label lblKpiPacks;
    @FXML private Label lblKpiActifs;
    @FXML private Label lblKpiPrixMoyen;
    @FXML private Label lblFooter;

    private final PackService service = new PackService();
    private final ObservableList<Pack> master = FXCollections.observableArrayList();

    private Pack selectedPack = null;
    private VBox selectedCard = null;

    private PieChart chartStatut;
    private BarChart<String, Number> chartTypes;
    private BarChart<String, Number> chartPrixTop;
    private BarChart<String, Number> chartActHist;
    private BarChart<String, Number> chartRedHist;
    private BarChart<String, Number> chartPrixBuckets;

    @FXML
    private void initialize() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, o, n) -> render());
        }
        if (rowsBox != null) {
            rowsBox.setPrefWrapLength(1180);
        }
        setupPackChartsUi();
        Platform.runLater(this::playPageEntrance);
        refresh();
    }

    private void setupPackChartsUi() {
        if (chartsHost == null) return;
        chartsHost.getChildren().clear();

        Label head = new Label("📊 Analytiques catalogue (tous les packs)");
        head.getStyleClass().add("adb-charts-head");

        chartStatut = new PieChart();
        chartStatut.setTitle("Répartition des statuts");
        chartStatut.setLabelsVisible(true);
        chartStatut.setLegendVisible(true);
        styleChart(chartStatut);

        chartTypes = newBar("Packs par type de formule", "Type", "Nombre");
        chartPrixTop = newBar("Prix de base (DT) — packs les plus chers", "Pack", "Prix (DT)");
        chartActHist = newBar("Capacité max (activités) — histogramme", "Tranche", "Nombre");
        chartRedHist = newBar("Réduction (%) — histogramme", "Tranche", "Nombre");
        chartPrixBuckets = newBar("Prix de base — tranches (DT)", "Tranche (DT)", "Nombre");

        HBox r1 = new HBox(14, chartStatut, chartTypes);
        r1.getStyleClass().add("adb-chart-row");
        HBox.setHgrow(chartStatut, Priority.ALWAYS);
        HBox.setHgrow(chartTypes, Priority.ALWAYS);

        HBox r2 = new HBox(14, chartPrixTop, chartActHist);
        r2.getStyleClass().add("adb-chart-row");
        HBox.setHgrow(chartPrixTop, Priority.ALWAYS);
        HBox.setHgrow(chartActHist, Priority.ALWAYS);

        HBox r3 = new HBox(14, chartRedHist, chartPrixBuckets);
        r3.getStyleClass().add("adb-chart-row");
        HBox.setHgrow(chartRedHist, Priority.ALWAYS);
        HBox.setHgrow(chartPrixBuckets, Priority.ALWAYS);

        chartsHost.getChildren().addAll(head, r1, r2, r3);
    }

    private BarChart<String, Number> newBar(String title, String xLabel, String yLabel) {
        CategoryAxis x = new CategoryAxis();
        x.setLabel(xLabel);
        NumberAxis y = new NumberAxis();
        y.setLabel(yLabel);
        BarChart<String, Number> b = new BarChart<>(x, y);
        b.setTitle(title);
        b.setLegendVisible(false);
        b.setAnimated(false);
        styleChart(b);
        return b;
    }

    private void styleChart(Chart c) {
        c.getStyleClass().add("adb-chart");
        c.setMinHeight(260);
        c.setPrefHeight(280);
    }

    private void updatePackChartsData() {
        if (chartsHost == null || chartStatut == null) return;

        List<Pack> data = new ArrayList<>(master);

        long nActif = data.stream().filter(p -> p.getStatutPack() == StatutPack.ACTIF).count();
        long nInactif = data.stream().filter(p -> p.getStatutPack() == StatutPack.INACTIF).count();
        long nAutre = data.size() - nActif - nInactif;
        ObservableList<PieChart.Data> pie = FXCollections.observableArrayList();
        if (nActif > 0) pie.add(new PieChart.Data("Actifs", nActif));
        if (nInactif > 0) pie.add(new PieChart.Data("Inactifs", nInactif));
        if (nAutre > 0) pie.add(new PieChart.Data("Autre / non renseigné", nAutre));
        if (pie.isEmpty()) pie.add(new PieChart.Data("Aucune donnée", 1));
        chartStatut.setData(pie);

        XYChart.Series<String, Number> sTypes = new XYChart.Series<>();
        sTypes.setName("Nombre");
        for (TypePack t : TypePack.values()) {
            long c = data.stream().filter(p -> p.getTypePack() == t).count();
            sTypes.getData().add(new XYChart.Data<>(t.name(), c));
        }
        chartTypes.getData().clear();
        chartTypes.getData().add(sTypes);

        XYChart.Series<String, Number> sPrix = new XYChart.Series<>();
        sPrix.setName("Prix");
        data.stream()
                .filter(p -> p.getPrixBase() != null)
                .sorted(Comparator.comparing(Pack::getPrixBase).reversed())
                .limit(10)
                .forEach(p -> sPrix.getData().add(new XYChart.Data<>(
                        trunc(safe(p.getNom()), 14),
                        p.getPrixBase().doubleValue())));
        if (sPrix.getData().isEmpty()) {
            sPrix.getData().add(new XYChart.Data<>("—", 0));
        }
        chartPrixTop.getData().clear();
        chartPrixTop.getData().add(sPrix);

        int b1 = 0, b2 = 0, b3 = 0, b4 = 0;
        for (Pack p : data) {
            int m = p.getNbActivitesMax();
            if (m <= 5) b1++;
            else if (m <= 10) b2++;
            else if (m <= 20) b3++;
            else b4++;
        }
        XYChart.Series<String, Number> sAct = new XYChart.Series<>();
        sAct.getData().addAll(
                new XYChart.Data<>("1 – 5", b1),
                new XYChart.Data<>("6 – 10", b2),
                new XYChart.Data<>("11 – 20", b3),
                new XYChart.Data<>("21 +", b4));
        chartActHist.getData().clear();
        chartActHist.getData().add(sAct);

        int r0 = 0, r1 = 0, r2 = 0, r3 = 0;
        for (Pack p : data) {
            BigDecimal rd = p.getReduction();
            double v = rd == null ? 0 : rd.doubleValue();
            if (v <= 0) r0++;
            else if (v <= 5) r1++;
            else if (v <= 10) r2++;
            else r3++;
        }
        XYChart.Series<String, Number> sRed = new XYChart.Series<>();
        sRed.getData().addAll(
                new XYChart.Data<>("0 %", r0),
                new XYChart.Data<>("1 – 5 %", r1),
                new XYChart.Data<>("6 – 10 %", r2),
                new XYChart.Data<>("11 + %", r3));
        chartRedHist.getData().clear();
        chartRedHist.getData().add(sRed);

        int p0 = 0, p1 = 0, p2 = 0, p3 = 0;
        for (Pack p : data) {
            if (p.getPrixBase() == null) continue;
            double v = p.getPrixBase().doubleValue();
            if (v <= 50) p0++;
            else if (v <= 100) p1++;
            else if (v <= 200) p2++;
            else p3++;
        }
        XYChart.Series<String, Number> sBuck = new XYChart.Series<>();
        sBuck.getData().addAll(
                new XYChart.Data<>("0 – 50", p0),
                new XYChart.Data<>("51 – 100", p1),
                new XYChart.Data<>("101 – 200", p2),
                new XYChart.Data<>("201 +", p3));
        chartPrixBuckets.getData().clear();
        chartPrixBuckets.getData().add(sBuck);
    }

    private static String trunc(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(1, max - 1)) + "…";
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
            updatePackChartsData();
        } catch (Exception e) {
            showError(e);
        }
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
        } catch (Exception e) {
            showError(e);
        }
    }

    private void render() {
        if (rowsBox == null) return;
        rowsBox.getChildren().clear();

        String q = (txtSearch == null || txtSearch.getText() == null)
                ? "" : txtSearch.getText().trim().toLowerCase();

        List<Pack> visible = new ArrayList<>();
        for (Pack p : master) {
            if (q.isEmpty() || matchPack(p, q)) visible.add(p);
        }
        visible.sort(Comparator.comparing(p -> safe(p.getNom()).toLowerCase()));

        updateKpis();

        int i = 0;
        for (Pack p : visible) {
            VBox card = buildPackCard(p);
            card.setOpacity(0);
            card.setTranslateY(8);
            rowsBox.getChildren().add(card);

            PauseTransition delay = new PauseTransition(Duration.millis(i * 22L));
            delay.setOnFinished(e -> playCardAppear(card));
            delay.play();
            i++;
        }

        if (lblTotal != null) {
            lblTotal.setText(visible.size() + " affiché(s) · " + master.size() + " au catalogue");
        }
    }

    private void updateKpis() {
        int n = master.size();
        long actifs = master.stream().filter(p -> p.getStatutPack() == StatutPack.ACTIF).count();
        long inactifs = n - actifs;

        if (lblActifs != null) lblActifs.setText(actifs + " actifs");
        if (lblInactifs != null) lblInactifs.setText(inactifs + " inactifs");
        if (lblKpiPacks != null) lblKpiPacks.setText(String.valueOf(n));
        if (lblKpiActifs != null) lblKpiActifs.setText(String.valueOf(actifs));

        BigDecimal sum = master.stream()
                .map(Pack::getPrixBase)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = BigDecimal.ZERO;
        long cnt = master.stream().map(Pack::getPrixBase).filter(Objects::nonNull).count();
        if (cnt > 0) {
            avg = sum.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP);
        }
        if (lblKpiPrixMoyen != null) {
            lblKpiPrixMoyen.setText(avg.compareTo(BigDecimal.ZERO) == 0 ? "—"
                    : String.format(Locale.FRANCE, "%.2f", avg));
        }
    }

    private boolean matchPack(Pack p, String q) {
        String nom = safe(p.getNom()).toLowerCase();
        String type = p.getTypePack() == null ? "" : p.getTypePack().name().toLowerCase();
        String statut = p.getStatutPack() == null ? "" : p.getStatutPack().name().toLowerCase();
        return nom.contains(q) || type.contains(q) || statut.contains(q);
    }

    private VBox buildPackCard(Pack p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("adb-pack-card");

        boolean actif = p.getStatutPack() == StatutPack.ACTIF;

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label chip = new Label(p.getTypePack() == null ? "—" : p.getTypePack().name());
        chip.getStyleClass().add("adb-type-chip");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label st = new Label(p.getStatutPack() == null ? "" : p.getStatutPack().name());
        st.getStyleClass().add(actif ? "adb-stat-actif" : "adb-stat-inactif");
        top.getChildren().addAll(chip, sp, st);

        Label name = new Label(safe(p.getNom()));
        name.getStyleClass().add("adb-pack-name");
        name.setWrapText(true);

        Region sep = new Region();
        sep.getStyleClass().add("adb-sep");
        sep.setPrefHeight(1);

        String prixTxt = (p.getPrixBase() == null ? "0" : p.getPrixBase().toPlainString()) + " DT";
        Label prix = new Label(prixTxt);
        prix.getStyleClass().add("adb-price");

        String redTxt = (p.getReduction() == null ? "0" : p.getReduction().toPlainString()) + " %";
        Label red = new Label("Réduction : " + redTxt);
        red.getStyleClass().add("adb-red-label");
        Label mx = new Label("Max activités : " + p.getNbActivitesMax());
        mx.getStyleClass().add("adb-metric");
        HBox meta = new HBox(16, red, mx);
        meta.setAlignment(Pos.CENTER_LEFT);

        Button btnEdit = new Button("Modifier");
        btnEdit.getStyleClass().add("adb-mini-btn");
        Button btnDel = new Button("Supprimer");
        btnDel.getStyleClass().add("adb-mini-del");
        HBox actions = new HBox(8, btnEdit, btnDel);
        actions.getStyleClass().add("adb-card-actions");

        card.getChildren().addAll(top, name, sep, prix, meta, actions);

        card.setOnMouseClicked(e -> {
            selectCard(card, p);
            if (e.getClickCount() == 2) onEdit();
        });
        btnEdit.setOnAction(e -> {
            selectCard(card, p);
            onEdit();
        });
        btnDel.setOnAction(e -> {
            selectCard(card, p);
            onDelete();
        });

        return card;
    }

    private void selectCard(VBox card, Pack p) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().removeAll("adb-pack-card-selected");
        }
        selectedCard = card;
        selectedPack = p;
        card.getStyleClass().removeAll("adb-pack-card-selected");
        card.getStyleClass().add("adb-pack-card-selected");

        ScaleTransition s1 = new ScaleTransition(Duration.millis(110), card);
        s1.setToX(1.02);
        s1.setToY(1.02);
        s1.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition s2 = new ScaleTransition(Duration.millis(130), card);
        s2.setToX(1.0);
        s2.setToY(1.0);
        s2.setInterpolator(Interpolator.EASE_BOTH);
        new SequentialTransition(s1, s2).play();
    }

    private void clearSelection() {
        if (selectedCard != null) {
            selectedCard.getStyleClass().removeAll("adb-pack-card-selected");
        }
        selectedPack = null;
        selectedCard = null;
    }

    private void playCardAppear(VBox card) {
        FadeTransition ft = new FadeTransition(Duration.millis(160), card);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(160), card);
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

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void openForm(Pack pack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PackForm.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            } catch (Exception ignored) {
            }
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

    @FXML
    private void onDashboard() {
        navigateSafe("/fxml/Dashboard.fxml");
    }

    @FXML
    private void onPacks() {
        refresh();
    }

    @FXML
    private void onInscriptions() {
        navigateSafe("/fxml/InscriptionList.fxml");
    }

    @FXML
    private void onRetour() {
        navigateSafe("/Menu.fxml");
    }

    private void navigateSafe(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) lblTotal.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError(e);
        }
    }
}
