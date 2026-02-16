package gui;

import Entities.UserApp;
import Services.interfaces.UserService;
import Entities.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.image.Image;
import javafx.scene.shape.Circle;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import javafx.beans.binding.Bindings;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;




import javafx.scene.control.*;



import java.sql.SQLException; // ✅ Import SQLException


public class ProfileController implements Initializable {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtTel;
    @FXML private Circle profileCircle;
    @FXML private VBox newPassSection;
    @FXML private PasswordField txtOldPass, txtNewPass, txtConfirmPass;
    @FXML private Label lblError, lblFullName, lblEmailTop;
    @FXML private Button btnUpdate;
    @FXML private ProgressBar passwordProgressBar;
    @FXML private StackPane mainStackPane;
    @FXML private Label errorNom, errorPrenom, errorTel, errorEmail;

    private UserApp currentUser;
    private UserService us = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.currentUser = Session.getConnectedUser();
        if (currentUser != null) {
            txtNom.setText(currentUser.getNom());
            txtPrenom.setText(currentUser.getPrenom());
            txtTel.setText(currentUser.getTelephone());
            txtEmail.setText(currentUser.getEmail());

            if (lblFullName != null) lblFullName.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            if (lblEmailTop != null) lblEmailTop.setText(currentUser.getEmail());

            updateProfileImage();

            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

            // Real-time Listeners
            if (errorNom != null) txtNom.textProperty().addListener((obs, oldV, newV) -> errorNom.setVisible(newV.isEmpty()));
            if (errorPrenom != null) txtPrenom.textProperty().addListener((obs, oldV, newV) -> errorPrenom.setVisible(newV.isEmpty()));

            txtTel.textProperty().addListener((obs, oldV, newV) -> {
                if (newV.length() > 8) txtTel.setText(oldV);
                if (errorTel != null) errorTel.setVisible(newV.length() != 8);
            });

            if (errorEmail != null) txtEmail.textProperty().addListener((obs, oldV, newV) -> errorEmail.setVisible(!newV.matches(emailRegex)));

            // Validation de Saisie
            if (btnUpdate != null) {
                btnUpdate.disableProperty().bind(
                        txtNom.textProperty().isEmpty()
                                .or(txtPrenom.textProperty().isEmpty())
                                .or(txtTel.textProperty().length().isNotEqualTo(8))
                                .or(Bindings.createBooleanBinding(() -> !txtEmail.getText().matches(emailRegex), txtEmail.textProperty()))
                );
            }

            // Password Strength Meter
            if (txtNewPass != null && passwordProgressBar != null) {
                txtNewPass.textProperty().addListener((obs, oldVal, newVal) -> {
                    double strength = calculateStrength(newVal);
                    passwordProgressBar.setProgress(strength);
                    if (strength < 0.4) passwordProgressBar.setStyle("-fx-accent: red;");
                    else if (strength < 0.7) passwordProgressBar.setStyle("-fx-accent: orange;");
                    else passwordProgressBar.setStyle("-fx-accent: green;");
                });
            }
        }
    }

    private double calculateStrength(String password) {
        if (password.isEmpty()) return 0;
        double s = 0;
        if (password.length() >= 6) s += 0.3;
        if (password.matches(".*[A-Z].*")) s += 0.3;
        if (password.matches(".*[0-9].*")) s += 0.4;
        return s;
    }

    public void showToast(String message) {
        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5;");
        toast.setOpacity(0);
        mainStackPane.getChildren().add(toast);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), toast);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2));
        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeOut.setOnFinished(e -> mainStackPane.getChildren().remove(toast));
        fadeIn.play();
    }

    @FXML
    void handleChangePhotoButton(ActionEvent event) {
        handleChangePhotoClick(null);
    }

    @FXML
    void handleUpdate(ActionEvent event) {
        currentUser.setNom(txtNom.getText());
        currentUser.setPrenom(txtPrenom.getText());
        currentUser.setTelephone(txtTel.getText());
        currentUser.setEmail(txtEmail.getText());
        try {
            us.update(currentUser); // ✅ Géré avec try-catch
            if (lblFullName != null) lblFullName.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            if (lblEmailTop != null) lblEmailTop.setText(currentUser.getEmail());
            showToast("✅ Profil mis à jour avec succès !");
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur SQL: " + e.getMessage()).show();
        }
    }

    @FXML
    void handleDeleteAccount(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement votre compte ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                us.delete(currentUser.getIdUser()); // ✅ Géré avec try-catch
                handleLogout(null);
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur SQL: " + e.getMessage()).show();
            }
        }
    }

    private void updateProfileImage() {
        try {
            String imagePath = currentUser.getImageUrl();
            if (imagePath != null && !imagePath.isEmpty()) {
                if (imagePath.startsWith("/")) {
                    profileCircle.setFill(new ImagePattern(new Image(getClass().getResourceAsStream(imagePath))));
                } else {
                    profileCircle.setFill(new ImagePattern(new Image(imagePath)));
                }
            } else {
                profileCircle.setFill(new ImagePattern(new Image(getClass().getResourceAsStream("/gui/default-user.png"))));
            }
        } catch (Exception e) {
            profileCircle.setFill(new ImagePattern(new Image(getClass().getResourceAsStream("/gui/default-user.png"))));
        }
    }

    @FXML
    void handleChangePhotoClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(profileCircle.getScene().getWindow());

        if (selectedFile != null) {
            // 1. Khoudh el Path ejdid
            String newImagePath = selectedFile.toURI().toString();

            // 2. Update el Object currentUser
            currentUser.setImageUrl(newImagePath);

            // 3. Update l'interface visuelle
            profileCircle.setFill(new ImagePattern(new Image(newImagePath)));

            // 4. ✅ AHAM KHOTWA: Sajjel fil Base de données direct
            try {
                us.update(currentUser);
                showToast("✅ Photo de profil mise à jour !");

                // Mise à jour de la session pour être sur
                Session.setConnectedUser(currentUser);

            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement de la photo: " + e.getMessage()).show();
            }
        }
    }
    @FXML
    void handleLogout(ActionEvent event) {
        try {
            Session.setConnectedUser(null);
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));
            Stage stage = (Stage) txtNom.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleVerifyPassword(ActionEvent event) {
        if (txtOldPass.getText().equals(currentUser.getMotDePasse())) {
            newPassSection.setVisible(true);
            lblError.setVisible(false);
            txtOldPass.setEditable(false);
        } else {
            lblError.setText("Mot de passe actuel incorrect !");
            lblError.setVisible(true);
        }
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        String newP = txtNewPass.getText();
        String confP = txtConfirmPass.getText();

        if (newP.isEmpty() || newP.length() < 4) {
            new Alert(Alert.AlertType.ERROR, "Le mot de passe est trop court !").show();
            return;
        }
        if (!newP.equals(confP)) {
            new Alert(Alert.AlertType.ERROR, "Les mots de passe ne correspondent pas !").show();
            return;
        }

        try {
            currentUser.setMotDePasse(newP);
            us.update(currentUser); // ✅ Géré avec try-catch
            showToast("✅ Mot de passe changé avec succès !");
            newPassSection.setVisible(false);
            txtOldPass.clear();
            txtOldPass.setEditable(true);
            txtNewPass.clear();
            txtConfirmPass.clear();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur SQL: " + e.getMessage()).show();
        }
    }
}