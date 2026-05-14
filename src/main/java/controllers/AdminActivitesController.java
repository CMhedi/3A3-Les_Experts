package controllers;

import GUI.utils.ActiviteQuickAdd;
import Models.Activite;
import Services.interfaces.ActiviteService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class AdminActivitesController {

    @FXML private Label lblTotalAct;
    @FXML private Label lblActiveAct;
    @FXML private Label lblAvgPrice;

    @FXML private TableView<Activite> tableActivites;
    @FXML private TableColumn<Activite, Integer> colId;
    @FXML private TableColumn<Activite, String> colNom;
    @FXML private TableColumn<Activite, String> colType;
    @FXML private TableColumn<Activite, String> colCategorie;
    @FXML private TableColumn<Activite, String> colNiveau;
    @FXML private TableColumn<Activite, String> colStatut;
    @FXML private TableColumn<Activite, Double> colPrix;
    @FXML private TableColumn<Activite, String> colImage;
    @FXML private TableColumn<Activite, Integer> colPack;

    private final ActiviteService activiteService = new ActiviteService();
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
        colPack.setCellValueFactory(new PropertyValueFactory<>("idPack"));

        tableActivites.setRowFactory(tv -> {
            TableRow<Activite> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showDetailsForActivite(row.getItem());
                }
            });
            return row;
        });

        loadActivites();
    }

    private void loadActivites() {
        try {
            List<Activite> list = activiteService.getAll();
            activites.setAll(list != null ? list : List.of());
            tableActivites.setItems(activites);
            tableActivites.refresh();
            updateKpis(list);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors du chargement des activités : " + e.getMessage());
        }
    }

    private void updateKpis(List<Activite> list) {
        if (lblTotalAct == null) {
            return;
        }
        int total = list == null ? 0 : list.size();
        long actives = list == null ? 0 : list.stream()
                .filter(a -> a.getStatut() != null && a.getStatut().toUpperCase().contains("DISPON"))
                .count();
        double avg = list == null || list.isEmpty() ? 0
                : list.stream().mapToDouble(Activite::getPrix).average().orElse(0);
        lblTotalAct.setText(String.valueOf(total));
        lblActiveAct.setText(String.valueOf(actives));
        lblAvgPrice.setText(String.format("%.2f TND", avg));
    }

    @FXML
    private void ajouterActivite() {
        ActiviteQuickAdd.openDialog(tableActivites.getScene().getWindow(), this::loadActivites);
    }

    /**
     * Lecture (R du CRUD) : détails frais depuis la base via {@link ActiviteService#getById(int)}.
     */
    @FXML
    private void afficherDetailsActivite() {
        Activite selected = tableActivites.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélectionnez une activité dans la liste, ou double-cliquez sur une ligne.");
            return;
        }
        showDetailsForActivite(selected);
    }

    private void showDetailsForActivite(Activite ref) {
        if (ref == null) {
            return;
        }
        try {
            Activite a = activiteService.getById(ref.getIdActivite());
            if (a == null) {
                showAlert("Activité introuvable en base.");
                return;
            }
            String text = String.format(
                    "ID : %d%nNom : %s%nType : %s%nCatégorie : %s%nNiveau : %s%nStatut : %s%nPrix : %.2f TND%nImage URL : %s%nPack (id) : %s%nLatitude : %s%nLongitude : %s",
                    a.getIdActivite(),
                    nvl(a.getNom()),
                    nvl(a.getTypeActivite()),
                    nvl(a.getCategorieAct()),
                    nvl(a.getNiveauAct()),
                    nvl(a.getStatut()),
                    a.getPrix(),
                    nvl(a.getImageUrl()),
                    a.getIdPack() > 0 ? String.valueOf(a.getIdPack()) : "—",
                    a.getLatitude() != null ? String.valueOf(a.getLatitude()) : "—",
                    a.getLongitude() != null ? String.valueOf(a.getLongitude()) : "—"
            );

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détails activité");
            alert.setHeaderText("Affichage (lecture seule — données base)");

            TextArea ta = new TextArea(text);
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefRowCount(14);
            ta.setMaxWidth(Double.MAX_VALUE);
            VBox box = new VBox(8, ta);
            VBox.setVgrow(ta, Priority.ALWAYS);
            alert.getDialogPane().setContent(box);
            alert.setResizable(true);
            alert.getDialogPane().setPrefWidth(520);
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lecture activité : " + e.getMessage());
        }
    }

    private static String nvl(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    @FXML
    private void actualiserListe() {
        loadActivites();
    }

    @FXML
    private void modifierActivite() {
        Activite selected = tableActivites.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner une activité !");
            return;
        }

        try {
            Activite fromDb = activiteService.getById(selected.getIdActivite());
            if (fromDb == null) {
                showAlert("Activité introuvable en base.");
                loadActivites();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/EditActivite.fxml"));
            Parent root = loader.load();

            EditActiviteController controller = loader.getController();
            controller.setActivite(fromDb);

            Stage popup = new Stage();
            popup.setTitle("Modifier Activité");
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setResizable(true);
            popup.setScene(new Scene(root));
            popup.showAndWait();

            if (controller.isSaved()) {
                loadActivites();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Impossible d'ouvrir la fenêtre de modification !");
        }
    }

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
                try {
                    activiteService.delete(selected.getIdActivite());
                    showAlert("Activité supprimée !");
                    loadActivites();
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Erreur lors de la suppression !");
                }
            }
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
}
