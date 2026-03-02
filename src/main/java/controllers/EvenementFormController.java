package controllers;

import Entities.Evenement;
import Services.EvenementService;
import enums.CategorieEvenement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EvenementFormController {

    @FXML
    private TextField txtTitre;
    @FXML
    private ComboBox<CategorieEvenement> cbCategorie;
    @FXML
    private DatePicker dpDate;
    @FXML
    private TextField txtHeure;
    @FXML
    private TextField txtLieu;
    @FXML
    private TextField txtPlaces;
    @FXML
    private ComboBox<String> cbStatut;
    @FXML
    private TextField txtPrix;
    @FXML
    private TextArea txtDescription;
    @FXML
    private Label lblInfo;

    private final EvenementService evenementService = new EvenementService();
    private Evenement editing;
    private Runnable onSaved;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        cbCategorie.getItems().setAll(CategorieEvenement.values());
        cbStatut.getItems().setAll("OUVERT", "ANNULE", "TERMINE");

        // ✅ Bloquer les dates passées dans le calendrier
        dpDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        hideInfo();
    }

    public void setData(Evenement e) {
        this.editing = e;
        if (e == null)
            return;
        txtTitre.setText(e.getTitre());
        cbCategorie.setValue(e.getCategorieEvt());
        if (e.getDateEvent() != null) {
            dpDate.setValue(e.getDateEvent().toLocalDate());
            txtHeure.setText(e.getDateEvent().toLocalTime().format(TIME_FMT));
        }
        txtLieu.setText(e.getLieu());
        txtPlaces.setText(String.valueOf(e.getNbPlaces()));
        cbStatut.setValue(e.getStatut());
        txtPrix.setText(String.valueOf(e.getPrix()));
        txtDescription.setText(e.getDescription());
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onSave() {
        if (validateFields()) {
            try {
                Evenement e = (editing == null) ? new Evenement() : editing;
                LocalTime t = LocalTime.parse(txtHeure.getText().trim(), TIME_FMT);

                e.setTitre(txtTitre.getText().trim());
                e.setCategorieEvt(cbCategorie.getValue());
                e.setDateEvent(LocalDateTime.of(dpDate.getValue(), t));
                e.setLieu(txtLieu.getText().trim());
                e.setNbPlaces(Integer.parseInt(txtPlaces.getText().trim()));
                e.setStatut(cbStatut.getValue());
                e.setPrix(Double.parseDouble(txtPrix.getText().trim()));
                e.setDescription(txtDescription.getText() == null ? "" : txtDescription.getText().trim());

                if (editing == null)
                    evenementService.add(e);
                else
                    evenementService.update(e);

                if (onSaved != null)
                    onSaved.run();
                onCancel();
            } catch (Exception ex) {
                showError("Erreur : " + ex.getMessage());
            }
        }
    }

    private boolean validateFields() {
        resetStyles();
        boolean valid = true;

        if (isBlank(txtTitre.getText())) {
            markError(txtTitre);
            valid = false;
        }
        if (cbCategorie.getValue() == null) {
            markError(cbCategorie);
            valid = false;
        }
        if (dpDate.getValue() == null || dpDate.getValue().isBefore(LocalDate.now())) {
            markError(dpDate);
            valid = false;
        }
        if (!isValidTime(txtHeure.getText())) {
            markError(txtHeure);
            valid = false;
        }
        if (isBlank(txtLieu.getText())) {
            markError(txtLieu);
            valid = false;
        }
        if (!isPositiveInt(txtPlaces.getText())) {
            markError(txtPlaces);
            valid = false;
        }
        if (cbStatut.getValue() == null) {
            markError(cbStatut);
            valid = false;
        }
        if (!isPositiveDouble(txtPrix.getText())) {
            markError(txtPrix);
            valid = false;
        }

        if (!valid)
            showError("Veuillez vérifier les champs en rouge (Date future obligatoire).");
        return valid;
    }

    private void markError(Control c) {
        c.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");
    }

    private void resetStyles() {
        Control[] cs = { txtTitre, cbCategorie, dpDate, txtHeure, txtLieu, txtPlaces, cbStatut, txtPrix };
        for (Control c : cs)
            c.setStyle("");
        hideInfo();
    }

    private void showError(String msg) {
        lblInfo.setText(msg);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);
        lblInfo.setStyle("-fx-text-fill: #ef4444;");
    }

    private void hideInfo() {
        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isPositiveInt(String s) {
        try {
            return Integer.parseInt(s.trim()) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPositiveDouble(String s) {
        try {
            return Double.parseDouble(s.trim()) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidTime(String s) {
        try {
            LocalTime.parse(s.trim(), TIME_FMT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @FXML
    private void onCancel() {
        txtTitre.getScene().getWindow().hide();
    }
}