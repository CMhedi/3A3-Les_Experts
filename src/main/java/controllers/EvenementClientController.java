package controllers;

import Entities.Evenement;
import Services.EvenementService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

public class EvenementClientController {

    @FXML private VBox cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    private final EvenementService service = new EvenementService();
    private List<Evenement> allEvenements;

    // --- API CONFIG ---
    private final String WEATHER_API_KEY = "VOTRE_OPENWEATHER_KEY";

    @FXML
    public void initialize() {
        loadAll();
    }

    private void loadAll() {
        try {
            allEvenements = service.getAll();
            displayCards(allEvenements);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayCards(List<Evenement> list) {
        cardsContainer.getChildren().clear();
        for (Evenement ev : list) {
            cardsContainer.getChildren().add(createCard(ev));
        }
        if (lblInfo != null) lblInfo.setText("(" + list.size() + ")");
    }

    private VBox createCard(Evenement ev) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Header : Titre + Weather Badge
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Weather Badge UI
        HBox weatherBadge = new HBox(5);
        weatherBadge.setAlignment(Pos.CENTER_LEFT);
        weatherBadge.setStyle("-fx-background-color: #f0f9ff; -fx-padding: 5 10; -fx-background-radius: 20; -fx-border-color: #bae6fd;");

        Label weatherIcon = new Label("☁️");
        Label tempLabel = new Label("..."); // Attente de l'API
        tempLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0369a1;");

        weatherBadge.getChildren().addAll(weatherIcon, tempLabel);
        header.getChildren().addAll(title, spacer, weatherBadge);

        // Lancement de l'appel API Météo asynchrone (mouch bch ma ytabatich el app)
        updateWeatherForCity(ev.getLieu(), weatherIcon, tempLabel);

        // Description & Lieu
        Label location = new Label("📍 " + ev.getLieu());
        location.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        Label desc = new Label(ev.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label("📅 " + ev.getDateEvent());

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button btnReserver = new Button("Réserver Now");
        btnReserver.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");

        footer.getChildren().addAll(dateLabel, spacer2, btnReserver);
        card.getChildren().addAll(header, location, desc, footer);

        return card;
    }

    private void updateWeatherForCity(String city, Label iconLabel, Label tempLabel) {
        if (city == null || city.isEmpty()) return;

        // Appel API asynchrone
        HttpClient.newHttpClient().sendAsync(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openweathermap.org/data/2.5/weather?q=" + city + "&units=metric&appid=" + WEATHER_API_KEY))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(HttpResponse::body).thenAccept(response -> {
            try {
                JSONObject json = new JSONObject(response);
                if (json.getInt("cod") == 200) {
                    double temp = json.getJSONObject("main").getDouble("temp");
                    String mainWeather = json.getJSONArray("weather").getJSONObject(0).getString("main");

                    // Update UI sur le thread principal
                    Platform.runLater(() -> {
                        tempLabel.setText(Math.round(temp) + "°C");
                        iconLabel.setText(getWeatherIcon(mainWeather));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> tempLabel.setText("N/A"));
            }
        });
    }

    private String getWeatherIcon(String main) {
        return switch (main.toLowerCase()) {
            case "clouds" -> "☁️";
            case "clear" -> "☀️";
            case "rain" -> "🌧️";
            case "snow" -> "❄️";
            case "thunderstorm" -> "⛈️";
            default -> "⛅";
        };
    }

    @FXML private void onSearch() {
        String q = searchField.getText().toLowerCase();
        displayCards(allEvenements.stream()
                .filter(ev -> ev.getTitre().toLowerCase().contains(q) || ev.getLieu().toLowerCase().contains(q))
                .collect(Collectors.toList()));
    }

    @FXML private void onRefresh() { searchField.clear(); loadAll(); }
    @FXML private void goToEvenements(ActionEvent event) { loadAll(); }
    @FXML private void goToReservations(ActionEvent event) { switchScene(event, "/views/client_reservations.fxml"); }
    @FXML private void goHome(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }
    @FXML private void logout(ActionEvent event) { switchScene(event, "/views/Login.fxml"); }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}