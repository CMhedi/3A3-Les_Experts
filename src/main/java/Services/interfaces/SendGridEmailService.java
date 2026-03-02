package Services.interfaces;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SendGridEmailService {

    private final HttpClient client = HttpClient.newHttpClient();

    private final String apiKey = System.getenv("SENDGRID_API_KEY");
    private final String senderEmail = System.getenv("SENDER_EMAIL");

    public void sendPlainText(String toEmail, String subject, String textBody) throws Exception {
        send(toEmail, subject, textBody, false);
    }

    // ✅ HTML email
    public void sendHtml(String toEmail, String subject, String htmlBody) throws Exception {
        send(toEmail, subject, htmlBody, true);
    }

    private void send(String toEmail, String subject, String body, boolean isHtml) throws Exception {

        if (apiKey == null || apiKey.isBlank())
            throw new IllegalStateException("SENDGRID_API_KEY missing.");
        if (senderEmail == null || senderEmail.isBlank())
            throw new IllegalStateException("SENDER_EMAIL missing.");
        if (toEmail == null || toEmail.isBlank())
            throw new IllegalArgumentException("Recipient email empty.");

        JsonObject root = new JsonObject();

        JsonObject from = new JsonObject();
        from.addProperty("email", senderEmail);
        root.add("from", from);

        root.addProperty("subject", subject);

        JsonArray personalizationArr = new JsonArray();
        JsonObject personalization = new JsonObject();
        JsonArray toArr = new JsonArray();
        JsonObject to = new JsonObject();
        to.addProperty("email", toEmail);
        toArr.add(to);
        personalization.add("to", toArr);
        personalizationArr.add(personalization);
        root.add("personalizations", personalizationArr);

        JsonArray contentArr = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("type", isHtml ? "text/html" : "text/plain");
        content.addProperty("value", body);
        contentArr.add(content);
        root.add("content", contentArr);

        String json = root.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 202) {
            throw new RuntimeException(
                    "SendGrid error: HTTP " +
                            response.statusCode() +
                            " - " + response.body()
            );
        }
    }
}