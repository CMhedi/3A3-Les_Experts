package Controllers;

import Models.Reservation;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class ReservationController {

    @FXML private TableView<Reservation> tableReservation;
    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, Date> colDate;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, Integer> colNb;
    @FXML private TableColumn<Reservation, Integer> colUser;
    @FXML private TableColumn<Reservation, Integer> colActivite;

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";

    private final String USER = "root";
    private final String PASSWORD = "";

    ObservableList<Reservation> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());

        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getDate()));

        colStatut.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getStatut()));

        colNb.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getNbPersonnes()).asObject());

        colUser.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getIdUser()).asObject());

        colActivite.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getIdActivite()).asObject());

        loadData();
    }

    private void loadData() {
        list.clear();
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            String sql = "SELECT * FROM reservation_activite";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                list.add(new Reservation(
                        rs.getInt("id_res_act"),
                        rs.getDate("date_reservation"),
                        rs.getString("statut_res"),
                        rs.getInt("nb_personnes"),
                        rs.getInt("id_user"),
                        rs.getInt("id_activite")
                ));
            }

            tableReservation.setItems(list);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void supprimerReservation() {

        Reservation selected = tableReservation.getSelectionModel().getSelectedItem();

        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez une réservation").show();
            return;
        }

        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            String sql = "DELETE FROM reservation_activite WHERE id_res_act = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, selected.getId());
            pst.executeUpdate();

            loadData();

        } catch (Exception e) {
            e.printStackTrace();
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

            // 🔹 1️⃣ Demander nouvelle date
            TextInputDialog dialogDate = new TextInputDialog(selected.getDate().toString());
            dialogDate.setHeaderText("Modifier la date (YYYY-MM-DD)");
            String newDate = dialogDate.showAndWait().orElse(null);

            if (newDate == null) return;

            // 🔹 2️⃣ Demander nouveau statut
            TextInputDialog dialogStatut = new TextInputDialog(selected.getStatut());
            dialogStatut.setHeaderText("Modifier le statut");
            String newStatut = dialogStatut.showAndWait().orElse(null);

            if (newStatut == null) return;

            // 🔹 3️⃣ Demander nouveau nombre personnes
            TextInputDialog dialogNb = new TextInputDialog(String.valueOf(selected.getNbPersonnes()));
            dialogNb.setHeaderText("Modifier nombre de personnes");
            String newNb = dialogNb.showAndWait().orElse(null);

            if (newNb == null) return;

            // 🔹 4️⃣ Connexion BD
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "UPDATE reservation_activite SET date_reservation=?, statut_res=?, nb_personnes=? WHERE id_res_act=?";
            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setDate(1, Date.valueOf(newDate));
            pst.setString(2, newStatut);
            pst.setInt(3, Integer.parseInt(newNb));
            pst.setInt(4, selected.getId());

            pst.executeUpdate();

            pst.close();
            conn.close();

            loadData();

            new Alert(Alert.AlertType.INFORMATION, "Modification réussie !").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la modification").show();
        }
    }

}
