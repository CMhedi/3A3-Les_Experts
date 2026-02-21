package Controllers;

import Models.Activite;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import utils.DataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminActivitesController {

    @FXML private TableView<Activite> tableActivites;

    @FXML private TableColumn<Activite, Integer> colId;
    @FXML private TableColumn<Activite, String> colNom;
    @FXML private TableColumn<Activite, String> colType;
    @FXML private TableColumn<Activite, String> colCategorie;
    @FXML private TableColumn<Activite, String> colNiveau;
    @FXML private TableColumn<Activite, String> colStatut;
    @FXML private TableColumn<Activite, Double> colPrix;
    @FXML private TableColumn<Activite, String> colImage;

    private ObservableList<Activite> activites = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup columns with correct property names (match getters in Activite.java)
        colId.setCellValueFactory(new PropertyValueFactory<>("idActivite"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeActivite"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorieAct"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveauAct"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));

        loadActivites(); // load from database
    }

    // ================= Load Activities from DB =================
    private void loadActivites() {
        try {
            Connection cnx = DataBase.getInstance().getConx();
            String sql = "SELECT * FROM activite";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            activites.clear();

            while (rs.next()) {
                activites.add(new Activite(
                        rs.getInt("id_activite"),
                        rs.getString("nom"),
                        rs.getString("type_activite"),
                        rs.getString("categorie_act"),
                        rs.getString("niveau_act"),
                        rs.getDouble("prix"),
                        rs.getString("statut"),
                        rs.getString("image_url")
                ));
            }

            tableActivites.setItems(activites);
            tableActivites.refresh(); // ensure TableView updates

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors du chargement des activités !");
        }
    }

    // ================= Modifier Type d'une Activité =================
    @FXML
    private void modifierActivite() {
        Activite selected = tableActivites.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner une activité !");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getTypeActivite());
        dialog.setTitle("Modifier Type");
        dialog.setHeaderText("Modifier le type de l'activité");
        dialog.setContentText("Nouveau Type :");

        dialog.showAndWait().ifPresent(newType -> {
            updateActiviteType(selected.getIdActivite(), newType);
        });
    }

    // ================= Update Database =================
    private void updateActiviteType(int id, String newType) {
        try {
            Connection cnx = DataBase.getInstance().getConx();
            String sql = "UPDATE activite SET type_activite = ? WHERE id_activite = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, newType);
            ps.setInt(2, id);
            ps.executeUpdate();

            showAlert("Activité modifiée avec succès !");
            loadActivites();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors de la modification !");
        }
    }

    // ================= Supprimer Activité =================
    @FXML
    private void supprimerActivite() {
        Activite selected = tableActivites.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner une activité !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Voulez-vous vraiment supprimer cette activité ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                deleteActivite(selected.getIdActivite());
            }
        });
    }

    private void deleteActivite(int id) {
        try {
            Connection cnx = DataBase.getInstance().getConx();
            String sql = "DELETE FROM activite WHERE id_activite = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();

            showAlert("Activité supprimée !");
            loadActivites();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors de la suppression !");
        }
    }

    // ================= Helper =================
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
