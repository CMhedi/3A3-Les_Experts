package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;

public class AdminApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL sideUrl = getClass().getResource("/GUI/SideBar.fxml");
        URL dashUrl = getClass().getResource("/GUI/AdminDashboard.fxml");
        if (sideUrl == null || dashUrl == null) {
            throw new IllegalStateException("FXML manquant: SideBar ou AdminDashboard (classpath /GUI/)");
        }

        BorderPane shell = new BorderPane();
        shell.setLeft(FXMLLoader.load(sideUrl));
        shell.setCenter(FXMLLoader.load(dashUrl));

        Scene scene = new Scene(shell);
        URL adminCss = getClass().getResource("/admin.css");
        if (adminCss != null) {
            scene.getStylesheets().add(adminCss.toExternalForm());
        }

        stage.setTitle("EcoAdventure - Admin Panel");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
