package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.application.Platform;
import Services.interfaces.MessageDAO;
import Entities.Message;
import Services.interfaces.AudioRecorder;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;

public class CallController {

    @FXML private Label callerName;
    @FXML private Label callStatus;
    @FXML private Circle profileCircle;
    @FXML private Button acceptButton;
    @FXML private Button rejectButton;

    private Stage stage;
    private MediaPlayer mediaPlayer;

    private Timeline callTimeoutTimer;
    private int remainingSeconds = 30;
    private static final int CALL_TIMEOUT_SECONDS = 30;

    private int conversationId = -1;
    private int currentUserId = -1;
    private String callType = "AUDIO_CALL";
    private MessageDAO messageDAO;
    private long callStartTime;

    private AudioRecorder callRecorder;
    private String callAudioPath;
    private boolean callAccepted = false;

    private String callerNameStr = "";

    // ✅ Protection contre les doubles clics
    private boolean isRecordingStarted = false;

    /**
     * Called automatically after FXML loading.
     * Use this to verify that all FXML components are injected.
     */
    @FXML
    public void initialize() {
        System.out.println("📱 CallController initialized");
        if (callerName == null) System.err.println("⚠️ callerName is NULL ! Check fx:id in FXML.");
        if (callStatus == null) System.err.println("⚠️ callStatus is NULL ! Check fx:id in FXML.");
        if (profileCircle == null) System.err.println("⚠️ profileCircle is NULL ! Check fx:id in FXML.");
        if (acceptButton == null) System.err.println("⚠️ acceptButton is NULL ! Check fx:id in FXML.");
        if (rejectButton == null) System.err.println("⚠️ rejectButton is NULL ! Check fx:id in FXML.");
    }

    public void setCallerData(String name, Stage stage) {
        Platform.runLater(() -> {
            this.callerNameStr = name;
            this.stage = stage;

            if (this.callerName != null) {
                this.callerName.setText(name);
                System.out.println("📞 Incoming call from: " + name);
            } else {
                System.err.println("❌ ERROR: callerName FXML component is NULL – cannot set caller name.");
            }

            playIncomingCallSound();
            callStartTime = System.currentTimeMillis();
            startCallTimeout();
        });
    }

    public void setCallData(int conversationId, int currentUserId, String callType, MessageDAO messageDAO) {
        this.conversationId = conversationId;
        this.currentUserId = currentUserId;
        this.callType = callType;
        this.messageDAO = messageDAO;

        System.out.println("✅ Call metadata set:");
        System.out.println("   Conversation: " + conversationId);
        System.out.println("   User: " + currentUserId);
        System.out.println("   Type: " + callType);
    }

    private void playIncomingCallSound() {
        try {
            URL resource = getClass().getResource("/sounds/Call.mp3");
            if (resource != null) {
                Media sound = new Media(resource.toString());
                mediaPlayer = new MediaPlayer(sound);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.play();
                System.out.println("🔔 Incoming call ringtone playing...");
            } else {
                System.out.println("⚠️ Call sound file not found");
            }
        } catch (Exception e) {
            System.err.println("❌ Error playing call sound: " + e.getMessage());
        }
    }

