package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class AjouterActiviteController {

    @FXML
    private TextField nomField;
    @FXML
    private ComboBox<String> typeBox;
    @FXML
    private ComboBox<String> categorieBox;
    @FXML
    private ComboBox<String> niveauBox;
    @FXML
    private TextField prixField;
    @FXML
    private ComboBox<String> statutBox;
    @FXML
    private TextField imageField;

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    @FXML
    public void initialize() {

        categorieBox.getItems().addAll("FITNESS", "RUNNING", "FOOTBALL", "BASKETBALL");
        niveauBox.getItems().addAll("DEBUTANT", "INTERMEDIAIRE", "AVANCE");
        statutBox.getItems().addAll( "DISPONIBLE", "INDISPONIBLE");
        typeBox.getItems().addAll(  "SPORT", "CAMPING", "INTELECTUEL", "CULTUREL");
    }

    @FXML
    private void ajouterActivite() {

        try {

            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "INSERT INTO activite (nom, type_activite, categorie_act, niveau_act, prix, statut, image_url, id_pack) VALUES (?, ?, ?, ?, ?, ?, ?, NULL)";

            PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, nomField.getText());
            pst.setString(2, typeBox.getValue());
            pst.setString(3, categorieBox.getValue());
            pst.setString(4, niveauBox.getValue());
            pst.setDouble(5, Double.parseDouble(prixField.getText()));
            pst.setString(6, statutBox.getValue());
            pst.setString(7, imageField.getText());

            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            int generatedId = 0;

            if (rs.next()) {
                generatedId = rs.getInt(1);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/recuActivite.fxml"));
            Parent root = loader.load();

            RecuActiviteController controller = loader.getController();
            controller.setData(
                    typeBox.getValue(),
                    nomField.getText(),
                    categorieBox.getValue(),
                    niveauBox.getValue(),
                    prixField.getText(),
                    statutBox.getValue()
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Reçu Activité");
            stage.show();

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur d'ajout").show();
        }
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
    @FXML
    public void goUserSeances(ActionEvent event) {
        switchScene(event, "/GUI/UserSeances.fxml");
    }

    public void goAdmin(ActionEvent event) {
        switchScene(event, "/GUI/AdminActivites.fxml");
    }
}






