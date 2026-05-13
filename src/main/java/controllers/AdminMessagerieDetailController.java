package controllers;

import Utiles.MyDB2;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminMessagerieDetailController {

    // ── Header ────────────────────────────────────────────────────
    @FXML private Label lblTitre, lblType, lblDateCreation;
    @FXML private Label lblNbMessages, lblNbParticipants;

    // ── Messages table ────────────────────────────────────────────
    @FXML private TableView<MsgRow>           tblMessages;
    @FXML private TableColumn<MsgRow, String> colMsgUser;
    @FXML private TableColumn<MsgRow, String> colMsgType;
    @FXML private TableColumn<MsgRow, String> colMsgContenu;
    @FXML private TableColumn<MsgRow, String> colMsgStatut;
    @FXML private TableColumn<MsgRow, String> colMsgDate;
    @FXML private TableColumn<MsgRow, Void>   colMsgActions;

    // ── Participants list ─────────────────────────────────────────
    @FXML private ListView<String> lstParticipants;

    // ── Admin composer ────────────────────────────────────────────
    @FXML private TextField txtAdminMsg;
    @FXML private Label     lblComposerFeedback;

    private int     conversationId;
    private String  conversationTitre;
    private boolean estGroupe;
    private Timeline autoRefreshTimeline;

    // ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupColumns();
    }

    public void setConversation(int id, String titre, boolean estGroupe) {
        this.conversationId    = id;
        this.conversationTitre = titre;
        this.estGroupe         = estGroupe;
        if (lblTitre != null) lblTitre.setText(titre);
        if (lblType  != null) lblType.setText(estGroupe ? "👥 Groupe" : "💬 Conversation privée");
        loadConversationDate();
        refreshAll();
        startAutoRefresh();
    }

    // ── Columns ───────────────────────────────────────────────────
    private void setupColumns() {
        colMsgUser   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().userNom));
        colMsgType   .setCellValueFactory(d -> new SimpleStringProperty(typeLabel(d.getValue().typeMessage)));
        colMsgContenu.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().contenuResume));
        colMsgStatut .setCellValueFactory(d -> new SimpleStringProperty(statutLabel(d.getValue().statutMessage)));
        colMsgDate   .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().dateEnvoi != null
                        ? d.getValue().dateEnvoi.toString().replace("T", " ").substring(0, 16) : ""));

        // Colour statut column
        colMsgStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String raw = getTableView().getItems().get(getIndex()).statutMessage;
                setStyle(switch (raw == null ? "" : raw) {
                    case "LU"       -> "-fx-text-fill:#059669;-fx-font-weight:bold;";
                    case "SUPPRIME" -> "-fx-text-fill:#dc2626;-fx-font-weight:bold;";
                    default         -> "-fx-text-fill:#2563eb;-fx-font-weight:bold;";
                });
            }
        });

        // Colour type column
        colMsgType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String raw = getTableView().getItems().get(getIndex()).typeMessage;
                setStyle(switch (raw == null ? "" : raw) {
                    case "IMAGE"                    -> "-fx-text-fill:#2563eb;";
                    case "VIDEO", "APPEL_VIDEO"     -> "-fx-text-fill:#059669;";
                    case "AUDIO", "VOCALE",
                         "APPEL_AUDIO"              -> "-fx-text-fill:#0891b2;";
                    case "PDF"                      -> "-fx-text-fill:#dc2626;";
                    case "GIF"                      -> "-fx-text-fill:#9333ea;";
                    default                         -> "-fx-text-fill:#475569;";
                });
            }
        });

        colMsgActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnSupp = new Button("🗑 Supprimer");
            {
                btnSupp.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11;-fx-cursor:hand;-fx-padding:4 8;");
                btnSupp.setOnAction(e -> supprimerMessage(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnSupp);
            }
        });
    }

    // ── Load data ─────────────────────────────────────────────────
    private void refreshAll() {
        loadMessages();
        loadParticipants();
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> refreshAll()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    private void loadConversationDate() {
        String sql = "SELECT date_creation FROM conversation WHERE id_conversation = ?";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && lblDateCreation != null) {
                    Timestamp ts = rs.getTimestamp("date_creation");
                    if (ts != null)
                        lblDateCreation.setText("Créée le " + ts.toLocalDateTime().toLocalDate());
                }
            }
        } catch (Exception e) {
            System.out.println("Conv date error: " + e.getMessage());
        }
    }

    private void loadMessages() {
        List<MsgRow> list = new ArrayList<>();
        String sql = """
                SELECT m.id_message, m.type_message, m.contenu, m.statut_message,
                       m.date_envoi, m.date_lecture, u.nom, u.prenom
                FROM message m
                JOIN user_app u ON m.id_user = u.id_user
                WHERE m.id_conversation = ?
                ORDER BY m.date_envoi ASC
                """;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MsgRow r = new MsgRow();
                    r.id            = rs.getInt("id_message");
                    r.typeMessage   = rs.getString("type_message");
                    r.contenu       = rs.getString("contenu");
                    r.statutMessage = rs.getString("statut_message");
                    Timestamp ts = rs.getTimestamp("date_envoi");
                    if (ts != null) r.dateEnvoi = ts.toLocalDateTime();
                    String nom    = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    r.userNom = ((nom != null ? nom : "") + " " + (prenom != null ? prenom : "")).trim();
                    String c = r.contenu != null ? r.contenu : "";
                    r.contenuResume = c.length() > 70 ? c.substring(0, 67) + "…" : c;
                    list.add(r);
                }
            }
        } catch (Exception e) {
            System.out.println("Messages load error: " + e.getMessage());
        }
        tblMessages.setItems(FXCollections.observableArrayList(list));
        if (lblNbMessages != null) lblNbMessages.setText(list.size() + " message(s)");
    }

    private void loadParticipants() {
        if (lstParticipants == null) return;
        lstParticipants.getItems().clear();
        // FIX: table conversation_membres → conversation_user
        String sql = """
                SELECT u.nom, u.prenom
                FROM conversation_user cu
                JOIN user_app u ON cu.id_user = u.id_user
                WHERE cu.id_conversation = ?
                ORDER BY u.nom
                """;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    lstParticipants.getItems().add(
                            "👤  " + rs.getString("nom") + " " + rs.getString("prenom"));
                    count++;
                }
                if (lblNbParticipants != null)
                    lblNbParticipants.setText(count + " participant(s)");
            }
        } catch (Exception e) {
            System.out.println("Participants load error: " + e.getMessage());
            if (lblNbParticipants != null) lblNbParticipants.setText("0 participant(s)");
        }
    }

    // ── Actions ───────────────────────────────────────────────────
    private void supprimerMessage(MsgRow row) {
        if (!confirm("Supprimer message", "Supprimer ce message de " + row.userNom + " ?")) return;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(
                     "DELETE FROM message WHERE id_message = ?")) {
            ps.setInt(1, row.id);
            ps.executeUpdate();
            loadMessages();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void onEnvoyerMessage(ActionEvent e) {
        if (txtAdminMsg == null) return;
        String contenu = txtAdminMsg.getText().trim();
        if (contenu.isEmpty()) {
            setFeedback("⚠ Le message ne peut pas être vide.", "#d97706");
            return;
        }

        Entities.UserApp admin = Entities.Session.getConnectedUser();
        if (admin == null) {
            setFeedback("⚠ Aucun utilisateur connecté.", "#dc2626");
            return;
        }

        // FIX: provide date_envoi explicitly (was missing → "Field 'date_creation' doesn't have a default value"
        //      was a red-herring from another DAO; here we always supply NOW())
        String sql = "INSERT INTO message "
                   + "(type_message, contenu, statut_message, date_envoi, id_conversation, id_user) "
                   + "VALUES ('TEXTE', ?, 'ENVOYE', NOW(), ?, ?)";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, contenu);
            ps.setInt(2, conversationId);
            ps.setInt(3, admin.getIdUser());
            ps.executeUpdate();
            txtAdminMsg.clear();
            setFeedback("✓ Message envoyé.", "#059669");
            loadMessages();
        } catch (Exception ex) {
            setFeedback("✗ Erreur : " + ex.getMessage(), "#dc2626");
        }
    }

    private void setFeedback(String msg, String color) {
        if (lblComposerFeedback != null) {
            lblComposerFeedback.setText(msg);
            lblComposerFeedback.setStyle(
                    "-fx-font-size:12;-fx-padding:0 16 10 16;-fx-text-fill:" + color + ";");
        }
    }

    @FXML
    private void onBanConversation(ActionEvent e) {
        if (!confirm("Bannir conversation",
                "Supprimer tous les messages de « " + conversationTitre + " » ?")) return;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(
                     "DELETE FROM message WHERE id_conversation = ?")) {
            ps.setInt(1, conversationId);
            int n = ps.executeUpdate();
            alert(Alert.AlertType.INFORMATION, "Succès", n + " message(s) supprimé(s).");
            loadMessages();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    @FXML
    private void onDeleteConversation(ActionEvent e) {
        if (!confirm("Supprimer conversation",
                "Supprimer définitivement cette conversation et tous ses messages ?")) return;
        try (Connection cnx = MyDB2.getConnection()) {
            // FIX: table conversation_membres → conversation_user
            cnx.createStatement().execute(
                    "DELETE FROM message WHERE id_conversation = " + conversationId);
            cnx.createStatement().execute(
                    "DELETE FROM conversation_user WHERE id_conversation = " + conversationId);
            cnx.createStatement().execute(
                    "DELETE FROM conversation WHERE id_conversation = " + conversationId);
            onRetour(e);
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    @FXML
    private void onRetour(ActionEvent e) {
        try {
            stopAutoRefresh();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AdminMessagerie.fxml"));
            AdminMessagerieController.changeCenterTo(root, tblMessages);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── Display helpers ───────────────────────────────────────────
    private String typeLabel(String type) {
        if (type == null) return "TEXTE";
        return switch (type) {
            case "TEXTE", "TEXT" -> "💬 Texte";
            case "IMAGE"         -> "🖼 Image";
            case "VIDEO"         -> "🎬 Vidéo";
            case "AUDIO"         -> "🎵 Audio";
            case "VOCALE"        -> "🎙 Vocal";
            case "PDF"           -> "📄 PDF";
            case "GIF"           -> "🎭 GIF";
            case "EMOJI"         -> "😊 Emoji";
            case "APPEL_AUDIO"   -> "📞 Appel audio";
            case "APPEL_VIDEO"   -> "📹 Appel vidéo";
            default              -> type;
        };
    }

    private String statutLabel(String statut) {
        if (statut == null) return "ENVOYE";
        return switch (statut) {
            case "LU"       -> "✓✓ Lu";
            case "SUPPRIME" -> "✗ Supprimé";
            default         -> "✓ Envoyé";
        };
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ── Inner data model ──────────────────────────────────────────
    public static class MsgRow {
        public int           id;
        public String        typeMessage;
        public String        contenu;
        public String        contenuResume;
        public String        statutMessage;
        public LocalDateTime dateEnvoi;
        public String        userNom;
    }
}