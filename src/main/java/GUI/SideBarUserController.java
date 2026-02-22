package GUI;

import GUI.utils.SceneUtils;
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
    void goToProfil(ActionEvent event) {
        changeCenter("/gui/Profile.fxml");
    }

    @FXML
    void goToMyReclamations(ActionEvent event) {
        changeCenter("/gui/AddReclamation.fxml");
    }


    @FXML
    void goToActivities(ActionEvent event) {
       // SceneUtils.loadScene(
              //  "/gui/UserActivities.fxml",
               // "/admin.css",
              //  (Node) event.getSource()
        //);
    }

    private void changeCenter(String fxmlPath) {
        try {

            BorderPane pane =
                    (BorderPane) mainPane.getScene().lookup("#mainPaneUser");

            if (pane == null) {
                System.out.println("⚠ Impossible de trouver mainPaneUser");
                return;
            }

            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            pane.setCenter(page);

        } catch (Exception e) {
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

    public void goToSeancesDisponibles(ActionEvent event) {
        SceneUtils.loadScene(
                "/UserSeanceView.fxml",   // adapte le chemin si besoin
                "/admin.css",                 // ou user.css si tu en as un
                (Node) event.getSource()
        );
    }
    private BorderPane mainPane;

    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void goToNutrition(ActionEvent event) {
       SceneUtils.loadScene(
                "/NuritionView.fxml",
                "/nutrition.css",
                (Node) event.getSource()
       );
    }
}