package Controllers;

import Models.Reservation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;

public class EditReservationPopupController {

    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Spinner<Integer> spNbPersonnes;

    @FXML private TextField tfIdReservation;
    @FXML private TextField tfIdUser;
    @FXML private TextField tfIdActivite;

    private Reservation reservation;                 // la réservation à modifier
    private ReservationController parentController;  // pour refresh la table après save

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    @FXML
    public void initialize() {
        // Statuts possibles (tu peux changer selon ton système)
        cbStatut.getItems().addAll("En attente", "Confirmée", "Annulée");

        // Spinner participants (1..500)
        SpinnerValueFactory<Integer> vf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1);
        spNbPersonnes.setValueFactory(vf);
        spNbPersonnes.setEditable(true);
    }

    /** Appelé depuis ReservationController après loader.load() */
    public void setReservation(Reservation r) {
        this.reservation = r;

        // Pré-remplissage
        tfIdReservation.setText(String.valueOf(r.getId()));
        tfIdUser.setText(String.valueOf(r.getIdUser()));
        tfIdActivite.setText(String.valueOf(r.getIdActivite()));

        if (r.getDate() != null) {
            dpDate.setValue(r.getDate().toLocalDate());
        }

        cbStatut.setValue(r.getStatut());

        if (spNbPersonnes.getValueFactory() != null) {
            spNbPersonnes.getValueFactory().setValue(r.getNbPersonnes());
        }
    }

    /** Pour pouvoir refresh la table après modification */
    public void setParentController(ReservationController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    @FXML
    private void onSave() {
        if (reservation == null) {
            new Alert(Alert.AlertType.ERROR, "Aucune réservation à modifier.").show();
            return;
        }

        // ===== Validations =====
        LocalDate newDate = dpDate.getValue();
        String newStatut = cbStatut.getValue();
        Integer newNb = spNbPersonnes.getValue();

        if (newDate == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez choisir une date.").show();
            return;
        }
        if (newStatut == null || newStatut.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez choisir un statut.").show();
            return;
        }
        if (newNb == null || newNb <= 0) {
            new Alert(Alert.AlertType.WARNING, "Nombre de participants invalide.").show();
            return;
        }

        // ===== UPDATE DB =====
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {

            String sql = "UPDATE reservation_activite " +
                    "SET date_reservation=?, statut_res=?, nb_personnes=? " +
                    "WHERE id_res_act=?";

            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setDate(1, Date.valueOf(newDate));
                pst.setString(2, newStatut);
                pst.setInt(3, newNb);
                pst.setInt(4, reservation.getId());
                pst.executeUpdate();
            }




            // Refresh table dans la page principale
            if (parentController != null) {
                parentController.refreshTable(); // on va ajouter cette méthode dans ReservationController
            }

            new Alert(Alert.AlertType.INFORMATION, "Modification réussie !").show();
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la modification.").show();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) tfIdReservation.getScene().getWindow();
        stage.close();
    }
}
