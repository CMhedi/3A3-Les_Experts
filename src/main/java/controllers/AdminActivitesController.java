package controllers;

import Models.Activite;
import Utiles.MyDB2;
import javafx.collections.transformation.FilteredList;
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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class AdminActivitesController {

    @FXML private TableView<Activite> tableActivites;
    @FXML private DatePicker dateReservation;
    @FXML private TextField txtSearch;
    @FXML private Label lblCount;
    @FXML private Label lblPageInfo;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private TableColumn<Activite, Integer> colId;
    @FXML private TableColumn<Activite, String> colNom;
    @FXML private TableColumn<Activite, String> colType;
    @FXML private TableColumn<Activite, String> colCategorie;
    @FXML private TableColumn<Activite, String> colNiveau;
    @FXML private TableColumn<Activite, String> colStatut;
    @FXML private TableColumn<Activite, Double> colPrix;
    @FXML private TableColumn<Activite, String> colImage;
    @FXML private TableColumn<Activite, Date> colDate;


    private static final int PAGE_SIZE = 8;
    private final ObservableList<Activite> activites = FXCollections.observableArrayList();
    private final List<Activite> filteredActivites = new ArrayList<>();
    private int currentPageIndex = 0;

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
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldValue, newValue) -> applyFilterAndPagination());
        }

        loadActivites();
    }


    //   telech de activ  d apres mon bd
    private void loadActivites() {
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement("SELECT * FROM activite");
             ResultSet rs = ps.executeQuery()) {

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
                        rs.getString("image_url"),
                        rs.getDate("date_reservation")
                ));
            }

            applyFilterAndPagination();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors du chargement des activités !");
        }
    }

    private void applyFilterAndPagination() {
        String search = txtSearch == null ? "" : txtSearch.getText().trim().toLowerCase();

        filteredActivites.clear();
        for (Activite activite : activites) {
            if (matchesSearch(activite, search)) {
                filteredActivites.add(activite);
            }
        }

        if (filteredActivites.isEmpty()) {
            currentPageIndex = 0;
        } else {
            int maxPageIndex = Math.max(0, (filteredActivites.size() - 1) / PAGE_SIZE);
            currentPageIndex = Math.min(currentPageIndex, maxPageIndex);
        }

        refreshPage();
    }

    private boolean matchesSearch(Activite activite, String search) {
        if (search == null || search.isEmpty()) {
            return true;
        }

        return contains(activite.getNom(), search)
                || contains(activite.getTypeActivite(), search)
                || contains(activite.getCategorieAct(), search)
                || contains(activite.getNiveauAct(), search)
                || contains(activite.getStatut(), search)
                || contains(activite.getImageUrl(), search)
                || String.valueOf(activite.getIdActivite()).contains(search);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private void refreshPage() {
        int fromIndex = currentPageIndex * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredActivites.size());

        if (fromIndex > toIndex) {
            fromIndex = 0;
            toIndex = Math.min(PAGE_SIZE, filteredActivites.size());
            currentPageIndex = 0;
        }

        List<Activite> pageItems = filteredActivites.subList(fromIndex, toIndex);
        tableActivites.setItems(FXCollections.observableArrayList(pageItems));
        tableActivites.refresh();

        if (lblCount != null) {
            lblCount.setText(filteredActivites.size() + " activité(s)");
        }
        if (lblPageInfo != null) {
            int pageCount = Math.max(1, (filteredActivites.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            lblPageInfo.setText("Page " + (filteredActivites.isEmpty() ? 0 : currentPageIndex + 1) + " / " + pageCount);
        }
        if (btnPrevPage != null) {
            btnPrevPage.setDisable(currentPageIndex <= 0);
        }
        if (btnNextPage != null) {
            btnNextPage.setDisable((currentPageIndex + 1) * PAGE_SIZE >= filteredActivites.size());
        }
    }

    @FXML
    private void previousPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            refreshPage();
        }
    }

    @FXML
    private void nextPage() {
        int pageCount = Math.max(1, (filteredActivites.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (currentPageIndex < pageCount - 1) {
            currentPageIndex++;
            refreshPage();
        }
    }

    // modif
    @FXML
    private void modifierActivite() {
        Activite selected = tableActivites.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner une activité !");
            return;
        }

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/EditActivite.fxml"));
            Parent root = loader.load();

            // controller du popup(modifier)
            EditActiviteController controller = loader.getController();
            controller.setActivite(selected);

            Stage popup = new Stage();
            popup.setTitle("Modifier Activité");
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setResizable(true);
            popup.setScene(new Scene(root));
            popup.showAndWait();

            //  si user a clique enregistre reload depuis DB
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

    //  Supprimer
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
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement("DELETE FROM activite WHERE id_activite = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();

            showAlert("Activité supprimée !");
            loadActivites();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors de la suppression !");
        }
    }


    //  Helper
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }


    @FXML
    private void goToUserSeances(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/MainLayout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void openstat() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/StatsActivite.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tableActivites.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Statistique - Admin");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Impossible d'ouvrir la page statistique !");
        }
    }



    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}

