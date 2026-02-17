package gui;

import Entities.Reclamation;
import Entities.Session;
import Services.interfaces.ReclamationService;
import enums.StatutReclamation;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class UserReclamationController {
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea txtContenu;
    @FXML private TableView<Reclamation> tableMyRecs;
    @FXML private TableColumn<Reclamation, String> colMyType, colMyContenu, colMyReponse;
    @FXML private TableColumn<Reclamation, StatutReclamation> colMyStatut;
    @FXML private Button btnModifier, btnEnvoyer;

    private ReclamationService rs = new ReclamationService();
    private Reclamation selectedRec = null;
    private int currentUserId = Session.getConnectedUser().getIdUser();

    @FXML
    public void initialize() {

        if (comboType != null) {
            comboType.setItems(FXCollections.observableArrayList("TECHNIQUE", "SERVICE", "PAIEMENT", "AUTRE"));
        }

        // --- Partia mta3 el TABLE ---
        if (tableMyRecs != null) {
            colMyType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colMyContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
            colMyStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
            colMyReponse.setCellValueFactory(new PropertyValueFactory<>("reponse"));

            // CellFactory lel loun mta3 el Statut
            colMyStatut.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(StatutReclamation item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item.toString());
                        switch (item) {
                            case EN_ATTENTE -> setStyle("-fx-text-fill: #F37021; -fx-font-weight: bold;");
                            case TRAITEE -> setStyle("-fx-text-fill: #143D30; -fx-font-weight: bold;");
                            case REJETEE -> setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
                        }
                    }
                }
            });

            // Listener bech ki t-selecti 7aja mel Table temchi lel Form (kenek fi page okhra)
            tableMyRecs.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) {
                    selectedRec = newVal;
                    // Ken thamma TextArea (ma3neha rna fil Form), n-3abiwha
                    if (txtContenu != null) {
                        txtContenu.setText(newVal.getContenu());
                        comboType.setValue(newVal.getType());
                        btnModifier.setDisable(newVal.getStatut() != StatutReclamation.EN_ATTENTE);
                        btnEnvoyer.setDisable(true);
                    }
                }
            });
            loadData();
        }
    }

    private void loadData() {
        try {
            List<Reclamation> data = rs.afficherParUser(currentUserId);
            if (tableMyRecs != null) {
                tableMyRecs.setItems(FXCollections.observableArrayList(data));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            BorderPane mainPane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");
            if (mainPane != null) {
                Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
                mainPane.setCenter(page);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void switchToList(ActionEvent event) {
        try {
            BorderPane mainPane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");
            if (mainPane != null) {
                Parent page = FXMLLoader.load(getClass().getResource("/gui/ListReclamation.fxml"));
                mainPane.setCenter(page);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void switchToForm(ActionEvent event) {
        try {
            BorderPane mainPane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");
            if (mainPane != null) {
                Parent page = FXMLLoader.load(getClass().getResource("/gui/AddReclamation.fxml"));
                mainPane.setCenter(page);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    void handleEnvoyer(ActionEvent event) throws SQLException {
        Reclamation r = new Reclamation();
        r.setType(comboType.getValue());
        r.setContenu(txtContenu.getText());
        r.setStatut(StatutReclamation.EN_ATTENTE);
        r.setIdUser(currentUserId);

        rs.ajouter(r);


        switchToList(event);
    }

    @FXML
    void handleUpdate(ActionEvent event) {
        Reclamation sel = tableMyRecs.getSelectionModel().getSelectedItem();

        // 1. Thabbet elli famma 7aja selectionnée
        if (sel == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une réclamation !");
            alert.show();
            return;
        }

        // 2. Sna3 el Dialog kima tfahemna
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modification");
        dialog.setHeaderText("Mettre à jour votre demande");

        VBox content = new VBox(15);
        content.setStyle("-fx-background-color: white; -fx-padding: 20;");

        ComboBox<String> editType = new ComboBox<>(FXCollections.observableArrayList("TECHNIQUE", "SERVICE", "PAIEMENT", "AUTRE"));
        editType.setValue(sel.getType());
        editType.setMaxWidth(Double.MAX_VALUE);

        TextArea editContenu = new TextArea(sel.getContenu());
        editContenu.setWrapText(true);
        editContenu.setPrefHeight(150);

        // Label erreur sghir (may-ben ken ki tebda fergha)
        Label lblError = new Label("La description ne peut pas être vide !");
        lblError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        lblError.setVisible(false);

        content.getChildren().addAll(
                new Label("Type de problème :"), editType,
                new Label("Description :"), editContenu,
                lblError
        );

        dialog.getDialogPane().setContent(content);

        // 3. Zid el Boutonnet
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 4. EL SÉCURITÉ HONI: Bloqui el bouton Enregistrer
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);


        editContenu.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean isInvalid = newVal.trim().isEmpty();
            saveButton.setDisable(isInvalid);
            lblError.setVisible(isInvalid);
        });


        saveButton.setDisable(sel.getContenu().trim().isEmpty());


        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/gui/style_reclamation.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("my-custom-dialog");


        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                try {
                    sel.setType(editType.getValue());
                    sel.setContenu(editContenu.getText());
                    rs.modifier(sel);
                    loadData(); // Refresh el Tableau
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    @FXML void handleDelete() throws SQLException {
        Reclamation sel = tableMyRecs.getSelectionModel().getSelectedItem();
        if (sel != null) {
            rs.supprimer(sel.getIdReclamation());
            loadData();
        }
    }

    @FXML void clearFields() {
        if (comboType != null) {
            comboType.setValue(null);
            txtContenu.clear();
            btnModifier.setDisable(true);
            btnEnvoyer.setDisable(false);
        }
        selectedRec = null;
    }
}