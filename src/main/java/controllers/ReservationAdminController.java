package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import enums.StatutReservation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReservationAdminController implements Initializable {

    @FXML
    private VBox cardsContainer;
    @FXML
    private TextField searchReservationField;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private List<ReservationEvenement> masterData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    private void loadData() {
        try {
            masterData = service.getAll();
            updateDashboard(masterData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDashboard(List<ReservationEvenement> list) {
        if (list == null)
            return;

        // 2. Clear and Display Cards
        cardsContainer.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Aucune réservation trouvée.");
            empty.getStyleClass().add("mutedText");
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (ReservationEvenement res : list) {
            cardsContainer.getChildren().add(createProfessionalCard(res));
        }
    }

    private HBox createProfessionalCard(ReservationEvenement res) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("modern-card");
        card.setStyle("-fx-padding: 15 25; -fx-background-radius: 15; -fx-background-color: white; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");

        // Info Block (Only Event Name)
        VBox info = new VBox(4);
        Label eventName = new Label(res.getNomEvenement());
        eventName.setStyle("-fx-font-weight: bold; -fx-text-fill: #059669; -fx-font-size: 15px;");
        info.getChildren().addAll(eventName);
        info.setPrefWidth(300);

        // Date & Tickets
        VBox details = new VBox(4);
        Label date = new Label("📅 " + res.getDateReservation().toString().replace("T", " "));
        date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        Label tickets = new Label("🎟️ " + res.getNbBillets() + " BILLETS");
        tickets.setStyle("-fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: #475569;");
        details.getChildren().addAll(date, tickets);
        details.setPrefWidth(200);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badge (Read Only)
        Label statusBadge = new Label(res.getStatutRes().toString());
        String color = "#64748b";
        if (res.getStatutRes() == StatutReservation.CONFIRMEE)
            color = "#10b981";
        else if (res.getStatutRes() == StatutReservation.EN_ATTENTE)
            color = "#f59e0b";
        else if (res.getStatutRes() == StatutReservation.ANNULEE)
            color = "#ef4444";

        statusBadge.setStyle("-fx-background-color: " + color + "15; -fx-text-fill: " + color + "; " +
                "-fx-padding: 6 15; -fx-background-radius: 30; -fx-font-weight: 900; -fx-font-size: 11px; " +
                "-fx-border-color: " + color + "33; -fx-border-radius: 30;");

        card.getChildren().addAll(info, details, spacer, statusBadge);
        return card;
    }

    @FXML
    private void onSearchReservation() {
        String q = searchReservationField.getText().toLowerCase();
        List<ReservationEvenement> filtered = masterData.stream()
                .filter(r -> (r.getNomEvenement() != null && r.getNomEvenement().toLowerCase().contains(q)) ||
                        r.getStatutRes().toString().toLowerCase().contains(q) ||
                        String.valueOf(r.getIdResEvt()).contains(q))
                .collect(Collectors.toList());
        updateDashboard(filtered);
    }

    @FXML
    private void onRefresh() {
        searchReservationField.clear();
        loadData();
    }

    private void switchScene(ActionEvent event, String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goHome(ActionEvent event) {
        switchScene(event, "/views/AdminDashboard.fxml");
    }

    @FXML
    private void goToEvents(ActionEvent event) {
        switchScene(event, "/views/evenement_list.fxml");
    }

    @FXML
    private void onLogout(ActionEvent event) {
        switchScene(event, "/views/Home.fxml");
    }
}