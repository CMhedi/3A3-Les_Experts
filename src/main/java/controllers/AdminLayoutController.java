package controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class AdminLayoutController {

    @FXML private StackPane contentPane;

    @FXML
    public void initialize() {
        load("admin_events.fxml");
    }

    @FXML
    private void goEvents() {
        load("admin_events.fxml");
    }

    @FXML
    private void goReservations() {
        load("admin_reservations.fxml");
    }

    @FXML
    private void logout() {
        // retourner vers login.fxml
    }

    private void load(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/admin/" + fxml));
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
