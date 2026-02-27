package controllers;

import Entities.Evenement;
import Services.EvenementService;
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
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        loadEvents();
        // Listener interactive pour la recherche
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> onSearch());
        }
    }

    // ===== CORE LOGIC (Gestion des Evénements) =====

    @FXML
    public void loadEvents() {
        try {
            allEvents = evenementService.getAll();
            renderCards(allEvents);
            updateTotal(allEvents == null ? 0 : allEvents.size());
        } catch (Exception e) {
            renderCards(List.of());
            updateTotal(0);
            System.err.println("Erreur chargement : " + e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        if (searchField != null) searchField.clear();
        loadEvents();
    }

    @FXML
    private void onSearch() {
        if (allEvents == null) return;

        String q = (searchField == null) ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            renderCards(allEvents);
            updateTotal(allEvents.size());
            return;
        }

        List<Evenement> filtered = allEvents.stream()
                .filter(ev ->
                        contains(ev.getTitre(), q) ||
                                contains(ev.getLieu(), q) ||
                                (ev.getCategorieEvt() != null && ev.getCategorieEvt().name().toLowerCase().contains(q))
                )
                .collect(Collectors.toList());

        renderCards(filtered);
        updateTotal(filtered.size());
    }

    // ===== CARD RENDERING =====

    private void renderCards(List<Evenement> list) {
        cardsContainer.getChildren().clear();
        if (list == null || list.isEmpty()) {
            cardsContainer.getChildren().add(new Label("Aucun événement trouvé."));
            return;
        }
        for (Evenement e : list) {
            cardsContainer.getChildren().add(buildCard(e));
        }
    }

    private HBox buildCard(Evenement e) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 25, 15, 25));

        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #f1f5f9; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );

        // UI Hover
        card.setOnMouseEntered(ev -> card.setStyle(card.getStyle() + "-fx-border-color: #143D30; -fx-translate-y: -2;"));
        card.setOnMouseExited(ev -> card.setStyle(card.getStyle() + "-fx-border-color: #f1f5f9; -fx-translate-y: 0;"));

        VBox infoBox = new VBox(5);
        Label title = new Label(nvl(e.getTitre()));
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: #1E293B;");

        Label meta = new Label(formatDate(e.getDateEvent()) + " • " + nvl(e.getLieu()) + " • Places: " + e.getNbPlaces());
        meta.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");

        infoBox.getChildren().addAll(title, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: #f0f7ff; -fx-text-fill: #0984e3; -fx-font-weight: bold; -fx-background-radius: 8;");
        btnEdit.setOnAction(ev -> openForm(e));

        Button btnDelete = new Button("Supprimer");
        btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-background-radius: 8;");
        btnDelete.setOnAction(ev -> deleteEvent(e));

        actions.getChildren().addAll(btnEdit, btnDelete);
        card.getChildren().addAll(infoBox, spacer, actions);

        return card;
    }

    // ===== CRUD ACTIONS =====

    private void deleteEvent(Evenement e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'événement : " + e.getTitre() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    evenementService.delete(e.getIdEvenement());
                    loadEvents();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.").show();
                }
            }
        });
    }

    private void openForm(Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/evenement_form.fxml"));
            Parent root = loader.load();
            EvenementFormController ctrl = loader.getController();
            ctrl.setData(e);
            ctrl.setOnSaved(this::loadEvents);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle(e == null ? "Nouveau Événement" : "Modifier Événement");
            stage.showAndWait();
        } catch (Exception ex) {
            System.err.println("Erreur ouverture formulaire : " + ex.getMessage());
        }
    }

    @FXML private void onAdd() { openForm(null); }
    private void updateTotal(int total) { if (lblInfo != null) lblInfo.setText("Total: " + total); }
    private boolean contains(String v, String q) { return v != null && v.toLowerCase().contains(q); }
    private String nvl(String s) { return s == null ? "" : s; }
    private String formatDate(LocalDateTime dt) { return dt == null ? "" : dt.format(DT_FMT); }
}