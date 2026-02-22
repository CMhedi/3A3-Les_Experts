package Services.interfaces;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService {

    private static final String API_KEY = "AIzaSyBwlAatML2QtspCnyV16MRYdYPdIcbmnCM";
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

    public static String translate(String text, String targetLanguageCode) throws Exception {
        // Convertir le code en nom de langue lisible
        String targetLanguageName;
        switch (targetLanguageCode) {
            case "fr": targetLanguageName = "French"; break;
            case "en": targetLanguageName = "English"; break;
            case "ar": targetLanguageName = "Arabic"; break;
            case "es": targetLanguageName = "Spanish"; break;
            case "de": targetLanguageName = "German"; break;
            case "zh": targetLanguageName = "Chinese"; break;
            case "ja": targetLanguageName = "Japanese"; break;
            case "ru": targetLanguageName = "Russian"; break;
            default: targetLanguageName = "English";
        }
        String prompt = String.format(
                "Translate the following French text to %s. Output only the translated text, no explanation, no extra words.\n\nText: %s",
                targetLanguageName, text
        );
        return askGemini(prompt);
    }

    public static void main(String[] args) throws Exception {
        String answer = askGemini("Hello, Gemini 2.5-Flash! Can you introduce yourself?");
        System.out.println(answer);
    }

}