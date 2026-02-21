package controllers;

import Services.admin.AdminInsightsService;
import Services.admin.dto.AdminOverview;
import Services.admin.dto.AlertItem;
import Services.admin.dto.DailyStat;
import Services.admin.dto.TopPackStat;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminInsightsController {

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private Spinner<Integer> spTopLimit;
    @FXML private Spinner<Integer> spMinRisk;

    @FXML private Label lblTotal;
    @FXML private Label lblConfirmed;
    @FXML private Label lblPending;
    @FXML private Label lblCanceled;
    @FXML private Label lblRevenue;
    @FXML private Label lblTrend;

    @FXML private LineChart<String, Number> chartInscriptions;
    @FXML private LineChart<String, Number> chartRevenue;

    @FXML private VBox boxTopPacks;
    @FXML private VBox boxAlerts;

    @FXML private ProgressIndicator piLoading;
    @FXML private Label lblStatus;

    private final AdminInsightsService insightsService = new AdminInsightsService();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private void initialize() {
        dpTo.setValue(LocalDate.now());
        dpFrom.setValue(LocalDate.now().minusDays(14));

        spTopLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 20, 5));
        spMinRisk.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 70));

        chartInscriptions.setLegendVisible(false);
        chartRevenue.setLegendVisible(false);

        Platform.runLater(this::loadAll);
    }

    @FXML
    private void onRefresh() {
        loadAll();
    }

    private void loadAll() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        int topLimit = spTopLimit.getValue();
        int minRisk = spMinRisk.getValue();

        piLoading.setVisible(true);
        lblStatus.setText("Loading...");

        Task<Void> task = new Task<>() {
            AdminOverview overview;
            List<AlertItem> alerts;

            @Override
            protected Void call() throws Exception {
                overview = insightsService.getOverview(from, to, topLimit);
                alerts = insightsService.getAlerts(from, to, minRisk, 200);
                return null;
            }

            @Override
            protected void succeeded() {
                piLoading.setVisible(false);
                lblStatus.setText("Updated ✅ (" + df.format(LocalDate.now()) + ")");
                applyOverview(overview);
                renderAlerts(alerts);
            }

            @Override
            protected void failed() {
                piLoading.setVisible(false);
                lblStatus.setText("Error: " + (getException() != null ? getException().getMessage() : "unknown"));
            }
        };

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void applyOverview(AdminOverview ov) {
        if (ov == null) return;

        lblTotal.setText(String.valueOf(ov.getTotal()));
        lblConfirmed.setText(String.valueOf(ov.getConfirmed()));
        lblPending.setText(String.valueOf(ov.getPending()));
        lblCanceled.setText(String.valueOf(ov.getCanceled()));

        BigDecimal rev = ov.getTotalRevenue() != null ? ov.getTotalRevenue() : BigDecimal.ZERO;
        lblRevenue.setText(rev.toPlainString());
        lblTrend.setText(String.format("%.2f%%", ov.getRevenueTrendPercent()));

        fillCharts(ov.getDailySeries());
        renderTopPacks(ov.getTopPacks());
    }

    private void fillCharts(List<DailyStat> series) {
        chartInscriptions.getData().clear();
        chartRevenue.getData().clear();

        XYChart.Series<String, Number> sIns = new XYChart.Series<>();
        XYChart.Series<String, Number> sRev = new XYChart.Series<>();

        if (series != null) {
            for (DailyStat d : series) {
                String x = d.getDate() != null ? d.getDate().toString() : "";
                sIns.getData().add(new XYChart.Data<>(x, d.getInscriptions()));
                BigDecimal r = d.getRevenue() != null ? d.getRevenue() : BigDecimal.ZERO;
                sRev.getData().add(new XYChart.Data<>(x, r));
            }
        }

        chartInscriptions.getData().add(sIns);
        chartRevenue.getData().add(sRev);
    }

    private void renderTopPacks(List<TopPackStat> packs) {
        boxTopPacks.getChildren().clear();
        if (packs == null || packs.isEmpty()) {
            boxTopPacks.getChildren().add(new Label("Aucun pack trouvé."));
            return;
        }
        for (TopPackStat p : packs) {
            HBox row = rowWidget(
                    safe(p.getPackName()), 260,
                    safe(p.getTypePack()), 160,
                    String.valueOf(p.getInscriptions()), 90,
                    (p.getRevenue() != null ? p.getRevenue().toPlainString() : "0"), 120
            );
            boxTopPacks.getChildren().add(row);
        }
    }

    private void renderAlerts(List<AlertItem> alerts) {
        boxAlerts.getChildren().clear();
        if (alerts == null || alerts.isEmpty()) {
            boxAlerts.getChildren().add(new Label("Aucune alerte."));
            return;
        }
        for (AlertItem a : alerts) {
            String date = (a.getDateInscription() != null) ? dtf.format(a.getDateInscription()) : "";
            String montant = (a.getMontant() != null) ? a.getMontant().toPlainString() : "0";

            HBox row = rowWidget(
                    date, 150,
                    safe(a.getUserName()), 160,
                    safe(a.getPackName()), 200,
                    String.valueOf(a.getRiskScore()), 60,
                    safe(a.getLevel()), 70,
                    safe(a.getSignals()), 260,
                    safe(a.getStatut()), 90,
                    montant, 100
            );
            boxAlerts.getChildren().add(row);
        }
    }

    private HBox rowWidget(Object... spec) {
        HBox row = new HBox(10);
        row.setStyle("-fx-padding:8; -fx-background-color:#ffffff; -fx-border-color:#f0f0f0; -fx-border-radius:10; -fx-background-radius:10;");

        for (int i = 0; i < spec.length; i += 2) {
            String text = String.valueOf(spec[i]);
            double w = ((Number) spec[i + 1]).doubleValue();

            Label cell = new Label(text);
            cell.setPrefWidth(w);
            cell.setMinWidth(w);
            cell.setMaxWidth(w);
            cell.setStyle("-fx-opacity:0.92;");

            row.getChildren().add(cell);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().add(spacer);
        return row;
    }

    private String safe(String s) { return s == null ? "" : s; }
}
