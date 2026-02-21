package Utiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;

public final class CaptchaUtil {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();

    private CaptchaUtil() {}

    public static String randomText(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(RND.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static Image renderImage(String text, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // background
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, width, height);

        // noise lines
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 10; i++) {
            g.setColor(new Color(180 + RND.nextInt(60), 180 + RND.nextInt(60), 180 + RND.nextInt(60)));
            int x1 = RND.nextInt(width);
            int y1 = RND.nextInt(height);
            int x2 = RND.nextInt(width);
            int y2 = RND.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        // dots noise
        for (int i = 0; i < 250; i++) {
            g.setColor(new Color(RND.nextInt(200), RND.nextInt(200), RND.nextInt(200)));
            int x = RND.nextInt(width);
            int y = RND.nextInt(height);
            g.fillRect(x, y, 2, 2);
        }

        // text
        g.setFont(new Font("Arial", Font.BOLD, 30));
        int charSpace = width / (text.length() + 1);
        int baseY = (int) (height * 0.65);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // random color
            g.setColor(new Color(40 + RND.nextInt(80), 40 + RND.nextInt(80), 40 + RND.nextInt(80)));

            // random rotation per char
            double angle = (RND.nextDouble() - 0.5) * 0.6; // ~ [-0.3..0.3] rad
            AffineTransform old = g.getTransform();
            AffineTransform at = new AffineTransform();
            int x = charSpace * (i + 1);
            at.translate(x, baseY);
            at.rotate(angle);
            g.setTransform(at);

            g.drawString(String.valueOf(c), 0, 0);
            g.setTransform(old);
        }

        g.dispose();
        return SwingFXUtils.toFXImage(img, null);
    }
}