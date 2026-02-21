package Controllers;


import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.Node;
import Services.ReservationService;

import Models.Reservation;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class ReservationController {

    @FXML
    private TableView<Reservation> tableReservation;
    //@FXML
    //private TableColumn<Reservation, Integer> colId;
   // @FXML
   // private TableColumn<Reservation, Date> colDate;
    @FXML
    private TableColumn<Reservation, String> colStatut;
    @FXML
    private TableColumn<Reservation, Integer> colNb;
    @FXML
    private TableColumn<Reservation, Integer> colUser;
    @FXML
    private TableColumn<Reservation, Integer> colActivite;

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final ReservationService reservationService = new ReservationService();

    private final String USER = "root";
    private final String PASSWORD = "";

    ObservableList<Reservation> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

       // colId.setCellValueFactory(data ->
               // new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());

       // colDate.setCellValueFactory(data ->
          //      new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getDate()));

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
                       // rs.getDate("date_reservation"),
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
    private void modifierReservation() {
        Reservation selected = tableReservation.getSelectionModel().getSelectedItem();

        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez une réservation").show();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/EditReservationPopup.fxml"));
            Parent root = loader.load();

            // ⚠️ Le nom ici doit correspondre EXACTEMENT à fx:controller dans EditReservationPopup.fxml
            EditReservationPopupController controller = loader.getController();
            controller.setReservation(selected);
            controller.setParentController(this);

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
                    "Erreur ouverture de modif:\n" + e.getClass().getSimpleName() + "\n" + e.getMessage()
            ).show();
        }
    }

    public void refreshTable() {
        loadData();
    }

    @FXML
    private void goToUserSeances(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/UserSeances.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}
