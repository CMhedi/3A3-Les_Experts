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

    private BorderPane mainPane;

    @FXML private Node sidebarRoot; // ✅ لازم fx:id="sidebarRoot" في SideBarUser.fxml

    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    @FXML
    public void goToProfil(ActionEvent event) {
        changeCenter("/GUI/Profile.fxml");
    }

    @FXML
    public void goToMyReclamations(ActionEvent event) {
        changeCenter("/GUI/AddReclamation.fxml");
    }

    @FXML
    public void goToNews(ActionEvent event) {
        changeCenter("/GUI/NewsView.fxml");
    }

    @FXML
    public void goToEvenements(ActionEvent event) {
        changeCenter("/GUI/reservation_list.fxml");
    }

    // ✅✅✅ هذي هي اللي ناقصة عندك وسببت الخطأ
    @FXML
    public void goToPacks(ActionEvent event) {
        changeCenter("/GUI/UserPackList.fxml");
    }

    @FXML
    public void goToMessengerie(ActionEvent event) {
        try {
            BorderPane pane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");
            if (pane != null) {
                Parent root = FXMLLoader.load(getClass().getResource("/GUI/MessengerApp.fxml"));
                pane.setCenter(root);
            } else {
                Parent root = FXMLLoader.load(getClass().getResource("/GUI/MessengerApp.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
            }
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible d'ouvrir la messagerie.");
        }
    }

    @FXML
    public void goToActivities(ActionEvent event) {
        // optional
    }

    @FXML
    public void goToSeancesDisponibles(ActionEvent event) {
        SceneUtils.loadScene("/UserSeanceView.fxml", "/admin.css", (Node) event.getSource());
    }

    @FXML
    public void goToNutrition(ActionEvent event) {
        SceneUtils.loadScene("/NuritionView.fxml", "/nutrition.css", (Node) event.getSource());
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        Entities.Session.logout();
        Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    private void changeCenter(String fxmlPath) {
        try {
            BorderPane pane = (mainPane != null)
                    ? mainPane
                    : (BorderPane) sidebarRoot.getScene().lookup("#mainPaneUser");

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
}