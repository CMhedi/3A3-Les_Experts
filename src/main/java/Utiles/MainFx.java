package Utiles;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainFx extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainFx.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            // On lance le MENU au lieu d'une page CRUD directement
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Gui/MessengerApp.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("❌ FXML file not found! Check path: /Gui/MessengerApp.fxml");
                return;
            }

            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Les Experts - Menu Principal");
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}