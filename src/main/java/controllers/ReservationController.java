package controllers;

import Models.Reservation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReservationController {

    // ===== TABLE =====
    @FXML private TableView<Reservation> tableReservation;

    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, Integer> colNb;

    @FXML private TableColumn<Reservation, String> colToken;
    @FXML private TableColumn<Reservation, String> colGeneratedAt;
    @FXML private TableColumn<Reservation, String> colCheckedIn;
    @FXML private TableColumn<Reservation, String> colCheckinTime;

    // ===== KPI (optional in FXML) =====
    @FXML private Label lblTotal;
    @FXML private Label lblTickets;
    @FXML private Label lblChecked;

    // ===== DB =====
    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    private final ObservableList<Reservation> list = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {

        // ===== VALUE FACTORIES (ONLY requested columns) =====
        colStatut.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(nvl(data.getValue().getStatut(), "-")));

        colNb.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getNbPersonnes()).asObject());

        colToken.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(nvl(data.getValue().getTicketToken(), "-")));

        colGeneratedAt.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(formatDT(data.getValue().getTicketGeneratedAt())));

        colCheckedIn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().isCheckedIn() ? "OUI" : "NON"));

        colCheckinTime.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(formatDT(data.getValue().getCheckinTime())));

        // badge style for checked_in (optional, needs CSS classes pill-ok/pill-no)
        colCheckedIn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().removeAll("pill-ok", "pill-no");

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(item);
                if ("OUI".equalsIgnoreCase(item)) getStyleClass().add("pill-ok");
                else getStyleClass().add("pill-no");
            }
        });

        loadData();
    }

    // =========================================================
    // LOAD DATA (keeps id_res_act + id_user + id_activite hidden)
    // =========================================================
    private void loadData() {
        list.clear();

        int total = 0;
        int tickets = 0;
        int checked = 0;

        String sql =
                "SELECT id_res_act, statut_res, nb_personnes, id_user, id_activite, " +
                        "       ticket_token, ticket_generated_at, checked_in, checkin_time " +
                        "FROM reservation_activite " +
                        "ORDER BY id_res_act DESC";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Reservation r = new Reservation();

                // Hidden but REQUIRED for edit/delete
                r.setId(rs.getInt("id_res_act"));
                r.setIdUser(rs.getInt("id_user"));
                r.setIdActivite(rs.getInt("id_activite"));

                // Visible columns
                r.setStatut(rs.getString("statut_res"));
                r.setNbPersonnes(rs.getInt("nb_personnes"));
                r.setTicketToken(rs.getString("ticket_token"));

                Timestamp tGen = rs.getTimestamp("ticket_generated_at");
                r.setTicketGeneratedAt(tGen != null ? tGen.toLocalDateTime() : null);

                r.setCheckedIn(rs.getInt("checked_in") == 1);

                Timestamp tChk = rs.getTimestamp("checkin_time");
                r.setCheckinTime(tChk != null ? tChk.toLocalDateTime() : null);

                list.add(r);

                total++;
                if (r.getTicketToken() != null && !r.getTicketToken().isBlank()) tickets++;
                if (r.isCheckedIn()) checked++;
            }

            tableReservation.setItems(list);

            // KPI update (if labels exist in your FXML)
            if (lblTotal != null) lblTotal.setText(String.valueOf(total));
            if (lblTickets != null) lblTickets.setText(String.valueOf(tickets));
            if (lblChecked != null) lblChecked.setText(String.valueOf(checked));

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur chargement réservations: " + e.getMessage()).show();
        }
    }

    // =========================================================
    // REFRESH (button)
    // =========================================================
    @FXML
    public void refreshTable() {
        loadData();
    }

    // =========================================================
    // DELETE
    // =========================================================
    @FXML
    private void supprimerReservation() {

        Reservation selected = tableReservation.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez une réservation").show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette réservation ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        String sql = "DELETE FROM reservation_activite WHERE id_res_act = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, selected.getId());
            pst.executeUpdate();

            loadData();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur suppression: " + e.getMessage()).show();
        }
    }

    // =========================================================
    // EDIT POPUP
    // =========================================================
    @FXML
    private void modifierReservation() {

        Reservation selected = tableReservation.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez une réservation").show();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/EditReservationPopup.fxml"));
            Parent root = loader.load();

            EditReservationPopupController controller = loader.getController();
            controller.setReservation(selected);
            controller.setParentController(this); // to refresh after save

            Stage dialogStage = new Stage();
            dialogStage.initOwner(tableReservation.getScene().getWindow());
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.setResizable(false);
            dialogStage.setTitle("Modifier Réservation");
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Erreur ouverture popup:\n" + e.getClass().getSimpleName() + "\n" + e.getMessage()
            ).show();
        }
    }

    // Called from popup after update
    public void refreshAfterEdit() {
        loadData();
    }

    // =========================================================
    // NAVIGATION
    // =========================================================
    @FXML
    private void goActivites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/AdminActivites.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tableReservation.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Activités");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la page Activités !").show();
        }
    }

    @FXML
    private void goToUserSeances(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/MainLayout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private String formatDT(LocalDateTime dt) {
        if (dt == null) return "-";
        return dt.format(dtf);
    }

    private String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}