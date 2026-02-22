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

        // Update Stats
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

        // 1. Référence & Date
        VBox idBox = new VBox(4);
        Label ref = new Label("REF-" + res.getIdResEvt());
        ref.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #1e293b;");
        Label date = new Label("📅 " + res.getDateReservation().toString().split("T")[0]);
        date.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        idBox.getChildren().addAll(ref, date);
        idBox.setPrefWidth(220);

        // 2. Event Target
        VBox evBox = new VBox(4);
        Label evHead = new Label("ÉVÉNEMENT");
        evHead.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label evVal = new Label("#" + res.getIdEvenement());
        evVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        evBox.getChildren().addAll(evHead, evVal);
        evBox.setPrefWidth(150);

        // 3. Billets Badge
        HBox ticketB = new HBox(5);
        ticketB.setAlignment(Pos.CENTER);
        ticketB.setStyle("-fx-background-color: #f8fafc; -fx-padding: 7 12; -fx-background-radius: 8; -fx-border-color: #e2e8f0;");
        Label tNum = new Label(res.getNbBillets() + " BILLETS");
        tNum.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #475569;");
        ticketB.getChildren().add(tNum);

        // 4. Status (Dynamic Color)
        Label status = new Label(res.getStatutRes().toString());
        String color = res.getStatutRes().equals(StatutReservation.CONFIRMEE) ? "#10b981" : "#f59e0b";
        status.setStyle("-fx-background-color: " + color + "15; -fx-text-fill: " + color + "; " +
                "-fx-padding: 5 12; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px; " +
                "-fx-border-color: " + color + "33; -fx-border-radius: 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 5. Actions
        Button btnDel = new Button("🗑");
        btnDel.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #e11d48; -fx-cursor: hand; -fx-background-radius: 8; -fx-min-width: 35; -fx-min-height: 35;");
        btnDel.setOnAction(e -> handleDelete(res));

        card.getChildren().addAll(idBox, evBox, ticketB, spacer, status, new Label("  "), btnDel);

        // Interactivity
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-border-color: #3b82f6; -fx-translate-y: -2;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-border-color: #3b82f6; -fx-translate-y: -2;", "-fx-border-color: #f1f5f9;")));

        return card;
    }

    private void handleDelete(ReservationEvenement res) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la réservation #" + res.getIdResEvt() + " ?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                service.delete(res.getIdResEvt());
                loadData();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void onSearchReservation() {
        String q = searchReservationField.getText().toLowerCase();
        List<ReservationEvenement> filtered = masterData.stream()
                .filter(r -> String.valueOf(r.getIdEvenement()).contains(q) ||
                        r.getStatutRes().toString().toLowerCase().contains(q))
                .collect(Collectors.toList());
        displayCards(filtered);
    }

    @FXML private void onRefresh() { searchReservationField.clear(); loadData(); }

    // Navigation Utils
    private void switchScene(ActionEvent event, String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void goHome(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }
    @FXML private void goToEvents(ActionEvent event) { switchScene(event, "/views/evenement_list.fxml"); }
    @FXML private void onLogout(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }
}