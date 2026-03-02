// ===== AjouterActiviteController.java (COMPLET CORRIGÉ) =====
package controllers;

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
import java.sql.*;
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

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    private int wrongAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_SECONDS = 30;
    private long lockUntilMillis = 0;

    @FXML
    public void initialize() {
        categorieBox.getItems().addAll("FITNESS", "RUNNING", "FOOTBALL", "BASKETBALL");
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

    // ✅ CORRIGÉ: on passe generatedId au reçu + setActiviteId(generatedId) + setData avec 7 params corrects
    private void doAjouterActivite() {

        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "INSERT INTO activite (nom, type_activite, categorie_act, niveau_act, prix, statut, image_url, id_pack, date_reservation) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?)";

            pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, nomField.getText());
            pst.setString(2, typeBox.getValue());
            pst.setString(3, categorieBox.getValue());
            pst.setString(4, niveauBox.getValue());
            pst.setDouble(5, Double.parseDouble(prixField.getText()));
            pst.setString(6, statutBox.getValue());
            pst.setString(7, imageField.getText());
            pst.setString(8, dateReservation.getValue().toString());

            pst.executeUpdate();

            rs = pst.getGeneratedKeys();
            int generatedId = 0;
            if (rs.next()) {
                generatedId = rs.getInt(1); // ✅ ID activité insérée
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/recuActivite.fxml"));
            Parent root = loader.load();

            RecuActiviteController controller = loader.getController();

            // ✅ IMPORTANT: injecter l'id activité
            controller.setActiviteId(generatedId);

            // ✅ IMPORTANT: signature setData correcte (7 paramètres)
            controller.setData(
                    typeBox.getValue(),
                    nomField.getText(),
                    categorieBox.getValue(),
                    niveauBox.getValue(),
                    prixField.getText(),
                    dateReservation.getValue(),  // LocalDate
                    statutBox.getValue()         // String date
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Reçu Activité");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur d'ajout : " + e.getMessage()).show();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (pst != null) pst.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
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
            popup.setResizable(false);
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