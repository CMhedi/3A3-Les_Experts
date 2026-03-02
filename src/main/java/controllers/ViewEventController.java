package controllers;

import Entities.Evenement;
import Entities.ReservationEvenement;
import Services.EvenementService;
import Services.ReservationEvenementService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ViewEventController implements Initializable {

    @FXML private Label lblTotalEvents;
    @FXML private Label lblTotalReservations;
    @FXML private Label lblTotalRevenue;
    @FXML private VBox containerTrends;
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> popularityBarChart;

    private final EvenementService evenementService = new EvenementService();
    private final ReservationEvenementService reservationService = new ReservationEvenementService();

    private List<Evenement> events;
    private List<ReservationEvenement> reservations;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshStats();
    }

    @FXML
    public void refreshStats() {
        try {
            events = evenementService.getAll();
            reservations = reservationService.getAll();

            // KPIs de base
            int totalEvents = events.size();
            int totalTicketsSold = reservations.stream().mapToInt(ReservationEvenement::getNbBillets).sum();
            int totalCapacity = events.stream().mapToInt(Evenement::getNbPlaces).sum() + totalTicketsSold;
            double totalRevenue = reservations.stream().mapToDouble(r -> r.getNbBillets() * r.getPrixUnitaire()).sum();

            lblTotalEvents.setText(String.valueOf(totalEvents));
            lblTotalReservations.setText(String.valueOf(totalTicketsSold));
            lblTotalRevenue.setText(String.format("%.2f DT", totalRevenue));

            // Graphiques
            populateCategoryChart(events);
            populatePopularityChart(events, reservations);

            // Intelligence Prédictive & Insights
            generateRealInsights(events, reservations, totalTicketsSold, totalCapacity, totalRevenue);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void exportToExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rapport Stratégique");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Colonnes détaillées pour l'admin
            String[] headers = {"ID", "Titre", "Catégorie", "Capacité Totale", "Billets Vendus", "Taux Remplissage (%)", "Prix Moyen (DT)", "CA Réalisé", "CA Perdu (Places Vides)"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Evenement ev : events) {
                Row row = sheet.createRow(rowIdx++);

                int sold = reservations.stream()
                        .filter(r -> r.getIdEvenement() == ev.getIdEvenement())
                        .mapToInt(ReservationEvenement::getNbBillets).sum();

                double avgPrice = reservations.stream()
                        .filter(r -> r.getIdEvenement() == ev.getIdEvenement())
                        .mapToDouble(ReservationEvenement::getPrixUnitaire).average().orElse(0.0);

                double currentRev = sold * avgPrice;
                double missingRev = ev.getNbPlaces() * avgPrice;
                double fillRate = (ev.getNbPlaces() + sold) > 0 ? (double) sold / (ev.getNbPlaces() + sold) * 100 : 0;

                row.createCell(0).setCellValue(ev.getIdEvenement());
                row.createCell(1).setCellValue(ev.getTitre());
                row.createCell(2).setCellValue(ev.getCategorieEvt() != null ? ev.getCategorieEvt().name() : "N/A");
                row.createCell(3).setCellValue(ev.getNbPlaces() + sold);
                row.createCell(4).setCellValue(sold);
                row.createCell(5).setCellValue(String.format("%.1f%%", fillRate));
                row.createCell(6).setCellValue(avgPrice);
                row.createCell(7).setCellValue(currentRev);
                row.createCell(8).setCellValue(missingRev);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName("Rapport_Events.xlsx");
            File file = fileChooser.showSaveDialog(lblTotalEvents.getScene().getWindow());

            if (file != null) {
                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    workbook.write(fileOut);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void generateRealInsights(List<Evenement> events, List<ReservationEvenement> reservations,
                                      int totalSold, int totalCapacity, double totalRevenue) {
        containerTrends.getChildren().clear();

        // 1. Analyse d'Occupation
        double occupancyRate = (totalCapacity > 0) ? (double) totalSold / totalCapacity * 100 : 0;
        addInsightItem("📊 Analyse de Part de Marché",
                String.format("Taux d'occupation : %.1f%%. La performance globale est jugée %s.",
                        occupancyRate, (occupancyRate > 50 ? "Satisfaisante" : "Critique")),
                occupancyRate > 50 ? "#10b981" : "#f59e0b");

        // 2. Concentration de Revenus
        Map<String, Double> categoryRevenue = reservations.stream()
                .collect(Collectors.groupingBy(r -> {
                    Evenement ev = events.stream().filter(e -> e.getIdEvenement() == r.getIdEvenement()).findFirst().orElse(null);
                    return (ev != null && ev.getCategorieEvt() != null) ? ev.getCategorieEvt().name() : "Divers";
                }, Collectors.summingDouble(r -> r.getNbBillets() * r.getPrixUnitaire())));
        String leadCat = categoryRevenue.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A");
        addInsightItem("💰 Concentration de Revenus", String.format("La catégorie '%s' est votre principal moteur de croissance ce mois-ci.", leadCat), "#2563eb");

        // 3. Prévision (Forecasting)
        double potentialRevenue = events.stream().mapToDouble(e -> {
            double price = reservations.stream().filter(r -> r.getIdEvenement() == e.getIdEvenement()).mapToDouble(ReservationEvenement::getPrixUnitaire).average().orElse(0.0);
            return (e.getNbPlaces() + (reservations.stream().filter(r -> r.getIdEvenement() == e.getIdEvenement()).mapToInt(ReservationEvenement::getNbBillets).sum())) * price;
        }).sum();
        addInsightItem("📈 Prévision de Croissance", String.format("Potentiel inexploité : +%.2f DT si tous les événements affichent complet.", (potentialRevenue - totalRevenue)), "#0891b2");

        // 4. Analyse des Risques
        long atRisk = events.stream().filter(e -> reservations.stream().noneMatch(r -> r.getIdEvenement() == e.getIdEvenement())).count();
        if (atRisk > 0) {
            addInsightItem("⚠️ Alerte Engagement", atRisk + " événement(s) n'ont aucune réservation. Risque de perte de profit.", "#dc2626");
        }

        // 5. Profil de Consommation
        double avgTickets = reservations.isEmpty() ? 0 : reservations.stream().mapToInt(ReservationEvenement::getNbBillets).average().orElse(0);
        addInsightItem("👥 Profil de Consommation", String.format("Moyenne de %.1f billets par client. Le public cible est majoritairement %s.", avgTickets, (avgTickets > 2 ? "de groupe" : "individuel")), "#7c3aed");
    }

    private void populateCategoryChart(List<Evenement> events) {
        Map<String, Long> categoryCount = events.stream()
                .collect(Collectors.groupingBy(e -> e.getCategorieEvt() != null ? e.getCategorieEvt().name() : "Sans Catégorie", Collectors.counting()));
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        categoryCount.forEach((cat, count) -> pieData.add(new PieChart.Data(cat, count)));
        categoryPieChart.setData(pieData);
    }

    private void populatePopularityChart(List<Evenement> events, List<ReservationEvenement> reservations) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Billets Vendus");
        Map<Integer, Integer> eventTickets = reservations.stream()
                .collect(Collectors.groupingBy(ReservationEvenement::getIdEvenement, Collectors.summingInt(ReservationEvenement::getNbBillets)));
        eventTickets.entrySet().stream().sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed()).limit(5)
                .forEach(entry -> {
                    events.stream().filter(e -> e.getIdEvenement() == entry.getKey()).findFirst()
                            .ifPresent(ev -> series.getData().add(new XYChart.Data<>(ev.getTitre(), entry.getValue())));
                });
        popularityBarChart.getData().clear();
        popularityBarChart.getData().add(series);
    }

    private void addInsightItem(String title, String content, String color) {
        VBox item = new VBox(5);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-color: " + color + "; -fx-border-width: 0 0 0 4;");
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + color + ";");
        Label lblContent = new Label(content);
        lblContent.setWrapText(true);
        lblContent.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        item.getChildren().addAll(lblTitle, lblContent);
        containerTrends.getChildren().add(item);
    }
}