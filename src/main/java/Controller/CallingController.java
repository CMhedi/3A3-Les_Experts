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
import Services.interfaces.MessageDAO;
import Entities.Message;
import Utiles.AudioRecorder;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;

public class CallingController {

    @FXML private Label callerName;
    @FXML private Label callStatus;
    @FXML private Circle callerImage;
    @FXML private Circle profileCircle;

    private Stage stage;
    private MediaPlayer mediaPlayer;

    // ✅ Variables pour gérer le timeout
    private Timeline callTimeoutTimer;
    private int remainingSeconds = 30;
    private static final int CALL_TIMEOUT_SECONDS = 30;

    // ✅ Variables pour enregistrer l'appel en BDD
    private int conversationId = -1;
    private int currentUserId = -1;
    private String callType = "AUDIO_CALL";
    private MessageDAO messageDAO;
    private long callStartTime;

    // ✅ Variables pour enregistrer l'audio d'appel
    private AudioRecorder callRecorder;
    private String callAudioPath;
    private boolean callAccepted = false;

    @FXML private Label contactName;
    public void setContactData(String name, Stage stage) {
        // Cette ligne ne plantera plus si l'injection @FXML a réussi
        if (callerName != null) {
            this.callerName.setText(name);
        } else {
            System.err.println("❌ Erreur: callerName est NULL. Vérifiez le fx:id dans le FXML.");
        }
        this.stage = stage;

        playCallingSound();
        callStartTime = System.currentTimeMillis();
        startCallTimeout();
    }

    // ✅ Injecter les données pour l'enregistrement en BDD
    public void setCallData(int conversationId, int currentUserId, String callType, MessageDAO messageDAO) {
        this.conversationId = conversationId;
        this.currentUserId = currentUserId;
        this.callType = callType;
        this.messageDAO = messageDAO;
    }

    private void playCallingSound() {
        try {
            URL resource = getClass().getResource("/sounds/Call.mp3");
            if (resource != null) {
                Media sound = new Media(resource.toString());
                mediaPlayer = new MediaPlayer(sound);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.play();
                System.out.println("🔔 Son d'appel lancé...");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur son d'appel: " + e.getMessage());
        }
    }

    // Dans CallingController.java, vérifie que c'est bien écrit comme ça :
    @FXML
    private void handleCancelCall() {
        System.out.println("❌ Appel rejeté manuellement");
        rejectCallAutomatically();
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
        String statusText = "Appel entrant... " + remainingSeconds + "s";
        callStatus.setText(statusText);

        // Couleur dynamique selon le temps restant
        if (remainingSeconds > 20) {
            callStatus.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14;");
        } else if (remainingSeconds > 10) {
            callStatus.setStyle("-fx-text-fill: #ff9500; -fx-font-size: 14;");
        } else {
            callStatus.setStyle("-fx-text-fill: #ff3b30; -fx-font-size: 14; -fx-font-weight: bold;");
        }
    }

    // ✅ BOUTON ACCEPTER : Lancer l'appel réel
    @FXML
    public void handleAccept() {
        System.out.println("✅ Appel accepté par l'utilisateur");
        callAccepted = true;

        // Arrêter le timer
        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        // Arrêter la musique
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        // Mettre à jour le statut
        callStatus.setText("Appel en cours...");
        callStatus.setStyle("-fx-text-fill: #34c759; -fx-font-size: 14; -fx-font-weight: bold;");

        // ✅ DÉMARRER L'ENREGISTREMENT AUDIO D'APPEL
        startCallRecording();

        // TODO: Ici, tu démarres la vraie communication audio/vidéo
        System.out.println("📞 Appel maintenant actif - Enregistrement audio en cours...");
    }

    // ✅ BOUTON REJETER
    @FXML
    private void handleReject() {
        System.out.println("❌ J'ai annulé l'appel");

        // 1. Arrêter le son
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        // 2. Arrêter le timer
        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        // 3. ICI : Tu dois envoyer un signal à l'autre utilisateur
        // Exemple : socketService.sendCancelSignal(targetUserId);

        // 4. Fermer ma fenêtre
        if (stage != null) {
            stage.close();
        }
    }

    private void rejectCallAutomatically() {
        // Arrêter le timer
        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        // Arrêter la musique
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            System.out.println("🔇 Son d'appel arrêté");
        }

        // Arrêter l'enregistrement si en cours
        if (callRecorder != null && callAccepted) {
            stopCallRecording();
        }

        // Enregistrer l'appel en BDD
        String status = (remainingSeconds <= 0) ? "NON_RÉPONDU" : "REJETÉ";
        saveCallToDatabase(status);

        // Fermer la fenêtre
        if (stage != null) {
            stage.close();
        }

        System.out.println("🚪 Fenêtre d'appel fermée");
    }

