package GUI;

import Entities.ReservationSeance;
import Entities.Seance;
import Services.ReservationSeanceService;
import enums.StatutPresence;
import javafx.animation.FadeTransition;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import GUI.utils.DialogUtils;
import java.time.LocalDate;
import java.util.List;
import Entities.Session;
import Entities.UserApp;
import enums.RoleUser;

public class PresenceController {

    @FXML private Label lblSeanceTitle;
    @FXML private Label lblStats;
    @FXML private Label lblTaux;
    @FXML private TableColumn<ReservationSeance, Void> colAction;
    @FXML private TableView<ReservationSeance> tablePresence;
    @FXML private TableColumn<ReservationSeance, String> colNom;
    @FXML private TableColumn<ReservationSeance, StatutPresence> colStatut;

    private Seance currentSeance;
    private final ReservationSeanceService service = new ReservationSeanceService();
    private final ObservableList<ReservationSeance> data = FXCollections.observableArrayList();

    // ================= SET SEANCE =================
    public void setSeance(Seance s) {

        this.currentSeance = s;

        lblSeanceTitle.setText("Appel de présence - " + s.getNom());

        if (s.getDateSeance().isAfter(LocalDate.now())) {
            lblStats.setText("Séance non commencée");
            lblTaux.setText("Appel indisponible");
            tablePresence.setDisable(true);
        }

        loadData();
    }

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        UserApp connectedUser = Session.getConnectedUser();

        if (connectedUser == null) {
            DialogUtils.showError("Erreur", "Utilisateur non connecté.");
            closeWindow();
            return;
        }

        if (connectedUser.getRole() != RoleUser.COACH) {
            DialogUtils.showError("Accès refusé", "Cette page est réservée aux coachs.");
            closeWindow();
            return;
        }

        tablePresence.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablePresence.setPlaceholder(new Label("Aucun participant inscrit"));

