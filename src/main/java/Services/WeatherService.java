package Services;

import com.google.gson.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class WeatherService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public static class WeatherData {
        public String city;
        public double temp;
        public String description;
        public int humidity;
        public double windSpeed;
        public String main; // Rain/Clear/Clouds...
    }

    private String getApiKeyOrThrow() {
        String apiKey = System.getenv("OPENWEATHER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENWEATHER_API_KEY manquante (variable d'environnement).");
        }
        return apiKey.trim();
    }

    public WeatherData getCurrentWeather(String city) throws Exception {
        String apiKey = getApiKeyOrThrow();

        if (city == null || city.isBlank()) city = "Tunis";
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);

        String url =
                "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity +
                        "&appid=" + apiKey +
                        "&units=metric&lang=fr";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("OpenWeather HTTP " + resp.statusCode() + " : " + resp.body());
        }

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();

        WeatherData data = new WeatherData();
        data.city = root.get("name").getAsString();

        JsonObject mainObj = root.getAsJsonObject("main");
        data.temp = mainObj.get("temp").getAsDouble();
        data.humidity = mainObj.get("humidity").getAsInt();

        JsonObject windObj = root.getAsJsonObject("wind");
        data.windSpeed = windObj.get("speed").getAsDouble();

        JsonObject w0 = root.getAsJsonArray("weather").get(0).getAsJsonObject();
        data.main = w0.get("main").getAsString();
        data.description = w0.get("description").getAsString();

        return data;
    }
}