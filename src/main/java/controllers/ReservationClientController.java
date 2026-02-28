package controllers;

import Entities.Evenement;
import Entities.ReservationEvenement;
import Services.EvenementService;
import Services.ReservationEvenementService;
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReservationClientController implements Initializable {

    @FXML private VBox cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label lblInfo;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private final EvenementService evenementService = new EvenementService();
    private List<ReservationEvenement> userReservations;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserReservations();
    }

    private void loadUserReservations() {
        try {
            userReservations = service.getAll();
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    private HBox createTicketCard(ReservationEvenement res) {
        Evenement ev = null;
        try { ev = evenementService.getById(res.getIdEvenement()); } catch (Exception e) {}
        String eventTitle = (ev != null) ? ev.getTitre() : "Événement #" + res.getIdEvenement();

        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 25, 15, 25));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");

        // Info Event
        VBox info = new VBox(4);
        Label title = new Label(eventTitle);
        title.setStyle("-fx-font-weight: 800; -fx-font-size: 17px; -fx-text-fill: #1e293b;");
        Label date = new Label("📅 " + (res.getDateReservation() != null ? res.getDateReservation().toString().split("T")[0] : "N/A"));
        date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        info.getChildren().addAll(title, date);
        info.setPrefWidth(280);

        // Billet Badge (Azyen)
        VBox badge = new VBox(0);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(5, 15, 5, 15));
        badge.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 12; -fx-border-color: #dcfce7; -fx-border-radius: 12;");
        Label val = new Label(String.valueOf(res.getNbBillets()));
        val.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #166534;");
        Label unit = new Label("BILLETS");
        unit.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #166534; -fx-opacity: 0.7;");
        badge.getChildren().addAll(val, unit);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Buttons Modern
        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #475569; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
        final Evenement finalEv = ev;
        btnEdit.setOnAction(e -> openUpdatePopup(res, finalEv));

        Button btnDel = new Button("✕");
        btnDel.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-background-radius: 8; -fx-font-weight: 900; -fx-cursor: hand; -fx-min-width: 35;");
        btnDel.setOnAction(e -> handleDelete(res));

        card.getChildren().addAll(info, badge, spacer, btnEdit, btnDel);

        // Hover Effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-translate-y: -2; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-translate-y: -2; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 6);", "")));

        return card;
    }

    private void openUpdatePopup(ReservationEvenement res, Evenement ev) {
        Stage popup = new Stage(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #064e3b; -fx-border-radius: 20; -fx-border-width: 2;");

        Label t = new Label("Mise à jour des billets");
        t.setStyle("-fx-font-weight: 900; -fx-font-size: 20px; -fx-text-fill: #064e3b;");

        Spinner<Integer> spinner = new Spinner<>(1, (ev != null ? ev.getNbPlaces() + res.getNbBillets() : 100), res.getNbBillets());
        spinner.setMaxWidth(150);

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        Button cancel = new Button("Annuler");
        cancel.setOnAction(e -> popup.close());
        Button save = new Button("Enregistrer");
        save.setStyle("-fx-background-color: #064e3b; -fx-text-fill: white; -fx-font-weight: bold;");
        save.setOnAction(e -> {
            try {
                res.setNbBillets(spinner.getValue());
                service.update(res);
                popup.close();
                loadUserReservations();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        actions.getChildren().addAll(cancel, save);
        root.getChildren().addAll(t, new Label("Nombre de places pour cet événement :"), spinner, actions);

        popup.setScene(new Scene(root));
        popup.show();
    }

    private void handleDelete(ReservationEvenement res) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Annuler cette réservation ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try { service.delete(res.getIdResEvt()); loadUserReservations(); } catch (Exception e) {}
        }
    }

    @FXML
    private void onSearch() {
        if (userReservations == null) return;
        String filter = searchField.getText().toLowerCase();
        List<ReservationEvenement> filtered = userReservations.stream()
                .filter(r -> {
                    try {
                        Evenement ev = evenementService.getById(r.getIdEvenement());
                        return ev != null && ev.getTitre().toLowerCase().contains(filter);
                    } catch (Exception e) { return false; }
                }).collect(Collectors.toList());
        displayReservations(filtered);
    }

    @FXML private void onRefresh() { searchField.clear(); loadUserReservations(); }
}