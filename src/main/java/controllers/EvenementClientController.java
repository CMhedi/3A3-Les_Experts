package controllers;

import Entities.Evenement;
import Entities.ReservationEvenement;
import Services.EvenementService;
import Services.ReservationEvenementService;
import enums.StatutReservation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class EvenementClientController implements Initializable {

    @FXML private VBox cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    private final EvenementService evenementService = new EvenementService();
    private final ReservationEvenementService reservationService = new ReservationEvenementService();
    private List<Evenement> allEvents;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
        if (lblInfo != null) lblInfo.setText("(" + list.size() + ")");
    }

    private VBox createEventCard(Evenement ev) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Header (Titre & Catégorie)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #1e293b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label cat = new Label(ev.getCategorieEvt().toString());
        cat.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 5 10; -fx-background-radius: 10; -fx-font-size: 11px;");
        header.getChildren().addAll(title, spacer, cat);

        // Body
        Label details = new Label("📍 " + ev.getLieu() + "  |  📅 " + ev.getDateEvent());
        details.setStyle("-fx-text-fill: #64748b;");
        Label desc = new Label(ev.getDescription());
        desc.setWrapText(true);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label places = new Label("👥 " + ev.getNbPlaces() + " places restantes");
        places.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (ev.getNbPlaces() > 0 ? "#10b981" : "#ef4444"));

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button btnReser = new Button("Réserver");
        btnReser.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btnReser.setDisable(ev.getNbPlaces() <= 0);
        btnReser.setOnAction(e -> handleReservation(ev));

        footer.getChildren().addAll(places, spacer2, btnReser);
        card.getChildren().addAll(header, details, desc, new Separator(), footer);
        return card;
    }

    private void handleReservation(Evenement ev) {
        // 1. Création du Dialog
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Confirmer votre réservation");
        dialog.setHeaderText("Événement: " + ev.getTitre());

        ButtonType confirmButtonType = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // 2. Formulaire
        VBox form = new VBox(10);
        form.setPadding(new Insets(20, 50, 10, 10));
        Spinner<Integer> nbBillets = new Spinner<>(1, ev.getNbPlaces(), 1);
        form.getChildren().addAll(new Label("Nombre de billets souhaités:"), nbBillets);
        dialog.getDialogPane().setContent(form);

        // 3. Traitement
        dialog.setResultConverter(btn -> btn == confirmButtonType ? nbBillets.getValue() : null);

        dialog.showAndWait().ifPresent(quantity -> {
            try {
                ReservationEvenement res = new ReservationEvenement();
                res.setIdEvenement(ev.getIdEvenement());
                res.setNbBillets(quantity);
                res.setDateReservation(LocalDateTime.now());
                res.setStatutRes(StatutReservation.EN_ATTENTE);

                reservationService.add(res); // Enregistrement Base

                // Optionnel: Mise à jour nb places dans l'entité Evenement
                ev.setNbPlaces(ev.getNbPlaces() - quantity);
                evenementService.update(ev);

                new Alert(Alert.AlertType.INFORMATION, "Réservation réussie !").show();
                onRefresh();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @FXML private void onSearch() {
        String q = searchField.getText().toLowerCase();
        displayCards(allEvents.stream()
                .filter(e -> e.getTitre().toLowerCase().contains(q) || e.getLieu().toLowerCase().contains(q))
                .collect(Collectors.toList()));
    }

    @FXML private void onRefresh() { searchField.clear(); loadEvents(); }

    // Navigation
    private void switchScene(ActionEvent event, String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void goToReservations(ActionEvent event) { switchScene(event, "/views/reservation_list.fxml"); }
    @FXML private void goHome(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }
    @FXML private void logout(ActionEvent event) { switchScene(event, "/views/Home.fxml"); }
    @FXML private void goToEvenements(ActionEvent event) { onRefresh(); }
}