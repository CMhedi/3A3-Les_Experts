package controllers;

import Services.interfaces.CaptchaService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class CaptchaVerificationController {

    @FXML private ImageView imgCaptcha;
    @FXML private TextField tfInput;
    @FXML private Label lblError;

    private final CaptchaService captchaService = new CaptchaService();

    private String currentCode;

    // true si validé, false si annulé
    private Consumer<Boolean> onResult;

    // appelé à chaque erreur
    private Runnable onWrongAttempt;

    @FXML
    public void initialize() {
        regen(null);
    }

    public void setOnResult(Consumer<Boolean> onResult) {
        this.onResult = onResult;
    }

    public void setOnWrongAttempt(Runnable onWrongAttempt) {
        this.onWrongAttempt = onWrongAttempt;
    }

    @FXML
    public void regen(ActionEvent e) {
        currentCode = captchaService.generateCode(5);
        imgCaptcha.setImage(captchaService.renderCaptcha(currentCode, 260, 80));
        tfInput.clear();
        lblError.setText("");
    }

    @FXML
    public void verify(ActionEvent e) {
        String input = tfInput.getText() == null ? "" : tfInput.getText().trim().toUpperCase();

        if (input.equals(currentCode)) {
            if (onResult != null) onResult.accept(true);
            close(e);
        } else {
            lblError.setText("Code incorrect !");
            if (onWrongAttempt != null) onWrongAttempt.run();
            regen(null);
        }
    }

    @FXML
    public void cancel(ActionEvent e) {
        if (onResult != null) onResult.accept(false);
        close(e);
    }

    private void close(ActionEvent e) {
        Stage st = (Stage) ((Node) e.getSource()).getScene().getWindow();
        st.close();
    }
}