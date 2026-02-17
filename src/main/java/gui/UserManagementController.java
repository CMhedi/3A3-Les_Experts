package gui;

import Entities.UserApp;
import Services.interfaces.UserService;
import enums.RoleUser;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent; // HEDHI EL S7I7A
import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UserManagementController {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtTel;
    @FXML private PasswordField txtMdp;
    @FXML private ComboBox<RoleUser> comboRole;
    @FXML private Button btnAjouter;


    @FXML private Label errorNom, errorPrenom, errorEmail, errorTel, errorMdp;

    private UserService us = new UserService();

    @FXML
    public void initialize() {
        comboRole.setItems(FXCollections.observableArrayList(RoleUser.values()));

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

        // 1. Validation Logic (Bordure rouge + Label error)
        txtNom.textProperty().addListener((o, old, n) -> {
            boolean valid = !n.isEmpty();
            errorNom.setVisible(!valid);
            txtNom.setStyle(valid ? "" : "-fx-border-color: red;");
        });

        txtEmail.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.isEmpty()) {
                errorEmail.setText("⚠️ L'email est obligatoire");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else if (!newV.contains("@")) {
                errorEmail.setText("⚠️ Il manque le symbole '@'");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else if (!newV.contains(".")) {
                errorEmail.setText("⚠️ Il manque le point '.' (ex: .com)");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else if (!newV.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                errorEmail.setText("⚠️ Format invalide (ex: nom@domaine.com)");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else {
                // Kol chay mrigel
                errorEmail.setVisible(false);
                txtEmail.setStyle("");
            }
        });

        txtTel.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.length() > 8) txtTel.setText(oldV);

            if (newV.isEmpty()) {
                errorTel.setText("⚠️ Le téléphone est obligatoire");
                errorTel.setVisible(true);
            } else if (!newV.matches("\\d*")) { // Ken dakhél 7rouf
                errorTel.setText("⚠️ Utilisez uniquement des chiffres");
                errorTel.setVisible(true);
                txtTel.setStyle("-fx-border-color: red;");
            } else if (newV.length() != 8) {
                errorTel.setText("⚠️ Il faut exactement 8 chiffres (" + newV.length() + "/8)");
                errorTel.setVisible(true);
                txtTel.setStyle("-fx-border-color: red;");
            } else {
                errorTel.setVisible(false);
                txtTel.setStyle("");
            }
        });

        txtMdp.textProperty().addListener((o, old, n) -> {
            boolean valid = n.length() >= 6;
            errorMdp.setVisible(!valid);
            txtMdp.setStyle(valid ? "" : "-fx-border-color: red;");
        });

        //  2. Disable Button ken el formulaire mouch s7i7
        btnAjouter.disableProperty().bind(
                txtNom.textProperty().isEmpty()
                        .or(txtEmail.textProperty().isEmpty())
                        .or(txtTel.textProperty().length().isNotEqualTo(8))
                        .or(txtMdp.textProperty().length().lessThan(6))
                        .or(comboRole.valueProperty().isNull())
                        .or(Bindings.createBooleanBinding(() -> !txtEmail.getText().matches(emailRegex), txtEmail.textProperty()))
        );

        // Styling el bouton
        btnAjouter.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 25;");
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        UserApp u = new UserApp();
        u.setNom(txtNom.getText());
        u.setPrenom(txtPrenom.getText());
        u.setEmail(txtEmail.getText());
        u.setTelephone(txtTel.getText());
        u.setMotDePasse(txtMdp.getText());
        u.setRole(comboRole.getValue());
        u.setImageUrl("/gui/default-user.png");

        try {
            us.add(u);


            // Redirect lel Login
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }

    @FXML
    void handleGoToLogin(MouseEvent event) {
        try {

            Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));


            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();


            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println(" Erreur: Ma l9itich el fichier Login.fxml!");
            e.printStackTrace();
        }
    }
}