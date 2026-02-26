package GUI;

import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.UserService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import enums.RoleUser;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class UserManagementController {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtTel, txtAge, txtExperience;
    @FXML private PasswordField txtMdp;
    @FXML private ComboBox<RoleUser> comboRole;
    @FXML private ComboBox<String> comboSpecialite;
    @FXML private ComboBox<String> comboDispo;
    @FXML private TextArea txtBio;
    @FXML private VBox paneCoach;
    @FXML private Button btnAjouter;

    @FXML private Label errorNom, errorPrenom, errorEmail, errorTel, errorMdp, errorAge, errorExperience;

    private final UserService us = new UserService();

    @FXML
    public void initialize() {

        // ===== 1) ComboBoxes =====
        List<RoleUser> roles = new ArrayList<>(Arrays.asList(RoleUser.values()));
        if (roles.contains(RoleUser.ADMIN)) roles.remove(RoleUser.ADMIN); // remove admin from public register
        comboRole.setItems(FXCollections.observableArrayList(roles));

        comboSpecialite.setItems(FXCollections.observableArrayList("FITNESS", "RUNNING", "FOOTBALL", "BASKETBALL", "YOGA", "AUTRE"));
        comboDispo.setItems(FXCollections.observableArrayList("MATIN", "SOIR", "JOURNEE_COMPLETE"));

        // ===== 2) Coach pane visibility =====
        paneCoach.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> comboRole.getValue() != null && comboRole.getValue() == RoleUser.COACH,
                comboRole.valueProperty()
        ));
        paneCoach.managedProperty().bind(paneCoach.visibleProperty());

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

        // ===== 3) Validations (same logic, just safer) =====

        txtNom.textProperty().addListener((o, old, n) -> {
            boolean valid = n != null && !n.trim().isEmpty();
            errorNom.setVisible(!valid);
            txtNom.setStyle(valid ? "" : "-fx-border-color: red;");
        });

        txtPrenom.textProperty().addListener((o, old, n) -> {
            boolean valid = n != null && !n.trim().isEmpty();
            errorPrenom.setVisible(!valid);
            txtPrenom.setStyle(valid ? "" : "-fx-border-color: red;");
        });

        txtEmail.textProperty().addListener((obs, oldV, newV) -> {
            String v = newV == null ? "" : newV.trim();
            if (v.isEmpty()) {
                errorEmail.setText("⚠️ L'email est obligatoire");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else if (!v.contains("@")) {
                errorEmail.setText("⚠️ Il manque le symbole '@'");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else if (!v.contains(".")) {
                errorEmail.setText("⚠️ Il manque le point '.' (ex: .com)");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else if (!v.matches(emailRegex)) {
                errorEmail.setText("⚠️ Format invalide (ex: nom@domaine.com)");
                errorEmail.setVisible(true);
                txtEmail.setStyle("-fx-border-color: red;");
            } else {
                errorEmail.setVisible(false);
                txtEmail.setStyle("");
            }
        });

        txtTel.textProperty().addListener((obs, oldV, newV) -> {
            String v = newV == null ? "" : newV.trim();

            if (v.length() > 8) {
                txtTel.setText(oldV);
                return;
            }

            if (v.isEmpty()) {
                errorTel.setText("⚠️ Le téléphone est obligatoire");
                errorTel.setVisible(true);
                txtTel.setStyle("-fx-border-color: red;");
            } else if (!v.matches("\\d*")) {
                errorTel.setText("⚠️ Utilisez uniquement des chiffres");
                errorTel.setVisible(true);
                txtTel.setStyle("-fx-border-color: red;");
            } else if (v.length() != 8) {
                errorTel.setText("⚠️ Il faut exactement 8 chiffres (" + v.length() + "/8)");
                errorTel.setVisible(true);
                txtTel.setStyle("-fx-border-color: red;");
            } else {
                errorTel.setVisible(false);
                txtTel.setStyle("");
            }
        });

        txtMdp.textProperty().addListener((o, old, n) -> {
            boolean valid = n != null && n.length() >= 6;
            errorMdp.setVisible(!valid);
            txtMdp.setStyle(valid ? "" : "-fx-border-color: red;");
        });

        // ===== Coach: Age validation =====
        txtAge.textProperty().addListener((obs, oldV, newV) -> {
            // validate only if coach
            if (comboRole.getValue() != RoleUser.COACH) {
                errorAge.setVisible(false);
                txtAge.setStyle("");
                return;
            }

            String v = newV == null ? "" : newV.trim();
            try {
                if (v.isEmpty()) {
                    errorAge.setText("⚠️ Âge obligatoire");
                    errorAge.setVisible(true);
                    txtAge.setStyle("-fx-border-color: red;");
                } else if (!v.matches("\\d+")) {
                    errorAge.setText("⚠️ Utilisez uniquement des chiffres");
                    errorAge.setVisible(true);
                    txtAge.setStyle("-fx-border-color: red;");
                } else {
                    int ageVal = Integer.parseInt(v);
                    if (ageVal < 18) {
                        errorAge.setText("⚠️ Minimum 18 ans pour un Coach");
                        errorAge.setVisible(true);
                        txtAge.setStyle("-fx-border-color: red;");
                    } else if (ageVal > 40) {
                        errorAge.setText("⚠️ L'âge max est 40 ans");
                        errorAge.setVisible(true);
                        txtAge.setStyle("-fx-border-color: red;");
                    } else {
                        errorAge.setVisible(false);
                        txtAge.setStyle("");
                    }
                }

                // trigger experience re-check
                if (txtExperience.getText() != null && !txtExperience.getText().isEmpty()) {
                    txtExperience.setText(txtExperience.getText());
                }

            } catch (NumberFormatException e) {
                errorAge.setText("⚠️ Format invalide");
                errorAge.setVisible(true);
                txtAge.setStyle("-fx-border-color: red;");
            }
        });

        // ===== Coach: Experience validation =====
        txtExperience.textProperty().addListener((obs, oldV, newV) -> {
            if (comboRole.getValue() != RoleUser.COACH) {
                errorExperience.setVisible(false);
                txtExperience.setStyle("");
                return;
            }

            try {
                String ageText = txtAge.getText() == null ? "" : txtAge.getText().trim();
                String expText = newV == null ? "" : newV.trim();

                if (ageText.isEmpty()) {
                    errorExperience.setText("⚠️ Saisissez d'abord l'âge");
                    errorExperience.setVisible(true);
                    txtExperience.setStyle("-fx-border-color: red;");
                    return;
                }

                int ageVal = Integer.parseInt(ageText);

                if (expText.isEmpty()) {
                    errorExperience.setText("⚠️ Expérience obligatoire");
                    errorExperience.setVisible(true);
                    txtExperience.setStyle("-fx-border-color: red;");
                } else if (!expText.matches("\\d+")) {
                    errorExperience.setText("⚠️ Utilisez uniquement des chiffres");
                    errorExperience.setVisible(true);
                    txtExperience.setStyle("-fx-border-color: red;");
                } else {
                    int expVal = Integer.parseInt(expText);
                    int ageActif = ageVal - 18;

                    if (expVal < 0) {
                        errorExperience.setText("⚠️ L'expérience ne peut pas être négative");
                        errorExperience.setVisible(true);
                        txtExperience.setStyle("-fx-border-color: red;");
                    } else if (expVal > ageActif) {
                        errorExperience.setText("⚠️ Avec " + ageVal + " ans, l'expérience max est " + ageActif + " ans (âge actif)");
                        errorExperience.setVisible(true);
                        txtExperience.setStyle("-fx-border-color: red;");
                    } else {
                        errorExperience.setVisible(false);
                        txtExperience.setStyle("");
                    }
                }
            } catch (NumberFormatException e) {
                errorExperience.setText("⚠️ Format de nombre invalide");
                errorExperience.setVisible(true);
                txtExperience.setStyle("-fx-border-color: red;");
            }
        });

        // ===== 4) Disable Button binding =====
        btnAjouter.disableProperty().bind(
                txtNom.textProperty().isEmpty()
                        .or(txtPrenom.textProperty().isEmpty())
                        .or(txtEmail.textProperty().isEmpty())
                        .or(txtTel.textProperty().length().isNotEqualTo(8))
                        .or(txtMdp.textProperty().length().lessThan(6))
                        .or(comboRole.valueProperty().isNull())
                        .or(Bindings.createBooleanBinding(() -> {
                                    boolean isEmailInvalid = txtEmail.getText() == null || !txtEmail.getText().matches(emailRegex);

                                    if (comboRole.getValue() == RoleUser.COACH) {
                                        return isEmailInvalid
                                                || errorAge.isVisible()
                                                || errorExperience.isVisible()
                                                || txtAge.getText() == null || txtAge.getText().isEmpty()
                                                || txtExperience.getText() == null || txtExperience.getText().isEmpty()
                                                || comboSpecialite.getValue() == null
                                                || comboDispo.getValue() == null;
                                    }
                                    return isEmailInvalid;
                                },
                                txtEmail.textProperty(),
                                comboRole.valueProperty(),
                                errorAge.visibleProperty(),
                                errorExperience.visibleProperty(),
                                txtAge.textProperty(),
                                txtExperience.textProperty(),
                                comboSpecialite.valueProperty(),
                                comboDispo.valueProperty()
                        ))
        );

        btnAjouter.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 25;");
    }
    @FXML
    void handleGoogleLogin(ActionEvent event) {
        try {
            // قراءة الملف من الـ resources
            Reader reader = new InputStreamReader(getClass().getResourceAsStream("/client_secrets.json"));
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), reader);

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientSecrets,
                    Arrays.asList("email", "profile", "openid"))
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            Oauth2 oauth2 = new Oauth2.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential).setApplicationName("EcoApp").build();
            Userinfo userinfo = oauth2.userinfo().get().execute();

            UserApp user = us.findByEmail(userinfo.getEmail());

            if (user == null) {
                user = new UserApp();
                user.setNom(userinfo.getFamilyName());
                user.setPrenom(userinfo.getGivenName());
                user.setEmail(userinfo.getEmail());

                // استعملنا USER_SIMPLE كيف ما موجود في الـ Enum متاعك
                user.setRole(RoleUser.USER_SIMPLE);

                user.setImageUrl(userinfo.getPicture());
                user.setTelephone("00000000"); // ديفولت
                user.setMotDePasse("GOOGLE_PASS_" + userinfo.getId());

                us.add(user);

                DialogUtils.showInfo("Bienvenue", "Compte créé via Google !");
            }

            Entities.Session.setConnectedUser(user);
            System.out.println("Connecté en tant que: " + userinfo.getEmail());
            redirectToHome(event);

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Connexion Google échouée.");
        }
    }

    private void redirectToHome(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/GUI/MainLayoutUser.fxml")); // بدلو بمسار صفحتك
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            // ====== 1) Clean fields ======
            String nom = txtNom.getText().trim();
            String prenom = txtPrenom.getText().trim();
            String email = txtEmail.getText().trim();
            String tel = txtTel.getText().trim();
            String mdp = txtMdp.getText().trim();

            UserApp u = new UserApp();
            u.setNom(nom);
            u.setPrenom(prenom);
            u.setEmail(email);
            u.setTelephone(tel);
            u.setMotDePasse(mdp);
            u.setRole(comboRole.getValue());

            // ====== 2) Avatar URL ======
            String fullName = nom + " " + prenom;
            String encodedName = java.net.URLEncoder.encode(fullName, java.nio.charset.StandardCharsets.UTF_8);
            String avatarUrl = "https://ui-avatars.com/api/?name=" + encodedName + "&background=random&color=fff&size=200";
            u.setImageUrl(avatarUrl);

            // ====== 3) Coach fields ======
            if (comboRole.getValue() == RoleUser.COACH) {
                u.setAge(Integer.parseInt(txtAge.getText().trim()));
                u.setExperience(txtExperience.getText().trim());
                u.setSpecialite(comboSpecialite.getValue());
                u.setDisponibilite(comboDispo.getValue());
                u.setBioCertifs(txtBio.getText() == null ? "" : txtBio.getText().trim());
            }

            // ====== 4) Save DB (hashing inside UserService.add()) ======
            us.add(u);

            DialogUtils.showInfo("Succès", "✅ Compte créé avec succès !");

            // ====== 5) Redirect Login ======
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            DialogUtils.showError("Erreur", "Impossible d'ajouter l'utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleGoToLogin(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur: Ma l9itich el fichier Login.fxml!");
            e.printStackTrace();
        }
    }
}