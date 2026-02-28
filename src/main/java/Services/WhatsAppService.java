package Services;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public class WhatsAppService {

    public static class SendResult {
        public final boolean ok;
        public final int statusCode;
        public final String responseBody;

        public SendResult(boolean ok, int statusCode, String responseBody) {
            this.ok = ok;
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String token="EAAPTu5bspesBQ8Vi0d6NhN90p6v3sZCMrhsXfNAwWj3aFtjvxDHPpZBJgDnm0xtZBP78zFjEE110eVk0cq3NQL5DG0gmhASjvNKgPd8vQgkqZAZCnE8KbmDro7t8luOj7Y4KOmE46KgDU4kPG6rCmrg9wQTLGeRFyaXkYu4QoKI9PNoxHarlJ3t1Q44TXZBEWE8Q0EYMb4Ul5hCVu4hDiyOtHOynuAN6zAfD50f5nQTjN8k5hxbJZChZAnn0hM1OxY6OYJZCMkAuD8Du3A7FXOWPb";
    private final String phoneNumberId="920875224452367";
    private final String apiVersion="v25.0";

    public WhatsAppService() {
        ;

    }

    public boolean isConfigured() {
        return token != null && !token.isBlank()
                && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    public SendResult sendTextMessage(String toE164, String body) throws IOException, InterruptedException {
        if (!isConfigured()) {
            return new SendResult(false, 0,
                    "WhatsApp not configured. Set WHATSAPP_TOKEN + WHATSAPP_PHONE_NUMBER_ID (+ optional WHATSAPP_API_VERSION).");
        }

        String to = normalizeE164(toE164);
        if (to == null) {
            return new SendResult(false, 0, "Invalid phone number. Use E.164 format like +216XXXXXXXX.");
        }

        String json = "{"
                + "\"messaging_product\":\"whatsapp\","
                + "\"to\":\"" + escapeJson(to) + "\","
                + "\"type\":\"text\","
                + "\"text\":{"
                + "\"preview_url\":false,"
                + "\"body\":\"" + escapeJson(body) + "\""
                + "}"
                + "}";

        String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        boolean ok = resp.statusCode() >= 200 && resp.statusCode() < 300;
        return new SendResult(ok, resp.statusCode(), resp.body());
    }

    // -------- helpers --------
    private static String normalizeE164(String s) {
        if (s == null) return null;
        String v = s.trim().replace(" ", "");
        if (!v.startsWith("+")) return null;
        if (v.length() < 8 || v.length() > 20) return null;
        for (int i = 1; i < v.length(); i++) {
            if (!Character.isDigit(v.charAt(i))) return null;
        }
        return v;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}