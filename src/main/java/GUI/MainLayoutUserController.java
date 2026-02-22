package GUI;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

public class MainLayoutUserController {

    @FXML
    private BorderPane mainPaneUser;

    @FXML
    private SideBarUserController sideBarUserController;

    @FXML
    public void initialize() {
        sideBarUserController.setMainPane(mainPaneUser);
    }
}