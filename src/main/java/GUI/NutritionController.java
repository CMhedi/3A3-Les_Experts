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
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class NutritionController {

    // ==============================
    // FXML COMPONENTS
    // ==============================

    @FXML private TextField txtIngredient;
    @FXML private TextField txtPoids;
    @FXML private TextField txtTaille;

    @FXML private Label lblCalories;
    @FXML private Label lblProteins;
    @FXML private Label lblFats;
    @FXML private Label lblCarbs;
    @FXML private Label lblMessage;

    @FXML private Label lblIMC;
    @FXML private Label lblIMCRemark;

    @FXML private LineChart<String, Number> caloriesChart;

    // ==============================
    // SERVICES
    // ==============================

    private NutritionLogService logService = new NutritionLogService();
    private double[] lastNutritionValues;

    // ==============================
    // ANALYSE ALIMENT
    // ==============================

    @FXML
    void handleAnalyzeFood(ActionEvent event) {

        if (txtIngredient.getText().isEmpty()) {
            lblMessage.setText("Veuillez entrer un aliment.");
            return;
        }

        lblMessage.setText("Analyse en cours...");

        new Thread(() -> {
            try {

                lastNutritionValues =
                        NutritionService.getNutritionValues(
                                txtIngredient.getText());

                Platform.runLater(() -> {
                    lblCalories.setText("🔥 " + lastNutritionValues[0] + " kcal");
                    lblProteins.setText("🥩 " + lastNutritionValues[1] + " g");
                    lblFats.setText("🧈 " + lastNutritionValues[2] + " g");
                    lblCarbs.setText("🍞 " + lastNutritionValues[3] + " g");
                    lblMessage.setText("");
                });

            } catch (Exception e) {

                Platform.runLater(() ->
                        lblMessage.setText("❌ Erreur API.")
                );
            }

        }).start();
    }

    // ==============================
    // AJOUT AU JOURNAL
    // ==============================

    @FXML
    void handleSaveToJournal(ActionEvent event) {

        try {

            if (lastNutritionValues == null) {
                lblMessage.setText("Analyse d'abord un aliment !");
                return;
            }

            NutritionLog log =
                    new NutritionLog(
                            Session.getConnectedUser().getIdUser(),
                            txtIngredient.getText(),
                            lastNutritionValues[0],
                            lastNutritionValues[1],
                            lastNutritionValues[2],
                            lastNutritionValues[3],
                            LocalDate.now()
                    );

            logService.add(log);

            lblMessage.setText("✅ Ajouté au journal !");

        } catch (Exception e) {
            lblMessage.setText("❌ Erreur sauvegarde.");
        }
    }

    // ==============================
    // AFFICHER JOURNAL
    // ==============================

    @FXML
    void handleShowJournal(ActionEvent event) {

        try {

            List<NutritionLog> list =
                    logService.getByUser(
                            Session.getConnectedUser().getIdUser());

            if (list.isEmpty()) {
                lblMessage.setText("Aucune entrée dans le journal.");
                return;
            }

            double totalCalories = 0;

            for (NutritionLog log : list) {
                totalCalories += log.getCalories();
            }

            lblCalories.setText("🔥 Total Journal : " + totalCalories + " kcal");
            lblProteins.setText("");
            lblFats.setText("");
            lblCarbs.setText("");
            lblMessage.setText("Journal chargé avec succès.");

        } catch (Exception e) {
            lblMessage.setText("❌ Erreur chargement journal.");
        }
    }

    // ==============================
    // TOTAL AUJOURD'HUI
    // ==============================

    @FXML
    void handleTodayTotal(ActionEvent event) {

        try {

            double total =
                    logService.getTodayTotal(
                            Session.getConnectedUser().getIdUser());

            lblCalories.setText("🔥 " + total + " kcal");
            lblProteins.setText("");
            lblFats.setText("");
            lblCarbs.setText("");
            lblMessage.setText("Total calorique du jour");

        } catch (Exception e) {
            lblMessage.setText("❌ Erreur calcul total.");
        }
    }

    // ==============================
    // CALCUL IMC
    // ==============================

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

    // ==============================
    // RETOUR
    // ==============================

    @FXML
    void handleRetour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/GUI/MainLayoutUser.fxml")
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}