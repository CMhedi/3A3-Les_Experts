package Controller;

import Entities.Conversation;
import Entities.Message;
import Services.interfaces.*;
import Utiles.AudioRecorder;
import Utiles.StompClientHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;




public class MessengerController implements Initializable {
    private static final String BASE_URL =null ;
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

    public MessageDAO messageDAO;
    private ConversationDAO conversationDAO;
    private List<Conversation> allConversations;
    private int selectedConversationId = -1;
    private int currentUserId = 3;  // valeur par défaut




    private String currentTargetLanguage = "fr"; // اللغة الافتراضية (نعرض بها الرسائل الأصلية)
    // conversationId -> projectId
    private Map<Integer, String> translatedCache = new HashMap<>(); // messageId -> texte traduit
    private final Set<Integer> translationInProgress = ConcurrentHashMap.newKeySet();
    private Set<Integer> lastFiveMessageIds = new HashSet<>();


    private ObservableList<Message> chatMessages = FXCollections.observableArrayList();
    private StompClientHandler stompHandler;
    private HttpClient httpClient;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        messageDAO = new MessageDAO();
        conversationDAO = new ConversationDAO();
        currentUserId = 3;
        messagesList.setFocusTraversable(false);
        conversationsList.setFocusTraversable(false);

        setupConversationsList();
        setupMessagesList(); // El version el jdida louta
        loadConversations();
        setupSearchListener();
        startAutoRefresh();
        gifService = new GifService("Filp4GHzXpQubEthmUu744ozFrXl464m");
        initializeStompClient();
        httpClient = HttpClient.newHttpClient();


// ✅ Add delay and null check
        if (stompHandler != null) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait for server
                    stompHandler.connect(
                            session -> System.out.println("✅ STOMP connecté"),
                            payload -> Platform.runLater(() -> processIncomingCall(payload))
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            System.err.println("❌ StompHandler initialization failed");
        }

    }

    private boolean requestUserLogin() {
        TextInputDialog dialog = new TextInputDialog("2");
        dialog.setTitle("👤 User Login");
        dialog.setHeaderText("Which user are you?");
        dialog.setContentText("Enter User ID (2, 3, 4, etc.):");
        dialog.setGraphic(null);

        var result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                currentUserId = Integer.parseInt(result.get().trim());
                System.out.println("✅ Logged in as User ID: " + currentUserId);
                return true;
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid User ID");
                alert.setContentText("Please enter a valid number!");
                alert.showAndWait();
                return requestUserLogin(); // Ask again
            }
        }
        return false;
    }
    private void processIncomingCall(String payload) {
        try {
            System.out.println("📞 Incoming call received: " + payload);

            // Parse the JSON payload
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> callData = mapper.readValue(payload, Map.class);

            String senderName = (String) callData.get("senderName");
            int senderId = ((Number) callData.get("senderId")).intValue();
            int conversationId = ((Number) callData.get("conversationId")).intValue();
            String callType = (String) callData.get("type");

            System.out.println("📱 Call from: " + senderName + " (Type: " + callType + ")");

            // ✅ Show incoming call window
            onIncomingCallReceived(senderName, senderId, conversationId, callType);

        } catch (Exception e) {
            System.err.println("❌ Error processing incoming call: " + e.getMessage());
            e.printStackTrace();
        }
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

    // ========== REMPLACER COMPLÈTEMENT setupMessagesList() DANS MessengerController ==========

    private void setupMessagesList() {
        messagesList.setCellFactory(lv -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox mainContainer = new VBox(5);
                    mainContainer.setPadding(new Insets(8, 12, 8, 12));

                    // ✅ CAS 1 : MESSAGES D'APPEL AUDIO
                    if ("AUDIO_CALL".equals(msg.getTypeMessage())) {
                        displayAudioCallMessage(mainContainer, msg);
                        mainContainer.setAlignment(Pos.CENTER_LEFT);
                    }

                    // ✅ CAS 2 : MESSAGES D'APPEL VIDÉO
                    else if ("VIDEO_CALL".equals(msg.getTypeMessage())) {
                        displayVideoCallMessage(mainContainer, msg);
                        mainContainer.setAlignment(Pos.CENTER_LEFT);
                    }

                    // ✅ CAS 3 : MESSAGES VOCAUX
                    else if ("VOCAL".equals(msg.getTypeMessage())) {
                        displayVocalMessage(mainContainer, msg);
                        if (msg.getIdUser() == currentUserId) {
                            mainContainer.setAlignment(Pos.CENTER_RIGHT);
                        } else {
                            mainContainer.setAlignment(Pos.CENTER_LEFT);
                        }
                    }

                    // ✅ CAS 4 : MESSAGES TEXTE
                    else {
                        displayTextMessage(mainContainer, msg);
                        if (msg.getIdUser() == currentUserId) {
                            mainContainer.setAlignment(Pos.CENTER_RIGHT);
                        } else {
                            mainContainer.setAlignment(Pos.CENTER_LEFT);
                        }
                    }

                    setGraphic(mainContainer);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 0;");

                    // Context Menu (Edit/Delete)
                    if (!("AUDIO_CALL".equals(msg.getTypeMessage()) || "VIDEO_CALL".equals(msg.getTypeMessage()))) {
                        VBox content = (VBox) mainContainer.getChildren().get(0);
                        if (content.getChildren().get(0) instanceof TextFlow) {
                            setupMessageContextMenu((TextFlow) content.getChildren().get(0), msg);
                        }
                    }
                }
            }

            // ✅ AFFICHER UN MESSAGE D'APPEL AUDIO
            private void displayAudioCallMessage(VBox container, Message msg) {
                // Conteneur principal de l'appel
                HBox callBox = new HBox(12);
                callBox.setStyle(
                        "-fx-background-color: #f5f5f5;" +
                                "-fx-background-radius: 12;" +
                                "-fx-padding: 12;"
                );
                callBox.setAlignment(Pos.CENTER_LEFT);

                // Icône d'appel
                Label callIcon = new Label("📞");
                callIcon.setFont(Font.font("Segoe UI Emoji", 28));
                callIcon.setMinWidth(40);

                // Contenu texte
                VBox textBox = new VBox(4);
                textBox.setPrefWidth(200);

                // Titre : "Appel vocal"
                Label callTypeLabel = new Label("Appel vocal");
                callTypeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                callTypeLabel.setTextFill(Color.web("#050505"));

                // Durée et statut de l'appel
                String callStatus = extractCallStatus(msg.getContenu());
                String callDuration = extractCallDuration(msg.getContenu());
                Label statusLabel = new Label(callStatus + " • " + callDuration);
                statusLabel.setFont(Font.font("Segoe UI", 12));
                statusLabel.setTextFill(Color.web("#7a7a7a"));

                textBox.getChildren().addAll(callTypeLabel, statusLabel);

                // Bouton "Rappeler"
                Button redialButton = new Button("Rappeler");
                redialButton.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: #007AFF;" +
                                "-fx-font-size: 12;" +
                                "-fx-padding: 6 12;" +
                                "-fx-cursor: hand;"
                );
                redialButton.setOnAction(e -> {
                    System.out.println("📞 Rappel automatique...");
                    // Déclencher un nouvel appel
                    startAudioCall();
                });

                // Heure
                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                Label timeLabel = new Label(time);
                timeLabel.setFont(Font.font("Segoe UI", 11));
                timeLabel.setTextFill(Color.web("#999999"));

                // VBox verticale pour les textes + bouton
                VBox rightBox = new VBox(6);
                rightBox.setAlignment(Pos.CENTER_LEFT);
                rightBox.getChildren().addAll(textBox, redialButton);

                callBox.getChildren().addAll(callIcon, rightBox);

                // Ajouter au container avec l'heure
                VBox mainCall = new VBox(3);
                mainCall.getChildren().add(callBox);
                mainCall.getChildren().add(timeLabel);

                container.getChildren().add(mainCall);
            }

            // ✅ AFFICHER UN MESSAGE D'APPEL VIDÉO
            private void displayVideoCallMessage(VBox container, Message msg) {
                // Conteneur principal de l'appel
                HBox callBox = new HBox(12);
                callBox.setStyle(
                        "-fx-background-color: #f0f8ff;" +
                                "-fx-background-radius: 12;" +
                                "-fx-padding: 12;"
                );
                callBox.setAlignment(Pos.CENTER_LEFT);

                // Icône d'appel vidéo
                Label callIcon = new Label("📹");
                callIcon.setFont(Font.font("Segoe UI Emoji", 28));
                callIcon.setMinWidth(40);

                // Contenu texte
                VBox textBox = new VBox(4);
                textBox.setPrefWidth(200);

                // Titre : "Appel vidéo"
                Label callTypeLabel = new Label("Appel vidéo");
                callTypeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                callTypeLabel.setTextFill(Color.web("#1976D2"));

                // Durée et statut de l'appel
                String callStatus = extractCallStatus(msg.getContenu());
                String callDuration = extractCallDuration(msg.getContenu());
                Label statusLabel = new Label(callStatus + " • " + callDuration);
                statusLabel.setFont(Font.font("Segoe UI", 12));
                statusLabel.setTextFill(Color.web("#7a7a7a"));

                textBox.getChildren().addAll(callTypeLabel, statusLabel);

                // Bouton "Rappeler"
                Button redialButton = new Button("Rappeler");
                redialButton.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: #007AFF;" +
                                "-fx-font-size: 12;" +
                                "-fx-padding: 6 12;" +
                                "-fx-cursor: hand;"
                );
                redialButton.setOnAction(e -> {
                    System.out.println("📹 Rappel vidéo automatique...");
                    // TODO: Déclencher un nouvel appel vidéo
                    startVideoCall();
                });

                // Heure
                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                Label timeLabel = new Label(time);
                timeLabel.setFont(Font.font("Segoe UI", 11));
                timeLabel.setTextFill(Color.web("#999999"));

                // VBox verticale pour les textes + bouton
                VBox rightBox = new VBox(6);
                rightBox.setAlignment(Pos.CENTER_LEFT);
                rightBox.getChildren().addAll(textBox, redialButton);

                callBox.getChildren().addAll(callIcon, rightBox);

                // Ajouter au container avec l'heure
                VBox mainCall = new VBox(3);
                mainCall.getChildren().add(callBox);
                mainCall.getChildren().add(timeLabel);

                container.getChildren().add(mainCall);
            }

            // ✅ AFFICHER UN MESSAGE VOCAL
            private void displayVocalMessage(VBox container, Message msg) {
                Text textNode = new Text(msg.getContenu());
                textNode.setFont(Font.font("Segoe UI Emoji", 14));
                TextFlow textFlow = new TextFlow(textNode);
                textFlow.setMaxWidth(300);

                if (msg.getIdUser() == currentUserId) {
                    textNode.setFill(Color.WHITE);
                    textFlow.setStyle(
                            "-fx-background-color: linear-gradient(to bottom right, red, #00c6ff);" +
                                    "-fx-background-radius: 18 18 2 18;" +
                                    "-fx-padding: 10 14;"
                    );
                } else {
                    textNode.setFill(Color.web("#050505"));
                    textFlow.setStyle(
                            "-fx-background-color: #efeff3;" +
                                    "-fx-background-radius: 18 18 18 2;" +
                                    "-fx-padding: 10 14;"
                    );
                }

                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                String status = (msg.getIdUser() == currentUserId && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : " ✓";
                Label footer = new Label(time + (msg.getIdUser() == currentUserId ? status : ""));
                footer.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7; -fx-padding: 0 5;");

                VBox messageBox = new VBox(3);
                messageBox.getChildren().addAll(textFlow, footer);
                container.getChildren().add(messageBox);
            }

            private void displayTextMessage(VBox container, Message msg) {
                String originalText = msg.getContenu();
                String displayText = originalText;

                // Si la langue cible n'est pas le français (langue source)
                if (!"fr".equals(currentTargetLanguage)) {
                    int msgId = msg.getIdMessage();
                    String targetLang = currentTargetLanguage;

                    // Ne traduire que si le message fait partie des 5 derniers
                    if (lastFiveMessageIds.contains(msgId)) {
                        // 1. Si déjà en cache, on affiche directement la traduction
                        if (translatedCache.containsKey(msgId)) {
                            displayText = translatedCache.get(msgId);
                        }
                        // 2. Sinon, si une traduction n'est pas déjà en cours, on la lance
                        else if (translationInProgress.add(msgId)) {
                            displayText = originalText + " ⏳"; // affichage temporaire

                            new Thread(() -> {
                                try {
                                    String translated = GeminiService.translate(originalText, targetLang);
                                    Platform.runLater(() -> {
                                        translatedCache.put(msgId, translated);
                                        translationInProgress.remove(msgId);
                                        refreshSingleMessage(msgId);
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Platform.runLater(() -> {
                                        // Fallback : afficher le texte original avec un indicateur d'erreur
                                        translatedCache.put(msgId, originalText + " ⚠️");
                                        translationInProgress.remove(msgId);
                                        refreshSingleMessage(msgId);
                                    });
                                }
                            }).start();
                        } else {
                            // Traduction déjà en cours pour ce message
                            displayText = originalText + " ⏳";
                        }
                    }
                }

                // Construction de l'affichage du message (bulle) avec displayText
                Text textNode = new Text(displayText);
                textNode.setFont(Font.font("Segoe UI Emoji", 14));
                TextFlow textFlow = new TextFlow(textNode);
                textFlow.setMaxWidth(300);

                if (msg.getIdUser() == currentUserId) {
                    textNode.setFill(Color.WHITE);
                    textFlow.setStyle(
                            "-fx-background-color: linear-gradient(to bottom right, red, #00c6ff);" +
                                    "-fx-background-radius: 18 18 2 18;" +
                                    "-fx-padding: 10 14;"
                    );
                } else {
                    textNode.setFill(Color.web("#050505"));
                    textFlow.setStyle(
                            "-fx-background-color: #efeff3;" +
                                    "-fx-background-radius: 18 18 18 2;" +
                                    "-fx-padding: 10 14;"
                    );
                }

                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                String status = (msg.getIdUser() == currentUserId && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : " ✓";
                Label footer = new Label(time + (msg.getIdUser() == currentUserId ? status : ""));
                footer.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7; -fx-padding: 0 5;");

                VBox messageBox = new VBox(3);
                messageBox.getChildren().addAll(textFlow, footer);
                container.getChildren().add(messageBox);
            }


            // ✅ EXTRAIRE LE STATUT DE L'APPEL
            private String extractCallStatus(String content) {
                // Format : "📞 Appel audio - ACCEPTÉ (15s)"
                if (content.contains("ACCEPTÉ")) return "Appel accepté";
                if (content.contains("REJETÉ")) return "Appel rejeté";
                if (content.contains("NON_RÉPONDU")) return "Appel non répondu";
                return "Appel";
            }

            // ✅ EXTRAIRE LA DURÉE DE L'APPEL
            private String extractCallDuration(String content) {
                // Format : "📞 Appel audio - ACCEPTÉ (15s)"
                try {
                    int startIdx = content.lastIndexOf("(");
                    int endIdx = content.lastIndexOf(")");
                    if (startIdx != -1 && endIdx != -1) {
                        return content.substring(startIdx + 1, endIdx);
                    }
                } catch (Exception e) {
                    // Ignorer l'erreur
                }
                return "0s";
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
        // Vérifications préalables
        if (selectedConversationId == -1) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner une conversation d'abord !");
            alert.showAndWait();
            return;

        }
        Conversation selectedConv = conversationsList.getSelectionModel().getSelectedItem();
        if (selectedConv == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setHeaderText(null);
            alert.setContentText("Aucune conversation sélectionnée !");
            alert.showAndWait();
            return;
        }

        // Créer le menu contextuel
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-font-size: 13;");

        // ========== CAS 1 : SI C'EST UN GROUPE (EstGroupe == 1) ==========
        if (selectedConv.getEstGroupe() == 1) {

            // ===== OPTION 1.1 : Afficher les membres =====
            MenuItem showMembersItem = new MenuItem("Afficher les membres 👥");
            showMembersItem.setOnAction(e -> {
                try {
                    List<String> membres = conversationDAO.getMembresByConversation(selectedConversationId);

                    if (membres == null || membres.isEmpty()) {
                        Alert emptyAlert = new Alert(Alert.AlertType.INFORMATION);
                        emptyAlert.setTitle("Aucun membre");
                        emptyAlert.setHeaderText(null);
                        emptyAlert.setContentText("Cette conversation n'a pas de membres !");
                        emptyAlert.showAndWait();
                        return;
                    }

                    // ✅ CORRECTION : Ajouter "Vous" si l'utilisateur actuel est dans la liste
                    // Vérifier le format des données retournées par getMembresByConversation()
                    boolean currentUserExists = membres.stream().anyMatch(m -> {
                        try {
                            // Si le format est "id|nom|email"
                            if (m.contains("|")) {
                                String[] parts = m.split("\\|");
                                if (parts.length > 0 && parts[0].matches("\\d+")) {
                                    return Integer.parseInt(parts[0].trim()) == currentUserId;
                                }
                            }
                            // Sinon essayer de matcher avec currentUserId comme string
                            return m.contains(String.valueOf(currentUserId));
                        } catch (Exception ex) {
                            return false;
                        }
                    });

                    // Ajouter l'indication "Vous" si trouvé
                    if (currentUserExists) {
                        // Mapper les membres pour ajouter l'indication "Vous"
                        List<String> membresWithYou = new ArrayList<>(membres);
                        membresWithYou = membresWithYou.stream()
                                .map(m -> {
                                    try {
                                        if (m.contains("|")) {
                                            String[] parts = m.split("\\|");
                                            if (parts.length > 0 && parts[0].matches("\\d+")) {
                                                if (Integer.parseInt(parts[0].trim()) == currentUserId) {
                                                    return parts[1] + " (Vous) ⭐";
                                                }
                                            }
                                        }
                                    } catch (Exception ex) {
                                        return m;
                                    }
                                    return m;
                                })
                                .collect(Collectors.toList());

                        // Afficher dans le dialogue
                        ListView<String> membersListView = new ListView<>(
                                FXCollections.observableArrayList(membresWithYou)
                        );
                        membersListView.setPrefHeight(250);
                        membersListView.setStyle("-fx-font-size: 12;");

                        Dialog<Void> dialog = new Dialog<>();
                        dialog.setTitle("Membres du groupe");
                        dialog.setHeaderText("Liste des participants (" + membresWithYou.size() + ")");
                        dialog.getDialogPane().setContent(membersListView);
                        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        dialog.showAndWait();
                    } else {
                        ListView<String> membersListView = new ListView<>(
                                FXCollections.observableArrayList(membres)
                        );
                        membersListView.setPrefHeight(250);
                        membersListView.setStyle("-fx-font-size: 12;");

                        Dialog<Void> dialog = new Dialog<>();
                        dialog.setTitle("Membres du groupe");
                        dialog.setHeaderText("Liste des participants (" + membres.size() + ")");
                        dialog.getDialogPane().setContent(membersListView);
                        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        dialog.showAndWait();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur");
                    errorAlert.setHeaderText("Erreur lors de la récupération des membres");
                    errorAlert.setContentText(ex.getMessage());
                    errorAlert.showAndWait();
                }
            });

            // ===== OPTION 1.2 : Ajouter un membre =====
            MenuItem addMemberItem = new MenuItem("Ajouter un membre ➕");
            addMemberItem.setOnAction(e -> {
                try {
                    // Récupérer tous les utilisateurs
                    List<String> allUsers = conversationDAO.getAllAppUsers();
                    if (allUsers == null || allUsers.isEmpty()) {
                        Alert emptyAlert = new Alert(Alert.AlertType.INFORMATION);
                        emptyAlert.setTitle("Aucun utilisateur");
                        emptyAlert.setHeaderText(null);
                        emptyAlert.setContentText("Aucun utilisateur disponible dans l'application !");
                        emptyAlert.showAndWait();
                        return;
                    }

                    // Récupérer les membres actuels
                    List<String> currentMembres = conversationDAO.getMembresByConversation(selectedConversationId);

                    // ✅ CORRECTION : Filtrer les utilisateurs disponibles
                    // Format supposé : "id|nom|email" ou "nom|email"
                    List<String> filteredUsers = allUsers.stream()
                            .filter(u -> {
                                try {
                                    String[] parts = u.split("\\|");
                                    String name = parts.length > 0 ? parts[0].trim().toLowerCase() : "";
                                    String identifier = parts.length > 1 ? parts[1].trim() : "";
                                    String email = parts.length > 2 ? parts[2].trim() : "";

                                    // Vérifier si l'utilisateur est déjà membre
                                    boolean alreadyMember = currentMembres.stream().anyMatch(m -> {
                                        try {
                                            String[] memberParts = m.split("\\|");
                                            if (memberParts.length > 0 && memberParts[0].matches("\\d+")) {
                                                return Integer.parseInt(memberParts[0].trim()) == Integer.parseInt(identifier);
                                            }
                                            return m.toLowerCase().contains(name);
                                        } catch (Exception ex) {
                                            return m.toLowerCase().equals(name);
                                        }
                                    });

                                    // Vérifier si c'est l'utilisateur actuel
                                    boolean isCurrentUser = false;
                                    if (identifier.matches("\\d+")) {
                                        isCurrentUser = Integer.parseInt(identifier) == currentUserId;
                                    } else if (email.matches("\\d+")) {
                                        isCurrentUser = Integer.parseInt(email) == currentUserId;
                                    }

                                    return !alreadyMember && !isCurrentUser;
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    return false;
                                }
                            })
                            .collect(Collectors.toList());

                    // Vérifier s'il y a des utilisateurs disponibles
                    if (filteredUsers.isEmpty()) {
                        Alert emptyAlert = new Alert(Alert.AlertType.INFORMATION);
                        emptyAlert.setTitle("Aucun utilisateur disponible");
                        emptyAlert.setHeaderText(null);
                        emptyAlert.setContentText("Tous les autres utilisateurs sont déjà membres de ce groupe !");
                        emptyAlert.showAndWait();
                        return;
                    }

                    // Afficher le dialogue de sélection
                    ChoiceDialog<String> dialog = new ChoiceDialog<>(filteredUsers.get(0), filteredUsers);
                    dialog.setTitle("Ajouter un membre");
                    dialog.setHeaderText("Sélectionnez l'utilisateur à ajouter au groupe :");
                    dialog.setContentText("Utilisateur :");

                    dialog.showAndWait().ifPresent(selectedUser -> {
                        try {
                            String[] parts = selectedUser.split("\\|");
                            String identifier = parts.length > 1 ? parts[1].trim() : "";
                            String email = parts.length > 2 ? parts[2].trim() : "";

                            // Utiliser l'identifiant approprié (ID ou email)
                            boolean added = false;
                            if (identifier.matches("\\d+")) {
                                added = conversationDAO.addMemberToConversation(selectedConversationId, Integer.parseInt(identifier));
                            } else if (!email.isEmpty()) {
                                added = conversationDAO.addMemberToConversation(selectedConversationId, email);
                            } else {
                                added = conversationDAO.addMemberToConversation(selectedConversationId, identifier);
                            }

                            if (added) {
                                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                                successAlert.setTitle("Succès");
                                successAlert.setHeaderText(null);
                                successAlert.setContentText("Membre ajouté avec succès ! ✅");
                                successAlert.showAndWait();
                            } else {
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                errorAlert.setTitle("Erreur");
                                errorAlert.setHeaderText(null);
                                errorAlert.setContentText("Impossible d'ajouter le membre. Veuillez réessayer.");
                                errorAlert.showAndWait();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Erreur");
                            errorAlert.setHeaderText("Erreur lors de l'ajout du membre");
                            errorAlert.setContentText(ex.getMessage());
                            errorAlert.showAndWait();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur");
                    errorAlert.setHeaderText("Erreur lors de la récupération des utilisateurs");
                    errorAlert.setContentText(ex.getMessage());
                    errorAlert.showAndWait();
                }
            });

            // Ajouter les options de groupe au menu
            contextMenu.getItems().addAll(showMembersItem, addMemberItem, new SeparatorMenuItem());
        }

        // ========== CAS 2 : OPTIONS COMMUNES (GROUPE ET PRIVÉE) ==========

        // ===== OPTION 2.1 : Modifier le nom du chat =====
        MenuItem editTitle = new MenuItem("Modifier le nom du chat ✏️");
        editTitle.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(chatUserName.getText());
            dialog.setTitle("Modifier le nom");
            dialog.setHeaderText("Entrez le nouveau nom de la conversation :");
            dialog.setContentText("Nouveau nom :");

            dialog.showAndWait().ifPresent(newName -> {
                if (newName.trim().isEmpty()) {
                    Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                    warningAlert.setTitle("Attention");
                    warningAlert.setHeaderText(null);
                    warningAlert.setContentText("Le nom ne peut pas être vide !");
                    warningAlert.showAndWait();
                    return;
                }

                if (conversationDAO.updateConversationTitle(selectedConversationId, newName.trim())) {
                    chatUserName.setText(newName.trim());
                    loadConversations();

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Succès");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Nom modifié avec succès ! ✅");
                    successAlert.showAndWait();
                } else {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Impossible de modifier le nom. Veuillez réessayer.");
                    errorAlert.showAndWait();
                }
            });
        });

        // ===== OPTION 2.2 : Supprimer la conversation =====
        MenuItem deleteChat = new MenuItem("Supprimer la conversation 🗑️");
        deleteChat.setOnAction(e -> {
            // Confirmation avant suppression
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation");
            confirmAlert.setHeaderText("Supprimer la conversation ?");
            confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cette conversation ? Cette action est irréversible.");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (conversationDAO.deleteConversation(selectedConversationId)) {
                        selectedConversationId = -1;
                        chatUserName.setText("Sélectionnez un chat");
                        messagesList.setItems(null);
                        loadConversations();

                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Succès");
                        successAlert.setHeaderText(null);
                        successAlert.setContentText("Conversation supprimée avec succès ! ✅");
                        successAlert.showAndWait();
                    } else {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Erreur");
                        errorAlert.setHeaderText(null);
                        errorAlert.setContentText("Impossible de supprimer la conversation. Veuillez réessayer.");
                        errorAlert.showAndWait();
                    }
                }
            });
        });
        // ===== Ajout de l'option TRADUCTION =====
        MenuItem translateItem = new MenuItem("Traduire la conversation 🌐");
        translateItem.setOnAction(e -> showLanguageMenu(chatHeader));

        // Ajouter les options communes au menu
        contextMenu.getItems().addAll(editTitle, deleteChat,translateItem);

        // ========== AFFICHER LE MENU CONTEXTUEL ==========
        contextMenu.show(chatHeader, Side.BOTTOM, 0, 0);
    }

    @FXML
    private void addNewConversation() {
        // Étape 1 : Demander le type de conversation (Privée ou Groupe)
        List<String> choices = List.of("Message Privé 👤", "Groupe 👥");
        ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Message Privé 👤", choices);
        typeDialog.setTitle("Nouvelle Conversation");
        typeDialog.setHeaderText("Quel type de conversation voulez-vous créer ?");
        typeDialog.setContentText("Type :");

        typeDialog.showAndWait().ifPresent(type -> {
            if (type.contains("Privé")) {
                // ========== CAS 1 : MESSAGE PRIVÉ ==========

                try {
                    // Récupérer tous les utilisateurs de l'application
                    List<String> allUsers = conversationDAO.getAllAppUsers();

                    if (allUsers == null || allUsers.isEmpty()) {
                        Alert emptyAlert = new Alert(Alert.AlertType.INFORMATION);
                        emptyAlert.setTitle("Aucun utilisateur");
                        emptyAlert.setHeaderText(null);
                        emptyAlert.setContentText("Aucun utilisateur disponible dans l'application !");
                        emptyAlert.showAndWait();
                        return;
                    }

                    // Récupérer les conversations privées existantes
                    List<String> existingPrivateChats = allConversations.stream()
                            .filter(c -> c.getEstGroupe() == 0)  // Filtrer les conversations privées (0 = privé)
                            .map(c -> c.getTitre().trim().toLowerCase())
                            .collect(Collectors.toList());

                    // ✅ CORRECTION : Filtrer les utilisateurs disponibles
                    // Format supposé : "nom|id|email" ou "nom|email|id" ou "id|nom|email"
                    List<String> availableUsers = allUsers.stream()
                            .filter(u -> {
                                try {
                                    String[] parts = u.split("\\|");

                                    // Essayer de détecter le format et extraire les infos
                                    String name = "";
                                    String userId = "";
                                    String email = "";

                                    // Format 1 : "nom|id|email"
                                    if (parts.length == 3) {
                                        name = parts[0].trim().toLowerCase();
                                        userId = parts[1].trim();
                                        email = parts[2].trim();
                                    }
                                    // Format 2 : "nom|email" (pas d'ID numérique)
                                    else if (parts.length == 2) {
                                        name = parts[0].trim().toLowerCase();
                                        email = parts[1].trim();
                                        // userId reste vide
                                    }
                                    // Format 3 : "id|nom|email"
                                    else if (parts.length >= 3 && parts[0].matches("\\d+")) {
                                        userId = parts[0].trim();
                                        name = parts[1].trim().toLowerCase();
                                        email = parts[2].trim();
                                    }

                                    // ✅ VÉRIFICATION 1 : Exclure l'utilisateur actuel
                                    boolean isCurrentUser = false;

                                    // Comparer par ID numérique si disponible
                                    if (!userId.isEmpty() && userId.matches("\\d+")) {
                                        isCurrentUser = Integer.parseInt(userId) == currentUserId;
                                    }
                                    // Sinon comparer par email
                                    else if (!email.isEmpty()) {
                                        // Si email est un nombre, comparer comme ID
                                        if (email.matches("\\d+")) {
                                            isCurrentUser = Integer.parseInt(email) == currentUserId;
                                        } else {
                                            // Sinon, vous devez avoir l'email de l'utilisateur actuel
                                            // isCurrentUser = email.equals(getCurrentUserEmail());
                                            isCurrentUser = false; // À adapter
                                        }
                                    }

                                    // ✅ VÉRIFICATION 2 : Vérifier qu'il n'existe pas déjà une conversation avec cet utilisateur
                                    boolean alreadyHasChat = existingPrivateChats.contains(name);

                                    // Retourner true si l'utilisateur doit être affiché
                                    return !alreadyHasChat && !isCurrentUser;

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            })
                            .collect(Collectors.toList());

                    // Vérifier s'il y a des utilisateurs disponibles
                    if (availableUsers.isEmpty()) {
                        Alert emptyAlert = new Alert(Alert.AlertType.INFORMATION);
                        emptyAlert.setTitle("Aucun utilisateur disponible");
                        emptyAlert.setHeaderText(null);
                        emptyAlert.setContentText("Vous avez déjà une conversation privée avec tous les autres utilisateurs.");
                        emptyAlert.showAndWait();
                        return;
                    }

                    // Afficher le dialogue de sélection d'utilisateur
                    ChoiceDialog<String> userDialog = new ChoiceDialog<>(availableUsers.get(0), availableUsers);
                    userDialog.setTitle("Sélectionner un contact");
                    userDialog.setHeaderText("Choisissez l'utilisateur avec qui commencer une conversation privée :");
                    userDialog.setContentText("Utilisateur :");

                    userDialog.showAndWait().ifPresent(selectedUser -> {
                        try {
                            String[] parts = selectedUser.split("\\|");
                            String name = parts[0].trim();

                            // Extraire l'identifiant (email ou ID)
                            String identifier = "";
                            if (parts.length == 2) {
                                identifier = parts[1].trim();
                            } else if (parts.length >= 3) {
                                // Essayer d'obtenir l'ID d'abord
                                if (parts[1].matches("\\d+")) {
                                    identifier = parts[1].trim();
                                } else {
                                    identifier = parts[2].trim();
                                }
                            }

                            // Créer la nouvelle conversation privée
                            int newId = conversationDAO.addConversation(
                                    new Conversation(name, 0)  // 0 = conversation privée
                            );

                            if (newId != -1) {
                                // Ajouter l'utilisateur actuel comme participant
                                conversationDAO.addMemberToConversation(newId, currentUserId);

                                // Ajouter l'autre utilisateur
                                if (identifier.matches("\\d+")) {
                                    // C'est un ID numérique
                                    conversationDAO.addMemberToConversation(newId, Integer.parseInt(identifier));
                                } else {
                                    // C'est un email ou autre identifiant
                                    conversationDAO.addMemberToConversation(newId, identifier);
                                }

                                // Rafraîchir la liste des conversations
                                loadConversations();

                                // Message de succès
                                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                                successAlert.setTitle("Succès");
                                successAlert.setHeaderText(null);
                                successAlert.setContentText("Conversation privée créée avec " + name + " ✅");
                                successAlert.showAndWait();
                            } else {
                                // Erreur lors de la création
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                errorAlert.setTitle("Erreur");
                                errorAlert.setHeaderText("Impossible de créer la conversation");
                                errorAlert.setContentText("Une erreur s'est produite lors de la création de la conversation.");
                                errorAlert.showAndWait();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Erreur");
                            errorAlert.setHeaderText("Erreur de traitement");
                            errorAlert.setContentText("Erreur : " + e.getMessage());
                            errorAlert.showAndWait();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur");
                    errorAlert.setHeaderText("Erreur lors de la récupération des utilisateurs");
                    errorAlert.setContentText(e.getMessage());
                    errorAlert.showAndWait();
                }

            } else {
                // ========== CAS 2 : GROUPE ==========

                // Demander le nom du groupe
                TextInputDialog nameInput = new TextInputDialog();
                nameInput.setTitle("Créer un groupe");
                nameInput.setHeaderText("Créer une nouvelle conversation de groupe");
                nameInput.setContentText("Nom du groupe :");
                nameInput.getEditor().setPromptText("Ex: Projet 2024, Amis, ...");

                nameInput.showAndWait().ifPresent(name -> {
                    // Vérifier que le nom n'est pas vide
                    if (!name.trim().isEmpty()) {
                        try {
                            // Créer le groupe
                            int newId = conversationDAO.addConversation(
                                    new Conversation(name.trim(), 1)  // 1 = groupe
                            );

                            if (newId != -1) {
                                // Ajouter le créateur comme premier membre
                                conversationDAO.addMemberToConversation(newId, currentUserId);

                                // Rafraîchir la liste
                                loadConversations();

                                // Message de succès
                                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                                successAlert.setTitle("Succès");
                                successAlert.setHeaderText(null);
                                successAlert.setContentText("Groupe \"" + name.trim() + "\" créé ! ✅\nVous pouvez maintenant ajouter des membres.");
                                successAlert.showAndWait();
                            } else {
                                // Erreur lors de la création
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                errorAlert.setTitle("Erreur");
                                errorAlert.setHeaderText("Impossible de créer le groupe");
                                errorAlert.setContentText("Une erreur s'est produite lors de la création du groupe.");
                                errorAlert.showAndWait();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Erreur");
                            errorAlert.setHeaderText("Erreur de traitement");
                            errorAlert.setContentText("Erreur : " + e.getMessage());
                            errorAlert.showAndWait();
                        }
                    } else {
                        // Nom vide
                        Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                        warningAlert.setTitle("Attention");
                        warningAlert.setHeaderText(null);
                        warningAlert.setContentText("Le nom du groupe ne peut pas être vide !");
                        warningAlert.showAndWait();
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

        // Trier par date (du plus ancien au plus récent)
        messages.sort(Comparator.comparing(Message::getDateEnvoi));

        // Mettre à jour l'ensemble des IDs des 5 derniers messages
        lastFiveMessageIds.clear();
        int size = messages.size();
        for (int i = Math.max(0, size - 5); i < size; i++) {
            lastFiveMessageIds.add(messages.get(i).getIdMessage());
        }

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



    private void sendCallSignal(int conversationId, String callType) {
        if (stompHandler != null && stompHandler.isConnected()) {
            try {
                Map<String, Object> callData = new HashMap<>();
                callData.put("senderName", chatUserName.getText());
                callData.put("senderId", currentUserId);
                callData.put("conversationId", conversationId);
                callData.put("type", callType);

                ObjectMapper mapper = new ObjectMapper();
                String payload = mapper.writeValueAsString(callData);

                // Use handler to send (method may be named differently; adjust as needed)
                stompHandler.send("/app/call.start", payload.getBytes(StandardCharsets.UTF_8));

                System.out.println("📤 Call signal sent via STOMP");
            } catch (Exception e) {
                System.err.println("❌ Error sending call signal: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ STOMP not connected – signal not sent");
        }
    }


    /**
     * Initialise le gestionnaire STOMP pour la communication WebSocket.
     * Cette méthode crée une instance de StompClientHandler avec l'URL du serveur,
     * puis tente de se connecter (la connexion réelle est déclenchée après dans initialize()).
     */

    private void initializeStompClient() {
        try {
            String serverUrl = "http://localhost:8080/ws";
            stompHandler = new StompClientHandler(serverUrl, currentUserId);
            System.out.println("🔗 STOMP handler created for user " + currentUserId);
        } catch (Exception e) {
            System.err.println("❌ STOMP initialization error: " + e.getMessage());
            stompHandler = null;
        }
    }

    @FXML
    public void startAudioCall() {
        String name = chatUserName.getText();

        if ("Sélectionnez un chat".equals(name) || selectedConversationId == -1) {
            showAlert("Attention", "Veuillez sélectionner un contact pour l'appeler.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Gui/CallingWindow.fxml"));
            Parent root = loader.load();
            CallingController controller = loader.getController();

            Stage callStage = new Stage();
            callStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(null);
            callStage.setScene(scene);

            controller.setContactData(name, callStage);
            controller.setCallData(selectedConversationId, currentUserId, "AUDIO_CALL", messageDAO);

            sendCallSignal(selectedConversationId, "AUDIO_CALL");

            makeStageDraggable(root, callStage);
            callStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la fenêtre d'appel.");
        }
    }


    // ========== UPDATED: startVideoCall() ==========
    @FXML
    private void startVideoCall() {
        String contactName = chatUserName.getText();

        if ("Sélectionnez un chat".equals(contactName) || selectedConversationId == -1) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setHeaderText("Aucune conversation sélectionnée");
            alert.setContentText("Veuillez sélectionner un contact pour l'appeler.");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Gui/CallingWindow.fxml"));
            Parent root = loader.load();
            CallingController controller = loader.getController();

            Stage callStage = new Stage();
            callStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(null);
            callStage.setScene(scene);

            controller.setContactData(contactName, callStage);
            controller.setCallData(selectedConversationId, currentUserId, "VIDEO_CALL", messageDAO);

            sendCallSignal(selectedConversationId, "VIDEO_CALL");

            makeStageDraggable(root, callStage);
            callStage.show();

            System.out.println("📹 Video call initiated to: " + contactName);

        } catch (IOException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Erreur");
            errorAlert.setHeaderText("Impossible de démarrer l'appel vidéo");
            errorAlert.setContentText("Erreur : " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    // ========== BONUS : startVideoCall() AVEC ENREGISTREMENT ==========
    private void makeStageDraggable(Parent root, Stage stage) {
        final double[] xOffset = {0};
        final double[][] yOffset = {{0}};
        root.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0][0] = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0][0]);
        });
    }

// Dans MessengerController.java

    private CallController currentIncomingCallController; // Pour stocker l'instance du récepteur
    private void onIncomingCallReceived(String senderName, int senderId, int conversationId, String callType) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Gui/CallWindow.fxml"));
                Parent root = loader.load();

                CallController controller = loader.getController();
                Stage incomingStage = new Stage();

                // Pass call data to receiver
                controller.setCallerData(senderName, incomingStage);
                controller.setCallData(conversationId, currentUserId, callType, messageDAO);

                incomingStage.setScene(new Scene(root));
                incomingStage.setTitle("Appel entrant de " + senderName);
                incomingStage.show();

                System.out.println("📞 Incoming call window displayed for: " + senderName);

            } catch (IOException e) {
                System.err.println("❌ Error loading CallWindow: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void onCallCancelledBySender() {
        if (currentIncomingCallController != null) {
            currentIncomingCallController.forceClose();
        }
    }

    private void showLanguageMenu(Node anchor) {
        ContextMenu langMenu = new ContextMenu();
        String[][] languages = {
                {"Français", "fr"},
                {"English", "en"},
                {"العربية", "ar"},
                {"Español", "es"},
                {"Deutsch", "de"},
                {"中文", "zh"},        // Chinois
                {"日本語", "ja"},       // Japonais
                {"Русский", "ru"}      // Russe
        };
        for (String[] lang : languages) {
            MenuItem item = new MenuItem(lang[0]);
            item.setOnAction(e -> {
                currentTargetLanguage = lang[1];
                loadMessages(selectedConversationId);
            });
            langMenu.getItems().add(item);
        }
        langMenu.show(anchor, Side.BOTTOM, 0, 0);
    }

    private void refreshSingleMessage(int messageId) {
        Platform.runLater(() -> {
            try {
                loadMessages(selectedConversationId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void listMTEngines() throws IOException, InterruptedException {
        String url = BASE_URL + "/account/mtengines";
        String authHeader = "";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("📋 MT Engines: " + response.body());
    }
}