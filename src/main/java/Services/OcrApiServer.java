package Services;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.sourceforge.tess4j.Tesseract;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrApiServer {

    private static final Gson gson = new Gson();

    private static final Pattern EXPIRY = Pattern.compile("\\b(0[1-9]|1[0-2])\\s*/\\s*\\d{2}\\b");
    private static final Pattern PAN_ANY = Pattern.compile("(?<!\\d)(\\d[ -]?){13,19}(?!\\d)");

    public static void main(String[] args) throws Exception {
        int port = 8088;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/ocr/card", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                writeJson(ex, 405, Map.of("error", "Method not allowed"));
                return;
            }

            try {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<?, ?> req = gson.fromJson(body, Map.class);

                String frontB64 = req == null ? null : (String) req.get("front_image_base64");
                String tessdata = req == null ? null : (String) req.get("tessdata_path");

                if (frontB64 == null || frontB64.isBlank()) {
                    writeJson(ex, 400, Map.of("error", "Missing front_image_base64"));
                    return;
                }

                BufferedImage front = decodeBase64Image(frontB64);
                String frontRaw = doOcr(front, tessdata);

                String pan = extractPan(frontRaw); // full digits (LOCAL only)
                String expiry = extractExpiry(frontRaw);
                String name = guessName(frontRaw);
                String brand = detectBrandFromPan(pan);

                Map<String, Object> out = new LinkedHashMap<>();
                out.put("timestamp", LocalDateTime.now().toString());
                out.put("brand", brand);
                out.put("name", name);
                out.put("pan", pan);
                out.put("expiry", expiry);
                out.put("last4", pan != null && pan.length() >= 4 ? pan.substring(pan.length() - 4) : "");

                writeJson(ex, 200, out);

            } catch (Exception e) {
                writeJson(ex, 500, Map.of("error", e.getMessage()));
            }
        });

        server.start();
        System.out.println("OCR API running: http://localhost:" + port + "/api/ocr/card");
    }

    private static String doOcr(BufferedImage img, String tessdataPath) throws Exception {
        Tesseract t = new Tesseract();

        String defaultPath = "C:\\Program Files\\Tesseract-OCR\\tessdata";
        t.setDatapath((tessdataPath != null && !tessdataPath.isBlank()) ? tessdataPath : defaultPath);

        t.setLanguage("eng");

        t.setTessVariable("user_defined_dpi", "300");
        t.setPageSegMode(6); // assume a block of text

        return t.doOCR(img);
    }

    private static BufferedImage decodeBase64Image(String b64) throws IOException {
        String clean = b64.contains(",") ? b64.substring(b64.indexOf(",") + 1) : b64;
        byte[] bytes = Base64.getDecoder().decode(clean);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new IOException("Invalid image");
            return img;
        }
    }

    private static String extractExpiry(String text) {
        Matcher m = EXPIRY.matcher(text.replace("\n", " "));
        if (!m.find()) return "";
        return m.group().replace(" ", "");
    }

    private static String guessName(String text) {
        String[] lines = text.split("\\R");
        String best = "";
        for (String line : lines) {
            String s = line.trim();
            if (s.length() < 8) continue;
            if (!s.matches("[A-Za-z ]+")) continue;

            int upper = 0;
            for (char c : s.toCharArray()) if (Character.isUpperCase(c)) upper++;
            if (upper >= 3 && s.length() > best.length()) best = s;
        }
        return best.trim();
    }

    private static String extractPan(String raw) {
        Matcher m = PAN_ANY.matcher(raw);
        String best = "";
        while (m.find()) {
            String candidate = m.group().replaceAll("\\D", "");
            if (candidate.length() >= 13 && candidate.length() <= 19) {
                if (luhnValid(candidate) && candidate.length() > best.length()) best = candidate;
            }
        }
        return best;
    }

    private static String detectBrandFromPan(String pan) {
        if (pan == null) return "CARD";
        if (pan.startsWith("4")) return "VISA";
        if (pan.matches("^5[1-5].*")) return "MASTERCARD";
        if (pan.matches("^2(2[2-9]\\d|[3-6]\\d\\d|7[01]\\d|720).*")) return "MASTERCARD";
        if (pan.startsWith("34") || pan.startsWith("37")) return "AMEX";
        if (pan.startsWith("6")) return "DISCOVER";
        return "CARD";
    }

    private static boolean luhnValid(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            char c = digits.charAt(i);
            if (c < '0' || c > '9') return false;
            int n = c - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    private static void writeJson(HttpExchange ex, int code, Object payload) throws IOException {
        byte[] bytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

}