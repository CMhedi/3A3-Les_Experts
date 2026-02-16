package gui;

import Utiles.MailService;
import Entities.UserApp;
import Services.interfaces.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    @FXML private Label lblEmailDisplay;
    @FXML private Button btnAction;
    @FXML private TextField f1, f2, f3, f4;

    private String correctCode;
    private UserApp currentUser; // ✅ Behr n-asta3mlouh fil handleAction
    private UserService us = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addTextLimiter(f1, f2);
        addTextLimiter(f2, f3);
        addTextLimiter(f3, f4);
        addTextLimiter(f4, null);
    }

    private void addTextLimiter(TextField tf, TextField nextTf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 1) {
                tf.setText(newVal.substring(0, 1));
            }
            if (newVal.length() == 1 && nextTf != null) {
                nextTf.requestFocus();
            }
        });
    }

    // ✅ HEDHI EL METHODE EL MOHEMMA
    public void initData(UserApp user, String code) {
        this.currentUser = user;  // 👈 Salla7na hedhi (bech maykounch null)
        this.correctCode = code;

        if (lblEmailDisplay != null && user != null) {
            lblEmailDisplay.setText(user.getEmail());
        }
        System.out.println("DEBUG: Interface prête pour " + user.getEmail() + " avec code: " + code);
    }

    @FXML
    void handleAction() {
        String enteredCode = f1.getText() + f2.getText() + f3.getText() + f4.getText();

        if (correctCode != null && enteredCode.equals(correctCode)) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ResetPassword.fxml"));
                Parent root = loader.load();

                ResetPasswordController controller = loader.getController();

                // ✅ Tawa currentUser ma3adech null
                controller.setUserEmail(currentUser.getEmail());

                Stage stage = (Stage) btnAction.getScene().getWindow();
                stage.setScene(new Scene(root));

            } catch (IOException e) {
                System.err.println("Erreur redirection ResetPassword: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            new Alert(Alert.AlertType.ERROR, "Code incorrect !").show();
        }
    }

    @FXML
    void handleBackToLogin(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Login.fxml"));
            Stage stage = (Stage) btnAction.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}