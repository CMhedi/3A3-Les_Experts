package GUI;

import Entities.Planning;
import GUI.utils.DialogUtils;
import Services.interfaces.PlanningService;
import exceptions.ValidationException;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PlanningFormController {

    // ================= UI =================
    @FXML private Label titleLabel;
    @FXML private TextField periodeField;
    @FXML private TextArea descriptionField;
    @FXML private Label errorLabel;

    // ================= SERVICE =================
    private final PlanningService planningService =
            new PlanningService();

    private Planning planning;

    // =================================================
    // INITIALIZATION
    // =================================================
    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    // =================================================
    // SET PLANNING (ADD / UPDATE MODE)
    // =================================================
    public void setPlanning(Planning p) {

        this.planning = (p == null) ? new Planning() : p;

        if (p == null) {
            titleLabel.setText("Ajouter un Planning");
        } else {
            titleLabel.setText("Modifier le Planning");
            periodeField.setText(p.getPeriode());
            descriptionField.setText(p.getDescription());
        }
    }

    // =================================================
    // SAVE
    // =================================================
    @FXML
    private void handleSave() {

        clearValidationUI();

        try {

            populatePlanningFromFields();

            if (planning.getIdPlanning() == 0) {
                planningService.add(planning);

                DialogUtils.showInfo(
                        "Succès",
                        "Planning ajouté avec succès."
                );
            }
            else {
                planningService.update(planning);

                DialogUtils.showInfo(
                        "Succès",
                        "Planning modifié avec succès."
                );
            }

            closeWindow();
        }
        catch (ValidationException e) {

            showValidationError(e.getMessage());
        }
        catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Une erreur interne est survenue."
            );
        }
    }

    // =================================================
    // POPULATE ENTITY
    // =================================================
    private void populatePlanningFromFields() {

        planning.setPeriode(
                periodeField.getText().trim());

        planning.setDescription(
                descriptionField.getText().trim());
    }

    // =================================================
    // VALIDATION UI
    // =================================================
    private void showValidationError(String message) {

        errorLabel.setText(message);
        errorLabel.setVisible(true);

        if (periodeField.getText().isBlank()) {
            addErrorStyle(periodeField);
        }

        if (descriptionField.getText().isBlank()) {
            addErrorStyle(descriptionField);
        }
    }

    private void clearValidationUI() {

        errorLabel.setVisible(false);

        removeErrorStyle(periodeField);
        removeErrorStyle(descriptionField);
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
                "Voulez-vous fermer sans enregistrer ?"
        )) {
            closeWindow();
        }
    }

    // =================================================
    // CLOSE WINDOW
    // =================================================
    private void closeWindow() {

        Stage stage =
                (Stage) periodeField.getScene().getWindow();

        stage.close();
    }
}
