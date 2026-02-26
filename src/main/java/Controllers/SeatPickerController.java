package Controllers;

import Models.CategoryAvailability;
import Services.AvailabilityService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SeatPickerController {

    @FXML private GridPane gridSeats;

    @FXML private Label lblCategorie;
    @FXML private Label lblCapacite;
    @FXML private Label lblReservees;
    @FXML private Label lblRestantes;
    @FXML private Label lblStatut;
    @FXML private Label lblTotal;

    private int reservationId;
    private String statut;
    private int nbPers;
    private int idUser;
    private int idActivite;

    private final AvailabilityService availabilityService = new AvailabilityService();

    private final Set<Button> selectedSeats = new HashSet<>();
    private int remaining = 0;
    private int reserved = 0;
    private int capacity = 0;

    public void setContext(int reservationId, String statut, int nbPers, int idUser, int idActivite) {
        this.reservationId = reservationId;
        this.statut = statut;
        this.nbPers = nbPers;
        this.idUser = idUser;
        this.idActivite = idActivite;

        loadAvailability();
        buildSeatsGrid();
        updateTotal();
    }

    // ✅ FIX ICI: reserved ne doit PAS compter la réservation qu'on vient d'ajouter
    private void loadAvailability() {
        try {
            CategoryAvailability info = availabilityService.fetchAvailabilityForActivite(idActivite);

            capacity = info.getCapaciteTotale();

            // ✅ reserved = réservé par les autres (exclure cette réservation)
            reserved = availabilityService.getReservedForActiviteExcludeReservation(idActivite, reservationId);

            // ✅ remaining recalculé
            remaining = Math.max(0, capacity - reserved);

            lblCategorie.setText(info.getCategorie());
            lblCapacite.setText(String.valueOf(capacity));
            lblReservees.setText(String.valueOf(reserved));
            lblRestantes.setText(String.valueOf(remaining));
            lblStatut.setText(remaining > 0 ? "DISPONIBLE" : "COMPLET");

        } catch (Exception e) {
            e.printStackTrace();
            lblStatut.setText("Erreur disponibilité");
            remaining = 0;
            reserved = 0;
            capacity = 0;
        }
    }

    private void buildSeatsGrid() {

        gridSeats.getChildren().clear();
        selectedSeats.clear();

        int rows = 10;
        int cols = 14;
        int totalSeats = rows * cols;

        int maxRealSeats = Math.min(totalSeats, capacity);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                int index = r * cols + c + 1;

                Button seat = new Button();
                seat.getStyleClass().addAll("seat", "seatRemaining"); // gris par défaut

                // hors capacité => disable (reste gris mais désactivé)
                if (index > maxRealSeats) {
                    seat.setDisable(true);
                } else {
                    // réservé => rouge + disabled
                    if (index <= reserved) {
                        seat.getStyleClass().remove("seatRemaining");
                        seat.getStyleClass().add("seatReserved");
                        seat.setDisable(true);
                    } else {
                        // disponible => click sélection
                        seat.setOnAction(ev -> toggleSeat(seat));
                    }
                }

                gridSeats.add(seat, c, r);
            }
        }
    }

    private void toggleSeat(Button seat) {

        // select
        if (!selectedSeats.contains(seat)) {

            if (selectedSeats.size() >= nbPers) return;
            if (selectedSeats.size() >= remaining) return;

            selectedSeats.add(seat);

            seat.getStyleClass().remove("seatRemaining");
            if (!seat.getStyleClass().contains("seatSelected")) {
                seat.getStyleClass().add("seatSelected");
            }

        } else {
            // unselect
            selectedSeats.remove(seat);

            seat.getStyleClass().remove("seatSelected");
            if (!seat.getStyleClass().contains("seatRemaining")) {
                seat.getStyleClass().add("seatRemaining");
            }
        }

        updateTotal();
    }

    private void updateTotal() {
        lblTotal.setText("Selected: " + selectedSeats.size() + " / " + nbPers);
    }

    // ✅ Confirmer => recuReservation.fxml
    @FXML
    private void continuer(ActionEvent event) {

        if (selectedSeats.size() != nbPers) {
            lblStatut.setText("Sélectionnez exactement " + nbPers + " places.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/recuReservation.fxml"));
            Parent root = loader.load();

            RecuReservationController controller = loader.getController();
            controller.setData(reservationId, statut, nbPers, idUser, idActivite);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/UserSeances.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}