package GUI;

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
import java.sql.SQLException;

public class ProfileController implements Initializable {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtTel;
    // --- CHAMPS COACH ---
    @FXML private VBox paneCoach;
    @FXML private TextField txtAge, txtExperience;
    @FXML private Label lblSpecialite, lblDispo;

    @FXML private Circle profileCircle;
    @FXML private VBox newPassSection;
    @FXML private PasswordField txtOldPass, txtNewPass, txtConfirmPass;
    @FXML private Label lblError, lblFullName, lblEmailTop;
    @FXML private Button btnUpdate;
    @FXML private ProgressBar passwordProgressBar;
    @FXML private StackPane mainStackPane;
    @FXML private Label errorNom, errorPrenom, errorTel, errorEmail;
    @FXML private Label lblAgeValue, lblExpValue, lblSpecValue;
    @FXML private ComboBox<String> comboDispoUpdate;
    private UserApp currentUser;
    private UserService us = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.currentUser = Session.getConnectedUser();

        if (currentUser != null) {
            // 1. Data 3adia
            txtNom.setText(currentUser.getNom());
            txtPrenom.setText(currentUser.getPrenom());
            txtTel.setText(currentUser.getTelephone());
            txtEmail.setText(currentUser.getEmail());

            if (lblFullName != null) lblFullName.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            if (lblEmailTop != null) lblEmailTop.setText(currentUser.getEmail());

            // 2. Logic Coach (Affichage barka + Combo)
            if (paneCoach != null) {
                boolean isCoach = currentUser.getRole().toString().equals("COACH");
                paneCoach.setVisible(isCoach);
                paneCoach.setManaged(isCoach);

                if (isCoach) {
                    if (lblAgeValue != null) lblAgeValue.setText(currentUser.getAge() + " ans");
                    if (lblExpValue != null) lblExpValue.setText(currentUser.getExperience() + " ans");
                    if (lblSpecValue != null) lblSpecValue.setText(currentUser.getSpecialite());

                    if (comboDispoUpdate != null) {
                        comboDispoUpdate.getItems().setAll("MATIN", "SOIR", "JOURNEE_COMPLETE");
                        comboDispoUpdate.setValue(currentUser.getDisponibilite());
                    }
                }
            }

            updateProfileImage();
// 1. Regex mta3 el-Email (Thabbet fih)
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

// 2. Listeners bech el-Labels mta3 el-Erreur yodh-hrou toul
            txtEmail.textProperty().addListener((obs, oldV, newV) -> {
                if (errorEmail != null) {
                    // Itha feragh wala el-format ghalat -> Error visible
                    errorEmail.setVisible(newV.isEmpty() || !newV.matches(emailRegex));
                }
            });

// 3. Binding el-Bouton (Ma t-khallihach tekhdem ken l-Email ghalat)
            if (btnUpdate != null) {
                btnUpdate.disableProperty().bind(
                        txtNom.textProperty().isEmpty()
                                .or(txtPrenom.textProperty().isEmpty())
                                .or(txtTel.textProperty().length().isNotEqualTo(8))
                                .or(txtEmail.textProperty().isEmpty()) // <--- Zid hedhi bech ma ya9belch feragh
                                .or(Bindings.createBooleanBinding(
                                        () -> !txtEmail.getText().matches(emailRegex),
                                        txtEmail.textProperty()
                                ))
                );
            }
        }
    }
    @FXML
    void handleUpdate(ActionEvent event) {
        currentUser.setNom(txtNom.getText());
        currentUser.setPrenom(txtPrenom.getText());
        currentUser.setTelephone(txtTel.getText());
        currentUser.setEmail(txtEmail.getText());

        // Update ken el-Disponibilité lil Coach
        if (currentUser.getRole().toString().equals("COACH") && comboDispoUpdate != null) {
            currentUser.setDisponibilite(comboDispoUpdate.getValue());
        }

        try {
            us.update(currentUser);
            if (lblFullName != null) lblFullName.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            showToast("✅ Profil mis à jour avec succès !");
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur SQL: " + e.getMessage()).show();
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(profileCircle.getScene().getWindow());

        if (selectedFile != null) {
            String newImagePath = selectedFile.toURI().toString();
            currentUser.setImageUrl(newImagePath);
            profileCircle.setFill(new ImagePattern(new Image(newImagePath)));
            try {
                us.update(currentUser);
                showToast("✅ Photo de profil mise à jour !");
                Session.setConnectedUser(currentUser);
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
            }
        }
    }

    @FXML
    void handleDeleteAccount(ActionEvent event) { // <--- Thabbet f'esmha s7i7!
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement votre compte ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                us.delete(currentUser.getIdUser());
                handleLogout(null);
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur SQL: " + e.getMessage()).show();
            }
        }
    }
    @FXML
    void handleLogout(ActionEvent event) {
        try {
            Session.setConnectedUser(null);
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));
            Stage stage = (Stage) mainStackPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
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
            new Alert(Alert.AlertType.ERROR, "Mot de passe trop court !").show();
            return;
        }
        if (!newP.equals(confP)) {
            new Alert(Alert.AlertType.ERROR, "Mots de passe non identiques !").show();
            return;
        }
        try {
            currentUser.setMotDePasse(newP);
            us.update(currentUser);
            showToast("✅ Mot de passe changé !");
            newPassSection.setVisible(false);
            txtOldPass.clear();
            txtOldPass.setEditable(true);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }
}