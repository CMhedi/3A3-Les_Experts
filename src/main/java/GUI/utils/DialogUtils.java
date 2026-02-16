package GUI.utils;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class DialogUtils {

    private static void applyStyle(Alert alert, String headerStyleClass) {

        DialogPane dialogPane = alert.getDialogPane();

        dialogPane.getStylesheets().add(
                DialogUtils.class
                        .getResource("/admin.css")
                        .toExternalForm()
        );

        dialogPane.getStyleClass().add("custom-dialog");

        if (headerStyleClass != null) {
            dialogPane.getStyleClass().add(headerStyleClass);
        }
    }

    // =====================================================
    // GENERIC BUILDER
    // =====================================================
    private static Alert buildAlert(Alert.AlertType type,
                                    String title,
                                    String message,
                                    String headerStyle) {

        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(title);

        Text text = new Text(message);
        text.getStyleClass().add("dialog-message");

        TextFlow flow = new TextFlow(text);
        flow.setPrefWidth(400);

        alert.getDialogPane().setContent(flow);

        applyStyle(alert, headerStyle);

        return alert;
    }

    // =====================================================
    // INFO
    // =====================================================
    public static void showInfo(String title, String message) {

        Alert alert = buildAlert(
                Alert.AlertType.INFORMATION,
                title,
                message,
                "dialog-info"
        );

        Button okBtn = (Button) alert.getDialogPane()
                .lookupButton(ButtonType.OK);

        okBtn.getStyleClass().add("btn-primary");

        alert.showAndWait();
    }

    // =====================================================
    // WARNING
    // =====================================================
    public static void showWarning(String title, String message) {

        Alert alert = buildAlert(
                Alert.AlertType.WARNING,
                title,
                message,
                "dialog-warning"
        );

        Button okBtn = (Button) alert.getDialogPane()
                .lookupButton(ButtonType.OK);

        okBtn.getStyleClass().add("btn-secondary");

        alert.showAndWait();
    }

    // =====================================================
    // ERROR
    // =====================================================
    public static void showError(String title, String message) {

        Alert alert = buildAlert(
                Alert.AlertType.ERROR,
                title,
                message,
                "dialog-error"
        );

        Button okBtn = (Button) alert.getDialogPane()
                .lookupButton(ButtonType.OK);

        okBtn.getStyleClass().add("btn-danger");

        alert.showAndWait();
    }

    // =====================================================
    // CONFIRMATION
    // =====================================================
    public static boolean showConfirmation(String title, String message) {

        Alert alert = buildAlert(
                Alert.AlertType.CONFIRMATION,
                title,
                message,
                "dialog-confirm"
        );

        ButtonType btnYes =
                new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);

        ButtonType btnNo =
                new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnYes, btnNo);

        Button yesBtn = (Button) alert.getDialogPane()
                .lookupButton(btnYes);

        Button noBtn = (Button) alert.getDialogPane()
                .lookupButton(btnNo);

        yesBtn.getStyleClass().add("btn-primary");
        noBtn.getStyleClass().add("btn-secondary");

        return alert.showAndWait().orElse(btnNo) == btnYes;
    }
}
