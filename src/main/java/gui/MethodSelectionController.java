package GUI;

import Entities.UserApp;
import Utiles.MailService;
import Utiles.SmsService; // El class elli 3malneha b-Vonage wala Twilio
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class MethodSelectionController {

    @FXML private VBox btnEmail;
    @FXML private VBox btnSMS;

    private UserApp currentUser;
    private String generatedCode;

    /**
     * Hedhi el méthode elli n-nadouha mel LoginController
     */
    public void initData(UserApp user) {
        this.currentUser = user;
        // Generi code fih 4 ar9am
        this.generatedCode = String.valueOf((int) (Math.random() * 9000) + 1000);
        System.out.println("DEBUG - Code Généré: " + generatedCode);
    }

    @FXML
    void selectEmail() {
        if (currentUser.getEmail() != null) {
            // 1. Envoi Mail
            MailService.sendOTP(currentUser.getEmail(), generatedCode);

            // 2. Aller vers la vérification
            goToVerifyCode();
        }
    }

    @FXML
    void selectSMS() {
        String phone = currentUser.getTelephone();
        if (phone != null && !phone.isEmpty()) {
            // 1. Formatage du numéro (216...)
            String formattedPhone = phone.startsWith("216") ? phone : "216" + phone;

            // 2. Envoi SMS (via Vonage ou autre)
            SmsService.sendOTP(formattedPhone, generatedCode);

            // 3. Aller vers la vérification
            goToVerifyCode();
        } else {
            showAlert("Erreur", "Cet utilisateur n'a pas de numéro de téléphone enregistré.");
        }
    }

    private void goToVerifyCode() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ForgotPassword.fxml"));
            Parent root = loader.load();

            // Passi el data lel ForgotPasswordController
            ForgotPasswordController controller = loader.getController();
            controller.initData(currentUser, generatedCode);

            // Change Scene
            Stage stage = (Stage) btnEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface de vérification.");
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        // Skar el popup wala erja3 lel login
        Stage stage = (Stage) btnEmail.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}