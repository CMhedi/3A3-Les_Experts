package Utiles;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

public final class CaptchaDialog {

    private CaptchaDialog() {}

    /**
     * ✅ Recommended: pass owner window so dialog never hides behind
     */
    public static boolean confirmDeletion(Window owner, String title, String message) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);

        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        ButtonType okBtn = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, cancelBtn);

        Label lblMsg = new Label(message);
        lblMsg.setWrapText(true);

        ImageView img = new ImageView();
        img.setFitWidth(220);
        img.setFitHeight(70);
        img.setPreserveRatio(false);

        TextField input = new TextField();
        input.setPromptText("Tape le code CAPTCHA");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #d43c3c; -fx-font-size: 12px;");

        Button refresh = new Button("↻");
        refresh.setFocusTraversable(false);

        // create captcha (JavaFX-only)
        final String[] currentText = new String[1];
        Runnable regenerate = () -> {
            currentText[0] = CaptchaUtilFX.randomText(5);
            img.setImage(CaptchaUtilFX.renderImage(currentText[0], 220, 70));
            input.clear();
            lblErr.setText("");
        };
        regenerate.run();

        refresh.setOnAction(e -> regenerate.run());

        HBox row = new HBox(10, img, refresh);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, lblMsg, row, input, lblErr);
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);

        // Disable OK until correct captcha
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okBtn);
        okButton.setDisable(true);

        input.textProperty().addListener((obs, old, val) -> {
            boolean ok = val != null && val.trim().equalsIgnoreCase(currentText[0]);
            okButton.setDisable(!ok);
            if (val == null || val.isBlank()) lblErr.setText("");
            else if (!ok) lblErr.setText("Captcha incorrect ❌");
            else lblErr.setText("");
        });

        dialog.setResultConverter(bt -> bt == okBtn);
        return dialog.showAndWait().orElse(false);
    }

    /**
     * Keep old signature if you call it elsewhere
     */
    public static boolean confirmDeletion(String title, String message) {
        return confirmDeletion(null, title, message);
    }
}