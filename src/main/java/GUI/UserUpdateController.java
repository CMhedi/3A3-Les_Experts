package GUI;

import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.UserService;
import enums.RoleUser;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;

public class UserUpdateController {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtAge, txtExperience, txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboRole, comboSpecialite, comboDispo;
    @FXML private Button btnUpdate;
    @FXML private Label errorNom, errorPrenom, errorEmail, errorPassword, errorAge, errorExperience, errorSpec, errorDispo;
    @FXML private VBox coachFieldsContainer;
    @FXML private Label lblLargeInitials, lblFullName, lblBadgeRole;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnShowPass;
    private boolean isPasswordVisible = false;
    private UserApp currentUser;
    private final UserService userService = new UserService();
    private AdminUsersController parentController;
    private final String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    @FXML
    public void initialize() {
        comboRole.setItems(FXCollections.observableArrayList("USER_SIMPLE", "ADMIN", "COACH"));
        comboSpecialite.setItems(FXCollections.observableArrayList("FITNESS", "YOGA", "RUNNING", "BASKETBALL"));
        comboDispo.setItems(FXCollections.observableArrayList("MATIN", "SOIR", "JOURNEE_COMPLETE"));

        Label[] labels = {errorNom, errorPrenom, errorEmail, errorPassword, errorAge, errorExperience, errorSpec, errorDispo};
        for (Label lb : labels) {
            if (lb != null) lb.managedProperty().bind(lb.visibleProperty());
        }

        coachFieldsContainer.visibleProperty().bind(comboRole.valueProperty().isEqualTo("COACH"));
        coachFieldsContainer.managedProperty().bind(coachFieldsContainer.visibleProperty());

        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

        txtNom.textProperty().addListener((o, old, n) -> updateFieldValidation(txtNom, errorNom, n.trim().isEmpty(), "⚠️ Nom obligatoire"));
        txtPrenom.textProperty().addListener((o, old, n) -> updateFieldValidation(txtPrenom, errorPrenom, n.trim().isEmpty(), "⚠️ Prénom obligatoire"));

        txtEmail.textProperty().addListener((obs, oldV, newV) -> {
            String v = (newV == null) ? "" : newV.trim();
            if (v.isEmpty()) updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Email obligatoire");
            else if (!v.matches(emailRegex)) updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Format invalide");
            else updateFieldValidation(txtEmail, errorEmail, false, "");
        });

        txtPassword.textProperty().addListener((o, old, n) -> {
            boolean isInvalid = (n != null && !n.isEmpty() && n.length() < 6);

            updateFieldValidation(txtPassword, errorPassword, isInvalid, "⚠️ Minimum 6 caractères");
            updateFieldValidation(txtPasswordVisible, errorPassword, isInvalid, "⚠️ Minimum 6 caractères");
        });

        txtAge.textProperty().addListener((o, old, n) -> validateAge(n));
        txtExperience.textProperty().addListener((o, old, n) -> validateExperience(n));

        setupButtonBinding();
    }

    @FXML
    void togglePassword(ActionEvent event) {
        if (!isPasswordVisible) {
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            btnShowPass.setText("🔒");
            isPasswordVisible = true;
        } else {
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            btnShowPass.setText("👁");
            isPasswordVisible = false;
        }
    }

    public void initData(UserApp user, AdminUsersController parent) {
        this.currentUser = user;
        this.parentController = parent;

        if (user != null) {
            txtNom.setText(user.getNom());
            txtPrenom.setText(user.getPrenom());
            txtEmail.setText(user.getEmail());
            txtPhone.setText(user.getTelephone() != null ? user.getTelephone() : "");

            // Populate Identity Card
            String initials = (user.getNom() != null && !user.getNom().isEmpty()) ? user.getNom().substring(0, 1).toUpperCase() : "";
            if (user.getPrenom() != null && !user.getPrenom().isEmpty()) {
                initials = user.getPrenom().substring(0, 1).toUpperCase() + initials;
            }
            lblLargeInitials.setText(initials);
            lblFullName.setText(user.getPrenom() + " " + user.getNom());
            lblBadgeRole.setText(user.getRole() != null ? user.getRole().name() : "USER");

            txtPassword.setText("");
            txtPassword.setPromptText("Laisser vide pour ne pas changer");
            txtPasswordVisible.setText("");
            txtPasswordVisible.setPromptText("Laisser vide pour ne pas changer");

            if (user.getRole() != null) {
                comboRole.setValue(user.getRole().name());
            }

            if (user.getRole() == RoleUser.COACH) {
                txtAge.setText(String.valueOf(user.getAge()));
                txtExperience.setText(user.getExperience());
                comboSpecialite.setValue(user.getSpecialite());
                comboDispo.setValue(user.getDisponibilite());
            }
        }
    }

