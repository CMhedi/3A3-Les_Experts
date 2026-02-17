package gui;

import Entities.UserApp;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SuccessController {

    @FXML private Label welcomeLabel;
    @FXML private ImageView userImageView;


    public void setUserData(UserApp u) {
        welcomeLabel.setText("Welcome, " + u.getNom() + " " + u.getPrenom() + " !");

    }
}