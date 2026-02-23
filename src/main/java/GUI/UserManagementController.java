package GUI;

import Entities.UserApp;
<<<<<<< HEAD
import Services.UserService;
=======
import GUI.utils.DialogUtils;
import Services.interfaces.UserService;
>>>>>>> origin/salma_integration
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
import javafx.scene.input.MouseEvent; // HEDHI EL S7I7A
<<<<<<< HEAD

=======
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
>>>>>>> origin/salma_integration
import java.io.IOException;

public class UserManagementController {

    @FXML private TextField txtNom, txtPrenom, txtEmail, txtTel, txtAge, txtExperience;
    @FXML private PasswordField txtMdp;
    @FXML private ComboBox<RoleUser> comboRole;
    @FXML private ComboBox<String> comboSpecialite; // Hasb el Enum fil SQL
    @FXML private ComboBox<String> comboDispo;      // Hasb el Enum fil SQL
    @FXML private TextArea txtBio;
    @FXML private VBox paneCoach;
    @FXML private Button btnAjouter;


    @FXML private Label errorNom, errorPrenom, errorEmail, errorTel, errorMdp,errorAge,errorExperience;

    private UserService us = new UserService();

    @FXML

    public void initialize() {

// 1. Setup ComboBoxes

        // Njibu el lista mta3 el roles lkol
        List<RoleUser> roles = new ArrayList<>(Arrays.asList(RoleUser.values()));

// Na7iw el ADMIN menha (thabbet mel ism exact f-Enum mte3ek)
        roles.remove(RoleUser.ADMIN);

// Tawa n7ottou el lista el "ndhifa" fel ComboBox
        comboRole.setItems(FXCollections.observableArrayList(roles));

        comboSpecialite.setItems(FXCollections.observableArrayList("FITNESS", "RUNNING", "FOOTBALL", "BASKETBALL", "YOGA", "AUTRE"));

        comboDispo.setItems(FXCollections.observableArrayList("MATIN", "SOIR", "JOURNEE_COMPLETE"));



// 2. PaneCoach Visibility Logic

        paneCoach.visibleProperty().bind(Bindings.createBooleanBinding(

                () -> comboRole.getValue() != null && comboRole.getValue().toString().equals("COACH"),

                comboRole.valueProperty())

        );

        paneCoach.managedProperty().bind(paneCoach.visibleProperty());



        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";



// --- EL-KOUD MTE3EK (MA BDELTECH FIH) ---

        txtNom.textProperty().addListener((o, old, n) -> {

            boolean valid = !n.isEmpty();

            errorNom.setVisible(!valid);

            txtNom.setStyle(valid ? "" : "-fx-border-color: red;");

        });



        txtEmail.textProperty().addListener((obs, oldV, newV) -> {

            if (newV.isEmpty()) {

                errorEmail.setText("⚠️ L'email est obligatoire");

                errorEmail.setVisible(true);

                txtEmail.setStyle("-fx-border-color: red;");

            } else if (!newV.contains("@")) {

                errorEmail.setText("⚠️ Il manque le symbole '@'");

                errorEmail.setVisible(true);

                txtEmail.setStyle("-fx-border-color: red;");

            } else if (!newV.contains(".")) {

                errorEmail.setText("⚠️ Il manque le point '.' (ex: .com)");

                errorEmail.setVisible(true);

                txtEmail.setStyle("-fx-border-color: red;");

            } else if (!newV.matches(emailRegex)) {

                errorEmail.setText("⚠️ Format invalide (ex: nom@domaine.com)");

                errorEmail.setVisible(true);

                txtEmail.setStyle("-fx-border-color: red;");

            } else {

                errorEmail.setVisible(false);

                txtEmail.setStyle("");

            }

        });



        txtTel.textProperty().addListener((obs, oldV, newV) -> {

            if (newV.length() > 8) txtTel.setText(oldV);

            if (newV.isEmpty()) {

                errorTel.setText("⚠️ Le téléphone est obligatoire");

                errorTel.setVisible(true);

            } else if (!newV.matches("\\d*")) {

                errorTel.setText("⚠️ Utilisez uniquement des chiffres");

                errorTel.setVisible(true);

                txtTel.setStyle("-fx-border-color: red;");

            } else if (newV.length() != 8) {

                errorTel.setText("⚠️ Il faut exactement 8 chiffres (" + newV.length() + "/8)");

                errorTel.setVisible(true);

                txtTel.setStyle("-fx-border-color: red;");

            } else {

                errorTel.setVisible(false);

                txtTel.setStyle("");

            }

        });



        txtMdp.textProperty().addListener((o, old, n) -> {

            boolean valid = n.length() >= 6;

            errorMdp.setVisible(!valid);

            txtMdp.setStyle(valid ? "" : "-fx-border-color: red;");

        });



// --- ZIEDA LEL-COACH (NAFS EL-STYLE) ---



// Validation Âge

        // Validation Âge (18-40 ans) - Blast el-Alert l-9dima
        txtAge.textProperty().addListener((obs, oldV, newV) -> {
            try {
                if (newV.isEmpty()) {
                    errorAge.setText("⚠️ Âge obligatoire");
                    errorAge.setVisible(true);
                    txtAge.setStyle("-fx-border-color: red;");
                } else if (!newV.matches("\\d+")) {
                    errorAge.setText("⚠️ Utilisez uniquement des chiffres");
                    errorAge.setVisible(true);
                    txtAge.setStyle("-fx-border-color: red;");
                } else {
                    int ageVal = Integer.parseInt(newV);

                    if (ageVal < 18) {
                        errorAge.setText("⚠️ Minimum 18 ans pour un Coach");
                        errorAge.setVisible(true);
                        txtAge.setStyle("-fx-border-color: red;");
                    } else if (ageVal > 40) {
                        // Hna el-check mta3 40 sna
                        errorAge.setText("⚠️ L'âge max est 40 ans");
                        errorAge.setVisible(true);
                        txtAge.setStyle("-fx-border-color: red;");
                    } else {
                        // Kol chay mrigel
                        errorAge.setVisible(false);
                        txtAge.setStyle("");
                    }
                }

                // Faza zeyda: Na3mlu "trigger" lel expérience bech t-thabbet rouha m3a l-âge jdid
                if (!txtExperience.getText().isEmpty()) {
                    txtExperience.setText(txtExperience.getText());
                }

            } catch (NumberFormatException e) {
                errorAge.setText("⚠️ Format invalide");
                errorAge.setVisible(true);
                txtAge.setStyle("-fx-border-color: red;");
            }
        });


// Validation Expérience (Label dynamique - Zero Alerts)
        txtExperience.textProperty().addListener((obs, oldV, newV) -> {
            try {
                String ageText = txtAge.getText();
                if (ageText.isEmpty()) {
                    errorExperience.setText("⚠️ Saisissez d'abord l'âge");
                    errorExperience.setVisible(true);
                    return;
                }

                int ageVal = Integer.parseInt(ageText);

                if (newV.isEmpty()) {
                    errorExperience.setText("⚠️ Expérience obligatoire");
                    errorExperience.setVisible(true);
                    txtExperience.setStyle("-fx-border-color: red;");
                } else if (!newV.matches("\\d+")) {
                    errorExperience.setText("⚠️ Utilisez uniquement des chiffres");
                    errorExperience.setVisible(true);
                    txtExperience.setStyle("-fx-border-color: red;");
                } else {
                    int expVal = Integer.parseInt(newV);
                    int ageActif = ageVal - 18; // Logic mta3 el logic l-9dima

                    if (expVal < 0) {
                        errorExperience.setText("⚠️ L'expérience ne peut pas être négative");
                        errorExperience.setVisible(true);
                        txtExperience.setStyle("-fx-border-color: red;");
                    } else if (expVal > ageActif) {
                        // El message el dynamique kima tlabt
                        errorExperience.setText("⚠️ Avec " + ageVal + " ans, l'expérience max est " + ageActif + " ans (âge actif)");
                        errorExperience.setVisible(true);
                        txtExperience.setStyle("-fx-border-color: red;");
                    } else {
                        // Kol chay mrigel
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



// 2. Disable Button (Zidtlek fiha el-check mta3 el-Coach kemel)
        btnAjouter.disableProperty().bind(
                txtNom.textProperty().isEmpty()
                        .or(txtEmail.textProperty().isEmpty())
                        .or(txtTel.textProperty().length().isNotEqualTo(8))
                        .or(txtMdp.textProperty().length().lessThan(6))
                        .or(comboRole.valueProperty().isNull())
                        .or(Bindings.createBooleanBinding(() -> {
                                    boolean isEmailInvalid = !txtEmail.getText().matches(emailRegex);

                                    // Ken ekhtar Coach, lezem n-zidou thabtou fil Specialité wel Disponibilité
                                    if (comboRole.getValue() == RoleUser.COACH) {
                                        return isEmailInvalid ||
                                                errorAge.isVisible() ||
                                                errorExperience.isVisible() ||
                                                txtAge.getText().isEmpty() ||
                                                txtExperience.getText().isEmpty() ||
                                                comboSpecialite.getValue() == null || // <--- Zieda hna
                                                comboDispo.getValue() == null;      // <--- Zieda hna
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
                                comboDispo.valueProperty() // <--- Ma n-sewech el-listener hna
                        ))
        );


        btnAjouter.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 25;");

    }
    @FXML
    void handleAjouter(ActionEvent event) {
        try {

            // ====== 1. Nettoyage des champs ======
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

            // ====== 2. Génération Avatar PRO ======
            String fullName = nom + " " + prenom;

            String encodedName = java.net.URLEncoder.encode(
                    fullName,
                    java.nio.charset.StandardCharsets.UTF_8
            );

            String avatarUrl =
                    "https://ui-avatars.com/api/?name=" +
                            encodedName +
                            "&background=random&color=fff&size=200";

            u.setImageUrl(avatarUrl);

            // ====== 3. Infos Coach ======
            if (comboRole.getValue() == RoleUser.COACH) {
                u.setAge(Integer.parseInt(txtAge.getText().trim()));
                u.setExperience(txtExperience.getText().trim());
                u.setSpecialite(comboSpecialite.getValue());
                u.setDisponibilite(comboDispo.getValue());
                u.setBioCertifs(txtBio.getText().trim());
            }

            // ====== 4. Ajout DB ======
            us.add(u);

            // ====== 5. Message succès ======
            DialogUtils.showInfo(
                    "Succès",
                    "✅ Compte créé avec succès !"
            );

            // ====== 6. Redirection Login ======
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
            System.err.println(" Erreur: Ma l9itich el fichier Login.fxml!");
            e.printStackTrace();
        }
    }
}