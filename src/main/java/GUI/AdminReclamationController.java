package GUI;

import Entities.Reclamation;
import Services.ReclamationService;
import enums.StatutReclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AdminReclamationController {
    @FXML private TableView<Reclamation> tableReclamations;
    @FXML private TableColumn<Reclamation, Integer> colUser;
    @FXML private TableColumn<Reclamation, String> colType;
    @FXML private TableColumn<Reclamation, StatutReclamation> colStatut;
    @FXML private TableColumn<Reclamation, Void> colActions; // ✅ Actions column

    @FXML private TextField txtSearch;
    @FXML private Label lblPending;
    @FXML private Label lblDone;

    @FXML private Label lblProcessing;
    private ReclamationService rs = new ReclamationService();
    private ObservableList<Reclamation> masterData = FXCollections.observableArrayList();
    @FXML private TextArea areaContenu;
    @FXML public void initialize() {
        // 1. Liaison des colonnes
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        // Removed colReponse

        // 2. Custom Cell Factory for Colors (Statut)
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(StatutReclamation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    switch (item) {
                        case EN_ATTENTE -> setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-background-radius: 10; -fx-alignment: center;");
                        case TRAITEE -> setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-background-radius: 10; -fx-alignment: center;");
                        case EN_COURS -> setStyle("-fx-background-color: #E0F2FE; -fx-text-fill: #0369A1; -fx-background-radius: 10; -fx-alignment: center;");
                        case REJETEE -> setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-background-radius: 10; -fx-alignment: center;");
                    }
                }
            }
        });

        // 3. Custom Cell Factory for Actions (Détails & Trash)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button("👁 Détails");
            private final Button btnDelete = new Button("🗑");
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(10, btnDetails, btnDelete);

            {
                btnDetails.setStyle("-fx-background-color: #143D30; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 15;");
                btnDelete.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 15;");
                pane.setAlignment(javafx.geometry.Pos.CENTER);

                btnDetails.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    tableReclamations.getSelectionModel().select(r);
                    showDetailsPopup(r);
                });

                btnDelete.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    tableReclamations.getSelectionModel().select(r);
                    try { handleDelete(); } catch (SQLException e) { e.printStackTrace(); }
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
        });

        tableReclamations.setRowFactory(tv -> {
            TableRow<Reclamation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {

                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Reclamation rowData = row.getItem();
                    showDetailsPopup(rowData);
                }
            });
            return row;
        });


        // 4. Activation de la recherche
        txtSearch.textProperty().addListener((obs, old, val) -> filterData(val));

        refresh();
    }
    private void showDetailsPopup(Reclamation r) {
        if (r.getStatut() == StatutReclamation.EN_ATTENTE) {
            try {
                rs.modifierStatut(r.getIdReclamation(), StatutReclamation.EN_COURS);
                refresh();
            } catch (SQLException e) { e.printStackTrace(); }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/DetailsReclamation.fxml"));
            Parent root = loader.load();

            // N-3aytou lel Controller jdid bech n-7ottou el data
            DetailsController controller = loader.getController();
            controller.setData(r.getUserName(), r.getType(), r.getContenu());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT); // Khater el FXML fih background-radius

            Scene scene = new Scene(root);
            scene.setFill(null); // Lel hwayes el bidha (transparent)
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void updateStatutLocal(StatutReclamation s) throws SQLException {
        Reclamation sel = tableReclamations.getSelectionModel().getSelectedItem();
        if (sel != null) {
            rs.modifierStatut(sel.getIdReclamation(), s);
            refresh();
        } else {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une ligne").show();
        }
    }


    private void cleanOldRejections() {
        try {
            List<Reclamation> all = rs.afficher();
            for (Reclamation r : all) {
                if (r.getStatut() == StatutReclamation.REJETEE && r.getDateCreation() != null) {
                    if (r.getDateCreation().isBefore(LocalDateTime.now().minusDays(15))) {
                        rs.supprimer(r.getIdReclamation());
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refresh() {
        try {
            cleanOldRejections(); // ✅ On nettoie avant de charger
            List<Reclamation> list = rs.afficher();
            masterData = FXCollections.observableArrayList(list);
            tableReclamations.setItems(masterData);
            updateStats();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateStats() {
        long pending = masterData.stream().filter(r -> r.getStatut() == StatutReclamation.EN_ATTENTE).count();
        long done = masterData.stream().filter(r -> r.getStatut() == StatutReclamation.TRAITEE).count();
        long processing = masterData.stream().filter(r -> r.getStatut() == StatutReclamation.EN_COURS).count();
        lblProcessing.setText(String.valueOf(processing)); // Nsit ma zedtech el variable hadi
        lblPending.setText(String.valueOf(pending));
        lblDone.setText(String.valueOf(done));
    }

    private void filterData(String query) {
        if (query == null || query.isEmpty()) {
            tableReclamations.setItems(masterData);
        } else {
            tableReclamations.setItems(FXCollections.observableArrayList(
                    masterData.stream()
                            .filter(r -> r.getContenu().toLowerCase().contains(query.toLowerCase()) ||
                                    r.getType().toLowerCase().contains(query.toLowerCase()))
                            .toList()
            ));
        }
    }


    @FXML
    void handleOpenResponse() {
        Reclamation selected = tableReclamations.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une réclamation").show();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/ReponsePopup.fxml"));
            Parent root = loader.load();

            ReponsePopupController popupCtrl = loader.getController();

            popupCtrl.setData(selected, this::refresh);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML void handleRejeter() throws SQLException { updateStatutLocal(StatutReclamation.REJETEE); }
    @FXML
    void handleTraitee() throws SQLException {
        updateStatutLocal(StatutReclamation.TRAITEE);
    }
    @FXML
    void handleEnCours() throws SQLException {
        updateStatutLocal(StatutReclamation.EN_COURS);
        // Thabbet elli EN_COURS mawjouda fel Enum StatutReclamation mte3ek
    }
    @FXML void handleDelete() throws SQLException {
        Reclamation sel = tableReclamations.getSelectionModel().getSelectedItem();
        if (sel != null) {
            rs.supprimer(sel.getIdReclamation());
            refresh();
            areaContenu.clear();
        }
    }


}