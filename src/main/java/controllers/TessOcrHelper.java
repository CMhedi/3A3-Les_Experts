package controllers;

import net.sourceforge.tess4j.Tesseract;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;

/**
 * Utilitaire OCR — résout le chemin tessdata automatiquement.
 *
 * Ordre de recherche :
 *   1. Variable d'environnement TESSDATA_PREFIX
 *   2. src/main/resources/tessdata  (dev IntelliJ)
 *   3. target/classes/tessdata      (après build Maven)
 *   4. ~/.tessdata                  (répertoire utilisateur)
 *
 * Si eng.traineddata est introuvable partout, il est téléchargé
 * depuis GitHub dans ~/.tessdata/ (~40 MB, une seule fois).
 */
public class TessOcrHelper {

    /** URL officielle du fichier de langue anglais Tesseract 4/5 */
    private static final String ENG_TRAINEDDATA_URL =
            "https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata";

    /** Chemin résolu (calculé une seule fois) */
    private static String resolvedDataPath = null;

    // ────────────────────────────────────────────────────────
    //  Point d'entrée public
    // ────────────────────────────────────────────────────────

    /**
     * Crée et configure une instance Tesseract prête à l'emploi.
     * Lance une RuntimeException avec un message clair si tout échoue.
     */
    public static Tesseract buildTesseract() {
        String dataPath = getOrResolveDataPath();

        Tesseract tess = new Tesseract();
        tess.setDatapath(dataPath);
        tess.setLanguage("eng");
        tess.setPageSegMode(6);   // mode "bloc de texte uniforme" — idéal pour une carte
        tess.setOcrEngineMode(1); // LSTM (plus précis que legacy)
        return tess;
    }

    // ────────────────────────────────────────────────────────
    //  Résolution du chemin tessdata
    // ────────────────────────────────────────────────────────

    public static String getOrResolveDataPath() {
        if (resolvedDataPath != null) return resolvedDataPath;

        // 1. Variable d'environnement
        String envPath = System.getenv("TESSDATA_PREFIX");
        if (hasEngData(envPath)) {
            resolvedDataPath = envPath;
            System.out.println("✅ tessdata trouvé via TESSDATA_PREFIX : " + envPath);
            return resolvedDataPath;
        }

        // 2. Chemin relatif dev (IntelliJ, répertoire courant = racine projet)
        String devPath = "src/main/resources/tessdata";
        if (hasEngData(devPath)) {
            resolvedDataPath = devPath;
            System.out.println("✅ tessdata trouvé (dev) : " + new File(devPath).getAbsolutePath());
            return resolvedDataPath;
        }

        // 3. Après build Maven : target/classes/tessdata
        String targetPath = "target/classes/tessdata";
        if (hasEngData(targetPath)) {
            resolvedDataPath = targetPath;
            System.out.println("✅ tessdata trouvé (build) : " + new File(targetPath).getAbsolutePath());
            return resolvedDataPath;
        }

        // 4. Répertoire utilisateur : ~/.tessdata
        String homePath = System.getProperty("user.home") + File.separator + ".tessdata";
        if (hasEngData(homePath)) {
            resolvedDataPath = homePath;
            System.out.println("✅ tessdata trouvé (home) : " + homePath);
            return resolvedDataPath;
        }

        // 5. Aucun trouvé → télécharger dans ~/.tessdata
        System.out.println("⚠ eng.traineddata introuvable — téléchargement automatique…");
        downloadEngTraineddata(homePath);

        if (hasEngData(homePath)) {
            resolvedDataPath = homePath;
            System.out.println("✅ tessdata téléchargé : " + homePath);
            return resolvedDataPath;
        }

        // Tout a échoué
        throw new RuntimeException(
                "Impossible de trouver ou télécharger eng.traineddata.\n\n"
                        + "Solutions :\n"
                        + "  A) Téléchargez https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata\n"
                        + "     et placez-le dans : src/main/resources/tessdata/\n"
                        + "  B) Définissez la variable d'environnement TESSDATA_PREFIX "
                        + "pointant vers le dossier contenant eng.traineddata");
    }

    // ────────────────────────────────────────────────────────
    //  Utilitaires
    // ────────────────────────────────────────────────────────

    /** Vérifie que le dossier contient eng.traineddata. */
    private static boolean hasEngData(String dirPath) {
        if (dirPath == null || dirPath.isBlank()) return false;
        File f = new File(dirPath, "eng.traineddata");
        return f.exists() && f.isFile() && f.length() > 0;
    }

    /**
     * Télécharge eng.traineddata depuis GitHub dans le répertoire cible.
     * Affiche la progression en console (tous les 5 Mo).
     */
    private static void downloadEngTraineddata(String targetDir) {
        File dir  = new File(targetDir);
        File dest = new File(dir, "eng.traineddata");

        try {
            // Créer le dossier si besoin
            Files.createDirectories(dir.toPath());

            System.out.println("⬇ Téléchargement de eng.traineddata (~40 Mo) depuis GitHub…");
            System.out.println("   → " + dest.getAbsolutePath());

            HttpURLConnection conn = (HttpURLConnection) new URL(ENG_TRAINEDDATA_URL).openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(120_000);
            conn.setRequestProperty("User-Agent", "EcoAdventure-JavaFX/1.0");
            conn.connect();

            int status = conn.getResponseCode();
            if (status != 200) {
                System.out.println("❌ Téléchargement échoué, HTTP " + status);
                return;
            }

            long total     = conn.getContentLengthLong();
            long downloaded = 0;
            long nextLog   = 5 * 1024 * 1024; // log tous les 5 Mo

            try (InputStream  in  = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {

                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    downloaded += n;
                    if (downloaded >= nextLog) {
                        System.out.printf("   … %.1f / %.1f Mo%n",
                                downloaded / 1_048_576.0,
                                total > 0 ? total / 1_048_576.0 : Double.NaN);
                        nextLog += 5 * 1024 * 1024;
                    }
                }
            }
            System.out.println("✅ Téléchargement terminé : " + dest.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("❌ Erreur téléchargement tessdata : " + e.getMessage());
            // Supprimer le fichier partiel s'il existe
            if (dest.exists()) dest.delete();
        }
    }
}