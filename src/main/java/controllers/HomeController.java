package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HomeController {

    public void goToAdmin(ActionEvent event) {
        load(event, "/views/evenement_list.fxml");
    }

    public void goToClient(ActionEvent event) {
        load(event, "/views/reservation_list.fxml");
    }

    private void load(ActionEvent event, String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
