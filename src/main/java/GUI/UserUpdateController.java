package GUI;

import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.UserService;
import enums.RoleUser;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class UserUpdateController {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtAge, txtExperience;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboRole, comboSpecialite, comboDispo;
    @FXML private Button btnUpdate;
    @FXML private Label errorNom, errorPrenom, errorEmail, errorPassword, errorAge, errorExperience, errorSpec, errorDispo;
    @FXML private VBox coachFieldsContainer;

    private UserApp currentUser;
    private final UserService userService = new UserService();
    private AdminUsersController parentController;
    private final String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    @FXML
    public void initialize() {
        // تعمير القوائم
        comboRole.setItems(FXCollections.observableArrayList("USER_SIMPLE", "ADMIN", "COACH"));
        comboSpecialite.setItems(FXCollections.observableArrayList("FITNESS", "YOGA", "RUNNING", "BASKETBALL"));
        comboDispo.setItems(FXCollections.observableArrayList("MATIN", "SOIR", "JOURNEE_COMPLETE"));

        // ربط الـ Labels باش ما يخليوش فراغ
        Label[] labels = {errorNom, errorPrenom, errorEmail, errorPassword, errorAge, errorExperience, errorSpec, errorDispo};
        for (Label lb : labels) { if (lb != null) lb.managedProperty().bind(lb.visibleProperty()); }

        // إظهار حقول الكوتش ديناميكياً
        coachFieldsContainer.visibleProperty().bind(comboRole.valueProperty().isEqualTo("COACH"));
        coachFieldsContainer.managedProperty().bind(coachFieldsContainer.visibleProperty());

        // ================= Validation Listeners (Strictly like Add) =================
        txtNom.textProperty().addListener((o, old, n) -> updateFieldValidation(txtNom, errorNom, n.trim().isEmpty(), "⚠️ Nom obligatoire"));
        txtPrenom.textProperty().addListener((o, old, n) -> updateFieldValidation(txtPrenom, errorPrenom, n.trim().isEmpty(), "⚠️ Prénom obligatoire"));

        txtEmail.textProperty().addListener((obs, oldV, newV) -> {
            String v = (newV == null) ? "" : newV.trim();
            if (v.isEmpty()) updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Email obligatoire");
            else if (!v.matches(emailRegex)) updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Format invalide");
            else updateFieldValidation(txtEmail, errorEmail, false, "");
        });

        txtPassword.textProperty().addListener((o, old, n) -> {
            boolean invalid = (n != null && !n.isEmpty() && n.length() < 6);
            updateFieldValidation(txtPassword, errorPassword, invalid, "⚠️ Minimum 6 caractères");
        });

        txtAge.textProperty().addListener((o, old, n) -> validateAge(n));
        txtExperience.textProperty().addListener((o, old, n) -> validateExperience(n));

        setupButtonBinding();
    }

    public void initData(UserApp user, AdminUsersController parent) {
        this.currentUser = user;
        this.parentController = parent;
        if (user != null) {
            txtNom.setText(user.getNom());
            txtPrenom.setText(user.getPrenom());
            txtEmail.setText(user.getEmail());
            if (user.getRole() != null) comboRole.setValue(user.getRole().name());

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

            if (!txtPassword.getText().isEmpty()) currentUser.setMotDePasse(txtPassword.getText());

            if (currentUser.getRole() == RoleUser.COACH) {
                currentUser.setAge(Integer.parseInt(txtAge.getText().trim()));
                currentUser.setExperience(txtExperience.getText().trim());
                currentUser.setSpecialite(comboSpecialite.getValue());
                currentUser.setDisponibilite(comboDispo.getValue());
            }

            userService.update(currentUser);
            DialogUtils.showInfo("Succès", "✅ Mise à jour réussie !");
            parentController.showUserTable();
        } catch (Exception e) {
            DialogUtils.showError("Erreur", "❌ " + e.getMessage());
        }
    }

    @FXML void handleCancel() { parentController.showUserTable(); }
}