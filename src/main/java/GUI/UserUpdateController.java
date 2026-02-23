package GUI;

import Entities.UserApp;
import Services.UserService;
import enums.RoleUser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.stream.Collectors;
import java.util.Arrays;

public class UserUpdateController {

    @FXML private TextField txtNom, txtPrenom, txtEmail;
    @FXML private PasswordField txtPassword; // Hada khassu ykoun f FXML darouri
    @FXML private ComboBox<String> comboRole;

    private UserApp currentUser;
    private UserService userService = new UserService();
    private AdminUsersController parentController;

    public void initData(UserApp user, AdminUsersController parent) {
        this.currentUser = user;
        this.parentController = parent;

        // Setup Roles
        ObservableList<String> roles = FXCollections.observableArrayList(
                Arrays.stream(RoleUser.values()).map(Enum::name).collect(Collectors.toList())
        );
        comboRole.setItems(roles);

        // Fill Data
        if (user != null) {
            if (txtNom != null) txtNom.setText(user.getNom());
            if (txtPrenom != null) txtPrenom.setText(user.getPrenom());
            if (txtEmail != null) txtEmail.setText(user.getEmail());
            if (comboRole != null && user.getRole() != null) {
                comboRole.setValue(user.getRole().name());
            }
        }
    }

    @FXML
    void handleUpdate() {
        try {
            if (comboRole.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Veuillez choisir un rôle!").show();
                return;
            }

            // Update user object
            currentUser.setNom(txtNom.getText());
            currentUser.setPrenom(txtPrenom.getText());
            currentUser.setEmail(txtEmail.getText());
            currentUser.setRole(RoleUser.valueOf(comboRole.getValue()));

            // Password update logic (if not empty)
            if (txtPassword != null) {
                String newPassword = txtPassword.getText();
                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    currentUser.setMotDePasse(newPassword);
                }
            }

            userService.update(currentUser);
            new Alert(Alert.AlertType.INFORMATION, "✅ Utilisateur mis à jour !").showAndWait();

            if (parentController != null) {
                parentController.loadUserData();
                parentController.showUserTable();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }

    @FXML
    void handleCancel() {
        if (parentController != null) {
            parentController.showUserTable();
        }
    }


    }
