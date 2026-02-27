package controllers;

import Entities.Evenement;
import Services.EvenementService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ClientEventsController implements Initializable {

    @FXML private VBox clientCardsContainer; // El container elli f ScrollPane
    @FXML private TextField searchField;

    private final EvenementService evenementService = new EvenementService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshEvents();
    }

    // Hedhi el fonction elli tejbed dima el jdid mel base
    @FXML
    public void refreshEvents() {
        try {
            clientCardsContainer.getChildren().clear();
            List<Evenement> list = evenementService.getAll();

            for (Evenement ev : list) {
                clientCardsContainer.getChildren().add(createEventCard(ev));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement des événements : " + e.getMessage());
        }
    }

    // Fonction bech n-créiw el Card dynamique m3a design l'client
    private VBox createEventCard(Evenement ev) {
        VBox card = new VBox(12);
        card.getStyleClass().add("event-card-client"); // Tzidha f styles/app.css
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Header: Titre + Statut
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(ev.getTitre());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(ev.getStatut().toUpperCase());
        statusBadge.setStyle("-fx-background-color: #fce4e4; -fx-text-fill: #e74c3c; -fx-padding: 5 10; -fx-background-radius: 10; -fx-font-size: 11px;");

        header.getChildren().addAll(titleLabel, spacer, statusBadge);

        // Details: Date, Lieu, Places
        Label detailsLabel = new Label("📍 " + ev.getLieu() + " | 📅 " + (ev.getDateEvent() != null ? ev.getDateEvent().toString() : "N/A"));
        detailsLabel.setStyle("-fx-text-fill: #7f8c8d;");

        Label placesLabel = new Label("👥 Places disponibles: " + ev.getNbPlaces());
        placesLabel.setStyle("-fx-text-fill: #34495e; -fx-font-weight: bold;");

        // Action: Bouton Réserver
        Button btnReserver = new Button("🎫 Réserver ma place");
        btnReserver.getStyleClass().add("primaryBtn");
        btnReserver.setMaxWidth(Double.MAX_VALUE); // Bechy ji 3la toul el card
        btnReserver.setOnAction(e -> handleReservation(ev));

        card.getChildren().addAll(header, detailsLabel, placesLabel, new Separator(), btnReserver);

        return card;
    }

    private void handleReservation(Evenement ev) {
        // Houni t7el el dialogue wala tbadel el scene lel reservation
        System.out.println("L'utilisateur veut réserver l'event ID: " + ev.getIdEvenement());
        // Would you like me to create the reservation logic next?
    }

    @FXML
    private void onSearch() {
        String filter = searchField.getText().toLowerCase();
        clientCardsContainer.getChildren().clear();
        try {
            List<Evenement> list = evenementService.getAll();
            list.stream()
                    .filter(e -> e.getTitre().toLowerCase().contains(filter) || e.getLieu().toLowerCase().contains(filter))
                    .forEach(e -> clientCardsContainer.getChildren().add(createEventCard(e)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Navigation (Lazemhom ykounou mawjoudin b nafs esm onAction f FXML)
    @FXML private void goHome() { /* Navigator.go("HomeClient") */ }
    @FXML private void goToReservations() { /* Navigator.go("MesReservations") */ }
}