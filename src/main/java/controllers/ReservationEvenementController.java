package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ReservationEvenementController {

    @FXML private TextField searchField;
    @FXML private Label lblInfo;
    @FXML private VBox cardsContainer;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", Locale.FRENCH);

    @FXML
    public void initialize() {
        // Chargement initial des données
        loadAll();
    }

    private void loadAll() {
        try {
            List<ReservationEvenement> list = service.getAll();
            displayCards(list);
            lblInfo.setText("Total: " + list.size());
        } catch (Exception e) {
            lblInfo.setText("Erreur chargement: " + e.getMessage());
        }
    }

    private void displayCards(List<ReservationEvenement> list) {
        cardsContainer.getChildren().clear();

        for (ReservationEvenement res : list) {
            // --- CONTENEUR DE LA CARTE ---
            HBox card = new HBox(20);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(15, 20, 15, 20));
            card.setStyle("-fx-background-color: white; " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: #eef0f2; " +
                    "-fx-border-width: 1; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 2);");

            // --- SECTION INFOS (ID MASQUÉ) ---
            VBox details = new VBox(4);

            // Date mise en avant
            Label dateLabel = new Label("📅 " + (res.getDateReservation() != null ? res.getDateReservation().format(dtf) : "Date inconnue"));
            dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

            // Détails billets et Event
            Label infoLabel = new Label("🎟 " + res.getNbBillets() + " Billets  •  Événement #" + res.getIdEvenement());
            infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

            details.getChildren().addAll(dateLabel, infoLabel);

            // --- BADGE DE STATUT ---
            Label statusBadge = new Label(res.getStatutRes() != null ? res.getStatutRes().name() : "N/A");
            String statusColor = "#95a5a6";
            if (res.getStatutRes() != null) {
                String status = res.getStatutRes().name();
                if (status.equals("CONFIRMEE")) statusColor = "#27ae60";
                else if (status.equals("ANNULEE")) statusColor = "#e74c3c";
            }
            statusBadge.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; " +
                    "-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");

            // --- ESPACEUR ---
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // --- BOUTONS D'ACTION ---
            Button editBtn = new Button("✏"); // Style icône uniquement ou texte court
            editBtn.setTooltip(new Tooltip("Modifier la réservation"));
            editBtn.setStyle("-fx-background-color: #f1f4f9; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 5;");
            editBtn.setPrefSize(35, 35);
            editBtn.setOnAction(e -> openPopup(res));

            Button deleteBtn = new Button("🗑");
            deleteBtn.setTooltip(new Tooltip("Supprimer la réservation"));
            deleteBtn.setStyle("-fx-background-color: #fff0f0; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 5;");
            deleteBtn.setPrefSize(35, 35);
            deleteBtn.setOnAction(e -> deleteAction(res));

            HBox actions = new HBox(10, editBtn, deleteBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);

            // --- ASSEMBLAGE ---
            card.getChildren().addAll(details, statusBadge, spacer, actions);
            cardsContainer.getChildren().add(card);
        }
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        try {
            List<ReservationEvenement> all = service.getAll();
            List<ReservationEvenement> filtered = all.stream()
                    .filter(r -> (r.getStatutRes() != null && r.getStatutRes().name().toLowerCase().contains(query))
                            || String.valueOf(r.getIdEvenement()).contains(query))
                    .collect(Collectors.toList());
            displayCards(filtered);
            lblInfo.setText("Résultats: " + filtered.size());
        } catch (Exception e) {
            lblInfo.setText("Erreur recherche");
        }
    }

    private void deleteAction(ReservationEvenement res) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Supprimer cette réservation ?");
        confirm.setContentText("Cette action est irréversible.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.delete(res.getIdResEvt());
                loadAll();
            } catch (Exception e) {
                lblInfo.setText("Erreur suppression");
            }
        }
    }

    private void openPopup(ReservationEvenement existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservation_form_popup.fxml"));
            Scene scene = new Scene(loader.load());
            ReservationEvenementFormController ctrl = loader.getController();
            ctrl.setData(existing);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(existing == null ? "Nouvelle réservation" : "Modifier");
            stage.setScene(scene);
            stage.showAndWait();

            if (ctrl.isSaved()) loadAll();
        } catch (Exception e) {
            lblInfo.setText("Erreur fenêtre");
        }
    }

    @FXML private void onRefresh() {
        searchField.clear();
        loadAll();
    }

    @FXML private void onAdd() {
        openPopup(null);
    }

    @FXML
    public void goHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Home.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            lblInfo.setText("Erreur retour accueil");
        }
    }
}