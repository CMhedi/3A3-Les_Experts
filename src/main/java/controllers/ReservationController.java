package controllers;

import Models.Reservation;
import Utiles.MyDB;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReservationController {

    @FXML private TableView<Reservation> tableReservation;

    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, Integer> colNb;
    @FXML private TableColumn<Reservation, String> colDate;
    @FXML private TableColumn<Reservation, String> colVille;
    @FXML private TableColumn<Reservation, String> colActivite;
    @FXML private TableColumn<Reservation, String> colUser;

    @FXML private Label lblTotal;
    @FXML private Label lblTickets;
    @FXML private Label lblChecked;

    private final ObservableList<Reservation> list = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colStatut.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(nvl(data.getValue().getStatut(), "-")));

        colNb.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getNbPersonnes()).asObject());

        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(formatDT(data.getValue().getDateReservation())));

        colVille.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(nvl(data.getValue().getVilleUser(), "-")));

        colActivite.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(nvl(data.getValue().getActiviteNom(), "#" + data.getValue().getIdActivite())));

        colUser.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(nvl(data.getValue().getUserDisplay(), "#" + data.getValue().getIdUser())));

        loadData();
    }

    private void loadData() {
        list.clear();

        int total = 0;
        int participants = 0;
        int scanned = 0;

        String sql = "SELECT r.id_res_act, r.statut_res, r.nb_personnes, r.id_user, r.id_activite, "
                + "r.date_reservation, r.ville_user, "
                + "a.nom AS activite_nom, "
                + "TRIM(CONCAT(COALESCE(u.nom,''),' ',COALESCE(u.prenom,''))) AS user_nom "
                + "FROM reservation_activite r "
                + "LEFT JOIN activite a ON a.id_activite = r.id_activite "
                + "LEFT JOIN user_app u ON u.id_user = r.id_user "
                + "ORDER BY r.id_res_act DESC";

        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("date_reservation");
                LocalDateTime dt = ts != null ? ts.toLocalDateTime() : null;

                Reservation r = new Reservation(
                        rs.getInt("id_res_act"),
                        dt,
                        rs.getString("statut_res"),
                        rs.getInt("nb_personnes"),
                        rs.getInt("id_user"),
                        rs.getInt("id_activite"),
                        rs.getString("ville_user")
                );
                String an = rs.getString("activite_nom");
                r.setActiviteNom(an != null && !an.isBlank() ? an : null);
                String un = rs.getString("user_nom");
                r.setUserDisplay(un != null && !un.trim().isEmpty() ? un.trim() : null);

                list.add(r);
                total++;
                participants += r.getNbPersonnes();
                if ("SCANNEE".equalsIgnoreCase(nvl(r.getStatut(), ""))) {
                    scanned++;
                }
            }

            tableReservation.setItems(list);

            if (lblTotal != null) {
                lblTotal.setText(String.valueOf(total));
            }
            if (lblTickets != null) {
                lblTickets.setText(String.valueOf(participants));
            }
            if (lblChecked != null) {
                lblChecked.setText(String.valueOf(scanned));
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur chargement réservations: " + e.getMessage()).show();
        }
    }

    @FXML
    public void refreshTable() {
        loadData();
    }

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

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        String sql = "DELETE FROM reservation_activite WHERE id_res_act = ?";

        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, selected.getId());
            pst.executeUpdate();
            loadData();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur suppression: " + e.getMessage()).show();
        }
    }

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
            controller.setParentController(this);

            Stage dialogStage = new Stage();
            dialogStage.initOwner(tableReservation.getScene().getWindow());
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.setResizable(true);
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

    public void refreshAfterEdit() {
        loadData();
    }

    private String formatDT(LocalDateTime dt) {
        if (dt == null) {
            return "-";
        }
        return dt.format(dtf);
    }

    private String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
