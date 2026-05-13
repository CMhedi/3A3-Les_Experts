package controllers.examples;

import Entities.UserApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import GUI.utils.SceneUtils;

/**
 * Example: How to integrate PackInscriptionView into your application
 *
 * This example shows:
 * 1. How to navigate to the pack inscription page
 * 2. How to add a button to the menu
 * 3. How to handle navigation
 * 4. How to pass parameters if needed
 */
public class MenuIntegrationExample {

    // ==================== EXAMPLE 1: Simple Navigation ====================

    /**
     * Add this button to your Menu.fxml or Dashboard
     *
     * <Button text="S'inscrire au Pack"
     *         onAction="#goToPackInscription"
     *         styleClass="btn-primary"/>
     */
    @FXML
    private void goToPackInscription() {
        try {
            // Simple scene switch using SceneUtils
            SceneUtils.switchScene("PackInscriptionView.fxml");
        } catch (Exception e) {
            System.err.println("Error navigating to pack inscription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== EXAMPLE 2: Direct FXMLLoader ====================

    /**
     * Alternative method using direct FXMLLoader
     */
    @FXML
    private void goToPackInscriptionDirect(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/PackInscriptionView.fxml")
            );

            VBox root = loader.load();

            // Optional: Get controller to initialize data
            // PackInscriptionViewController controller = loader.getController();

            Scene scene = new Scene(root);

            // Get the current window and change scene
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("S'inscrire au Pack - EcoAdventure");
            stage.show();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== EXAMPLE 3: With Data Passing ====================

    /**
     * Pass data to the controller if needed
     */
    @FXML
    private void goToPackInscriptionWithData(ActionEvent event) {
        int userId = 0; // Set your userId here or pass it differently
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/PackInscriptionView.fxml")
            );

            VBox root = loader.load();

            // Get controller and set initial data
            // PackInscriptionViewController controller = loader.getController();
            // controller.setUserId(userId);
            // controller.initialize();

            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== EXAMPLE 4: Menu Item HTML ====================

    /**
     * Add this to your Menu.fxml under the MenuBar:
     *
     * <Menu text="Packs">
     *     <MenuItem text="Voir les Packs" onAction="#viewPacksList"/>
     *     <MenuItem text="S'inscrire à un Pack" onAction="#goToPackInscription"/>
     *     <MenuItem text="Mes Inscriptions" onAction="#viewMyInscriptions"/>
     * </Menu>
     */

    // ==================== EXAMPLE 5: Dashboard Integration ====================

    /**
     * Example button configuration for Dashboard:
     *
     * <Button text="📦 S'inscrire au Pack"
     *         styleClass="btn-primary btn-lg"
     *         prefWidth="300"
     *         prefHeight="60"
     *         onAction="#goToPackInscription">
     *     <tooltip>
     *         <Tooltip text="Découvrez et inscrivez-vous à nos packs exclusifs"/>
     *     </tooltip>
     * </Button>
     */

    // ==================== EXAMPLE 6: Navigation with Callback ====================

    /**
     * Navigate with callback after inscription
     */
    private void goToPackInscriptionWithCallback() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/PackInscriptionView.fxml")
            );

            VBox root = loader.load();

            // Get controller and set callback
            // PackInscriptionViewController controller = loader.getController();
            // controller.setOnInscriptionComplete(() -> {
            //     showSuccess("Inscription réussie!");
            //     goBackToMenu();
            // });

            Scene scene = new Scene(root);
            Stage stage = getCurrentStage();
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== EXAMPLE 7: Controller Navigation ====================

    /**
     * Template for any controller that needs to navigate to pack inscription
     */
    public class ExampleDashboardController {

        @FXML
        private Button btnPackInscription;

        @FXML
        public void initialize() {
            btnPackInscription.setOnAction(event -> {
                try {
                    SceneUtils.switchScene("PackInscriptionView.fxml");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    // ==================== EXAMPLE 8: Styling ====================

    /**
     * Add these styles to your Menu or Button:
     *
     * CSS:
     * -fx-background-color: #10B981;
     * -fx-text-fill: white;
     * -fx-font-size: 14;
     * -fx-font-weight: bold;
     * -fx-padding: 12 24 12 24;
     * -fx-border-radius: 8;
     * -fx-cursor: hand;
     *
     * On hover:
     * -fx-background-color: #059669;
     * -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.2), 12, 0.15, 0, 4);
     */

    // ==================== EXAMPLE 9: Scene Stack Navigation ====================

    /**
     * If using a scene stack for back navigation
     */
    private static final java.util.Stack<Scene> sceneStack = new java.util.Stack<>();

    public void goToPackInscriptionWithStack() {
        try {
            // Save current scene
            Stage stage = getStage();
            sceneStack.push(stage.getScene());

            // Load new scene
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/PackInscriptionView.fxml")
            );
            VBox root = loader.load();
            Scene scene = new Scene(root);

            // Switch to new scene
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goBack() {
        if (!sceneStack.isEmpty()) {
            getStage().setScene(sceneStack.pop());
        }
    }

    // ==================== EXAMPLE 10: Conditional Navigation ====================

    /**
     * Navigate based on user role/status
     */
    public void goToPackInscriptionIfEligible(UserApp user) {
        if (user == null) {
            showError("Veuillez vous connecter d'abord");
            goToLogin();
            return;
        }

        if ("USER_SIMPLE".equals(user.getRole()) || "CLIENT".equals(user.getRole())) {
            try {
                SceneUtils.switchScene("PackInscriptionView.fxml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showError("Vous n'avez pas accès à cette fonction");
        }
    }

    // ==================== HELPER METHODS ====================

    private void showSuccess(String message) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void goToLogin() {
        try {
            SceneUtils.switchScene("Login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goBackToMenu() {
        try {
            SceneUtils.switchScene("Menu.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Stage getStage() {
        // Get current stage from any node in the scene
        return null; // Implementation depends on your application structure
    }

    private Stage getCurrentStage() {
        // Alternative implementation
        return null;
    }

    // ==================== SAMPLE MENU STRUCTURE ====================

    /**
     * Complete Menu.fxml structure example:
     *
     * <VBox xmlns="http://javafx.com/javafx/21"
     *       xmlns:fx="http://javafx.com/fxml/1"
     *       fx:controller="controllers.MenuController">
     *
     *     <MenuBar>
     *         <Menu text="Accueil">
     *             <MenuItem text="Dashboard" onAction="#goDashboard"/>
     *         </Menu>
     *
     *         <Menu text="Packs">
     *             <MenuItem text="Parcourir les Packs" onAction="#viewPacksList"/>
     *             <MenuItem text="S'inscrire au Pack" onAction="#goToPackInscription"/>
     *             <Separator/>
     *             <MenuItem text="Mes Inscriptions" onAction="#viewMyInscriptions"/>
     *         </Menu>
     *
     *         <Menu text="Compte">
     *             <MenuItem text="Profil" onAction="#viewProfile"/>
     *             <MenuItem text="Paramètres" onAction="#viewSettings"/>
     *             <Separator/>
     *             <MenuItem text="Déconnexion" onAction="#logout"/>
     *         </Menu>
     *     </MenuBar>
     * </VBox>
     */

    // ==================== QUICK COPY-PASTE CODE ====================

    /**
     * Quick integration: Copy this entire method to any controller
     */
    public static void integratePackInscriptionNavigation() {
        // This is a static example - adapt to your needs

        // In your controller class, add this method:
        /*
        @FXML
        private void goToPackInscription() {
            try {
                SceneUtils.switchScene("PackInscriptionView.fxml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Add this button to your FXML:
        // <Button text="S'inscrire au Pack"
        //         onAction="#goToPackInscription"
        //         styleClass="btn-primary"/>
        */
    }
}

/**
 * FXML Template - Copy to your Menu.fxml:
 *
 * <Button fx:id="btnPackInscription"
 *         text="🎒 S'inscrire au Pack"
 *         styleClass="btn-primary"
 *         prefWidth="250"
 *         prefHeight="50"
 *         onAction="#goToPackInscription">
 *     <tooltip>
 *         <Tooltip text="Découvrez et inscrivez-vous à nos packs exclusifs"/>
 *     </tooltip>
 * </Button>
 */

/**
 * CSS Classes to add to your stylesheets:
 *
 * .btn-primary {
 *     -fx-background-color: #10B981;
 *     -fx-text-fill: white;
 *     -fx-font-weight: bold;
 *     -fx-padding: 12 24 12 24;
 *     -fx-border-radius: 8;
 *     -fx-cursor: hand;
 * }
 *
 * .btn-primary:hover {
 *     -fx-background-color: #059669;
 * }
 */