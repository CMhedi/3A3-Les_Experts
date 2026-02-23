package controllers;

import Entities.Inscription;
import Entities.Pack;
import Entities.UserApp;
import Services.InscriptionService;
import Services.PackService;
import Services.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class InscriptionFormController {

    @FXML private ComboBox<UserApp> cbUser;
    @FXML private ComboBox<Pack> cbPack;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Label lblDiscount;
    @FXML private TextField txtMontant;

    private final UserService userService = new UserService();
    private final PackService packService = new PackService();
    private final InscriptionService inscriptionService = new InscriptionService();

    private Inscription current;
    private Consumer<Void> onSaved;


    @FXML
    private void initialize() {

        try {
            cbUser.getItems().setAll(userService.getAll());
            cbPack.getItems().setAll(packService.getAll());
        } catch (Exception e) {
            e.printStackTrace();
        }

        cbStatut.getItems().setAll(inscriptionService.getAllowedStatutsFromDB());
        cbStatut.getSelectionModel().selectFirst();

        // ✅ IMPORTANT: recalcul automatique (réduction + montant)
        cbUser.valueProperty().addListener((obs, oldV, newV) -> updateMontant());
        cbPack.valueProperty().addListener((obs, oldV, newV) -> updateMontant());
        cbStatut.valueProperty().addListener((obs, oldV, newV) -> updateMontant()); // optionnel

        updateMontant(); // initialise l'affichage
    }



    public void setInscription(Inscription insc) {
        this.current = insc;

        if (insc != null) {
            // select user/pack by id
            cbUser.getSelectionModel().select(
                    cbUser.getItems().stream().filter(u -> u.getIdUser() == insc.getIdUser()).findFirst().orElse(null)
            );
            cbPack.getSelectionModel().select(
                    cbPack.getItems().stream().filter(p -> p.getIdPack() == insc.getIdPack()).findFirst().orElse(null)
            );
            if (insc.getStatutInscr() != null) cbStatut.getSelectionModel().select(insc.getStatutInscr());
        }
        updateMontant();
    }

    public void setOnSaved(Runnable r) {
        this.onSaved = v -> { r.run(); };
    }

    private void updateMontant() {
        UserApp u = cbUser.getValue();
        Pack p = cbPack.getValue();
        if (u == null || p == null) {
            lblDiscount.setText("0%");
            txtMontant.setText("");
            return;
        }

        int percent = inscriptionService.discountPercent(u.getIdUser());
        BigDecimal montant = inscriptionService.computeMontant(p, u.getIdUser());

        lblDiscount.setText(percent + "%");
        txtMontant.setText(montant + " DT");
    }

    @FXML
    private void onSave() {
        UserApp u = cbUser.getValue();
        Pack p = cbPack.getValue();
        if (u == null || p == null) return;

        if (current == null) current = new Inscription();

        current.setIdUser(u.getIdUser());
        current.setIdPack(p.getIdPack());
        current.setStatutInscr(cbStatut.getValue());

        if (current.getIdInscription() == 0) {
            inscriptionService.add(current, p);
        } else {
            inscriptionService.update(current, p);
        }

        if (onSaved != null) onSaved.accept(null);
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage st = (Stage) cbUser.getScene().getWindow();
        st.close();
    }
}
