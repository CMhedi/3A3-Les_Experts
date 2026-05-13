package controllers;

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
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.json.*;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ════════════════════════════════════════════════════════════════════
 *  MERGED MessengerController
 *  = Original full controller  +  new UI (avatars, filter tabs,
 *    green bubbles, reaction chips, rich conversation cells)
 * ════════════════════════════════════════════════════════════════════
 */
public class MessengerController implements Initializable {

    // ── Constants ──────────────────────────────────────────────────
    private static final String BASE_URL = null;
    /** Green for own-message bubbles (matches screenshot) */
    private static final String GREEN_OWN   = "#16a34a";
    /** Light gray for incoming bubbles */
    private static final String GRAY_OTHER  = "#E9E9EB";
    /** Avatar background palette – colour chosen by name hash */
    private static final String[] AVATAR_PALETTE = {
            "#22c55e","#3b82f6","#f59e0b","#ef4444","#8b5cf6","#06b6d4","#ec4899","#f97316"
    };
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    // ══════════════════════════════════════════════════════════════
    //  FXML FIELDS  (originals + new UI fields)
    // ══════════════════════════════════════════════════════════════

    // — Existing —
    @FXML private Button          recordButton;
    @FXML private Button          aiRecordButton;
    @FXML private HBox            chatHeader;
    @FXML private TextField       searchField;
    @FXML private ListView<Conversation> conversationsList;
    @FXML private ListView<Message>      messagesList;
    @FXML private TextField       messageInput;
    @FXML private Label           chatUserName;
    @FXML private Label           chatStatus;
    @FXML private VBox            botMessagesContainer;
    @FXML private TextField       botInput;
    @FXML private VBox            aiPanel;
    @FXML private ProgressIndicator aiLoader;
    @FXML private VBox            chatVBox; // kept for compatibility

    // — New (added in merged FXML) —
    @FXML private Label         chatAvatarLbl;
    @FXML private Label         chatTypeBadge;
    @FXML private Circle        statusDot;
    @FXML private ToggleButton  filterTous, filterNom, filterDate, filterGroupes, filterPrives;

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE STATE
    // ══════════════════════════════════════════════════════════════

    // — Existing —
    private boolean           isAiRecording = false;
    private AudioRecorder     recorder;
    private boolean           isRecording   = false;
    private AudioRecorder     aiRecorder;
    private boolean           aiVisible     = true;

    public  MessageDAO        messageDAO;
    private ConversationDAO   conversationDAO;
    private List<Conversation> allConversations;
    private int               selectedConversationId = -1;
    private int               currentUserId          = 66; // TODO: inject from session
    private VoiceMessagePlayer voicePlayer           = new VoiceMessagePlayer();

    private String            currentTargetLanguage  = "fr";
    private Map<Integer, String> translatedCache     = new HashMap<>();
    private final Set<Integer>   translationInProgress = ConcurrentHashMap.newKeySet();
    private Set<Integer>         lastFiveMessageIds  = new HashSet<>();

    private ObservableList<Message> chatMessages = FXCollections.observableArrayList();
    private StompClientHandler stompHandler;
    private HttpClient         httpClient;
    private GifService         gifService;
    private Timeline           recordingLimitTimer;
    private CallController     currentIncomingCallController;

    // — New —
    private ToggleGroup filterGroup;
    private String      activeFilter = "Tous";
    /** messageId → { emoji → count }  (session-only, not persisted) */
    private final Map<Integer, Map<String,Integer>> reactionsMap = new ConcurrentHashMap<>();
    /** conversationId → last-message timestamp (for Date filter sort) */
    private final Map<Integer, LocalDateTime> lastMsgTimeMap = new HashMap<>();

    // ══════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        messageDAO      = new MessageDAO();
        conversationDAO = new ConversationDAO();
        currentUserId   = 66; // TODO: replace with session user

        messagesList.setFocusTraversable(false);
        conversationsList.setFocusTraversable(false);

        buildFilterGroup();       // ← NEW
        setupConversationsList(); // ← UPDATED (rich cells)
        setupMessagesList();      // ← UPDATED (green bubbles + reactions)
        loadConversations();
        setupSearchListener();
        startAutoRefresh();

        gifService = new GifService("Filp4GHzXpQubEthmUu744ozFrXl464m");
        initializeStompClient();
        httpClient = HttpClient.newHttpClient();

