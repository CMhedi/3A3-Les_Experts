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
import java.util.List;
import java.util.stream.Collectors;

public class ReservationClientController {

    @FXML private VBox cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private List<ReservationEvenement> allReservations;

    @FXML
    public void initialize() {
        loadAll(); // Load normal ki n7ellu el page direct
    }

    private void loadAll() {
        try {
            allReservations = service.getAll();
            displayCards(allReservations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- HEDHI EL METHODE EL NA9SA LI KENET M3ATLETELK EL DENYA ---
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
        if (lblInfo != null) lblInfo.setText("Total: " + list.size() + " réservation(s)");
    }

    private VBox createCard(ReservationEvenement res) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        HBox row1 = new HBox();
        row1.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Réservation #" + res.getIdResEvt());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(res.getStatutRes().toString());
        status.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 5 12; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        row1.getChildren().addAll(title, spacer, status);

        Label details = new Label("📅 Date: " + res.getDateReservation() + " | 🎫 Billets: " + res.getNbBillets());
        details.setStyle("-fx-text-fill: #64748b;");

        card.getChildren().addAll(row1, details);
        return card;
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText().toLowerCase();
        List<ReservationEvenement> filtered = allReservations.stream()
                .filter(r -> String.valueOf(r.getIdEvenement()).contains(q) || r.getStatutRes().toString().toLowerCase().contains(q))
                .collect(Collectors.toList());
        displayCards(filtered);
    }

    @FXML private void onRefresh() { searchField.clear(); loadAll(); }

    // Navigation
    @FXML
    private void goToEvents(ActionEvent event) {
        switchScene(event, "/views/EvenementClient.fxml");
    }

    @FXML
    private void goHome(ActionEvent event) {
        switchScene(event, "/views/Home.fxml");
    }

    @FXML
    private void onLogout(ActionEvent event) {
        switchScene(event, "/views/Home.fxml");
    }

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