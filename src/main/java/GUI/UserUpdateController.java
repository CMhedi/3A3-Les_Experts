package GUI;

import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.interfaces.UserService;
import enums.RoleUser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.stream.Collectors;
import java.util.Arrays;

public class UserUpdateController {

    @FXML private TextField txtNom, txtPrenom, txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnTogglePassword;
    @FXML private ComboBox<String> comboRole;

    private UserApp currentUser;
    private UserService userService = new UserService();
    private AdminUsersController parentController;
    private boolean isPasswordVisible = false;

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
            txtNom.setText(user.getNom());
            txtPrenom.setText(user.getPrenom());
            txtEmail.setText(user.getEmail());

            // وضع كلمة السر الحالية في الحقلين
            txtPassword.setText(user.getMotDePasse());
            txtPasswordVisible.setText(user.getMotDePasse());

            if (user.getRole() != null) {
                comboRole.setValue(user.getRole().name());
            }
        }

        // ربط الحقلين ببعضهم (أي تغيير في أحدهما ينتقل للآخر)
        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());
    }

    @FXML
    void togglePassword() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            txtPasswordVisible.setVisible(true);
            txtPassword.setVisible(false);
            btnTogglePassword.setText("🙈"); // أيقونة الإخفاء
        } else {
            txtPasswordVisible.setVisible(false);
            txtPassword.setVisible(true);
            btnTogglePassword.setText("👁"); // أيقونة الإظهار
        }
    }

    @FXML
    void handleUpdate() {
        try {
            if (comboRole.getValue() == null) {
                DialogUtils.showWarning("Attention", "Veuillez choisir un rôle !");
                return;
            }

            currentUser.setNom(txtNom.getText());
            currentUser.setPrenom(txtPrenom.getText());
            currentUser.setEmail(txtEmail.getText());
            currentUser.setRole(RoleUser.valueOf(comboRole.getValue()));

            String updatedPassword = txtPassword.getText();
            if (updatedPassword != null && !updatedPassword.trim().isEmpty()) {
                currentUser.setMotDePasse(updatedPassword);
            }

            userService.update(currentUser);
            DialogUtils.showInfo("Succès", "✅ Utilisateur mis à jour avec succès !");
            if (parentController != null) {
                parentController.loadUserData();
                parentController.showUserTable();
            }

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible de mettre à jour : " + e.getMessage());        }
    }

    @FXML
    void handleCancel() {
        if (parentController != null) {
            parentController.showUserTable();
        }
    }
}