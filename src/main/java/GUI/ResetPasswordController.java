package GUI;
import GUI.utils.DialogUtils;
import Services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class ResetPasswordController {

    @FXML private PasswordField txtNewPass, txtConfirmPass;
    private String userEmail;
    private UserService us = new UserService();

    public void setUserEmail(String email) {
        this.userEmail = email;
    }

    @FXML
    void handleReset(ActionEvent event) {
        String mdp1 = txtNewPass.getText();
        String mdp2 = txtConfirmPass.getText();

        if (mdp1.isEmpty() || mdp1.length() < 6) {
            DialogUtils.showWarning("Attention", "⚠️ Mot de passe trop court (min 6 caractères) !");
            return;
        }

        if (!mdp1.equals(mdp2)) {
            DialogUtils.showError("Erreur de saisie", "❌ Les mots de passe ne correspondent pas !");
            return;
        }

        try {
            us.updatePassword(userEmail, mdp1);

            DialogUtils.showInfo("Succès", "✅ Votre mot de passe a été réinitialisé avec succès.");

            Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
            Stage stage = (Stage) txtNewPass.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            DialogUtils.showError("Erreur système", "❌ Erreur lors de la mise à jour : " + e.getMessage());
        }
    }
}