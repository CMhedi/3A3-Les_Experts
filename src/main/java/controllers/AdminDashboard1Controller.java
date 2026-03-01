package controllers;

import Utiles.MyDB;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDashboard1Controller {

    // ===== fx:id (match AdminDashboard.fxml) =====
    @FXML private Label lblRevenue;
    @FXML private Label lblPacks;
    @FXML private Label lblReservations;
    @FXML private Label lblClaims;

    @FXML private AreaChart<String, Number> revenueChart;
    @FXML private PieChart rolePieChart;

    @FXML private TableView<RecentInscriptionRow> tableRecentInscriptions;
    @FXML private TableColumn<RecentInscriptionRow, String> colUser;
    @FXML private TableColumn<RecentInscriptionRow, String> colPack;
    @FXML private TableColumn<RecentInscriptionRow, String> colMontant;

    @FXML private TableView<MiniRow> miniTableActivite;
    @FXML private TableColumn<MiniRow, String> colMiniActNom;
    @FXML private TableColumn<MiniRow, String> colMiniActStatut;

    @FXML private TableView<MiniRow> miniTableEvenement;
    @FXML private TableColumn<MiniRow, String> colMiniEvtTitre;
    @FXML private TableColumn<MiniRow, String> colMiniEvtStatut;

    @FXML
    public void initialize() {
        setupTables();
        Platform.runLater(this::refreshDashboard);
    }

    private void setupTables() {
        if (colUser != null) colUser.setCellValueFactory(d -> d.getValue().userProperty());
        if (colPack != null) colPack.setCellValueFactory(d -> d.getValue().packProperty());
        if (colMontant != null) colMontant.setCellValueFactory(d -> d.getValue().montantProperty());

        if (colMiniActNom != null) colMiniActNom.setCellValueFactory(d -> d.getValue().nomProperty());
        if (colMiniActStatut != null) colMiniActStatut.setCellValueFactory(d -> d.getValue().statutProperty());

        if (colMiniEvtTitre != null) colMiniEvtTitre.setCellValueFactory(d -> d.getValue().nomProperty());
        if (colMiniEvtStatut != null) colMiniEvtStatut.setCellValueFactory(d -> d.getValue().statutProperty());
    }

    public void refreshDashboard() {
        try (Connection cnx = MyDB.getConnection()) {

            // 1) KPI Cards
            double revenue = getRevenue(cnx);
            int packsActifs = getPacksActifs(cnx);
            int reservations = getTotalReservations(cnx);
            int claims = getClaimsEnAttente(cnx);

            setLabel(lblRevenue, String.format("%.2f DT", revenue));
            setLabel(lblPacks, String.valueOf(packsActifs));
            setLabel(lblReservations, String.valueOf(reservations));
            setLabel(lblClaims, String.valueOf(claims));

            // 2) AreaChart: performance inscriptions
            loadRevenueChart(cnx);

            // 3) PieChart: roles
            loadRolePieChart(cnx);

            // 4) Dernières inscriptions
            loadRecentInscriptions(cnx);

            // 5) Mini tables
            loadMiniActivites(cnx);
            loadMiniEvenements(cnx);

        } catch (Exception e) {
            e.printStackTrace();
            // fallback visuel (sans crash)
            setLabel(lblRevenue, "0.00 DT");
            setLabel(lblPacks, "0");
            setLabel(lblReservations, "0");
            setLabel(lblClaims, "0");
        }
    }

    // ===================== KPI Queries =====================

    private double getRevenue(Connection cnx) throws SQLException {
        String sql =
                "SELECT COALESCE(SUM(montant_total),0) " +
                        "FROM inscription " +
                        "WHERE statut_inscr IN ('VALIDEE','CONFIRMEE')";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        }
    }

    private int getPacksActifs(Connection cnx) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pack WHERE statut_pack='ACTIF'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int getClaimsEnAttente(Connection cnx) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reclamation WHERE statut='EN_ATTENTE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int getTotalReservations(Connection cnx) throws SQLException {
        int evt = scalarInt(cnx, "SELECT COUNT(*) FROM reservation_evenement");
        int act = scalarInt(cnx, "SELECT COUNT(*) FROM reservation_activite");
        int sea = scalarInt(cnx, "SELECT COUNT(*) FROM reservation_seance");
        return evt + act + sea;
    }

    // ===================== Charts =====================

    private void loadRevenueChart(Connection cnx) throws SQLException {
        if (revenueChart == null) return;

        String sql =
                "SELECT DATE(date_inscription) d, COALESCE(SUM(montant_total),0) m " +
                        "FROM inscription " +
                        "GROUP BY DATE(date_inscription) " +
                        "ORDER BY d";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Montant (DT)");

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String date = rs.getString("d");
                double montant = rs.getDouble("m");
                series.getData().add(new XYChart.Data<>(date, montant));
            }
        }

        revenueChart.getData().clear();
        revenueChart.getData().add(series);
    }

    private void loadRolePieChart(Connection cnx) throws SQLException {
        if (rolePieChart == null) return;

        String sql = "SELECT role, COUNT(*) c FROM user_app GROUP BY role";
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String role = rs.getString("role");
                if (role == null || role.isBlank()) role = "UNKNOWN";
                int c = rs.getInt("c");
                data.add(new PieChart.Data(role, c));
            }
        }

        // si jamais vide
        if (data.isEmpty()) {
            data.addAll(new PieChart.Data("UNKNOWN", 1));
        }

        rolePieChart.setData(data);
    }

    // ===================== Tables =====================

    private void loadRecentInscriptions(Connection cnx) throws SQLException {
        if (tableRecentInscriptions == null) return;

        String sql =
                "SELECT CONCAT(u.prenom,' ',u.nom) AS userName, p.nom AS packName, i.montant_total AS montant " +
                        "FROM inscription i " +
                        "JOIN user_app u ON u.id_user = i.id_user " +
                        "JOIN pack p ON p.id_pack = i.id_pack " +
                        "ORDER BY i.date_inscription DESC " +
                        "LIMIT 8";

        ObservableList<RecentInscriptionRow> rows = FXCollections.observableArrayList();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String user = rs.getString("userName");
                String pack = rs.getString("packName");
                double montant = rs.getDouble("montant");
                rows.add(new RecentInscriptionRow(
                        safe(user),
                        safe(pack),
                        String.format("%.2f DT", montant)
                ));
            }
        }

        tableRecentInscriptions.setItems(rows);
    }

    private void loadMiniActivites(Connection cnx) throws SQLException {
        if (miniTableActivite == null) return;

        String sql =
                "SELECT nom, statut " +
                        "FROM activite " +
                        "ORDER BY id_activite DESC " +
                        "LIMIT 5";

        ObservableList<MiniRow> rows = FXCollections.observableArrayList();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new MiniRow(
                        safe(rs.getString("nom")),
                        safe(rs.getString("statut"))
                ));
            }
        }

        if (rows.isEmpty()) rows.add(new MiniRow("—", "—"));
        miniTableActivite.setItems(rows);
    }

    private void loadMiniEvenements(Connection cnx) throws SQLException {
        if (miniTableEvenement == null) return;

        String sql =
                "SELECT titre, statut " +
                        "FROM evenement " +
                        "ORDER BY date_event DESC " +
                        "LIMIT 5";

        ObservableList<MiniRow> rows = FXCollections.observableArrayList();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new MiniRow(
                        safe(rs.getString("titre")),
                        safe(rs.getString("statut"))
                ));
            }
        }

        if (rows.isEmpty()) rows.add(new MiniRow("—", "—"));
        miniTableEvenement.setItems(rows);
    }

    // ===================== Utils =====================

    private int scalarInt(Connection cnx, String sql) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void setLabel(Label lbl, String value) {
        if (lbl != null) lbl.setText(value);
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    // ===================== Row Models =====================

    public static class RecentInscriptionRow {
        private final SimpleStringProperty user = new SimpleStringProperty();
        private final SimpleStringProperty pack = new SimpleStringProperty();
        private final SimpleStringProperty montant = new SimpleStringProperty();

        public RecentInscriptionRow(String user, String pack, String montant) {
            this.user.set(user);
            this.pack.set(pack);
            this.montant.set(montant);
        }

        public SimpleStringProperty userProperty() { return user; }
        public SimpleStringProperty packProperty() { return pack; }
        public SimpleStringProperty montantProperty() { return montant; }
    }

    public static class MiniRow {
        private final SimpleStringProperty nom = new SimpleStringProperty();
        private final SimpleStringProperty statut = new SimpleStringProperty();

        public MiniRow(String nom, String statut) {
            this.nom.set(nom);
            this.statut.set(statut);
        }

        public SimpleStringProperty nomProperty() { return nom; }
        public SimpleStringProperty statutProperty() { return statut; }
    }
}