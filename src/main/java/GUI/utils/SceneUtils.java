package GUI.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;

public class SceneUtils {

    public static FXMLLoader loadScene(
            String fxmlPath,
            String cssPath,
            Node source) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    SceneUtils.class.getResource(fxmlPath)
            );

            Parent root = loader.load();

            Scene scene = new Scene(root);

            // Charger CSS spécifique
            if (cssPath != null) {
                scene.getStylesheets().add(
                        SceneUtils.class
                                .getResource(cssPath)
                                .toExternalForm()
                );
            }

            Stage stage =
                    (Stage) source.getScene().getWindow();

            stage.setScene(scene);
            stage.show();

            return loader;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static FXMLLoader loadScene(String fxml,
                                       Node source) {
        return loadScene(fxml, null, source);
    }
}
