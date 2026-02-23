package GUI;
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

        if (mdp1.isEmpty() || mdp1.length() < 4) {
            new Alert(Alert.AlertType.WARNING, "Mot de passe trop court !").show();
            return;
        }

        if (!mdp1.equals(mdp2)) {
            new Alert(Alert.AlertType.ERROR, "Les mots de passe ne correspondent pas !").show();
            return;
        }

        try {
            //  UPDATE fil Database
            us.updatePassword(userEmail, mdp1);
            new Alert(Alert.AlertType.INFORMATION, "Succès ! Votre mot de passe a été réinitialisé.").show();


            Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));
            Stage stage = (Stage) txtNewPass.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour : " + e.getMessage()).show();
        }
    }
}