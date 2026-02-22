package GUI;


import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NewsController {

    @FXML private ListView<JSONObject> newsListView;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ProgressIndicator loader;
    @FXML private Button btnRefresh;

    private final String API_KEY = "b6089080f5e741808763357531184f9b"; // Khoudha men newsapi.org (Gratuit)

    @FXML
    public void initialize() {
        // 1. Setup ComboBox
        comboCategory.setItems(FXCollections.observableArrayList("sports", "health", "science"));
        comboCategory.setValue("sports");

        // 2. Custom Cell Factory (Bech n-affichou tsawer w texte m3a b3adhhom)
        newsListView.setCellFactory(param -> new ListCell<JSONObject>() {
            @Override
            protected void updateItem(JSONObject item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(5);
                    Label title = new Label(item.getString("title"));
                    title.setStyle("-fx-font-weight: bold; -fx-wrap-text: true;");

                    Label desc = new Label(item.optString("description", "Pas de description"));
                    desc.setStyle("-fx-font-size: 11; -fx-text-fill: gray; -fx-wrap-text: true;");
// West el updateItem fel setCellFactory, zid hedhi bech t-farraq bin el articles
                    container.setStyle("-fx-padding: 10; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
                    title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #27ae60;"); // Khdar kima el thème mte3ek
                    container.getChildren().addAll(title, desc);
                    setGraphic(container);
                }
            }
        });

        // 3. Auto-load
        loadNews();
    }

    @FXML
    void loadNews() {
        loader.setVisible(true);
        btnRefresh.setDisable(true);

        new Thread(() -> {
            try {
                String selectedCategory = comboCategory.getValue().toLowerCase();
                String urlString = "https://newsapi.org/v2/everything?q=" + selectedCategory +
                        "&language=fr&sortBy=publishedAt&apiKey=" + API_KEY;

                System.out.println("Tentative: " + urlString); // Debugging

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // OBLIGATOIRE bech l-API mat-bloquich el Java
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = conn.getResponseCode();
                System.out.println("Status Code: " + responseCode);

                if (responseCode == 200) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) result.append(line);
                    rd.close();

                    JSONObject response = new JSONObject(result.toString());
                    JSONArray articles = response.getJSONArray("articles");

                    System.out.println("Nombre d'articles: " + articles.length());

                    ObservableList<JSONObject> data = FXCollections.observableArrayList();
                    for (int i = 0; i < articles.length(); i++) {
                        data.add(articles.getJSONObject(i));
                    }

                    javafx.application.Platform.runLater(() -> {
                        newsListView.setItems(data);
                        loader.setVisible(false);
                        btnRefresh.setDisable(false);
                    });
                } else {
                    // Ken jetek erreur 403 ya3ni l-User Agent mouch mrigel
                    System.err.println("Erreur API Code: " + responseCode);
                    javafx.application.Platform.runLater(() -> {
                        loader.setVisible(false);
                        btnRefresh.setDisable(false);
                    });
                }

            } catch (Exception e) {
                System.err.println("Mochkla fel Connection: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    loader.setVisible(false);
                    btnRefresh.setDisable(false);
                });
            }
        }).start();
    }
}