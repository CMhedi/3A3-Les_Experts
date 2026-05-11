package demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/GUI/UserSeances.fxml"));

        Scene scene = new Scene(loader.load());
        stage.setTitle("espacesuser");
        stage.setScene(scene);
        stage.show();
    }
}
