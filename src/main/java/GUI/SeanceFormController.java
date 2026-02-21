package GUI;

import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.interfaces.SeanceService;
import Services.interfaces.UserService;
import enums.StatutSeance;
import exceptions.ValidationException;
import Entities.Session;
import Entities.UserApp;
import enums.RoleUser;
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

    @FXML private Spinner<LocalTime> heureDebutSpinner;
    @FXML private Spinner<LocalTime> heureFinSpinner;

    @FXML private TextField capaciteField;
    @FXML private ComboBox<StatutSeance> statutCombo;
    @FXML private ComboBox<UserApp> coachCombo;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final UserService userService = new UserService();
    private UserApp connectedUser;
    private Seance seance;
    private int planningId;

    // =================================================
    // INITIALISATION
    // =================================================
    @FXML
    public void initialize() {

        connectedUser = Session.getConnectedUser();

        if (connectedUser == null) {
            DialogUtils.showError(
                    "Erreur",
                    "Utilisateur non connecté."
            );
            close();
            return;
        }

        if (connectedUser.getRole() != RoleUser.ADMIN) {
            DialogUtils.showError(
                    "Accès refusé",
                    "Cette page est réservée aux administrateurs."
            );
            close();
            return;
        }

        errorLabel.setVisible(false);

        statutCombo.getItems().setAll(StatutSeance.values());

        loadCoachs();
        configureCoachCombo();
        initTimeSpinners();
    }

    // =================================================
    // CONFIG COACH
    // =================================================
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

            titleLabel.setText("Modifier la Séance");

            nomField.setText(s.getNom());
            datePicker.setValue(s.getDateSeance());

            heureDebutSpinner.getValueFactory()
                    .setValue(s.getHeureDebut());

            heureFinSpinner.getValueFactory()
                    .setValue(s.getHeureFin());

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
            } else {

                seanceService.update(seance);

                DialogUtils.showInfo(
                        "Succès",
                        "Séance modifiée avec succès."
                );
            }

            close();

        } catch (ValidationException e) {

            showValidationError(e.getMessage());

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Erreur lors de l'enregistrement."
            );
        }
    }

    private void populateSeanceFromFields() {

        seance.setNom(nomField.getText().trim());
        seance.setDateSeance(datePicker.getValue());
        seance.setHeureDebut(heureDebutSpinner.getValue());
        seance.setHeureFin(heureFinSpinner.getValue());
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

        LocalTime debut = heureDebutSpinner.getValue();
        LocalTime fin = heureFinSpinner.getValue();

        if (debut == null) {
            addErrorStyle(heureDebutSpinner);
            return "L'heure de début est obligatoire.";
        }

        if (fin == null) {
            addErrorStyle(heureFinSpinner);
            return "L'heure de fin est obligatoire.";
        }

        if (!debut.isBefore(fin)) {
            addErrorStyle(heureDebutSpinner);
            addErrorStyle(heureFinSpinner);
            return "L'heure de début doit être avant l'heure de fin.";
        }

        try {
            int cap = Integer.parseInt(capaciteField.getText());
            if (cap <= 0) {
                addErrorStyle(capaciteField);
                return "La capacité doit être > 0.";
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
    // SPINNER CONFIGURATION PRO
    // =================================================
    private void initTimeSpinners() {

        SpinnerValueFactory<LocalTime> debutFactory =
                new SpinnerValueFactory<>() {

                    {
                        setValue(LocalTime.of(8, 0));
                    }

                    @Override
                    public void decrement(int steps) {
                        setValue(getValue().minusMinutes(30 * steps));
                    }

                    @Override
                    public void increment(int steps) {
                        setValue(getValue().plusMinutes(30 * steps));
                    }
                };

        SpinnerValueFactory<LocalTime> finFactory =
                new SpinnerValueFactory<>() {

                    {
                        setValue(LocalTime.of(9, 0));
                    }

                    @Override
                    public void decrement(int steps) {
                        setValue(getValue().minusMinutes(30 * steps));
                    }

                    @Override
                    public void increment(int steps) {
                        setValue(getValue().plusMinutes(30 * steps));
                    }
                };

        heureDebutSpinner.setValueFactory(debutFactory);
        heureFinSpinner.setValueFactory(finFactory);

        heureDebutSpinner.setEditable(false);
        heureFinSpinner.setEditable(false);
    }

    // =================================================
    // UI HELPERS
    // =================================================
    private void showValidationError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearValidationUI() {
        errorLabel.setVisible(false);
        removeErrorStyle(datePicker);
        removeErrorStyle(heureDebutSpinner);
        removeErrorStyle(heureFinSpinner);
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
        Stage stage = (Stage) datePicker.getScene().getWindow();
        stage.close();
    }
}
