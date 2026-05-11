package GUI;

import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.UserService;
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

public class UserAddController {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtAge, txtExperience, txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboRole, comboSpecialite, comboDispo;
    @FXML private Button btnEnregistrer;
    @FXML private Label errorNom, errorPrenom, errorEmail, errorPassword, errorAge, errorSpec, errorDispo, errorExperience;
    @FXML private VBox coachFieldsContainer;

    private final UserService userService = new UserService();
    private final String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    @FXML
    public void initialize() {

        comboRole.setItems(FXCollections.observableArrayList("USER_SIMPLE", "ADMIN", "COACH"));
        comboSpecialite.setItems(FXCollections.observableArrayList("FITNESS", "YOGA", "RUNNING", "BASKETBALL"));
        comboDispo.setItems(FXCollections.observableArrayList("MATIN", "SOIR", "JOURNEE_COMPLETE"));

        Label[] labels = {errorNom, errorPrenom, errorEmail, errorPassword, errorAge, errorSpec, errorDispo, errorExperience};
        for (Label lb : labels) {
            if (lb != null) lb.managedProperty().bind(lb.visibleProperty());
        }

        // ================= Validation =================
        txtNom.textProperty().addListener((o, old, n) ->
                updateFieldValidation(txtNom, errorNom, n.trim().isEmpty(), "⚠️ Nom obligatoire"));

        txtPrenom.textProperty().addListener((o, old, n) ->
                updateFieldValidation(txtPrenom, errorPrenom, n.trim().isEmpty(), "⚠️ Prénom obligatoire"));

        txtEmail.textProperty().addListener((obs, oldV, newV) -> {
            String v = (newV == null) ? "" : newV.trim();
            if (v.isEmpty()) {
                updateFieldValidation(txtEmail, errorEmail, true, "⚠️ L'email est obligatoire");
            } else if (!v.contains("@")) {
                updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Il manque le symbole '@'");
            } else if (!v.contains(".")) {
                updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Il manque le point '.'");
            } else if (!v.matches(emailRegex)) {
                updateFieldValidation(txtEmail, errorEmail, true, "⚠️ Format invalide");
            } else {
                updateFieldValidation(txtEmail, errorEmail, false, "");
            }
        });

        txtPassword.textProperty().addListener((o, old, n) ->
                updateFieldValidation(txtPassword, errorPassword, n == null || n.length() < 6, "⚠️ Minimum 6 caractères"));

        txtAge.textProperty().addListener((o, old, n) -> {
            // âge فقط للـ COACH
            if (!"COACH".equals(comboRole.getValue())) {
                if (errorAge != null) errorAge.setVisible(false);
                txtAge.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-color: #f8fafc; -fx-text-fill: #1e293b;");
                return;
            }
            if (n == null || n.trim().isEmpty()) {
                updateFieldValidation(txtAge, errorAge, true, "⚠️ Âge obligatoire");
                return;
            }
            try {
                int age = Integer.parseInt(n.trim());
                updateFieldValidation(txtAge, errorAge, age < 18 || age > 40, "⚠️ Âge entre 18-40");
            } catch (Exception e) {
                updateFieldValidation(txtAge, errorAge, true, "⚠️ Entrez un nombre");
            }
        });

        txtExperience.textProperty().addListener((obs, oldV, newV) -> {
            // experience فقط للـ COACH
            if (!"COACH".equals(comboRole.getValue())) {
                if (errorExperience != null) errorExperience.setVisible(false);
                txtExperience.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-color: #f8fafc; -fx-text-fill: #1e293b;");
                return;
            }

            String ageText = (txtAge.getText() == null) ? "" : txtAge.getText().trim();
            String expText = (newV == null) ? "" : newV.trim();

            if (ageText.isEmpty()) {
                updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Saisissez d'abord l'âge");
                return;
            }

            try {
                int ageVal = Integer.parseInt(ageText);
                int ageActif = ageVal - 18;

                if (expText.isEmpty()) {
                    updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Expérience obligatoire");
                } else if (!expText.matches("\\d+")) {
                    updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Utilisez uniquement des chiffres");
                } else {
                    int expVal = Integer.parseInt(expText);

                    if (expVal < 0) {
                        updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Ne peut pas être négative");
                    } else if (expVal > ageActif) {
                        String msg = "⚠️ Max " + ageActif + " ans (âge actif: " + ageVal + "-18)";
                        updateFieldValidation(txtExperience, errorExperience, true, msg);
                    } else {
                        updateFieldValidation(txtExperience, errorExperience, false, "");
                    }
                }
            } catch (NumberFormatException e) {
                updateFieldValidation(txtExperience, errorExperience, true, "⚠️ Âge invalide");
            }
        });

        // ================= Coach fields visible only if COACH =================
        coachFieldsContainer.visibleProperty().bind(comboRole.valueProperty().isEqualTo("COACH"));
        coachFieldsContainer.managedProperty().bind(coachFieldsContainer.visibleProperty());

        // ✅ re-check coach validation when role changes
        comboRole.valueProperty().addListener((obs, o, n) -> {
            if (!"COACH".equals(n)) {
                if (errorAge != null) errorAge.setVisible(false);
                if (errorExperience != null) errorExperience.setVisible(false);
            }
        });

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
                        .or(txtPrenom.textProperty().isEmpty())
                        .or(txtEmail.textProperty().isEmpty())
                        .or(txtPassword.textProperty().length().lessThan(6))
                        .or(comboRole.valueProperty().isNull())
                        .or(errorEmail.visibleProperty())
                        .or(errorNom.visibleProperty())
                        .or(errorPrenom.visibleProperty())
                        .or(errorPassword.visibleProperty())
                        .or(Bindings.createBooleanBinding(() -> {
                                    if ("COACH".equals(comboRole.getValue())) {
                                        boolean specMissing = comboSpecialite.getValue() == null;
                                        boolean dispoMissing = comboDispo.getValue() == null;
                                        return (errorAge != null && errorAge.isVisible())
                                                || (errorExperience != null && errorExperience.isVisible())
                                                || specMissing
                                                || dispoMissing;
                                    }
                                    return false;
                                }, comboRole.valueProperty(),
                                comboSpecialite.valueProperty(),
                                comboDispo.valueProperty(),
                                errorAge.visibleProperty(),
                                errorExperience.visibleProperty()))
        );
    }

    @FXML
    void handleAdd() {
        try {
            UserApp newUser = new UserApp();
            newUser.setNom(txtNom.getText().trim());
            newUser.setPrenom(txtPrenom.getText().trim());
            newUser.setEmail(txtEmail.getText().trim());
            newUser.setTelephone(txtPhone.getText() != null ? txtPhone.getText().trim() : "");
            newUser.setRole(RoleUser.valueOf(comboRole.getValue()));
            newUser.setMotDePasse(txtPassword.getText()); // hashing يتحلّ في UserService.add()

            // ✅ Coach fields (only if coach)
            if (newUser.getRole() == RoleUser.COACH) {
                newUser.setAge(parseIntOrZero(txtAge.getText()));
                newUser.setExperience(txtExperience.getText() == null ? "" : txtExperience.getText().trim());
                newUser.setSpecialite(comboSpecialite.getValue());
                newUser.setDisponibilite(comboDispo.getValue());
            }

            userService.add(newUser);

            DialogUtils.showInfo("Succès", "✅ Utilisateur ajouté avec succès!");
            handleCancel();

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "❌ Erreur: " + e.getMessage());
        }
    }

    private int parseIntOrZero(String v) {
        try {
            if (v == null || v.trim().isEmpty()) return 0;
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @FXML
    void handleCancel() {
        try {
            Scene scene = btnEnregistrer.getScene();
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/AdminUsers.fxml"));

            if (scene.getRoot() instanceof BorderPane mainPane) {
                mainPane.setCenter(root);
            } else {
                Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}