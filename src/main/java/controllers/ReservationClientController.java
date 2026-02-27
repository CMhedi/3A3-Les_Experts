package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
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

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationClientController {

    @FXML private VBox cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private List<ReservationEvenement> allReservations;

    private final String API_KEY = "sk_d37f1a91b211235dd61ee19e3438b61e2e1ded0d";

    @FXML
    public void initialize() {
        loadAll();
    }

    private void loadAll() {
        try {
            allReservations = service.getAll();
            displayCards(allReservations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadReservationsByEventId(int eventId) {
        try {
            allReservations = service.getAll();
            List<ReservationEvenement> filtered = allReservations.stream()
                    .filter(r -> r.getIdEvenement() == eventId)
                    .collect(Collectors.toList());
            displayCards(filtered);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayCards(List<ReservationEvenement> list) {
        cardsContainer.getChildren().clear();
        for (ReservationEvenement res : list) {
            cardsContainer.getChildren().add(createCard(res));
        }
        if (lblInfo != null) lblInfo.setText("🎫 Vos billets réservés | Total: " + list.size());
    }

    private VBox createCard(ReservationEvenement res) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");

        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Réservation #" + res.getIdResEvt());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: #1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnPdf = new Button("📄 PDF");
        btnPdf.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 6 15;");
        btnPdf.setOnAction(e -> handleDownloadPdf(res));

        Label status = new Label(res.getStatutRes().toString());
        status.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #065f46; -fx-padding: 5 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");

        row1.getChildren().addAll(title, spacer, btnPdf, status);

        Label details = new Label("📅 " + res.getDateReservation() + "  |  🎫 " + res.getNbBillets() + " places");
        details.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        card.getChildren().addAll(row1, details);
        return card;
    }

    private void handleDownloadPdf(ReservationEvenement res) {
        try {
            String userHome = System.getProperty("user.home");
            String fileName = "Ticket_EcoAdventure_" + res.getIdResEvt() + ".pdf";
            Path path = Paths.get(userHome, "Downloads", fileName);

            // --- FIX : Formatage et encodage de la data du QR Code ---
            String rawData = "ECO ADVENTURE TICKET\n" +
                    "--------------------\n" +
                    "ID: #" + res.getIdResEvt() + "\n" +
                    "Evenement ID: " + res.getIdEvenement() + "\n" +
                    "Nombre de places: " + res.getNbBillets() + "\n" +
                    "Statut: " + res.getStatutRes();

            // URLEncoder mte3na bech el URL yetba3ath s7i7 lèl API
            String encodedData = URLEncoder.encode(rawData, StandardCharsets.UTF_8);
            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encodedData;

            String htmlContent = "<html><head><style>" +
                    "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f1f5f9; padding: 40px; text-align: center; }" +
                    ".ticket { width: 550px; margin: auto; background: white; border-radius: 25px; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.1); border-top: 15px solid #064e3b; }" +
                    ".header { background: #064e3b; color: white; padding: 30px; }" +
                    ".header h1 { margin: 0; font-size: 30px; letter-spacing: 2px; }" +
                    ".content { padding: 40px; color: #1e293b; text-align: left; }" +
                    ".info-row { display: flex; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px dashed #e2e8f0; padding-bottom: 10px; }" +
                    ".label { font-weight: bold; color: #64748b; text-transform: uppercase; font-size: 11px; }" +
                    ".value { font-weight: bold; color: #0f172a; font-size: 15px; }" +
                    ".qr-container { text-align: center; margin-top: 30px; padding: 25px; background: #f8fafc; border-radius: 20px; border: 2px solid #e2e8f0; }" +
                    ".qr-img { width: 170px; height: 170px; border: 5px solid white; }" +
                    ".footer { background: #f8fafc; padding: 20px; font-size: 12px; color: #94a3b8; font-style: italic; }" +
                    "</style></head><body>" +
                    "<div class='ticket'>" +
                    "  <div class='header'><h1>ECO ADVENTURE</h1><p>BILLETERIE OFFICIELLE</p></div>" +
                    "  <div class='content'>" +
                    "    <div class='info-row'><span class='label'>N° RÉSERVATION</span><span class='value'>#" + res.getIdResEvt() + "</span></div>" +
                    "    <div class='info-row'><span class='label'>ID ÉVÉNEMENT</span><span class='value'>" + res.getIdEvenement() + "</span></div>" +
                    "    <div class='info-row'><span class='label'>DATE D'ACHAT</span><span class='value'>" + res.getDateReservation() + "</span></div>" +
                    "    <div class='info-row'><span class='label'>PLACES</span><span class='value'>" + res.getNbBillets() + " Billets</span></div>" +
                    "    <div class='info-row'><span class='label'>STATUT</span><span class='value' style='color:#059669'>" + res.getStatutRes() + "</span></div>" +
                    "    <div class='qr-container'>" +
                    "       <p style='margin-bottom: 15px; font-weight: bold; color: #064e3b;'>SCANNABLE À L'ENTRÉE</p>" +
                    "       <img src='" + qrCodeUrl + "' class='qr-img' />" +
                    "    </div>" +
                    "  </div>" +
                    "  <div class='footer'>Ce billet est strictement personnel et scannable via smartphone.</div>" +
                    "</div>" +
                    "</body></html>";

            // --- APPEL API PDFSHIFT ---
            String auth = "api:" + API_KEY;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            String jsonPayload = "{\"source\": \"" + htmlContent.replace("\"", "\\\"").replace("\n", "") + "\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.pdfshift.io/v3/convert/pdf"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + encodedAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(path))
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            Platform.runLater(() -> {
                                try {
                                    File file = path.toFile();
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(file);
                                    }
                                } catch (Exception ex) { ex.printStackTrace(); }

                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Succès");
                                alert.setHeaderText("Ticket avec QR Code prêt !");
                                alert.setContentText("Le PDF a été généré avec succès.");
                                alert.showAndWait();
                            });
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void onSearch() {
        String q = searchField.getText().toLowerCase();
        List<ReservationEvenement> filtered = allReservations.stream()
                .filter(r -> String.valueOf(r.getIdEvenement()).contains(q) || r.getStatutRes().toString().toLowerCase().contains(q))
                .collect(Collectors.toList());
        displayCards(filtered);
    }

    @FXML private void onRefresh() { searchField.clear(); loadAll(); }
    @FXML private void goToEvents(ActionEvent event) { switchScene(event, "/GUI/EvenementClient.fxml"); }
    @FXML private void goHome(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }
    @FXML private void onLogout(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }

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