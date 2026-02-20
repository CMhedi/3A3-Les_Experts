package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import Utiles.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationClientController {

    @FXML private VBox cardsContainer;
    @FXML private Label lblInfo;
    @FXML private TextField searchField;

    private final ReservationEvenementService service = new ReservationEvenementService();

    @FXML
    public void initialize() {
        loadData();
    }

    // El méthode elli kenet t-9ala9 fik fil EvenementClientController
    public void loadReservationsByEventId(int eventId) {
        try {
            List<ReservationEvenement> filtered = service.getAll().stream()
                    .filter(r -> r.getIdEvenement() == eventId)
                    .collect(Collectors.toList());
            renderCards(filtered);
            if (lblInfo != null) lblInfo.setText("Filtre: " + filtered.size() + " réservation(s)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        try {
            List<ReservationEvenement> list = service.getAll();
            renderCards(list);
            if (lblInfo != null) lblInfo.setText("Total: " + list.size() + " réservation(s)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderCards(List<ReservationEvenement> list) {
        cardsContainer.getChildren().clear();
        if (list.isEmpty()) {
            cardsContainer.getChildren().add(new Label("Aucune réservation trouvée."));
            return;
        }
        for (ReservationEvenement res : list) {
            cardsContainer.getChildren().add(createCard(res));
        }
    }

    private VBox createCard(ReservationEvenement res) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        HBox header = new HBox(15);
        Label title = new Label("Réservation #" + res.getIdResEvt());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #143D30;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Fix lel Enum error (toUpperCase/equalsIgnoreCase)
        String statusName = (res.getStatutRes() != null) ? res.getStatutRes().name() : "INCONNU";
        Label statusLabel = new Label(statusName.toUpperCase());

        if (statusName.equalsIgnoreCase("CONFIRME") || statusName.equalsIgnoreCase("PAYEE")) {
            statusLabel.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-padding: 5 10; -fx-background-radius: 10;");
        } else {
            statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 5 10; -fx-background-radius: 10;");
        }

        header.getChildren().addAll(title, spacer, statusLabel);
        Label details = new Label("📅 Date: " + res.getDateReservation() + "  |  🎫 Billets: " + res.getNbBillets());
        details.getStyleClass().add("mutedText");

        card.getChildren().addAll(header, details);
        return card;
    }

    @FXML void onRefresh() { loadData(); }

    @FXML
    void goHome(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SceneNavigator.go(stage, "/views/Home.fxml", "EcoAdventure - Accueil");
    }
}