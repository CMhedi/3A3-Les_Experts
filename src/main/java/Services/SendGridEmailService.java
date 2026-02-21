package Services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SendGridEmailService {

    private final HttpClient client = HttpClient.newHttpClient();

    // ✅ use Environment Variables
    private final String apiKey = System.getenv("SENDGRID_API_KEY");
    private final String senderEmail = System.getenv("SENDER_EMAIL"); // verified sender

    public void sendPlainText(String toEmail, String subject, String textBody) throws Exception {
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalStateException("SENDGRID_API_KEY is missing in Environment Variables.");
        if (senderEmail == null || senderEmail.isBlank())
            throw new IllegalStateException("SENDER_EMAIL is missing in Environment Variables.");
        if (toEmail == null || toEmail.isBlank())
            throw new IllegalArgumentException("Recipient email is empty.");

        // Build JSON for SendGrid v3 Mail Send API
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
        content.addProperty("type", "text/plain");
        content.addProperty("value", textBody);
        contentArr.add(content);
        root.add("content", contentArr);

        String json = root.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // SendGrid success = 202 Accepted
        if (response.statusCode() != 202) {
            throw new RuntimeException("SendGrid error: HTTP " + response.statusCode() + " - " + response.body());
        }
    }
}