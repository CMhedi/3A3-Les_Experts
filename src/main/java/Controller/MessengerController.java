package Controller;

import Services.interfaces.GeminiService;
import Services.interfaces.GifService;
import javafx.scene.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import Services.interfaces.MessageDAO;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;



import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.geometry.Insets;
import org.json.JSONArray;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import Services.interfaces.GifService;
import org.json.JSONArray;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import Entities.Message;
import Entities.Conversation;
import Services.interfaces.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import Utiles.AudioRecorder;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MessengerController implements Initializable {
    @FXML
    private Button recordButton;
    @FXML
    private Button aiRecordButton;


    private boolean isAiRecording = false;


    private AudioRecorder recorder;
    private boolean isRecording = false;
    private AudioRecorder aiRecorder;
    @FXML
    private HBox chatHeader;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<Conversation> conversationsList;
    @FXML
    private ListView<Message> messagesList;
    @FXML
    private TextField messageInput;
    @FXML
    private Label chatUserName;
    @FXML
    private Label chatStatus;
    @FXML
    private VBox botMessagesContainer;
    @FXML
    private TextField botInput;
    @FXML
    private VBox aiPanel;
    @FXML
    private ProgressIndicator aiLoader;

    private boolean aiVisible = true;

    public Services.interfaces.MessageDAO messageDAO;
    private ConversationDAO conversationDAO;
    private List<Conversation> allConversations;
    private int selectedConversationId = -1;
    private final int currentUserId = 3;


    private ObservableList<Message> chatMessages = FXCollections.observableArrayList();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        messageDAO = new MessageDAO();
        conversationDAO = new ConversationDAO();

        messagesList.setFocusTraversable(false);
        conversationsList.setFocusTraversable(false);

        setupConversationsList();
        setupMessagesList(); // El version el jdida louta
        loadConversations();
        setupSearchListener();
        startAutoRefresh();
        gifService = new GifService("Filp4GHzXpQubEthmUu744ozFrXl464m");
    }


    private String callChatBotAPI(String userInput) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> payload = Map.of("input", userInput);
            String requestBody = mapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/chatbot/query"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, String> result = mapper.readValue(response.body(), Map.class);
            return result.get("response");
        } catch (Exception e) {
            e.printStackTrace();
            return "Error contacting chatbot API.";
        }
    }

    @FXML
    private void toggleAI() {
        aiVisible = !aiVisible;

        aiPanel.setVisible(aiVisible);
        aiPanel.setManaged(aiVisible);
    }

    private void startAutoRefresh() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            if (selectedConversationId != -1) {
                messageDAO.markAsRead(selectedConversationId, currentUserId);
                loadMessages(selectedConversationId);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void setupConversationsList() {
        conversationsList.setCellFactory(lv -> new ListCell<Conversation>() {
            @Override
            protected void updateItem(Conversation conv, boolean empty) {
                super.updateItem(conv, empty);
                if (empty || conv == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String prefix = (conv.getEstGroupe() == 1) ? "👥 " : "👤 ";
                    setText(prefix + conv.getTitre());
                    setStyle("-fx-padding: 12; -fx-font-size: 14; -fx-background-color: transparent;");
                }
            }
        });

        conversationsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedConversationId = newVal.getIdConversation();
                chatUserName.setText(newVal.getTitre());
                messageDAO.markAsRead(selectedConversationId, currentUserId);
                loadMessages(selectedConversationId);
            }
        });
    }

    // --- MISE À JOUR : SetupMessagesList Modern & Emoji Color ---
    private void setupMessagesList() {
        messagesList.setCellFactory(lv -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox mainContainer = new VBox(3);

                    Text textNode = new Text(msg.getContenu());
                    textNode.setFont(Font.font("Segoe UI Emoji", 14)); // Force emoji color

                    TextFlow textFlow = new TextFlow(textNode);
                    textFlow.setMaxWidth(300);

                    // Style des Bulles Modernes
                    if (msg.getIdUser() == currentUserId) {
                        textNode.setFill(Color.WHITE);
                        textFlow.setStyle(
                                "-fx-background-color: linear-gradient(to bottom right, red, #00c6ff);" +
                                        "-fx-background-radius: 18 18 2 18;" +
                                        "-fx-padding: 10 14;"
                        );
                        mainContainer.setAlignment(Pos.CENTER_RIGHT);
                    } else {
                        textNode.setFill(Color.web("#050505"));
                        textFlow.setStyle(
                                "-fx-background-color: #efeff3;" +
                                        "-fx-background-radius: 18 18 18 2;" +
                                        "-fx-padding: 10 14;"
                        );
                        mainContainer.setAlignment(Pos.CENTER_LEFT);
                    }

                    // Time & Status
                    String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                    String status = (msg.getIdUser() == currentUserId && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : " ✓";
                    Label footer = new Label(time + (msg.getIdUser() == currentUserId ? status : ""));
                    footer.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7; -fx-padding: 0 5;");

                    mainContainer.getChildren().addAll(textFlow, footer);
                    setGraphic(mainContainer);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 12;");

                    // Context Menu (Edit/Delete)
                    setupMessageContextMenu(textFlow, msg);
                }
            }
        });
    }

    private void setupMessageContextMenu(TextFlow textFlow, Message msg) {
        ContextMenu menu = new ContextMenu();
        if (msg.getIdUser() == currentUserId) {
            MenuItem editItem = new MenuItem("Modifier ✏️");
            editItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(msg.getContenu());
                dialog.showAndWait().ifPresent(newVal -> {
                    if (!newVal.trim().isEmpty() && messageDAO.updateMessageContent(msg.getIdMessage(), newVal)) {
                        loadMessages(selectedConversationId);
                    }
                });
            });
            menu.getItems().add(editItem);
        }
        if (currentUserId == 2 || msg.getIdUser() == currentUserId) {
            MenuItem deleteItem = new MenuItem("Supprimer 🗑️");
            deleteItem.setOnAction(e -> {
                if (messageDAO.deleteMessage(msg.getIdMessage())) {
                    loadMessages(selectedConversationId);
                }
            });
            menu.getItems().add(deleteItem);
        }
        if (!menu.getItems().isEmpty()) {
            textFlow.setOnContextMenuRequested(e -> menu.show(textFlow, e.getScreenX(), e.getScreenY()));
        }
    }

    @FXML
    private void askBot() {
        String query = botInput.getText().trim();
        if (query.isEmpty()) return;

        addBotBubble(query, true); // Show user bubble
        botInput.clear();
        if (aiLoader != null) aiLoader.setVisible(true);

        // Run in background thread
        new Thread(() -> {
            try {
                // Call Gemini 2.5-Flash
                String response = GeminiService.askGemini(query);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    if (aiLoader != null) aiLoader.setVisible(false);
                    addBotBubble(response, false); // Show bot bubble
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (aiLoader != null) aiLoader.setVisible(false);
                    addBotBubble("⚠️ Problème de connexion avec Gemini!", false);
                });
            }
        }).start();
    }


    @FXML
    private void handleAttachFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier ou une image");

        // Filtres optionnels pour les images
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            // Pour l'instant on affiche juste le nom, tu pourras ajouter la logique d'envoi plus tard
            messageInput.setText("[Fichier: " + selectedFile.getName() + "]");
            System.out.println("Fichier sélectionné : " + selectedFile.getAbsolutePath());
        }
    }

    // Hadhi bech tzid el "Bulle" mte3 el message fel Assistant AI
    private void addBotBubble(String text, boolean isUser) {
        if (botMessagesContainer == null) return;

        // Création d'un Label pour le texte
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(220); // Bechk ma yokhorejch mel cadre
        label.setFont(Font.font("Segoe UI", 13));

        // Création d'un HBox pour l'alignement (Limin ou Lisar)
        HBox wrapper = new HBox(label);

        if (isUser) {
            // Style de l'utilisateur (Bleu clair, aligné à droite)
            label.setStyle("-fx-background-color: #e1f5fe; -fx-padding: 10; -fx-background-radius: 15 15 0 15; -fx-text-fill: #0d47a1;");
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            VBox.setMargin(wrapper, new Insets(5, 0, 5, 40)); // Marge à gauche pour décaler
        } else {
            // Style de l'IA (Blanc/Gris, aligné à gauche)
            label.setStyle("-fx-background-color: #ffffff; -fx-padding: 10; -fx-background-radius: 15 15 15 0; -fx-border-color: #efefef; -fx-text-fill: #333333;");
            wrapper.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(wrapper, new Insets(5, 40, 5, 0)); // Marge à droite pour décaler
        }

        // Ajouter le message dans le conteneur principal du bot
        botMessagesContainer.getChildren().add(wrapper);

        // Scroll automatique vers le bas si tu as un ScrollPane
        if (botMessagesContainer.getParent() instanceof ScrollPane) {
            ScrollPane sp = (ScrollPane) botMessagesContainer.getParent();
            sp.setVvalue(1.0);
        }
    }

    // Logic sghira mte3 réponsat (Tnajem tzid feha kima t7eb)
    private String getBotResponse(String input) {
        input = input.toLowerCase();
        if (input.contains("ahla") || input.contains("salut")) return "Ahla bik Ahmed! 😊 Kifech najem n3awnek?";
        if (input.contains("pidev") || input.contains("projet")) return "Bonne chance fel PiDev mte3kom! 🚀";
        if (input.contains("aide")) return "Najem n3awnek ta3raf kifech tzid membre wala tfasa5 chat.";
        return "Mazzelt neta3lem fi 7ajet jdida, ama fhemtek chnowa t7eb: " + input;
    }

    @FXML
    private void showEmojiPicker(ActionEvent event) {
        ContextMenu emojiMenu = new ContextMenu();
        emojiMenu.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        FlowPane flowPane = new FlowPane();
        flowPane.setPrefWidth(260);
        flowPane.setHgap(8);
        flowPane.setVgap(8);
        flowPane.setPadding(new Insets(12));

        String[] emojis = {
                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
                "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
                "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
                "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🔥", "✨", "🌟",
                "🚀", "💯", "👍", "👎", "👏", "🙌", "🙏", "✅", "❌", "😂"
        };

        for (String emoji : emojis) {
            Button emojiBtn = new Button(emoji);
            emojiBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3;");
            emojiBtn.setFont(Font.font("Segoe UI Emoji", 20));
            emojiBtn.setOnAction(e -> {
                messageInput.appendText(emoji);
                messageInput.requestFocus();
            });
            flowPane.getChildren().add(emojiBtn);
        }

        CustomMenuItem customMenuItem = new CustomMenuItem(flowPane);
        customMenuItem.setHideOnClick(false);
        emojiMenu.getItems().add(customMenuItem);

        Button sourceBtn = (Button) event.getSource();
        emojiMenu.show(sourceBtn, Side.TOP, 0, -10);
    }

    // --- LE RESTE DU CODE (SANS CHANGEMENT DE LOGIC) ---

    @FXML
    private void showChatOptions() {
        if (selectedConversationId == -1) return;
        Conversation selectedConv = conversationsList.getSelectionModel().getSelectedItem();
        if (selectedConv == null) return;

        ContextMenu contextMenu = new ContextMenu();
        if (selectedConv.getEstGroupe() == 1) {
            MenuItem showMembersItem = new MenuItem("Afficher les membres 👥");
            showMembersItem.setOnAction(e -> {
                List<String> membres = conversationDAO.getMembresByConversation(selectedConversationId);
                if (membres.stream().noneMatch(m -> m.toLowerCase().contains("ahmed"))) {
                    membres.add(0, "Ahmed Admin (Moi) ⭐");
                }
                ListView<String> membersListView = new ListView<>(FXCollections.observableArrayList(membres));
                membersListView.setPrefHeight(200);
                Dialog<Void> dialog = new Dialog<>();
                dialog.setTitle("Membres");
                dialog.getDialogPane().setContent(membersListView);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                dialog.showAndWait();
            });

            MenuItem addMemberItem = new MenuItem("Ajouter un membre ➕");
            addMemberItem.setOnAction(e -> {
                List<String> allUsers = conversationDAO.getAllAppUsers();
                List<String> currentMembres = conversationDAO.getMembresByConversation(selectedConversationId);
                List<String> filteredUsers = allUsers.stream()
                        .filter(u -> {
                            String nameInBase = u.split("\\|")[0].trim().toLowerCase();
                            boolean alreadyMember = currentMembres.stream().anyMatch(m -> m.toLowerCase().equals(nameInBase));
                            return !alreadyMember && !nameInBase.contains("ahmed");
                        })
                        .collect(Collectors.toList());

                if (filteredUsers.isEmpty()) {
                    new Alert(Alert.AlertType.INFORMATION, "Aucun utilisateur disponible !").show();
                    return;
                }

                ChoiceDialog<String> dialog = new ChoiceDialog<>(filteredUsers.get(0), filteredUsers);
                dialog.showAndWait().ifPresent(selectedUser -> {
                    String email = selectedUser.substring(selectedUser.lastIndexOf("|") + 2).trim();
                    if (conversationDAO.addMemberToConversation(selectedConversationId, email)) {
                        new Alert(Alert.AlertType.INFORMATION, "Ajouté !").show();
                    }
                });
            });
            contextMenu.getItems().addAll(showMembersItem, addMemberItem, new SeparatorMenuItem());
        }

        MenuItem editTitle = new MenuItem("Modifier le nom du chat ✏️");
        MenuItem deleteChat = new MenuItem("Supprimer la conversation 🗑️");
        editTitle.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(chatUserName.getText());
            dialog.showAndWait().ifPresent(newName -> {
                if (conversationDAO.updateConversationTitle(selectedConversationId, newName)) {
                    chatUserName.setText(newName);
                    loadConversations();
                }
            });
        });
        deleteChat.setOnAction(e -> {
            if (conversationDAO.deleteConversation(selectedConversationId)) {
                selectedConversationId = -1;
                chatUserName.setText("Sélectionnez un chat");
                messagesList.setItems(null);
                loadConversations();
            }
        });
        contextMenu.getItems().addAll(editTitle, deleteChat);
        contextMenu.show(chatHeader, Side.BOTTOM, 0, 0);
    }

    @FXML
    private void addNewConversation() {
        List<String> choices = List.of("Message Privé 👤", "Groupe 👥");
        ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Message Privé 👤", choices);
        typeDialog.showAndWait().ifPresent(type -> {
            if (type.contains("Privé")) {
                List<String> allUsers = conversationDAO.getAllAppUsers();
                List<String> existingPrivateChats = allConversations.stream()
                        .filter(c -> c.getEstGroupe() == 0)
                        .map(c -> c.getTitre().trim().toLowerCase())
                        .collect(Collectors.toList());
                List<String> availableUsers = allUsers.stream()
                        .filter(u -> {
                            String name = u.split("\\|")[0].trim().toLowerCase();
                            return !existingPrivateChats.contains(name) && !name.contains("ahmed");
                        })
                        .collect(Collectors.toList());
                if (availableUsers.isEmpty()) return;
                ChoiceDialog<String> userDialog = new ChoiceDialog<>(availableUsers.get(0), availableUsers);
                userDialog.showAndWait().ifPresent(selectedUser -> {
                    String name = selectedUser.split("\\|")[0].trim();
                    String email = selectedUser.substring(selectedUser.lastIndexOf("|") + 2).trim();
                    int newId = conversationDAO.addConversation(new Conversation(name, 0));
                    if (newId != -1) {
                        conversationDAO.addMemberToConversation(newId, currentUserId);
                        conversationDAO.addMemberToConversation(newId, email);
                        loadConversations();
                    }
                });
            } else {
                TextInputDialog nameInput = new TextInputDialog();
                nameInput.showAndWait().ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        int newId = conversationDAO.addConversation(new Conversation(name.trim(), 1));
                        if (newId != -1) {
                            conversationDAO.addMemberToConversation(newId, currentUserId);
                            loadConversations();
                        }
                    }
                });
            }
        });
    }

    @FXML
    private void sendMessage() {
        if (selectedConversationId == -1) return;
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message("TEXTE", text, "ENVOYE", LocalDateTime.now(), selectedConversationId, currentUserId);
        if (messageDAO.addMessage(m)) {
            messageInput.clear();
            loadMessages(selectedConversationId);
        }
    }

    @FXML
    private void sendLike() {
        if (selectedConversationId == -1) return;
        Message m = new Message("TEXTE", "❤️", "ENVOYE", LocalDateTime.now(), selectedConversationId, currentUserId);
        messageDAO.addMessage(m);
        loadMessages(selectedConversationId);
    }

    private void loadConversations() {
        allConversations = conversationDAO.getAllConversations(currentUserId);
        conversationsList.setItems(FXCollections.observableArrayList(allConversations));
    }

    private void loadMessages(int conversationId) {
        List<Message> messages = messageDAO.getMessagesByConversation(conversationId);
        messagesList.setItems(FXCollections.observableArrayList(messages));
        if (!messages.isEmpty()) messagesList.scrollTo(messages.size() - 1);
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, old, newVal) -> {
            List<Conversation> results = conversationDAO.searchConversations(newVal, currentUserId);
            conversationsList.setItems(FXCollections.observableArrayList(results));
        });
    }

    @FXML
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) sendMessage();
    }

    private void loadConversationHistory(int conversationId) {
        // Clear old messages
        chatMessages.clear();

        // Use DAO instead of raw SQL
        List<Message> messages = messageDAO.getMessagesByConversation(conversationId);
        chatMessages.setAll(messages);

        messagesList.setItems(chatMessages);
        if (!chatMessages.isEmpty()) messagesList.scrollTo(chatMessages.size() - 1);

        // Update AI panel with bot messages
        botMessagesContainer.getChildren().clear();
        for (Message msg : chatMessages) {
            if (msg.getIdUser() == 0) { // AI message
                Label aiLabel = new Label(msg.getContenu());
                aiLabel.setStyle("-fx-background-color: #e0f7fa; -fx-padding: 8; -fx-background-radius: 10;");
                botMessagesContainer.getChildren().add(aiLabel);
            }
        }
    }

    @FXML
    private void handleRecordButton() {
        try {
            if (!isRecording) {
                // Générer un nom unique pour chaque vocal
                String fileName = "voice_" + currentUserId + "_" + System.currentTimeMillis() + ".wav";
                String audioPath = "src/main/resources/uploads/audio/" + fileName;

                recorder = new AudioRecorder();
                recorder.startRecording(audioPath);

                recordButton.setText("Stop");
                isRecording = true;

                // Timeline pour arrêter automatiquement après 2 minutes
                Timeline stopTimeline = new Timeline(new KeyFrame(Duration.minutes(2), e -> {
                    if (isRecording) stopRecordingAndSave(audioPath);
                }));
                stopTimeline.setCycleCount(1);
                stopTimeline.play();

            } else {
                // Arrêt manuel
                String audioPath = recorder.getAudioFilePath(); // Ajouter getter dans AudioRecorder
                stopRecordingAndSave(audioPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processVoiceToText(String audioFilePath) {
        try {
            Model model = new Model("models/vosk-model-small-fr");
            Recognizer recognizer = new Recognizer(model, 16000);

            InputStream ais = new FileInputStream(audioFilePath);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = ais.read(buffer)) != -1) {
                recognizer.acceptWaveForm(buffer, bytesRead);
            }
            ais.close();

            String resultJson = recognizer.getFinalResult();

            ObjectMapper mapper = new ObjectMapper();
            String text = mapper.readTree(resultJson).get("text").asText();

            Platform.runLater(() -> {
                botInput.setText(text);
                askBot();
            });

            recognizer.close();
            model.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void stopRecordingAndSave(String audioPath) {
        try {
            recorder.stopRecording();
            recordButton.setText("🎙️");
            isRecording = false;

            // Ajouter le message vocal dans la base de données
            messageDAO.addVocalMessage("uploads/audio/" + new File(audioPath).getName(),
                    selectedConversationId, currentUserId);

            // Rafraîchir la conversation pour afficher le message vocal
            loadMessages(selectedConversationId);

            // --- Nouvelle ligne : transformer audio → texte ---
            processVoiceToText(audioPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleVoiceInput() {
        System.out.println("Micro cliqué !");
        // Plus tard, tu peux ajouter ici la logique pour la reconnaissance vocale
    }

    private Timeline recordingLimitTimer;

    @FXML
    private void handleAiRecord() {
        try {
            if (!isAiRecording) {
                // 1. Démarrer l'enregistrement
                aiRecorder = new AudioRecorder();

                // On utilise le dossier 'temp' de l'utilisateur pour éviter les conflits OneDrive
                String audioPath = System.getProperty("java.io.tmpdir") + File.separator + "ai_voice_" + System.currentTimeMillis() + ".wav";
                aiRecorder.startRecording(audioPath);

                isAiRecording = true;
                aiRecordButton.setText("⏹"); // Changer l'icône en bouton 'Stop'

                // 2. Lancer le minuteur de 2 minutes (120 secondes)
                recordingLimitTimer = new Timeline(new KeyFrame(Duration.seconds(120), e -> {
                    if (isAiRecording) {
                        System.out.println("Limite de 2 minutes atteinte. Arrêt automatique.");
                        handleAiRecord(); // Appelle récursivement pour déclencher la partie 'else'
                    }
                }));
                recordingLimitTimer.setCycleCount(1);
                recordingLimitTimer.play();

            } else {
                // 3. Arrêter l'enregistrement
                if (recordingLimitTimer != null) {
                    recordingLimitTimer.stop();
                }

                aiRecorder.stopRecording();
                isAiRecording = false;
                aiRecordButton.setText("🎙️"); // Retour à l'icône micro

                String audioFilePath = aiRecorder.getAudioFilePath();

                // 4. Conversion Voice → Text (Extraction du JSON)
                String rawJson = convertVoiceToText(audioFilePath);
                String userText = "";

                try {
                    // On extrait uniquement la valeur du champ "text"
                    ObjectMapper mapper = new ObjectMapper();
                    userText = mapper.readTree(rawJson).get("text").asText();
                } catch (Exception e) {
                    userText = rawJson; // Backup si ce n'est pas du JSON
                }

                if (userText == null || userText.trim().isEmpty()) {
                    addBotBubble("Désolé, je n'ai pas pu comprendre l'audio. Réessayez.", false);
                    return;
                }

                // 5. Affichage et appel à l'IA (Gemini)
                final String finalUserText = userText;
                addBotBubble(finalUserText, true); // Affiche la bulle utilisateur

                // On utilise un Thread pour ne pas bloquer l'interface
                new Thread(() -> {
                    try {
                        // Utilisation directe du service Gemini
                        String aiReply = GeminiService.askGemini(finalUserText);

                        // Mise à jour de l'UI sur le thread JavaFX
                        Platform.runLater(() -> addBotBubble(aiReply, false));
                    } catch (Exception e) {
                        Platform.runLater(() -> addBotBubble("⚠️ Erreur de connexion avec l'IA.", false));
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getAIResponse(String userText) {
        try {
            // Use your existing Gemini service instead of the failing localhost API
            return GeminiService.askGemini(userText);
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Error: " + e.getMessage();
        }
    }

    private String convertVoiceToText(String audioFilePath) {
        try {
            Model model = new Model("C:\\Users\\alakh\\OneDrive\\Bureau\\3A3-Les_Experts\\src\\main\\resources\\models\\vosk-model-small-en-us-0.15");
            try (Recognizer recognizer = new Recognizer(model, 16000)) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(audioFilePath));
                byte[] buffer = new byte[4096];
                int nbytes;
                while ((nbytes = ais.read(buffer)) >= 0) {
                    recognizer.acceptWaveForm(buffer, nbytes);
                }

                // Parse the JSON result
                String json = recognizer.getFinalResult();
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(json).get("text").asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @FXML
    private VBox chatVBox;

    private void addMessageToChat(String sender, String message) {
        Label label = new Label(sender + ": " + message);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #e1f5fe; -fx-padding: 8; -fx-background-radius: 10;");

        Platform.runLater(() -> {
            boolean isUser = sender.equalsIgnoreCase("You");
            chatVBox.getChildren().add(label);
            ScrollPane scrollPane = (ScrollPane) chatVBox.getParent();
            scrollPane.setVvalue(1.0);
            addBotBubble(message, isUser);
        });
    }

    private void processVoiceToTextForAI(String audioPath) {
        try {
            Model model = new Model("models/vosk-model-small-fr");
            Recognizer recognizer = new Recognizer(model, 16000);

            InputStream ais = new FileInputStream(audioPath);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = ais.read(buffer)) != -1) {
                recognizer.acceptWaveForm(buffer, bytesRead);
            }
            ais.close();

            String resultJson = recognizer.getFinalResult();

            ObjectMapper mapper = new ObjectMapper();
            String text = mapper.readTree(resultJson).get("text").asText();

            Platform.runLater(() -> {
                botInput.setText(text);
                askBot();   // teb3ath lel Gemini
            });

            recognizer.close();
            model.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private GifService gifService;

    @FXML
    private void openGifPicker() {
        Stage stage = new Stage();
        stage.setTitle("Search GIF");

        // Layout principal
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);

        // Barre de recherche
        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Tapez votre recherche ici...");
        searchField.setPrefWidth(320);

        Button searchButton = new Button("Rechercher");
        searchButton.setDefaultButton(true);
        searchBox.getChildren().addAll(searchField, searchButton);

        // Indicateur de chargement
        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setMaxSize(40, 40);

        // Zone d'affichage des GIFs
        FlowPane gifsPane = new FlowPane();
        gifsPane.setHgap(15);
        gifsPane.setVgap(15);
        gifsPane.setPadding(new Insets(10));
        gifsPane.setAlignment(Pos.CENTER);

        // ScrollPane
        ScrollPane scrollPane = new ScrollPane(gifsPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background-color:transparent;");

        root.getChildren().addAll(searchBox, progress, scrollPane);

        // Action du bouton recherche
        searchButton.setOnAction(e -> {
            String query = searchField.getText().trim();
            if (query.isEmpty()) return;

            gifsPane.getChildren().clear();
            progress.setVisible(true);

            new Thread(() -> {
                try {
                    GifService gifService = new GifService("Filp4GHzXpQubEthmUu744ozFrXl464m");
                    JSONArray gifs = gifService.searchGifs(query, 12);

                    Platform.runLater(() -> {
                        progress.setVisible(false);

                        if (gifs.length() == 0) {
                            gifsPane.getChildren().add(new Label("Aucun GIF trouvé pour : " + query));
                            return;
                        }

                        for (int i = 0; i < gifs.length(); i++) {
                            try {
                                JSONObject gifObj = gifs.getJSONObject(i);
                                String gifUrl = gifObj.getString("url");

                                // Affichage du GIF animé avec WebView
                                WebView web = new WebView();
                                web.setPrefSize(120, 120);
                                web.getEngine().load(gifUrl);
                                web.setCursor(Cursor.HAND);

                                // Sélection du GIF
                                String finalUrl = gifUrl;
                                web.setOnMouseClicked(ev -> {
                                    if (messageInput != null) {
                                        messageInput.appendText(" " + finalUrl);
                                    }
                                    stage.close();
                                });

                                gifsPane.getChildren().add(web);
                            } catch (Exception ex) {
                                System.err.println("Erreur sur le GIF #" + i + " : " + ex.getMessage());
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        progress.setVisible(false);
                        new Alert(Alert.AlertType.ERROR, "Erreur API : " + ex.getMessage()).show();
                    });
                }
            }).start();
        });

        Scene scene = new Scene(root, 500, 500);
        stage.setScene(scene);
        stage.show();
    }
}