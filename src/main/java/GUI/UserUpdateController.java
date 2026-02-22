package GUI;

import Entities.UserApp;
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
    @FXML private PasswordField txtPassword;         // الحقل المخفي (النقاط)
    @FXML private TextField txtPasswordVisible;      // الحقل الظاهر (الكتيبة)
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
                new Alert(Alert.AlertType.WARNING, "Veuillez choisir un rôle!").show();
                return;
            }

            // تحديث الكائن currentUser
            currentUser.setNom(txtNom.getText());
            currentUser.setPrenom(txtPrenom.getText());
            currentUser.setEmail(txtEmail.getText());
            currentUser.setRole(RoleUser.valueOf(comboRole.getValue()));

            // تحديث كلمة السر (بما أنهما مرتبطان، نأخذ القيمة من أي منهما)
            String updatedPassword = txtPassword.getText();
            if (updatedPassword != null && !updatedPassword.trim().isEmpty()) {
                currentUser.setMotDePasse(updatedPassword);
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