package Controllers;

import javafx.concurrent.Worker;
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

        // Tunis par défaut
        double lat = 36.8065;
        double lon = 10.1815;
        int zoom = 12;

        engine.loadContent(buildMapHtml(lat, lon, zoom));

        // Fix WebView: après chargement, on force Leaflet à recalculer la taille
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                engine.executeScript(
                        "setTimeout(function(){ if(window.__map){ window.__map.invalidateSize(); } }, 300);"
                );
            }
        });

        // Fix lors du resize fenêtre
        webView.widthProperty().addListener((o, a, b) ->
                engine.executeScript("if(window.__map){ window.__map.invalidateSize(); }")
        );
        webView.heightProperty().addListener((o, a, b) ->
                engine.executeScript("if(window.__map){ window.__map.invalidateSize(); }")
        );
    }

    private String buildMapHtml(double lat, double lon, int zoom) {

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='utf-8'/>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>" +

                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +

                "<style>" +
                "html, body { margin:0; padding:0; height:100%; width:100%; }" +
                "#map { height:100%; width:100%; }" +
                "</style>" +
                "</head>" +

                "<body>" +
                "<div id='map'></div>" +

                "<script>" +
                "var map = L.map('map').setView([" + lat + "," + lon + "], " + zoom + ");" +
                "window.__map = map;" +

                // ✅ IMPORTANT: un seul domaine (évite les carrés gris sur WebView)
                "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                "maxZoom: 19," +
                "attribution: '© OpenStreetMap contributors'" +
                "}).addTo(map);" +

                "L.marker([" + lat + "," + lon + "]).addTo(map);" +

                // petit refresh
                "setTimeout(function(){ map.invalidateSize(); }, 300);" +
                "</script>" +

                "</body>" +
                "</html>";
    }

    @FXML
    private void close(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}