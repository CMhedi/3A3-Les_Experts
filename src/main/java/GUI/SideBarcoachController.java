
package GUI;
import GUI.utils.DialogUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;

public class SideBarcoachController{


    private BorderPane mainContent;

    @FXML
    void goToProfil(ActionEvent event) {
        changeCenter("/GUI/Profile.fxml", event);
    }

    @FXML
    void goToActivities(ActionEvent event) {
        // changeCenter("/gui/UserActivities.fxml", event);
    }
    @FXML
    void goToevenement(ActionEvent event) {
        // changeCenter("/gui/evenement(.fxml", event);
    }
    @FXML
    void goToplanning(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/CoachDashboard.fxml")
            );

            Parent root = loader.load();

            Scene scene = new Scene(root);

            scene.getStylesheets().add(
                    getClass().getResource("/admin.css")
                            .toExternalForm()
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {

            e.printStackTrace();

            DialogUtils.showError(
                    "Erreur",
                    "Impossible d’ouvrir le Dashboard Coach."
            );
        }
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
    private void changeCenter(String fxmlPath, ActionEvent event) {
        try {

            BorderPane mainPane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");

            if (mainPane != null) {
                Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
                mainPane.setCenter(page);
            } else {

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
        Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}