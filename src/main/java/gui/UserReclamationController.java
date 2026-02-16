package gui;

import Entities.Reclamation;
import Entities.Session;
import Services.interfaces.ReclamationService;
import enums.StatutReclamation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class UserReclamationController {
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea txtContenu;
    @FXML private TableView<Reclamation> tableMyRecs;
    @FXML private TableColumn<Reclamation, String> colMyType, colMyContenu;
    @FXML private TableColumn<Reclamation, StatutReclamation> colMyStatut;
    @FXML private Button btnModifier, btnEnvoyer;
    @FXML private TableColumn<Reclamation, String> colMyReponse;
    private ReclamationService rs = new ReclamationService();
    private Reclamation selectedRec = null;
    private int currentUserId = Session.getConnectedUser().getIdUser();
    @FXML public void initialize() {
        // 1. Liaison des types
        comboType.setItems(FXCollections.observableArrayList("TECHNIQUE", "SERVICE", "PAIEMENT", "AUTRE"));

        // 2. Mapping des colonnes
        colMyType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colMyContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colMyStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        // colMyReponse.setCellValueFactory(new PropertyValueFactory<>("reponse")); // Zidha ken zedt column reponse fil FXML

        // 3. Styling des cellules (Colors)
        colMyStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(StatutReclamation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // Style m-alwan حسب el statut
                    switch (item) {
                        case EN_ATTENTE -> setStyle("-fx-text-fill: #F37021; -fx-font-weight: bold;");
                        case TRAITEE -> setStyle("-fx-text-fill: #143D30; -fx-font-weight: bold;");
                        case REJETEE -> setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // 4. Unique Listener (Un seul suffit !)
        tableMyRecs.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedRec = newVal;
                comboType.setValue(newVal.getType());

                // Affichage Contenu + Réponse si elle existe
                String affichage = newVal.getContenu();
                if (newVal.getReponse() != null && !newVal.getReponse().isEmpty()) {
                    affichage += "\n\n--------------------------\n✅ RÉPONSE ADMIN:\n" + newVal.getReponse();
                }
                txtContenu.setText(affichage);

                // Control des boutons
                // On ne peut modifier que si c'est EN_ATTENTE
                boolean editable = newVal.getStatut() == StatutReclamation.EN_ATTENTE;
                btnModifier.setDisable(!editable);
                btnEnvoyer.setDisable(true); // Disable envoyer car on est en mode sélection
            }
        });
        colMyReponse.setCellValueFactory(new PropertyValueFactory<>("reponse"));
        loadData();
    }

    private void loadData() {
        if (Session.getConnectedUser() == null) {
            System.out.println("Erreur: Aucun utilisateur connecté !");
            return;
        }

        try {
            int id = Session.getConnectedUser().getIdUser(); // Njibou el ID s7i7
            List<Reclamation> data = rs.afficherParUser(id);
            tableMyRecs.setItems(FXCollections.observableArrayList(data));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @FXML void handleEnvoyer() throws SQLException {
        Reclamation r = new Reclamation();
        r.setType(comboType.getValue());
        r.setContenu(txtContenu.getText());
        r.setStatut(StatutReclamation.EN_ATTENTE);
        r.setIdUser(currentUserId);
        rs.ajouter(r);
        loadData(); clearFields();
    }

    @FXML void handleUpdate() throws SQLException {
        if (selectedRec != null) {
            selectedRec.setType(comboType.getValue());
            selectedRec.setContenu(txtContenu.getText());
            rs.modifier(selectedRec);
            loadData(); clearFields();
        }
    }

    @FXML void handleDelete() throws SQLException {
        Reclamation sel = tableMyRecs.getSelectionModel().getSelectedItem();
        if (sel != null) {
            rs.supprimer(sel.getIdReclamation());
            loadData(); clearFields();
        }
    }

    @FXML void clearFields() {
        comboType.setValue(null);
        txtContenu.clear();
        tableMyRecs.getSelectionModel().clearSelection(); // ✅ Na7i el sélection mel tableau
        btnModifier.setDisable(true);
        btnEnvoyer.setDisable(false);
        selectedRec = null;
    }
}