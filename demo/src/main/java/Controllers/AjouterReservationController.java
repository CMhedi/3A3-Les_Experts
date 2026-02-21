package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class AjouterReservationController {

    @FXML
    private DatePicker dateReservation;

    @FXML
    private ComboBox<String> statutRes;

    @FXML
    private TextField nbPersonnes;

    @FXML
    private TextField idUser;

    @FXML
    private TextField idActivite;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnAnnuler;

    // Paramètres de connexion à la base de données
    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";

    private final String USER = "root";
    private final String PASSWORD = "";

    @FXML
    public void initialize() {
        statutRes.getItems().addAll(
                "Confirmée",
                "En_attente",
                "Annulée"
        );
    }

    @FXML
    private void ajouterReservation() {
        LocalDate date = dateReservation.getValue();
        String statut = statutRes.getValue();
        String nb = nbPersonnes.getText();
        String userId = idUser.getText();
        String activiteId = idActivite.getText();

        if (date == null || statut == null || nb.isEmpty() || userId.isEmpty() || activiteId.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs !", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            int nbPers = Integer.parseInt(nb);
            int idUserInt = Integer.parseInt(userId);
            int idActiviteInt = Integer.parseInt(activiteId);

            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            String sql = "INSERT INTO reservation_activite (date_reservation, statut_res, nb_personnes, id_user, id_activite) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);


            pst.setDate(1, Date.valueOf(date)); // conversion LocalDate -> java.sql.Date
            pst.setString(2, statut);
            pst.setInt(3, nbPers);
            pst.setInt(4, idUserInt);
            pst.setInt(5, idActiviteInt);

            int result = pst.executeUpdate();

            if (result > 0) {

                ResultSet rs = pst.getGeneratedKeys();
                int generatedId = 0;

                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }

                // 🔹 Charger l'interface reçu
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/recuReservation.fxml"));
                Parent root = loader.load();

                RecuReservationController controller = loader.getController();
                controller.setData(
                        generatedId,
                        date.toString(),
                        statut,
                        nbPers,
                        idUserInt,
                        idActiviteInt
                );

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Reçu de Réservation");
                stage.show();

                annuler();
            }


            pst.close();
            conn.close();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Veuillez entrer des nombres valides pour le nombre de personnes, l'ID utilisateur et l'ID activité !", ButtonType.OK);
            alert.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de l'ajout dans la base : " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void annuler() {
        dateReservation.setValue(null);
        statutRes.setValue(null);
        nbPersonnes.clear();
        idUser.clear();
        idActivite.clear();
    }
    @FXML
    public void btnswitch(ActionEvent event) {
        switchScene(event, "/GUI/reservation.fxml");
    }
    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de retour vers : " + fxmlPath);
            e.printStackTrace();
        }
    }
}

