package Utiles;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.security.SecureRandom;

public final class CaptchaUtilFX {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();

    private CaptchaUtilFX() {}

    public static String randomText(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(CHARS.charAt(RND.nextInt(CHARS.length())));
        return sb.toString();
    }

    public static Image renderImage(String text, int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext g = canvas.getGraphicsContext2D();

        g.setFill(Color.rgb(245,247,250));
        g.fillRect(0,0,width,height);

        g.setLineWidth(2);
        for (int i = 0; i < 10; i++) {
            g.setStroke(Color.rgb(170 + RND.nextInt(60),170 + RND.nextInt(60),170 + RND.nextInt(60)));
            g.strokeLine(RND.nextDouble()*width, RND.nextDouble()*height,
                    RND.nextDouble()*width, RND.nextDouble()*height);
        }

        for (int i = 0; i < 250; i++) {
            g.setFill(Color.rgb(RND.nextInt(200), RND.nextInt(200), RND.nextInt(200), 0.65));
            g.fillRect(RND.nextDouble()*width, RND.nextDouble()*height, 2, 2);
        }

        g.setFont(Font.font("System", 30));
        int space = width / (text.length() + 1);
        double baseY = height * 0.65;

        for (int i = 0; i < text.length(); i++) {
            g.setFill(Color.rgb(35 + RND.nextInt(90),35 + RND.nextInt(90),35 + RND.nextInt(90)));
            double angle = (RND.nextDouble() - 0.5) * 35;

            g.save();
            g.translate(space*(i+1), baseY);
            g.rotate(angle);
            g.fillText(String.valueOf(text.charAt(i)), 0, 0);
            g.restore();
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        WritableImage out = new WritableImage(width, height);
        canvas.snapshot(params, out);
        return out;
    }
}