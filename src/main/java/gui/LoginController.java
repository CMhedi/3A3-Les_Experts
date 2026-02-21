package GUI;

import Entities.Session;
import Entities.UserApp;
import Services.interfaces.UserService;
import Utiles.MailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtMdp;

    private UserService us = new UserService();

    @FXML
    void handleLogin(ActionEvent event) {

        String email = txtEmail.getText();
        String mdp = txtMdp.getText();

        if (email.isEmpty() || mdp.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez remplir tous les champs !").show();
            return;
        }

        try {

            UserApp user = us.findByEmail(email);

            if (user != null &&
                    user.getMotDePasse().equals(mdp)) {

                // ✅ Stocker session
                Session.setConnectedUser(user);

                System.out.println("✅ Login success: "
                        + user.getNom()
                        + " (Role: "
                        + user.getRole() + ")");

                String fxmlPath = "";
                String cssPath = null;

                switch (user.getRole()) {

                    case ADMIN:
                        fxmlPath = "/gui/MainLayout.fxml";
                        break;

                    case USER_SIMPLE:
                        fxmlPath = "/gui/MainLayoutUser.fxml";
                        break;

                    case COACH:
                        fxmlPath = "/gui/MainLayoutcoach.fxml";
                        break;

                    default:
                        fxmlPath = "/gui/SuccessPage.fxml";
                }

                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource(fxmlPath));

                Parent root = loader.load();

                Scene scene = new Scene(root);

                if (cssPath != null) {
                    scene.getStylesheets().add(
                            getClass().getResource(cssPath)
                                    .toExternalForm()
                    );
                }

                Stage stage =
                        (Stage) ((Node) event.getSource())
                                .getScene().getWindow();

                stage.setScene(scene);
                stage.centerOnScreen();
                stage.show();

            } else {

                new Alert(Alert.AlertType.ERROR,
                        "Email ou Mot de passe incorrect !").show();
            }

        } catch (Exception e) {

            e.printStackTrace();

            new Alert(Alert.AlertType.ERROR,
                    "Erreur lors du login: "
                            + e.getMessage()).show();
        }
    }
    @FXML
    void goToRegister(MouseEvent event) {
        try {

            Parent root = FXMLLoader.load(getClass().getResource("/gui/UserManagement.fxml"));
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

        if (email.isEmpty() || !email.contains("@")) {
            new Alert(Alert.AlertType.WARNING, "Veuillez saisir votre email dans le champ de texte avant de cliquer !").show();
            return;
        }

        UserApp user = us.findByEmail(email);

        if (user != null) {
            // 1. Gènèri el Code
            String generatedOTP = String.valueOf((int) (Math.random() * 9000) + 1000);

            // 2. Ab3ath el Mail direct
            MailService.sendOTP(email, generatedOTP);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ForgotPassword.fxml"));
                Parent root = loader.load();

                GUI.ForgotPasswordController controller = loader.getController();
                // Passi el User wel Code s7i7
                controller.initData(user, generatedOTP);

                Stage stage = (Stage) txtEmail.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();

            } catch (IOException e) { e.printStackTrace(); }
        } else {
            new Alert(Alert.AlertType.ERROR, "Aucun utilisateur trouvé avec cet email !").show();
        }
    }}