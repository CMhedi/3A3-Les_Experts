// Controllers/DisponibiliteCategorieController.java (COMPLET)
package Controllers;

import Models.CategoryAvailability;
import Services.AvailabilityService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DisponibiliteCategorieController {

    @FXML private TableView<CategoryAvailability> tableDisponibilite;

    @FXML private TableColumn<CategoryAvailability, String> colCategorie;
    @FXML private TableColumn<CategoryAvailability, Integer> colCapacite;
    @FXML private TableColumn<CategoryAvailability, Integer> colReservees;
    @FXML private TableColumn<CategoryAvailability, Integer> colRestantes;
    @FXML private TableColumn<CategoryAvailability, String> colStatut;

    @FXML private Label lblInfo;

    private final AvailabilityService service = new AvailabilityService();

    private int idActivite;

    public void setIdActivite(int idActivite) {
        this.idActivite = idActivite;
        loadData();
    }

    @FXML
    public void initialize() {

        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capaciteTotale"));
        colReservees.setCellValueFactory(new PropertyValueFactory<>("placesReservees"));
        colRestantes.setCellValueFactory(new PropertyValueFactory<>("placesRestantes"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
    }

    private void loadData() {
        try {

            CategoryAvailability availability =
                    service.fetchAvailabilityForActivite(idActivite); // ✅ BONNE MÉTHODE

            ObservableList<CategoryAvailability> list =
                    FXCollections.observableArrayList(availability);

            tableDisponibilite.setItems(list);

            lblInfo.setText("Disponibilité chargée avec succès");

        } catch (Exception e) {
            e.printStackTrace();
            lblInfo.setText("Erreur chargement disponibilité");
        }
    }
}