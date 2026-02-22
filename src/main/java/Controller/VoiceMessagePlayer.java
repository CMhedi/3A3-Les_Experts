package Controller;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;

public class VoiceMessagePlayer {

    private MediaPlayer mediaPlayer;

    /**
     * تشغيل ملف صوتي من مجلد uploads/audio
     */
    public void playVoiceMessage(String audioFileName) {
        try {
            // المسار الكامل
            String audioPath = "src/main/resources/uploads/audio/" + audioFileName;
            File audioFile = new File(audioPath);

            if (!audioFile.exists()) {
                System.err.println("❌ الملف غير موجود: " + audioPath);
                return;
            }

            // تشغيل الملف
            Media media = new Media(audioFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();

            System.out.println("▶️ تشغيل: " + audioFileName);

        } catch (Exception e) {
            System.err.println("❌ خطأ تشغيل الملف: " + e.getMessage());
        }
    }

    /**
     * إيقاف التشغيل
     */
    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            System.out.println("⏹️ توقف التشغيل");
        }
    }
}