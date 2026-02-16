package GUI;

import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.interfaces.SeanceService;
import Services.interfaces.UserService;
import enums.StatutSeance;
import exceptions.ValidationException;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.util.List;

public class SeanceFormController {

    // ================= UI =================
    @FXML private Label titleLabel;
    @FXML private Label errorLabel;
    @FXML private TextField nomField;

    @FXML private DatePicker datePicker;
    @FXML private TextField heureDebutField;
    @FXML private TextField heureFinField;
    @FXML private TextField capaciteField;

    @FXML private ComboBox<StatutSeance> statutCombo;
    @FXML private ComboBox<UserApp> coachCombo;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final UserService userService = new UserService();

    private Seance seance;
    private int planningId;

    // =================================================
    // INIT
    // =================================================
    @FXML
    public void initialize() {

        errorLabel.setVisible(false);

        statutCombo.getItems().setAll(StatutSeance.values());

        loadCoachs();
        configureCoachCombo();
    }

    private void configureCoachCombo() {

        coachCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserApp item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : item.getNom() + " " + item.getPrenom());
            }
        });

        coachCombo.setButtonCell(coachCombo.getCellFactory().call(null));
    }

    private void loadCoachs() {
        try {
            List<UserApp> coachs = userService.getAllCoachs();
            coachCombo.getItems().setAll(coachs);

        } catch (Exception e) {
            DialogUtils.showError("Erreur",
                    "Impossible de charger les coachs.");
        }
    }

    // =================================================
    // SET MODE (ADD / EDIT)
    // =================================================
    public void setSeance(Seance s, int planningId) {

        this.planningId = planningId;

        this.seance = (s == null) ? new Seance() : s;

        if (s == null) {
            titleLabel.setText("Ajouter une Séance");
        } else {
            if (s != null) {
                nomField.setText(s.getNom());
            }


            titleLabel.setText("Modifier la Séance");

            datePicker.setValue(s.getDateSeance());
            heureDebutField.setText(s.getHeureDebut().toString());
            heureFinField.setText(s.getHeureFin().toString());
            capaciteField.setText(String.valueOf(s.getCapacite()));
            statutCombo.setValue(s.getStatutSeance());
        }
    }

    // =================================================
    // SAVE
    // =================================================
    @FXML
    private void handleSave() {

        clearValidationUI();
        seance.setNom(nomField.getText().trim());

        String validationError = validateForm();

        if (validationError != null) {
            showValidationError(validationError);
            return;
        }

        try {

            populateSeanceFromFields();

            if (seance.getIdSeance() == 0) {

                seanceService.add(seance);

                DialogUtils.showInfo(
                        "Succès",
                        "Séance ajoutée avec succès."
                );
            }
            else {

                seanceService.update(seance);

                DialogUtils.showInfo(
                        "Succès",
                        "Séance modifiée avec succès."
                );
            }

            close();
        }
        catch (ValidationException e) {

            showValidationError(e.getMessage());
        }
        catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Erreur lors de l'enregistrement."
            );
        }
    }

    private void populateSeanceFromFields() {

        seance.setDateSeance(datePicker.getValue());
        seance.setHeureDebut(LocalTime.parse(heureDebutField.getText()));
        seance.setHeureFin(LocalTime.parse(heureFinField.getText()));
        seance.setCapacite(Integer.parseInt(capaciteField.getText()));
        seance.setStatutSeance(statutCombo.getValue());
        seance.setIdCoach(coachCombo.getValue().getIdUser());
        seance.setIdPlanning(planningId);
    }

    // =================================================
    // VALIDATION
    // =================================================
    private String validateForm() {
        if (nomField.getText().isBlank())
            return "Le nom de la séance est obligatoire.";

        if (datePicker.getValue() == null) {
            addErrorStyle(datePicker);
            return "La date est obligatoire.";
        }

        if (heureDebutField.getText().isBlank()) {
            addErrorStyle(heureDebutField);
            return "L'heure de début est obligatoire.";
        }

        if (heureFinField.getText().isBlank()) {
            addErrorStyle(heureFinField);
            return "L'heure de fin est obligatoire.";
        }

        try {
            LocalTime debut = LocalTime.parse(heureDebutField.getText());
            LocalTime fin = LocalTime.parse(heureFinField.getText());

            if (!debut.isBefore(fin)) {
                addErrorStyle(heureDebutField);
                addErrorStyle(heureFinField);
                return "L'heure de début doit être avant l'heure de fin.";
            }

        } catch (Exception e) {
            addErrorStyle(heureDebutField);
            addErrorStyle(heureFinField);
            return "Format heure invalide (HH:mm).";
        }

        try {
            int cap = Integer.parseInt(capaciteField.getText());
            if (cap <= 0) {
                addErrorStyle(capaciteField);
                return "La capacité doit être supérieure à 0.";
            }
        } catch (Exception e) {
            addErrorStyle(capaciteField);
            return "Capacité invalide.";
        }

        if (statutCombo.getValue() == null) {
            addErrorStyle(statutCombo);
            return "Choisissez un statut.";
        }

        if (coachCombo.getValue() == null) {
            addErrorStyle(coachCombo);
            return "Choisissez un coach.";
        }

        return null;
    }

    // =================================================
    // UI VALIDATION HELPERS
    // =================================================
    private void showValidationError(String message) {

        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearValidationUI() {

        errorLabel.setVisible(false);

        removeErrorStyle(datePicker);
        removeErrorStyle(heureDebutField);
        removeErrorStyle(heureFinField);
        removeErrorStyle(capaciteField);
        removeErrorStyle(statutCombo);
        removeErrorStyle(coachCombo);
    }

    private void addErrorStyle(Control field) {
        field.getStyleClass().add("field-error");
    }

    private void removeErrorStyle(Control field) {
        field.getStyleClass().remove("field-error");
    }

    // =================================================
    // CANCEL
    // =================================================
    @FXML
    private void handleCancel() {

        if (DialogUtils.showConfirmation(
                "Annuler",
                "Fermer sans enregistrer ?"
        )) {
            close();
        }
    }

    private void close() {
        Stage stage =
                (Stage) datePicker.getScene().getWindow();
        stage.close();
    }
}
