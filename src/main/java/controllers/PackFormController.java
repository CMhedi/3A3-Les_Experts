package controllers;

import Entities.Pack;
import Services.PackService;
import enums.StatutPack;
import enums.TypePack;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class PackFormController {

    @FXML private Label lblTitle;
    @FXML private TextField txtNom;
    @FXML private ComboBox<TypePack> cmbType;
    @FXML private TextField txtPrixBase;
    @FXML private TextField txtReduction;
    @FXML private Spinner<Integer> spMax;
    @FXML private ComboBox<StatutPack> cmbStatut;

    private final PackService service = new PackService();
    private Pack editing;
    private Runnable onSaved;

    @FXML
    private void initialize() {
        cmbType.getItems().setAll(TypePack.values());
        cmbStatut.getItems().setAll(StatutPack.values());

        spMax.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 10));

        // placeholders
        txtPrixBase.setPromptText("ex: 50.00");
        txtReduction.setPromptText("ex: 5.00");
    }

    public void setPack(Pack pack) {
        this.editing = pack;
        if (pack == null) {
            lblTitle.setText("Ajouter un pack");
            cmbType.getSelectionModel().selectFirst();
            cmbStatut.getSelectionModel().selectFirst();
        } else {
            lblTitle.setText("Modifier le pack #" + pack.getIdPack());
            txtNom.setText(pack.getNom());
            cmbType.getSelectionModel().select(pack.getTypePack());
            txtPrixBase.setText(pack.getPrixBase() == null ? "" : pack.getPrixBase().toPlainString());
            txtReduction.setText(pack.getReduction() == null ? "" : pack.getReduction().toPlainString());
            spMax.getValueFactory().setValue(pack.getNbActivitesMax());
            cmbStatut.getSelectionModel().select(pack.getStatutPack());
        }
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    private void onCancel() {
        Stage st = (Stage) txtNom.getScene().getWindow();
        st.close();
    }

    @FXML
    private void onSave() {
        try {
            String nom = safe(txtNom.getText());
            if (nom.isEmpty()) {
                showInfo("Validation", "Le nom est obligatoire.");
                return;
            }

            BigDecimal prix = parseMoney(txtPrixBase.getText(), "Prix de base");
            BigDecimal red = parseMoney(txtReduction.getText(), "Réduction");
            int max = spMax.getValue();

            if (editing == null) editing = new Pack();
            editing.setNom(nom);
            editing.setTypePack(cmbType.getValue());
            editing.setPrixBase(prix);
            editing.setReduction(red);
            editing.setNbActivitesMax(max);
            editing.setStatutPack(cmbStatut.getValue());

            if (editing.getIdPack() == 0) {
                service.add(editing);
            } else {
                service.update(editing);
            }

            if (onSaved != null) onSaved.run();
            onCancel();
        } catch (Exception e) {
            e.printStackTrace();
            showError(e.getMessage());
        }
    }

    private BigDecimal parseMoney(String raw, String label) {
        String s = safe(raw);
        if (s.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.replace(',', '.'));
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " invalide (ex: 50.00)");
        }
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText("Impossible d'enregistrer");
        a.setContentText(msg);
        a.showAndWait();
    }
}
