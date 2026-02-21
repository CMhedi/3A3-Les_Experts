package Services;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class WeatherService {

    private static final String API_KEY = "104fcd6db2678659a41ca3611efb7e2f";
    private static final String CITY = "Tunis";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String getWeatherMessage() {

        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?q="
                    + CITY + "&appid=" + API_KEY + "&units=metric&lang=fr";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

            double temp = root.getAsJsonObject("main").get("temp").getAsDouble();
            String condition = root.getAsJsonArray("weather")
                    .get(0).getAsJsonObject()
                    .get("main").getAsString();

            if (condition.contains("Rain")) {
                return "⚠️ Pluie prévue aujourd'hui.";
            }

            if (temp > 35) {
                return "🔥 Forte chaleur (" + temp + "°C).";
            }

            return "✅ Météo favorable (" + temp + "°C).";

        } catch (Exception e) {
            return "⚠️ Impossible de vérifier la météo.";
        }
    }
}