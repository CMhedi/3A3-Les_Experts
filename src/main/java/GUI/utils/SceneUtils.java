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
        return null;
    }
    public static FXMLLoader loadScene(String fxml,
                                       Node source) {
        return loadScene(fxml, null, source);
    }

    public static void switchScene(String s) {
    }
}
