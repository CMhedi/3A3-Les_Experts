package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.time.LocalDate;

// ====== ADDED IMPORTS (PDF + SNAPSHOT) ======
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class RecuActiviteController {

    @FXML private Label lblType;
    @FXML private Label lblNom;
    @FXML private Label lblCategorie;
    @FXML private Label lblNiveau;
    @FXML private Label lblPrix;
    @FXML private Label lblStatut;
    @FXML private Label lblDate;

    // ====== ADDED (root container of receipt) ======
    @FXML private VBox receiptCard;

    public void setData(String Type, String nom, String categorie,
                        String niveau, String prix, LocalDate statut, String date) {

        lblType.setText("Type : " + Type);
        lblNom.setText("Nom : " + nom);
        lblCategorie.setText("Catégorie : " + categorie);
        lblNiveau.setText("Niveau : " + niveau);
        lblPrix.setText("Prix : " + prix + " DT");
        lblStatut.setText("Statut : " + statut);

        lblDate.setText("Date : " + date);
    }

    @FXML
    private void fermer(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }

    @FXML
    private void reserver(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/addres.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====== ADDED FUNCTION: Download receipt as PDF ======
    @FXML
    private void downloadPdf(ActionEvent event) {
        try {
            // 1) snapshot du reçu (la carte entière)
            SnapshotParameters params = new SnapshotParameters();
            WritableImage fxImage = receiptCard.snapshot(params, null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);

            // 2) choisir l'endroit de sauvegarde
            FileChooser fc = new FileChooser();
            fc.setTitle("Enregistrer le reçu en PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fc.setInitialFileName("recu_activite.pdf");

            File file = fc.showSaveDialog(((Node) event.getSource()).getScene().getWindow());
            if (file == null) return;

            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }

            // 3) créer PDF avec la taille exacte de l'image
            try (PDDocument doc = new PDDocument()) {
                float width = bufferedImage.getWidth();
                float height = bufferedImage.getHeight();

                PDPage page = new PDPage(new PDRectangle(width, height));
                doc.addPage(page);

                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, bufferedImage);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, 0, 0, width, height);
                }

                doc.save(file);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}