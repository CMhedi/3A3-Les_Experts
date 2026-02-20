package Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MessengerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load FXML file from resources/Gui folder
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Gui/Messenger.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Instagram Messenger - JavaFX");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();

            System.out.println("✅ Application started successfully");
        } catch (Exception e) {
            System.out.println("❌ Error loading FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}