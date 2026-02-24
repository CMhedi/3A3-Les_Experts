package Controller;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javafx.scene.control.ProgressIndicator;
import Entities.Conversation;
import Entities.Message;
import Services.interfaces.ConversationDAO;
import Services.interfaces.GeminiService;
import Services.interfaces.GifService;
import Services.interfaces.MessageDAO;
import Services.interfaces.AudioRecorder;
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
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// ===================================
// ✅ FIX: Alignement des GIFs à gauche
// ===================================
// Le code original avait : gifsPane.setAlignment(Pos.CENTER);
// La correction: gifsPane.setAlignment(Pos.TOP_LEFT);
// ===================================

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
    private VoiceMessagePlayer voicePlayer = new VoiceMessagePlayer();

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
                    else if ("VIDEO".equals(msg.getTypeMessage())) {
                        displayVideoMessage(mainContainer, msg);
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
            private HBox buildFileBubble(Message msg) {
                HBox box = new HBox(10);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(10, 14, 10, 14));
                box.setMaxWidth(280);
                if (msg.getIdUser() == currentUserId) {
                    box.setStyle(
                            "-fx-background-color: #007AFF;" +
                                    "-fx-background-radius: 18 18 4 18;"
                    );
                } else {
                    box.setStyle(
                            "-fx-background-color: #E9E9EB;" +
                                    "-fx-background-radius: 18 18 18 4;"
                    );
                }
                return box;
            }

            private void finishBubble(VBox container, Message msg, javafx.scene.layout.Region bubble) {
                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                String status = (msg.getIdUser() == currentUserId && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : " ✓";
                Label footer = new Label(time + (msg.getIdUser() == currentUserId ? status : ""));
                footer.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7; -fx-padding: 2 2 0 2;");
                if (msg.getIdUser() == currentUserId) footer.setAlignment(Pos.CENTER_RIGHT);

                VBox messageBox = new VBox(3, bubble, footer);
                messageBox.setMaxWidth(bubble.getMaxWidth() + 12);
                container.getChildren().add(messageBox);
            }
            private void displayVideoMessage(VBox container, Message msg) {
                String videoPath = msg.getContenu();
                String fullPath = "C:\\wamp\\htdocs\\" + videoPath.replace("/", "\\");

                final int THUMB_W = 220;
                final int THUMB_H = 160;

                // ── Thumbnail StackPane ───────────────────────────────────────────
                StackPane thumbStack = new StackPane();
                thumbStack.setPrefSize(THUMB_W, THUMB_H);
                thumbStack.setMaxSize(THUMB_W, THUMB_H);
                thumbStack.setMinSize(THUMB_W, THUMB_H);
                thumbStack.setCursor(Cursor.HAND);

                // Dark background (shown while loading or if no thumbnail)
                javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle(THUMB_W, THUMB_H, Color.web("#1a1a2e"));
                bg.setArcWidth(24); bg.setArcHeight(24);

                // Thumbnail image (filled by MediaPlayer snapshot)
                ImageView thumbView = new ImageView();
                thumbView.setFitWidth(THUMB_W);
                thumbView.setFitHeight(THUMB_H);
                thumbView.setPreserveRatio(false);

                // Dark gradient overlay
                javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(THUMB_W, THUMB_H);
                overlay.setFill(new javafx.scene.paint.LinearGradient(
                        0, 0, 0, 1, true,
                        javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, Color.color(0,0,0,0.1)),
                        new javafx.scene.paint.Stop(1, Color.color(0,0,0,0.5))
                ));
                overlay.setArcWidth(24); overlay.setArcHeight(24);

                // ── Play button ───────────────────────────────────────────────────
                StackPane playCircle = new StackPane();
                javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(26, Color.color(1,1,1,0.9));
                circle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 2);");
                Label playIcon = new Label("▶");
                playIcon.setStyle("-fx-text-fill: #111; -fx-font-size: 18px; -fx-padding: 0 0 0 3;");
                playCircle.getChildren().addAll(circle, playIcon);

                // ── Duration badge bottom-right ───────────────────────────────────
                Label durationLabel = new Label("•••");
                durationLabel.setStyle(
                        "-fx-background-color: rgba(0,0,0,0.7);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 11px;" +
                                "-fx-background-radius: 5;" +
                                "-fx-padding: 2 6;"
                );
                StackPane.setAlignment(durationLabel, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(durationLabel, new Insets(0, 8, 8, 0));

                thumbStack.getChildren().addAll(bg, thumbView, overlay, playCircle, durationLabel);

                // Clip to rounded corners
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(THUMB_W, THUMB_H);
                clip.setArcWidth(24); clip.setArcHeight(24);
                thumbStack.setClip(clip);

                // ── Load thumbnail + real duration asynchronously ─────────────────
                new Thread(() -> {
                    File videoFile = new File(fullPath);
                    if (!videoFile.exists()) return;
                    Platform.runLater(() -> {
                        try {
                            Media media = new Media(videoFile.toURI().toString());
                            MediaPlayer mp = new MediaPlayer(media);

                            mp.setOnReady(() -> {
                                // ✅ Get real duration on ready
                                double totalSecs = mp.getTotalDuration().toSeconds();
                                int mins = (int)(totalSecs / 60);
                                int secs = (int)(totalSecs % 60);
                                durationLabel.setText(String.format("%d:%02d", mins, secs));

                                // Seek to 10% of video to get a nice thumbnail frame
                                double seekTo = Math.min(1.5, totalSecs * 0.1);
                                mp.seek(Duration.seconds(seekTo));
                            });

                            // ✅ Snapshot after seeking
                            mp.setOnStopped(() -> {
                                javafx.scene.media.MediaView mv = new javafx.scene.media.MediaView(mp);
                                mv.setFitWidth(THUMB_W);
                                mv.setFitHeight(THUMB_H);
                                mv.setPreserveRatio(false);
                                // Add to a temporary scene to allow snapshot
                                StackPane tempPane = new StackPane(mv);
                                tempPane.setPrefSize(THUMB_W, THUMB_H);
                                new Scene(tempPane); // needed for snapshot
                                javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
                                sp.setFill(Color.BLACK);
                                javafx.scene.image.WritableImage snap = tempPane.snapshot(sp, null);
                                if (snap != null && snap.getWidth() > 1) {
                                    thumbView.setImage(snap);
                                }
                                mp.dispose();
                            });

                            // currentTime listener → stop after seeking to capture frame
                            mp.currentTimeProperty().addListener((obs, oldT, newT) -> {
                                if (newT.greaterThan(Duration.seconds(0.5))) {
                                    mp.stop(); // triggers onStopped → snapshot
                                }
                            });

                            mp.play();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Platform.runLater(() -> durationLabel.setText("—"));
                        }
                    });
                }).start();

                // ── Hover effect ──────────────────────────────────────────────────
                thumbStack.setOnMouseEntered(e -> circle.setFill(Color.color(1,1,1,1.0)));
                thumbStack.setOnMouseExited(e  -> circle.setFill(Color.color(1,1,1,0.9)));

                // ── Click → open player ───────────────────────────────────────────
                thumbStack.setOnMouseClicked(e -> openVideoPlayer(fullPath));

                // ── Timestamp footer ──────────────────────────────────────────────
                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                String status = (msg.getIdUser() == currentUserId && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : " ✓";
                Label footer = new Label(time + (msg.getIdUser() == currentUserId ? status : ""));
                footer.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7; -fx-padding: 2 2 0 2;");
                if (msg.getIdUser() == currentUserId) footer.setAlignment(Pos.CENTER_RIGHT);

                // ── Tight wrapper bubble (NO extra padding/color stretching) ──────
                // ✅ KEY FIX: use HBox to prevent full-width stretch
                HBox bubbleWrapper = new HBox(thumbStack);
                bubbleWrapper.setMaxWidth(THUMB_W + 16);
                bubbleWrapper.setPrefWidth(THUMB_W + 16);
                bubbleWrapper.setPadding(new Insets(4));

                if (msg.getIdUser() == currentUserId) {
                    bubbleWrapper.setStyle(
                            "-fx-background-color: #007AFF;" +
                                    "-fx-background-radius: 18 18 4 18;"
                    );
                } else {
                    bubbleWrapper.setStyle(
                            "-fx-background-color: #E9E9EB;" +
                                    "-fx-background-radius: 18 18 18 4;"
                    );
                }

                VBox messageBox = new VBox(3);
                messageBox.getChildren().addAll(bubbleWrapper, footer);
                // ✅ Prevent messageBox from stretching full width
                messageBox.setMaxWidth(THUMB_W + 24);

                container.getChildren().add(messageBox);
            }
            private void playVideo(String filePath) {
                try {
                    File videoFile = new File(filePath);
                    if (!videoFile.exists()) {
                        showAlert("Erreur", "Fichier vidéo introuvable : " + filePath);
                        return;
                    }

                    Media media = new Media(videoFile.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    MediaView mediaView = new MediaView(mediaPlayer);

                    StackPane root = new StackPane(mediaView);
                    root.setStyle("-fx-background-color: black;");

                    Scene scene = new Scene(root, 800, 600);
                    Stage stage = new Stage();
                    stage.setTitle("Lecture vidéo");
                    stage.setScene(scene);
                    stage.show();

                    mediaPlayer.play();

                    // Stop playback when window is closed
                    stage.setOnCloseRequest(e -> mediaPlayer.stop());
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de lire la vidéo : " + e.getMessage());
                }
            }
            private void openVideoPlayer(String fullPath) {
                File videoFile = new File(fullPath);
                if (!videoFile.exists()) {
                    showAlert("Erreur", "Fichier vidéo introuvable :\n" + fullPath);
                    return;
                }

                try {
                    Media media = new Media(videoFile.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    javafx.scene.media.MediaView mediaView = new javafx.scene.media.MediaView(mediaPlayer);

                    // ── Controls ─────────────────────────────────────────────────
                    Button playPauseBtn = new Button("⏸");
                    playPauseBtn.setStyle(
                            "-fx-background-color: transparent; -fx-text-fill: white;" +
                                    "-fx-font-size: 20px; -fx-cursor: hand;"
                    );

                    javafx.scene.control.Slider timeSlider = new javafx.scene.control.Slider();
                    timeSlider.setStyle("-fx-accent: #007AFF;");
                    HBox.setHgrow(timeSlider, javafx.scene.layout.Priority.ALWAYS);

                    Label timeLabel = new Label("0:00");
                    timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-min-width: 40;");

                    HBox controls = new HBox(12, playPauseBtn, timeSlider, timeLabel);
                    controls.setAlignment(Pos.CENTER_LEFT);
                    controls.setPadding(new Insets(10, 16, 10, 16));
                    controls.setStyle("-fx-background-color: rgba(0,0,0,0.80);");

                    // ── Layout ────────────────────────────────────────────────────
                    StackPane videoArea = new StackPane(mediaView);
                    videoArea.setStyle("-fx-background-color: black;");

                    javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
                    root.setCenter(videoArea);
                    root.setBottom(controls);
                    root.setStyle("-fx-background-color: black;");

                    // Bind size to window
                    mediaView.fitWidthProperty().bind(videoArea.widthProperty());
                    mediaView.fitHeightProperty().bind(videoArea.heightProperty());
                    mediaView.setPreserveRatio(true);

                    // ── Wire controls ─────────────────────────────────────────────
                    mediaPlayer.setOnReady(() -> {
                        timeSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());
                    });

                    mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                        if (!timeSlider.isValueChanging()) {
                            timeSlider.setValue(newT.toSeconds());
                        }
                        int s = (int) newT.toSeconds();
                        timeLabel.setText(String.format("%d:%02d", s / 60, s % 60));
                    });

                    timeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                        if (!isChanging) {
                            mediaPlayer.seek(Duration.seconds(timeSlider.getValue()));
                        }
                    });

                    timeSlider.setOnMouseClicked(e -> {
                        mediaPlayer.seek(Duration.seconds(timeSlider.getValue()));
                    });

                    mediaPlayer.setOnEndOfMedia(() -> {
                        playPauseBtn.setText("▶");
                        mediaPlayer.seek(Duration.ZERO);
                        mediaPlayer.pause();
                    });

                    playPauseBtn.setOnAction(e -> {
                        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                            mediaPlayer.pause();
                            playPauseBtn.setText("▶");
                        } else {
                            mediaPlayer.play();
                            playPauseBtn.setText("⏸");
                        }
                    });

                    // ── Stage ─────────────────────────────────────────────────────
                    Stage stage = new Stage();
                    stage.setTitle("▶  " + videoFile.getName());
                    stage.setScene(new Scene(root, 800, 520, Color.BLACK));
                    stage.setOnCloseRequest(e -> {
                        mediaPlayer.stop();
                        mediaPlayer.dispose();
                    });
                    stage.show();

                    mediaPlayer.play();

                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de lire la vidéo : " + e.getMessage());
                }
            }
            private void displayAudioCallMessage(VBox container, Message msg) {
                displayCallMessage(container, msg, "📞", "#00b894");
            }

            private void displayVideoCallMessage(VBox container, Message msg) {
                displayCallMessage(container, msg, "📹", "#0984e3");
            }

            private void displayCallMessage(VBox container, Message msg, String icon, String color) {
                HBox callBox = new HBox(15);
                callBox.setStyle(
                        "-fx-background-color: #F5F5F5;" +
                                "-fx-background-radius: 20;" +
                                "-fx-padding: 12;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
                );
                callBox.setAlignment(Pos.CENTER_LEFT);

                Label callIcon = new Label(icon);
                callIcon.setFont(Font.font("Segoe UI Emoji", 32));
                callIcon.setMinWidth(40);
                callIcon.setAlignment(Pos.CENTER);

                VBox textBox = new VBox(4);
                textBox.setPrefWidth(200);

                Label callTypeLabel = new Label(icon.equals("📞") ? "Appel vocal" : "Appel vidéo");
                callTypeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                callTypeLabel.setTextFill(Color.web(color));

                String callStatus = extractCallStatus(msg.getContenu());
                String callDuration = extractCallDuration(msg.getContenu());
                Label statusLabel = new Label(callStatus + " • " + callDuration);
                statusLabel.setFont(Font.font("Segoe UI", 12));
                statusLabel.setTextFill(Color.web("#7a7a7a"));

                textBox.getChildren().addAll(callTypeLabel, statusLabel);

                Button redialButton = new Button("Rappeler");
                redialButton.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: " + color + ";" +
                                "-fx-font-size: 12;" +
                                "-fx-padding: 6 12;" +
                                "-fx-cursor: hand;" +
                                "-fx-border-color: " + color + ";" +
                                "-fx-border-radius: 15;" +
                                "-fx-background-radius: 15;"
                );
                redialButton.setOnAction(e -> {
                    if (icon.equals("📞")) startAudioCall();
                    else startVideoCall();
                });

                VBox rightBox = new VBox(6);
                rightBox.setAlignment(Pos.CENTER_LEFT);
                rightBox.getChildren().addAll(textBox, redialButton);

                callBox.getChildren().addAll(callIcon, rightBox);

                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                Label timeLabel = new Label(time);
                timeLabel.setFont(Font.font("Segoe UI", 11));
                timeLabel.setTextFill(Color.web("#999999"));

                VBox mainCall = new VBox(3);
                mainCall.getChildren().addAll(callBox, timeLabel);
                container.getChildren().add(mainCall);
            }

            private void displayVocalMessage(VBox container, Message msg) {
                String audioPath = msg.getContenu();
                String fullPath = "C:\\wamp\\htdocs\\" + audioPath.replace("/", "\\");

                HBox voiceBox = new HBox(12);
                voiceBox.setAlignment(Pos.CENTER_LEFT);
                voiceBox.setPadding(new Insets(8, 14, 8, 14));
                if (msg.getIdUser() == currentUserId) {
                    voiceBox.setStyle("-fx-background-radius: 18; -fx-background-color: #007AFF;");
                } else {
                    voiceBox.setStyle("-fx-background-radius: 18; -fx-background-color: #E9E9EB;");
                }

                // ── Play button ───────────────────────────────────────────────────
                Button playStopBtn = new Button("▶");
                playStopBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: " + (msg.getIdUser() == currentUserId ? "white" : "#007AFF") + ";" +
                                "-fx-font-size: 18; -fx-cursor: hand;"
                );

                // ── Waveform bars ─────────────────────────────────────────────────
                HBox waveform = new HBox(3);
                waveform.setAlignment(Pos.CENTER_LEFT);
                String barColor = (msg.getIdUser() == currentUserId ? "white" : "#555");
                for (int i = 0; i < 5; i++) {
                    Rectangle bar = new Rectangle(3, 10 + i * 2);
                    bar.setFill(Color.web(barColor));
                    bar.setArcWidth(2);
                    bar.setArcHeight(2);
                    waveform.getChildren().add(bar);
                }

                // ── Duration label (real, loaded async) ───────────────────────────
                Label durationLabel = new Label("⏳");
                durationLabel.setStyle(
                        "-fx-font-size: 11;" +
                                "-fx-text-fill: " + (msg.getIdUser() == currentUserId ? "#DDD" : "#888") + ";"
                );

                new Thread(() -> {
                    try {
                        File audioFile = new File(fullPath);
                        if (audioFile.exists()) {
                            Platform.runLater(() -> {
                                try {
                                    Media media = new Media(audioFile.toURI().toString());
                                    MediaPlayer tempPlayer = new MediaPlayer(media);
                                    tempPlayer.setOnReady(() -> {
                                        double totalSecs = tempPlayer.getTotalDuration().toSeconds();
                                        int mins = (int)(totalSecs / 60);
                                        int secs = (int)(totalSecs % 60);
                                        durationLabel.setText(String.format("%d:%02d", mins, secs));
                                        tempPlayer.dispose();
                                    });
                                    tempPlayer.setOnError(() -> {
                                        durationLabel.setText("—");
                                        tempPlayer.dispose();
                                    });
                                } catch (Exception e) {
                                    durationLabel.setText("—");
                                }
                            });
                        } else {
                            Platform.runLater(() -> durationLabel.setText("?"));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> durationLabel.setText("—"));
                    }
                }).start();

                // ── AI button ─────────────────────────────────────────────────────
                Button aiBtn = new Button("🤖");
                aiBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: " + (msg.getIdUser() == currentUserId ? "white" : "#007AFF") + ";" +
                                "-fx-font-size: 14; -fx-cursor: hand;"
                );
                aiBtn.setTooltip(new Tooltip("Répondre avec IA"));
                aiBtn.setOnAction(e -> handleAiReplyFromVoice(fullPath));

                // ── Delete button ─────────────────────────────────────────────────
                Button deleteBtn = new Button("✕");
                deleteBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: " + (msg.getIdUser() == currentUserId ? "white" : "#999") + ";" +
                                "-fx-font-size: 14; -fx-cursor: hand; -fx-padding: 0 3;"
                );
                deleteBtn.setVisible(msg.getIdUser() == currentUserId);
                deleteBtn.setOnAction(e -> {
                    if (voicePlayer.isCurrentlyPlaying(fullPath)) voicePlayer.stopPlayback();
                    if (messageDAO.deleteMessage(msg.getIdMessage())) loadMessages(selectedConversationId);
                });

                // ✅ NOW add all children — after all are declared
                voiceBox.getChildren().addAll(playStopBtn, waveform, durationLabel, aiBtn);
                if (msg.getIdUser() == currentUserId) {
                    voiceBox.getChildren().add(deleteBtn);
                }

                // ── Play action ───────────────────────────────────────────────────
                playStopBtn.setOnAction(e -> {
                    if (voicePlayer.isCurrentlyPlaying(fullPath)) {
                        voicePlayer.stopPlayback();
                        playStopBtn.setText("▶");
                    } else {
                        voicePlayer.playVoiceMessage(fullPath);
                        playStopBtn.setText("⏸");
                    }
                });

                // ── Footer ────────────────────────────────────────────────────────
                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                String status = (msg.getIdUser() == currentUserId && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : " ✓";
                Label footer = new Label(time + (msg.getIdUser() == currentUserId ? status : ""));
                footer.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7; -fx-padding: 0 5;");

                VBox messageBox = new VBox(3);
                messageBox.getChildren().addAll(voiceBox, footer);
                container.getChildren().add(messageBox);
            }
            private void handleAiReplyFromVoice(String audioFilePath) {
                // Show loading indicator
                Stage loadingStage = new Stage();
                VBox loadingBox = new VBox(15);
                loadingBox.setAlignment(Pos.CENTER);
                loadingBox.setPadding(new Insets(30));
                loadingBox.setStyle("-fx-background-color: white; -fx-background-radius: 15;");

                ProgressIndicator spinner = new ProgressIndicator();
                spinner.setMaxSize(50, 50);
                Label loadingLabel = new Label("🤖 L'IA transcrit et analyse...");
                loadingLabel.setStyle("-fx-font-size: 14px;");
                loadingBox.getChildren().addAll(spinner, loadingLabel);

                loadingStage.setScene(new Scene(loadingBox, 280, 140));
                loadingStage.setTitle("IA en cours...");
                loadingStage.show();

                new Thread(() -> {
                    try {
                        // ── STEP 1: Transcribe audio → text ──────────────────────────
                        String transcribedText = transcribeAudioFile(audioFilePath);

                        if (transcribedText == null || transcribedText.trim().isEmpty()) {
                            Platform.runLater(() -> {
                                loadingStage.close();
                                showAlert("IA", "Je n'ai pas pu comprendre l'audio.");
                            });
                            return;
                        }

                        Platform.runLater(() -> loadingLabel.setText("💬 Transcription: \"" + transcribedText + "\"\n\n🤖 Gemini réfléchit..."));

                        // ── STEP 2: Send to Gemini ────────────────────────────────────
                        String aiResponse = GeminiService.askGemini(
                                "Tu es un assistant vocal. L'utilisateur a dit: \"" + transcribedText + "\". " +
                                        "Réponds de manière courte et naturelle (max 2 phrases), comme si tu parlais à voix haute."
                        );

                        Platform.runLater(() -> loadingLabel.setText("🔊 Génération audio..."));

                        // ── STEP 3: Convert AI response → Audio (TTS) ────────────────
                        String ttsAudioPath = generateTTS(aiResponse);

                        Platform.runLater(() -> {
                            loadingStage.close();

                            // Show dialog with transcription + AI response + options
                            showAiReplyDialog(transcribedText, aiResponse, ttsAudioPath);
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            loadingStage.close();
                            showAlert("Erreur IA", ex.getMessage());
                        });
                    }
                }).start();
            }

            // ── Transcribe using Vosk ─────────────────────────────────────────────────
            private String transcribeAudioFile(String audioFilePath) {
                try {
                    String modelPath = "src/main/resources/models/vosk-model-small-en-us-0.15";
                    Model model = new Model(modelPath);
                    Recognizer recognizer = new Recognizer(model, 16000);

                    FileInputStream fis = new FileInputStream(audioFilePath);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        recognizer.acceptWaveForm(buffer, bytesRead);
                    }
                    fis.close();

                    String json = recognizer.getFinalResult();
                    recognizer.close();
                    model.close();

                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readTree(json).get("text").asText();

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            // ── TTS: Windows SAPI (no external API needed) ───────────────────────────
            private String generateTTS(String text) {
                try {
                    String outputPath = "C:\\wamp\\htdocs\\uploads\\Audio\\ai_reply_" +
                            System.currentTimeMillis() + ".wav";
                    new File("C:\\wamp\\htdocs\\uploads\\Audio").mkdirs();

                    // Use Windows built-in TTS via PowerShell
                    String psScript = String.format(
                            "Add-Type -AssemblyName System.Speech; " +
                                    "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                    "$synth.SetOutputToWaveFile('%s'); " +
                                    "$synth.Speak('%s'); " +
                                    "$synth.Dispose();",
                            outputPath,
                            text.replace("'", " ").replace("\"", " ")  // escape quotes
                    );

                    ProcessBuilder pb = new ProcessBuilder(
                            "powershell", "-Command", psScript
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    process.waitFor();

                    File outputFile = new File(outputPath);
                    return outputFile.exists() ? outputPath : null;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            // ── Show result dialog ────────────────────────────────────────────────────
            private void showAiReplyDialog(String transcription, String aiText, String ttsPath) {
                Stage dialog = new Stage();
                dialog.setTitle("🤖 Réponse IA");

                VBox root = new VBox(16);
                root.setPadding(new Insets(20));
                root.setStyle("-fx-background-color: #f8f9fa;");

                // Transcription bubble
                Label transTitle = new Label("🎤 Vous avez dit :");
                transTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

                Label transText = new Label("\"" + transcription + "\"");
                transText.setWrapText(true);
                transText.setStyle(
                        "-fx-background-color: #007AFF; -fx-text-fill: white;" +
                                "-fx-padding: 10 14; -fx-background-radius: 16; -fx-font-size: 13px;"
                );

                // AI response bubble
                Label aiTitle = new Label("🤖 Réponse IA :");
                aiTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

                Label aiLabel = new Label(aiText);
                aiLabel.setWrapText(true);
                aiLabel.setMaxWidth(380);
                aiLabel.setStyle(
                        "-fx-background-color: #E9E9EB; -fx-text-fill: #111;" +
                                "-fx-padding: 10 14; -fx-background-radius: 16; -fx-font-size: 13px;"
                );

                // Buttons
                HBox buttons = new HBox(10);
                buttons.setAlignment(Pos.CENTER);

                // ▶ Play AI audio
                Button playBtn = new Button("▶ Écouter");
                playBtn.setStyle(
                        "-fx-background-color: #007AFF; -fx-text-fill: white;" +
                                "-fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;"
                );
                playBtn.setDisable(ttsPath == null);
                if (ttsPath != null) {
                    playBtn.setOnAction(e -> {
                        try {
                            Media media = new Media(new File(ttsPath).toURI().toString());
                            MediaPlayer player = new MediaPlayer(media);
                            player.play();
                            playBtn.setText("⏸ En cours...");
                            player.setOnEndOfMedia(() -> {
                                playBtn.setText("▶ Écouter");
                                player.dispose();
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }

                // 📤 Send as vocal message
                Button sendVoiceBtn = new Button("📤 Envoyer vocal");
                sendVoiceBtn.setStyle(
                        "-fx-background-color: #34c759; -fx-text-fill: white;" +
                                "-fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;"
                );
                sendVoiceBtn.setDisable(ttsPath == null);
                if (ttsPath != null) {
                    sendVoiceBtn.setOnAction(e -> {
                        // Copy to Audio folder with proper name and save as vocal message
                        String relativePath = "uploads/Audio/" + new File(ttsPath).getName();
                        messageDAO.addVocalMessage(relativePath, selectedConversationId, currentUserId);
                        loadMessages(selectedConversationId);
                        dialog.close();
                    });
                }

                // 💬 Send as text message
                Button sendTextBtn = new Button("💬 Envoyer texte");
                sendTextBtn.setStyle(
                        "-fx-background-color: #5856d6; -fx-text-fill: white;" +
                                "-fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;"
                );
                sendTextBtn.setOnAction(e -> {
                    Message msg = new Message("TEXTE", aiText, "ENVOYE",
                            LocalDateTime.now(), selectedConversationId, currentUserId);
                    messageDAO.addMessage(msg);
                    loadMessages(selectedConversationId);
                    dialog.close();
                });

                Button closeBtn = new Button("✕ Fermer");
                closeBtn.setStyle(
                        "-fx-background-color: #ff3b30; -fx-text-fill: white;" +
                                "-fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;"
                );
                closeBtn.setOnAction(e -> dialog.close());

                buttons.getChildren().addAll(playBtn, sendVoiceBtn, sendTextBtn, closeBtn);

                root.getChildren().addAll(transTitle, transText, aiTitle, aiLabel, buttons);

                ScrollPane sp = new ScrollPane(root);
                sp.setFitToWidth(true);
                sp.setStyle("-fx-background-color: #f8f9fa;");

                dialog.setScene(new Scene(sp, 440, 380));
                dialog.show();
            }

            private String extractAudioFileName(String filePath) {
                if (filePath == null || filePath.isEmpty()) {
                    return "Unknown.wav";
                }

                if (filePath.contains("/")) {
                    return filePath.substring(filePath.lastIndexOf("/") + 1);
                }

                if (filePath.contains("\\")) {
                    return filePath.substring(filePath.lastIndexOf("\\") + 1);
                }

                return filePath;
            }

            private void displayTextMessage(VBox container, Message msg) {
                String originalText = msg.getContenu();
                String displayText = originalText;

                // ----- Gestion de la traduction (inchangée) -----
                if (!"fr".equals(currentTargetLanguage)) {
                    int msgId = msg.getIdMessage();
                    String targetLang = currentTargetLanguage;
                    if (lastFiveMessageIds.contains(msgId)) {
                        if (translatedCache.containsKey(msgId)) {
                            displayText = translatedCache.get(msgId);
                        } else if (translationInProgress.add(msgId)) {
                            displayText = originalText + " ⏳";
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
                                        translatedCache.put(msgId, originalText + " ⚠️");
                                        translationInProgress.remove(msgId);
                                        refreshSingleMessage(msgId);
                                    });
                                }
                            }).start();
                        } else {
                            displayText = originalText + " ⏳";
                        }
                    }
                }

                // ----- Détection du type de contenu -----

                boolean isLocalAttachment = originalText.startsWith("uploads/");
                boolean isImageUrl = isImageUrl(originalText);
                boolean isPdfUrl = isPdfUrl(originalText);

                if (isLocalAttachment) {
                    // Pièce jointe locale (fichier uploadé)
                    displayAttachment(container, msg, originalText);
                } else if (isImageUrl) {
                    // Image distante (GIF, etc.) – code existant
                    displayImageFromUrl(container, msg, originalText);
                } else if (isPdfUrl) {
                    // PDF distant
                    displayPdfFromUrl(container, msg, originalText);
                } else {
                    // Message texte normal (avec éventuelle traduction)
                    displayTextBubble(container, msg, displayText);
                }
            }
            private void displayPdfFromUrl(VBox container, Message msg, String url) {
                VBox attachmentBox = new VBox(5);
                attachmentBox.setPadding(new Insets(8));
                attachmentBox.setAlignment(Pos.CENTER);
                if (msg.getIdUser() == currentUserId) {
                    attachmentBox.setStyle("-fx-background-color: #007AFF; -fx-background-radius: 20 20 4 20; -fx-padding: 8;");
                } else {
                    attachmentBox.setStyle("-fx-background-color: #E9E9EB; -fx-background-radius: 20 20 20 4; -fx-padding: 8;");
                }

                HBox pdfBox = new HBox(10);
                pdfBox.setAlignment(Pos.CENTER_LEFT);

                Label icon = new Label("📄");
                icon.setFont(Font.font("Segoe UI Emoji", 32));

                Label fileName = new Label(getFileNameFromUrl(url));
                fileName.setFont(Font.font("Segoe UI", 13));
                fileName.setTextFill(msg.getIdUser() == currentUserId ? Color.WHITE : Color.BLACK);

                Button openBtn = new Button("Ouvrir");
                openBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;");
                openBtn.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert("Erreur", "Impossible d'ouvrir le lien.");
                    }
                });

                pdfBox.getChildren().addAll(icon, fileName, openBtn);
                attachmentBox.getChildren().add(pdfBox);

                String time = msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm"));
                String status = (msg.getIdUser() == currentUserId && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : " ✓";
                Label footer = new Label(time + (msg.getIdUser() == currentUserId ? status : ""));
                footer.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7; -fx-padding: 0 5;");

                VBox messageBox = new VBox(3);
                messageBox.getChildren().addAll(attachmentBox, footer);
                container.getChildren().add(messageBox);
            }

            private String getFileNameFromUrl(String url) {
                try {
                    String path = new URI(url).getPath();
                    return path.substring(path.lastIndexOf('/') + 1);
                } catch (Exception e) {
                    return "document.pdf";
                }
            }
            private void displayImageFromUrl(VBox container, Message msg, String imageUrl) {
                StackPane imageStack = new StackPane();
                imageStack.setPrefSize(220, 220);
                imageStack.setMaxSize(220, 220);
                imageStack.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16;");

                ProgressIndicator progress = new ProgressIndicator();
                progress.setMaxSize(36, 36);
                imageStack.getChildren().add(progress);

                // Rounded clip
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(220, 220);
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                imageStack.setClip(clip);

                new Thread(() -> {
                    try {
                        byte[] imageData = downloadImageWithHeaders(imageUrl);
                        if (imageData == null || imageData.length == 0) throw new Exception("Empty");
                        Image image = new Image(new ByteArrayInputStream(imageData));
                        Platform.runLater(() -> {
                            if (!image.isError()) {
                                ImageView imageView = new ImageView(image);
                                imageView.setFitWidth(220);
                                imageView.setFitHeight(220);
                                imageView.setPreserveRatio(true);
                                imageView.setCursor(Cursor.HAND);
                                imageStack.getChildren().setAll(imageView);

                                // Click → fullscreen
                                imageStack.setOnMouseClicked(e -> {
                                    Stage stage = new Stage();
                                    ImageView bigView = new ImageView(image);
                                    bigView.setPreserveRatio(true);
                                    bigView.setFitWidth(700);
                                    bigView.setFitHeight(700);
                                    ScrollPane sp = new ScrollPane(bigView);
                                    sp.setStyle("-fx-background-color: black;");
                                    stage.setScene(new Scene(sp, 700, 700, Color.BLACK));
                                    stage.show();
                                });
                            } else {
                                imageStack.getChildren().setAll(new Label("❌"));
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> imageStack.getChildren().setAll(new Label("❌")));
                    }
                }).start();

                // ── Tight bubble wrapper ──────────────────────────────────────────
                HBox bubbleWrapper = new HBox(imageStack);
                bubbleWrapper.setMaxWidth(232);
                bubbleWrapper.setPrefWidth(232);
                bubbleWrapper.setPadding(new Insets(4));
                if (msg.getIdUser() == currentUserId) {
                    bubbleWrapper.setStyle(
                            "-fx-background-color: #007AFF;" +
                                    "-fx-background-radius: 18 18 4 18;"
                    );
                } else {
                    bubbleWrapper.setStyle(
                            "-fx-background-color: #E9E9EB;" +
                                    "-fx-background-radius: 18 18 18 4;"
                    );
                }

                finishBubble(container, msg, bubbleWrapper);
            }
            private void displayTextBubble(VBox container, Message msg, String text) {
                Text textNode = new Text(text);
                textNode.setFont(Font.font("Segoe UI Emoji", 14));
                TextFlow textFlow = new TextFlow(textNode);
                textFlow.setMaxWidth(300);

                if (msg.getIdUser() == currentUserId) {
                    textNode.setFill(Color.WHITE);
                    textFlow.setStyle(
                            "-fx-background-color: linear-gradient(to bottom right, #007AFF, #00C6FF);" +
                                    "-fx-background-radius: 20 20 4 20;" +
                                    "-fx-padding: 10 14;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
                    );
                } else {
                    textNode.setFill(Color.web("#050505"));
                    textFlow.setStyle(
                            "-fx-background-color: #E9E9EB;" +
                                    "-fx-background-radius: 20 20 20 4;" +
                                    "-fx-padding: 10 14;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);"
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
            private boolean isImageUrl(String url) {
                return url.toLowerCase().matches("(?i).*\\.(jpg|jpeg|png|gif|bmp|webp)(\\?.*)?$");
            }

            private boolean isPdfUrl(String url) {
                return url.toLowerCase().matches("(?i).*\\.pdf(\\?.*)?$");
            }
            private void displayAttachment(VBox container, Message msg, String path) {
                String mimeType = getMimeTypeFromPath(path);

                if ("image".equals(mimeType)) {
                    // ✅ NEW: pass container + msg + path directly (3 args)
                    displayImageAttachment(container, msg, path);

                } else if ("pdf".equals(mimeType)) {
                    HBox bubbleWrapper = buildFileBubble(msg);
                    displayPdfAttachment(bubbleWrapper, path, msg);
                    finishBubble(container, msg, bubbleWrapper);

                } else {
                    HBox bubbleWrapper = buildFileBubble(msg);
                    displayGenericFile(bubbleWrapper, path, mimeType, msg);
                    finishBubble(container, msg, bubbleWrapper);
                }
            }
            private void displayImageAttachment(VBox container, Message msg, String path) {
                File imageFile = new File("C:\\wamp\\htdocs\\" + path.replace("/", "\\"));

                // ── Tight image bubble ────────────────────────────────────────────
                StackPane imageStack = new StackPane();
                imageStack.setPrefSize(220, 220);
                imageStack.setMaxSize(220, 220);
                imageStack.setStyle(
                        "-fx-background-color: #1a1a2e;" +
                                "-fx-background-radius: 16;" +
                                "-fx-cursor: hand;"
                );

                ImageView imageView = new ImageView();
                imageView.setFitWidth(220);
                imageView.setFitHeight(220);
                imageView.setPreserveRatio(true);

                // Rounded clip
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(220, 220);
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                imageStack.setClip(clip);

                imageStack.getChildren().add(imageView);

                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);

                    // Click → fullscreen
                    imageStack.setOnMouseClicked(e -> {
                        Stage stage = new Stage();
                        stage.setTitle("Image");
                        ImageView bigView = new ImageView(image);
                        bigView.setPreserveRatio(true);
                        bigView.setFitWidth(700);
                        bigView.setFitHeight(700);
                        ScrollPane sp = new ScrollPane(bigView);
                        sp.setFitToWidth(true);
                        sp.setFitToHeight(true);
                        sp.setStyle("-fx-background-color: black;");
                        stage.setScene(new Scene(sp, 700, 700, Color.BLACK));
                        stage.show();
                    });
                } else {
                    Label err = new Label("❌ Image introuvable");
                    err.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                    imageStack.getChildren().add(err);
                }

                // ── Tight bubble wrapper ──────────────────────────────────────────
                HBox bubbleWrapper = new HBox(imageStack);
                bubbleWrapper.setMaxWidth(232);
                bubbleWrapper.setPrefWidth(232);
                bubbleWrapper.setPadding(new Insets(4));
                if (msg.getIdUser() == currentUserId) {
                    bubbleWrapper.setStyle(
                            "-fx-background-color: #007AFF;" +
                                    "-fx-background-radius: 18 18 4 18;"
                    );
                } else {
                    bubbleWrapper.setStyle(
                            "-fx-background-color: #E9E9EB;" +
                                    "-fx-background-radius: 18 18 18 4;"
                    );
                }

                finishBubble(container, msg, bubbleWrapper);
            }
            private void displayGenericFile(HBox attachmentBox, String path, String mimeType, Message msg) {
                HBox fileBox = new HBox(10);
                fileBox.setAlignment(Pos.CENTER_LEFT);

                String icon = getFileIcon(mimeType);
                Label iconLabel = new Label(icon);
                iconLabel.setFont(Font.font("Segoe UI Emoji", 32));

                Label fileName = new Label(new File(path).getName());
                fileName.setFont(Font.font("Segoe UI", 13));
                fileName.setTextFill(msg.getIdUser() == currentUserId ? Color.WHITE : Color.BLACK);

                Button openBtn = new Button("Ouvrir");
                openBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;");
                openBtn.setOnAction(e -> openFile(path));

                fileBox.getChildren().addAll(iconLabel, fileName, openBtn);
                attachmentBox.getChildren().add(fileBox);
            }

            private String getFileIcon(String mimeType) {
                switch (mimeType) {
                    case "word": return "📝";
                    case "pdf": return "📄";
                    default: return "📎";
                }
            }
            private String getMimeTypeFromPath(String path) {
                String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                switch (ext) {
                    case "png": case "jpg": case "jpeg": case "gif": case "bmp": case "webp":
                        return "image";
                    case "pdf":
                        return "pdf";
                    case "docx": case "doc":
                        return "word";
                    default:
                        return "file";
                }
            }
            private void displayPdfAttachment(HBox attachmentBox, String path, Message msg) {
                HBox pdfBox = new HBox(10);
                pdfBox.setAlignment(Pos.CENTER_LEFT);

                Label icon = new Label("📄");
                icon.setFont(Font.font("Segoe UI Emoji", 32));

                Label fileName = new Label(new File(path).getName());
                fileName.setFont(Font.font("Segoe UI", 13));
                fileName.setTextFill(msg.getIdUser() == currentUserId ? Color.WHITE : Color.BLACK);

                Button openBtn = new Button("Ouvrir");
                openBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;");
                openBtn.setOnAction(e -> openFile(path));

                pdfBox.getChildren().addAll(icon, fileName, openBtn);
                attachmentBox.getChildren().add(pdfBox);
            }
            private void openFile(String relativePath) {
                try {
                    File file = new File("C:\\wamp\\htdocs\\" + relativePath.replace("/", "\\"));
                    if (file.exists()) {
                        Desktop.getDesktop().open(file);
                    } else {
                        showAlert("Erreur", "Fichier introuvable : " + file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible d'ouvrir le fichier.");
                }
            }


            private String extractCallStatus(String content) {
                // Format : "📞 Appel audio - ACCEPTÉ (15s)"
                if (content.contains("ACCEPTÉ")) return "Appel accepté";
                if (content.contains("REJETÉ")) return "Appel rejeté";
                if (content.contains("NON_RÉPONDU")) return "Appel non répondu";
                return "Appel";
            }

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

    private String getAudioDuration(String filePath) {
        try {
            Media media = new Media(new File(filePath).toURI().toString());
            MediaPlayer player = new MediaPlayer(media);
            // Attendre que les métadonnées soient chargées
            final String[] duration = { "0:00" };
            player.setOnReady(() -> {
                double totalSeconds = player.getTotalDuration().toSeconds();
                int minutes = (int) (totalSeconds / 60);
                int seconds = (int) (totalSeconds % 60);
                duration[0] = String.format("%d:%02d", minutes, seconds);
            });
            // On laisse un peu de temps pour charger (bloquant, à éviter en UI)
            // Dans un vrai projet, on ferait cela de manière asynchrone.
            // Pour l'exemple, on retourne une valeur par défaut.
            return duration[0];
        } catch (Exception e) {
            return "0:00";
        }
    }

    @FXML
    private void handleAttachFile(ActionEvent event) {
        if (selectedConversationId == -1) {
            showAlert("Attention", "Sélectionnez d'abord une conversation.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.flv", "*.wmv"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Documents", "*.docx", "*.xlsx", "*.pptx")
        );

        File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (selectedFile != null) {
            // Determine file type
            String fileName = selectedFile.getName().toLowerCase();
            String typeMessage;
            String uploadSubDir;

            if (fileName.matches(".*\\.(mp4|avi|mov|mkv|flv|wmv)$")) {
                typeMessage = "VIDEO";
                uploadSubDir = "video";
            } else if (fileName.matches(".*\\.(png|jpg|jpeg|gif|bmp|webp)$")) {
                typeMessage = "FICHIER"; // or you could keep as FICHIER with image preview
                uploadSubDir = "files";
            } else {
                typeMessage = "FICHIER";
                uploadSubDir = "files";
            }

            String uploadedPath = uploadFile(selectedFile, uploadSubDir);
            if (uploadedPath != null) {
                Message msg = new Message(typeMessage, uploadedPath, "ENVOYE", LocalDateTime.now(), selectedConversationId, currentUserId);
                if (messageDAO.addMessage(msg)) {
                    loadMessages(selectedConversationId);
                }
            } else {
                showAlert("Erreur", "Échec de l'upload du fichier.");
            }
        }
    }

    private String uploadFile(File file, String subDir) {
        try {
            // Base upload directory (adjust if needed)
            String baseDir = "C:\\wamp\\htdocs\\uploads\\";
            String uploadDir = baseDir + subDir;
            File destDir = new File(uploadDir);

            // Create directory if it doesn't exist
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            // Generate unique filename to avoid overwrites
            String fileName = System.currentTimeMillis() + "_" + file.getName();
            File destFile = new File(destDir, fileName);

            // Copy the file
            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Return relative path to store in database (e.g., "uploads/video/12345_video.mp4")
            return "uploads/" + subDir + "/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
                String fileName = "voice_" + currentUserId + "_" + System.currentTimeMillis() + ".wav";

                // ✅ NEW PATH
                String audioPath = "C:\\wamp\\htdocs\\uploads\\Audio\\" + fileName;

                // Create directory if needed
                new File("C:\\wamp\\htdocs\\uploads\\Audio").mkdirs();

                recorder = new AudioRecorder();
                recorder.startRecording(audioPath);
                recordButton.setText("Stop");
                isRecording = true;

                Timeline stopTimeline = new Timeline(new KeyFrame(Duration.minutes(2), e -> {
                    if (isRecording) stopRecordingAndSave(audioPath);
                }));
                stopTimeline.setCycleCount(1);
                stopTimeline.play();

            } else {
                String audioPath = recorder.getAudioFilePath();
                stopRecordingAndSave(audioPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processVoiceToText(String audioFilePath) {
        try {
            String modelPath = "src/main/resources/models/vosk-model-small-en-us-0.15";
            File modelFolder = new File(modelPath);  // ✅ تعريف صحيح بدلاً من null

            if (!modelFolder.exists()) {
                System.err.println("❌ مجلد النموذج غير موجود: " + modelFolder.getAbsolutePath());
                return;
            }

            Model model = new Model(modelFolder.getAbsolutePath());  // ✅ تعريف واحد فقط

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
            System.err.println("❌ خطأ معالجة الصوت: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void stopRecordingAndSave(String audioPath) {
        try {
            recorder.stopRecording();
            recordButton.setText("🎙️");
            isRecording = false;

            // ✅ NEW relative path stored in DB
            String relativePath = "uploads/Audio/" + new File(audioPath).getName();
            messageDAO.addVocalMessage(relativePath, selectedConversationId, currentUserId);

            loadMessages(selectedConversationId);
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
        gifsPane.setAlignment(Pos.TOP_LEFT);  // ✅ CORRECTION : GIFs sur le côté gauche au lieu du centre

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
                            gifsPane.getChildren().add(new Label("❌ Aucun GIF trouvé pour : " + query));
                            return;
                        }

                        for (int i = 0; i < gifs.length(); i++) {
                            try {
                                JSONObject gifObj = gifs.getJSONObject(i);
                                String imageUrl = gifObj.getString("url");
                                final int index = i + 1;

                                // ✅ Créer un conteneur VBox pour chaque image
                                VBox gifContainer = new VBox();
                                gifContainer.setAlignment(Pos.CENTER);
                                gifContainer.setPrefSize(120, 120);
                                gifContainer.setStyle(
                                        "-fx-border-color: #ddd;" +
                                                "-fx-border-radius: 8;" +
                                                "-fx-padding: 5;" +
                                                "-fx-background-color: #f5f5f5;"
                                );

                                // ProgressIndicator pendant le chargement
                                ProgressIndicator gifProgress = new ProgressIndicator();
                                gifProgress.setMaxSize(30, 30);
                                gifContainer.getChildren().add(gifProgress);

                                // Charger l'image dans un thread séparé
                                new Thread(() -> {
                                    try {
                                        // ✅ Télécharger l'image avec HTTP headers
                                        byte[] imageData = downloadImageWithHeaders(imageUrl);

                                        if (imageData == null || imageData.length == 0) {
                                            throw new Exception("Données d'image vides");
                                        }

                                        // ✅ Créer l'ImageView
                                        ImageView imageView = new ImageView();
                                        imageView.setFitWidth(120);
                                        imageView.setFitHeight(120);
                                        imageView.setPreserveRatio(true);
                                        imageView.setCursor(Cursor.HAND);

                                        // ✅ Charger l'image à partir des données
                                        Image image = new Image(new ByteArrayInputStream(imageData));

                                        Platform.runLater(() -> {
                                            if (!image.isError() && image.getWidth() > 0) {
                                                imageView.setImage(image);
                                                gifContainer.getChildren().clear();
                                                gifContainer.getChildren().add(imageView);
                                                gifContainer.setCursor(Cursor.HAND);

                                                System.out.println("✅ Image #" + index + " affichée avec succès");

                                                // Ajouter les effets de hover
                                                gifContainer.setOnMouseEntered(event -> {
                                                    gifContainer.setStyle(
                                                            "-fx-border-color: #007AFF;" +
                                                                    "-fx-border-width: 2;" +
                                                                    "-fx-border-radius: 8;" +
                                                                    "-fx-padding: 5;" +
                                                                    "-fx-background-color: #e8f4ff;"
                                                    );
                                                    gifContainer.setScaleX(1.05);
                                                    gifContainer.setScaleY(1.05);
                                                });

                                                gifContainer.setOnMouseExited(event -> {
                                                    gifContainer.setStyle(
                                                            "-fx-border-color: #ddd;" +
                                                                    "-fx-border-radius: 8;" +
                                                                    "-fx-padding: 5;" +
                                                                    "-fx-background-color: #f5f5f5;"
                                                    );
                                                    gifContainer.setScaleX(1.0);
                                                    gifContainer.setScaleY(1.0);
                                                });

                                                // Clic pour ajouter à la rédaction
                                                String finalUrl = imageUrl;
                                                gifContainer.setOnMouseClicked(ev -> {
                                                    if (messageInput != null) {
                                                        messageInput.appendText(" " + finalUrl + " ");
                                                        System.out.println("✅ Image #" + index + " ajoutée à la rédaction");
                                                    }
                                                    stage.close();
                                                });
                                            } else {
                                                // Image erreur
                                                Label errorLabel = new Label("❌");
                                                errorLabel.setFont(Font.font("Segoe UI Emoji", 30));
                                                gifContainer.getChildren().clear();
                                                gifContainer.getChildren().add(errorLabel);
                                                System.err.println("❌ Erreur chargement image #" + index);
                                            }
                                        });

                                    } catch (Exception ex) {
                                        System.err.println("❌ Erreur téléchargement image #" + index + " : " + ex.getMessage());
                                        Platform.runLater(() -> {
                                            Label errorLabel = new Label("❌");
                                            errorLabel.setFont(Font.font("Segoe UI Emoji", 30));
                                            gifContainer.getChildren().clear();
                                            gifContainer.getChildren().add(errorLabel);
                                        });
                                    }
                                }).start();

                                gifsPane.getChildren().add(gifContainer);

                            } catch (Exception ex) {
                                System.err.println("❌ Erreur parsing image #" + (i+1) + " : " + ex.getMessage());
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        progress.setVisible(false);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("❌ Erreur");
                        alert.setHeaderText("Erreur lors de la recherche");
                        alert.setContentText("Erreur API: " + ex.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        });

        Scene scene = new Scene(root, 500, 500);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * ✅ Télécharge une image avec les HTTP headers appropriés
     */
    private byte[] downloadImageWithHeaders(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // ✅ Ajouter les headers essentiels
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
        connection.setRequestProperty("Referer", "https://giphy.com/");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        System.out.println("📡 Réponse HTTP: " + responseCode + " pour " + urlString);

        if (responseCode != 200) {
            throw new Exception("Erreur HTTP " + responseCode);
        }

        // Lire les données de l'image
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        InputStream is;
        is = connection.getInputStream();
        byte[] data = new byte[4096];
        int nRead;

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        is.close();
        connection.disconnect();

        return buffer.toByteArray();
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