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

    public void setMainPane(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    @FXML
    void goToProfil(ActionEvent event) {
        changeCenter("/GUI/Profile.fxml");
    }

    @FXML
    void goToMyReclamations(ActionEvent event) {
        changeCenter("/GUI/AddReclamation.fxml");
    }

    @FXML
    void goToNews(ActionEvent event) {
        changeCenter("/GUI/NewsView.fxml");
    }

    @FXML
    void goToEvenements(ActionEvent event) {
        changeCenter("/views/ClientEvents.fxml");
    }
    @FXML
    void goToRsrv(ActionEvent event) {
        changeCenter("/views/reservation_list.fxml");
    }

    @FXML
    private void goToMessengerie(ActionEvent event) {
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
    void goToActivities(ActionEvent event) {
        // logic pour les activités ken thib t-activiha
    }

    public void goToSeancesDisponibles(ActionEvent event) {
        SceneUtils.loadScene("/UserSeanceView.fxml", "/admin.css", (Node) event.getSource());
    }

    public void goToNutrition(ActionEvent event) {
        SceneUtils.loadScene("/NuritionView.fxml", "/nutrition.css", (Node) event.getSource());
    }

    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        Entities.Session.logout();
        Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    private void changeCenter(String fxmlPath) {
        try {
            // Thabet hna: nesta'mlou el mainPane eli t-setta wala n-lawjou 'lih mel scene
            BorderPane pane = (mainPane != null) ? mainPane : (BorderPane) sidebarRoot.getScene().lookup("#mainPaneUser");

            if (pane == null) {
                // Fallback ken malqinech el pane bel-id
                System.out.println("⚠ Impossible de trouver mainPaneUser");
                return;
            }

            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            pane.setCenter(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private Node sidebarRoot; // matensech tzid fx:id="sidebarRoot" fi el FXML mta' el user sidebar
}