package controllers;

import Entities.Inscription;
import Entities.Pack;
import Entities.UserApp;
import Services.EcoWhatsAppTemplates;
import Services.WhatsAppService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class WhatsAppDialogController {

    @FXML private TextField txtPhone;
    @FXML private ComboBox<EcoWhatsAppTemplates.Type> cbType;
    @FXML private TextArea txtMessage;
    @FXML private Label lblStatus;

    private final WhatsAppService wa = new WhatsAppService();

    private UserApp user;
    private Pack pack;
    private Inscription insc;

    @FXML
    private void initialize() {
        cbType.getItems().setAll(EcoWhatsAppTemplates.Type.values());
        cbType.getSelectionModel().select(EcoWhatsAppTemplates.Type.CONFIRMATION);

        cbType.valueProperty().addListener((obs, o, n) -> refreshMessage());
        refreshMessage();
    }

    public void setContext(UserApp user, Pack pack, Inscription insc, String phoneE164) {
        this.user = user;
        this.pack = pack;
        this.insc = insc;
        if (phoneE164 != null) txtPhone.setText(phoneE164);
        refreshMessage();
    }

    private void refreshMessage() {
        EcoWhatsAppTemplates.Type type = cbType.getValue();
        if (type == null) type = EcoWhatsAppTemplates.Type.CONFIRMATION;
        txtMessage.setText(EcoWhatsAppTemplates.build(type, user, pack, insc));
        lblStatus.setText(wa.isConfigured()
                ? "WhatsApp prêt ✅"
                : "⚠️ Configure WHATSAPP_TOKEN + WHATSAPP_PHONE_NUMBER_ID (ENV).");
    }

    @FXML
    private void onSend() {
        try {
            lblStatus.setText("Envoi en cours...");
            WhatsAppService.SendResult res = wa.sendTextMessage(txtPhone.getText(), txtMessage.getText());
            if (res.ok) {
                lblStatus.setText("Envoyé ✅ (HTTP " + res.statusCode + ")");
            } else {
                lblStatus.setText("Erreur ❌ (HTTP " + res.statusCode + "): " + res.responseBody);
            }
        } catch (Exception e) {
            lblStatus.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void onClose() {
        Stage st = (Stage) txtPhone.getScene().getWindow();
        st.close();
    }
}