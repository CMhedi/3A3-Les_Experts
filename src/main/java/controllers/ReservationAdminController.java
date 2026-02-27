package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import enums.StatutReservation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReservationAdminController implements Initializable {

    @FXML private VBox cardsContainer;
    @FXML private TextField searchReservationField;
    @FXML private Label lblStats;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private List<ReservationEvenement> masterData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    private void loadData() {
        try {
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

        long confirmes = list.stream().filter(r -> r.getStatutRes().equals(StatutReservation.CONFIRMEE)).count();
        lblStats.setText("📊 " + list.size() + " Total | ✅ " + confirmes + " Confirmées");

        for (ReservationEvenement res : list) {
            cardsContainer.getChildren().add(createAdminCard(res));
        }
    }

    private HBox createAdminCard(ReservationEvenement res) {
        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                "-fx-border-color: #f1f5f9; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 4);");

        // Section Référence et Date
        VBox idBox = new VBox(4);
        Label ref = new Label("RÉSERVATION #" + res.getIdResEvt());
        ref.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1e293b;");
        Label date = new Label("📅 " + res.getDateReservation().toString().split("T")[0]);
        date.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        idBox.getChildren().addAll(ref, date);
        idBox.setPrefWidth(300); // Zidna f-el width khater na7ina el-ID mta3 el-evenement

        // Section Billets
        HBox ticketB = new HBox(5);
        ticketB.setAlignment(Pos.CENTER);
        ticketB.setStyle("-fx-background-color: #f8fafc; -fx-padding: 7 12; -fx-background-radius: 8; -fx-border-color: #e2e8f0;");
        Label tNum = new Label(res.getNbBillets() + " BILLETS");
        tNum.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #475569;");
        ticketB.getChildren().add(tNum);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Section Statut
        Label status = new Label(res.getStatutRes().toString());
        String color = res.getStatutRes().equals(StatutReservation.CONFIRMEE) ? "#10b981" : "#f59e0b";
        status.setStyle("-fx-background-color: " + color + "15; -fx-text-fill: " + color + "; " +
                "-fx-padding: 6 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px; " +
                "-fx-border-color: " + color + "33; -fx-border-radius: 20;");

        // Card Construction (Bouton Supprimer N7inaH)
        card.getChildren().addAll(idBox, ticketB, spacer, status);

        // Effects
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-border-color: #143D30; -fx-translate-y: -2;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-border-color: #143D30; -fx-translate-y: -2;", "-fx-border-color: #f1f5f9;")));

        return card;
    }

    @FXML
    private void onSearchReservation() {
        if (masterData == null) return;
        String q = searchReservationField.getText().toLowerCase();
        List<ReservationEvenement> filtered = masterData.stream()
                .filter(r -> r.getStatutRes().toString().toLowerCase().contains(q) ||
                        String.valueOf(r.getIdResEvt()).contains(q))
                .collect(Collectors.toList());
        displayCards(filtered);
    }

    @FXML private void onRefresh() { searchReservationField.clear(); loadData(); }
}