        configureRowStyle();
    }

    private void closeWindow() {
        Stage stage = (Stage) tablePresence.getScene().getWindow();
        stage.close();
    }

    // ================= ROW STYLE =================
    private void configureRowStyle() {

        tablePresence.setRowFactory(tv -> new TableRow<>() {

            @Override
            protected void updateItem(ReservationSeance item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().removeAll("row-present", "row-absent", "row-nonmarque");

                if (item == null || empty) return;

                switch (item.getStatutPresence()) {
                    case PRESENT -> getStyleClass().add("row-present");
                    case ABSENT -> getStyleClass().add("row-absent");
                    case NON_MARQUE -> getStyleClass().add("row-nonmarque");
                }
            }
        });
    }

    // ================= LOAD DATA =================
    private void loadData() {

        List<ReservationSeance> list =
                service.getReservationsBySeance(currentSeance.getIdSeance());

        data.setAll(list);

        for (ReservationSeance r : data) {
            if (r.getStatutPresence() == null) {
                r.setStatutPresence(StatutPresence.NON_MARQUE);
            }
        }

        tablePresence.setItems(data);

        configureUserColumn();
        configureStatutColumn();
        configureActionColumn();

        updateStats();
    }

    // ================= USER COLUMN (Avatar + Nom + Email) =================
    private void configureUserColumn() {

        colNom.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(""));

        colNom.setCellFactory(col -> new TableCell<>() {

            private final ImageView avatar = new ImageView();
            private final Label name = new Label();
            private final Label email = new Label();
            private final VBox userInfo = new VBox(2);
            private final HBox container = new HBox(12);

            {
                avatar.setFitWidth(40);
                avatar.setFitHeight(40);

                Circle clip = new Circle(20, 20, 20);
                avatar.setClip(clip);

                avatar.getStyleClass().add("avatar-image");
                name.getStyleClass().add("user-name");
                email.getStyleClass().add("user-email");

                userInfo.getChildren().addAll(name, email);
                container.getChildren().addAll(avatar, userInfo);
                container.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() >= data.size()) {
                    setGraphic(null);
                    return;
                }

                ReservationSeance r = data.get(getIndex());

                if (r == null || r.getUser() == null) {
                    setGraphic(null);
                    return;
                }

                UserApp user = r.getUser();

                name.setText(user.getNom());
                email.setText(user.getEmail());

                try {
                    if (user.getImage_url() != null && !user.getImage_url().isEmpty()) {
                        avatar.setImage(new Image(user.getImage_url(), true));
                    } else {
                        avatar.setImage(new Image(
                                getClass().getResource("/gui/default-user.png").toExternalForm()
                        ));
                    }
                } catch (Exception e) {
                    avatar.setImage(new Image(
                            getClass().getResource("/gui/default-user.png").toExternalForm()
                    ));
                }

                setGraphic(container);
            }
        });
    }

    // ================= STATUT COLUMN =================
    private void configureStatutColumn() {

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutPresence"));

        colStatut.setCellFactory(col -> new TableCell<>() {

            private final Label badge = new Label();

            {
                badge.getStyleClass().add("status-badge");
            }

            @Override
            protected void updateItem(StatutPresence statut, boolean empty) {
                super.updateItem(statut, empty);

                if (empty || statut == null) {
                    setGraphic(null);
                    return;
                }

                badge.getStyleClass().removeAll(
                        "badge-present", "badge-absent", "badge-neutral");

                switch (statut) {
                    case PRESENT -> {
                        badge.setText("✔ Présent");
                        badge.getStyleClass().add("badge-present");
                    }
                    case ABSENT -> {
                        badge.setText("✖ Absent");
                        badge.getStyleClass().add("badge-absent");
                    }
                    case NON_MARQUE -> {
                        badge.setText("• Non marqué");
                        badge.getStyleClass().add("badge-neutral");
                    }
                }

                setGraphic(badge);
            }
        });
    }

    // ================= ACTION COLUMN =================
    private void configureActionColumn() {

        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button btnPresent = new Button("✔");
            private final Button btnAbsent = new Button("✖");
            private final HBox pane = new HBox(8, btnPresent, btnAbsent);

            {
                btnPresent.getStyleClass().add("btn-present");
                btnAbsent.getStyleClass().add("btn-absent");
                pane.setAlignment(Pos.CENTER);

                btnPresent.setOnAction(e -> {
                    ReservationSeance r = data.get(getIndex());
                    updatePresence(r, StatutPresence.PRESENT);
                });

                btnAbsent.setOnAction(e -> {
                    ReservationSeance r = data.get(getIndex());
                    updatePresence(r, StatutPresence.ABSENT);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() >= data.size()) {
                    setGraphic(null);
                    return;
                }

                ReservationSeance r = data.get(getIndex());

                btnPresent.setDisable(r.getStatutPresence() == StatutPresence.PRESENT);
                btnAbsent.setDisable(r.getStatutPresence() == StatutPresence.ABSENT);

                setGraphic(pane);
            }
        });
    }

    // ================= UPDATE STATS =================
    private void updateStats() {

        long present = data.stream()
                .filter(r -> r.getStatutPresence() == StatutPresence.PRESENT)
                .count();

        long absent = data.stream()
                .filter(r -> r.getStatutPresence() == StatutPresence.ABSENT)
                .count();

        long total = data.size();

        double taux = total == 0 ? 0 :
                (double) present / total * 100;

        lblStats.setText("Présents: " + present +
                " | Absents: " + absent +
                " | Total: " + total);

        lblTaux.setText("Taux de présence: " +
                String.format("%.1f %%", taux));
    }

    // ================= UPDATE PRESENCE =================
    private void updatePresence(ReservationSeance r, StatutPresence statut) {

        try {
            r.setStatutPresence(statut);
            service.updatePresence(r.getIdReservation(), statut);

            tablePresence.refresh();
            updateStats();
            animateSingleRow(r);

        } catch (Exception e) {
            DialogUtils.showError("Erreur",
                    "Impossible de mettre à jour la présence.");
        }
    }

    private void animateSingleRow(ReservationSeance r) {

        tablePresence.lookupAll(".table-row-cell").forEach(node -> {

            TableRow<ReservationSeance> row =
                    (TableRow<ReservationSeance>) node;

            if (row.getItem() == r) {

                FadeTransition ft =
                        new FadeTransition(Duration.millis(250), row);

                ft.setFromValue(0.5);
                ft.setToValue(1);
                ft.play();
            }
        });
    }

    // ================= BUTTONS =================
    @FXML
    private void handleSave() {

        for (ReservationSeance r : data) {
            service.updatePresence(
                    r.getIdReservation(),
                    r.getStatutPresence()
            );
        }

        updateStats();
        DialogUtils.showInfo("Succès",
                "Présences sauvegardées avec succès.");
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    @FXML
    private void handleCloturer() {

        boolean confirmed = DialogUtils.showConfirmation(
                "Clôturer séance",
                "Clôturer définitivement cette séance ?");

        if (confirmed) {
            tablePresence.setDisable(true);
            DialogUtils.showInfo("Séance clôturée",
                    "La séance a été clôturée avec succès.");
        }
    }
}