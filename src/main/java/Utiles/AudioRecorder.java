package Utiles;

import javax.sound.sampled.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioRecorder {

    private TargetDataLine line;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private File audioFile;

    // Démarrer l'enregistrement
    public void startRecording(String directoryPath) throws LineUnavailableException {
        // Crée un nom de fichier unique basé sur timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        String fileName = "message_" + timestamp + ".wav";

        // Crée le dossier si nécessaire
        File dir = new File(directoryPath);
        if (!dir.exists()) dir.mkdirs();

        audioFile = new File(dir, fileName);

        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("⚠️ Line not supported");
            return;
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        // Thread pour écrire le flux audio
        Thread thread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(line)) {
                AudioSystem.write(ais, fileType, audioFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        thread.start();
        System.out.println("🎙️ Recording started: " + audioFile.getAbsolutePath());
    }

    // Stop recording
    public void stopRecording() {
        if (line != null) {
            line.stop();
            line.close();
            System.out.println("🛑 Recording stopped. Saved to: " + (audioFile != null ? audioFile.getAbsolutePath() : "null"));
        }
    }

    // Récupérer le chemin du fichier
    public String getAudioFilePath() {
        return audioFile != null ? audioFile.getAbsolutePath() : null;
    }
}