    private void startCallTimeout() {
        remainingSeconds = CALL_TIMEOUT_SECONDS;
        updateCallStatus();

        callTimeoutTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateCallStatus();

            if (remainingSeconds <= 0) {
                callTimeoutTimer.stop();
                System.out.println("⏱️ Call timeout - auto-rejecting");
                rejectCallAutomatically();
            }
        }));

        callTimeoutTimer.setCycleCount(CALL_TIMEOUT_SECONDS);
        callTimeoutTimer.play();

        System.out.println("⏱️ Call timeout timer started: 30 seconds");
    }

    private void updateCallStatus() {
        if (callStatus == null) return; // safety check
        String statusText = "Incoming call... " + remainingSeconds + "s";
        callStatus.setText(statusText);

        if (remainingSeconds > 20) {
            callStatus.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14;");
        } else if (remainingSeconds > 10) {
            callStatus.setStyle("-fx-text-fill: #ff9500; -fx-font-size: 14;");
        } else {
            callStatus.setStyle("-fx-text-fill: #ff3b30; -fx-font-size: 14; -fx-font-weight: bold;");
        }
    }

    @FXML
    public void handleAccept() {
        if (isRecordingStarted) {
            System.out.println("⚠️ Accept already processed – ignoring");
            return;
        }
        isRecordingStarted = true;
        callAccepted = true;

        if (acceptButton != null) acceptButton.setDisable(true);
        if (rejectButton != null) rejectButton.setDisable(true);

        System.out.println("✅ Call ACCEPTED by receiver");

        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        if (callStatus != null) {
            callStatus.setText("Call connected...");
            callStatus.setStyle("-fx-text-fill: #34c759; -fx-font-size: 14; -fx-font-weight: bold;");
        }

        if ("AUDIO_CALL".equals(callType) || "VIDEO_CALL".equals(callType)) {
            startCallRecording();
        }

        System.out.println("🎤 Call is now active - recording started");
    }

    @FXML
    private void handleReject() {
        if (callAccepted) {
            System.out.println("⚠️ Call already accepted – ignoring reject");
            return;
        }
        if (isRecordingStarted) {
            System.out.println("⚠️ Reject already processed – ignoring");
            return;
        }
        isRecordingStarted = true;

        if (acceptButton != null) acceptButton.setDisable(true);
        if (rejectButton != null) rejectButton.setDisable(true);

        System.out.println("❌ Call REJECTED by receiver");

        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        saveCallToDatabase("REJETÉ");

        if (stage != null) {
            stage.close();
        }
    }

    private void rejectCallAutomatically() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        if (callRecorder != null && callAccepted) {
            stopCallRecording();
        }

        saveCallToDatabase("NON_RÉPONDU");

        if (stage != null) {
            stage.close();
        }

        System.out.println("🚪 Call window closed - timeout");
    }

    private void startCallRecording() {
        try {
            File callFolder = new File("src/main/resources/Call");
            if (!callFolder.exists()) {
                callFolder.mkdirs();
            }

            String fileName = "call_" + currentUserId + "_" + System.currentTimeMillis() + ".wav";
            callAudioPath = callFolder.getAbsolutePath() + File.separator + fileName;

            callRecorder = new AudioRecorder();
            callRecorder.startRecording(callAudioPath);

            System.out.println("🎙️ Call recording started: " + callAudioPath);

        } catch (Exception e) {
            System.err.println("❌ Error starting recording: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopCallRecording() {
        try {
            if (callRecorder != null) {
                callRecorder.stopRecording();

                long callDuration = (System.currentTimeMillis() - callStartTime) / 1000;
                System.out.println("✅ Call recording stopped");
                System.out.println("⏱️ Call duration: " + callDuration + " seconds");
            }
        } catch (Exception e) {
            System.err.println("❌ Error stopping recording: " + e.getMessage());
        }
    }

    private void saveCallToDatabase(String callStatus) {
        try {
            if (conversationId == -1 || currentUserId == -1 || messageDAO == null) {
                System.err.println("❌ ERROR: Incomplete call data");
                return;
            }

            if (callRecorder != null && callAccepted) {
                stopCallRecording();
            }

            long callDuration = (System.currentTimeMillis() - callStartTime) / 1000;

            String callIcon = "VIDEO_CALL".equals(callType) ? "📹" : "📞";
            String callTypeLabel = "VIDEO_CALL".equals(callType) ? "Appel vidéo" : "Appel audio";
            String contentMessage = callIcon + " " + callTypeLabel + " - " + callStatus + " (" + callDuration + "s)";

            Message callMessage = new Message(
                    callType,
                    contentMessage,
                    "ENVOYE",
                    LocalDateTime.now(),
                    conversationId,
                    currentUserId
            );

            boolean success = messageDAO.addMessage(callMessage);

            if (success) {
                System.out.println("✅ Call saved to database: " + contentMessage);

                if (callAccepted && callAudioPath != null) {
                    System.out.println("💾 Audio recording saved: " + callAudioPath);
                }
            } else {
                System.err.println("❌ ERROR: Failed to save call to database");
            }

        } catch (Exception e) {
            System.err.println("❌ ERROR saving call: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void forceClose() {
        Platform.runLater(() -> {
            if (callTimeoutTimer != null) {
                callTimeoutTimer.stop();
            }
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            saveCallToDatabase("REJETÉ");

            if (stage != null) {
                stage.close();
            }

            System.out.println("🚪 Call cancelled by sender");
        });
    }
}