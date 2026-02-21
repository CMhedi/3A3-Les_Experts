package Controllers;

import Services.AiRecommendationService;
import Services.ActiviteService;
import Models.Activite;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class ChatbotUserController {

    @FXML private TextArea taChat;
    @FXML private TextField tfMessage;
    @FXML private TextArea taResults;
    @FXML private Label lblStatus;

    private final AiRecommendationService ai = new AiRecommendationService();
    private final ActiviteService db = new ActiviteService();

    @FXML
    private void send(ActionEvent event) {
        String msg = tfMessage.getText();
        if (msg == null || msg.isBlank()) return;

        append("Vous", msg);
        tfMessage.clear();
        lblStatus.setText("Analyse IA + recherche SQL...");

        new Thread(() -> {
            try {
                AiRecommendationService.Filters f = ai.extractFilters(msg);

                int limit = (f.limit == null ? 5 : f.limit);
                List<Activite> results = db.searchByFilters(f.type_activite, f.categorie_act, f.niveau_act, f.prix_max, f.limit);

                Platform.runLater(() -> {
                    append("Bot", (f.message == null ? "Voici mes recommandations :" : f.message));
                    renderResults(results);
                    lblStatus.setText("");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblStatus.setText("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    private void renderResults(List<Activite> list) {
        if (list == null || list.isEmpty()) {
            taResults.setText("Aucune activité trouvée avec ces critères.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Activite a : list) {
            sb.append("• ")
                    .append(a.getNom())
                    .append(" | ").append(a.getTypeActivite())
                    .append(" | ").append(a.getCategorieAct())
                    .append(" | ").append(a.getNiveauAct())
                    .append(" | ").append(a.getPrix()).append(" DT")
                    .append(" | ").append(a.getStatut())
                    .append("\n");
        }
        taResults.setText(sb.toString());
    }

    private void append(String who, String text) {
        taChat.appendText("[" + who + "] " + text + "\n\n");
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/UserSeances.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}