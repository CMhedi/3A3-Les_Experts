package Services.interfaces;

import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public class AiRecommendationService {

    private static final String ENDPOINT = "https://api.openai.com/v1/responses";
    private static final String MODEL = "gpt-4o-mini";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final Gson gson = new Gson();

    public static class Filters {
        public String type_activite;   // SPORT/CAMPING/INTELECTUEL/CULTUREL
        public String categorie_act;   // FITNESS/RUNNING/FOOTBALL/BASKETBALL
        public String niveau_act;      // DEBUTANT/INTERMEDIAIRE/AVANCE
        public Double prix_max;        // ex 60
        public Integer limit;          // ex 5
        public String message;         // feedback
        public String source;          // "openai" ou "local"
    }

    /** Point d'entrée PRO : essaie OpenAI -> si échec -> fallback local */
    public Filters extractFiltersPro(String userMessage) {
        try {
            Filters f = extractFiltersOpenAI(userMessage);
            f.source = "openai";
            return normalizeDefaults(f);
        } catch (Exception e) {
            // fallback local si quota / offline / etc.
            Filters f = extractFiltersLocal(userMessage);
            f.source = "local";
            f.message = (f.message == null ? "" : f.message + "\n") +
                    "⚠️ Mode local activé (API indisponible / quota).";
            return normalizeDefaults(f);
        }
    }

    // ==============================
    // 1) OPENAI (Structured outputs)
    // ==============================
    private Filters extractFiltersOpenAI(String userMessage) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY manquante.");
        }

        JsonObject schema = buildSchemaStrict();

        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);

        JsonArray input = new JsonArray();

        JsonObject dev = new JsonObject();
        dev.addProperty("role", "developer");
        dev.addProperty("content",
                "Tu es un assistant EcoAdventure. " +
                        "Retourne uniquement un JSON conforme au schéma. " +
                        "Si un filtre n'est pas précisé: mets null. limit par défaut 5. " +
                        "Valeurs autorisées: type_activite(SPORT/CAMPING/INTELECTUEL/CULTUREL), " +
                        "categorie_act(FITNESS/RUNNING/FOOTBALL/BASKETBALL), " +
                        "niveau_act(DEBUTANT/INTERMEDIAIRE/AVANCE)."
        );
        input.add(dev);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        input.add(user);

        payload.add("input", input);

        JsonObject text = new JsonObject();
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.addProperty("name", "activity_filters");
        format.add("schema", schema);
        format.addProperty("strict", true);
        text.add("format", format);
        payload.add("text", text);

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

        if (root.has("output_text") && !root.get("output_text").isJsonNull()) {
            return gson.fromJson(root.get("output_text").getAsString().trim(), Filters.class);
        }

        String jsonText = extractTextFromOutput(root);
        if (jsonText != null && !jsonText.isBlank()) {
            return gson.fromJson(jsonText.trim(), Filters.class);
        }

        throw new RuntimeException("Réponse OpenAI invalide (JSON introuvable).");
    }

    private JsonObject buildSchemaStrict() {
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

        // strict=true => required = toutes les keys
        JsonArray required = new JsonArray();
        required.add("type_activite");
        required.add("categorie_act");
        required.add("niveau_act");
        required.add("prix_max");
        required.add("limit");
        required.add("message");
        schema.add("required", required);

        schema.addProperty("additionalProperties", false);
        return schema;
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
                    if (cObj.has("text")) return cObj.get("text").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ==============================
    // 2) FALLBACK LOCAL (intelligent)
    // ==============================
    private Filters extractFiltersLocal(String userMessage) {
        Filters f = new Filters();
        String s = (userMessage == null ? "" : userMessage).toUpperCase(Locale.ROOT);

        // Type
        if (s.contains("CAMP")) f.type_activite = "CAMPING";
        else if (s.contains("CULT")) f.type_activite = "CULTUREL";
        else if (s.contains("INTEL")) f.type_activite = "INTELECTUEL";
        else if (s.contains("SPORT")) f.type_activite = "SPORT";

        // Catégorie
        if (s.contains("FIT")) f.categorie_act = "FITNESS";
        else if (s.contains("RUN")) f.categorie_act = "RUNNING";
        else if (s.contains("FOOT")) f.categorie_act = "FOOTBALL";
        else if (s.contains("BASK")) f.categorie_act = "BASKETBALL";

        // Niveau
        if (s.contains("DEBUT")) f.niveau_act = "DEBUTANT";
        else if (s.contains("INTER")) f.niveau_act = "INTERMEDIAIRE";
        else if (s.contains("AVANC")) f.niveau_act = "AVANCE";

        // Prix max (ex: 60, 60DT, 60 DT)
        f.prix_max = extractNumber(s);

        // limit (top 3/top 5)
        if (s.contains("TOP 3") || s.contains("TOP3")) f.limit = 3;
        else if (s.contains("TOP 10") || s.contains("TOP10")) f.limit = 10;
        else f.limit = 5;

        f.message = "Je filtre les activités selon ta demande.";
        return f;
    }

    private Double extractNumber(String s) {
        // cherche le premier nombre dans la phrase
        StringBuilder num = new StringBuilder();
        boolean started = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                num.append(c);
                started = true;
            } else if (started) {
                break;
            }
        }
        if (num.length() == 0) return null;
        try {
            return Double.parseDouble(num.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Filters normalizeDefaults(Filters f) {
        if (f.limit == null) f.limit = 5;
        if (f.message == null) f.message = "Voici une recommandation.";
        return f;
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