package Controller;

import javafx.fxml.FXML;
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
import Utiles.AudioRecorder;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;

/**
 * ✅ COMPLETE RECEIVER CONTROLLER (RÉCEPTEUR)
 * Handles incoming call reception, accept/reject, recording, and database persistence
 */
public class CallController {

    // ✅ FXML Components - MUST match CallWindow.fxml fx:ids
    @FXML private Label callerName;
    @FXML private Label callStatus;
    @FXML private Circle profileCircle;

    // Window reference
    private Stage stage;
    private MediaPlayer mediaPlayer;

    // ✅ Call Timeout Management (30 seconds)
    private Timeline callTimeoutTimer;
    private int remainingSeconds = 30;
    private static final int CALL_TIMEOUT_SECONDS = 30;

    // ✅ Call Data for Database Recording
    private int conversationId = -1;
    private int currentUserId = -1;
    private String callType = "AUDIO_CALL";  // or "VIDEO_CALL"
    private MessageDAO messageDAO;
    private long callStartTime;

    // ✅ Call Audio Recording
    private AudioRecorder callRecorder;
    private String callAudioPath;
    private boolean callAccepted = false;

    // ✅ Caller Information
    private String callerNameStr = "";


    /**
     * ✅ SET CALLER DATA (Called from MessengerController)
     * Display caller information on the incoming call window
     */
    public void setCallerData(String name, Stage stage) {
        Platform.runLater(() -> {
            this.callerNameStr = name;
            this.stage = stage;

            // Set UI elements
            if (this.callerName != null) {
                this.callerName.setText(name);
                System.out.println("📞 Incoming call from: " + name);
            } else {
                System.err.println("❌ ERROR: callerName FXML component is NULL");
            }

            // Start call sound and timer
            playIncomingCallSound();
            callStartTime = System.currentTimeMillis();
            startCallTimeout();
        });
    }

    /**
     * ✅ SET CALL DATA (Called from MessengerController)
     * Store metadata needed for database recording
     */
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

    /**
     * ✅ PLAY INCOMING CALL SOUND
     * Play ringtone during incoming call
     */
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

    /**
     * ✅ START CALL TIMEOUT (30 seconds)
     * Auto-reject if no response after 30 seconds
     */
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

    /**
     * ✅ UPDATE CALL STATUS DISPLAY
     * Show countdown and change color based on remaining time
     */
    private void updateCallStatus() {
        String statusText = "Incoming call... " + remainingSeconds + "s";

        if (callStatus != null) {
            callStatus.setText(statusText);

            // Dynamic color based on remaining time
            if (remainingSeconds > 20) {
                callStatus.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14;");
            } else if (remainingSeconds > 10) {
                callStatus.setStyle("-fx-text-fill: #ff9500; -fx-font-size: 14;");  // Orange
            } else {
                callStatus.setStyle("-fx-text-fill: #ff3b30; -fx-font-size: 14; -fx-font-weight: bold;");  // Red
            }
        }
    }

    /**
     * ✅ HANDLE ACCEPT BUTTON
     * Receiver accepts the incoming call
     */
    @FXML
    public void handleAccept() {
        System.out.println("✅ Call ACCEPTED by receiver");
        callAccepted = true;

        // Stop timer
        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        // Stop ringtone
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        // Update UI
        if (callStatus != null) {
            callStatus.setText("Call connected...");
            callStatus.setStyle("-fx-text-fill: #34c759; -fx-font-size: 14; -fx-font-weight: bold;");
        }

        // Start recording if audio call
        if ("AUDIO_CALL".equals(callType) || "VIDEO_CALL".equals(callType)) {
            startCallRecording();
        }

        // TODO: Establish actual audio/video connection here
        System.out.println("🎤 Call is now active - recording started");
    }

    /**
     * ✅ HANDLE REJECT BUTTON
     * Receiver manually rejects the call
     */
    @FXML
    private void handleReject() {
        System.out.println("❌ Call REJECTED by receiver");

        // Stop timer
        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        // Stop ringtone
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        // Save call status to database
        saveCallToDatabase("REJETÉ");

        // Close window
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * ✅ AUTO-REJECT AFTER TIMEOUT
     * Called when 30-second timer expires
     */
    private void rejectCallAutomatically() {
        // Stop ringtone
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        // Stop recording if accepted
        if (callRecorder != null && callAccepted) {
            stopCallRecording();
        }

        // Save call as NON_RÉPONDU
        saveCallToDatabase("NON_RÉPONDU");

        // Close window
        if (stage != null) {
            stage.close();
        }

        System.out.println("🚪 Call window closed - timeout");
    }

    /**
     * ✅ START CALL RECORDING
     * Record audio during accepted call
     */
    private void startCallRecording() {
        try {
            // Create call folder
            File callFolder = new File("src/main/resources/Call");
            if (!callFolder.exists()) {
                callFolder.mkdirs();
            }

            // Generate unique filename
            String fileName = "call_" + currentUserId + "_" + System.currentTimeMillis() + ".wav";
            callAudioPath = callFolder.getAbsolutePath() + File.separator + fileName;

            // Start recording
            callRecorder = new AudioRecorder();
            callRecorder.startRecording(callAudioPath);

            System.out.println("🎙️ Call recording started: " + callAudioPath);

        } catch (Exception e) {
            System.err.println("❌ Error starting recording: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ STOP CALL RECORDING
     * Stop recording and calculate duration
     */
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

    /**
     * ✅ SAVE CALL TO DATABASE
     * Record the call in the Message table
     */
    private void saveCallToDatabase(String callStatus) {
        try {
            // Validate data
            if (conversationId == -1 || currentUserId == -1 || messageDAO == null) {
                System.err.println("❌ ERROR: Incomplete call data");
                return;
            }

            // Stop recording if active
            if (callRecorder != null && callAccepted) {
                stopCallRecording();
            }

            // Calculate duration
            long callDuration = (System.currentTimeMillis() - callStartTime) / 1000;

            // Format message content
            String callIcon = "VIDEO_CALL".equals(callType) ? "📹" : "📞";
            String callTypeLabel = "VIDEO_CALL".equals(callType) ? "Appel vidéo" : "Appel audio";
            String contentMessage = callIcon + " " + callTypeLabel + " - " + callStatus + " (" + callDuration + "s)";

            // Create Message entity
            Message callMessage = new Message(
                    callType,                   // Type: "AUDIO_CALL" or "VIDEO_CALL"
                    contentMessage,             // Content: call description
                    "ENVOYE",                   // Status
                    LocalDateTime.now(),        // Timestamp
                    conversationId,             // Conversation ID
                    currentUserId               // User ID (receiver)
            );

            // Save to database
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

    /**
     * ✅ FORCE CLOSE (Called by sender when they cancel)
     * Closes window immediately without user interaction
     */
    public void forceClose() {
        Platform.runLater(() -> {
            // Stop timer and sound
            if (callTimeoutTimer != null) {
                callTimeoutTimer.stop();
            }
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            // Save call as REJETÉ (sender cancelled)
            saveCallToDatabase("REJETÉ");

            // Close
            if (stage != null) {
                stage.close();
            }

            System.out.println("🚪 Call cancelled by sender");
        });
    }
}