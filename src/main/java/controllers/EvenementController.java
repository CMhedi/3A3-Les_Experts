package controllers;

import Entities.Evenement;
import Services.EvenementService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
    }

    // ===== sidebar / topbar actions =====

    @FXML
    public void loadEvents() {
        try {
            allEvents = evenementService.getAll(); // adapte si besoin
            renderCards(allEvents);
            updateTotal(allEvents == null ? 0 : allEvents.size());
        } catch (Exception e) {
            renderCards(List.of());
            updateTotal(0);
            System.out.println("Erreur chargement : " + e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        if (searchField != null) searchField.clear();
        loadEvents();
    }

    @FXML
    private void onSearch() {
        if (allEvents == null) {
            loadEvents();
            return;
        }

        String q = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim();
        if (q.isEmpty()) {
            renderCards(allEvents);
            updateTotal(allEvents.size());
            return;
        }

        String qq = q.toLowerCase(Locale.ROOT);

        List<Evenement> filtered = allEvents.stream()
                .filter(ev ->
                        contains(ev.getTitre(), qq) ||
                                contains(ev.getLieu(), qq) ||
                                contains(ev.getStatut(), qq) ||
                                (ev.getCategorieEvt() != null && ev.getCategorieEvt().name().toLowerCase(Locale.ROOT).contains(qq)) ||
                                formatDate(ev.getDateEvent()).toLowerCase(Locale.ROOT).contains(qq)
                )
                .collect(Collectors.toList());

        renderCards(filtered);
        updateTotal(filtered.size());
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void goHome() {
        System.out.println("goHome() à implémenter selon ta navigation.");
    }

    @FXML
    private void goToReservations() {
        System.out.println("goToReservations() à implémenter selon ta navigation.");
    }

    // ===== render cards =====

    private void renderCards(List<Evenement> list) {
        cardsContainer.getChildren().clear();

        if (list == null || list.isEmpty()) {
            cardsContainer.getChildren().add(emptyState("Aucun événement trouvé."));
            return;
        }

        for (Evenement e : list) {
            cardsContainer.getChildren().add(buildCard(e));
        }
    }

    private Node emptyState(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: 700; -fx-padding: 10;");
        return lbl;
    }

    private HBox buildCard(Evenement e) {

        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(0,0,0,0.06);" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(17,24,39,0.08), 18, 0.18, 0, 6);"
        );

        VBox left = new VBox(6);
        left.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(nvl(e.getTitre()));
        title.setStyle("-fx-font-size: 15; -fx-font-weight: 900; -fx-text-fill: #1f2937;");

        Label meta = new Label(metaLine(e));
        meta.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12; -fx-font-weight: 700;");

        Label desc = new Label(trimTo(nvl(e.getDescription()), 140));
        desc.setWrapText(true);
        desc.setMaxWidth(520);
        desc.setStyle("-fx-text-fill: #374151; -fx-font-size: 12;");

        left.getChildren().addAll(title, meta, desc);

        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox tags = new HBox(8);
        tags.setAlignment(Pos.CENTER_RIGHT);

        Label catTag = tag(e.getCategorieEvt() == null ? "" : e.getCategorieEvt().name(), "#eef2ff", "#1e3a8a");
        Label stTag = tag(nvl(e.getStatut()), "#e8f5ee", "#065f46");

        tags.getChildren().addAll(catTag, stTag);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: #f0f7ff; -fx-text-fill: #0984e3; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 7 12; -fx-cursor: hand;");
        btnEdit.setOnAction(ev -> openForm(e));

        Button btnDelete = new Button("Supprimer");
        btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: 900; -fx-background-radius: 10; -fx-padding: 7 12; -fx-cursor: hand;");
        btnDelete.setOnAction(ev -> deleteEvent(e));

        actions.getChildren().addAll(btnEdit, btnDelete);

        right.getChildren().addAll(tags, actions);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(left, spacer, right);

        return card;
    }

    private Label tag(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-font-weight: 900;" +
                        "-fx-padding: 5 10;" +
                        "-fx-background-radius: 999;"
        );
        return l;
    }

    private String metaLine(Evenement e) {
        String date = formatDate(e.getDateEvent());
        String lieu = nvl(e.getLieu());
        String places = "Places: " + e.getNbPlaces();
        return date + "  •  " + lieu + "  •  " + places;
    }

    private String formatDate(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DT_FMT);
    }

    // ===== delete =====

    private void deleteEvent(Evenement e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'événement ?");
        confirm.setContentText("Titre : " + nvl(e.getTitre()));

        ButtonType ok = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(ok, cancel);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ok) {
                try {
                    evenementService.delete(e.getIdEvenement()); // adapte si besoin
                    loadEvents();
                } catch (Exception ex) {
                    System.out.println("Erreur suppression : " + ex.getMessage());
                }
            }
        });
    }

    // ===== open modal form =====

    private void openForm(Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/evenement_form.fxml"));
            Parent root = loader.load();

            EvenementFormController ctrl = loader.getController();
            ctrl.setData(e);
            ctrl.setOnSaved(this::loadEvents);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(e == null ? "Nouvel Événement" : "Modifier Événement");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception ex) {
            System.out.println("Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    // ===== small helpers =====

    private void updateTotal(int total) {
        if (lblInfo != null) lblInfo.setText("Total: " + total);
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String trimTo(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}