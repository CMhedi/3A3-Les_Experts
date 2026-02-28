package controllers;

import Entities.Evenement;
import Entities.ReservationEvenement;
import Services.EvenementService;
import Services.ReservationEvenementService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminDashboardController {

    @FXML private Label lblTotalEvents;
    @FXML private Label lblTotalTickets;
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> ticketsBarChart;

    private final EvenementService evService = new EvenementService();
    private final ReservationEvenementService resService = new ReservationEvenementService();

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    public void refreshDashboard() {
        try {
            List<Evenement> events = evService.getAll();
            List<ReservationEvenement> reservations = resService.getAll();

            // 1. Stats Rapides
            lblTotalEvents.setText(String.valueOf(events.size()));
            int totalTickets = reservations.stream().mapToInt(ReservationEvenement::getNbBillets).sum();
            lblTotalTickets.setText(String.valueOf(totalTickets));

            // 2. PieChart: Categories
            Map<String, Long> catStats = events.stream()
                    .collect(Collectors.groupingBy(e -> e.getCategorieEvt().toString(), Collectors.counting()));
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            catStats.forEach((cat, count) -> pieData.add(new PieChart.Data(cat, count)));
            categoryPieChart.setData(pieData);

            // 3. BarChart: Top Ventes
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Billets Vendus");
            events.stream().limit(6).forEach(ev -> {
                int sold = reservations.stream()
                        .filter(r -> r.getIdEvenement() == ev.getIdEvenement())
                        .mapToInt(ReservationEvenement::getNbBillets).sum();
                series.getData().add(new XYChart.Data<>(ev.getTitre(), sold));
            });
            ticketsBarChart.getData().clear();
            ticketsBarChart.getData().add(series);

        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void exportToExcel() {
        String fileName = "Rapport_Reservations.xlsx";
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rapport EcoAdventure");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"ID", "Event ID", "Billets", "Statut", "Date"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            List<ReservationEvenement> resList = resService.getAll();
            int rowNum = 1;
            for (ReservationEvenement r : resList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getIdResEvt());
                row.createCell(1).setCellValue(r.getIdEvenement());
                row.createCell(2).setCellValue(r.getNbBillets());
                row.createCell(3).setCellValue(r.getStatutRes().toString());
                row.createCell(4).setCellValue(r.getDateReservation().toString());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                workbook.write(fileOut);
                if (Desktop.isDesktopSupported()) {
                    File excelFile = new File(fileName);
                    if (excelFile.exists()) Desktop.getDesktop().open(excelFile);
                }
                new Alert(Alert.AlertType.INFORMATION, "Excel généré : " + fileName).show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
        }
    }
}