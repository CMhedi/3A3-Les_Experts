package Utiles;

import javax.sound.sampled.*;
import java.io.*;

public class AudioRecorder {

    private TargetDataLine line;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private File audioFile;
    private boolean isRecording = false;

    /**
     * Démarre l'enregistrement audio dans le fichier spécifié.
     * @param filePath chemin complet du fichier .wav à créer
     */
    public void startRecording(String filePath) throws LineUnavailableException, IOException {
        File targetFile = new File(filePath);
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // crée les dossiers parents si nécessaire
        }

        // Trouve un format audio supporté par le système
        AudioFormat format = findSupportedFormat();
        if (format == null) {
            throw new LineUnavailableException("Aucun format audio compatible trouvé.");
        }

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Ligne non supportée pour le format : " + format);
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        isRecording = true;
        audioFile = targetFile;

        // Thread d'écriture du flux audio
        Thread writerThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(line)) {
                AudioSystem.write(ais, fileType, audioFile);
            } catch (IOException e) {
                System.err.println("❌ Erreur écriture fichier audio : " + e.getMessage());
            }
        });
        writerThread.start();

        System.out.println("🎙️ Enregistrement démarré : " + audioFile.getAbsolutePath());
    }

    /**
     * Arrête l'enregistrement et ferme la ligne.
     */
    public void stopRecording() {
        if (line != null && isRecording) {
            line.stop();
            line.close();
            isRecording = false;
            System.out.println("🛑 Enregistrement arrêté. Fichier : " +
                    (audioFile != null ? audioFile.getAbsolutePath() : "null"));
        }
    }

    /**
     * Retourne le chemin absolu du fichier audio en cours.
     */
    public String getAudioFilePath() {
        return audioFile != null ? audioFile.getAbsolutePath() : null;
    }

    /**
     * Cherche un format audio supporté parmi les combinaisons courantes.
     * Priorité : 44.1 kHz → 16 kHz → 22.05 kHz → 48 kHz, 16 bits → 8 bits, little‑endian.
     */
    private AudioFormat findSupportedFormat() {
        float[] sampleRates = {44100.0f, 16000.0f, 22050.0f, 48000.0f};
        int[] sampleBits = {16, 8};
        boolean bigEndian = false; // little‑endian (standard sur Windows)

        for (float rate : sampleRates) {
            for (int bits : sampleBits) {
                AudioFormat format = new AudioFormat(rate, bits, 1, true, bigEndian); // signé, mono
                if (AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, format))) {
                    System.out.println("✅ Format audio utilisé : " + format);
                    return format;
                }
            }
        }
        return null;
    }
}