package Services.interfaces;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiService {

    private static final String API_KEY = "AIzaSyBV2ZsmiFA3MCyrvDwKqKMpoLkUQ6P8_IM";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

    public static String askGemini(String userQuery) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        // Create the JSON payload
        JSONObject payload = new JSONObject();
        payload.put("model", "gemini-2.5-flash");

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userQuery);
        messages.put(userMessage);

        payload.put("messages", messages);

        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        // Send the request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } else {
            return "Error Gemini API: " + response.statusCode() + "\n" + response.body();
        }
    }

    public static void main(String[] args) throws Exception {
        String answer = askGemini("Hello, Gemini 2.5-Flash! Can you introduce yourself?");
        System.out.println(answer);
    }

}