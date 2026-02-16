package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;

public class SideBarUserController {

    @FXML
    void goToMyReclamations(ActionEvent event) {
        changeCenter("/gui/UserReclamation.fxml", event);
    }

    @FXML
    void goToProfil(ActionEvent event) {
        changeCenter("/gui/Profile.fxml", event); // Ken 3andek page profil
    }

    @FXML
    void goToActivities(ActionEvent event) {
        // changeCenter("/gui/UserActivities.fxml", event);
    }

    private void changeCenter(String fxmlPath, ActionEvent event) {
        try {
            // Njibou el BorderPane kbir mel Scene
            BorderPane mainPane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");

            if (mainPane != null) {
                Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
                mainPane.setCenter(page);
            } else {
                // Ken mal9ach el ID, n-jarbou el root
                Parent root = ((Node) event.getSource()).getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(FXMLLoader.load(getClass().getResource(fxmlPath)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        Entities.Session.logout();
        Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}