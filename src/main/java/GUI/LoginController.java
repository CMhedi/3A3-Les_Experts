package GUI;

import Entities.Session;
import Entities.UserApp;
<<<<<<< HEAD
import Services.UserService;
=======
import GUI.utils.DialogUtils;
import Services.interfaces.UserService;
>>>>>>> origin/salma_integration
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
<<<<<<< HEAD

=======
import org.mindrot.jbcrypt.BCrypt;
>>>>>>> origin/salma_integration
public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtMdp;
    @FXML private TextField txtMdpVisible;
    @FXML private Button btnToggleMdp;

    private boolean isMdpVisible = false;
    private UserService us = new UserService();

    @FXML
    public void initialize() {
        txtMdp.textProperty().bindBidirectional(txtMdpVisible.textProperty());
    }

    @FXML
    void togglePassword() {
        isMdpVisible = !isMdpVisible;
        if (isMdpVisible) {
            txtMdpVisible.setVisible(true);
            txtMdp.setVisible(false);
            btnToggleMdp.setText("🙈");
        } else {
            txtMdpVisible.setVisible(false);
            txtMdp.setVisible(true);
            btnToggleMdp.setText("👁");
        }
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = txtEmail.getText();
        String mdp = txtMdp.getText();

        if (email.isEmpty() || mdp.isEmpty()) {
            DialogUtils.showWarning(
                    "Champs manquants",
                    "Veuillez remplir tous les champs !"
            );            return;
        }

        try {
            UserApp user = us.findByEmail(email);

            if (user != null && BCrypt.checkpw(mdp, user.getMotDePasse())) {                // ✅ Stocker session

                Session.setConnectedUser(user);

                String fxmlPath = "";
                switch (user.getRole()) {
                    case ADMIN: fxmlPath = "/GUI/MainLayout.fxml"; break;
                    case USER_SIMPLE: fxmlPath = "/GUI/MainLayoutUser.fxml"; break;
                    case COACH: fxmlPath = "/GUI/MainLayoutcoach.fxml"; break;
                    default: fxmlPath = "/GUI/SuccessPage.fxml";
                }

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
                stage.show();

            } else {
                DialogUtils.showError("Échec de connexion", "Email ou Mot de passe incorrect !");
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur système", "Une erreur est survenue lors du login : " + e.getMessage());
        }
    }

    @FXML
    void goToRegister(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/UserManagement.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("❌ Erreur redirection Register: " + e.getMessage());
        }
    }

    @FXML
    void handleForgotPassword() {
        String email = txtEmail.getText();
        UserApp user = us.findByEmail(email);

        if (user != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MethodSelection.fxml"));
                Parent root = loader.load();
                MethodSelectionController controller = loader.getController();
                controller.initData(user);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) { e.printStackTrace(); }
        } else {
            DialogUtils.showWarning(
                    "Email Manquant",
                    "Veuillez saisir un email valide d'abord."
            );
        }
    }
}

