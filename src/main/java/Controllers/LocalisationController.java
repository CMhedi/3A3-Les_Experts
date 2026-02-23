package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class LocalisationController {

    @FXML private WebView webView;

    @FXML
    public void initialize() {

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        // Carte centrée sur Tunis par défaut
        double lat = 36.8065;
        double lon = 10.1815;

        String url = "https://www.openstreetmap.org/export/embed.html?bbox="
                + (lon - 0.05) + "%2C"
                + (lat - 0.05) + "%2C"
                + (lon + 0.05) + "%2C"
                + (lat + 0.05)
                + "&layer=mapnik&marker="
                + lat + "%2C" + lon;

        engine.load(url);
    }

    @FXML
    private void close(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}