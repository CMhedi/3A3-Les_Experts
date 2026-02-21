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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Arrays;

public class UserAddController {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtAge, txtExperience;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboRole, comboSpecialite, comboDispo;
    @FXML private Button btnEnregistrer;
    @FXML private Label errorNom, errorPrenom, errorEmail, errorPassword, errorAge, errorSpec, errorDispo,errorExperience;
    @FXML private VBox coachFieldsContainer;

    private UserService userService = new UserService();
    private final String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    @FXML
    public void initialize() {
        // 1. تعبئة القوائم
        comboRole.setItems(FXCollections.observableArrayList("USER_SIMPLE", "ADMIN", "COACH"));
        comboSpecialite.setItems(FXCollections.observableArrayList("FITNESS", "YOGA", "RUNNING", "BASKETBALL"));
        comboDispo.setItems(FXCollections.observableArrayList("MATIN", "SOIR", "JOURNEE_COMPLETE"));

        // 2. تفعيل الـ ManagedProperty لجميع الـ Labels لضمان عدم تهليلك الـ Design
        Label[] labels = {errorNom, errorPrenom, errorEmail, errorPassword, errorAge, errorSpec, errorDispo, errorExperience};
        for (Label lb : labels) {
            if (lb != null) {
                lb.managedProperty().bind(lb.visibleProperty());
            }
        }

        // 3. مستمعات التحقق (Listeners)
        txtNom.textProperty().addListener((o, old, n) ->
                updateFieldValidation(txtNom, errorNom, n.trim().isEmpty(), "⚠️ Nom obligatoire"));

        txtPrenom.textProperty().addListener((o, old, n) ->
                updateFieldValidation(txtPrenom, errorPrenom, n.trim().isEmpty(), "⚠️ Prénom obligatoire"));

        txtEmail.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.isEmpty()) {
                updateFieldValidation(txtEmail, errorEmail, true, "⚠️ L'email est obligatoire");
            } else if (!newV.contains("@")) {
                updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Il manque le symbole '@'");
            } else if (!newV.contains(".")) {
                updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Il manque le point '.'");
            } else if (!newV.matches(emailRegex)) {
                updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Format invalide");
            } else {
                updateFieldValidation(txtEmail, errorEmail, false, "");
            }
        });

        txtPassword.textProperty().addListener((o, old, n) ->
                updateFieldValidation(txtPassword, errorPassword, n.length() < 6, "⚠️ Minimum 6 caractères"));

        txtAge.textProperty().addListener((o, old, n) -> {
            try {
                int age = Integer.parseInt(n);
                updateFieldValidation(txtAge, errorAge, age < 18 || age > 40, "⚠️ Âge entre 18-40");
            } catch (Exception e) {
                updateFieldValidation(txtAge, errorAge, true, "⚠️ Entrez un nombre");
            }
        });
        txtExperience.textProperty().addListener((obs, oldV, newV) -> {
            String ageText = txtAge.getText();

            // 1. التحقق إذا كان العمر فارغ
            if (ageText.isEmpty()) {
                updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Saisissez d'abord l'âge");
                return;
            }

            try {
                int ageVal = Integer.parseInt(ageText);
                int ageActif = ageVal - 18;

                // 2. التحقق من المدخلات
                if (newV.isEmpty()) {
                    updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Expérience obligatoire");
                } else if (!newV.matches("\\d+")) {
                    updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Utilisez uniquement des chiffres");
                } else {
                    int expVal = Integer.parseInt(newV);

                    if (expVal < 0) {
                        updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Ne يمكن pas être négative");
                    } else if (expVal > ageActif) {
                        // الميساج الديناميكي متاعك
                        String msg = "⚠️ Max " + ageActif + " ans (âge actif: " + ageVal + "-18)";
                        updateFieldValidation(txtExperience, errorExperience, true, msg);
                    } else {
                        // كل شيء مريغل
                        updateFieldValidation(txtExperience, errorExperience, false, "");
                    }
                }
            } catch (NumberFormatException e) {
                updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Âge invalide");
            }
        });

        // 4. منطق الكوتش
        coachFieldsContainer.visibleProperty().bind(comboRole.valueProperty().isEqualTo("COACH"));
        coachFieldsContainer.managedProperty().bind(coachFieldsContainer.visibleProperty());

        setupButtonBinding();
    }

    private void updateFieldValidation(Control field, Label label, boolean isInvalid, String message) {
        if (isInvalid) {
            if (label != null) {
                label.setText(message);
                label.setVisible(true);
                label.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
            field.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: #fef2f2; -fx-text-fill: #1e293b;");
        } else {
            if (label != null) label.setVisible(false);
            field.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-color: #f8fafc; -fx-text-fill: #1e293b;");
        }
    }
    private void setupButtonBinding() {
        btnEnregistrer.disableProperty().bind(
                txtNom.textProperty().isEmpty()
                        .or(txtEmail.textProperty().isEmpty())
                        .or(errorEmail.visibleProperty())
                        .or(errorNom.visibleProperty())
                        .or(txtPassword.textProperty().length().lessThan(6))
                        .or(comboRole.valueProperty().isNull())
                        .or(Bindings.createBooleanBinding(() -> {
                            if ("COACH".equals(comboRole.getValue())) {
                                // إذا كان كوتش، لازم يثبت في غلطات العمر والخبرة
                                return errorAge.isVisible() || errorExperience.isVisible() || comboSpecialite.getValue() == null;
                            }
                            return false;
                        }, comboRole.valueProperty(), errorAge.visibleProperty(), errorExperience.visibleProperty(), comboSpecialite.valueProperty()))
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
            Scene scene = btnEnregistrer.getScene();
            if (scene.getRoot() instanceof BorderPane mainPane) {
                Parent root = FXMLLoader.load(getClass().getResource("/gui/AdminUsers.fxml"));
                mainPane.setCenter(root);
            } else {
                Parent root = FXMLLoader.load(getClass().getResource("/gui/AdminUsers.fxml"));
                Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}