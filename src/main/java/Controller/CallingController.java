package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import Services.interfaces.MessageDAO;
import Entities.Message;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;

public class CallingController {

    @FXML private Label contactName;
    private Stage stage;
    private MediaPlayer mediaPlayer;

    // ✅ Variables pour gérer le timeout
    private Timeline callTimeoutTimer;
    private int remainingSeconds = 30;
    private static final int CALL_TIMEOUT_SECONDS = 30;

    // ✅ NOUVELLES VARIABLES : Pour enregistrer l'appel en BDD
    private int conversationId = -1;
    private int currentUserId = -1;
    private String callType = "AUDIO_CALL";  // "AUDIO_CALL" ou "VIDEO_CALL"
    private MessageDAO messageDAO;
    private long callStartTime;
    private boolean callAccepted = false;

    public void setContactData(String name, Stage stage) {
        this.contactName.setText(name);
        this.stage = stage;

        playCallingSound(); // Yabda el sout hna

        // ✅ Démarrer le timer de 30 secondes
        callStartTime = System.currentTimeMillis();
        startCallTimeout();
    }

    // ✅ NOUVELLE MÉTHODE : Injecter les données nécessaires
    public void setCallData(int conversationId, int currentUserId, String callType, MessageDAO messageDAO) {
        this.conversationId = conversationId;
        this.currentUserId = currentUserId;
        this.callType = callType;
        this.messageDAO = messageDAO;
    }

    private void playCallingSound() {
        try {
            // Path mte3 el fichier mte3ek fi resources
            URL resource = getClass().getResource("/sounds/Call.mp3");
            if (resource != null) {
                Media sound = new Media(resource.toString());
                mediaPlayer = new MediaPlayer(sound);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Boucle infinie
                mediaPlayer.play();
                System.out.println("🔔 Son d'appel lancé...");
            } else {
                System.err.println("❌ Fichier audio non trouvé : /sounds/Call.mp3");
            }
        } catch (Exception e) {
            System.err.println("❌ Sout makhdemch: " + e.getMessage());
        }
    }

    // ✅ NOUVELLE MÉTHODE : Démarrer le timer de 30 secondes
    private void startCallTimeout() {
        remainingSeconds = CALL_TIMEOUT_SECONDS;

        System.out.println("⏱️ Timer démarré : 30 secondes");

        // Créer une Timeline qui se déclenche chaque seconde
        callTimeoutTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;

            // Log tous les 10 secondes pour suivi
            if (remainingSeconds % 10 == 0 || remainingSeconds <= 5) {
                System.out.println("⏱️ Temps restant : " + remainingSeconds + "s");
            }

            // Si le temps est écoulé (0 secondes)
            if (remainingSeconds <= 0) {
                callTimeoutTimer.stop();
                rejectCallAutomatically();
            }
        }));

        // Faire boucler CALL_TIMEOUT_SECONDS fois (30 secondes)
        callTimeoutTimer.setCycleCount(CALL_TIMEOUT_SECONDS);
        callTimeoutTimer.play();
    }

    // ✅ NOUVELLE MÉTHODE : Accepter l'appel
    @FXML
    public void handleAcceptCall() {
        System.out.println("✅ Appel accepté par l'utilisateur");
        callAccepted = true;

        // Arrêter le timer
        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        // Enregistrer l'appel accepté
        saveCallToDatabase("ACCEPTÉ");

        // Arrêter la musique
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        // TODO: Démarrer l'appel réel (audio/vidéo streaming)
        System.out.println("📞 Appel maintenant actif...");
    }

    @FXML
    private void handleCancelCall() {
        System.out.println("❌ Appel rejeté manuellement par l'utilisateur");
        rejectCallAutomatically();
    }

    // ✅ NOUVELLE MÉTHODE : Rejet automatique ou manuel
    private void rejectCallAutomatically() {
        // Arrêter le timer
        if (callTimeoutTimer != null) {
            callTimeoutTimer.stop();
        }

        // Enregistrer l'appel rejeté
        String status = (remainingSeconds <= 0) ? "NON_RÉPONDU" : "REJETÉ";
        saveCallToDatabase(status);

        // Arrêter la musique
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            System.out.println("🔇 Son d'appel arrêté");
        }

        // Fermer la fenêtre
        if (stage != null) {
            stage.close();
            System.out.println("🚪 Fenêtre d'appel fermée");
        }

        // Log détaillé
        if (remainingSeconds <= 0) {
            System.out.println("⏰ Appel rejeté automatiquement (timeout après 30 secondes)");
        }
    }

    // ✅ NOUVELLE MÉTHODE : Enregistrer l'appel dans la base de données
    private void saveCallToDatabase(String callStatus) {
        try {
            // Vérifier que toutes les données sont disponibles
            if (conversationId == -1 || currentUserId == -1 || messageDAO == null) {
                System.err.println("❌ Erreur : Données d'appel incomplètes");
                return;
            }

            // Calculer la durée de l'appel
            long callDuration = (System.currentTimeMillis() - callStartTime) / 1000; // en secondes

            // Créer le message d'appel
            String callIcon = callType.equals("VIDEO_CALL") ? "📹" : "📞";
            String callTypeLabel = callType.equals("VIDEO_CALL") ? "Appel vidéo" : "Appel audio";
            String contentMessage = callIcon + " " + callTypeLabel + " - " + callStatus +
                    " (" + callDuration + "s)";

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
            } else {
                System.err.println("❌ Erreur : Impossible d'enregistrer l'appel");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'enregistrement de l'appel : " + e.getMessage());
            e.printStackTrace();
        }
    }
}