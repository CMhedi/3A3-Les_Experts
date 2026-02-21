package Controllers;

import Services.WeatherService;
import Services.WeatherService.WeatherData;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class WeatherController {

    @FXML private TextField tfCity;
    @FXML private Label lblStatus;

    @FXML private Label lblCity;
    @FXML private Label lblTemp;
    @FXML private Label lblDesc;
    @FXML private Label lblHumidity;
    @FXML private Label lblWind;

    private final WeatherService weatherService = new WeatherService();

    @FXML
    public void initialize() {
        tfCity.setText("Tunis");
        refresh(null);
    }

    @FXML
    public void refresh(ActionEvent event) {
        String city = tfCity.getText();
        if (city == null || city.isBlank()) city = "Tunis";

        lblStatus.setText("Chargement météo...");
        tfCity.setDisable(true);

        String finalCity = city;

        new Thread(() -> {
            try {
                WeatherData data = weatherService.getCurrentWeather(finalCity);

                Platform.runLater(() -> {
                    lblCity.setText("Ville : " + data.city);
                    lblTemp.setText(String.format("Température : %.1f °C", data.temp));
                    lblDesc.setText("Description : " + data.description + " (" + data.main + ")");
                    lblHumidity.setText("Humidité : " + data.humidity + " %");
                    lblWind.setText(String.format("Vent : %.1f m/s", data.windSpeed));

                    lblStatus.setText("✅ Météo mise à jour.");
                    tfCity.setDisable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Erreur météo : " + e.getMessage());
                    tfCity.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    public void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/AjouterActivite.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}