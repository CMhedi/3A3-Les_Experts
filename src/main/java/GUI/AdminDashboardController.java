package GUI;

import Entities.Inscription;
import Utiles.MyDB;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AdminDashboardController {

    @FXML private Label lblRevenue, lblPacks, lblReservations, lblClaims;
    @FXML private AreaChart<String, Number> revenueChart;
    @FXML private PieChart rolePieChart;

    @FXML private TableView<Inscription> tableRecentInscriptions;
    @FXML private TableColumn<Inscription, String> colUser, colPack, colMontant;

    @FXML private TableView<Map<String, String>> miniTableActivite, miniTableEvenement;
    @FXML private TableColumn<Map<String, String>, String> colMiniActNom, colMiniActStatut, colMiniEvtTitre, colMiniEvtStatut;

    private Connection conn = MyDB.getInstance().getConnection();

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    public void refreshDashboard() {
        try {
            updateKpis();
            loadAreaChart();
            loadPieChart();
            loadRecentInscriptionsTable();
            loadMiniTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateKpis() throws SQLException {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT SUM(montant_total) FROM inscription WHERE statut_inscr = 'VALIDEE'");
        if (rs.next()) lblRevenue.setText(String.format("%.2f DT", rs.getDouble(1)));

        rs = st.executeQuery("SELECT COUNT(*) FROM pack WHERE statut_pack = 'ACTIF'");
        if (rs.next()) lblPacks.setText(String.valueOf(rs.getInt(1)));

        rs = st.executeQuery("SELECT (SELECT COUNT() FROM reservation_activite) + (SELECT COUNT() FROM reservation_evenement)");
        if (rs.next()) lblReservations.setText(String.valueOf(rs.getInt(1)));

        rs = st.executeQuery("SELECT COUNT(*) FROM reclamation WHERE statut = 'EN_ATTENTE'");
        if (rs.next()) lblClaims.setText(String.valueOf(rs.getInt(1)));
    }

    private void loadAreaChart() throws SQLException {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenu");
        String sql = "SELECT DATE_FORMAT(date_inscription, '%b') as mois, SUM(montant_total) as total FROM inscription GROUP BY mois";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            series.getData().add(new XYChart.Data<>(rs.getString("mois"), rs.getDouble("total")));
        }
        revenueChart.getData().add(series);
    }

    private void loadPieChart() throws SQLException {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        String sql = "SELECT role, COUNT(*) FROM user_app GROUP BY role";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            pieData.add(new PieChart.Data(rs.getString(1), rs.getInt(2)));
        }
        rolePieChart.setData(pieData);
    }

    private void loadRecentInscriptionsTable() throws SQLException {
        colUser.setCellValueFactory(new PropertyValueFactory<>("nomUser"));
        colPack.setCellValueFactory(new PropertyValueFactory<>("nomPack"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantTotal"));

        ObservableList<Inscription> data = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.nom as user_name, p.nom as pack_name FROM inscription i " +
                "JOIN user_app u ON i.id_user = u.id_user JOIN pack p ON i.id_pack = p.id_pack LIMIT 5";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            Inscription ins = new Inscription();
            ins.setNomUser(rs.getString("user_name"));
            ins.setNomPack(rs.getString("pack_name"));
            ins.setMontantTotal(rs.getBigDecimal("montant_total"));
            data.add(ins);
        }
        tableRecentInscriptions.setItems(data);
    }

    private void loadMiniTables() throws SQLException {
        colMiniActNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("item")));
        colMiniActStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("statut")));

        colMiniEvtTitre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("item")));
        colMiniEvtStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("statut")));

        // ✅ Query Correcte : esmha 'statut'
        fillGenericTable("SELECT nom, statut FROM activite LIMIT 3", miniTableActivite);
        fillGenericTable("SELECT titre, statut FROM evenement LIMIT 3", miniTableEvenement);
    }

    private void fillGenericTable(String sql, TableView<Map<String, String>> table) throws SQLException {
        ObservableList<Map<String, String>> data = FXCollections.observableArrayList();
        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            Map<String, String> row = new HashMap<>();
            row.put("item", rs.getString(1));
            row.put("statut", rs.getString(2));
            data.add(row);
        }
        table.setItems(data);
    }
}