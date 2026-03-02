package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import enums.StatutReservation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReservationAdminController implements Initializable {

    @FXML
    private VBox cardsContainer;
    @FXML
    private TextField searchReservationField;
    @FXML
    private Label lblStats;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private List<ReservationEvenement> masterData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    private void loadData() {
        try {
            // Chargement initial des données
            masterData = service.getAll();
            displayCards(masterData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayCards(List<ReservationEvenement> list) {
        cardsContainer.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label empty = new Label("Aucune réservation à afficher.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            cardsContainer.getChildren().add(empty);
            lblStats.setText("0 Réservations");
            return;
        }

        // Mise à jour des statistiques textuelles
        long confirmes = list.stream().filter(r -> r.getStatutRes().equals(StatutReservation.CONFIRMEE)).count();
        lblStats.setText("📊 " + list.size() + " Total | ✅ " + confirmes + " Confirmées");

        for (ReservationEvenement res : list) {
            cardsContainer.getChildren().add(createAdminCard(res));
        }
    }

    private HBox createAdminCard(ReservationEvenement res) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 30, 20, 30));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 12, 0, 0, 5); " +
                "-fx-border-color: #f1f5f9; -fx-border-width: 1; -fx-border-radius: 18;");

        // --- 1. BLOC GAUCHE : Événement et Client ---
        VBox mainInfo = new VBox(5);
        Label title = new Label(res.getNomEvenement() != null ? res.getNomEvenement() : "Événement #" + res.getIdEvenement());
        title.setStyle("-fx-font-weight: 900; -fx-font-size: 18px; -fx-text-fill: #0f172a;");

        Label client = new Label("Par : " + (res.getNomUser() != null ? res.getNomUser() : "Client #" + res.getIdUser()));
        client.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold;");
        mainInfo.getChildren().addAll(title, client);
        mainInfo.setPrefWidth(350);

        // --- 2. BLOC MILIEU : Date et Billets ---
        VBox meta = new VBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label date = new Label("📅 " + (res.getDateReservation() != null ? res.getDateReservation().toString().split("T")[0] : "N/A"));
        date.setStyle("-fx-text-fill: #475569; -fx-font-weight: 600; -fx-font-size: 13px;");

        Label tickets = new Label("👥 " + res.getNbBillets() + " Billets");
        tickets.setStyle("-fx-text-fill: #475569; -fx-font-weight: 600; -fx-font-size: 13px;");

        meta.getChildren().addAll(date, tickets);

        // Spacer pour pousser le prix vers la droite
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- 3. BLOC DROITE : Prix (Remplace l'icône/bouton) ---
        Label priceLabel = new Label(String.format("%.2f", res.getPrixTotal()) + " DT");
        priceLabel.setStyle("-fx-text-fill: #143D30; -fx-font-weight: 900; -fx-font-size: 22px;");

        // Construction de la carte sans boutons ni badges de statut
        card.getChildren().addAll(mainInfo, meta, spacer, priceLabel);

        return card;
    }

    @FXML
    private void onSearchReservation() {
        String q = searchReservationField.getText().toLowerCase();
        if (masterData == null) return;

        List<ReservationEvenement> filtered = masterData.stream()
                .filter(r -> (r.getNomEvenement() != null && r.getNomEvenement().toLowerCase().contains(q)) ||
                        (r.getNomUser() != null && r.getNomUser().toLowerCase().contains(q)) ||
                        r.getStatutRes().toString().toLowerCase().contains(q))
                .collect(Collectors.toList());
        displayCards(filtered);
    }
}