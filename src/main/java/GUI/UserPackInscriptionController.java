package GUI;

import Entities.Pack;
import Entities.UserApp;
import Services.PackServiceUser;
import Services.InscriptionServiceUser;
import Services.UserService;
import Services.ticket.TicketPdfGenerator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.math.BigDecimal;

public class UserPackInscriptionController {

    private static final int CURRENT_USER_ID = 1; // ✅ test

    @FXML private Label lblUser, lblPack, lblPrix, lblInscrits, lblCapacite, lblTotal;
    @FXML private Spinner<Integer> spNb;

    private Pack selectedPack;

    private final UserService userService = new UserService();
    private final PackServiceUser packService = new PackServiceUser();
    private final InscriptionServiceUser inscriptionService = new InscriptionServiceUser();

    private UserApp currentUser;

    public void setSelectedPack(Pack pack) {
        this.selectedPack = pack;
        loadData();
    }

    @FXML
    private void initialize() {
        spNb.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        spNb.valueProperty().addListener((obs, o, n) -> updateTotal());
    }

    private void loadData() {
        try {
            currentUser = userService.getById(CURRENT_USER_ID);

            lblUser.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            lblPack.setText(selectedPack.getNom());

            BigDecimal prixFinal = selectedPack.getPrixBase().subtract(selectedPack.getReduction());
            if (prixFinal.compareTo(BigDecimal.ZERO) < 0) prixFinal = BigDecimal.ZERO;

            lblPrix.setText(prixFinal.toPlainString() + " DT");

            int inscrits = packService.countInscriptionsForPack(selectedPack.getIdPack());
            lblInscrits.setText(String.valueOf(inscrits));

            lblCapacite.setText(String.valueOf(selectedPack.getNbActivitesMax()));
            updateTotal();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur chargement confirmation.").showAndWait();
        }
    }

    private void updateTotal() {
        if (selectedPack == null) return;
        int nb = (spNb.getValue() == null) ? 1 : spNb.getValue();

        BigDecimal prixFinal = selectedPack.getPrixBase().subtract(selectedPack.getReduction());
        if (prixFinal.compareTo(BigDecimal.ZERO) < 0) prixFinal = BigDecimal.ZERO;

        BigDecimal total = prixFinal.multiply(BigDecimal.valueOf(nb));
        lblTotal.setText(total.toPlainString() + " DT");
    }

    @FXML
    private void backToList() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/UserPackList.fxml"));
            BorderPane pane = (BorderPane) lblUser.getScene().lookup("#mainPaneUser");
            if (pane != null) pane.setCenter(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void validate() {
        try {
            int nb = spNb.getValue();
            int inscrits = packService.countInscriptionsForPack(selectedPack.getIdPack());
            int capacite = selectedPack.getNbActivitesMax();

            if (inscrits + nb > capacite) {
                String msg = "Désolé " + currentUser.getPrenom() + " " + currentUser.getNom()
                        + ", le pack \"" + selectedPack.getNom()
                        + "\" est malheureusement saturé pour le moment.\n"
                        + "Merci de choisir un autre pack 🙏";
                new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
                backToList();
                return;
            }

            BigDecimal prixFinal = selectedPack.getPrixBase().subtract(selectedPack.getReduction());
            if (prixFinal.compareTo(BigDecimal.ZERO) < 0) prixFinal = BigDecimal.ZERO;

            BigDecimal total = prixFinal.multiply(BigDecimal.valueOf(nb));

            int newInscrId = inscriptionService.createUserInscription(
                    currentUser.getIdUser(),
                    selectedPack.getIdPack(),
                    total
            );

            // ✅ هذا باش يخدم بعد ما نزيدو overload في TicketPdfGenerator
            TicketPdfGenerator.generateTicketPdf(newInscrId, currentUser, selectedPack, total, nb);

            new Alert(Alert.AlertType.INFORMATION,
                    "Inscription validée ✅\nTicket généré avec succès (PDF + QR).").showAndWait();

            backToList();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur validation inscription.").showAndWait();
        }
    }
}