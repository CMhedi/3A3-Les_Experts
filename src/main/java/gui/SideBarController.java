package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SideBarController {

    @FXML
    void goToReclamations(ActionEvent event) {

        changeCenter("/gui/AdminReclamation.fxml", event);
    }

    @FXML
    void goToDashboard(ActionEvent event) {
        changeCenter("/gui/AdminDashboard.fxml", event);
    }
    @FXML
    void goTogestionuser(ActionEvent event) {
        changeCenter("/gui/AdminUsers.fxml", event);
    }
   @FXML
    void handleLogout(ActionEvent event) { // ✅ Thabbet f'ism el méthode
        try {
            Entities.Session.logout();
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeCenter(String fxmlPath, ActionEvent event) {
        try {

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = stage.getScene();

            if (scene.getRoot() instanceof BorderPane mainPane) {

                URL url = getClass().getResource(fxmlPath);
                if (url == null) {
                    System.out.println("❌ Erreur: Ficher introuvable -> " + fxmlPath);
                    return;
                }
                Parent root = FXMLLoader.load(url);
                mainPane.setCenter(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    void goToProfil(ActionEvent event) {
        changeCenter("/gui/Profile.fxml", event);
    }
}