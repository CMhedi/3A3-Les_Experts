package Services;

import com.google.gson.*;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class AiRecommendationService {

    private static final String ENDPOINT = "https://api.openai.com/v1/responses";
    private static final String MODEL = "gpt-4o-mini";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final Gson gson = new Gson();

    public static class Filters {
        public String type_activite;
        public String categorie_act;
        public String niveau_act;
        public Double prix_max;
        public Integer limit;
        public String message;
    }

    public Filters extractFilters(String userMessage) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY manquante (variable d'environnement).");
        }

        // ===== 1) JSON schema strict (required doit contenir toutes les keys) =====
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject props = new JsonObject();
        props.add("type_activite", propStringNullable());
        props.add("categorie_act", propStringNullable());
        props.add("niveau_act", propStringNullable());
        props.add("prix_max", propNumberNullable());
        props.add("limit", propIntNullable());
        props.add("message", propStringNullable());

        schema.add("properties", props);

        // ✅ strict=true => required contient TOUTES les propriétés
        JsonArray required = new JsonArray();
        required.add("type_activite");
        required.add("categorie_act");
        required.add("niveau_act");
        required.add("prix_max");
        required.add("limit");
        required.add("message");
        schema.add("required", required);

        schema.addProperty("additionalProperties", false);

        // ===== 2) Payload =====
        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);

        JsonArray input = new JsonArray();

        JsonObject dev = new JsonObject();
        dev.addProperty("role", "developer");
        dev.addProperty("content",
                "Tu es un assistant EcoAdventure. " +
                        "Tu dois extraire des filtres SQL pour la table activite : " +
                        "type_activite (SPORT/CAMPING/INTELECTUEL/CULTUREL), " +
                        "categorie_act (FITNESS/RUNNING/FOOTBALL/BASKETBALL), " +
                        "niveau_act (DEBUTANT/INTERMEDIAIRE/AVANCE), prix_max. " +
                        "Si l'utilisateur ne précise pas un filtre, mets null. " +
                        "limit par défaut 5. " +
                        "Ajoute un champ message en français (conseil utilisateur). " +
                        "Retourne uniquement du JSON conforme au schéma."
        );
        input.add(dev);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        input.add(user);

        payload.add("input", input);

        // ===== 3) Structured outputs correct =====
        JsonObject text = new JsonObject();
        JsonObject format = new JsonObject();

        format.addProperty("type", "json_schema");
        format.addProperty("name", "activity_filters");
        format.add("schema", schema);
        format.addProperty("strict", true);

        text.add("format", format);
        payload.add("text", text);

        // ===== 4) HTTP =====
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("OpenAI HTTP " + resp.statusCode() + " : " + resp.body());
        }

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();

        // Cas 1: output_text
        if (root.has("output_text") && !root.get("output_text").isJsonNull()) {
            String jsonText = root.get("output_text").getAsString().trim();
            return gson.fromJson(jsonText, Filters.class);
        }

        // Cas 2: output[] content[] text
        String jsonText = extractTextFromOutput(root);
        if (jsonText != null && !jsonText.isBlank()) {
            return gson.fromJson(jsonText.trim(), Filters.class);
        }

        throw new RuntimeException("Impossible de récupérer le JSON de filtres depuis la réponse OpenAI.");
    }

    private String extractTextFromOutput(JsonObject root) {
        try {
            if (!root.has("output") || !root.get("output").isJsonArray()) return null;

            JsonArray output = root.getAsJsonArray("output");
            for (JsonElement outEl : output) {
                if (!outEl.isJsonObject()) continue;
                JsonObject outObj = outEl.getAsJsonObject();

                if (!outObj.has("content") || !outObj.get("content").isJsonArray()) continue;

                JsonArray content = outObj.getAsJsonArray("content");
                for (JsonElement cEl : content) {
                    if (!cEl.isJsonObject()) continue;
                    JsonObject cObj = cEl.getAsJsonObject();

                    if (cObj.has("text")) {
                        return cObj.get("text").getAsString();
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private JsonObject propStringNullable() {
        JsonObject o = new JsonObject();
        JsonArray anyOf = new JsonArray();
        JsonObject s = new JsonObject(); s.addProperty("type", "string");
        JsonObject n = new JsonObject(); n.addProperty("type", "null");
        anyOf.add(s); anyOf.add(n);
        o.add("anyOf", anyOf);
        return o;
    }

    private JsonObject propNumberNullable() {
        JsonObject o = new JsonObject();
        JsonArray anyOf = new JsonArray();
        JsonObject s = new JsonObject(); s.addProperty("type", "number");
        JsonObject n = new JsonObject(); n.addProperty("type", "null");
        anyOf.add(s); anyOf.add(n);
        o.add("anyOf", anyOf);
        return o;
    }

    private JsonObject propIntNullable() {
        JsonObject o = new JsonObject();
        JsonArray anyOf = new JsonArray();
        JsonObject s = new JsonObject(); s.addProperty("type", "integer");
        JsonObject n = new JsonObject(); n.addProperty("type", "null");
        anyOf.add(s); anyOf.add(n);
        o.add("anyOf", anyOf);
        return o;
    }
}