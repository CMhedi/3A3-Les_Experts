package controllers;

import Entities.Conversation;
import Entities.Message;
import Services.interfaces.ConversationDAO;
import Services.interfaces.MessageDAO;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class AdminMessagerieDetailController implements Initializable {

    @FXML private Label lblTitre;
    @FXML private Label lblType;
    @FXML private Label lblDateCreation;

    @FXML private TableView<Message> tblMessages;
    @FXML private TableColumn<Message, String> colMsgUser;
    @FXML private TableColumn<Message, String> colMsgType;
    @FXML private TableColumn<Message, String> colMsgContenu;
    @FXML private TableColumn<Message, String> colMsgStatut;
    @FXML private TableColumn<Message, String> colMsgDate;
    @FXML private TableColumn<Message, Void> colMsgActions;
    @FXML private Label lblNbMessages;

    @FXML private ListView<String> lstParticipants;
    @FXML private Label lblNbParticipants;

    @FXML private TextField txtAdminMsg;
    @FXML private Label lblComposerFeedback;

    private MessageDAO messageDAO;
    private ConversationDAO conversationDAO;

    private int conversationId;
    // Note: Ensure this ID exists in your 'utilisateur' table to avoid SQL errors
    private int ADMIN_USER_ID = 5011;

    private final ObservableList<Message> messages = FXCollections.observableArrayList();
    private final ObservableList<String> participants = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] AVATAR_COLORS = {"#22c55e","#3b82f6","#f59e0b","#ef4444","#8b5cf6","#06b6d4","#ec4899"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        messageDAO = new MessageDAO();
        conversationDAO = new ConversationDAO();
        setupMessagesTable();
        setupParticipantsList();
    }

    /**
     * Updated to handle the 'est_groupe' logic from image_1da319.png (0 or 1)
     */
    public void setConversation(int id, String titre, int estGroupe, String dateCreation) {
        this.conversationId = id;
        lblTitre.setText(titre);
        lblDateCreation.setText("Créée le " + dateCreation);

        boolean isGroup = (estGroupe == 1);
        lblType.setText(isGroup ? "GROUPE" : "PRIVE");
        String bg = isGroup ? "#3b82f6" : "#22c55e";

        lblType.setStyle("-fx-font-size:11;-fx-text-fill:white;" +
                "-fx-background-color:" + bg + ";-fx-background-radius:10;-fx-padding:2 8;");

        loadMessages();
        loadParticipants();
    }

    private void setupMessagesTable() {
        // User Column
        colMsgUser.setCellValueFactory(cd -> new SimpleStringProperty("User #" + cd.getValue().getIdUser()));
        colMsgUser.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                String label = item;
                HBox box = new HBox(7);
                box.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().addAll(makeAvatar(label, 24), makeLabel(label));
                setGraphic(box); setText(null);
            }
        });

        // Type Column
        colMsgType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTypeMessage()));
        colMsgType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label b = new Label(item);
                String bg = switch (item.toUpperCase()) {
                    case "VOCAL"  -> "#f59e0b";
                    case "VIDEO"  -> "#8b5cf6";
                    case "FICHIER"-> "#06b6d4";
                    default       -> "#64748b";
                };
                b.setStyle("-fx-background-color:" + bg + "22;-fx-text-fill:" + bg + ";" +
                        "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;-fx-font-weight:bold;");
                setGraphic(b); setText(null); setAlignment(Pos.CENTER);
            }
        });

        // Contenu Column
        colMsgContenu.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getContenu()));
        colMsgContenu.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setWrapText(true);
                setText(item);
            }
        });

        // Statut Column
        colMsgStatut.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatutMessage()));
        colMsgStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String color = "LU".equals(item) ? "#22c55e" : "#3b82f6";
                Label b = new Label(item);
                b.setStyle("-fx-background-color:" + color + "22;-fx-text-fill:" + color + ";-fx-background-radius:10;-fx-padding:2 8;");
                setGraphic(b); setText(null); setAlignment(Pos.CENTER);
            }
        });

        // Date Column
        colMsgDate.setCellValueFactory(cd -> {
            LocalDateTime dt = cd.getValue().getDateEnvoi();
            return new SimpleStringProperty(dt != null ? dt.format(FMT) : "");
        });

        // Action Column
        colMsgActions.setCellFactory(col -> new TableCell<>() {
            final Button btnDel = new Button("🗑");
            {
                btnDel.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-cursor:hand;");
                btnDel.setOnAction(e -> {
                    Message m = getTableRow().getItem();
                    if (m != null && messageDAO.deleteMessage(m.getIdMessage())) loadMessages();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnDel);
            }
        });

        tblMessages.setItems(messages);
    }

    private void setupParticipantsList() {
        lstParticipants.setItems(participants);
        lstParticipants.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) { setGraphic(null); setText(null); return; }
                HBox box = new HBox(9);
                box.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().addAll(makeAvatar(name, 26), makeLabel(name));
                setGraphic(box);
            }
        });
    }

    private void loadMessages() {
        messages.clear();
        List<Message> list = messageDAO.getMessagesByConversation(conversationId);
        if (list != null) {
            list.sort(Comparator.comparing(Message::getDateEnvoi));
            messages.setAll(list);
        }
        lblNbMessages.setText(messages.size() + " message(s)");
        if (!messages.isEmpty()) Platform.runLater(() -> tblMessages.scrollTo(messages.size() - 1));
    }

    private void loadParticipants() {
        participants.clear();
        try {
            List<String> list = conversationDAO.getMembresByConversation(conversationId);
            if (list != null) participants.setAll(list);
        } catch (Exception e) { e.printStackTrace(); }
        lblNbParticipants.setText(participants.size() + " participant(s)");
    }

    @FXML private void onRetour() {
        ((Stage) lblTitre.getScene().getWindow()).close();
    }

    @FXML private void onEnvoyerMessage() {
        String text = txtAdminMsg.getText().trim();
        if (text.isEmpty()) return;

        Message msg = new Message("TEXTE", text, "ENVOYE", LocalDateTime.now(), conversationId, ADMIN_USER_ID);
        if (messageDAO.addMessage(msg)) {
            txtAdminMsg.clear();
            loadMessages();
            setFeedback("✔ Envoyé", "#22c55e");
        }
    }

    private void setFeedback(String text, String color) {
        lblComposerFeedback.setText(text);
        lblComposerFeedback.setStyle("-fx-text-fill:" + color + ";");
    }

    private Label makeAvatar(String name, int size) {
        String letter = (name == null || name.isBlank()) ? "?" : name.substring(0, 1).toUpperCase();
        int idx = Math.abs(name.hashCode()) % AVATAR_COLORS.length;
        Label av = new Label(letter);
        av.setStyle("-fx-background-color:" + AVATAR_COLORS[idx] + ";-fx-text-fill:white;-fx-background-radius:50;" +
                "-fx-min-width:" + size + ";-fx-min-height:" + size + ";-fx-alignment:center;-fx-font-weight:bold;");
        return av;
    }

    private Label makeLabel(String text) {
        return new Label(text);
    }

    public void setConversation(int id, String titre, String type, String dateCreation) {
    }
}