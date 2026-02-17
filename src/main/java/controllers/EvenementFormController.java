package controllers;

import Entities.Evenement;
import Services.EvenementService;
import enums.CategorieEvenement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class EvenementFormController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<CategorieEvenement> cbCategorie;
    @FXML private DatePicker dpDate;
    @FXML private TextField txtHeure;
    @FXML private TextField txtLieu;
    @FXML private TextField txtPlaces;
    @FXML private TextField txtStatut;

    @FXML private Label lblInfo;

    private final EvenementService service = new EvenementService();
    private Evenement existing;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        cbCategorie.getItems().setAll(CategorieEvenement.values());
        if (txtHeure != null) txtHeure.setText("12:00");
    }

    public void setData(Evenement e) {
        this.existing = e;

        if (e == null) return;

        txtTitre.setText(nvl(e.getTitre()));
        txtDescription.setText(nvl(e.getDescription()));
        cbCategorie.setValue(e.getCategorieEvt());
        if (e.getDateEvent() != null) {
            dpDate.setValue(e.getDateEvent().toLocalDate());
            txtHeure.setText(e.getDateEvent().toLocalTime().toString());
        }
        txtLieu.setText(nvl(e.getLieu()));
        txtPlaces.setText(String.valueOf(e.getNbPlaces()));
        txtStatut.setText(nvl(e.getStatut()));
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    private void onSave() {
        try {
            String titre = nvl(txtTitre.getText()).trim();
            if (titre.isEmpty()) { lblInfo.setText("Titre obligatoire."); return; }

            if (cbCategorie.getValue() == null) { lblInfo.setText("Catégorie obligatoire."); return; }
            if (dpDate.getValue() == null) { lblInfo.setText("Date obligatoire."); return; }

            int places;
            try {
                places = Integer.parseInt(nvl(txtPlaces.getText()).trim());
            } catch (Exception ex) {
                lblInfo.setText("Places doit être un nombre.");
                return;
            }

            LocalTime time = parseTime(nvl(txtHeure.getText()).trim());
            LocalDateTime dateEvent = LocalDateTime.of(dpDate.getValue(), time);

            Evenement e = existing == null ? new Evenement() : existing;
            e.setTitre(titre);
            e.setDescription(nvl(txtDescription.getText()).trim());
            e.setCategorieEvt(cbCategorie.getValue());
            e.setDateEvent(dateEvent);
            e.setLieu(nvl(txtLieu.getText()).trim());
            e.setNbPlaces(places);
            e.setStatut(nvl(txtStatut.getText()).trim());

            if (existing == null) service.add(e);
            else service.update(e);

            if (onSaved != null) onSaved.run();
            close();

        } catch (Exception ex) {
            lblInfo.setText("Erreur: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) txtTitre.getScene().getWindow();
        stage.close();
    }

    private LocalTime parseTime(String value) {
        try {
            if (value.matches("\\d{2}:\\d{2}")) return LocalTime.parse(value);
        } catch (Exception ignored) {}
        return LocalTime.of(12, 0);
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
