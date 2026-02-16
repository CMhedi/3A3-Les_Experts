package GUI;

import GUI.utils.SceneUtils;
import javafx.fxml.FXML;

public class MenuController {

    // =============================
    // ADMIN
    // =============================
    @FXML
    void openAdmin(javafx.event.ActionEvent event) {

        SceneUtils.loadScene(
                "/AdminPlanningView.fxml",
                "/admin.css",
                (javafx.scene.Node) event.getSource()
        );
    }

    // =============================
    // COACH
    // =============================
    @FXML
    void openCoach(javafx.event.ActionEvent event) {

        SceneUtils.loadScene(
                "/CoachDashboard.fxml",
                "/admin.css",
                (javafx.scene.Node) event.getSource()
        );
    }

    // =============================
    // USER
    // =============================
    @FXML
    void openUser(javafx.event.ActionEvent event) {

        SceneUtils.loadScene(
                "/UserSeanceView.fxml",
                "/admin.css",
                (javafx.scene.Node) event.getSource()
        );
    }
}
