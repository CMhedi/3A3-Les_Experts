package org.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/GUI/AjouterActivite.fxml"));

        Scene scene = new Scene(loader.load());
        stage.setTitle("Ajouter Réservation");
        stage.setScene(scene);
        stage.show();
    }
}
