package GUI;

import Entities.Reclamation;
import Services.interfaces.ReclamationService;
import enums.StatutReclamation;
import GUI.ReponsePopupController;
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
    @FXML private TableColumn<Reclamation, String> colReponse; // ✅

    @FXML private TextField txtSearch;
    @FXML private Label lblPending;
    @FXML private Label lblDone;

    private ReclamationService rs = new ReclamationService();
    private ObservableList<Reclamation> masterData = FXCollections.observableArrayList();
    @FXML private TextArea areaContenu;
    @FXML public void initialize() {
        // 1. Liaison des colonnes
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colReponse.setCellValueFactory(new PropertyValueFactory<>("reponse")); // ✅

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
                        case REJETEE -> setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-background-radius: 10; -fx-alignment: center;");
                    }
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la Réclamation");
        alert.setHeaderText("Message de : " + r.getUserName());

        // El klem el kol y-ji hna
        TextArea textArea = new TextArea(
                "👤 CLIENT : " + r.getUserName() + "\n" +
                        "📂 TYPE : " + r.getType() + "\n\n" +
                        "📝 MESSAGE :\n" + r.getContenu()
        );

        textArea.setEditable(false);
        textArea.setWrapText(true); // Bech el klem may-okhrojsh 3al jnab
        textArea.setPrefHeight(250);

        alert.getDialogPane().setContent(textArea);

        // N-zidou el CSS mta3ek bech el popup mat-jish sghira w "Windows"
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/gui/style_admin.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("my-custom-dialog");

        alert.showAndWait();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ReponsePopup.fxml"));
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
    @FXML void handleDelete() throws SQLException {
        Reclamation sel = tableReclamations.getSelectionModel().getSelectedItem();
        if (sel != null) {
            rs.supprimer(sel.getIdReclamation());
            refresh();
            areaContenu.clear();
        }
    }


}