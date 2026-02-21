package Controllers;

import Models.Activite;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.DataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Date;
import java.time.LocalDate;

public class EditActiviteController {

    @FXML private TextField tfNom;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField tfPrix;
    @FXML private TextField tfImageUrl;
    @FXML private DatePicker dpDate;   // ✅ FIXED

    private Activite activite;
    private boolean saved = false;

    @FXML
    public void initialize() {

        cbType.getItems().setAll("SPORT", "CAMPING", "INTELECTUEL", "CULTUREL");
        cbCategorie.getItems().setAll("FITNESS", "RUNNING", "FOOTBALL", "BASKETBALL");
        cbNiveau.getItems().setAll("DEBUTANT", "INTERMEDIAIRE", "AVANCE");
        cbStatut.getItems().setAll("DISPONIBLE", "INDISPONIBLE");
    }

    public void setActivite(Activite a) {
        this.activite = a;

        tfNom.setText(a.getNom());
        cbType.setValue(safeUpper(a.getTypeActivite()));
        cbCategorie.setValue(safeUpper(a.getCategorieAct()));
        cbNiveau.setValue(safeUpper(a.getNiveauAct()));
        cbStatut.setValue(safeUpper(a.getStatut()));
        tfPrix.setText(String.valueOf(a.getPrix()));
        tfImageUrl.setText(a.getImageUrl());

        // ✅ Correct Date conversion
        if (a.getDate() != null) {
            dpDate.setValue(a.getDate().toLocalDate());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {

        if (activite == null) {
            showAlert("Aucune activité chargée.");
            return;
        }

        String nom = tfNom.getText().trim();
        String type = cbType.getValue();
        String cat  = cbCategorie.getValue();
        String niv  = cbNiveau.getValue();
        String statut = cbStatut.getValue();
        String img = tfImageUrl.getText().trim();
        LocalDate localDate = dpDate.getValue();

        if (nom.isEmpty() || type == null || cat == null || niv == null || statut == null || localDate == null) {
            showAlert("Veuillez remplir tous les champs obligatoires.");
            return;
        }

        double prix;
        try {
            prix = Double.parseDouble(tfPrix.getText().trim());
            if (prix < 0) throw new NumberFormatException();
        } catch (Exception e) {
            showAlert("Prix invalide !");
            return;
        }

        try {
            Connection cnx = DataBase.getInstance().getConx();

            String sql =
                    "UPDATE activite SET nom=?, type_activite=?, categorie_act=?, niveau_act=?, prix=?, statut=?, image_url=?, date_reservation=? " +
                            "WHERE id_activite=?";  // ✅ FIXED SPACE

            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setString(1, nom);
            ps.setString(2, type);
            ps.setString(3, cat);
            ps.setString(4, niv);
            ps.setDouble(5, prix);
            ps.setString(6, statut);
            ps.setString(7, img);
            ps.setDate(8, Date.valueOf(localDate));   // ✅ FIXED DATE
            ps.setInt(9, activite.getIdActivite());   // ✅ FIXED ORDER

            ps.executeUpdate();

            // Update object in memory
            activite.setNom(nom);
            activite.setTypeActivite(type);
            activite.setCategorieAct(cat);
            activite.setNiveauAct(niv);
            activite.setPrix(prix);
            activite.setStatut(statut);
            activite.setImageUrl(img);
            activite.setDate(Date.valueOf(localDate));

            saved = true;
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors de la modification !");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String safeUpper(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }
}