    private void updateFieldValidation(Control field, Label label, boolean isInvalid, String message) {
        if (isInvalid) {
            label.setText(message);
            label.setVisible(true);
            field.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: #fef2f2;");
        } else {
            label.setVisible(false);
            field.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-color: #f8fafc;");
        }
    }

    private void validateAge(String n) {
        if (!"COACH".equals(comboRole.getValue())) return;
        if (n.isEmpty()) { updateFieldValidation(txtAge, errorAge, true, "⚠️ Âge requis"); return; }
        try {
            int age = Integer.parseInt(n.trim());
            updateFieldValidation(txtAge, errorAge, age < 18 || age > 40, "⚠️ 18-40 ans");
        } catch (Exception e) { updateFieldValidation(txtAge, errorAge, true, "⚠️ Nombre requis"); }
    }

    private void validateExperience(String n) {
        if (!"COACH".equals(comboRole.getValue())) return;
        if (n.isEmpty()) { updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Expérience requise"); return; }
        // Logic بسيط: الـ Experience لازم تكون أرقام وأصغر من (العمر - 18)
        try {
            int exp = Integer.parseInt(n.trim());
            int age = Integer.parseInt(txtAge.getText().trim());
            updateFieldValidation(txtExperience, errorExperience, exp < 0 || exp > (age - 18), "⚠️ Expérience invalide");
        } catch (Exception e) { updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Chiffres uniquement"); }
    }

    private void setupButtonBinding() {
        btnUpdate.disableProperty().bind(
                txtNom.textProperty().isEmpty()
                        .or(txtPrenom.textProperty().isEmpty())
                        .or(txtEmail.textProperty().isEmpty())
                        .or(errorEmail.visibleProperty())
                        .or(errorNom.visibleProperty())
                        .or(errorPassword.visibleProperty())
                        .or(Bindings.createBooleanBinding(() -> {
                                    if ("COACH".equals(comboRole.getValue())) {
                                        return txtAge.getText().trim().isEmpty() || txtExperience.getText().trim().isEmpty()
                                                || errorAge.isVisible() || errorExperience.isVisible()
                                                || comboSpecialite.getValue() == null || comboDispo.getValue() == null;
                                    }
                                    return false;
                                }, comboRole.valueProperty(), txtAge.textProperty(), txtExperience.textProperty(),
                                errorAge.visibleProperty(), errorExperience.visibleProperty(),
                                comboSpecialite.valueProperty(), comboDispo.valueProperty()))
        );
    }

    @FXML
    void handleUpdate() {
        try {
            currentUser.setNom(txtNom.getText().trim());
            currentUser.setPrenom(txtPrenom.getText().trim());
            currentUser.setEmail(txtEmail.getText().trim());
            currentUser.setRole(RoleUser.valueOf(comboRole.getValue()));

            String newPass = txtPassword.getText();
            if (newPass != null && !newPass.isEmpty()) {
                String hashed = BCrypt.hashpw(newPass, BCrypt.gensalt());
                currentUser.setMotDePasse(hashed);
            }

            if (currentUser.getRole() == RoleUser.COACH) {
                currentUser.setAge(Integer.parseInt(txtAge.getText().trim()));
                currentUser.setExperience(txtExperience.getText().trim());
                currentUser.setSpecialite(comboSpecialite.getValue());
                currentUser.setDisponibilite(comboDispo.getValue());
            }

            userService.update(currentUser);
            GUI.utils.DialogUtils.showInfo("Succès", "✅ Mise à jour réussie !");
            parentController.showUserTable();

        } catch (Exception e) {
            GUI.utils.DialogUtils.showError("Erreur", "❌ " + e.getMessage());
        }
    }

    @FXML void handleCancel() { parentController.showUserTable(); }
}