        if (stompHandler != null) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    stompHandler.connect(
                            session -> System.out.println("✅ STOMP connecté"),
                            payload -> Platform.runLater(() -> processIncomingCall(payload))
                    );
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }).start();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NEW ── FILTER TABS
    // ══════════════════════════════════════════════════════════════

    private void buildFilterGroup() {
        filterGroup = new ToggleGroup();
        filterTous.setToggleGroup(filterGroup);
        filterNom.setToggleGroup(filterGroup);
        filterDate.setToggleGroup(filterGroup);
        filterGroupes.setToggleGroup(filterGroup);
        filterPrives.setToggleGroup(filterGroup);
        filterTous.setSelected(true);
        styleTab(filterTous, true);

        filterGroup.selectedToggleProperty().addListener((obs, ov, nv) -> {
            if (nv == null) { ov.setSelected(true); return; }
            filterGroup.getToggles().forEach(t -> styleTab((ToggleButton) t, t == nv));
            activeFilter = ((ToggleButton) nv).getText();
            applyFilter();
        });
    }

    private void styleTab(ToggleButton btn, boolean selected) {
        btn.setStyle(selected
                ? "-fx-background-radius:20;-fx-cursor:hand;-fx-font-size:12;-fx-padding:4 11;" +
                "-fx-background-color:#111827;-fx-text-fill:white;"
                : "-fx-background-radius:20;-fx-cursor:hand;-fx-font-size:12;-fx-padding:4 11;" +
                "-fx-background-color:transparent;-fx-text-fill:#374151;" +
                "-fx-border-color:#d1d5db;-fx-border-radius:20;-fx-border-width:1;");
    }

    @FXML private void onFilterChanged() { /* handled by ToggleGroup listener */ }

    private void applyFilter() {
        if (allConversations == null) return;
        String q = searchField.getText().toLowerCase(Locale.ROOT).trim();
        List<Conversation> result = allConversations.stream()
                .filter(c -> {
                    boolean typeOk = switch (activeFilter) {
                        case "Groupes" -> c.getEstGroupe() == 1;
                        case "Privés"  -> c.getEstGroupe() == 0;
                        default        -> true;
                    };
                    if (!typeOk) return false;
                    if (q.isEmpty()) return true;
                    return c.getTitre().toLowerCase().contains(q);
                })
                .sorted(switch (activeFilter) {
                    case "Nom"  -> Comparator.comparing(Conversation::getTitre,
                            String.CASE_INSENSITIVE_ORDER);
                    case "Date" -> Comparator.comparing(
                            (Conversation c) -> lastMsgTimeMap.getOrDefault(
                                    c.getIdConversation(), LocalDateTime.MIN)).reversed();
                    default     -> Comparator.comparing(Conversation::getTitre,
                            String.CASE_INSENSITIVE_ORDER);
                })
                .collect(Collectors.toList());
        conversationsList.setItems(FXCollections.observableArrayList(result));
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATED ── CONVERSATION LIST  (rich custom cells)
    // ══════════════════════════════════════════════════════════════

    private void setupConversationsList() {
        conversationsList.setCellFactory(lv -> new ConversationCell());
        conversationsList.getSelectionModel().selectedItemProperty()
                .addListener((obs, ov, nv) -> {
                    if (nv == null) return;
                    selectedConversationId = nv.getIdConversation();
                    updateChatHeader(nv);
                    messageDAO.markAsRead(selectedConversationId, currentUserId);
                    loadMessages(selectedConversationId);
                });
    }

    /** Update the chat-area header when the user selects a conversation. */
    private void updateChatHeader(Conversation conv) {
        String name = conv.getTitre();
        chatUserName.setText(name);

        // Avatar
        chatAvatarLbl.setText(buildInitials(name));
        chatAvatarLbl.setStyle(
                "-fx-background-color:" + avatarColor(name) + ";-fx-text-fill:white;" +
                        "-fx-background-radius:50;-fx-min-width:38;-fx-min-height:38;" +
                        "-fx-max-width:38;-fx-max-height:38;" +
                        "-fx-alignment:center;-fx-font-weight:bold;-fx-font-size:13;");

        // Privé / Groupe badge
        boolean isGroup = conv.getEstGroupe() == 1;
        chatTypeBadge.setText(isGroup ? "Groupe" : "Privé");
        chatTypeBadge.setStyle(
                "-fx-background-color:" + (isGroup ? "#dbeafe" : "#dcfce7") + ";" +
                        "-fx-text-fill:" + (isGroup ? "#1d4ed8" : "#16a34a") + ";" +
                        "-fx-background-radius:10;-fx-padding:1 8;-fx-font-size:10;-fx-font-weight:bold;");

        // Status (default offline — update via STOMP if available)
        statusDot.setFill(Color.web("#9ca3af"));
        chatStatus.setText("Hors ligne");
    }

    // ── Inner: rich conversation cell ───────────────────────────
    private class ConversationCell extends ListCell<Conversation> {
        private final HBox   root         = new HBox(10);
        private final Label  avatarLbl    = new Label();
        private final VBox   info         = new VBox(3);
        private final HBox   topRow       = new HBox();
        private final Label  nameLbl      = new Label();
        private final Label  dateLbl      = new Label();
        private final HBox   bottomRow    = new HBox(5);
        private final Circle statusCircle = new Circle(4);
        private final Label  subLbl       = new Label();

        ConversationCell() {
            avatarLbl.setAlignment(Pos.CENTER);

            nameLbl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#111827;");
            dateLbl.setStyle("-fx-font-size:11;-fx-text-fill:#9ca3af;");
            subLbl .setStyle("-fx-font-size:11;-fx-text-fill:#6b7280;");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            topRow.getChildren().addAll(nameLbl, sp, dateLbl);

            bottomRow.setAlignment(Pos.CENTER_LEFT);
            bottomRow.getChildren().addAll(statusCircle, subLbl);

            info.getChildren().addAll(topRow, bottomRow);
            HBox.setHgrow(info, Priority.ALWAYS);

            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 12, 10, 12));
            root.getChildren().addAll(avatarLbl, info);

            // Right-click: delete conversation
            ContextMenu cm = new ContextMenu();
            MenuItem del = new MenuItem("🗑 Supprimer");
            del.setOnAction(e -> {
                Conversation c = getItem();
                if (c == null) return;
                if (conversationDAO.deleteConversation(c.getIdConversation())) {
                    allConversations.remove(c);
                    applyFilter();
                    if (selectedConversationId == c.getIdConversation()) {
                        selectedConversationId = -1;
                        chatUserName.setText("Sélectionnez un chat");
                        chatTypeBadge.setText("");
                        messagesList.setItems(null);
                    }
                }
            });
            cm.getItems().add(del);
            root.setOnContextMenuRequested(e -> cm.show(root, e.getScreenX(), e.getScreenY()));
        }

        @Override
        protected void updateItem(Conversation conv, boolean empty) {
            super.updateItem(conv, empty);
            if (empty || conv == null) { setGraphic(null); setStyle(""); return; }

            String name    = conv.getTitre();
            String color   = avatarColor(name);
            boolean group  = conv.getEstGroupe() == 1;

            avatarLbl.setText(buildInitials(name));
            avatarLbl.setStyle(
                    "-fx-background-color:" + color + ";-fx-text-fill:white;-fx-background-radius:50;" +
                            "-fx-alignment:center;-fx-min-width:40;-fx-min-height:40;" +
                            "-fx-max-width:40;-fx-max-height:40;-fx-font-weight:bold;-fx-font-size:14;");

            nameLbl.setText(name);

            LocalDateTime last = lastMsgTimeMap.get(conv.getIdConversation());
            dateLbl.setText(last != null ? fmtDate(last) : "");

            statusCircle.setFill(Color.web("#9ca3af")); // offline by default
            subLbl.setText(group ? "👥 Groupe" : "🔒 Privée");

            boolean sel = isSelected();
            root.setStyle(
                    "-fx-background-color:" + (sel ? "#f0fdf4" : "transparent") + ";" +
                            (sel ? "-fx-border-color:transparent transparent transparent #22c55e;" +
                                    "-fx-border-width:0 0 0 3;" : ""));

            setGraphic(root);
            setStyle("-fx-padding:0;-fx-background-color:transparent;");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATED ── MESSAGE LIST  (green bubbles + reaction chips)
    // ══════════════════════════════════════════════════════════════

    private void setupMessagesList() {
        messagesList.setCellFactory(lv -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color:transparent;");
                    return;
                }

                VBox mainContainer = new VBox(5);
                mainContainer.setPadding(new Insets(8, 12, 8, 12));

                switch (msg.getTypeMessage()) {
                    case "AUDIO_CALL" -> { displayCallMessage(mainContainer, msg, "📞", "#00b894"); mainContainer.setAlignment(Pos.CENTER_LEFT); }
                    case "VIDEO_CALL" -> { displayCallMessage(mainContainer, msg, "📹", "#0984e3"); mainContainer.setAlignment(Pos.CENTER_LEFT); }
                    case "VOCAL"  -> { displayVocalMessage(mainContainer, msg);  mainContainer.setAlignment(msg.getIdUser()==currentUserId?Pos.CENTER_RIGHT:Pos.CENTER_LEFT); }
                    case "VIDEO"  -> { displayVideoMessage(mainContainer, msg);  mainContainer.setAlignment(msg.getIdUser()==currentUserId?Pos.CENTER_RIGHT:Pos.CENTER_LEFT); }
                    default       -> { displayTextMessage(mainContainer, msg);   mainContainer.setAlignment(msg.getIdUser()==currentUserId?Pos.CENTER_RIGHT:Pos.CENTER_LEFT); }
                }

                setGraphic(mainContainer);
                setStyle("-fx-background-color:transparent;-fx-padding:3 0;");

                // Context menu (edit / delete / react) for non-call messages
                if (!"AUDIO_CALL".equals(msg.getTypeMessage()) && !"VIDEO_CALL".equals(msg.getTypeMessage())) {
                    if (!mainContainer.getChildren().isEmpty()) {
                        VBox firstBox = (VBox) mainContainer.getChildren().get(0);
                        if (!firstBox.getChildren().isEmpty() && firstBox.getChildren().get(0) instanceof TextFlow tf) {
                            setupMessageContextMenu(tf, msg);
                        }
                    }
                }
            }

            // ── Text message ───────────────────────────────────────
            private void displayTextMessage(VBox container, Message msg) {
                String originalText = msg.getContenu();
                String displayText  = originalText;

                // Translation (existing logic, untouched)
                if (!"fr".equals(currentTargetLanguage)) {
                    int msgId = msg.getIdMessage();
                    String targetLang = currentTargetLanguage;
                    if (lastFiveMessageIds.contains(msgId)) {
                        if (translatedCache.containsKey(msgId)) {
                            displayText = translatedCache.get(msgId);
                        } else if (translationInProgress.add(msgId)) {
                            displayText = originalText + " ⏳";
                            String fDisplay = displayText;
                            new Thread(() -> {
                                try {
                                    String tr = GeminiService.translate(originalText, targetLang);
                                    Platform.runLater(() -> { translatedCache.put(msgId, tr); translationInProgress.remove(msgId); refreshSingleMessage(msgId); });
                                } catch (Exception ex) {
                                    Platform.runLater(() -> { translatedCache.put(msgId, originalText+" ⚠️"); translationInProgress.remove(msgId); refreshSingleMessage(msgId); });
                                }
                            }).start();
                        } else { displayText = originalText + " ⏳"; }
                    }
                }

                boolean isLocal  = originalText.startsWith("uploads/");
                boolean isImgUrl = isImageUrl(originalText);
                boolean isPdfUrl = isPdfUrl(originalText);

                if      (isLocal)  displayAttachment(container, msg, originalText);
                else if (isImgUrl) displayImageFromUrl(container, msg, originalText);
                else if (isPdfUrl) displayPdfFromUrl(container, msg, originalText);
                else               displayTextBubble(container, msg, displayText);
            }

            /** ── UPDATED: green own bubble, white incoming bubble ─ */
            private void displayTextBubble(VBox container, Message msg, String text) {
                Text     textNode = new Text(text);
                textNode.setFont(Font.font("Segoe UI Emoji", 14));
                TextFlow textFlow = new TextFlow(textNode);
                textFlow.setMaxWidth(300);

                boolean own = msg.getIdUser() == currentUserId;
                if (own) {
                    textNode.setFill(Color.WHITE);
                    textFlow.setStyle(
                            "-fx-background-color:" + GREEN_OWN + ";" +
                                    "-fx-background-radius:20 20 4 20;-fx-padding:10 14;" +
                                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),5,0,0,2);");
                } else {
                    textNode.setFill(Color.web("#050505"));
                    textFlow.setStyle(
                            "-fx-background-color:#ffffff;" +
                                    "-fx-background-radius:20 20 20 4;-fx-padding:10 14;" +
                                    "-fx-border-color:#e5e7eb;-fx-border-radius:20 20 20 4;" +
                                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),4,0,0,1);");
                }

                // ── Reaction chips ───────────────────────────────
                Map<String,Integer> reacts = reactionsMap.getOrDefault(msg.getIdMessage(), Collections.emptyMap());
                HBox reactRow = new HBox(4);
                reactRow.setVisible(!reacts.isEmpty());
                reactRow.setManaged(!reacts.isEmpty());
                reacts.forEach((emoji, count) -> {
                    Button rb = new Button(emoji + " " + count);
                    rb.setStyle(
                            "-fx-background-color:" + (own ? "rgba(255,255,255,0.2)" : "#f3f4f6") + ";" +
                                    "-fx-text-fill:" + (own ? "white" : "#374151") + ";" +
                                    "-fx-background-radius:12;-fx-padding:2 8;-fx-font-size:11;-fx-cursor:hand;");
                    rb.setOnAction(ev -> toggleReaction(msg.getIdMessage(), emoji));
                    reactRow.getChildren().add(rb);
                });

                // ── Footer (time + read status) ──────────────────
                String time   = msg.getDateEnvoi().format(FMT_TIME);
                String status = (own && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : (own ? " ✓" : "");
                Label footer  = new Label(time + status);
                footer.setStyle("-fx-font-size:9px;-fx-text-fill:" +
                        (own ? "rgba(255,255,255,0.65)" : "#9ca3af") + ";-fx-padding:0 2;");
                if (own) footer.setAlignment(Pos.CENTER_RIGHT);

                VBox messageBox = new VBox(3, textFlow, reactRow, footer);
                container.getChildren().add(messageBox);
            }

            private void toggleReaction(int msgId, String emoji) {
                Map<String,Integer> m = reactionsMap.computeIfAbsent(msgId, k -> new LinkedHashMap<>());
                int cur = m.getOrDefault(emoji, 0);
                if (cur <= 0) m.remove(emoji); else m.put(emoji, cur - 1);
                loadMessages(selectedConversationId);
            }

            private void showReactionPicker(Message msg) {
                String[] emojis = {"❤️","👍","😂","😮","😢","😡","🎉","🔥","🙏","😍"};
                ContextMenu picker = new ContextMenu();
                HBox box = new HBox(4);
                for (String e : emojis) {
                    Button b = new Button(e);
                    b.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-font-size:18;-fx-padding:2;");
                    b.setOnAction(ev -> {
                        Map<String,Integer> m = reactionsMap.computeIfAbsent(msg.getIdMessage(), k -> new LinkedHashMap<>());
                        m.put(e, m.getOrDefault(e, 0) + 1);
                        picker.hide();
                        loadMessages(selectedConversationId);
                    });
                    box.getChildren().add(b);
                }
                picker.getItems().add(new CustomMenuItem(box, false));
                picker.show(messagesList, Side.TOP, 0, 0);
            }

            // ── Vocal message ─────────────────────────────────────
            private void displayVocalMessage(VBox container, Message msg) {
                String audioPath = msg.getContenu();
                String fullPath  = "C:\\wamp\\htdocs\\" + audioPath.replace("/","\\");
                boolean own = msg.getIdUser() == currentUserId;

                HBox voiceBox = new HBox(12);
                voiceBox.setAlignment(Pos.CENTER_LEFT);
                voiceBox.setPadding(new Insets(8, 14, 8, 14));
                voiceBox.setStyle(
                        "-fx-background-color:" + (own ? GREEN_OWN : GRAY_OTHER) + ";" +
                                "-fx-background-radius:18;" +
                                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),4,0,0,1);");

                Button playBtn = new Button("▶");
                playBtn.setStyle(
                        "-fx-background-color:transparent;" +
                                "-fx-text-fill:" + (own ? "white" : "#22c55e") + ";" +
                                "-fx-font-size:18;-fx-cursor:hand;");

                HBox waveform = new HBox(3);
                waveform.setAlignment(Pos.CENTER_LEFT);
                for (int i = 0; i < 6; i++) {
                    Rectangle bar = new Rectangle(3, 8 + i * 2, Color.web(own ? "rgba(255,255,255,0.7)" : "#9ca3af"));
                    bar.setArcWidth(2); bar.setArcHeight(2);
                    waveform.getChildren().add(bar);
                }

                Label durationLbl = new Label("⏳");
                durationLbl.setStyle("-fx-font-size:11;-fx-text-fill:" + (own ? "#d1fae5" : "#6b7280") + ";");

                // Load real duration async
                new Thread(() -> {
                    try {
                        File f = new File(fullPath);
                        if (f.exists()) Platform.runLater(() -> {
                            try {
                                Media media = new Media(f.toURI().toString());
                                MediaPlayer tmp = new MediaPlayer(media);
                                tmp.setOnReady(() -> {
                                    double s = tmp.getTotalDuration().toSeconds();
                                    durationLbl.setText(String.format("%d:%02d",(int)(s/60),(int)(s%60)));
                                    tmp.dispose();
                                });
                                tmp.setOnError(() -> { durationLbl.setText("—"); tmp.dispose(); });
                            } catch (Exception ex) { durationLbl.setText("—"); }
                        });
                        else Platform.runLater(() -> durationLbl.setText("?"));
                    } catch (Exception ex) { Platform.runLater(() -> durationLbl.setText("—")); }
                }).start();

                playBtn.setOnAction(e -> {
                    if (voicePlayer.isCurrentlyPlaying(fullPath)) {
                        voicePlayer.stopPlayback(); playBtn.setText("▶");
                    } else {
                        voicePlayer.playVoiceMessage(fullPath); playBtn.setText("⏸");
                    }
                });

                voiceBox.getChildren().addAll(playBtn, waveform, durationLbl);

                if (own) {
                    Button del = new Button("✕");
                    del.setStyle("-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.7);" +
                            "-fx-font-size:13;-fx-cursor:hand;-fx-padding:0 3;");
                    del.setOnAction(e -> {
                        if (voicePlayer.isCurrentlyPlaying(fullPath)) voicePlayer.stopPlayback();
                        if (messageDAO.deleteMessage(msg.getIdMessage())) loadMessages(selectedConversationId);
                    });
                    voiceBox.getChildren().add(del);
                }

                String time   = msg.getDateEnvoi().format(FMT_TIME);
                String status = (own && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : (own ? " ✓" : "");
                Label footer  = new Label(time + status);
                footer.setStyle("-fx-font-size:9px;-fx-text-fill:#9ca3af;");

                container.getChildren().add(new VBox(3, voiceBox, footer));
            }

            // ── Video message ─────────────────────────────────────
            private void displayVideoMessage(VBox container, Message msg) {
                String videoPath = msg.getContenu();
                String fullPath  = "C:\\wamp\\htdocs\\" + videoPath.replace("/","\\");
                boolean own = msg.getIdUser() == currentUserId;
                final int W = 220, H = 160;

                StackPane thumbStack = new StackPane();
                thumbStack.setPrefSize(W,H); thumbStack.setMaxSize(W,H); thumbStack.setMinSize(W,H);
                thumbStack.setCursor(Cursor.HAND);

                Rectangle bg = new Rectangle(W, H, Color.web("#1a1a2e"));
                bg.setArcWidth(24); bg.setArcHeight(24);
                ImageView thumbView = new ImageView();
                thumbView.setFitWidth(W); thumbView.setFitHeight(H); thumbView.setPreserveRatio(false);
                Rectangle overlay = new Rectangle(W, H);
                overlay.setFill(new javafx.scene.paint.LinearGradient(0,0,0,1,true,
                        javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0,Color.color(0,0,0,0.1)),
                        new javafx.scene.paint.Stop(1,Color.color(0,0,0,0.45))));
                overlay.setArcWidth(24); overlay.setArcHeight(24);

                javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(24, Color.color(1,1,1,0.9));
                Label playIcon = new Label("▶"); playIcon.setStyle("-fx-text-fill:#111;-fx-font-size:16;-fx-padding:0 0 0 2;");
                StackPane playCircle = new StackPane(circle, playIcon);

                Label durLbl = new Label("•••");
                durLbl.setStyle("-fx-background-color:rgba(0,0,0,0.6);-fx-text-fill:white;" +
                        "-fx-font-size:10;-fx-background-radius:4;-fx-padding:2 6;");
                StackPane.setAlignment(durLbl, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(durLbl, new Insets(0,8,8,0));

                thumbStack.getChildren().addAll(bg, thumbView, overlay, playCircle, durLbl);
                Rectangle clip = new Rectangle(W,H); clip.setArcWidth(24); clip.setArcHeight(24);
                thumbStack.setClip(clip);

                new Thread(() -> {
                    File vf = new File(fullPath);
                    if (!vf.exists()) return;
                    Platform.runLater(() -> {
                        try {
                            Media m = new Media(vf.toURI().toString());
                            MediaPlayer mp = new MediaPlayer(m);
                            mp.setOnReady(() -> {
                                double t = mp.getTotalDuration().toSeconds();
                                durLbl.setText(String.format("%d:%02d",(int)(t/60),(int)(t%60)));
                                mp.seek(Duration.seconds(Math.min(1.5, t*0.1)));
                            });
                            mp.currentTimeProperty().addListener((obs,ov,nv) -> { if(nv.greaterThan(Duration.seconds(0.5))) mp.stop(); });
                            mp.setOnStopped(() -> {
                                MediaView mv = new MediaView(mp);
                                mv.setFitWidth(W); mv.setFitHeight(H); mv.setPreserveRatio(false);
                                StackPane tp = new StackPane(mv); tp.setPrefSize(W,H);
                                new Scene(tp);
                                javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
                                sp.setFill(Color.BLACK);
                                javafx.scene.image.WritableImage snap = tp.snapshot(sp,null);
                                if (snap!=null && snap.getWidth()>1) thumbView.setImage(snap);
                                mp.dispose();
                            });
                            mp.play();
                        } catch (Exception ex) { durLbl.setText("—"); }
                    });
                }).start();

                thumbStack.setOnMouseEntered(e -> circle.setFill(Color.WHITE));
                thumbStack.setOnMouseExited(e  -> circle.setFill(Color.color(1,1,1,0.9)));
                thumbStack.setOnMouseClicked(e -> openVideoPlayer(fullPath));

                HBox bw = new HBox(thumbStack);
                bw.setMaxWidth(W+16); bw.setPrefWidth(W+16); bw.setPadding(new Insets(4));
                bw.setStyle("-fx-background-color:" + (own ? GREEN_OWN : GRAY_OTHER) + ";" +
                        "-fx-background-radius:" + (own ? "18 18 4 18" : "18 18 18 4") + ";");

                String time   = msg.getDateEnvoi().format(FMT_TIME);
                String status = (own && "LU".equals(msg.getStatutMessage())) ? " ✓✓" : (own ? " ✓" : "");
                Label footer  = new Label(time + status);
                footer.setStyle("-fx-font-size:9px;-fx-text-fill:#9ca3af;");

                VBox mb = new VBox(3, bw, footer); mb.setMaxWidth(W+24);
                container.getChildren().add(mb);
            }

            private void openVideoPlayer(String fullPath) {
                File vf = new File(fullPath);
                if (!vf.exists()) { showAlert("Erreur","Fichier introuvable :\n"+fullPath); return; }
                try {
                    Media m = new Media(vf.toURI().toString());
                    MediaPlayer mp = new MediaPlayer(m);
                    MediaView mv = new MediaView(mp);

                    Button pp = new Button("⏸");
                    pp.setStyle("-fx-background-color:transparent;-fx-text-fill:white;-fx-font-size:20;-fx-cursor:hand;");
                    Slider sl = new Slider();
                    sl.setStyle("-fx-accent:#22c55e;");
                    HBox.setHgrow(sl, Priority.ALWAYS);
                    Label tl = new Label("0:00");
                    tl.setStyle("-fx-text-fill:white;-fx-font-size:12;-fx-min-width:40;");
                    HBox controls = new HBox(12, pp, sl, tl);
                    controls.setAlignment(Pos.CENTER_LEFT);
                    controls.setPadding(new Insets(10,16,10,16));
                    controls.setStyle("-fx-background-color:rgba(0,0,0,0.8);");

                    StackPane va = new StackPane(mv);
                    va.setStyle("-fx-background-color:black;");
                    BorderPane root = new BorderPane();
                    root.setCenter(va); root.setBottom(controls); root.setStyle("-fx-background-color:black;");
                    mv.fitWidthProperty().bind(va.widthProperty());
                    mv.fitHeightProperty().bind(va.heightProperty());
                    mv.setPreserveRatio(true);

                    mp.setOnReady(()  -> sl.setMax(mp.getTotalDuration().toSeconds()));
                    mp.currentTimeProperty().addListener((obs,ov,nv) -> {
                        if (!sl.isValueChanging()) sl.setValue(nv.toSeconds());
                        int s = (int)nv.toSeconds();
                        tl.setText(String.format("%d:%02d",s/60,s%60));
                    });
                    sl.valueChangingProperty().addListener((obs,was,is) -> { if(!is) mp.seek(Duration.seconds(sl.getValue())); });
                    sl.setOnMouseClicked(e -> mp.seek(Duration.seconds(sl.getValue())));
                    mp.setOnEndOfMedia(() -> { pp.setText("▶"); mp.seek(Duration.ZERO); mp.pause(); });
                    pp.setOnAction(e -> { if(mp.getStatus()==MediaPlayer.Status.PLAYING){mp.pause();pp.setText("▶");}else{mp.play();pp.setText("⏸");} });

                    Stage st = new Stage();
                    st.setTitle("▶ "+vf.getName());
                    st.setScene(new Scene(root,800,520,Color.BLACK));
                    st.setOnCloseRequest(e -> { mp.stop(); mp.dispose(); });
                    st.show(); mp.play();
                } catch (Exception ex) { showAlert("Erreur","Impossible de lire la vidéo : "+ex.getMessage()); }
            }

            // ── Call messages ─────────────────────────────────────
            private void displayCallMessage(VBox container, Message msg, String icon, String color) {
                HBox callBox = new HBox(15);
                callBox.setStyle("-fx-background-color:#f5f5f5;-fx-background-radius:20;" +
                        "-fx-padding:12;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),5,0,0,2);");
                callBox.setAlignment(Pos.CENTER_LEFT);

                Label callIcon = new Label(icon);
                callIcon.setFont(Font.font("Segoe UI Emoji",32)); callIcon.setMinWidth(40); callIcon.setAlignment(Pos.CENTER);

                VBox tb = new VBox(4);
                Label typeLabel = new Label("📞".equals(icon) ? "Appel vocal" : "Appel vidéo");
                typeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD,13));
                typeLabel.setTextFill(Color.web(color));
                Label stLabel = new Label(extractCallStatus(msg.getContenu())+" • "+extractCallDuration(msg.getContenu()));
                stLabel.setFont(Font.font("Segoe UI",12)); stLabel.setTextFill(Color.web("#7a7a7a"));
                tb.getChildren().addAll(typeLabel, stLabel);

                Button redial = new Button("Rappeler");
                redial.setStyle("-fx-background-color:transparent;-fx-text-fill:"+color+";" +
                        "-fx-font-size:12;-fx-padding:5 12;-fx-cursor:hand;" +
                        "-fx-border-color:"+color+";-fx-border-radius:15;-fx-background-radius:15;");
                redial.setOnAction(e -> { if("📞".equals(icon)) startAudioCall(); else startVideoCall(); });

                VBox right = new VBox(6); right.setAlignment(Pos.CENTER_LEFT);
                right.getChildren().addAll(tb, redial);
                callBox.getChildren().addAll(callIcon, right);

                String time = msg.getDateEnvoi().format(FMT_TIME);
                Label tl = new Label(time); tl.setFont(Font.font("Segoe UI",11)); tl.setTextFill(Color.web("#999"));
                container.getChildren().add(new VBox(3, callBox, tl));
            }

            private String extractCallStatus(String c)   { if(c.contains("ACCEPTÉ"))return "Appel accepté"; if(c.contains("REJETÉ"))return "Appel rejeté"; return "Appel non répondu"; }
            private String extractCallDuration(String c) { try{ int s=c.lastIndexOf("("),e=c.lastIndexOf(")"); if(s!=-1&&e!=-1)return c.substring(s+1,e); }catch(Exception ex){} return "0s"; }

            // ── Attachment / Image / PDF helpers (unchanged) ──────
            private void displayAttachment(VBox container, Message msg, String path) {
                String mime = getMime(path);
                if ("image".equals(mime)) displayImageAttachment(container, msg, path);
                else { HBox bw = buildFileBubble(msg); if("pdf".equals(mime)) displayPdfAttachment(bw,path,msg); else displayGenericFile(bw,path,mime,msg); finishBubble(container,msg,bw); }
            }
            private HBox buildFileBubble(Message msg) {
                HBox b=new HBox(10); b.setAlignment(Pos.CENTER_LEFT); b.setPadding(new Insets(10,14,10,14)); b.setMaxWidth(280);
                b.setStyle("-fx-background-color:" + (msg.getIdUser()==currentUserId?GREEN_OWN:GRAY_OTHER) + ";" +
                        "-fx-background-radius:" + (msg.getIdUser()==currentUserId?"18 18 4 18":"18 18 18 4") + ";");
                return b;
            }
            private void finishBubble(VBox container, Message msg, javafx.scene.layout.Region bubble) {
                String time=msg.getDateEnvoi().format(FMT_TIME);
                boolean own=msg.getIdUser()==currentUserId;
                String status=(own&&"LU".equals(msg.getStatutMessage()))?" ✓✓":(own?" ✓":"");
                Label f=new Label(time+status); f.setStyle("-fx-font-size:9px;-fx-text-fill:#9ca3af;-fx-padding:2 2 0 2;");
                if(own) f.setAlignment(Pos.CENTER_RIGHT);
                VBox mb=new VBox(3,bubble,f); mb.setMaxWidth(bubble.getMaxWidth()+12);
                container.getChildren().add(mb);
            }
            private void displayImageAttachment(VBox container, Message msg, String path) {
                File imgFile=new File("C:\\wamp\\htdocs\\"+path.replace("/","\\"));
                StackPane is=new StackPane(); is.setPrefSize(220,220); is.setMaxSize(220,220);
                is.setStyle("-fx-background-color:#1a1a2e;-fx-background-radius:16;-fx-cursor:hand;");
                ImageView iv=new ImageView(); iv.setFitWidth(220); iv.setFitHeight(220); iv.setPreserveRatio(true);
                Rectangle clip=new Rectangle(220,220); clip.setArcWidth(24); clip.setArcHeight(24); is.setClip(clip);
                is.getChildren().add(iv);
                if(imgFile.exists()) {
                    Image img=new Image(imgFile.toURI().toString()); iv.setImage(img);
                    is.setOnMouseClicked(e->{ Stage s=new Stage(); ImageView bv=new ImageView(img); bv.setPreserveRatio(true); bv.setFitWidth(700); bv.setFitHeight(700); s.setScene(new Scene(new ScrollPane(bv),700,700,Color.BLACK)); s.show(); });
                } else { is.getChildren().add(new Label("❌")); }
                HBox bw=new HBox(is); bw.setMaxWidth(232); bw.setPrefWidth(232); bw.setPadding(new Insets(4));
                boolean own=msg.getIdUser()==currentUserId;
                bw.setStyle("-fx-background-color:"+(own?GREEN_OWN:GRAY_OTHER)+";-fx-background-radius:"+(own?"18 18 4 18":"18 18 18 4")+";");
                finishBubble(container,msg,bw);
            }
            private void displayImageFromUrl(VBox container, Message msg, String imageUrl) {
                StackPane is=new StackPane(); is.setPrefSize(220,220); is.setMaxSize(220,220);
                is.setStyle("-fx-background-color:#1a1a2e;-fx-background-radius:16;");
                ProgressIndicator pi=new ProgressIndicator(); pi.setMaxSize(36,36); is.getChildren().add(pi);
                Rectangle clip=new Rectangle(220,220); clip.setArcWidth(24); clip.setArcHeight(24); is.setClip(clip);
                new Thread(()->{ try{
                    byte[] data=downloadImageWithHeaders(imageUrl);
                    if(data==null||data.length==0) throw new Exception("empty");
                    Image img=new Image(new ByteArrayInputStream(data));
                    Platform.runLater(()->{
                        if(!img.isError()){
                            ImageView iv=new ImageView(img); iv.setFitWidth(220); iv.setFitHeight(220); iv.setPreserveRatio(true); iv.setCursor(Cursor.HAND);
                            is.getChildren().setAll(iv);
                            is.setOnMouseClicked(e->{ Stage s=new Stage(); ImageView bv=new ImageView(img); bv.setPreserveRatio(true); bv.setFitWidth(700); bv.setFitHeight(700); s.setScene(new Scene(new ScrollPane(bv),700,700,Color.BLACK)); s.show(); });
                        } else is.getChildren().setAll(new Label("❌"));
                    });
                } catch(Exception ex){ Platform.runLater(()->is.getChildren().setAll(new Label("❌"))); }}).start();
                boolean own=msg.getIdUser()==currentUserId;
                HBox bw=new HBox(is); bw.setMaxWidth(232); bw.setPrefWidth(232); bw.setPadding(new Insets(4));
                bw.setStyle("-fx-background-color:"+(own?GREEN_OWN:GRAY_OTHER)+";-fx-background-radius:"+(own?"18 18 4 18":"18 18 18 4")+";");
                finishBubble(container,msg,bw);
            }
            private void displayPdfFromUrl(VBox container, Message msg, String url) {
                HBox bw=buildFileBubble(msg); HBox pb=new HBox(10); pb.setAlignment(Pos.CENTER_LEFT);
                Label ic=new Label("📄"); ic.setFont(Font.font("Segoe UI Emoji",32));
                Label fn=new Label(getFileNameFromUrl(url)); fn.setFont(Font.font("Segoe UI",13));
                fn.setTextFill(msg.getIdUser()==currentUserId?Color.WHITE:Color.BLACK);
                Button ob=new Button("Ouvrir"); ob.setStyle("-fx-background-color:#3498db;-fx-text-fill:white;-fx-background-radius:15;-fx-padding:5 15;");
                ob.setOnAction(e->{ try{ Desktop.getDesktop().browse(new URI(url)); }catch(Exception ex){ showAlert("Erreur","Impossible d'ouvrir le lien."); } });
                pb.getChildren().addAll(ic,fn,ob); bw.getChildren().add(pb); finishBubble(container,msg,bw);
            }
            private void displayPdfAttachment(HBox bw,String path,Message msg){ HBox pb=new HBox(10); pb.setAlignment(Pos.CENTER_LEFT); Label ic=new Label("📄"); ic.setFont(Font.font("Segoe UI Emoji",32)); Label fn=new Label(new File(path).getName()); fn.setFont(Font.font("Segoe UI",13)); fn.setTextFill(msg.getIdUser()==currentUserId?Color.WHITE:Color.BLACK); Button ob=new Button("Ouvrir"); ob.setStyle("-fx-background-color:#3498db;-fx-text-fill:white;-fx-background-radius:15;-fx-padding:5 15;"); ob.setOnAction(e->openFile(path)); pb.getChildren().addAll(ic,fn,ob); bw.getChildren().add(pb); }
            private void displayGenericFile(HBox bw,String path,String mime,Message msg){ HBox fb=new HBox(10); fb.setAlignment(Pos.CENTER_LEFT); Label ic=new Label(getFileIcon(mime)); ic.setFont(Font.font("Segoe UI Emoji",32)); Label fn=new Label(new File(path).getName()); fn.setFont(Font.font("Segoe UI",13)); fn.setTextFill(msg.getIdUser()==currentUserId?Color.WHITE:Color.BLACK); Button ob=new Button("Ouvrir"); ob.setStyle("-fx-background-color:#3498db;-fx-text-fill:white;-fx-background-radius:15;-fx-padding:5 15;"); ob.setOnAction(e->openFile(path)); fb.getChildren().addAll(ic,fn,ob); bw.getChildren().add(fb); }
            private void openFile(String rel){ try{ File f=new File("C:\\wamp\\htdocs\\"+rel.replace("/","\\")); if(f.exists()) Desktop.getDesktop().open(f); else showAlert("Erreur","Fichier introuvable : "+f.getAbsolutePath()); }catch(IOException ex){ showAlert("Erreur","Impossible d'ouvrir le fichier."); } }
            private String getMime(String p){ String e=p.substring(p.lastIndexOf('.')+1).toLowerCase(); return switch(e){ case"png","jpg","jpeg","gif","bmp","webp"->"image"; case"pdf"->"pdf"; case"docx","doc"->"word"; default->"file"; }; }
            private String getFileIcon(String m){ return switch(m){ case"word"->"📝"; case"pdf"->"📄"; default->"📎"; }; }
            private String getFileNameFromUrl(String u){ try{ String p=new URI(u).getPath(); return p.substring(p.lastIndexOf('/')+1); }catch(Exception e){ return "document"; } }
            private boolean isImageUrl(String u){ return u.toLowerCase().matches("(?i).*\\.(jpg|jpeg|png|gif|bmp|webp)(\\?.*)?$"); }
            private boolean isPdfUrl(String u)  { return u.toLowerCase().matches("(?i).*\\.pdf(\\?.*)?$"); }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  CONTEXT MENU  (edit / delete / react)
    // ════════════════════════════════════════════════════════════
    private void setupMessageContextMenu(TextFlow tf, Message msg) {
        ContextMenu menu = new ContextMenu();
        if (msg.getIdUser() == currentUserId) {
            MenuItem edit = new MenuItem("Modifier ✏️");
            edit.setOnAction(e -> {
                TextInputDialog d = new TextInputDialog(msg.getContenu());
                d.showAndWait().ifPresent(v -> {
                    if (!v.trim().isEmpty() && messageDAO.updateMessageContent(msg.getIdMessage(), v))
                        loadMessages(selectedConversationId);
                });
            });
            menu.getItems().add(edit);
        }
        // React option
        MenuItem react = new MenuItem("😊 Réagir");
        react.setOnAction(e -> {
            String[] emojis = {"❤️","👍","😂","😮","😢","🎉","🔥","🙏"};
            ContextMenu picker = new ContextMenu();
            HBox box = new HBox(4);
            for (String em : emojis) {
                Button b = new Button(em);
                b.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-font-size:18;-fx-padding:2;");
                b.setOnAction(ev -> {
                    reactionsMap.computeIfAbsent(msg.getIdMessage(), k -> new LinkedHashMap<>())
                            .merge(em, 1, Integer::sum);
                    picker.hide(); menu.hide();
                    loadMessages(selectedConversationId);
                });
                box.getChildren().add(b);
            }
            picker.getItems().add(new CustomMenuItem(box, false));
            picker.show(tf, Side.TOP, 0, 0);
        });
        menu.getItems().add(react);

        if (currentUserId == 2 || msg.getIdUser() == currentUserId) {
            MenuItem del = new MenuItem("Supprimer 🗑️");
            del.setOnAction(e -> { if (messageDAO.deleteMessage(msg.getIdMessage())) loadMessages(selectedConversationId); });
            menu.getItems().add(del);
        }
        if (!menu.getItems().isEmpty())
            tf.setOnContextMenuRequested(e -> menu.show(tf, e.getScreenX(), e.getScreenY()));
    }

    // ════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ════════════════════════════════════════════════════════════

    private void loadConversations() {
        allConversations = conversationDAO.getAllConversations(currentUserId);
        // Populate last-message time map for Date sort
        if (allConversations != null) {
            lastMsgTimeMap.clear();
            for (Conversation c : allConversations) {
                List<Message> msgs = messageDAO.getMessagesByConversation(c.getIdConversation());
                if (msgs != null && !msgs.isEmpty()) {
                    msgs.stream().max(Comparator.comparing(Message::getDateEnvoi))
                            .ifPresent(last -> lastMsgTimeMap.put(c.getIdConversation(), last.getDateEnvoi()));
                }
            }
        }
        applyFilter();
    }

    private void loadMessages(int conversationId) {
        List<Message> messages = messageDAO.getMessagesByConversation(conversationId);
        messages.sort(Comparator.comparing(Message::getDateEnvoi));

        // Update last-five for translation
        lastFiveMessageIds.clear();
        int sz = messages.size();
        for (int i = Math.max(0, sz-5); i < sz; i++)
            lastFiveMessageIds.add(messages.get(i).getIdMessage());

        // Update lastMsgTimeMap
        if (!messages.isEmpty())
            lastMsgTimeMap.put(conversationId, messages.get(messages.size()-1).getDateEnvoi());

        messagesList.setItems(FXCollections.observableArrayList(messages));
        if (!messages.isEmpty())
            Platform.runLater(() -> messagesList.scrollTo(messages.size()-1));
    }

    private void startAutoRefresh() {
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (selectedConversationId != -1) {
                messageDAO.markAsRead(selectedConversationId, currentUserId);
                loadMessages(selectedConversationId);
            }
        }));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, ov, nv) -> applyFilter());
    }

    // ════════════════════════════════════════════════════════════
    //  FXML HANDLERS
    // ════════════════════════════════════════════════════════════

    @FXML private void sendMessage() {
        if (selectedConversationId == -1) return;
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message("TEXTE", text, "ENVOYE", LocalDateTime.now(), selectedConversationId, currentUserId);
        if (messageDAO.addMessage(m)) { messageInput.clear(); loadMessages(selectedConversationId); }
    }

    @FXML private void sendLike() {
        if (selectedConversationId == -1) return;
        Message m = new Message("TEXTE","❤️","ENVOYE",LocalDateTime.now(),selectedConversationId,currentUserId);
        messageDAO.addMessage(m); loadMessages(selectedConversationId);
    }

    @FXML private void handleKeyPress(KeyEvent e) { if (e.getCode()==KeyCode.ENTER) sendMessage(); }

    @FXML private void toggleAI() {
        aiVisible = !aiVisible;
        aiPanel.setVisible(aiVisible);
        aiPanel.setManaged(aiVisible);
    }

    @FXML private void showEmojiPicker(ActionEvent event) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color:white;-fx-background-radius:15;" +
                "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.1),10,0,0,5);");
        FlowPane fp = new FlowPane(); fp.setPrefWidth(260); fp.setHgap(8); fp.setVgap(8); fp.setPadding(new Insets(12));
        String[] emojis = {"😀","😃","😄","😁","😆","😅","😂","🤣","😊","😇","🙂","😉","😍","🥰","😘","😎","🤩","🥳","😏","😒","❤️","🧡","💛","💚","💙","💜","🔥","✨","🌟","🚀","💯","👍","👎","👏","🙏","✅","❌"};
        for (String em : emojis) {
            Button b = new Button(em); b.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:3;"); b.setFont(Font.font("Segoe UI Emoji",20));
            b.setOnAction(e -> { messageInput.appendText(em); messageInput.requestFocus(); });
            fp.getChildren().add(b);
        }
        CustomMenuItem ci = new CustomMenuItem(fp); ci.setHideOnClick(false); menu.getItems().add(ci);
        menu.show((Button)event.getSource(), Side.TOP, 0, -10);
    }

    @FXML private void handleAttachFile(ActionEvent event) {
        if (selectedConversationId == -1) { showAlert("Attention","Sélectionnez d'abord une conversation."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir un fichier");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous","*.*"),
                new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg","*.gif"),
                new FileChooser.ExtensionFilter("Vidéos","*.mp4","*.avi","*.mov","*.mkv","*.flv","*.wmv"),
                new FileChooser.ExtensionFilter("PDF","*.pdf"),
                new FileChooser.ExtensionFilter("Documents","*.docx","*.xlsx","*.pptx")
        );
        File f = fc.showOpenDialog(((Node)event.getSource()).getScene().getWindow());
        if (f == null) return;
        String name = f.getName().toLowerCase();
        String type = name.matches(".*\\.(mp4|avi|mov|mkv|flv|wmv)$") ? "VIDEO" : "FICHIER";
        String sub  = type.equals("VIDEO") ? "video" : "files";
        String path = uploadFile(f, sub);
        if (path != null) {
            Message msg = new Message(type, path, "ENVOYE", LocalDateTime.now(), selectedConversationId, currentUserId);
            if (messageDAO.addMessage(msg)) loadMessages(selectedConversationId);
        } else showAlert("Erreur","Échec de l'upload.");
    }

    private String uploadFile(File file, String sub) {
        try {
            File dir = new File("C:\\wamp\\htdocs\\uploads\\" + sub);
            dir.mkdirs();
            String fn = System.currentTimeMillis() + "_" + file.getName();
            Files.copy(file.toPath(), new File(dir, fn).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "uploads/" + sub + "/" + fn;
        } catch (IOException e) { e.printStackTrace(); return null; }
    }

    @FXML private void handleRecordButton() {
        try {
            if (!isRecording) {
                String fn = "voice_"+currentUserId+"_"+System.currentTimeMillis()+".wav";
                String ap = "C:\\wamp\\htdocs\\uploads\\Audio\\"+fn;
                new File("C:\\wamp\\htdocs\\uploads\\Audio").mkdirs();
                recorder = new AudioRecorder(); recorder.startRecording(ap);
                recordButton.setText("⏹"); recordButton.setStyle("-fx-background-color:#fee2e2;-fx-background-radius:50;-fx-cursor:hand;-fx-font-size:20;");
                isRecording = true;
                Timeline timeline = new Timeline(new KeyFrame(Duration.minutes(2), e -> {
                    if (isRecording) stopRecordingAndSave(ap);
                }));
                timeline.setCycleCount(1);
                timeline.play();

            } else {
                stopRecordingAndSave(recorder.getAudioFilePath());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopRecordingAndSave(String ap) {
        try {
            recorder.stopRecording();
            recordButton.setText("🎙️"); recordButton.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-font-size:20;");
            isRecording = false;
            String rel = "uploads/Audio/"+new File(ap).getName();
            messageDAO.addVocalMessage(rel, selectedConversationId, currentUserId);
            loadMessages(selectedConversationId);
            processVoiceToText(ap);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleAiRecord() {
        try {
            if (!isAiRecording) {
                aiRecorder = new AudioRecorder();
                String ap = System.getProperty("java.io.tmpdir")+File.separator+"ai_voice_"+System.currentTimeMillis()+".wav";
                aiRecorder.startRecording(ap);
                isAiRecording = true; aiRecordButton.setText("⏹");
                recordingLimitTimer = new Timeline(new KeyFrame(Duration.seconds(120), e -> { if(isAiRecording) handleAiRecord(); }));
                recordingLimitTimer.setCycleCount(1); recordingLimitTimer.play();
            } else {
                if (recordingLimitTimer!=null) recordingLimitTimer.stop();
                aiRecorder.stopRecording(); isAiRecording = false; aiRecordButton.setText("🎙️");
                String ap = aiRecorder.getAudioFilePath();
                String userText = convertVoiceToText(ap);
                if (userText==null||userText.trim().isEmpty()) { addBotBubble("Je n'ai pas compris. Réessayez.",false); return; }
                addBotBubble(userText, true);
                String ft = userText;
                new Thread(()->{ try{ String r=GeminiService.askGemini(ft); Platform.runLater(()->addBotBubble(r,false)); }catch(Exception e){ Platform.runLater(()->addBotBubble("⚠️ Erreur IA.",false)); } }).start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void askBot() {
        String q = botInput.getText().trim();
        if (q.isEmpty()) return;
        addBotBubble(q, true); botInput.clear();
        if (aiLoader!=null) aiLoader.setVisible(true);
        new Thread(()->{ try{ String r=GeminiService.askGemini(q); Platform.runLater(()->{ if(aiLoader!=null) aiLoader.setVisible(false); addBotBubble(r,false); }); }catch(Exception e){ Platform.runLater(()->{ if(aiLoader!=null) aiLoader.setVisible(false); addBotBubble("⚠️ Erreur Gemini!",false); }); } }).start();
    }

    private void addBotBubble(String text, boolean isUser) {
        if (botMessagesContainer==null) return;
        Label lbl = new Label(text); lbl.setWrapText(true); lbl.setMaxWidth(220);
        lbl.setFont(Font.font("Segoe UI",13));
        HBox wrapper = new HBox(lbl);
        if (isUser) { lbl.setStyle("-fx-background-color:#e1f5fe;-fx-padding:10;-fx-background-radius:15 15 0 15;-fx-text-fill:#0d47a1;"); wrapper.setAlignment(Pos.CENTER_RIGHT); VBox.setMargin(wrapper,new Insets(5,0,5,40)); }
        else         { lbl.setStyle("-fx-background-color:#fff;-fx-padding:10;-fx-background-radius:15 15 15 0;-fx-border-color:#efefef;-fx-text-fill:#333;"); wrapper.setAlignment(Pos.CENTER_LEFT); VBox.setMargin(wrapper,new Insets(5,40,5,0)); }
        botMessagesContainer.getChildren().add(wrapper);
        if (botMessagesContainer.getParent() instanceof ScrollPane sp) sp.setVvalue(1.0);
    }

    @FXML private void openGifPicker() {
        Stage stage = new Stage(); stage.setTitle("Search GIF");
        VBox root = new VBox(15); root.setPadding(new Insets(15)); root.setAlignment(Pos.TOP_CENTER);
        HBox sb = new HBox(10); TextField sf = new TextField(); sf.setPromptText("Tapez votre recherche..."); sf.setPrefWidth(320);
        Button searchBtn = new Button("Rechercher"); searchBtn.setDefaultButton(true); sb.getChildren().addAll(sf,searchBtn);
        ProgressIndicator prog = new ProgressIndicator(); prog.setVisible(false); prog.setMaxSize(40,40);
        FlowPane gp = new FlowPane(); gp.setHgap(15); gp.setVgap(15); gp.setPadding(new Insets(10)); gp.setAlignment(Pos.TOP_LEFT);
        ScrollPane sp = new ScrollPane(gp); sp.setFitToWidth(true); sp.setPrefHeight(350); sp.setStyle("-fx-background-color:transparent;");
        root.getChildren().addAll(sb,prog,sp);
        searchBtn.setOnAction(e -> {
            String q = sf.getText().trim(); if(q.isEmpty()) return;
            gp.getChildren().clear(); prog.setVisible(true);
            new Thread(()->{ try{
                JSONArray gifs=gifService.searchGifs(q,12);
                Platform.runLater(()->{ prog.setVisible(false);
                    if(gifs.length()==0){ gp.getChildren().add(new Label("❌ Aucun GIF pour : "+q)); return; }
                    for(int i=0;i<gifs.length();i++){ try{
                        JSONObject go=gifs.getJSONObject(i); String url=go.getString("url");
                        VBox gc=new VBox(); gc.setAlignment(Pos.CENTER); gc.setPrefSize(120,120);
                        gc.setStyle("-fx-border-color:#ddd;-fx-border-radius:8;-fx-padding:5;-fx-background-color:#f5f5f5;");
                        ProgressIndicator gpi=new ProgressIndicator(); gpi.setMaxSize(30,30); gc.getChildren().add(gpi);
                        String fu=url;
                        new Thread(()->{ try{ byte[] d=downloadImageWithHeaders(fu); if(d==null||d.length==0) throw new Exception("empty");
                            Image img=new Image(new ByteArrayInputStream(d));
                            Platform.runLater(()->{ if(!img.isError()&&img.getWidth()>0){
                                ImageView iv=new ImageView(img); iv.setFitWidth(120); iv.setFitHeight(120); iv.setPreserveRatio(true); iv.setCursor(Cursor.HAND);
                                gc.getChildren().clear(); gc.getChildren().add(iv); gc.setCursor(Cursor.HAND);
                                gc.setOnMouseEntered(ev->{ gc.setStyle("-fx-border-color:#22c55e;-fx-border-width:2;-fx-border-radius:8;-fx-padding:5;-fx-background-color:#f0fdf4;"); gc.setScaleX(1.05); gc.setScaleY(1.05); });
                                gc.setOnMouseExited(ev->{ gc.setStyle("-fx-border-color:#ddd;-fx-border-radius:8;-fx-padding:5;-fx-background-color:#f5f5f5;"); gc.setScaleX(1.0); gc.setScaleY(1.0); });
                                gc.setOnMouseClicked(ev->{ if(messageInput!=null) messageInput.appendText(" "+fu+" "); stage.close(); });
                            } else gc.getChildren().setAll(new Label("❌")); });
                        } catch(Exception ex){ Platform.runLater(()->gc.getChildren().setAll(new Label("❌"))); }}).start();
                        gp.getChildren().add(gc);
                    } catch(Exception ex){} }
                });
            } catch(Exception ex){ Platform.runLater(()->{ prog.setVisible(false); showAlert("Erreur GIF",ex.getMessage()); }); }}).start();
        });
        stage.setScene(new Scene(root,500,500)); stage.show();
    }

    @FXML public void showChatOptions() {
        if (selectedConversationId==-1) { showAlert("Attention","Sélectionnez une conversation."); return; }
        Conversation sel = conversationsList.getSelectionModel().getSelectedItem();
        if (sel==null) { showAlert("Attention","Aucune conversation sélectionnée."); return; }
        ContextMenu cm = new ContextMenu(); cm.setStyle("-fx-font-size:13;");

        if (sel.getEstGroupe()==1) {
            MenuItem members = new MenuItem("Afficher les membres 👥");
            members.setOnAction(e -> { try{ List<String> m=conversationDAO.getMembresByConversation(selectedConversationId);
                if(m==null||m.isEmpty()){ showAlert("Membres","Aucun membre trouvé."); return; }
                ListView<String> lv=new ListView<>(FXCollections.observableArrayList(m)); lv.setPrefHeight(250);
                Dialog<Void> d=new Dialog<>(); d.setTitle("Membres"); d.setHeaderText("Participants ("+m.size()+")");
                d.getDialogPane().setContent(lv); d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); d.showAndWait();
            }catch(Exception ex){ showAlert("Erreur",ex.getMessage()); } });

            MenuItem addMember = new MenuItem("Ajouter un membre ➕");
            addMember.setOnAction(e -> { try{ List<String> all=conversationDAO.getAllAppUsers();
                if(all==null||all.isEmpty()){ showAlert("Info","Aucun utilisateur disponible."); return; }
                List<String> cur=conversationDAO.getMembresByConversation(selectedConversationId);
                List<String> avail=all.stream().filter(u->!cur.stream().anyMatch(m->m.contains(u.split("\\|")[0]))).collect(Collectors.toList());
                if(avail.isEmpty()){ showAlert("Info","Tous les utilisateurs sont déjà membres."); return; }
                ChoiceDialog<String> cd=new ChoiceDialog<>(avail.get(0),avail); cd.setTitle("Ajouter"); cd.setContentText("Utilisateur :");
                cd.showAndWait().ifPresent(su->{ try{ String[] p=su.split("\\|"); String id=p.length>1?p[1].trim():"";
                    boolean added=id.matches("\\d+")?conversationDAO.addMemberToConversation(selectedConversationId,Integer.parseInt(id)):conversationDAO.addMemberToConversation(selectedConversationId,id);
                    showAlert(added?"Succès":"Erreur",added?p[0]+" ajouté ✅":"Impossible d'ajouter le membre.");
                }catch(Exception ex){ showAlert("Erreur",ex.getMessage()); } });
            }catch(Exception ex){ showAlert("Erreur",ex.getMessage()); } });

            cm.getItems().addAll(members, addMember, new SeparatorMenuItem());
        }

        MenuItem editTitle = new MenuItem("Modifier le nom ✏️");
        editTitle.setOnAction(e -> { TextInputDialog d=new TextInputDialog(chatUserName.getText()); d.setTitle("Modifier le nom"); d.setContentText("Nouveau nom :"); d.showAndWait().ifPresent(n->{ if(n.trim().isEmpty()){ showAlert("Attention","Le nom ne peut pas être vide."); return; } if(conversationDAO.updateConversationTitle(selectedConversationId,n.trim())){ chatUserName.setText(n.trim()); loadConversations(); showAlert("Succès","Nom modifié ✅"); }else showAlert("Erreur","Impossible de modifier."); }); });

        MenuItem deleteChat = new MenuItem("Supprimer la conversation 🗑️");
        deleteChat.setOnAction(e -> { Alert conf=new Alert(Alert.AlertType.CONFIRMATION,"Supprimer définitivement ?",ButtonType.YES,ButtonType.CANCEL); conf.setTitle("Supprimer"); conf.showAndWait().ifPresent(r->{ if(r==ButtonType.YES){ if(conversationDAO.deleteConversation(selectedConversationId)){ selectedConversationId=-1; chatUserName.setText("Sélectionnez un chat"); chatTypeBadge.setText(""); messagesList.setItems(null); loadConversations(); }else showAlert("Erreur","Impossible de supprimer."); } }); });

        MenuItem translate = new MenuItem("Traduire la conversation 🌐");
        translate.setOnAction(e -> showLanguageMenu(chatHeader));

        cm.getItems().addAll(editTitle, deleteChat, translate);
        cm.show(chatHeader, Side.BOTTOM, 0, 0);
    }

    @FXML private void addNewConversation() {
        List<String> choices = List.of("Message Privé 👤","Groupe 👥");
        ChoiceDialog<String> td = new ChoiceDialog<>("Message Privé 👤",choices);
        td.setTitle("Nouvelle Conversation"); td.setHeaderText("Type de conversation ?"); td.setContentText("Type :");
        td.showAndWait().ifPresent(type -> {
            if (type.contains("Privé")) {
                try { List<String> all=conversationDAO.getAllAppUsers();
                    if(all==null||all.isEmpty()){ showAlert("Info","Aucun utilisateur disponible."); return; }
                    List<String> avail=all.stream().filter(u->{ try{ String[]p=u.split("\\|"); String id=p.length>1?p[1].trim():""; return !id.equals(String.valueOf(currentUserId)); }catch(Exception ex){ return true; } }).collect(Collectors.toList());
                    if(avail.isEmpty()){ showAlert("Info","Aucun utilisateur disponible."); return; }
                    ChoiceDialog<String> cd=new ChoiceDialog<>(avail.get(0),avail); cd.setTitle("Nouveau chat privé"); cd.setContentText("Utilisateur :");
                    cd.showAndWait().ifPresent(su->{ try{ String[]p=su.split("\\|"); String name=p[0].trim(); String id=p.length>1?p[1].trim():"";
                        int newId=conversationDAO.addConversation(new Conversation(name,0));
                        if(newId!=-1){ conversationDAO.addMemberToConversation(newId,currentUserId); if(id.matches("\\d+")) conversationDAO.addMemberToConversation(newId,Integer.parseInt(id)); else conversationDAO.addMemberToConversation(newId,id); loadConversations(); showAlert("Succès","Conversation créée avec "+name+" ✅"); }
                    }catch(Exception ex){ showAlert("Erreur",ex.getMessage()); } });
                } catch(Exception ex){ showAlert("Erreur",ex.getMessage()); }
            } else {
                TextInputDialog ni=new TextInputDialog(); ni.setTitle("Créer un groupe"); ni.setContentText("Nom du groupe :"); ni.getEditor().setPromptText("Ex: Projet 2024...");
                ni.showAndWait().ifPresent(name->{ if(name.trim().isEmpty()){ showAlert("Attention","Le nom ne peut pas être vide."); return; }
                    try{ int newId=conversationDAO.addConversation(new Conversation(name.trim(),1));
                        if(newId!=-1){ conversationDAO.addMemberToConversation(newId,currentUserId); loadConversations(); showAlert("Succès","Groupe \""+name.trim()+"\" créé ✅"); }
                    }catch(Exception ex){ showAlert("Erreur",ex.getMessage()); } });
            }
        });
    }

    @FXML public void startAudioCall() {
        if (selectedConversationId==-1||"Sélectionnez un chat".equals(chatUserName.getText())) { showAlert("Attention","Sélectionnez un contact."); return; }
        try { FXMLLoader l=new FXMLLoader(getClass().getResource("/Gui/CallingWindow.fxml")); Parent root=l.load(); CallingController c=l.getController();
            Stage st=new Stage(); st.initStyle(StageStyle.TRANSPARENT); Scene sc=new Scene(root); sc.setFill(null); st.setScene(sc);
            c.setContactData(chatUserName.getText(),st); c.setCallData(selectedConversationId,currentUserId,"AUDIO_CALL",messageDAO);
            sendCallSignal(selectedConversationId,"AUDIO_CALL"); makeStageDraggable(root,st); st.show();
        } catch(IOException e){ showAlert("Erreur","Impossible de charger la fenêtre d'appel."); }
    }

    @FXML private void startVideoCall() {
        if (selectedConversationId==-1||"Sélectionnez un chat".equals(chatUserName.getText())) { showAlert("Attention","Sélectionnez un contact."); return; }
        try { FXMLLoader l=new FXMLLoader(getClass().getResource("/Gui/CallingWindow.fxml")); Parent root=l.load(); CallingController c=l.getController();
            Stage st=new Stage(); st.initStyle(StageStyle.TRANSPARENT); Scene sc=new Scene(root); sc.setFill(null); st.setScene(sc);
            c.setContactData(chatUserName.getText(),st); c.setCallData(selectedConversationId,currentUserId,"VIDEO_CALL",messageDAO);
            sendCallSignal(selectedConversationId,"VIDEO_CALL"); makeStageDraggable(root,st); st.show();
        } catch(IOException e){ showAlert("Erreur","Impossible de démarrer l'appel vidéo."); }
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS (all originals kept)
    // ════════════════════════════════════════════════════════════

    private void sendCallSignal(int convId, String callType) {
        if (stompHandler!=null&&stompHandler.isConnected()) {
            try { Map<String,Object> d=new HashMap<>(); d.put("senderName",chatUserName.getText()); d.put("senderId",currentUserId); d.put("conversationId",convId); d.put("type",callType);
                stompHandler.send("/app/call.start", new ObjectMapper().writeValueAsString(d).getBytes(StandardCharsets.UTF_8));
            } catch(Exception e){ e.printStackTrace(); }
        }
    }

    private void initializeStompClient() {
        try { stompHandler=new StompClientHandler("http://localhost:8080/ws",currentUserId); }
        catch(Exception e){ System.err.println("❌ STOMP init error: "+e.getMessage()); stompHandler=null; }
    }

    private void processIncomingCall(String payload) {
        try { ObjectMapper m=new ObjectMapper(); Map<String,Object> d=m.readValue(payload,Map.class);
            String sn=(String)d.get("senderName"); int si=((Number)d.get("senderId")).intValue();
            int ci=((Number)d.get("conversationId")).intValue(); String ct=(String)d.get("type");
            onIncomingCallReceived(sn,si,ci,ct);
        } catch(Exception e){ e.printStackTrace(); }
    }

    private void onIncomingCallReceived(String sn,int sid,int cid,String ct) {
        Platform.runLater(()->{
            try { FXMLLoader l=new FXMLLoader(getClass().getResource("/Gui/CallWindow.fxml")); Parent root=l.load(); CallController c=l.getController();
                Stage st=new Stage(); c.setCallerData(sn,st); c.setCallData(cid,currentUserId,ct,messageDAO); st.setScene(new Scene(root)); st.setTitle("Appel entrant de "+sn); st.show();
            } catch(IOException e){ e.printStackTrace(); }
        });
    }

    public void onCallCancelledBySender() { if(currentIncomingCallController!=null) currentIncomingCallController.forceClose(); }

    private void processVoiceToText(String ap) {
        try { Model model=new Model("src/main/resources/models/vosk-model-small-en-us-0.15");
            Recognizer rec=new Recognizer(model,16000); InputStream is=new FileInputStream(ap); byte[] buf=new byte[4096]; int n;
            while((n=is.read(buf))!=-1) rec.acceptWaveForm(buf,n); is.close();
            String json=rec.getFinalResult(); String text=new ObjectMapper().readTree(json).get("text").asText();
            Platform.runLater(()->{ botInput.setText(text); askBot(); });
            rec.close(); model.close();
        } catch(Exception e){ e.printStackTrace(); }
    }

    private String convertVoiceToText(String ap) {
        try { Model m=new Model("C:\\Users\\alakh\\OneDrive\\Bureau\\3A3-Les_Experts\\src\\main\\resources\\models\\vosk-model-small-en-us-0.15");
            try(Recognizer rec=new Recognizer(m,16000)){ AudioInputStream ais=AudioSystem.getAudioInputStream(new File(ap)); byte[] buf=new byte[4096]; int n; while((n=ais.read(buf))>=0) rec.acceptWaveForm(buf,n); return new ObjectMapper().readTree(rec.getFinalResult()).get("text").asText(); }
        } catch(Exception e){ e.printStackTrace(); return ""; }
    }

    private void showLanguageMenu(Node anchor) {
        ContextMenu m=new ContextMenu();
        String[][] langs={{"Français","fr"},{"English","en"},{"العربية","ar"},{"Español","es"},{"Deutsch","de"},{"中文","zh"},{"日本語","ja"},{"Русский","ru"}};
        for (String[] l : langs) { MenuItem it=new MenuItem(l[0]); it.setOnAction(e->{ currentTargetLanguage=l[1]; loadMessages(selectedConversationId); }); m.getItems().add(it); }
        m.show(anchor, Side.BOTTOM, 0, 0);
    }

    private void refreshSingleMessage(int id) { Platform.runLater(()->{ try{ loadMessages(selectedConversationId); }catch(Exception e){ e.printStackTrace(); } }); }

    private void makeStageDraggable(Parent root, Stage stage) {
        final double[] x={0}; final double[] y={0};
        root.setOnMousePressed(e->{ x[0]=e.getSceneX(); y[0]=e.getSceneY(); });
        root.setOnMouseDragged(e->{ stage.setX(e.getScreenX()-x[0]); stage.setY(e.getScreenY()-y[0]); });
    }

    private byte[] downloadImageWithHeaders(String urlStr) throws Exception {
        URL url=new URL(urlStr); HttpURLConnection conn=(HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET"); conn.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setRequestProperty("Accept","image/webp,image/apng,image/*,*/*;q=0.8");
        conn.setRequestProperty("Referer","https://giphy.com/"); conn.setConnectTimeout(5000); conn.setReadTimeout(5000);
        if(conn.getResponseCode()!=200) throw new Exception("HTTP "+conn.getResponseCode());
        ByteArrayOutputStream buf=new ByteArrayOutputStream(); InputStream is=conn.getInputStream(); byte[] d=new byte[4096]; int n;
        while((n=is.read(d,0,d.length))!=-1) buf.write(d,0,n); is.close(); conn.disconnect(); return buf.toByteArray();
    }

    private void showAlert(String title, String msg) {
        Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.show();
    }

    private void addMessageToChat(String sender, String message) {
        Label lbl=new Label(sender+": "+message); lbl.setWrapText(true); lbl.setStyle("-fx-background-color:#e1f5fe;-fx-padding:8;-fx-background-radius:10;");
        Platform.runLater(()->{ if(chatVBox!=null) chatVBox.getChildren().add(lbl); addBotBubble(message,"You".equalsIgnoreCase(sender)); });
    }

    // ── Static helpers ──────────────────────────────────────────
    static String buildInitials(String name) {
        if (name==null||name.isBlank()) return "?";
        String[] p=name.trim().split("\\s+");
        if (p.length>=2) return (p[0].charAt(0)+""+p[1].charAt(0)).toUpperCase();
        return name.substring(0,Math.min(2,name.length())).toUpperCase();
    }

    static String avatarColor(String name) {
        return AVATAR_PALETTE[Math.abs(name.hashCode())%AVATAR_PALETTE.length];
    }

    static String fmtDate(LocalDateTime dt) {
        if (dt==null) return "";
        LocalDate today=LocalDate.now();
        if (dt.toLocalDate().equals(today)) return dt.format(FMT_TIME);
        if (dt.toLocalDate().equals(today.minusDays(1))) return "Hier";
        return dt.format(FMT_DATE);
    }
}