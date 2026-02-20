package controllers;

import Entities.Evenement;
import Services.EvenementService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EvenementFormController {

    @FXML private TextField txtTitre;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private DatePicker dpDate;
    @FXML private TextField txtHeure;
    @FXML private TextField txtLieu;
    @FXML private TextField txtPlaces;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextArea txtDescription;
    @FXML private Label lblInfo;

    private final EvenementService evenementService = new EvenementService();

    private Evenement editing;          // null => création, sinon édition
    private Runnable onSaved;           // callback refresh

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        cbCategorie.getItems().setAll("Randonnée", "Camping", "Kayak", "Escalade");
        cbStatut.getItems().setAll("PLANIFIE", "OUVERT", "COMPLET", "ANNULE");

        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    // ====== appelé depuis EvenementController ======
    public void setData(Evenement e) {
        this.editing = e;

        if (e == null) {
            return;
        }

        // --- ADAPTE ces getters/setters selon ton entity ---
        txtTitre.setText(nvl(e.getTitre()));
        cbCategorie.setValue(e.getCategorieEvt() == null ? null : e.getCategorieEvt().toString());

        LocalDateTime dt = e.getDateEvent();
        if (dt != null) {
            dpDate.setValue(dt.toLocalDate());
            txtHeure.setText(dt.toLocalTime().format(TIME_FMT));
        }

        txtLieu.setText(nvl(e.getLieu()));
        txtPlaces.setText(e.getNbPlaces() == null ? "" : String.valueOf(e.getNbPlaces()));
        cbStatut.setValue(nvl(e.getStatut()));
        txtDescription.setText(nvl(e.getDescription()));
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onSave() {
        clearValidation();

        boolean ok = true;

        if (isBlank(txtTitre.getText())) { markError(txtTitre); ok = false; }
        if (cbCategorie.getValue() == null) { markError(cbCategorie); ok = false; }
        if (dpDate.getValue() == null) { markError(dpDate); ok = false; }
        if (!isValidTime(txtHeure.getText())) { markError(txtHeure); ok = false; }
        if (isBlank(txtLieu.getText())) { markError(txtLieu); ok = false; }
        if (!isPositiveInt(txtPlaces.getText())) { markError(txtPlaces); ok = false; }
        if (cbStatut.getValue() == null) { markError(cbStatut); ok = false; }

        if (!ok) {
            showError("Veuillez corriger les champs en rouge.");
            return;
        }

        try {
            Evenement e = (editing == null) ? new Evenement() : editing;

            LocalDate d = dpDate.getValue();
            LocalTime t = LocalTime.parse(txtHeure.getText().trim(), TIME_FMT);
            LocalDateTime dateEvent = LocalDateTime.of(d, t);

            // --- ADAPTE ces setters selon ton entity ---
            e.setTitre(txtTitre.getText().trim());
            e.setCategorieEvt(cbCategorie.getValue()); // si ton type est enum -> convertis ici
            e.setDateEvent(dateEvent);
            e.setLieu(txtLieu.getText().trim());
            e.setNbPlaces(Integer.parseInt(txtPlaces.getText().trim()));
            e.setStatut(cbStatut.getValue());
            e.setDescription(txtDescription.getText() == null ? "" : txtDescription.getText().trim());

            if (editing == null) {
                evenementService.add(e);      // ou creer(...)
                showSuccess("Événement ajouté avec succès.");
            } else {
                evenementService.update(e);   // ou modifier(...)
                showSuccess("Événement modifié avec succès.");
            }

            if (onSaved != null) onSaved.run();

            // fermer la fenêtre
            txtTitre.getScene().getWindow().hide();

        } catch (Exception ex) {
            showError("Erreur lors de l'enregistrement : " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        txtTitre.getScene().getWindow().hide();
    }

    // ================= Helpers validation =================

    private void clearValidation() {
        reset(txtTitre);
        reset(cbCategorie);
        reset(dpDate);
        reset(txtHeure);
        reset(txtLieu);
        reset(txtPlaces);
        reset(cbStatut);
        reset(txtDescription);
    }

    private void reset(Control c) {
        c.getStyleClass().removeAll("error", "valid");
    }

    private void markError(Control c) {
        c.getStyleClass().removeAll("valid");
        if (!c.getStyleClass().contains("error")) c.getStyleClass().add("error");
    }

    private void showError(String msg) {
        lblInfo.setText(msg);
        lblInfo.setManaged(true);
        lblInfo.setVisible(true);
        lblInfo.getStyleClass().setAll("formInfo", "error");
    }

    private void showSuccess(String msg) {
        lblInfo.setText(msg);
        lblInfo.setManaged(true);
        lblInfo.setVisible(true);
        lblInfo.getStyleClass().setAll("formInfo", "success");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isPositiveInt(String s) {
        try { return Integer.parseInt(s.trim()) > 0; }
        catch (Exception e) { return false; }
    }

    private boolean isValidTime(String s) {
        if (isBlank(s)) return false;
        try { LocalTime.parse(s.trim(), TIME_FMT); return true; }
        catch (Exception e) { return false; }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}