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
    public void initialize() {
        javafx.application.Platform.runLater(() -> changeCenter("/GUI/ListReclamation.fxml"));
    }

    @FXML
    void goToProfil(ActionEvent event) {
        changeCenter("/GUI/Profile.fxml");
    }

    @FXML
    void goToMyReclamations(ActionEvent event) {
        changeCenter("/GUI/ListReclamation.fxml");
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
    void goTorsrv(ActionEvent event) {
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
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible d'ouvrir la messagerie.");
        }
    }

    @FXML
    void goToActivities(ActionEvent event) {
        changeCenter("/views/reservation_activites_user.fxml");
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
    public void goToUserSeance(ActionEvent event) {
        SceneUtils.loadScene("/GUI/UserSeances.fxml", "/GUI/userSeances.css", (Node) event.getSource());
    }

    // =====================================================================
    // NOUVEAU : Navigation vers la page d'inscription aux packs
    // Appelé par onAction="#goToPackInscription" dans SideBarUser.fxml
    // =====================================================================
    @FXML
    public void goToPackInscription(ActionEvent event) {
        changeCenter("/fxml/PackInscriptionView.fxml");
    }

    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        Entities.Session.logout();
        Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
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

    @FXML
    private Node sidebarRoot;
}