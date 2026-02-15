package Controllers;

import Models.Activite;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.DataBase;

import java.io.IOException;
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

    private final ObservableList<Activite> activites = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idActivite"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeActivite"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorieAct"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveauAct"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));

        loadActivites();
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
            tableActivites.refresh();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors du chargement des activités !");
        }
    }

    // ================= Modifier Activité (POPUP) =================
    @FXML
    private void modifierActivite() {
        Activite selected = tableActivites.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner une activité !");
            return;
        }

        try {
            // ⚠️ Mets le bon chemin selon ton dossier resources
            // Exemple: "/Views/EditActivite.fxml" si ton fxml est dans resources/Views/
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/EditActivite.fxml"));
            Parent root = loader.load();

            // Controller du popup
            EditActiviteController controller = loader.getController();
            controller.setActivite(selected);  // remplir les champs

            Stage popup = new Stage();
            popup.setTitle("Modifier Activité");
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setResizable(false);
            popup.setScene(new Scene(root));
            popup.showAndWait();

            // Si l'utilisateur a cliqué ENREGISTRER => reload depuis DB
            if (controller.isSaved()) {
                loadActivites();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Impossible d'ouvrir la fenêtre de modification !");
        }
    }
    @FXML
    private void openReservations() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/reservation.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tableActivites.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Réservations - Admin");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Impossible d'ouvrir la page Réservations !");
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
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }


    @FXML
    private void goToUserSeances(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/UserSeances.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}

