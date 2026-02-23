package GUI;

import Entities.Session;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtMdp;
    @FXML private TextField txtMdpVisible;
    @FXML private Button btnToggleMdp;

    private boolean isMdpVisible = false;
    private final UserService us = new UserService();

    @FXML
    public void initialize() {
        // ✅ même valeur pour les 2 champs (password caché + visible)
        txtMdp.textProperty().bindBidirectional(txtMdpVisible.textProperty());

        // ✅ état initial
        txtMdpVisible.setVisible(false);
        txtMdpVisible.setManaged(false);
        txtMdp.setVisible(true);
        txtMdp.setManaged(true);
        btnToggleMdp.setText("👁");
    }

    @FXML
    void togglePassword() {
        isMdpVisible = !isMdpVisible;

        if (isMdpVisible) {
            txtMdpVisible.setVisible(true);
            txtMdpVisible.setManaged(true);

            txtMdp.setVisible(false);
            txtMdp.setManaged(false);

            btnToggleMdp.setText("🙈");
            txtMdpVisible.requestFocus();
            txtMdpVisible.positionCaret(txtMdpVisible.getText().length());
        } else {
            txtMdpVisible.setVisible(false);
            txtMdpVisible.setManaged(false);

            txtMdp.setVisible(true);
            txtMdp.setManaged(true);

            btnToggleMdp.setText("👁");
            txtMdp.requestFocus();
            txtMdp.positionCaret(txtMdp.getText().length());
        }
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = (txtEmail.getText() == null) ? "" : txtEmail.getText().trim();
        String mdp = (txtMdp.getText() == null) ? "" : txtMdp.getText();

        if (email.isEmpty() || mdp.isEmpty()) {
            DialogUtils.showWarning("Champs manquants", "Veuillez remplir tous les champs !");
            return;
        }

        try {
            UserApp user = us.findByEmail(email);

            if (user == null) {
                DialogUtils.showError("Échec de connexion", "Email ou Mot de passe incorrect !");
                return;
            }

            String stored = user.getMotDePasse();
            boolean ok;

            // ✅ Support: hashed (BCrypt) + fallback plain-text (si anciens comptes)
            if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))) {
                ok = BCrypt.checkpw(mdp, stored);
            } else {
                ok = mdp.equals(stored);
            }

            if (!ok) {
                DialogUtils.showError("Échec de connexion", "Email ou Mot de passe incorrect !");
                return;
            }

            // ✅ Stocker session
            Session.setConnectedUser(user);

            String fxmlPath;
            switch (user.getRole()) {
                case ADMIN -> fxmlPath = "/GUI/MainLayout.fxml";
                case USER_SIMPLE -> fxmlPath = "/GUI/MainLayoutUser.fxml";
                case COACH -> fxmlPath = "/GUI/MainLayoutcoach.fxml";
                default -> fxmlPath = "/GUI/SuccessPage.fxml";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

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
            e.printStackTrace();
        }
    }

    @FXML
    void handleForgotPassword() {
        String email = (txtEmail.getText() == null) ? "" : txtEmail.getText().trim();

        if (email.isEmpty()) {
            DialogUtils.showWarning("Email Manquant", "Veuillez saisir un email d'abord.");
            return;
        }

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
                stage.setResizable(false);
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                DialogUtils.showError("Erreur", "Impossible d'ouvrir la page Forgot Password.");
            }
        } else {
            DialogUtils.showWarning("Email invalide", "Aucun utilisateur trouvé avec cet email.");
        }
    }
}