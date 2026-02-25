package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
        if (lblInfo != null) lblInfo.setText("Vos billets réservés   Total: " + list.size() + " réservation(s)");
    }

    private VBox createCard(ReservationEvenement res) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Réservation #" + res.getIdResEvt());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton PDF
        Button btnPdf = new Button("📄 PDF");
        btnPdf.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnPdf.setOnAction(e -> handleDownloadPdf(res));

        Label status = new Label(res.getStatutRes().toString());
        status.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 5 12; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        row1.getChildren().addAll(title, spacer, btnPdf, status);

        Label details = new Label("📅 Date: " + res.getDateReservation() + " | 🎫 Billets: " + res.getNbBillets());
        details.setStyle("-fx-text-fill: #64748b;");

        card.getChildren().addAll(row1, details);
        return card;
    }

    private void handleDownloadPdf(ReservationEvenement res) {
        try {
            String userHome = System.getProperty("user.home");
            String fileName = "Ticket_Adventure_" + res.getIdResEvt() + ".pdf";
            Path path = Paths.get(userHome, "Downloads", fileName);

            String htmlContent = "<html><head><style>" +
                    "body { font-family: 'Segoe UI', sans-serif; background-color: #f4f7f6; display: flex; justify-content: center; padding: 20px; }" +
                    ".ticket { width: 500px; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 10px 20px rgba(0,0,0,0.1); border-left: 10px solid #27ae60; }" +
                    ".header { background: #2c3e50; color: white; padding: 20px; text-align: center; }" +
                    ".content { padding: 30px; color: #34495e; }" +
                    ".info-row { display: flex; justify-content: space-between; margin-bottom: 15px; border-bottom: 1px dashed #bdc3c7; padding-bottom: 5px; }" +
                    ".label { font-weight: bold; color: #7f8c8d; text-transform: uppercase; font-size: 12px; }" +
                    ".value { font-weight: bold; color: #2c3e50; }" +
                    ".footer { background: #ecf0f1; padding: 15px; text-align: center; font-size: 12px; font-style: italic; color: #95a5a6; }" +
                    ".qr-placeholder { margin-top: 20px; border-top: 2px solid #27ae60; padding-top: 10px; font-weight: bold; color: #27ae60; text-align: center; }" +
                    "</style></head><body>" +
                    "<div class='ticket'>" +
                    "  <div class='header'><h1>ECO ADVENTURE</h1><p>VOTRE BILLET D'ENTRÉE</p></div>" +
                    "  <div class='content'>" +
                    "    <div class='info-row'><span class='label'>RÉSERVATION</span><span class='value'>#" + res.getIdResEvt() + "</span></div>" +
                    "    <div class='info-row'><span class='label'>ÉVÉNEMENT ID</span><span class='value'>" + res.getIdEvenement() + "</span></div>" +
                    "    <div class='info-row'><span class='label'>DATE</span><span class='value'>" + res.getDateReservation() + "</span></div>" +
                    "    <div class='info-row'><span class='label'>PLACES</span><span class='value'>" + res.getNbBillets() + " Billets</span></div>" +
                    "    <div class='info-row'><span class='label'>STATUT</span><span class='value' style='color:#27ae60'>" + res.getStatutRes() + "</span></div>" +
                    "    <div class='qr-placeholder'>TICKET VALIDE - BONNE AVENTURE !</div>" +
                    "  </div>" +
                    "  <div class='footer'>Merci de présenter ce PDF (numérique ou imprimé) le jour J.</div>" +
                    "</div>" +
                    "</body></html>";

            // --- APPEL API ---
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
                            javafx.application.Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Succès");
                                alert.setHeaderText("Ticket Généré !");
                                alert.setContentText("Le PDF est prêt dans votre dossier Téléchargements.");
                                alert.showAndWait();
                            });
                        } else {
                            System.err.println("Erreur API : " + response.statusCode());
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
    @FXML private void goToEvents(ActionEvent event) { switchScene(event, "/views/EvenementClient.fxml"); }
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