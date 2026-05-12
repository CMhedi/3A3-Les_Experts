package Utiles;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public final class SceneNavigator {
    private SceneNavigator() {}

    public static void go(Stage stage, String fxmlPath, String title) {
        try {
            URL res = SceneNavigator.class.getResource(fxmlPath);
            if (res == null) {
                System.err.println("ERREUR: Fichier FXML introuvable -> " + fxmlPath);
                return;
            }
            Parent root = FXMLLoader.load(res);
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.sizeToScene();
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}