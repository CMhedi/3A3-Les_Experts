package controllers;

import Entities.ReservationEvenement;
import Services.ReservationEvenementService;
import enums.StatutReservation;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReservationEvenementFormController {

    @FXML private Label titleLabel;

    @FXML private TextField idField;
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

        heureField.setText("12");
        minuteField.setText("00");
        datePicker.setValue(LocalDate.now());
    }

    public void setData(ReservationEvenement existing) {
        this.existing = existing;

        if (existing == null) {
            titleLabel.setText("Nouvelle réservation");
            idField.clear();
            return;
        }

        titleLabel.setText("Modifier réservation");
        idField.setText(String.valueOf(existing.getIdResEvt()));

        if (existing.getDateReservation() != null) {
            datePicker.setValue(existing.getDateReservation().toLocalDate());
            heureField.setText(String.format("%02d", existing.getDateReservation().getHour()));
            minuteField.setText(String.format("%02d", existing.getDateReservation().getMinute()));
        }

        if (existing.getStatutRes() != null) {
            statutCombo.getSelectionModel().select(existing.getStatutRes());
        }

        billetsField.setText(String.valueOf(existing.getNbBillets()));
        eventIdField.setText(String.valueOf(existing.getIdEvenement()));
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        errorLabel.setText("");

        try {
            LocalDate d = datePicker.getValue();
            if (d == null) {
                errorLabel.setText("Date obligatoire.");
                return;
            }

            int hh = parseInt(heureField.getText(), "Heure invalide.");
            int mm = parseInt(minuteField.getText(), "Minute invalide.");
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) {
                errorLabel.setText("Heure/Minute invalide.");
                return;
            }

            StatutReservation statut = statutCombo.getValue();
            if (statut == null) {
                errorLabel.setText("Statut obligatoire.");
                return;
            }

            int billets = parseInt(billetsField.getText(), "Nb billets invalide.");
            if (billets <= 0) {
                errorLabel.setText("Nb billets doit être > 0.");
                return;
            }

            int eventId = parseInt(eventIdField.getText(), "Event ID invalide.");
            if (eventId <= 0) {
                errorLabel.setText("Event ID doit être > 0.");
                return;
            }

            LocalDateTime dateRes = LocalDateTime.of(d, LocalTime.of(hh, mm));

            ReservationEvenement r = (existing == null) ? new ReservationEvenement() : existing;
            r.setDateReservation(dateRes);
            r.setStatutRes(statut);
            r.setNbBillets(billets);
            r.setIdEvenement(eventId);

            if (existing == null) {
                service.add(r);
            } else {
                service.update(r);
            }

            saved = true;
            close();

        } catch (Exception e) {
            errorLabel.setText("Erreur enregistrement: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }

    private int parseInt(String s, String message) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(message);
        }
    }
}
