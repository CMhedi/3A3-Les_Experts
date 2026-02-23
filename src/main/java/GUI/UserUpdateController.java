package GUI;

import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.UserService;
import enums.RoleUser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UserUpdateController {

    @FXML private TextField txtNom, txtPrenom, txtEmail;

    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnTogglePassword;

    @FXML private ComboBox<String> comboRole;

    private UserApp currentUser;
    private final UserService userService = new UserService();
    private AdminUsersController parentController;

    private boolean isPasswordVisible = false;

    public void initData(UserApp user, AdminUsersController parent) {
        this.currentUser = user;
        this.parentController = parent;

        // ✅ Setup roles
        ObservableList<String> roles = FXCollections.observableArrayList(
                Arrays.stream(RoleUser.values()).map(Enum::name).collect(Collectors.toList())
        );
        comboRole.setItems(roles);

        // ✅ Fill data
        if (user != null) {
            txtNom.setText(nvl(user.getNom()));
            txtPrenom.setText(nvl(user.getPrenom()));
            txtEmail.setText(nvl(user.getEmail()));

            // ⚠️ ما نحطّوش mdp hashed من DB في الحقول (security + confusion)
            txtPassword.clear();
            txtPasswordVisible.clear();

            if (user.getRole() != null) comboRole.setValue(user.getRole().name());
        }

        // ✅ Bind visible/unvisible password fields
        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());

        // ✅ initial state
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);
        txtPassword.setVisible(true);
        txtPassword.setManaged(true);
        btnTogglePassword.setText("👁");
        isPasswordVisible = false;
    }

    @FXML
    void togglePassword() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);

            txtPassword.setVisible(false);
            txtPassword.setManaged(false);

            btnTogglePassword.setText("🙈");
            txtPasswordVisible.requestFocus();
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
        } else {
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);

            txtPassword.setVisible(true);
            txtPassword.setManaged(true);

            btnTogglePassword.setText("👁");
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
        }
    }

    @FXML
    void handleUpdate() {
        try {
            if (currentUser == null) {
                DialogUtils.showError("Erreur", "Utilisateur introuvable.");
                return;
            }
            if (comboRole.getValue() == null) {
                DialogUtils.showWarning("Attention", "Veuillez choisir un rôle !");
                return;
            }

            currentUser.setNom(nvl(txtNom.getText()).trim());
            currentUser.setPrenom(nvl(txtPrenom.getText()).trim());
            currentUser.setEmail(nvl(txtEmail.getText()).trim());
            currentUser.setRole(RoleUser.valueOf(comboRole.getValue()));

            // ✅ mot de passe: update seulement si user a saisi quelque chose
            String updatedPassword = txtPassword.getText();
            if (updatedPassword != null && !updatedPassword.trim().isEmpty()) {
                currentUser.setMotDePasse(updatedPassword.trim());
                // hashing يتعمل في UserService.update() (كيما عطيتك قبل)
            }

            userService.update(currentUser);

            DialogUtils.showInfo("Succès", "✅ Utilisateur mis à jour avec succès !");

            if (parentController != null) {
                parentController.loadUserData();
                parentController.showUserTable();
            }

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible de mettre à jour : " + e.getMessage());
        }
    }

    @FXML
    void handleCancel() {
        if (parentController != null) {
            parentController.showUserTable();
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}