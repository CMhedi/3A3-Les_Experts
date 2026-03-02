package GUI;

import Entities.Planning;
import Entities.Session;
import Entities.UserApp;
import Services.PlanningService;
import enums.RoleUser;
import enums.StatutPlanning;
import exceptions.ValidationException;
import GUI.utils.DialogUtils;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class PlanningFormController {

    // ================= UI =================
    @FXML private Label titleLabel;
    @FXML private TextField titreField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private ComboBox<StatutPlanning> statutCombo;
    @FXML private TextArea descriptionField;
    @FXML private Label errorLabel;

    // ================= SERVICE =================
    private final PlanningService planningService = new PlanningService();

    private Planning planning;
    private UserApp connectedUser;

    // =================================================
    // INITIALIZATION
    // =================================================
    @FXML
    public void initialize() {

        errorLabel.setVisible(false);

        statutCombo.setItems(
                FXCollections.observableArrayList(StatutPlanning.values())
        );

        connectedUser = Session.getConnectedUser();

        if (connectedUser == null || connectedUser.getRole() != RoleUser.ADMIN) {
            DialogUtils.showError("Erreur", "Accès refusé.");
            closeWindow();
        }
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

            titreField.setText(p.getTitre());
            dateDebutPicker.setValue(p.getDateDebut());
            dateFinPicker.setValue(p.getDateFin());
            statutCombo.setValue(p.getStatut());
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

            validateFields();

            populatePlanningFromFields();

            if (planning.getIdPlanning() == 0) {
                planningService.add(planning);
                DialogUtils.showInfo("Succès", "Planning ajouté avec succès.");
            } else {
                planningService.update(planning);
                DialogUtils.showInfo("Succès", "Planning modifié avec succès.");
            }

            closeWindow();

        } catch (ValidationException e) {
            showValidationError(e.getMessage());
        } catch (Exception e) {
            DialogUtils.showError("Erreur", e.getMessage());
        }
    }

    // =================================================
    // VALIDATION MÉTIER RÉELLE
    // =================================================
    private void validateFields() {

        if (titreField.getText().isBlank())
            throw new ValidationException("Le titre est obligatoire.");

        if (dateDebutPicker.getValue() == null)
            throw new ValidationException("La date de début est obligatoire.");

        if (dateFinPicker.getValue() == null)
            throw new ValidationException("La date de fin est obligatoire.");

        if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue()))
            throw new ValidationException("La date de fin doit être après la date de début.");

        if (statutCombo.getValue() == null)
            throw new ValidationException("Veuillez sélectionner un statut.");

        if (descriptionField.getText().isBlank())
            throw new ValidationException("La description est obligatoire.");
    }

    // =================================================
    // POPULATE ENTITY
    // =================================================
    private void populatePlanningFromFields() {

        planning.setTitre(titreField.getText().trim());
        planning.setDateDebut(dateDebutPicker.getValue());
        planning.setDateFin(dateFinPicker.getValue());
        planning.setStatut(statutCombo.getValue());
        planning.setDescription(descriptionField.getText().trim());
    }

    // =================================================
    // UI ERROR HANDLING
    // =================================================
    private void showValidationError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearValidationUI() {
        errorLabel.setVisible(false);
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
            closeWindow();
        }
    }

    // =================================================
    // CLOSE WINDOW
    // =================================================
    private void closeWindow() {
        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();
    }
}