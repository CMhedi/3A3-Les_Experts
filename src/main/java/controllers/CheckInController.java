// CheckInController.java (COMPLET) -> sans affichage d'ID + sans DEBUG dans detailsArea
package controllers;

import Services.interfaces.LocalTicketServer;
import Services.interfaces.TicketApiClient;
import GUI.utils.SceneUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class CheckInController {

    @FXML private TextArea tokenArea;
    @FXML private TextArea detailsArea;

    @FXML
    public void initialize() {
        if (detailsArea != null) {
            detailsArea.setText("✅ Check-in prêt.\n➡️ Colle un token puis clique Vérifier.");
        }

        try {
            LocalTicketServer.startOnce();
            System.out.println("[CheckIn] Server started/ready");
        } catch (Exception e) {
            e.printStackTrace();
            if (detailsArea != null) {
                detailsArea.setText("❌ Erreur démarrage serveur local:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void verify() {
        try {
            if (tokenArea == null || detailsArea == null) {
                System.out.println("[CheckIn] fx:id not injected (tokenArea/detailsArea null)");
                return;
            }

            String token = normalizeToken(tokenArea.getText());

            if (token.isBlank()) {
                detailsArea.setText("❌ Token vide.");
                return;
            }

            if (!token.contains(".")) {
                detailsArea.setText("❌ Token invalide.");
                return;
            }

            TicketApiClient api = new TicketApiClient();
            TicketApiClient.VerifyResp resp = api.verify(token);

            if (resp == null || !resp.valid || resp.details == null) {
                detailsArea.setText("❌ Ticket invalide ou introuvable en base.");
                return;
            }

            var d = resp.details;

            String header = resp.alreadyUsed
                    ? "⚠️ TICKET DÉJÀ UTILISÉ\n\n"
                    : "✅ TICKET VALIDE (check-in OK)\n\n";

            // ✅ PAS D'ID (ni reservationId, ni activiteId, ni userId)
            String out =
                    header +
                            "Statut : " + safe(d.statut) + "\n" +
                            "Utilisateur : " + safe(d.activiteNom) + "\n" + // (comme dans ton code)
                            "Activité : " + safe(d.activiteNom) + "\n" +
                            "Type : " + safe(d.typeActivite) + "\n" +
                            "Catégorie : " + safe(d.categorie) + "\n" +
                            "Niveau : " + safe(d.niveau) + "\n" +
                            "Nb Personnes : " + d.nbPersonnes + "\n" +
                            "Prix Unitaire : " + d.prixUnitaire + "\n" +
                            "TOTAL : " + d.total + "\n" +
                            "Checked-in : " + (d.checkedIn ? "OUI ✅" : "NON ❌") + "\n" +
                            "Heure : " + (d.checkinTime == null ? "-" : d.checkinTime);

            detailsArea.setText(out);

        } catch (Exception e) {
            e.printStackTrace();
            if (detailsArea != null) {
                detailsArea.setText("❌ Erreur:\n" + e.getMessage());
            }
        }
    }

    // ✅ NEW: Retourner vers UserSeances.fxml
    @FXML
    private void retourner(ActionEvent event) {
        SceneUtils.loadScene("/GUI/UserSeances.fxml", (javafx.scene.Node) event.getSource());
    }

    private String normalizeToken(String input) {
        if (input == null) return "";
        String txt = input.trim();

        int idx = txt.indexOf("ECOA|");
        if (idx >= 0) txt = txt.substring(idx + 5);

        return txt.replaceAll("\\s+", "").trim();
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}