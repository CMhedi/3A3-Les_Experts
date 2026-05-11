package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;

public class MainLayoutController {

    @FXML private Button btnDashboard;
    @FXML private Button btnPacks;
    @FXML private Button btnInscriptions;
    @FXML private Button btnPackBuilder;
    @FXML private Button btnInsights;

    @FXML private StackPane contentPane;
    @FXML private VBox sidebarContent;
    @FXML private HBox topbar;

    @FXML
    private void initialize() {
        animateIntro();
    }

    @FXML
    private void openDashboard() {
        setActive(btnDashboard);
        loadIntoCenter("/fxml/Dashboard.fxml", "Dashboard");
    }

    @FXML
    private void openPacks() {
        setActive(btnPacks);
        loadIntoCenter("/fxml/PackList.fxml", "Packs");
    }

    @FXML
    private void openInscriptions() {
        setActive(btnInscriptions);
        loadIntoCenter("/fxml/InscriptionList.fxml", "Inscriptions");
    }

    @FXML
    private void openPackBuilder() {
        setActive(btnPackBuilder);
        loadIntoCenter("/fxml/PackInscriptionBuilder.fxml", "Pack Builder");
    }

    @FXML
    private void openInsights() {
        setActive(btnInsights);
        loadIntoCenter("/fxml/AdminInsights.fxml", "Insights");
    }
    @FXML private Button btnReservActiv;
    @FXML
    private void openReservActiv() {

        setActive(btnReservActiv);
        loadIntoCenter("/GUI/AdminActivites.fxml", "ReservActiv");
    }

    @FXML
    private void openSideBar(ActionEvent event) {
        try {
            URL sideUrl = getClass().getResource("/gui/SideBar.fxml");
            if (sideUrl == null) {
                System.out.println("❌ SideBar.fxml introuvable -> /gui/SideBar.fxml");
                return;
            }

            FXMLLoader sideLoader = new FXMLLoader(sideUrl);
            Parent sideBar = sideLoader.load();

            BorderPane shell = new BorderPane();
            shell.setLeft(sideBar);

            URL usersUrl = getClass().getResource("/gui/AdminUsers.fxml");
            if (usersUrl != null) {
                Parent usersView = FXMLLoader.load(usersUrl);
                shell.setCenter(usersView);
            } else {
                Label lbl = new Label("AdminUsers.fxml introuvable ( /gui/AdminUsers.fxml )");
                lbl.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
                shell.setCenter(new StackPane(lbl));
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();

            Scene newScene = new Scene(shell, w, h);

            URL adminCss = getClass().getResource("/admin.css");
            if (adminCss != null) newScene.getStylesheets().add(adminCss.toExternalForm());

            stage.setScene(newScene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout() {
        System.out.println("Logout...");
    }

    private void loadIntoCenter(String fxmlPath, String fallbackTitle) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                throw new IllegalArgumentException("FXML introuvable: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();
            animateCenterSwap(view);

        } catch (Exception e) {
            e.printStackTrace();
            Label err = new Label("Erreur chargement : " + fxmlPath + "\n\n" + e.getMessage());
            err.getStyleClass().add("page-subtitle");
            animateCenterSwap(err);
        }
    }

    private void animateCenterSwap(Node newView) {
        if (contentPane == null) return;

        newView.setOpacity(0);
        newView.setScaleX(0.985);
        newView.setScaleY(0.985);

        contentPane.getChildren().setAll(newView);

        FadeTransition fade = new FadeTransition(Duration.millis(260), newView);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(260), newView);
        scale.setFromX(0.985);
        scale.setFromY(0.985);
        scale.setToX(1.0);
        scale.setToY(1.0);

        fade.play();
        scale.play();
    }

    private void setActive(Button active) {
        // Liste à jour sans btnEvenements
        Button[] navButtons = {btnDashboard, btnPacks, btnInscriptions, btnPackBuilder, btnInsights};

        for (Button btn : navButtons) {
            if (btn != null) btn.getStyleClass().remove("nav-active");
        }

        if (active != null) {
            active.getStyleClass().add("nav-active");
        }
    }

    private void animateIntro() {
        if (sidebarContent != null) {
            sidebarContent.setOpacity(0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(420), sidebarContent);
            tt.setFromX(-14);
            tt.setToX(0);

            FadeTransition ft = new FadeTransition(Duration.millis(420), sidebarContent);
            ft.setFromValue(0);
            ft.setToValue(1);

            tt.play();
            ft.play();
        }

        if (topbar != null) {
            topbar.setOpacity(0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(380), topbar);
            tt.setFromY(-8);
            tt.setToY(0);

            FadeTransition ft = new FadeTransition(Duration.millis(380), topbar);
            ft.setFromValue(0);
            ft.setToValue(1);

            tt.play();
            ft.play();
        }
    }
}