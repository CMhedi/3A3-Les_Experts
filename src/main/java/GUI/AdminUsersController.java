package GUI;

import Entities.UserApp;
import GUI.utils.DialogUtils;
import Services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;

public class AdminUsersController {

    @FXML private ListView<UserApp> userListView;
    @FXML private VBox mainContent;
    @FXML private TextField txtSearch;

    private final UserService userService = new UserService();
    private final ObservableList<UserApp> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupUserListView();
        loadUserData();
        txtSearch.textProperty().addListener((obs, old, newVal) -> filterData(newVal));

        // Shortcut Ctrl + R pour actualiser
        Platform.runLater(() -> {
            if (mainContent != null && mainContent.getScene() != null) {
                mainContent.getScene().getAccelerators().put(
                    new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.R, javafx.scene.input.KeyCombination.CONTROL_DOWN),
                    this::loadUserData
                );
            }
        });
    }

    private void setupUserListView() {
        userListView.setCellFactory(param -> new ListCell<UserApp>() {
            @Override
            protected void updateItem(UserApp user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                // 1. Card Container (Slightly smaller height)
                HBox card = new HBox(0);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPrefHeight(70);
                card.setStyle("-fx-background-color: white; " +
                             "-fx-background-radius: 15; " +
                             "-fx-padding: 0 25; " +
                             "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
                
                // 2. Member Column (Avatar + Name)
                HBox memberBox = new HBox(15);
                memberBox.setAlignment(Pos.CENTER_LEFT);
                memberBox.setPrefWidth(250);

                String initial = (user.getNom() != null && !user.getNom().isBlank()) ? user.getNom().substring(0, 1).toUpperCase() : "?";
                if (user.getPrenom() != null && !user.getPrenom().isBlank()) {
                     initial = user.getPrenom().substring(0, 1).toUpperCase() + initial;
                }

                Label avatar = new Label(initial);
                avatar.setStyle("-fx-background-color: #F1F5F9; " +
                               "-fx-text-fill: #475569; " +
                               "-fx-font-weight: 800; " +
                               "-fx-font-size: 13px; " +
                               "-fx-min-width: 45; " +
                               "-fx-min-height: 45; " +
                               "-fx-background-radius: 12; " +
                               "-fx-alignment: center;");

                Label name = new Label(user.getPrenom() + " " + user.getNom());
                name.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #1E293B;");
                memberBox.getChildren().addAll(avatar, name);

                // 3. Coordinates Column
                VBox coordBox = new VBox(2);
                coordBox.setAlignment(Pos.CENTER_LEFT);
                coordBox.setPrefWidth(250);
                
                Label email = new Label("✉  " + (user.getEmail() != null ? user.getEmail() : "—"));
                email.setStyle("-fx-text-fill: #3B82F6; -fx-font-size: 12px; -fx-font-weight: bold;");
                
                Label phone = new Label("📞  " + (user.getTelephone() != null ? user.getTelephone() : "—"));
                phone.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
                coordBox.getChildren().addAll(email, phone);

                // 4. Role Column
                HBox roleBox = new HBox();
                roleBox.setAlignment(Pos.CENTER);
                roleBox.setPrefWidth(150);
                String roleText = (user.getRole() != null) ? user.getRole().name() : "USER_SIMPLE";
                Label roleBadge = new Label(roleText);
                String badgeStyle = "-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: 800;";
                if (roleText.equals("ADMIN")) {
                    badgeStyle += "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;";
                } else {
                    badgeStyle += "-fx-background-color: #DBEAFE; -fx-text-fill: #2563EB;";
                }
                roleBadge.setStyle(badgeStyle);
                roleBox.getChildren().add(roleBadge);

                // 5. Profil Column
                HBox profileBox = new HBox();
                profileBox.setAlignment(Pos.CENTER);
                profileBox.setPrefWidth(150);
                Label profileLbl = new Label("Membre Simple");
                profileLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
                profileBox.getChildren().add(profileLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // 6. Actions Column
                HBox actions = new HBox(8);
                actions.setAlignment(Pos.CENTER_RIGHT);

                Button btnView = new Button("👁");
                btnView.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-background-radius: 10; " +
                                "-fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32; -fx-font-size: 14px;");
                btnView.setOnAction(e -> handleViewUser(user));
                
                Button btnEdit = new Button("✎");
                btnEdit.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-background-radius: 10; " +
                                "-fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32; -fx-font-size: 14px;");
                btnEdit.setOnAction(e -> showEditPage(user));

                Button btnDel = new Button("🗑");
                btnDel.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 10; " +
                                "-fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32; -fx-font-size: 14px;");
                btnDel.setOnAction(e -> handleDeleteUser(user));

                actions.getChildren().addAll(btnView, btnEdit, btnDel);

                card.getChildren().addAll(memberBox, coordBox, roleBox, profileBox, spacer, actions);
                
                VBox cardWrapper = new VBox(card);
                cardWrapper.setStyle("-fx-padding: 5 0; -fx-background-color: transparent;");
                
                setGraphic(cardWrapper);
                setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            }
        });
    }

    private void handleViewUser(UserApp user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/UserDetailsPopup.fxml"));
            Parent root = loader.load();
            
            UserDetailsPopupController controller = loader.getController();
            controller.initData(user);
            
            Stage stage = new Stage();
            stage.setTitle("Détails de l'utilisateur");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            
            Scene scene = new Scene(root);
            scene.setFill(null);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadUserData() {
        new Thread(() -> {
            try {
                List<UserApp> data = userService.getAll();
                Platform.runLater(() -> {
                    userList.setAll(data);
                    userListView.setItems(userList);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> DialogUtils.showError("Erreur", "Impossible de charger les utilisateurs."));
            }
        }).start();
    }

    @FXML
    void showAddModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/UserAddForm.fxml"));
            Parent addView = loader.load();
            mainContent.getChildren().setAll(addView);
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible d'ouvrir le formulaire d'ajout.");
        }
    }

    private void showEditPage(UserApp user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/UserUpdateForm.fxml"));
            Parent updateView = loader.load();

            GUI.UserUpdateController controller = loader.getController();
            controller.initData(user, this);

            mainContent.getChildren().setAll(updateView);

        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible d'ouvrir le formulaire de modification.");
        }
    }

    public void showUserTable() {
        try {
            Stage stage = (Stage) mainContent.getScene().getWindow();
            Scene scene = stage.getScene();

            if (scene.getRoot() instanceof BorderPane mainPane) {
                Parent root = FXMLLoader.load(getClass().getResource("/GUI/AdminUsers.fxml"));
                mainPane.setCenter(root); // change center only
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
            DialogUtils.showError("Erreur", "Navigation impossible vers AdminUsers.");
        }
    }

    private void handleDeleteUser(UserApp user) {
        if (user == null) return;

        String nom = user.getNom() != null ? user.getNom() : "";
        boolean confirm = DialogUtils.showConfirmation(
                "Suppression",
                "Voulez-vous vraiment supprimer l'utilisateur : " + nom + " ?"
        );

        if (confirm) {
            try {
                userService.delete(user.getIdUser());
                userList.remove(user);
                userListView.refresh();
                DialogUtils.showInfo("Succès", "Utilisateur supprimé !");
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtils.showError("Erreur suppression", "❌ Impossible de supprimer : " + e.getMessage());
            }
        }
    }

    private void filterData(String query) {
        if (query == null || query.isEmpty()) {
            userListView.setItems(userList);
        } else {
            String q = query.toLowerCase();
            userListView.setItems(userList.filtered(u ->
                    (u.getNom() != null && u.getNom().toLowerCase().contains(q)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
            ));
        }
    }
}