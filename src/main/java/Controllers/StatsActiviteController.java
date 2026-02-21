package Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.sql.*;
import java.text.DecimalFormat;

public class StatsActiviteController {

    // ======= KPI Labels =======
    @FXML private Label kpiTotalActivites;
    @FXML private Label kpiDisponibles;
    @FXML private Label kpiReservations;
    @FXML private Label kpiPrixMoyen;

    // ======= Charts =======
    @FXML private PieChart pieCategories;
    @FXML private BarChart<String, Number> barTopReserved;
    @FXML private LineChart<String, Number> lineEvolution;
    @FXML private Label lblLineHint;

    // ======= DB CONFIG =======
    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    @FXML
    public void initialize() {
        refresh(null);
    }

    @FXML
    public void refresh(ActionEvent event) {
        loadKpis();
        loadPieCategories();
        loadBarTopReserved();
        loadLineEvolution();
    }

    // ================= KPI =================
    private void loadKpis() {
        try (Connection cn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            int total = scalarInt(cn, "SELECT COUNT(*) FROM activite");
            int dispo = scalarInt(cn, "SELECT COUNT(*) FROM activite WHERE LOWER(statut)='disponible'");
            int reservations = scalarInt(cn, "SELECT COUNT(*) FROM reservation_activite");
            double avgPrix = scalarDouble(cn, "SELECT COALESCE(AVG(prix),0) FROM activite");

            kpiTotalActivites.setText(String.valueOf(total));
            kpiDisponibles.setText(String.valueOf(dispo));
            kpiReservations.setText(String.valueOf(reservations));
            kpiPrixMoyen.setText(new DecimalFormat("#0.00").format(avgPrix) + " DT");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= PIE: categories =================
    private void loadPieCategories() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        String sql = "SELECT categorie_act, COUNT(*) AS c FROM activite GROUP BY categorie_act ORDER BY c DESC";

        try (Connection cn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String cat = rs.getString("categorie_act");
                int count = rs.getInt("c");
                if (cat == null || cat.isBlank()) cat = "Non défini";
                data.add(new PieChart.Data(cat, count));
            }

            pieCategories.setData(data);
            pieCategories.setLegendVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= BAR: top reserved activities =================
    private void loadBarTopReserved() {
        barTopReserved.getData().clear();

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Réservations");

        String sql = """
            SELECT a.nom, COUNT(r.id_res_act) AS nb
            FROM activite a
            LEFT JOIN reservation_activite r ON r.id_activite = a.id_activite
            GROUP BY a.id_activite, a.nom
            ORDER BY nb DESC
            LIMIT 8
        """;

        try (Connection cn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String nom = rs.getString("nom");
                int nb = rs.getInt("nb");
                if (nom == null || nom.isBlank()) nom = "Sans nom";
                // limiter texte x-axis
                if (nom.length() > 14) nom = nom.substring(0, 14) + "…";
                s.getData().add(new XYChart.Data<>(nom, nb));
            }

            barTopReserved.getData().add(s);
            barTopReserved.setLegendVisible(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= LINE: evolution by date_reservation (fallback if null) =================
    private void loadLineEvolution() {
        lineEvolution.getData().clear();

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Activités");

        // Si date_reservation est une DATE/DATETIME et remplie
        String sqlDate = """
            SELECT DATE(date_reservation) AS d, COUNT(*) AS c
            FROM activite
            WHERE date_reservation IS NOT NULL
            GROUP BY DATE(date_reservation)
            ORDER BY d
            LIMIT 14
        """;

        // Fallback: par statut si pas de dates
        String sqlFallback = """
            SELECT statut AS d, COUNT(*) AS c
            FROM activite
            GROUP BY statut
            ORDER BY c DESC
        """;

        try (Connection cn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            boolean hasDates = scalarInt(cn, "SELECT COUNT(*) FROM activite WHERE date_reservation IS NOT NULL") > 0;

            String sql = hasDates ? sqlDate : sqlFallback;
            lblLineHint.setText(hasDates ? "Derniers 14 jours (selon date_reservation)" : "Fallback: par statut (date_reservation vide)");

            try (PreparedStatement ps = cn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String x = rs.getString("d");
                    int c = rs.getInt("c");
                    if (x == null || x.isBlank()) x = "Non défini";
                    s.getData().add(new XYChart.Data<>(x, c));
                }
            }

            lineEvolution.getData().add(s);
            lineEvolution.setLegendVisible(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Helpers =====
    private int scalarInt(Connection cn, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double scalarDouble(Connection cn, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    @FXML
    private void goActivites(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/AdminActivites.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goReservations(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/reservation.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/AdminActivites.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}