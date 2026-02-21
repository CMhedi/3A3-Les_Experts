package Utiles;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class CaptchaDialog {

    private CaptchaDialog() {}

    /**
     * Returns true if captcha validated, else false.
     */
    public static boolean confirmDeletion(String title, String message) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);

        ButtonType okBtn = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, cancelBtn);

        Label lblMsg = new Label(message);

        ImageView img = new ImageView();
        img.setFitWidth(220);
        img.setFitHeight(70);
        img.setPreserveRatio(false);

        TextField input = new TextField();
        input.setPromptText("Tape le code CAPTCHA");

        Button refresh = new Button("↻");
        refresh.setFocusTraversable(false);

        // create captcha
        final String[] currentText = new String[1];
        Runnable regenerate = () -> {
            currentText[0] = CaptchaUtil.randomText(5);
            img.setImage(CaptchaUtil.renderImage(currentText[0], 220, 70));
            input.clear();
        };
        regenerate.run();

        refresh.setOnAction(e -> regenerate.run());

        HBox row = new HBox(10, img, refresh);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, lblMsg, row, input);
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);

        // Disable OK until correct captcha
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okBtn);
        okButton.setDisable(true);

        input.textProperty().addListener((obs, old, val) -> {
            okButton.setDisable(val == null || !val.trim().equalsIgnoreCase(currentText[0]));
        });

        dialog.setResultConverter(bt -> bt == okBtn);

        return dialog.showAndWait().orElse(false);
    }
}