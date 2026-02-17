package gui;

import Entities.UserApp;
import Services.interfaces.UserService;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;

public class AdminUsersController {

    @FXML private TableView<UserApp> userTable;
    @FXML private VBox mainContent; // Injecti el content container bech nbedlou el view
    @FXML private TableColumn<UserApp, String> colNom, colPrenom, colEmail, colRole;
    @FXML private TableColumn<UserApp, Void> colActions;
    @FXML private TextField txtSearch;

    private UserService userService = new UserService();
    private ObservableList<UserApp> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Setup Columns
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // 2. Setup Actions Column
        addActionsButtons();

        // 3. Charger les données
        loadUserData();

        // 4. Barre de recherche dynamique
        txtSearch.textProperty().addListener((obs, old, newVal) -> filterData(newVal));
    }

    public void loadUserData() {
        try {
            userList.setAll(userService.getAll());
            userTable.setItems(userList);
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement: " + e.getMessage());
        }
    }

    private void addActionsButtons() {
        Callback<TableColumn<UserApp, Void>, TableCell<UserApp, Void>> cellFactory = param -> {
            return new TableCell<UserApp, Void>() {
                private final Button btnEdit = new Button("📝");
                private final Button btnDel = new Button("🗑️");
                private final HBox pane = new HBox(btnEdit, btnDel);

                {
                    pane.setSpacing(10);
                    pane.setAlignment(Pos.CENTER);
                    btnEdit.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                    btnEdit.setOnAction(event -> {
                        UserApp user = getTableView().getItems().get(getIndex());
                        showEditPage(user); // Nadiw el méthode mta3 el view switching
                    });

                    btnDel.setOnAction(event -> {
                        UserApp user = getTableView().getItems().get(getIndex());
                        handleDeleteUser(user);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(pane);
                    }
                }
            };
        };
        colActions.setCellFactory(cellFactory);
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
                    userTable.refresh();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Erreur suppression: " + e.getMessage()).show();
                }
            }
        });
    }

    private void filterData(String query) {
        if (query == null || query.isEmpty()) {
            userTable.setItems(userList);
        } else {
            ObservableList<UserApp> filtered = userList.filtered(u ->
                    u.getNom().toLowerCase().contains(query.toLowerCase()) ||
                            u.getEmail().toLowerCase().contains(query.toLowerCase())
            );
            userTable.setItems(filtered);
        }
        userTable.refresh();
    }



}
