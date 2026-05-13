package controllers;

import Utiles.MyDB2;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminMessagerieController {

    // ── KPI ───────────────────────────────────────────────────────
    @FXML private Label kpiConversations, kpiGroupes, kpiMessages7j, kpiActifs;

    // ── Conversations table ───────────────────────────────────────
    @FXML private TableView<ConvRow>            tblConversations;
    @FXML private TableColumn<ConvRow, Integer> colId;
    @FXML private TableColumn<ConvRow, String>  colTitre;
    @FXML private TableColumn<ConvRow, String>  colType;
    @FXML private TableColumn<ConvRow, String>  colCreateur;
    @FXML private TableColumn<ConvRow, Integer> colNbParticipants;
    @FXML private TableColumn<ConvRow, Integer> colNbMessages;
    @FXML private TableColumn<ConvRow, String>  colDate;
    @FXML private TableColumn<ConvRow, Void>    colActions;

    // ── Top users (VBox populated in code) ───────────────────────
    @FXML private VBox vboxTopCreateurs;
    @FXML private VBox vboxTopCommunicateurs;

    // ── Charts ────────────────────────────────────────────────────
    @FXML private PieChart                  chartConvTypes;
    @FXML private BarChart<String, Number>  chartMsgJour;
    @FXML private PieChart                  chartMsgTypes;

    // ── Filters ───────────────────────────────────────────────────
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbType;

    private final List<ConvRow> allRows = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (cmbType != null) {
            cmbType.setItems(FXCollections.observableArrayList("Tous", "Groupe", "Privée"));
            cmbType.setValue("Tous");
            cmbType.valueProperty().addListener((o, old, nw) -> filterTable());
        }
        if (txtSearch != null)
            txtSearch.textProperty().addListener((o, old, nw) -> filterTable());

        setupColumns();
        refreshAll();
    }

    // ── Column setup ──────────────────────────────────────────────
    private void setupColumns() {
        colId            .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().id).asObject());
        colTitre         .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().titre));
        colType          .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().estGroupe ? "👥 Groupe" : "💬 Privée"));
        colCreateur      .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().createurNom));
        colNbParticipants.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().nbParticipants).asObject());
        colNbMessages    .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().nbMessages).asObject());
        colDate          .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().dateCreation != null ? d.getValue().dateCreation.toLocalDate().toString() : "—"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoir = new Button("👁 Voir");
            private final Button btnBan  = new Button("🚫 Bannir");
            private final Button btnSupp = new Button("🗑");
            private final HBox   box     = new HBox(4, btnVoir, btnBan, btnSupp);
            {
                btnVoir.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11;-fx-cursor:hand;-fx-padding:4 8;");
                btnBan .setStyle("-fx-background-color:#f59e0b;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11;-fx-cursor:hand;-fx-padding:4 8;");
                btnSupp.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11;-fx-cursor:hand;-fx-padding:4 8;");
                btnVoir.setOnAction(e -> ouvrirDetail(getTableView().getItems().get(getIndex())));
                btnBan .setOnAction(e -> banConversation(getTableView().getItems().get(getIndex())));
                btnSupp.setOnAction(e -> supprimerConversation(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ── Refresh ───────────────────────────────────────────────────
    @FXML private void onRefresh(ActionEvent e) { refreshAll(); }

    private void refreshAll() {
        loadKpi();
        loadConversations();
        loadTopCreateurs();
        loadTopCommunicateurs();
        loadCharts();
    }

    // ── KPI ───────────────────────────────────────────────────────
    private void loadKpi() {
        try (Connection cnx = MyDB2.getConnection()) {
            kpiConversations.setText(scalar(cnx, "SELECT COUNT(*) FROM conversation"));
            kpiGroupes      .setText(scalar(cnx, "SELECT COUNT(*) FROM conversation WHERE est_groupe = 1"));
            kpiMessages7j   .setText(scalar(cnx, "SELECT COUNT(*) FROM message WHERE date_envoi >= DATE_SUB(NOW(), INTERVAL 7 DAY)"));
            kpiActifs       .setText(scalar(cnx, "SELECT COUNT(DISTINCT id_user) FROM message WHERE date_envoi >= DATE_SUB(NOW(), INTERVAL 7 DAY)"));
        } catch (Exception e) {
            System.out.println("KPI error: " + e.getMessage());
        }
    }

    private String scalar(Connection cnx, String sql) throws SQLException {
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            return rs.next() ? String.valueOf(rs.getInt(1)) : "0";
        }
    }

    // ── Conversations ─────────────────────────────────────────────
    private void loadConversations() {
        allRows.clear();
        // FIX: table conversation_membres → conversation_user
        String sql = """
                SELECT c.id_conversation, c.titre, c.est_groupe, c.date_creation,
                       (SELECT CONCAT(u2.nom,' ',u2.prenom)
                        FROM message m2 JOIN user_app u2 ON m2.id_user = u2.id_user
                        WHERE m2.id_conversation = c.id_conversation
                        ORDER BY m2.date_envoi ASC LIMIT 1) AS initiateur,
                       COUNT(DISTINCT cu.id_user)   AS nb_participants,
                       COUNT(DISTINCT m.id_message) AS nb_messages
                FROM conversation c
                LEFT JOIN conversation_user cu ON c.id_conversation = cu.id_conversation
                LEFT JOIN message m            ON c.id_conversation = m.id_conversation
                GROUP BY c.id_conversation, c.titre, c.est_groupe, c.date_creation
                ORDER BY c.date_creation DESC
                """;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ConvRow r = new ConvRow();
                r.id             = rs.getInt("id_conversation");
                r.titre          = rs.getString("titre");
                r.estGroupe      = rs.getInt("est_groupe") == 1;
                String init      = rs.getString("initiateur");
                r.createurNom    = init != null ? init : "—";
                r.nbParticipants = rs.getInt("nb_participants");
                r.nbMessages     = rs.getInt("nb_messages");
                Timestamp ts     = rs.getTimestamp("date_creation");
                if (ts != null) r.dateCreation = ts.toLocalDateTime();
                allRows.add(r);
            }
        } catch (Exception e) {
            System.out.println("Conversations load error: " + e.getMessage());
        }
        filterTable();
    }

    private void filterTable() {
        String search = txtSearch != null ? txtSearch.getText().toLowerCase().trim() : "";
        String type   = cmbType  != null ? cmbType.getValue() : "Tous";
        tblConversations.setItems(FXCollections.observableArrayList(
                allRows.stream()
                        .filter(r -> search.isEmpty()
                                || r.titre.toLowerCase().contains(search)
                                || r.createurNom.toLowerCase().contains(search))
                        .filter(r -> "Tous".equals(type)
                                || ("Groupe".equals(type) && r.estGroupe)
                                || ("Privée".equals(type) && !r.estGroupe))
                        .toList()
        ));
    }

    // ── Top users ─────────────────────────────────────────────────
    private void loadTopCreateurs() {
        if (vboxTopCreateurs == null) return;
        vboxTopCreateurs.getChildren().clear();
        // FIX: table conversation_membres → conversation_user
        String sql = """
                SELECT u.id_user, u.nom, u.prenom, COUNT(DISTINCT cu.id_conversation) AS cnt
                FROM conversation_user cu
                JOIN user_app u ON cu.id_user = u.id_user
                GROUP BY u.id_user, u.nom, u.prenom
                ORDER BY cnt DESC LIMIT 5
                """;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int rank = 1;
            while (rs.next()) {
                addUserRow(vboxTopCreateurs, rank++,
                        rs.getString("nom"), rs.getString("prenom"),
                        rs.getInt("id_user"), rs.getInt("cnt"), "conv.", "#dbeafe", "#1d4ed8");
            }
        } catch (Exception e) {
            System.out.println("Top créateurs error: " + e.getMessage());
        }
    }

    private void loadTopCommunicateurs() {
        if (vboxTopCommunicateurs == null) return;
        vboxTopCommunicateurs.getChildren().clear();
        String sql = """
                SELECT u.id_user, u.nom, u.prenom, COUNT(m.id_message) AS cnt
                FROM message m
                JOIN user_app u ON m.id_user = u.id_user
                WHERE m.date_envoi >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                GROUP BY u.id_user, u.nom, u.prenom
                ORDER BY cnt DESC LIMIT 5
                """;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int rank = 1;
            while (rs.next()) {
                addUserRow(vboxTopCommunicateurs, rank++,
                        rs.getString("nom"), rs.getString("prenom"),
                        rs.getInt("id_user"), rs.getInt("cnt"), "msg.", "#d1fae5", "#065f46");
            }
        } catch (Exception e) {
            System.out.println("Top communicateurs error: " + e.getMessage());
        }
    }

    private void addUserRow(VBox container, int rank,
                            String nom, String prenom, int userId, int count,
                            String unit, String badgeBg, String badgeFg) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 14, 9, 14));
        row.setStyle("-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;");

        String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".";
        Label lblMedal = new Label(medal);
        lblMedal.setMinWidth(28);
        lblMedal.setStyle("-fx-font-size:15;");

        Label lblName = new Label(nom + " " + prenom);
        lblName.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Label lblCount = new Label(count + " " + unit);
        lblCount.setStyle("-fx-font-size:11;-fx-font-weight:700;"
                + "-fx-background-color:" + badgeBg + ";"
                + "-fx-text-fill:" + badgeFg + ";"
                + "-fx-background-radius:12;-fx-padding:2 9;");

        Button btnBan = new Button("🚫 Bannir");
        btnBan.setStyle("-fx-background-color:#fef2f2;-fx-text-fill:#dc2626;"
                + "-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;-fx-padding:3 9;");
        btnBan.setOnAction(e -> banUser(userId, nom + " " + prenom));

        row.getChildren().addAll(lblMedal, lblName, lblCount, btnBan);
        container.getChildren().add(row);
    }

    // ── Charts ────────────────────────────────────────────────────
    private void loadCharts() {
        loadChartConvTypes();
        loadChartMsgJour();
        loadChartMsgTypes();
    }

    private void loadChartConvTypes() {
        if (chartConvTypes == null) return;
        chartConvTypes.getData().clear();
        try (Connection cnx = MyDB2.getConnection()) {
            int priv  = Integer.parseInt(scalar(cnx, "SELECT COUNT(*) FROM conversation WHERE est_groupe = 0"));
            int group = Integer.parseInt(scalar(cnx, "SELECT COUNT(*) FROM conversation WHERE est_groupe = 1"));
            chartConvTypes.getData().addAll(
                    new PieChart.Data("Privées (" + priv  + ")", priv),
                    new PieChart.Data("Groupes (" + group + ")", group)
            );
        } catch (Exception e) {
            System.out.println("Chart conv types error: " + e.getMessage());
        }
    }

    private void loadChartMsgJour() {
        if (chartMsgJour == null) return;
        chartMsgJour.getData().clear();
        String sql = """
                SELECT DATE(date_envoi) AS jour, COUNT(*) AS cnt
                FROM message
                WHERE date_envoi >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                GROUP BY DATE(date_envoi)
                ORDER BY jour ASC
                """;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Messages");
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String jour = rs.getString("jour");
                if (jour != null && jour.length() == 10)
                    jour = jour.substring(8) + "/" + jour.substring(5, 7);
                series.getData().add(new XYChart.Data<>(jour, rs.getInt("cnt")));
            }
        } catch (Exception e) {
            System.out.println("Chart msg/jour error: " + e.getMessage());
        }
        chartMsgJour.getData().add(series);
    }

    private void loadChartMsgTypes() {
        if (chartMsgTypes == null) return;
        chartMsgTypes.getData().clear();
        String sql = """
                SELECT type_message, COUNT(*) AS cnt
                FROM message
                WHERE date_envoi >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                GROUP BY type_message
                ORDER BY cnt DESC
                """;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("type_message");
                if (type == null) type = "AUTRE";
                chartMsgTypes.getData().add(
                        new PieChart.Data(type + " (" + rs.getInt("cnt") + ")", rs.getInt("cnt")));
            }
        } catch (Exception e) {
            System.out.println("Chart msg types error: " + e.getMessage());
        }
    }

    // ── Navigation ────────────────────────────────────────────────
    private void ouvrirDetail(ConvRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminMessagerieDetail.fxml"));
            Parent root = loader.load();
            AdminMessagerieDetailController ctrl = loader.getController();
            ctrl.setConversation(row.id, row.titre, row.estGroupe);
            changeCenterTo(root, tblConversations);
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Actions ───────────────────────────────────────────────────
    private void banConversation(ConvRow row) {
        if (!confirm("Bannir conversation",
                "Supprimer tous les messages de « " + row.titre + " » ?")) return;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(
                     "DELETE FROM message WHERE id_conversation = ?")) {
            ps.setInt(1, row.id);
            int n = ps.executeUpdate();
            alert(Alert.AlertType.INFORMATION, "Succès", n + " message(s) supprimé(s).");
            refreshAll();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void supprimerConversation(ConvRow row) {
        if (!confirm("Supprimer conversation",
                "Supprimer définitivement « " + row.titre + " » et tous ses messages ?")) return;
        try (Connection cnx = MyDB2.getConnection()) {
            // FIX: table conversation_membres → conversation_user
            cnx.createStatement().execute(
                    "DELETE FROM message WHERE id_conversation = " + row.id);
            cnx.createStatement().execute(
                    "DELETE FROM conversation_user WHERE id_conversation = " + row.id);
            cnx.createStatement().execute(
                    "DELETE FROM conversation WHERE id_conversation = " + row.id);
            alert(Alert.AlertType.INFORMATION, "Succès", "Conversation supprimée.");
            refreshAll();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void banUser(int userId, String nomComplet) {
        if (!confirm("Bannir utilisateur",
                "Supprimer tous les messages de « " + nomComplet + " » dans toutes les conversations ?")) return;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(
                     "DELETE FROM message WHERE id_user = ?")) {
            ps.setInt(1, userId);
            int n = ps.executeUpdate();
            alert(Alert.AlertType.INFORMATION, "Utilisateur banni",
                    n + " message(s) supprimé(s) pour " + nomComplet + ".");
            refreshAll();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    static void changeCenterTo(Parent newRoot, Node anchor) {
        Scene scene = anchor.getScene();
        if (scene == null) return;
        if (scene.getRoot() instanceof BorderPane bp) {
            bp.setCenter(newRoot);
        } else {
            Node found = scene.lookup("#mainPaneUser");
            if (found instanceof BorderPane bp) {
                bp.setCenter(newRoot);
            } else {
                Stage stage = (Stage) scene.getWindow();
                stage.setScene(new Scene(newRoot, scene.getWidth(), scene.getHeight()));
            }
        }
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        return a.showAndWait().map(b -> b == ButtonType.OK).orElse(false);
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ── Data model ────────────────────────────────────────────────
    public static class ConvRow {
        public int           id;
        public String        titre;
        public boolean       estGroupe;
        public LocalDateTime dateCreation;
        public String        createurNom;
        public int           nbParticipants;
        public int           nbMessages;
    }
}