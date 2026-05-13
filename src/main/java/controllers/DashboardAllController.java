package controllers;

import Entities.*;
import Utiles.MyDB2;
import enums.RoleUser;
import enums.StatutPack;
import enums.TypePack;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DashboardAllController {

    // ===== KPI
    @FXML private Label lblDate;
    @FXML private Label kpiPacks, kpiUsers, kpiInscr, kpiRevenue;

    // ===== Top packs
    @FXML private TableView<DashboardController.PackStat> tblTopPacks;
    @FXML private TableColumn<DashboardController.PackStat, String> colPack;
    @FXML private TableColumn<DashboardController.PackStat, Number> colNb;
    @FXML private TableColumn<DashboardController.PackStat, String> colPrix;
    @FXML private TableColumn<DashboardController.PackStat, String> colStatut;

    // ===== Loyal user
    @FXML private Label lblLoyalName, lblLoyalEmail, lblLoyalCount;

    // ===== Tabs tables (toutes classes)
    @FXML private TableView<Pack> tblPacks;
    @FXML private TableView<Inscription> tblInscriptions;
    @FXML private TableView<UserApp> tblUsers;
    @FXML private TableView<Activite> tblActivites;
    @FXML private TableView<Evenement> tblEvenements;
    @FXML private TableView<ReservationActivite> tblResAct;
    @FXML private TableView<ReservationEvenement> tblResEvt;
    @FXML private TableView<Planning> tblPlanning;
    @FXML private TableView<Seance> tblSeances;
    @FXML private TableView<Reclamation> tblReclamations;
    @FXML private TableView<Conversation> tblConversations;
    @FXML private TableView<Message> tblMessages;

    private final DashboardController dash = new DashboardController();

    @FXML
    public void initialize() {
        // top packs columns
        colPack.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPackName()));
        colNb.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getInscriptionCount()));
        colPrix.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPrixBase() + " DT"));
        colStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatutPack()));

        refreshAll();
    }

    @FXML
    private void onRefresh(ActionEvent e) {
        refreshAll();
    }

    @FXML
    private void onDashboard(ActionEvent e) {
        // Stay on Dashboard - just refresh
        refreshAll();
    }

    @FXML
    private void onPacks(ActionEvent e) {
        try {
            navigateTo("/fxml/PackList.fxml");
        } catch (Exception ex) {
            showError("Erreur de navigation", ex.getMessage());
        }
    }

    @FXML
    private void onInscriptions(ActionEvent e) {
        try {
            navigateTo("/fxml/InscriptionList.fxml");
        } catch (Exception ex) {
            showError("Erreur de navigation", ex.getMessage());
        }
    }

    @FXML
    private void onRetour(ActionEvent e) {
        try {
            navigateTo("/Menu.fxml");
        } catch (Exception ex) {
            showError("Erreur de navigation", ex.getMessage());
        }
    }

    @FXML
    private void onUsers(ActionEvent e) {
        // Placeholder - you can add user management FXML later
        showError("Gestion des Utilisateurs", "Interface en développement");
    }

    @FXML
    private void onReclamations(ActionEvent e) {
        try {
            navigateTo("/GUI/AddReclamation.fxml");
        } catch (Exception ex) {
            showError("Erreur de navigation", ex.getMessage());
        }
    }

    @FXML
    private void onMessaging(ActionEvent e) {
        // Placeholder - you can add messaging FXML later
        showError("Messagerie", "Interface en développement");
    }

    @FXML
    private void onProfile(ActionEvent e) {
        // Placeholder - you can add profile FXML later
        showError("Mon Profil", "Interface en développement");
    }

    @FXML
    private void onEvents(ActionEvent e) {
        // Placeholder - you can add event management FXML later
        showError("Gestion Événement", "Interface en développement");
    }

    @FXML
    private void onEventDashboard(ActionEvent e) {
        // Placeholder - you can add event dashboard FXML later
        showError("Dashboard Événement", "Interface en développement");
    }

    @FXML
    private void onEventReservations(ActionEvent e) {
        // Placeholder - you can add event reservations FXML later
        showError("Réservations Événements", "Interface en développement");
    }

    @FXML
    private void onPlanning(ActionEvent e) {
        try {
            navigateTo("/AdminPlanningView.fxml");
        } catch (Exception ex) {
            showError("Erreur de navigation", ex.getMessage());
        }
    }

    /**
     * Navigation helper - loads the specified FXML file in the current stage
     */
    private void navigateTo(String fxmlPath) throws Exception {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
        javafx.scene.Parent root = loader.load();
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        javafx.stage.Stage stage = (javafx.stage.Stage) lblDate.getScene().getWindow();
        stage.setScene(scene);
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshAll() {
        lblDate.setText(LocalDateTime.now().toString().replace("T", "  "));

        // Load main lists from DB
        List<Pack> packs = loadPacks();
        List<UserApp> users = loadUsers();
        List<Inscription> inscriptions = loadInscriptions();

        // KPI
        kpiPacks.setText(String.valueOf(packs.size()));
        kpiUsers.setText(String.valueOf(users.size()));
        kpiInscr.setText(String.valueOf(inscriptions.size()));
        kpiRevenue.setText(dash.computeKpis(inscriptions).getRevenueTotal() + " DT");

        // Top packs (streams)
        var top = dash.topActivatedPacks(packs, inscriptions, 6);
        tblTopPacks.setItems(FXCollections.observableArrayList(top));

        // Loyal user
        var loyalOpt = dash.mostLoyalUser(users, inscriptions);
        if (loyalOpt.isPresent()) {
            var loyal = loyalOpt.get();
            lblLoyalName.setText(loyal.getFullName());
            lblLoyalEmail.setText(loyal.getEmail());
            lblLoyalCount.setText(String.valueOf(loyal.getTotalInscriptions()));
        } else {
            lblLoyalName.setText("—");
            lblLoyalEmail.setText("—");
            lblLoyalCount.setText("0");
        }

        // Fill “toutes classes” tables
        tblPacks.setItems(FXCollections.observableArrayList(packs));
        tblUsers.setItems(FXCollections.observableArrayList(users));
        tblInscriptions.setItems(FXCollections.observableArrayList(inscriptions));

        // autres entités (si tes tables existent)
        tblActivites.setItems(FXCollections.observableArrayList(loadActivites()));
        tblEvenements.setItems(FXCollections.observableArrayList(loadEvenements()));
        tblResAct.setItems(FXCollections.observableArrayList(loadReservationActivite()));
        tblResEvt.setItems(FXCollections.observableArrayList(loadReservationEvenement()));
        tblPlanning.setItems(FXCollections.observableArrayList(loadPlanning()));
        tblSeances.setItems(FXCollections.observableArrayList(loadSeances()));
        tblReclamations.setItems(FXCollections.observableArrayList(loadReclamations()));
        tblConversations.setItems(FXCollections.observableArrayList(loadConversations()));
        tblMessages.setItems(FXCollections.observableArrayList(loadMessages()));
    }

    // =========================
    // DAO minimal
    // =========================

    private List<Pack> loadPacks() {
        List<Pack> list = new ArrayList<>();
        String sql = "SELECT id_pack, nom, type_pack, prix_base, reduction, nb_activites_max, statut_pack FROM pack";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pack p = new Pack();
                p.setIdPack(rs.getInt("id_pack"));
                p.setNom(rs.getString("nom"));
                try { p.setTypePack(TypePack.valueOf(rs.getString("type_pack"))); } catch (Exception ignored) {}
                p.setPrixBase(rs.getBigDecimal("prix_base"));
                p.setReduction(rs.getBigDecimal("reduction"));
                p.setNbActivitesMax(rs.getInt("nb_activites_max"));
                try { p.setStatutPack(StatutPack.valueOf(rs.getString("statut_pack"))); } catch (Exception ignored) {}
                list.add(p);
            }
        } catch (Exception e) {
            System.out.println("Pack load error: " + e.getMessage());
        }
        return list;
    }

    private List<UserApp> loadUsers() {
        List<UserApp> list = new ArrayList<>();
        String sql = "SELECT id_user, nom, prenom, email, telephone, image_url, role, mot_de_passe, date_creation FROM user_app";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UserApp u = new UserApp();
                u.setIdUser(rs.getInt("id_user"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setTelephone(rs.getString("telephone"));
                u.setImageUrl(rs.getString("image_url"));
                try { u.setRole(RoleUser.valueOf(rs.getString("role"))); } catch (Exception ignored) {}
                u.setMotDePasse(rs.getString("mot_de_passe"));
                Timestamp t = rs.getTimestamp("date_creation");
                if (t != null) u.setDateCreation(t.toLocalDateTime());
                list.add(u);
            }
        } catch (Exception e) {
            System.out.println("User load error: " + e.getMessage());
        }
        return list;
    }

    private List<Inscription> loadInscriptions() {
        List<Inscription> list = new ArrayList<>();
        String sql = "SELECT id_inscription, date_inscription, statut_inscr, montant_total, id_user, id_pack FROM inscription";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Inscription i = new Inscription();
                i.setIdInscription(rs.getInt("id_inscription"));
                Timestamp t = rs.getTimestamp("date_inscription");
                if (t != null) i.setDateInscription(t.toLocalDateTime());
                i.setStatutInscr(rs.getString("statut_inscr"));
                i.setMontantTotal(rs.getBigDecimal("montant_total"));
                i.setIdUser(rs.getInt("id_user"));
                i.setIdPack(rs.getInt("id_pack"));
                list.add(i);
            }
        } catch (Exception e) {
            System.out.println("Inscription load error: " + e.getMessage());
        }
        return list;
    }

    private List<Activite> loadActivites() { return loadGeneric("activite", Activite.class); }
    private List<Evenement> loadEvenements() { return loadGeneric("evenement", Evenement.class); }
    private List<ReservationActivite> loadReservationActivite() { return loadGeneric("reservation_activite", ReservationActivite.class); }
    private List<ReservationEvenement> loadReservationEvenement() { return loadGeneric("reservation_evenement", ReservationEvenement.class); }
    private List<Planning> loadPlanning() { return loadGeneric("planning", Planning.class); }
    private List<Seance> loadSeances() { return loadGeneric("seance", Seance.class); }
    private List<Reclamation> loadReclamations() { return loadGeneric("reclamation", Reclamation.class); }
    private List<Conversation> loadConversations() { return loadGeneric("conversation", Conversation.class); }
    private List<Message> loadMessages() { return loadGeneric("message", Message.class); }

    private <T> List<T> loadGeneric(String table, Class<T> type) {
        List<T> list = new ArrayList<>();
        String sql = "SELECT * FROM " + table + " LIMIT 200";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(type.getDeclaredConstructor().newInstance());
            }
        } catch (Exception e) {
            System.out.println("Load " + table + " error: " + e.getMessage());
        }
        return list;
    }
}
