package GUI;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class NewsController {

    @FXML private FlowPane newsContainer;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ProgressIndicator loader;
    @FXML private Button btnRefresh;

    private final String API_KEY = "b6089080f5e741808763357531184f9b";

    @FXML
    public void initialize() {
        comboCategory.setItems(FXCollections.observableArrayList("sports", "health", "science", "environment"));
        comboCategory.setValue("health");

        Platform.runLater(this::loadNews);
    }

    @FXML
    void loadNews() {
        if (loader == null || btnRefresh == null || newsContainer == null) return;

        loader.setVisible(true);
        btnRefresh.setDisable(true);
        newsContainer.getChildren().clear();

        new Thread(() -> {
            try {
                String selectedCategory = comboCategory.getValue().toLowerCase();
                String urlString = "https://newsapi.org/v2/everything?q=" + selectedCategory +
                        "&language=fr&sortBy=publishedAt&apiKey=" + API_KEY;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (conn.getResponseCode() == 200) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) result.append(line);
                    rd.close();

                    JSONObject response = new JSONObject(result.toString());
                    JSONArray articles = response.getJSONArray("articles");

                    for (int i = 0; i < articles.length(); i++) {
                        JSONObject article = articles.getJSONObject(i);
                        Platform.runLater(() -> createCard(article));
                    }
                }

                Platform.runLater(() -> {
                    loader.setVisible(false);
                    btnRefresh.setDisable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loader.setVisible(false);
                    btnRefresh.setDisable(false);
                });
            }
        }).start();
    }

    private void createCard(JSONObject article) {
        // Card Container - Flat Design (No Shadow)
        VBox card = new VBox(10);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 15; " +
                "-fx-border-color: #EDF2F7; " + // Border khfif barcha
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 15;");

        // Image handling
        ImageView imageView = new ImageView();
        String imgUrl = article.optString("urlToImage", "");
        if (imgUrl.isEmpty() || imgUrl.equals("null")) {
            imgUrl = "https://via.placeholder.com/300x160?text=EcoAdventure";
        }

        try {
            Image image = new Image(imgUrl, 300, 160, false, true, true);
            imageView.setImage(image);
        } catch (Exception ignored) {}

        imageView.setFitWidth(300);
        imageView.setFitHeight(160);

        // Clip for Rounded Corners on Top
        Rectangle clip = new Rectangle(300, 160);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageView.setClip(clip);

        // Content Text
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(12, 15, 20, 15));

        Label title = new Label(article.getString("title"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2D3748;");
        title.setWrapText(true);
        title.setMinHeight(45);

        Label desc = new Label(article.optString("description", "Détails non disponibles."));
        desc.setStyle("-fx-font-size: 12; -fx-text-fill: #718096;");
        desc.setWrapText(true);
        desc.setMaxHeight(60);

        infoBox.getChildren().addAll(title, desc);
        card.getChildren().addAll(imageView, infoBox);

        // Interactive effects (Simple scale)
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-border-color: #2ECC71;"); // Border ywalli khdar
            card.setCursor(javafx.scene.Cursor.HAND);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-border-color: #2ECC71;", "-fx-border-color: #EDF2F7;"));
        });

        card.setOnMouseClicked(e -> {
            try { Desktop.getDesktop().browse(new URI(article.getString("url"))); } catch (Exception ignored) {}
        });

        newsContainer.getChildren().add(card);
    }
}