package gui;

import Entities.UserApp;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SuccessController {

    @FXML private Label welcomeLabel;
    @FXML private ImageView userImageView;

    // Hadhi el méthode elli bech n'aytouilha mel Controller lokher
    public void setUserData(UserApp u) {
        welcomeLabel.setText("Welcome, " + u.getNom() + " " + u.getPrenom() + " !");
/*
        // Ken 3andek image_url, n'affichiwha, sinon n'7ottou wa7da par défaut
        if (u.getImageUrl() != null && !u.getImageUrl().isEmpty()) {
            userImageView.setImage(new Image(u.getImageUrl()));
        } else {
            // Image par défaut mel resources mte3ek
            userImageView.setImage(new Image(getClass().getResourceAsStream("/gui/default-user.png")));
        }*/
    }
}