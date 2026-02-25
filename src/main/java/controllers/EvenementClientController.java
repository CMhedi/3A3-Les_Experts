package controllers;

import Entities.Evenement;
import Services.EvenementService;
import Services.ReservationEvenementService; // Thabet elli el service hedha mawjoud 3andek
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private final ReservationEvenementService resService = new ReservationEvenementService();
    private List<Evenement> allEvenements;

    // --- API CONFIG ---
    private final String WEATHER_API_KEY = "ba052da226f1813069d54ebd059266ce";

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
        cardsContainer.setSpacing(15);
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

        // Houni rja3na nsta3mlou ImageView bech el météo todh-her mrigla
        ImageView weatherIcon = new ImageView();
        weatherIcon.setFitWidth(30);
        weatherIcon.setFitHeight(30);

        Label tempLabel = new Label("...");
        tempLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0369a1;");

        weatherBadge.getChildren().addAll(weatherIcon, tempLabel);
        header.getChildren().addAll(title, spacer, weatherBadge);

        // Appel API Météo (Fixé)
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

        // Popup de réservation
        btnReserver.setOnAction(e -> openReservationForm(ev));

        footer.getChildren().addAll(dateLabel, spacer2, btnReserver);
        card.getChildren().addAll(header, location, desc, footer);

        return card;
    }

    // --- FORMULAIRE RÉSERVATION (Yzid fèl Base de données) ---
    private void openReservationForm(Evenement ev) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Confirmation");

        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #ffffff;");

        Label txt = new Label("Réserver pour: " + ev.getTitre());
        txt.setStyle("-fx-font-weight: bold;");

        TextField input = new TextField("1");
        input.setPromptText("Nombre de places");

        Button btn = new Button("Confirmer");
        btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");

        btn.setOnAction(e -> {
            try {
                // Houni tzid el logic mta3 el Service mte3ek
                // resService.ajouter(new Reservation(ev.getId(), nb_places));

                popup.close();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Réservation ajoutée !");
                a.show();

                // Refresh lel page reservation client
                goToReservations(new ActionEvent(cardsContainer, null));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        box.getChildren().addAll(txt, input, btn);
        popup.setScene(new Scene(box, 300, 200));
        popup.show();
    }

    private void updateWeatherForCity(String city, ImageView iconView, Label tempLabel) {
        if (city == null || city.isEmpty()) return;

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
                    String iconCode = json.getJSONArray("weather").getJSONObject(0).getString("icon");
                    // URL mta3 el icon mel OpenWeather
                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                    Platform.runLater(() -> {
                        tempLabel.setText(Math.round(temp) + "°C");
                        iconView.setImage(new Image(iconUrl));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> tempLabel.setText("N/A"));
            }
        });
    }

    @FXML private void onSearch() {
        String q = searchField.getText().toLowerCase();
        displayCards(allEvenements.stream()
                .filter(ev -> ev.getTitre().toLowerCase().contains(q) || ev.getLieu().toLowerCase().contains(q))
                .collect(Collectors.toList()));
    }

    @FXML private void onRefresh() { searchField.clear(); loadAll(); }
    @FXML private void goToEvenements(ActionEvent event) { loadAll(); }
    @FXML private void goToReservations(ActionEvent event) { switchScene(event, "/views/reservation_list.fxml"); }
    @FXML private void goHome(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }
    @FXML private void logout(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }

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