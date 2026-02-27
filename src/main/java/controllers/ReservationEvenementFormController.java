package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import enums.StatutReservation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReservationEvenementFormController {

    @FXML private Label titleLabel;
    @FXML private DatePicker datePicker;
    @FXML private TextField heureField;
    @FXML private TextField minuteField;
    @FXML private ComboBox<StatutReservation> statutCombo;
    @FXML private TextField billetsField;
    @FXML private TextField eventIdField;
    @FXML private Label errorLabel;

    private final ReservationEvenementService service = new ReservationEvenementService();
    private ReservationEvenement existing;
    private boolean saved = false;

    @FXML
    public void initialize() {
        statutCombo.getItems().setAll(StatutReservation.values());

        // Valeurs par défaut pour faciliter la saisie
        datePicker.setValue(LocalDate.now());
        heureField.setText("12");
        minuteField.setText("00");
    }

    public void setData(ReservationEvenement existing) {
        this.existing = existing;
        if (existing == null) {
            titleLabel.setText("Nouvelle réservation");
            return;
        }
        titleLabel.setText("Modifier la réservation");

        // Remplissage des champs pour la modification
        if (existing.getDateReservation() != null) {
            datePicker.setValue(existing.getDateReservation().toLocalDate());
            heureField.setText(String.format("%02d", existing.getDateReservation().getHour()));
            minuteField.setText(String.format("%02d", existing.getDateReservation().getMinute()));
        }
        statutCombo.setValue(existing.getStatutRes());
        billetsField.setText(String.valueOf(existing.getNbBillets()));
        eventIdField.setText(String.valueOf(existing.getIdEvenement()));
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void onSave() {
        errorLabel.setText(""); // Reset du message d'erreur
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");

        try {
            // --- CONTRÔLE DE SAISIE ---

            // 1. Vérification de la Date
            LocalDate date = datePicker.getValue();
            if (date == null) {
                showError("Veuillez sélectionner une date.");
                return;
            }

            // 2. Vérification Heure/Minute (doivent être des entiers valides)
            int hh = validateInt(heureField.getText(), "L'heure doit être un nombre.");
            int mm = validateInt(minuteField.getText(), "Les minutes doivent être un nombre.");

            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) {
                showError("Heure (0-23) ou Minute (0-59) invalide.");
                return;
            }

            // 3. Vérification du Statut
            StatutReservation statut = statutCombo.getValue();
            if (statut == null) {
                showError("Veuillez choisir un statut.");
                return;
            }

            // 4. Vérification du nombre de billets
            int nbBillets = validateInt(billetsField.getText(), "Le nombre de billets doit être un nombre entier.");
            if (nbBillets <= 0) {
                showError("Le nombre de billets doit être supérieur à 0.");
                return;
            }

            // 5. Vérification de l'ID Event
            int eventId = validateInt(eventIdField.getText(), "L'ID Événement doit être un nombre.");
            if (eventId <= 0) {
                showError("ID Événement invalide.");
                return;
            }

            // --- TRAITEMENT SI TOUT EST VALIDE ---
            LocalDateTime dateComplete = LocalDateTime.of(date, LocalTime.of(hh, mm));

            ReservationEvenement r = (existing == null) ? new ReservationEvenement() : existing;
            r.setDateReservation(dateComplete);
            r.setStatutRes(statut);
            r.setNbBillets(nbBillets);
            r.setIdEvenement(eventId);

            if (existing == null) {
                service.add(r);
            } else {
                service.update(r);
            }

            saved = true;
            close();

        } catch (Exception e) {
            showError("Erreur de base de données : " + e.getMessage());
        }
    }

    // Méthode utilitaire pour valider les entiers
    private int validateInt(String text, String errorMsg) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            showError(errorMsg);
            throw new RuntimeException("Validation failed"); // Interrompt l'exécution
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    @FXML private void onCancel() { close(); }

    private void close() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}