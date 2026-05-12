package controllers;

import Entities.Evenement;
import Services.EvenementService;
import GUI.utils.DialogUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EvenementController {

    @FXML private TextField searchField;
    @FXML private VBox cardsContainer;
    @FXML private Label lblInfo;

    private final EvenementService evenementService = new EvenementService();
    private List<Evenement> allEvents;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);

    @FXML
    public void initialize() {
        loadEvents();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> onSearch());
        }
    }

    public void loadEvents() {
        try {
            allEvents = evenementService.getAll();
            renderCards(allEvents);
            updateTotal(allEvents == null ? 0 : allEvents.size());
        } catch (Exception e) {
            renderCards(List.of());
            updateTotal(0);
        }
    }

    private void renderCards(List<Evenement> list) {
        cardsContainer.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label empty = new Label("Aucun événement trouvé.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16;");
            cardsContainer.getChildren().add(empty);
            return;
        }
        for (Evenement e : list) {
            cardsContainer.getChildren().add(buildCard(e));
        }
    }

    private HBox buildCard(Evenement e) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(25));
        card.setMaxWidth(Double.MAX_VALUE); // Permet à la carte de s'étirer en largeur

        String baseStyle = "-fx-background-color: white; -fx-background-radius: 18; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 15, 0, 0, 10);";
        card.setStyle(baseStyle);

        // --- Thumbnail ---
        if (e.getImageUrl() != null && !e.getImageUrl().isEmpty()) {
            try {
                java.io.File file = new java.io.File("src/main/resources/uploads/events/" + e.getImageUrl());
                if (file.exists()) {
                    javafx.scene.image.ImageView thumb = new javafx.scene.image.ImageView(new javafx.scene.image.Image(file.toURI().toString()));
                    thumb.setFitWidth(80);
                    thumb.setFitHeight(80);
                    thumb.setPreserveRatio(true);
                    
                    StackPane thumbContainer = new StackPane(thumb);
                    thumbContainer.setPrefSize(80, 80);
                    thumbContainer.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 12;");
                    card.getChildren().add(thumbContainer);
                }
            } catch (Exception ignored) {}
        }

        // --- Bloc Texte (Info) ---
        VBox infoBox = new VBox(8);
        HBox.setHgrow(infoBox, Priority.ALWAYS); // Très important : pousse les autres blocs vers la droite

        // Statut
        boolean isTermine = "TERMINE".equalsIgnoreCase(e.getStatut());
        Label statusBadge = new Label(isTermine ? "ÉVÉNEMENT PASSÉ" : "ACTIF");
        statusBadge.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 10; -fx-font-weight: 900;",
                isTermine ? "#f1f5f9" : "#dcfce7", isTermine ? "#64748b" : "#10b981"
        ));

        // Titre avec retour à la ligne
        Label title = new Label(e.getTitre());
        title.setWrapText(true); // Empêche le texte d'être coupé par des "..."
        title.setMaxWidth(500);  // Largeur raisonnable avant de passer à la ligne
        title.setStyle("-fx-font-size: 20; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label meta = new Label("📅 " + formatDate(e.getDateEvent()) + "   📍 " + e.getLieu());
        meta.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-font-weight: 600;");

        infoBox.getChildren().addAll(statusBadge, title, meta);

        // --- Bloc Places (Chiffre) ---
        VBox capBox = new VBox(2);
        capBox.setAlignment(Pos.CENTER);
        capBox.setMinWidth(100);
        Label nbPlaces = new Label(String.valueOf(e.getNbPlaces()));
        nbPlaces.setStyle("-fx-font-size: 26; -fx-font-weight: 900; -fx-text-fill: #143D30;");
        Label capText = new Label("PLACES");
        capText.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8; -fx-font-weight: 900;");
        capBox.getChildren().addAll(nbPlaces, capText);

        // --- Bloc Actions ---
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-background-radius: 8; -fx-border-radius: 8; -fx-font-weight: bold; -fx-padding: 8 15; -fx-cursor: hand;");
        if (isTermine) btnEdit.setDisable(true);
        btnEdit.setOnAction(ev -> openForm(e));

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #e11d48; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand;");
        btnDelete.setOnAction(ev -> deleteEvent(e));

        actions.getChildren().addAll(btnEdit, btnDelete);

        // Hover Effect
        card.setOnMouseEntered(ev -> card.setStyle(baseStyle + "-fx-border-color: #143D30;"));
        card.setOnMouseExited(ev -> card.setStyle(baseStyle));

        card.getChildren().addAll(infoBox, capBox, actions);
        return card;
    }

    private void deleteEvent(Evenement e) {
        if (DialogUtils.showConfirmation("Suppression", "Voulez-vous supprimer " + e.getTitre() + " ?")) {
            try {
                evenementService.delete(e.getIdEvenement());
                loadEvents();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void openForm(Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/evenement_form.fxml"));
            Parent root = loader.load();
            EvenementFormController ctrl = loader.getController();
            if (e != null) ctrl.setData(e);
            ctrl.setOnSaved(this::loadEvents);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML private void onAdd() { openForm(null); }

    private void onSearch() {
        if (allEvents == null) return;
        String q = searchField.getText().toLowerCase();
        List<Evenement> filtered = allEvents.stream()
                .filter(ev -> ev.getTitre().toLowerCase().contains(q) || ev.getLieu().toLowerCase().contains(q))
                .collect(Collectors.toList());
        renderCards(filtered);
    }

    private void updateTotal(int total) {
        if (lblInfo != null) lblInfo.setText(total + " événements au catalogue");
    }

    private String formatDate(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DT_FMT);
    }
}