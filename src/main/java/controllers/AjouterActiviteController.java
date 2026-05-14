package controllers;

import Models.Activite;
import Services.interfaces.ActiviteService;
import Services.interfaces.WeatherService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class AjouterActiviteController {

    @FXML private TextField nomField;
    @FXML private ComboBox<String> typeBox;
    @FXML private ComboBox<String> categorieBox;
    @FXML private ComboBox<String> niveauBox;
    @FXML private TextField prixField;
    @FXML private ComboBox<String> statutBox;
    @FXML private TextField imageField;
    @FXML private DatePicker dateReservation;

    private final ActiviteService activiteService = new ActiviteService();

    private int wrongAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_SECONDS = 30;
    private long lockUntilMillis = 0;

    @FXML
    public void initialize() {
        categorieBox.getItems().addAll("FITNESS", "RUNNING", "FOOTBALL", "BASKETBALL", "TENNIS", "NATATION", "RANDONNEE", "CYCLISME", "YOGA", "AUTRE");
        niveauBox.getItems().addAll("DEBUTANT", "INTERMEDIAIRE", "AVANCE");
        statutBox.getItems().addAll("DISPONIBLE", "INDISPONIBLE");
        typeBox.getItems().addAll("SPORT", "CAMPING", "INTELECTUEL", "CULTUREL");
    }

    @FXML
    private void ajouterActivite() {

        long now = System.currentTimeMillis();
        if (now < lockUntilMillis) {
            long remain = (lockUntilMillis - now) / 1000;
            new Alert(Alert.AlertType.ERROR, "Trop d'erreurs. Réessayer dans " + remain + "s.").show();
            return;
        }

        if (!validateForm()) {
            return;
        }

        openCaptcha(ok -> {
            if (ok) {

                wrongAttempts = 0;

                WeatherService weatherService = new WeatherService();
                String weatherMsg;

                try {
                    WeatherService.WeatherData data = weatherService.getCurrentWeather("Tunis");
                    weatherMsg = "Météo à " + data.city + " : " +
                            data.description + ", " +
                            String.format("%.1f", data.temp) + "°C";
                } catch (Exception e) {
                    weatherMsg = "⚠️ Erreur météo : " + e.getMessage();
                }

                Alert weatherAlert = new Alert(Alert.AlertType.INFORMATION);
                weatherAlert.setTitle("Information Météo");
                weatherAlert.setHeaderText(null);
                weatherAlert.setContentText(weatherMsg);
                weatherAlert.showAndWait();

                doAjouterActivite();
            }
        });
    }

    private boolean validateForm() {
        String nom = nomField.getText() != null ? nomField.getText().trim() : "";
        if (nom.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le nom de l'activité est obligatoire.").show();
            return false;
        }
        if (typeBox.getValue() == null || categorieBox.getValue() == null
                || niveauBox.getValue() == null || statutBox.getValue() == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez renseigner type, catégorie, niveau et statut.").show();
            return false;
        }
        try {
            double p = Double.parseDouble(prixField.getText().trim());
            if (p < 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Prix invalide (nombre positif attendu).").show();
            return false;
        }
        return true;
    }

    private void doAjouterActivite() {
        try {
            double prix = Double.parseDouble(prixField.getText().trim());
            String img = imageField.getText() != null ? imageField.getText().trim() : "";

            Activite a = new Activite(
                    0,
                    nomField.getText().trim(),
                    typeBox.getValue(),
                    categorieBox.getValue(),
                    niveauBox.getValue(),
                    prix,
                    statutBox.getValue(),
                    img,
                    0,
                    null,
                    null
            );

            int generatedId = activiteService.addAndReturnId(a);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/recuActivite.fxml"));
            Parent root = loader.load();

            RecuActiviteController controller = loader.getController();

            controller.setActiviteId(generatedId);

            java.time.LocalDate ld = dateReservation.getValue() != null
                    ? dateReservation.getValue()
                    : java.time.LocalDate.now();

            controller.setData(
                    typeBox.getValue(),
                    nomField.getText().trim(),
                    categorieBox.getValue(),
                    niveauBox.getValue(),
                    prixField.getText().trim(),
                    ld,
                    statutBox.getValue()
            );

            Stage receiptStage = new Stage();
            receiptStage.setScene(new Scene(root));
            receiptStage.setTitle("Reçu Activité");
            receiptStage.show();

            Stage addStage = (Stage) nomField.getScene().getWindow();
            addStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur d'ajout : " + e.getMessage()).show();
        }
    }

    private void openCaptcha(Consumer<Boolean> onDone) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/CaptchaVerification.fxml"));
            Parent root = loader.load();

            CaptchaVerificationController vc = loader.getController();

            vc.setOnWrongAttempt(() -> {
                wrongAttempts++;
                int remaining = MAX_ATTEMPTS - wrongAttempts;

                if (wrongAttempts >= MAX_ATTEMPTS) {
                    lockUntilMillis = System.currentTimeMillis() + (LOCK_SECONDS * 1000L);
                    new Alert(Alert.AlertType.ERROR,
                            "Trop d'erreurs ! Ajout bloqué pendant " + LOCK_SECONDS + " secondes.").show();
                } else {
                    new Alert(Alert.AlertType.WARNING,
                            "Code incorrect. Il vous reste " + remaining + " tentative(s).").show();
                }
            });

            vc.setOnResult(ok -> {
                if (onDone != null) onDone.accept(ok);
            });

            Stage popup = new Stage();
            popup.setTitle("Vérification Anti-Bot");
            popup.initOwner(nomField.getScene().getWindow());
            popup.setScene(new Scene(root));
            popup.setResizable(true);
            popup.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur ouverture CAPTCHA : " + e.getMessage()).show();
            if (onDone != null) onDone.accept(false);
        }
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de retour vers : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    public void goUserSeances(ActionEvent event) {
        switchScene(event, "/GUI/UserSeances.fxml");
    }

    public void goAdmin(ActionEvent event) {
        switchScene(event, "/GUI/AdminActivites.fxml");
    }

    @FXML
    private void goToWeather(ActionEvent event) {
        switchScene(event, "/GUI/Weather.fxml");
    }
}