    // ✅ DÉMARRER L'ENREGISTREMENT AUDIO D'APPEL
    private void startCallRecording() {
        try {
            // Créer le dossier s'il n'existe pas
            File callFolder = new File("src/main/resources/Call");
            if (!callFolder.exists()) {
                callFolder.mkdirs();
            }

            // Générer un nom unique pour l'enregistrement d'appel
            String fileName = "call_" + currentUserId + "_" + System.currentTimeMillis() + ".wav";
            callAudioPath = callFolder.getAbsolutePath() + File.separator + fileName;

            // Démarrer l'enregistrement
            callRecorder = new AudioRecorder();
            callRecorder.startRecording(callAudioPath);

            System.out.println("🎙️ Enregistrement d'appel démarré : " + callAudioPath);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage d'enregistrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ ARRÊTER L'ENREGISTREMENT AUDIO D'APPEL
    private void stopCallRecording() {
        try {
            if (callRecorder != null) {
                callRecorder.stopRecording();
                System.out.println("✅ Enregistrement d'appel arrêté");

                // Calculer la durée totale
                long callDuration = (System.currentTimeMillis() - callStartTime) / 1000;
                System.out.println("📞 Durée de l'appel : " + callDuration + " secondes");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'arrêt d'enregistrement : " + e.getMessage());
        }
    }

    // ✅ ENREGISTRER L'APPEL EN BASE DE DONNÉES
    private void saveCallToDatabase(String callStatus) {
        try {
            // Vérifier que toutes les données sont disponibles
            if (conversationId == -1 || currentUserId == -1 || messageDAO == null) {
                System.err.println("❌ Erreur : Données d'appel incomplètes");
                return;
            }

            // Arrêter l'enregistrement si en cours
            if (callRecorder != null && callAccepted) {
                stopCallRecording();
            }

            // Calculer la durée de l'appel
            long callDuration = (System.currentTimeMillis() - callStartTime) / 1000;

            // Créer le message d'appel
            String callIcon = callType.equals("VIDEO_CALL") ? "📹" : "📞";
            String callTypeLabel = callType.equals("VIDEO_CALL") ? "Appel vidéo" : "Appel audio";
            String contentMessage = callIcon + " " + callTypeLabel + " - " + callStatus + " (" + callDuration + "s)";

            // Créer l'entité Message
            Message callMessage = new Message(
                    callType,                          // Type : "AUDIO_CALL" ou "VIDEO_CALL"
                    contentMessage,                    // Contenu : description de l'appel
                    "ENVOYE",                          // Statut
                    LocalDateTime.now(),               // Date
                    conversationId,                    // ID conversation
                    currentUserId                      // ID utilisateur
            );

            // Ajouter le message à la base de données
            boolean success = messageDAO.addMessage(callMessage);

            if (success) {
                System.out.println("✅ Appel enregistré en BDD : " + contentMessage);

                // Si l'appel a été accepté, sauvegarder aussi le chemin de l'audio
                if (callAccepted && callAudioPath != null) {
                    System.out.println("💾 Enregistrement audio : " + callAudioPath);
                }
            } else {
                System.err.println("❌ Erreur : Impossible d'enregistrer l'appel");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'enregistrement de l'appel : " + e.getMessage());
            e.printStackTrace();
        }
    }
}



