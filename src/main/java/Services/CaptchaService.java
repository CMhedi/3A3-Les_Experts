package Services;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.security.SecureRandom;

public class CaptchaService {

    private static final SecureRandom RAND = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public String generateCode(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RAND.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public WritableImage renderCaptcha(String code, int w, int h) {
        Canvas canvas = new Canvas(w, h);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Fond
        g.setFill(Color.web("#f3f4f6"));
        g.fillRect(0, 0, w, h);

        // Bruit (lignes)
        for (int i = 0; i < 10; i++) {
            g.setStroke(Color.color(RAND.nextDouble(), RAND.nextDouble(), RAND.nextDouble(), 0.35));
            g.setLineWidth(1 + RAND.nextDouble() * 2);
            g.strokeLine(RAND.nextDouble() * w, RAND.nextDouble() * h,
                    RAND.nextDouble() * w, RAND.nextDouble() * h);
        }

        // Texte (caractères avec petites rotations)
        g.setFont(Font.font("Segoe UI", 34));
        double x = 20;
        double y = 52;

        for (char c : code.toCharArray()) {
            g.save();
            double angle = -15 + RAND.nextDouble() * 30; // -15..+15
            g.translate(x, y);
            g.rotate(angle);
            g.setFill(Color.web("#111827"));
            g.fillText(String.valueOf(c), 0, 0);
            g.restore();
            x += 38;
        }

        // Bruit (points)
        for (int i = 0; i < 80; i++) {
            g.setFill(Color.color(0, 0, 0, 0.15));
            g.fillOval(RAND.nextDouble() * w, RAND.nextDouble() * h, 2, 2);
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
}