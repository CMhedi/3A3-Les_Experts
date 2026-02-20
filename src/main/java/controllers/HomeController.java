package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;
import Utiles.SceneNavigator;

public class HomeController {

    @FXML
    public void goToClient(ActionEvent e) {
        try {
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            // Thabbet f-ism el FXML: lazem ykoun reservation_list.fxml
            SceneNavigator.go(stage, "/views/reservation_list.fxml", "EcoAdventure - Mes Réservations");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void goToAdmin(ActionEvent e) {
        try {
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            SceneNavigator.go(stage, "/views/evenement_list.fxml", "EcoAdventure - Admin");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}