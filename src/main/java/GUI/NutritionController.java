package GUI;

import Entities.NutritionLog;
import Entities.Session;
import Services.interfaces.NutritionLogService;
import Services.interfaces.NutritionService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class NutritionController {

    @FXML
    private TextField txtFood;
    @FXML
    private LineChart<String, Number> caloriesChart;
    @FXML
    private TextArea txtResult;

    private NutritionLogService logService =
            new NutritionLogService();

    private double[] lastNutritionValues;

    @FXML
    void handleAnalyzeFood(ActionEvent event) {

        if (txtFood.getText().isEmpty()) {
            txtResult.setText("Veuillez entrer un aliment.");
            return;
        }

        txtResult.setText("Analyse en cours...");

        new Thread(() -> {

            try {

                lastNutritionValues =
                        NutritionService.getNutritionValues(
                                txtFood.getText());

                String result =
                        "🔥 Calories: " + lastNutritionValues[0] + " kcal\n" +
                                "🥩 Protéines: " + lastNutritionValues[1] + " g\n" +
                                "🧈 Lipides: " + lastNutritionValues[2] + " g\n" +
                                "🍞 Glucides: " + lastNutritionValues[3] + " g";

                Platform.runLater(() ->
                        txtResult.setText(result)
                );

            } catch (Exception e) {

                Platform.runLater(() ->
                        txtResult.setText("Erreur API.")
                );
            }

        }).start();
    }

    @FXML
    void handleSaveToJournal(ActionEvent event) {

        try {

            if (lastNutritionValues == null) {
                txtResult.setText("Analyse d'abord un aliment !");
                return;
            }

            NutritionLog log =
                    new NutritionLog(
                            Session.getConnectedUser().getIdUser(),
                            txtFood.getText(),
                            lastNutritionValues[0],
                            lastNutritionValues[1],
                            lastNutritionValues[2],
                            lastNutritionValues[3],
                            LocalDate.now()
                    );

            logService.add(log);

            txtResult.appendText("\n\n✅ Ajouté au journal !");

        } catch (Exception e) {
            txtResult.setText("Erreur sauvegarde.");
        }
    }

    @FXML
    void handleShowJournal(ActionEvent event) {

        try {

            List<NutritionLog> list =
                    logService.getByUser(
                            Session.getConnectedUser().getIdUser());

            StringBuilder sb = new StringBuilder();

            for (NutritionLog log : list) {

                sb.append(log.getLogDate())
                        .append(" - ")
                        .append(log.getFoodName())
                        .append(" - ")
                        .append(log.getCalories())
                        .append(" kcal\n");
            }

            txtResult.setText(sb.toString());

        } catch (Exception e) {
            txtResult.setText("Erreur chargement journal.");
        }
    }

    @FXML
    void handleTodayTotal(ActionEvent event) {

        try {

            double total =
                    logService.getTodayTotal(
                            Session.getConnectedUser().getIdUser());

            txtResult.setText("Total aujourd'hui : " +
                    total + " kcal");

        } catch (Exception e) {
            txtResult.setText("Erreur calcul total.");
        }
    }
    @FXML
    private TextField txtPoids, txtTaille;

    @FXML
    private Label lblIMC, lblIMCRemark;

    @FXML
    void handleIMC(ActionEvent event) {

        try {

            String poidsText = txtPoids.getText().replace(",", ".");
            String tailleText = txtTaille.getText().replace(",", ".");

            double poids = Double.parseDouble(poidsText);
            double taille = Double.parseDouble(tailleText);

            double imc = poids / (taille * taille);

            String categorie;
            String remarque;

            if (imc < 18.5) {
                categorie = "Maigreur";
                remarque = "Apport nutritionnel plus riche recommandé.";
            } else if (imc < 25) {
                categorie = "Normal";
                remarque = "Excellent équilibre corporel.";
            } else if (imc < 30) {
                categorie = "Surpoids";
                remarque = "Activité physique conseillée.";
            } else {
                categorie = "Obésité";
                remarque = "Consultez un professionnel de santé.";
            }

            lblIMC.setText("IMC : " + String.format("%.2f", imc) + " (" + categorie + ")");
            lblIMCRemark.setText(remarque);

        } catch (Exception e) {
            lblIMC.setText("Entrée invalide");
            lblIMCRemark.setText("");
        }
    }

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/gui/MainLayoutUser.fxml")
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}