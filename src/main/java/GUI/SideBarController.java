package GUI;

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

/**
 * SideBarController
 * - بعد ما الـ Sidebar يتشحن داخل BorderPane (بعد الـ login)، يفتح تلقائياً صفحة Users في الـ center.
 * - زرّ "Ouvrir MainLayout" يبدّل Scene بالكامل لـ MainLayout.fxml
 */
public class SideBarController {

    @FXML private VBox sidebarRoot; // ✅ مربوط بـ fx:id في SideBar.fxml

    @FXML
    private void initialize() {
        // ✅ بعد ما الـ Scene تولّي موجودة (Node attached)، نفتح Users في center
        Platform.runLater(() -> changeCenter("/gui/AdminUsers.fxml"));
    }

    @FXML
    void goToReclamations(ActionEvent event) {
<<<<<<< HEAD
        changeCenter("/gui/AdminReclamation.fxml");
=======

        changeCenter("/GUI/AdminReclamation.fxml", event);
>>>>>>> origin/salma_integration
    }

    @FXML
    void goToDashboard(ActionEvent event) {
<<<<<<< HEAD
        changeCenter("/gui/AdminDashboard.fxml");
=======
        changeCenter("/GUI/AdminDashboard.fxml", event);
>>>>>>> origin/salma_integration
    }

    @FXML
    void goTogestionuser(ActionEvent event) {
<<<<<<< HEAD
        changeCenter("/gui/AdminUsers.fxml");
=======
        changeCenter("/GUI/AdminUsers.fxml", event);
>>>>>>> origin/salma_integration
    }

    @FXML
    void goToProfil(ActionEvent event) {
        changeCenter("/gui/Profile.fxml");
    }

    @FXML
    private void goToPlanning(ActionEvent event) {
        // كما كان عندك
        SceneUtils.loadScene(
                "/AdminPlanningView.fxml",   // adapte si besoin
                "/admin.css",
                (Node) event.getSource()
        );
    }

    @FXML
    void openMainLayout(ActionEvent event) {
        loadScene("/gui/MainLayout.fxml", event); // ✅ عدّل المسار إذا مختلف
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            Entities.Session.logout();
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Change the center of the host BorderPane (إذا الـ root متاع الـ Scene هو BorderPane).
     * ✅ ماعادش يلزمنا event باش نبدّل center
     */
    private void changeCenter(String fxmlPath) {
        try {
            BorderPane mainPane = getHostBorderPane();
            if (mainPane == null) {
                // معناها الـ Sidebar ماهاش داخل BorderPane (ولا مازال ما attached)
                return;
            }

            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println("❌ Erreur: Fichier introuvable -> " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(url);
            mainPane.setCenter(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
<<<<<<< HEAD

    private BorderPane getHostBorderPane() {
        if (sidebarRoot == null || sidebarRoot.getScene() == null) return null;
        if (sidebarRoot.getScene().getRoot() instanceof BorderPane bp) return bp;
        return null;
=======
    @FXML
    void goToProfil(ActionEvent event) {
        changeCenter("/GUI/Profile.fxml", event);
>>>>>>> origin/salma_integration
    }

    private void loadScene(String fxmlPath, ActionEvent event) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println("❌ FXML introuvable -> " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(url);

            // نحافظو على نفس الـ window size (اختياري)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();

            Scene newScene = new Scene(root, w, h);
            stage.setScene(newScene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
