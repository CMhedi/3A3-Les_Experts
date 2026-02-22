package Controller;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;

public class VoiceMessagePlayer {

    private MediaPlayer mediaPlayer;
    private String currentPlayingPath;

    /**
     * تشغيل ملف صوتي - يقبل المسار الكامل مباشرة
     */
    public void playVoiceMessage(String fullAudioPath) {
        try {
            File audioFile = new File(fullAudioPath);

            if (!audioFile.exists()) {
                System.err.println("❌ الملف غير موجود: " + fullAudioPath);
                System.err.println("📂 المسار المتوقع: " + audioFile.getAbsolutePath());
                return;
            }

            // إيقاف التشغيل السابق إذا كان مختلفاً
            if (mediaPlayer != null && !fullAudioPath.equals(currentPlayingPath)) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            currentPlayingPath = fullAudioPath;
            Media media = new Media(audioFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            // إضافة معالج لنهاية التشغيل
            mediaPlayer.setOnEndOfMedia(() -> {
                System.out.println("✅ انتهى التشغيل: " + audioFile.getName());
                currentPlayingPath = null;
            });

            mediaPlayer.play();
            System.out.println("▶️ تشغيل: " + audioFile.getName());

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
            mediaPlayer.dispose();
            mediaPlayer = null;
            currentPlayingPath = null;
            System.out.println("⏹️ توقف التشغيل");
        }
    }

    /**
     * التحقق من تشغيل أي ملف
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    /**
     * التحقق من هل الملف الحالي قيد التشغيل
     */
    public boolean isCurrentlyPlaying(String audioPath) {
        return currentPlayingPath != null && currentPlayingPath.equals(audioPath) && isPlaying();
    }
}