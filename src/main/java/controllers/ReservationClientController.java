package controllers;

import Entities.Evenement;
import Entities.ReservationEvenement;
import Services.EvenementService;
import Services.ReservationEvenementService;
import GUI.utils.DialogUtils;
import javafx.application.Platform;
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
import java.nio.file.Path;
import java.nio.file.Paths;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReservationClientController implements Initializable {

    @FXML
    private VBox cardsContainer;
    @FXML
    private TextField searchField;
    @FXML
    private Label lblInfo;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private final EvenementService evenementService = new EvenementService();
    private List<ReservationEvenement> userReservations;

    // --- API KEYS ---
    private final String PDFSHIFT_KEY = "sk_d37f1a91b211235dd61ee19e3438b61e2e1ded0d";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserReservations();
    }

    private void loadUserReservations() {
        try {
            if (Entities.Session.getConnectedUser() == null) {
                System.err.println("Aucun utilisateur connecté !");
                return;
            }
            int idUser = Entities.Session.getConnectedUser().getIdUser();
            userReservations = service.getByUser(idUser);
            displayReservations(userReservations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayReservations(List<ReservationEvenement> list) {
        cardsContainer.getChildren().clear();
        try {
            if (list == null || list.isEmpty()) {
                lblInfo.setText("0 Billets actifs");
                Label empty = new Label("Aucune réservation trouvée.");
                empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 20;");
                cardsContainer.getChildren().add(empty);
                return;
            }
            lblInfo.setText(list.size() + " Billets actifs");
            for (ReservationEvenement res : list) {
                cardsContainer.getChildren().add(createTicketCard(res));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createRatingSection(ReservationEvenement res) {
        VBox section = new VBox(5);
        section.setPadding(new Insets(10, 0, 0, 0));

        HBox stars = new HBox(5);
        stars.setAlignment(Pos.CENTER_LEFT);

        boolean isTerminee = res.getStatutRes() == enums.StatutReservation.TERMINEE;

        if (isTerminee) {
            for (int i = 1; i <= 5; i++) {
                final int rating = i;
                Label star = new Label(res.getNote() >= i ? "⭐" : "☆");
                star.setStyle("-fx-font-size: 18px; -fx-cursor: hand; -fx-text-fill: "
                        + (res.getNote() >= i ? "#FFD700" : "#cbd5e1") + ";");

                star.setOnMouseClicked(e -> {
                    try {
                        service.updateNote(res.getIdResEvt(), rating);
                        res.setNote(rating);
                        // Refresh the UI or at least the stars
                        loadUserReservations();
                        showNotification("Merci pour votre évaluation ! ⭐ " + rating + "/5");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                stars.getChildren().add(star);
            }
            Label lbl = new Label("Évaluer l'expérience");
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #143D30; -fx-font-weight: bold;");
            section.getChildren().addAll(lbl, stars);
        } else {
            Label placeholder = new Label("🔒 Évaluation disponible après l'événement");
            placeholder.setStyle(
                    "-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-background-color: #f1f5f9; -fx-padding: 4 8; -fx-background-radius: 5;");
            section.getChildren().add(placeholder);
        }

        return section;
    }

    private HBox createTicketCard(ReservationEvenement res) {
        String eventTitle = (res.getNomEvenement() != null) ? res.getNomEvenement()
                : "Événement #" + res.getIdEvenement();

        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 30, 20, 30));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 20, 0, 0, 10); " +
                "-fx-border-color: #f1f5f9; -fx-border-width: 1; -fx-border-radius: 20; " +
                "-fx-border-color: #143D30; -fx-border-width: 0 0 0 8;");

        // 1. Info Event (Title & Category/Date)
        VBox info = new VBox(5);
        Label title = new Label(eventTitle);
        title.setStyle("-fx-font-weight: 900; -fx-font-size: 20px; -fx-text-fill: #143D30;");

        Label dateLabel = new Label("📅 Aventure pour le : "
                + (res.getDateReservation() != null ? res.getDateReservation().toString().split("T")[0] : "N/A"));
        dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 600;");

        Label priceLabel = new Label("💰 Total: " + String.format("%.2f", res.getPrixTotal()) + " DT");
        priceLabel.setStyle("-fx-text-fill: #143D30; -fx-font-size: 15px; -fx-font-weight: 800; -fx-padding: 5 0 0 0;");

        info.getChildren().addAll(title, dateLabel, priceLabel, createRatingSection(res));
        info.setPrefWidth(380);

        // 2. Billet Badge (Premium Green #143D30)
        VBox badge = new VBox(-2);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(12, 25, 12, 25));
        badge.setStyle("-fx-background-color: #143D30; -fx-background-radius: 15;");

        Label val = new Label(String.valueOf(res.getNbBillets()));
        val.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: white;");

        Label unit = new Label("BILLETS");
        unit.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: rgba(255,255,255,0.7);");
        badge.getChildren().addAll(val, unit);
        badge.setPrefWidth(110);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Actions Modernes
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnPDF = new Button("⬇ Billet");
        btnPDF.setStyle("-fx-background-color: #143D30; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 12; -fx-padding: 10 20; -fx-cursor: hand;");
        btnPDF.setOnAction(e -> generateTicketPDF(res));

        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #143D30; -fx-font-weight: bold; " +
                "-fx-background-radius: 12; -fx-padding: 10 20; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> openUpdatePopup(res));

        Button btnDel = new Button("✕");
        btnDel.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #e11d48; -fx-background-radius: 12; " +
                "-fx-font-weight: 900; -fx-cursor: hand; -fx-min-width: 45; -fx-min-height: 45;");
        btnDel.setOnAction(e -> handleDelete(res));

        actions.getChildren().addAll(btnPDF, btnEdit, btnDel);
        card.getChildren().addAll(info, badge, spacer, actions);

        // Hover Effect
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle()
                    + "-fx-translate-y: -4; -fx-border-color: #143D30; -fx-effect: dropshadow(three-pass-box, rgba(20, 61, 48, 0.1), 20, 0, 0, 10);");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace(
                    "-fx-translate-y: -4; -fx-border-color: #143D30; -fx-effect: dropshadow(three-pass-box, rgba(20, 61, 48, 0.1), 20, 0, 0, 10);",
                    "-fx-border-color: #f1f5f9;"));
        });

        return card;
    }

    private void openUpdatePopup(ReservationEvenement res) {
        // Simple Popup
        try {
            Evenement ev = evenementService.getById(res.getIdEvenement());

            Stage popup = new Stage(StageStyle.UNDECORATED);
            popup.initModality(Modality.APPLICATION_MODAL);

            VBox root = new VBox(25);
            root.setPadding(new Insets(40));
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: white; -fx-background-radius: 30; " +
                    "-fx-border-color: #143D30; -fx-border-radius: 30; -fx-border-width: 3; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 30, 0, 0, 15);");

            Label t = new Label("Modifier vos billets");
            t.setStyle("-fx-font-weight: 900; -fx-font-size: 24px; -fx-text-fill: #143D30;");

            Label sub = new Label(res.getNomEvenement());
            sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");

            Spinner<Integer> spinner = new Spinner<>(1, (ev != null ? ev.getNbPlaces() + res.getNbBillets() : 100),
                    res.getNbBillets());
            spinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            spinner.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            HBox actions = new HBox(20);
            actions.setAlignment(Pos.CENTER);
            Button cancel = new Button("Annuler");
            cancel.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 12; -fx-padding: 10 25; -fx-font-weight: bold;");
            cancel.setOnAction(e -> popup.close());

            Button save = new Button("Enregistrer");
            save.setStyle(
                    "-fx-background-color: #143D30; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 25; -fx-font-weight: bold;");
            save.setOnAction(e -> {
                try {
                    res.setNbBillets(spinner.getValue());
                    service.update(res);
                    popup.close();
                    loadUserReservations();
                    showNotification("Mise à jour réussie !");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            actions.getChildren().addAll(cancel, save);
            root.getChildren().addAll(t, sub, spinner, actions);

            popup.setScene(new Scene(root));
            popup.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String message) {
        // Use generic Pane to avoid ClassCastException (root might be BorderPane)
        Pane root = (Pane) cardsContainer.getScene().getRoot();

        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: #143D30; -fx-text-fill: white; -fx-padding: 15 30; " +
                "-fx-background-radius: 30; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        // Manual positioning for compatibility with all Pane types
        toast.setManaged(false);
        toast.layoutXProperty().bind(root.widthProperty().subtract(toast.widthProperty()).divide(2));
        toast.layoutYProperty().bind(root.heightProperty().subtract(toast.heightProperty()).subtract(50));

        root.getChildren().add(toast);

        javafx.animation.FadeTransition in = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500),
                toast);
        in.setFromValue(0);
        in.setToValue(1);

        javafx.animation.FadeTransition out = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500),
                toast);
        out.setFromValue(1);
        out.setToValue(0);
        out.setDelay(javafx.util.Duration.seconds(2));
        out.setOnFinished(e -> root.getChildren().remove(toast));

        in.play();
        out.play();
    }

    private void handleDelete(ReservationEvenement res) {
        boolean confirmed = DialogUtils.showConfirmation(
                "Annulation",
                "Annuler votre billet pour '" + res.getNomEvenement() + "' ?\n" +
                        "Cette action est irréversible.");

        if (confirmed) {
            try {
                service.delete(res.getIdResEvt());
                loadUserReservations();
                showNotification("Billet annulé avec succès.");
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtils.showError("Erreur", "❌ Impossible d'annuler le billet.");
            }
        }
    }

    @FXML
    private void onSearch() {
        if (userReservations == null)
            return;
        String filter = searchField.getText().toLowerCase();
        List<ReservationEvenement> filtered = userReservations.stream()
                .filter(r -> r.getNomEvenement() != null && r.getNomEvenement().toLowerCase().contains(filter))
                .collect(Collectors.toList());
        displayReservations(filtered);
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        loadUserReservations();
    }

    private void generateTicketPDF(ReservationEvenement res) {
        showNotification("Génération de votre billet Premium...");

        try {
            // ✅ Fix: URL Encoding pour le QR Code (bech ma yatlach dechet)
            String qrData = "🔖 BILLET ECOADVENTURE\n" +
                    "--------------------------\n" +
                    "ID: " + res.getIdResEvt() + "\n" +
                    "EVENT: " + res.getNomEvenement() + "\n" +
                    "CLIENT: " + res.getNomUser() + "\n" +
                    "BILLETS: " + res.getNbBillets() + "\n" +
                    "TOTAL: " + String.format("%.2f", res.getPrixTotal()) + " DT\n" +
                    "VALIDE: OUI";

            String encodedData = java.net.URLEncoder.encode(qrData, "UTF-8");
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encodedData;

            // 🎨 Design Ultra-Premium HTML/CSS (Ticket + Stub)
            String htmlTemplate = "<html><head><style>" +
                    "body { font-family: 'Inter', sans-serif; background: #e2e8f0; padding: 60px; }" +
                    ".container { max-width: 800px; margin: auto; display: flex; box-shadow: 0 30px 60px rgba(0,0,0,0.15); border-radius: 20px; overflow: hidden; }"
                    +
                    ".main { background: white; flex: 3; padding: 40px; position: relative; border-right: 2px dashed #e2e8f0; }"
                    +
                    ".stub { background: #143D30; flex: 1; padding: 30px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: white; }"
                    +
                    ".logo { font-size: 24px; font-weight: 900; color: #143D30; margin-bottom: 30px; letter-spacing: -1px; }"
                    +
                    ".event-name { font-size: 32px; font-weight: 900; color: #0f172a; margin-bottom: 25px; line-height: 1.1; }"
                    +
                    ".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; }" +
                    ".info-item label { color: #94a3b8; font-size: 11px; text-transform: uppercase; font-weight: 800; display: block; margin-bottom: 5px; }"
                    +
                    ".info-item span { color: #1e293b; font-size: 15px; font-weight: 700; }" +
                    ".price-tag { margin-top: 40px; padding: 15px 25px; background: #f0fdf4; border-radius: 12px; display: inline-block; border: 1px solid #bbf7d0; color: #143D30; font-weight: 900; font-size: 20px; }"
                    +
                    ".qr-stub { background: white; padding: 10px; border-radius: 10px; margin-bottom: 15px; }" +
                    ".stub-id { font-size: 10px; opacity: 0.7; font-weight: 600; text-transform: uppercase; }" +
                    ".decoration-circle { height: 30px; width: 30px; background: #e2e8f0; border-radius: 50%; position: absolute; right: -15px; }"
                    +
                    ".dec-top { top: -15px; }.dec-bottom { bottom: -15px; }" +
                    "</style></head><body>" +
                    "<div class='container'>" +
                    "  <div class='main'>" +
                    "    <div class='decoration-circle dec-top'></div>" +
                    "    <div class='decoration-circle dec-bottom'></div>" +
                    "    <div class='logo'>🍃 ECOADVENTURE</div>" +
                    "    <div class='event-name'>"
                    + (res.getNomEvenement() != null ? res.getNomEvenement() : "EXPÉDITION") + "</div>" +
                    "    <div class='info-grid'>" +
                    "      <div class='info-item'><label>Passager</label><span>" + res.getNomUser() + "</span></div>" +
                    "      <div class='info-item'><label>Date de l'aventure</label><span>"
                    + (res.getDateReservation() != null ? res.getDateReservation().toString().split("T")[0]
                            : "À déterminer")
                    + "</span></div>" +
                    "      <div class='info-item'><label>Places réservées</label><span>" + res.getNbBillets()
                    + " Billets</span></div>" +
                    "      <div class='info-item'><label>ID Billet</label><span>#EA" + res.getIdResEvt()
                    + "</span></div>" +
                    "    </div>" +
                    "    <div class='price-tag'>" + String.format("%.2f", res.getPrixTotal()) + " DT</div>" +
                    "  </div>" +
                    "  <div class='stub'>" +
                    "    <div class='qr-stub'><img src='" + qrUrl + "' width='120'></div>" +
                    "    <div class='stub-id'>Validateur d'entrée</div>" +
                    "    <p style='font-size: 12px; margin-top: 20px; text-align: center; opacity: 0.8;'>Présentez ce coupon à l'entrée du site EcoAdventure.</p>"
                    +
                    "  </div>" +
                    "</div>" +
                    "</body></html>";

            JSONObject json = new JSONObject();
            json.put("source", htmlTemplate);

            HttpClient.newHttpClient().sendAsync(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.pdfshift.io/v3/convert/pdf"))
                            .header("Authorization",
                                    "Basic " + java.util.Base64.getEncoder()
                                            .encodeToString(("api:" + PDFSHIFT_KEY).getBytes()))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray()).thenAccept(response -> {
                        try {
                            if (response.statusCode() == 200) {
                                Path path = Paths.get("Billet_" + res.getIdResEvt() + ".pdf");
                                java.nio.file.Files.write(path, response.body());
                                Platform.runLater(() -> {
                                    showNotification("✅ Billet Premium disponible !");
                                    try {
                                        java.awt.Desktop.getDesktop().open(path.toFile());
                                    } catch (Exception ignored) {
                                    }
                                });
                            } else {
                                System.err.println("API Error: " + response.body());
                                Platform.runLater(() -> showNotification("❌ Erreur de génération (API)"));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showNotification("❌ Erreur critique de design");
        }
    }
}
