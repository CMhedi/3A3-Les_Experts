package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientLayoutController {

    @FXML private StackPane contentPane;

    @FXML private Button btnEvents;
    @FXML private Button btnMyReservations;

    @FXML
    public void initialize() {
        // Charger une page par défaut dans contentPane (optionnel)
        // Si tu n'as pas encore les pages, commente cette ligne.
        // loadInContent("/views/client/client_events.fxml");

        setActive(btnEvents, btnMyReservations);
    }

    @FXML
    public void goHome(ActionEvent event) {
        switchScene(event, "/views/Home.fxml");
    }

    @FXML
    public void goEvents(ActionEvent event) {
        // Mets ici ton FXML client des événements (exemple)
        // loadInContent("/views/client/client_events.fxml");
        setActive(btnEvents, btnMyReservations);
    }

    @FXML
    public void goMyReservations(ActionEvent event) {
        // Mets ici ton FXML client des réservations (exemple)
        // loadInContent("/views/client/client_my_reservations.fxml");
        setActive(btnMyReservations, btnEvents);
    }

    @FXML
    public void logout(ActionEvent event) {
        // Option simple: retour Home
        switchScene(event, "/views/Home.fxml");
    }

    // ----------------- Helpers -----------------

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger : " + fxmlPath, e);
        }
    }

    private void loadInContent(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger dans contentPane : " + fxmlPath, e);
        }
    }

    private void setActive(Button active, Button inactive) {
        // Active (fond vert)
        active.setStyle("-fx-background-color: #2D4A3E; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
        // Inactive (gris transparent)
        inactive.setStyle("-fx-background-color: transparent; -fx-text-fill: #A0A0A0; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
    }
}
