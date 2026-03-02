package controllers;

import Entities.Evenement;
import Entities.ReservationEvenement;
import Services.EvenementService;
import Services.ReservationEvenementService;
import GUI.utils.DialogUtils;
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
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class ClientEventsController implements Initializable {

    @FXML
    private VBox clientCardsContainer;
    @FXML
    private TextField searchField;

    private final EvenementService evenementService = new EvenementService();
    private final ReservationEvenementService reservationService = new ReservationEvenementService();

    // --- API CONFIG ---
    private final String WEATHER_API_KEY = "ba052da226f1813069d54ebd059266ce";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        searchField.setStyle(
                "-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 15; -fx-border-color: #e2e8f0;");
        refreshEvents();
    }

    @FXML
    public void refreshEvents() {
        try {
            clientCardsContainer.getChildren().clear();
            int userId = (Entities.Session.getConnectedUser() != null) ? Entities.Session.getConnectedUser().getIdUser()
                    : 1;
            List<Evenement> list = evenementService.getRecommendedEvents(userId);
            for (Evenement ev : list) {
                clientCardsContainer.getChildren().add(createEventCard(ev));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleReservation(Evenement ev) {
        // --- 1. Check if user already has a reservation for this event ---
        int currentUserId = (Entities.Session.getConnectedUser() != null)
                ? Entities.Session.getConnectedUser().getIdUser()
                : 1;
        ReservationEvenement existingRes = null;
        try {
            List<ReservationEvenement> userRes = reservationService.getByUser(currentUserId);
            existingRes = userRes.stream()
                    .filter(r -> r.getIdEvenement() == ev.getIdEvenement())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (existingRes != null) {
            handleReservationUpdate(existingRes, ev);
            return;
        }

        openReservationPopup(ev, null);
    }

    private void handleReservationUpdate(ReservationEvenement existing, Evenement ev) {
        boolean confirmed = DialogUtils.showConfirmation(
                "Réservation existante",
                "Vous avez déjà une réservation pour '" + ev.getTitre() + "'.\n" +
                        "Voulez-vous modifier le nombre de billets de votre réservation actuelle ?");

        if (confirmed) {
            openReservationPopup(ev, existing);
        }
    }

    private void openReservationPopup(Evenement ev, ReservationEvenement existing) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #064e3b; -fx-border-width: 2; -fx-border-radius: 20;");

        Label icon = new Label("🎟️");
        icon.setStyle("-fx-font-size: 40px;");

        Label title = new Label(existing == null ? "Réserver votre place" : "Modifier ma réservation");
        title.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif; -fx-font-weight: 900; -fx-font-size: 22px; -fx-text-fill: #1e293b;");

        Label subTitle = new Label(ev.getTitre());
        subTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");

        VBox inputGroup = new VBox(8);
        inputGroup.setAlignment(Pos.CENTER_LEFT);
        Label labelSpinner = new Label("Nombre de billets :");
        labelSpinner.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        int max = ev.getNbPlaces() + (existing != null ? existing.getNbBillets() : 0);
        Spinner<Integer> spinner = new Spinner<>(1, max, (existing != null ? existing.getNbBillets() : 1));
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;");

        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);

        Button btnConfirm = new Button("Confirmer");
        btnConfirm.setStyle(
                "-fx-background-color: #064e3b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");

        btnCancel.setOnAction(e -> popupStage.close());

        btnConfirm.setOnAction(e -> {
            try {
                if (existing == null) {
                    ReservationEvenement newRes = new ReservationEvenement();
                    newRes.setIdEvenement(ev.getIdEvenement());
                    newRes.setNbBillets(spinner.getValue());
                    newRes.setStatutRes(StatutReservation.CONFIRMEE);
                    newRes.setDateReservation(LocalDateTime.now());
                    newRes.setIdUser(Entities.Session.getConnectedUser() != null
                            ? Entities.Session.getConnectedUser().getIdUser()
                            : 1);
                    reservationService.add(newRes);
                    ev.setNbPlaces(ev.getNbPlaces() - spinner.getValue());
                } else {
                    int diff = spinner.getValue() - existing.getNbBillets();
                    existing.setNbBillets(spinner.getValue());
                    reservationService.update(existing);
                    ev.setNbPlaces(ev.getNbPlaces() - diff);
                }

                evenementService.update(ev);
                popupStage.close();
                refreshEvents();
                showCustomAlert("Succès", "Votre opération a été effectuée !");
            } catch (Exception ex) {
                ex.printStackTrace();
                showCustomAlert("Erreur", "Une erreur est survenue lors de l'opération.");
            }
        });

        buttons.getChildren().addAll(btnCancel, btnConfirm);
        inputGroup.getChildren().addAll(labelSpinner, spinner);
        root.getChildren().addAll(icon, title, subTitle, inputGroup, buttons);

        Scene scene = new Scene(root);
        scene.setFill(null);
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private VBox createEventCard(Evenement ev) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 8);");

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

        boolean isTermine_ou_Plein = "TERMINE".equalsIgnoreCase(ev.getStatut()) || ev.getNbPlaces() <= 0;

        // --- Rating Social Proof ---
        try {
            ReservationEvenementService rs = new ReservationEvenementService();
            java.util.Map<Integer, Double> avgMap = rs.getAverageRatingsMap();
            java.util.Map<Integer, Integer> countMap = rs.getReviewCountMap();

            if (avgMap.containsKey(ev.getIdEvenement())) {
                double avg = avgMap.get(ev.getIdEvenement());
                int count = countMap.get(ev.getIdEvenement());
                Label ratingLabel = new Label(String.format("⭐ %.1f (%d avis)", avg, count));
                ratingLabel.setStyle(
                        "-fx-text-fill: #ca8a04; -fx-font-weight: 900; -fx-font-size: 11px; -fx-background-color: #fefce8; -fx-padding: 3 8; -fx-background-radius: 8;");
                titleArea.getChildren().add(ratingLabel);
            }
        } catch (Exception ex) {
        }

        Label statusBadge = new Label(isTermine_ou_Plein ? "INDISPONIBLE" : "DISPONIBLE");
        statusBadge.setStyle(isTermine_ou_Plein
                ? "-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 10px;"
                : "-fx-background-color: #ecfdf5; -fx-text-fill: #10b981; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 10px;");

        String priceText = (ev.getPrix() <= 0) ? "GRATUIT" : ev.getPrix() + " DT";
        Label priceBadge = new Label(priceText);
        priceBadge.setStyle(
                "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 10px; -fx-border-color: #3b82f6; -fx-border-radius: 15; -fx-border-width: 1;");

        HBox badges = new HBox(8, priceBadge, statusBadge);
        badges.setAlignment(Pos.CENTER_RIGHT);

        // --- Weather Badge ---
        HBox weatherBox = new HBox(8);
        weatherBox.setAlignment(Pos.CENTER_LEFT);
        weatherBox.setStyle(
                "-fx-background-color: #f8fafc; -fx-padding: 6 12; -fx-background-radius: 12; -fx-border-color: #e2e8f0;");

        ImageView weatherIcon = new ImageView();
        weatherIcon.setFitWidth(24);
        weatherIcon.setFitHeight(24);

        Label weatherLabel = new Label("Chargement...");
        weatherLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 11px; -fx-font-weight: 800;");
        weatherBox.getChildren().addAll(weatherIcon, weatherLabel);

        updateWeather(ev.getLieu(), weatherIcon, weatherLabel);

        header.getChildren().addAll(titleArea, spacer, weatherBox, badges);

        // --- Pro Recommendation System (Stars & Match %) ---
        if (ev.getRelevanceScore() > 10) {
            VBox recoBox = new VBox(2);
            recoBox.setAlignment(Pos.CENTER);
            recoBox.setPadding(new Insets(0, 15, 0, 15));

            HBox stars = new HBox(2);
            stars.setAlignment(Pos.CENTER);
            int starCount = 1;
            if (ev.getRelevanceScore() > 85)
                starCount = 5;
            else if (ev.getRelevanceScore() > 60)
                starCount = 4;
            else if (ev.getRelevanceScore() > 35)
                starCount = 3;
            else if (ev.getRelevanceScore() > 20)
                starCount = 2;

            for (int i = 0; i < 5; i++) {
                Label star = new Label(i < starCount ? "⭐" : "☆");
                star.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (i < starCount ? "#FFD700" : "#cbd5e1") + ";");
                stars.getChildren().add(star);
            }

            Label matchLabel = new Label(String.format("%.0f%% Match", Math.min(100, ev.getRelevanceScore())));
            matchLabel.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #d97706; -fx-background-color: #fffbeb; -fx-padding: 2 6; -fx-background-radius: 5;");

            recoBox.getChildren().addAll(stars, matchLabel);
            header.getChildren().add(1, recoBox);
        }

        HBox stats = new HBox(20);
        stats.setStyle("-fx-padding: 10 0;");
        Label dateInfo = new Label(
                "📅 " + (ev.getDateEvent() != null ? ev.getDateEvent().toString().split("T")[0] : "À venir"));
        Label slotsInfo = new Label("👥 " + ev.getNbPlaces() + " places restantes");
        dateInfo.setStyle("-fx-text-fill: #475569; -fx-font-weight: 600;");
        slotsInfo.setStyle("-fx-text-fill: #475569; -fx-font-weight: 600;");
        stats.getChildren().addAll(dateInfo, slotsInfo);

        Button btnReserver = new Button("🎫 Réserver maintenant");
        btnReserver.setMaxWidth(Double.MAX_VALUE);
        btnReserver.setStyle(
                "-fx-background-color: linear-gradient(to right, #064e3b, #059669); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12; -fx-cursor: hand;");

        if (isTermine_ou_Plein) {
            btnReserver.setDisable(true);
            btnReserver.setText("❌ Événement Terminé/Plein");
            btnReserver.setStyle(
                    "-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-background-radius: 12; -fx-padding: 12; -fx-font-weight: bold;");
        }

        btnReserver.setOnAction(e -> handleReservation(ev));

        card.getChildren().addAll(header, stats, new Separator(), btnReserver);

        // Hover Effect Pro
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(6, 78, 59, 0.15), 25, 0, 0, 10); " +
                    "-fx-translate-y: -5; -fx-border-color: #064e3b; -fx-border-width: 1; -fx-border-radius: 20;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 8); " +
                    "-fx-translate-y: 0; -fx-border-color: transparent;");
        });

        return card;
    }

    private void showCustomAlert(String title, String content) {
        if (title.equalsIgnoreCase("Erreur")) {
            DialogUtils.showError(title, content);
        } else {
            DialogUtils.showInfo(title, content);
        }
    }

    @FXML
    private void onSearch() {
        String filter = searchField.getText().toLowerCase();
        clientCardsContainer.getChildren().clear();
        try {
            int userId = (Entities.Session.getConnectedUser() != null) ? Entities.Session.getConnectedUser().getIdUser()
                    : 1;
            List<Evenement> list = evenementService.getRecommendedEvents(userId);
            list.stream()
                    .filter(e -> e.getTitre().toLowerCase().contains(filter)
                            || e.getLieu().toLowerCase().contains(filter))
                    .forEach(e -> clientCardsContainer.getChildren().add(createEventCard(e)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWeather(String city, ImageView iconView, Label tempLabel) {
        if (city == null || city.isEmpty())
            return;
        HttpClient.newHttpClient().sendAsync(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openweathermap.org/data/2.5/weather?q="
                                + city.split(",")[0].trim().replace(" ", "%20") + "&units=metric&appid="
                                + WEATHER_API_KEY))
                        .build(),
                HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getInt("cod") == 200) {
                            double t = json.getJSONObject("main").getDouble("temp");
                            String icon = json.getJSONArray("weather").getJSONObject(0).getString("icon");
                            String iconUrl = "https://openweathermap.org/img/wn/" + icon + "@2x.png";
                            Platform.runLater(() -> {
                                tempLabel.setText(Math.round(t) + "°C");
                                iconView.setImage(new Image(iconUrl));
                            });
                        } else {
                            Platform.runLater(() -> tempLabel.setText("N/A"));
                        }
                    } catch (Exception ignored) {
                        Platform.runLater(() -> tempLabel.setText("N/A"));
                    }
                });
    }
}