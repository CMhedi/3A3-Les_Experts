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

public class CallingController {

    @FXML private Label callerName;
    @FXML private Label callStatus;
    @FXML private Circle callerImage;
    @FXML private Circle profileCircle;
    @FXML private Button acceptButton;   // Pour désactiver après acceptation
    @FXML private Button rejectButton;   // Pour désactiver après rejet

    private Stage stage;
    private MediaPlayer mediaPlayer;

    // Timeout
    private Timeline callTimeoutTimer;
    private int remainingSeconds = 30;
    private static final int CALL_TIMEOUT_SECONDS = 30;

    // Call data
    private int conversationId = -1;
    private int currentUserId = -1;
    private String callType = "AUDIO_CALL";
    private MessageDAO messageDAO;
    private long callStartTime;

    // Recording
    private AudioRecorder callRecorder;
    private String callAudioPath;
    private boolean callAccepted = false;


    // Protection contre les doubles clics
    private boolean isHandled = false;

    @FXML private Label contactName; // (inutilisé mais conservé)

    public void setContactData(String name, Stage stage) {
        Platform.runLater(() -> {
            if (callerName != null) {
                callerName.setText(name);
            } else {
                System.err.println("❌ Erreur: callerName est NULL.");
            }
            this.stage = stage;

            playCallingSound();
            callStartTime = System.currentTimeMillis();
            startCallTimeout();
        });
    }

    public void setCallData(int conversationId, int currentUserId, String callType, MessageDAO messageDAO) {
        this.conversationId = conversationId;
        this.currentUserId = currentUserId;
        this.callType = callType;
        this.messageDAO = messageDAO;
    }
    // إضافة دالة لتشغيل الملفات الصوتية


    private void playCallingSound() {
        try {
            String soundFile = "AUDIO_CALL".equals(callType) ? "/sounds/Call.mp3" : "/sounds/VideoCall.mp3";
            URL resource = getClass().getResource(soundFile);
            if (resource != null) {
                Media sound = new Media(resource.toString());
                mediaPlayer = new MediaPlayer(sound);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.play();
                System.out.println("🔔 Son d'appel " + ( "AUDIO_CALL".equals(callType) ? "audio" : "vidéo") + " lancé...");
            } else {
                System.err.println("❌ Fichier son non trouvé: " + soundFile);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur son d'appel: " + e.getMessage());
        }
    }

    @FXML
    private void handleAccept() {
        if (isHandled) return;
        isHandled = true;
        callAccepted = true;

        // Désactiver les boutons
        if (acceptButton != null) acceptButton.setDisable(true);
        if (rejectButton != null) rejectButton.setDisable(true);

        System.out.println("✅ Appel accepté par l'utilisateur");

        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        callStatus.setText("Appel en cours...");
        callStatus.setStyle("-fx-text-fill: #34c759; -fx-font-size: 14; -fx-font-weight: bold;");

        startCallRecording();

        // TODO: Envoyer un signal d'acceptation via STOMP (ex: /app/call.accept)
        System.out.println("📞 Appel maintenant actif - Enregistrement audio en cours...");
    }

    @FXML
    private void handleReject() {
        if (isHandled) return;
        isHandled = true;

        // Désactiver les boutons
        if (acceptButton != null) acceptButton.setDisable(true);
        if (rejectButton != null) rejectButton.setDisable(true);

        System.out.println("❌ Appel rejeté par l'utilisateur");
        rejectCall("REJETÉ");
    }

    private void startCallTimeout() {
        remainingSeconds = CALL_TIMEOUT_SECONDS;
        updateCallStatus();

        callTimeoutTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateCallStatus();

            if (remainingSeconds <= 0) {
                callTimeoutTimer.stop();
                rejectCallAutomatically();
            }
        }));

        callTimeoutTimer.setCycleCount(CALL_TIMEOUT_SECONDS);
        callTimeoutTimer.play();

        System.out.println("⏱️ Timer démarré : 30 secondes");
    }

    private void updateCallStatus() {
        // Pour l'appelant : "Appel en cours..." au lieu de "Appel entrant..."
        String statusText = "Appel en cours... " + remainingSeconds + "s";
        callStatus.setText(statusText);

        if (remainingSeconds > 20) {
            callStatus.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14;");
        } else if (remainingSeconds > 10) {
            callStatus.setStyle("-fx-text-fill: #ff9500; -fx-font-size: 14;");
        } else {
            callStatus.setStyle("-fx-text-fill: #ff3b30; -fx-font-size: 14; -fx-font-weight: bold;");
        }
    }

    private void rejectCallAutomatically() {
        if (isHandled) return;
        isHandled = true;
        System.out.println("⏱️ Timeout - appel non répondu");
        rejectCall("NON_RÉPONDU");
    }

    private void rejectCall(String status) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        if (callRecorder != null && callAccepted) {
            stopCallRecording();
        }

        saveCallToDatabase(status);

        // TODO: Envoyer un signal de rejet via STOMP (ex: /app/call.reject)

        if (stage != null) {
            stage.close();
        }
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

            System.out.println("🎙️ Enregistrement d'appel démarré : " + callAudioPath);
        } catch (Exception e) {
            System.err.println("❌ Erreur démarrage enregistrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopCallRecording() {
        try {
            if (callRecorder != null) {
                callRecorder.stopRecording();
                long callDuration = (System.currentTimeMillis() - callStartTime) / 1000;
                System.out.println("📞 Durée de l'appel : " + callDuration + " secondes");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur arrêt enregistrement : " + e.getMessage());
        }
    }

    private void saveCallToDatabase(String callStatus) {
        try {
            if (conversationId == -1 || currentUserId == -1 || messageDAO == null) {
                System.err.println("❌ Données d'appel incomplètes");
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
                System.out.println("✅ Appel enregistré BDD : " + contentMessage);
                if (callAccepted && callAudioPath != null) {
                    System.out.println("💾 Audio : " + callAudioPath);
                }
            } else {
                System.err.println("❌ Échec enregistrement BDD");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde BDD : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Optionnel : méthode appelée si l'autre utilisateur a rejeté/accepté
    public void remoteEndCall() {
        Platform.runLater(() -> {
            if (stage != null) {
                stage.close();
            }
        });
    }
}