package gui;

import Entities.UserApp;
import Services.interfaces.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;

public class AdminUsersController {

    @FXML private ListView<UserApp> userListView; // التغيير هنا
    @FXML private VBox mainContent;
    @FXML private TextField txtSearch;

    private UserService userService = new UserService();
    private ObservableList<UserApp> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupUserListView();
        loadUserData();
        txtSearch.textProperty().addListener((obs, old, newVal) -> filterData(newVal));
    }

    private void setupUserListView() {
        userListView.setCellFactory(param -> new ListCell<UserApp>() {
            @Override
            protected void updateItem(UserApp user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    // 1. حاوية الـ Card
                    HBox card = new HBox(20);
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");

                    // 2. Avatar (أول حرف من الاسم)
                    Label avatar = new Label(user.getNom().substring(0, 1).toUpperCase());
                    avatar.setStyle("-fx-background-color: #143D30; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-min-width: 45; -fx-min-height: 45; -fx-background-radius: 25; -fx-alignment: center;");

                    // 3. المعلومات الشخصية
                    VBox info = new VBox(5);
                    Label name = new Label(user.getNom() + " " + user.getPrenom());
                    name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
                    Label email = new Label(user.getEmail());
                    email.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
                    info.getChildren().addAll(name, email);

                    // 4. الـ Role Badge (ملون حسب الدور)
                    Label roleBadge = new Label(user.getRole().toString());
                    String badgeStyle = "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
                    if (user.getRole().toString().equals("ADMIN")) badgeStyle += "-fx-background-color: #1e293b;";
                    else if (user.getRole().toString().equals("COACH")) badgeStyle += "-fx-background-color: #143D30;";
                    else badgeStyle += "-fx-background-color: #94a3b8;";
                    roleBadge.setStyle(badgeStyle);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // 5. أزرار التحكم
                    HBox actions = new HBox(10);
                    Button btnEdit = new Button("✏️");
                    btnEdit.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-cursor: hand;");
                    btnEdit.setOnAction(e -> showEditPage(user));

                    Button btnDel = new Button("🗑️");
                    btnDel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand;");
                    btnDel.setOnAction(e -> handleDeleteUser(user));

                    actions.getChildren().addAll(btnEdit, btnDel);

                    card.getChildren().addAll(avatar, info, roleBadge, spacer, actions);
                    setGraphic(card);
                }
            }
        });
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
            }
        }).start();
    }


    @FXML
    void showAddModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/UserAddForm.fxml"));
            Parent addView = loader.load();

            // Baddel el blasa el bidha barka bel Form mta3 el Ajout
            mainContent.getChildren().setAll(addView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showEditPage(UserApp user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/UserUpdateForm.fxml"));

            // 1. Load el view el loula (hedhi elli t-khali el @FXML y-welliw mouch null)
            Parent updateView = loader.load();

            // 2. Tawa nadi el controller
            UserUpdateController controller = loader.getController();

            // 3. Tawa nab3ath el data
            controller.initData(user, this);

            mainContent.getChildren().setAll(updateView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showUserTable() {
        try {
            // Njibou el Stage root
            Stage stage = (Stage) mainContent.getScene().getWindow();
            Scene scene = stage.getScene();

            // Thabbet elli el root howa BorderPane (elli fih el fx:include)
            if (scene.getRoot() instanceof BorderPane mainPane) {
                Parent root = FXMLLoader.load(getClass().getResource("/gui/AdminUsers.fxml"));
                mainPane.setCenter(root); // N-badlou ken el center, ma nmes-sh el Sidebar
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
        }
    }
    private void handleDeleteUser(UserApp user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + user.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.delete(user.getIdUser());
                    userList.remove(user);
                    userListView.refresh();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Erreur suppression: " + e.getMessage()).show();
                }
            }
        });
    }

    private void filterData(String query) {
        if (query == null || query.isEmpty()) {
            userListView.setItems(userList);
        } else {
            userListView.setItems(userList.filtered(u ->
                    u.getNom().toLowerCase().contains(query.toLowerCase()) ||
                            u.getEmail().toLowerCase().contains(query.toLowerCase())
            ));
        }
    }



}
