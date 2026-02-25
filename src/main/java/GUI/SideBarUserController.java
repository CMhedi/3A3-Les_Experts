package GUI;

import GUI.utils.DialogUtils;
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
        changeCenter("/GUI/Profile.fxml");
    }

    @FXML
    void goToMyReclamations(ActionEvent event) {
        changeCenter("/GUI/AddReclamation.fxml");
    }

    @FXML
    private void goToMessengerie(ActionEvent event) {
        try {
            // Chercher le BorderPane principal (ID "mainPaneUser" ou "mainPaneCoach" selon votre layout)
            BorderPane mainPane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");
            if (mainPane != null) {
                Parent root = FXMLLoader.load(getClass().getResource("/GUI/MessengerApp.fxml"));
                mainPane.setCenter(root);
            } else {
                // Fallback : charger la vue dans une nouvelle scène si le BorderPane n'est pas trouvé
                Parent root = FXMLLoader.load(getClass().getResource("/GUI/MessengerApp.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Optionnel : afficher une alerte
            DialogUtils.showError("Erreur", "Impossible d'ouvrir la messagerie.");
        }
    }

    @FXML
    void goToNews(ActionEvent event) {
        changeCenter("/GUI/NewsView.fxml");
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
        Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
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