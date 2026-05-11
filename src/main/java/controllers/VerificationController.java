package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.security.SecureRandom;
import java.util.function.Consumer;

public class VerificationController {

    @FXML private Label lblCode;
    @FXML private TextField tfInput;
    @FXML private Label lblError;

    private String currentCode;

    // callback: true = validé, false = annulé
    private Consumer<Boolean> onResult;

    // callback: appelé à chaque erreur (tentative ratée)
    private Runnable onWrongAttempt;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @FXML
    public void initialize() {
        generateCode();
    }

    public void setOnResult(Consumer<Boolean> onResult) {
        this.onResult = onResult;
    }

    public void setOnWrongAttempt(Runnable onWrongAttempt) {
        this.onWrongAttempt = onWrongAttempt;
    }

    private void generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        currentCode = sb.toString();
        lblCode.setText(currentCode);
    }

    @FXML
    private void regen(ActionEvent event) {
        generateCode();
        tfInput.clear();
        lblError.setText("");
    }

    @FXML
    private void verify(ActionEvent event) {
        String input = tfInput.getText() == null ? "" : tfInput.getText().trim();

        if (input.equalsIgnoreCase(currentCode)) {
            if (onResult != null) onResult.accept(true);
            close(event);
        } else {
            lblError.setText("Code incorrect !");
            if (onWrongAttempt != null) onWrongAttempt.run(); // ✅ notifie le parent
            generateCode();
            tfInput.clear();
        }
    }

    @FXML
    private void cancel(ActionEvent event) {
        if (onResult != null) onResult.accept(false);
        close(event);
    }

    private void close(ActionEvent event) {
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.close();
    }
}