package controllers;

import Entities.Evenement;
import Services.EvenementService;
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
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class EvenementClientController {

    @FXML private VBox cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    private final EvenementService evenementService = new EvenementService();
    private List<Evenement> allEvents;

    @FXML
    public void initialize() {
        loadEvents();
    }

    private void loadEvents() {
        try {
            allEvents = evenementService.getAll();
            displayCards(allEvents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayCards(List<Evenement> list) {
        cardsContainer.getChildren().clear();
        for (Evenement ev : list) {
            cardsContainer.getChildren().add(createEventCard(ev));
        }
    }

    private VBox createEventCard(Evenement ev) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label category = new Label(ev.getCategorieEvt() != null ? ev.getCategorieEvt().name() : "Général");
        category.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 11px;");

        header.getChildren().addAll(title, spacer, category);

        Label info = new Label("📍 " + ev.getLieu() + "  |  📅 " + ev.getDateEvent());
        info.setStyle("-fx-text-fill: #64748b;");

        Label desc = new Label(ev.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #475569;");

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label places = new Label("👥 Places: " + ev.getNbPlaces());
        places.setStyle("-fx-font-weight: bold; -fx-text-fill: #059669;");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button btnReserve = new Button("Réserver");
        btnReserve.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8;");
        btnReserve.setOnAction(e -> handleReservation(ev));

        footer.getChildren().addAll(places, spacer2, btnReserve);
        card.getChildren().addAll(header, info, desc, new Separator(), footer);

        return card;
    }

    private void handleReservation(Evenement ev) {
        // Houni t7el el popup mta3 el réservation (form)
        System.out.println("Réserver l'event: " + ev.getTitre());
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText().toLowerCase();
        List<Evenement> filtered = allEvents.stream()
                .filter(e -> e.getTitre().toLowerCase().contains(q) || e.getLieu().toLowerCase().contains(q))
                .collect(Collectors.toList());
        displayCards(filtered);
    }

    @FXML private void onRefresh() { searchField.clear(); loadEvents(); }

    @FXML private void goToReservations(ActionEvent event) { switchScene(event, "/views/reservation_list_client.fxml"); }
    @FXML private void goToEvenements(ActionEvent event) { loadEvents(); }
    @FXML private void goHome(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }
    @FXML private void logout(ActionEvent event) { switchScene(event, "/views/Login.fxml"); }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("❌ Path mal9itech: " + fxmlPath);
                return;
            }
            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}