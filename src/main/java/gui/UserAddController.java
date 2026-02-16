package gui;

import Entities.UserApp;
import Services.interfaces.UserService;
import enums.RoleUser;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class UserAddController {

    @FXML private TextField txtNom, txtPrenom, txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboRole;
    @FXML private Button btnEnregistrer; // Zid el ID mta3 el bouton fil FXML

    // Labels lel error (lezem t-zidhom fil FXML)
    @FXML private Label errorNom, errorPrenom, errorEmail, errorPassword;

    private UserService userService = new UserService();
    private final String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    @FXML
    public void initialize() {
        // Setup Roles
        comboRole.setItems(FXCollections.observableArrayList(
                Arrays.stream(RoleUser.values()).map(Enum::name).collect(Collectors.toList())
        ));

        // ✅ 1. Validation Nom & Prenom
        txtNom.textProperty().addListener((o, old, n) -> {
            boolean valid = !n.trim().isEmpty();
            errorNom.setVisible(!valid);
            txtNom.setStyle(valid ? "" : "-fx-border-color: red;");
        });

        txtPrenom.textProperty().addListener((o, old, n) -> {
            boolean valid = !n.trim().isEmpty();
            errorPrenom.setVisible(!valid);
            txtPrenom.setStyle(valid ? "" : "-fx-border-color: red;");
        });

        // ✅ 2. Validation Email
        txtEmail.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.isEmpty()) {
                errorEmail.setText("⚠️ L'email est obligatoire");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else if (!newV.matches(emailRegex)) {
                errorEmail.setText("⚠️ Format invalide (ex: nom@domaine.com)");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else {
                errorEmail.setVisible(false);
                txtEmail.setStyle("");
            }
        });

        // ✅ 3. Validation Password
        txtPassword.textProperty().addListener((o, old, n) -> {
            boolean valid = n.length() >= 6;
            errorPassword.setText("⚠️ Minimum 6 caractères");
            errorPassword.setVisible(!valid);
            txtPassword.setStyle(valid ? "" : "-fx-border-color: red;");
        });

        // ✅ 4. Disable Button (Binding)
        // El bouton "Enregistrer" mayekhdem ken ki yabda kol chay mrigel
        btnEnregistrer.disableProperty().bind(
                txtNom.textProperty().isEmpty()
                        .or(txtPrenom.textProperty().isEmpty())
                        .or(txtEmail.textProperty().isEmpty())
                        .or(txtPassword.textProperty().length().lessThan(6))
                        .or(comboRole.valueProperty().isNull())
                        .or(Bindings.createBooleanBinding(() -> !txtEmail.getText().matches(emailRegex), txtEmail.textProperty()))
        );
    }

    @FXML
    void handleAdd() {
        try {
            UserApp newUser = new UserApp();
            newUser.setNom(txtNom.getText());
            newUser.setPrenom(txtPrenom.getText());
            newUser.setEmail(txtEmail.getText());
            newUser.setRole(RoleUser.valueOf(comboRole.getValue()));
            newUser.setMotDePasse(txtPassword.getText());
            newUser.setImageUrl("default-user.png");

            userService.add(newUser);
            new Alert(Alert.AlertType.INFORMATION, "✅ Utilisateur ajouté avec succès!").showAndWait();
            handleCancel();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "❌ Erreur: " + e.getMessage()).show();
        }
    }

    @FXML
    void handleCancel() {
        try {
            // 1. Njibou el root mta3 el Scene (elli houwa el BorderPane l-kbir)
            Parent layout = txtNom.getScene().getRoot();

            if (layout instanceof BorderPane mainPane) {
                // 2. Loadi ken el view mta3 el Table dakhil el center
                Parent root = FXMLLoader.load(getClass().getResource("/gui/AdminUsers.fxml"));
                mainPane.setCenter(root);
            } else {
                // 3. Fallback: ken el layout mouch BorderPane (safety), badel el scene kemla
                Parent root = FXMLLoader.load(getClass().getResource("/gui/AdminUsers.fxml"));
                Stage stage = (Stage) txtNom.getScene().getWindow();
                stage.setScene(new Scene(root));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}