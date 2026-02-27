package controllers;

import Entities.Evenement;
import Entities.ReservationEvenement;
import Services.EvenementService;
import Services.ReservationEvenementService;
import enums.StatutReservation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class ClientEventsController implements Initializable {

    @FXML private VBox clientCardsContainer;
    @FXML private TextField searchField;

    private final EvenementService evenementService = new EvenementService();
    private final ReservationEvenementService reservationService = new ReservationEvenementService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Style mta3 el Search Field
        searchField.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 15; -fx-border-color: #e2e8f0;");
        refreshEvents();
    }

    @FXML
    public void refreshEvents() {
        try {
            clientCardsContainer.getChildren().clear();
            List<Evenement> list = evenementService.getAll();
            for (Evenement ev : list) {
                clientCardsContainer.getChildren().add(createEventCard(ev));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- POPUP AZYEN (MODERN DESIGN) ---
    private void handleReservation(Evenement ev) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UNDECORATED); // Na7iw el cadre el 5ayeb mta3 windows

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        // Style Glassmorphism / Modern Card
        root.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #064e3b; -fx-border-width: 2; -fx-border-radius: 20;");

        Label icon = new Label("🎟️");
        icon.setStyle("-fx-font-size: 40px;");

        Label title = new Label("Réserver votre place");
        title.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-weight: 900; -fx-font-size: 22px; -fx-text-fill: #1e293b;");

        Label subTitle = new Label(ev.getTitre());
        subTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");

        VBox inputGroup = new VBox(8);
        inputGroup.setAlignment(Pos.CENTER_LEFT);
        Label labelSpinner = new Label("Nombre de billets :");
        labelSpinner.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        Spinner<Integer> spinner = new Spinner<>(1, ev.getNbPlaces(), 1);
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;");

        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);

        Button btnConfirm = new Button("Confirmer");
        btnConfirm.setStyle("-fx-background-color: #064e3b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");

        btnCancel.setOnAction(e -> popupStage.close());
        btnConfirm.setOnAction(e -> {
            try {
                ReservationEvenement newRes = new ReservationEvenement();
                newRes.setIdEvenement(ev.getIdEvenement());
                newRes.setNbBillets(spinner.getValue());
                newRes.setStatutRes(StatutReservation.CONFIRMEE);
                newRes.setDateReservation(LocalDateTime.now());

                reservationService.add(newRes);
                ev.setNbPlaces(ev.getNbPlaces() - spinner.getValue());
                evenementService.update(ev);

                popupStage.close();
                refreshEvents();
                showCustomAlert("Succès", "Votre réservation a été enregistrée !");
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        buttons.getChildren().addAll(btnCancel, btnConfirm);
        inputGroup.getChildren().addAll(labelSpinner, spinner);
        root.getChildren().addAll(icon, title, subTitle, inputGroup, buttons);

        Scene scene = new Scene(root);
        scene.setFill(null); // Bechy ji el bord arrondis mrigel
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    // --- CARDS AZYEN (MODERN) ---
    private VBox createEventCard(Evenement ev) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 8);");

        // Header Section
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleArea = new VBox(3);
        Label titleLabel = new Label(ev.getTitre());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        Label locationLabel = new Label("📍 " + ev.getLieu());
        locationLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        titleArea.getChildren().addAll(titleLabel, locationLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priceBadge = new Label("DISPONIBLE");
        priceBadge.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #10b981; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 10px;");

        header.getChildren().addAll(titleArea, spacer, priceBadge);

        // Stats Row
        HBox stats = new HBox(20);
        stats.setStyle("-fx-padding: 10 0;");
        Label dateInfo = new Label("📅 " + (ev.getDateEvent() != null ? ev.getDateEvent().toString().split("T")[0] : "À venir"));
        Label slotsInfo = new Label("👥 " + ev.getNbPlaces() + " places restantes");
        dateInfo.setStyle("-fx-text-fill: #475569; -fx-font-weight: 600;");
        slotsInfo.setStyle("-fx-text-fill: #475569; -fx-font-weight: 600;");
        stats.getChildren().addAll(dateInfo, slotsInfo);

        Button btnReserver = new Button("🎫 Réserver maintenant");
        btnReserver.setMaxWidth(Double.MAX_VALUE);
        btnReserver.setStyle("-fx-background-color: linear-gradient(to right, #064e3b, #059669); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12; -fx-cursor: hand;");

        if(ev.getNbPlaces() <= 0) {
            btnReserver.setDisable(true);
            btnReserver.setText("Complet");
            btnReserver.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-padding: 12;");
        }

        btnReserver.setOnAction(e -> handleReservation(ev));

        // Hover Effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-translate-y: -8; -fx-effect: dropshadow(three-pass-box, rgba(6,78,59,0.2), 20, 0, 0, 10);"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-translate-y: -8; -fx-effect: dropshadow(three-pass-box, rgba(6,78,59,0.2), 20, 0, 0, 10);", "")));

        card.getChildren().addAll(header, stats, new Separator(), btnReserver);
        return card;
    }

    private void showCustomAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
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
        } catch (Exception e) { e.printStackTrace(); }
    }
}