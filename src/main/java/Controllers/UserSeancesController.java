package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UserSeancesController {

    // ====== Bouton top: "Utilisateur" => AdminActivites.fxml ======
    @FXML
    private void goToAdmin(ActionEvent event) {
        switchScene(event, "/GUI/AdminActivites.fxml");  // adapte le chemin
    }

    // ====== Bouton bas: "+ Ajouter une activité" ======
    @FXML
    private void openAjouterActivite(ActionEvent event) {
        switchScene(event, "/GUI/AjouterActivite.fxml"); // adapte le chemin
    }

    // ====== Exemple réserver (tu gardes ta logique) ======
    @FXML
    private void reserver(ActionEvent event) {

        switchScene(event, "/GUI/addres.fxml");
    }

    // ====== Utilitaire switch scene ======
    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
