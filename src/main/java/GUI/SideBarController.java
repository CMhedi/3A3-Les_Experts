package GUI;

import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SideBarController {

    @FXML
    private VBox sidebarRoot;

    @FXML
    private void initialize() {
        Platform.runLater(() -> changeCenter("/gui/AdminUsers.fxml"));
    }

    @FXML
    void goToDashboard(ActionEvent event) {
        changeCenter("/GUI/AdminDashboard.fxml");
    }

    @FXML
    private void goToPackList(ActionEvent event) {
        changeCenter("/fxml/PackList.fxml");
    }

    @FXML
    private void goToInscriptionList(ActionEvent event) {
        changeCenter("/fxml/InscriptionList.fxml");
    }

    @FXML
    void goToReclamations(ActionEvent event) {
        changeCenter("/GUI/AdminReclamation.fxml");
    }

    @FXML
    void goTogestionuser(ActionEvent event) {
        changeCenter("/GUI/AdminUsers.fxml");
    }

    @FXML
    void goToProfil(ActionEvent event) {
        changeCenter("/GUI/Profile.fxml");
    }

    @FXML
    void goToEvenementsAdmin(ActionEvent event) {
        changeCenter("/views/evenement_list.fxml");
    }

    @FXML
    void goToStats(ActionEvent event) {
        changeCenter("/views/viewevent.fxml");
    }

    @FXML
    void goToEvenements(ActionEvent event) {
        changeCenter("/views/AdminDashboard.fxml");
    }

    @FXML
    void goToReservations(ActionEvent event) {
        changeCenter("/views/admin_reservations.fxml");
    }

    @FXML
    private void goToPlanning(ActionEvent event) {
        SceneUtils.loadScene(
                "/AdminPlanningView.fxml",
                "/admin.css",
                (Node) event.getSource());
    }

    @FXML
    private void goToAdminMessagerie(ActionEvent event) {
        changeCenter("/fxml/AdminMessagerie.fxml");
    }

    @FXML
    private void goToMessengerie(ActionEvent event) {
        try {
            BorderPane mainPane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");
            if (mainPane != null) {
                Parent root = FXMLLoader.load(getClass().getResource("/GUI/MessengerApp.fxml"));
                mainPane.setCenter(root);
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
    void openMainLayout(ActionEvent event) {
        loadScene("/gui/MainLayout.fxml", event);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            Entities.Session.logout();
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Utilitaires privés
    // ─────────────────────────────────────────────────────────

    private void changeCenter(String fxmlPath) {
        try {
            BorderPane mainPane = getHostBorderPane();
            if (mainPane == null) {
                System.out.println("❌ Sidebar n'est pas attachée à un BorderPane.");
                return;
            }

            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println("❌ Fichier introuvable : " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(url);
            mainPane.setCenter(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BorderPane getHostBorderPane() {
        if (sidebarRoot == null || sidebarRoot.getScene() == null)
            return null;
        if (sidebarRoot.getScene().getRoot() instanceof BorderPane bp)
            return bp;
        Node rootNode = sidebarRoot.getScene().getRoot();
        if (rootNode.lookup("#mainPaneUser") instanceof BorderPane bp)
            return bp;
        return null;
    }

    private void loadScene(String fxmlPath, ActionEvent event) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) return;
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}