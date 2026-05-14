package controllers;

import Entities.Pack;
import GUI.utils.ActiviteQuickAdd;
import GUI.utils.DialogUtils;
import Models.Activite;
import Services.PackService;
import Services.interfaces.ActiviteService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Formulaire d'activité (ajout rapide, création pro avec pack/GPS, modification avec relecture base).
 */
public class ActiviteQuickAddDialogController {

    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblDbHint;
    @FXML private TextField tfNom;
    @FXML private TextField tfPrix;
    @FXML private TextField tfImage;
    @FXML private TextField tfLat;
    @FXML private TextField tfLng;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private ComboBox<String> cbStatut;
    @FXML private ComboBox<Integer> cbPack;
    @FXML private TitledPane tpProOptions;
    @FXML private Button btnSave;

    private final ActiviteService activiteService = new ActiviteService();
    private final PackService packService = new PackService();

    private Runnable onSaved;
    private ActiviteQuickAdd.FormMode mode = ActiviteQuickAdd.FormMode.ADD_BASIC;
    private int editingId = -1;
    private Activite snapshotFromDb;

    private final Map<Integer, String> packLabelById = new LinkedHashMap<>();

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public boolean configure(ActiviteQuickAdd.FormMode mode, Integer idActiviteIfEdit) {
        this.mode = mode == null ? ActiviteQuickAdd.FormMode.ADD_BASIC : mode;
        this.editingId = (this.mode == ActiviteQuickAdd.FormMode.EDIT && idActiviteIfEdit != null)
                ? idActiviteIfEdit : -1;
        loadPackComboItems();

        boolean proPanel = this.mode == ActiviteQuickAdd.FormMode.ADD_PRO
                || this.mode == ActiviteQuickAdd.FormMode.EDIT;
        if (tpProOptions != null) {
            tpProOptions.setVisible(proPanel);
            tpProOptions.setManaged(proPanel);
            tpProOptions.setExpanded(proPanel);
        }

        if (lblDbHint != null) {
            lblDbHint.setVisible(false);
            lblDbHint.setManaged(false);
        }

        if (this.mode == ActiviteQuickAdd.FormMode.EDIT && editingId > 0) {
            Activite db = activiteService.getById(editingId);
            snapshotFromDb = db;
            if (db == null) {
                DialogUtils.showError("Introuvable", "Cette activité n'existe pas ou a été supprimée.");
                return false;
            }
            applyActiviteToForm(db);
            if (lblTitle != null) {
                lblTitle.setText("Modifier l'activité");
            }
            if (lblSubtitle != null) {
                lblSubtitle.setText("Données rechargées depuis la base — ajustez les champs puis enregistrez.");
            }
            if (lblDbHint != null) {
                lblDbHint.setText("Lecture fraîche (réf. #" + editingId + ") avant modification.");
                lblDbHint.setVisible(true);
                lblDbHint.setManaged(true);
            }
            if (btnSave != null) {
                btnSave.setText("Mettre à jour");
            }
        } else if (this.mode == ActiviteQuickAdd.FormMode.ADD_PRO) {
            snapshotFromDb = null;
            if (lblTitle != null) {
                lblTitle.setText("Nouvelle activité (création pro)");
            }
            if (lblSubtitle != null) {
                lblSubtitle.setText("Champs essentiels + formule catalogue et coordonnées GPS optionnelles.");
            }
            if (btnSave != null) {
                btnSave.setText("Enregistrer");
            }
        } else {
            snapshotFromDb = null;
            if (lblTitle != null) {
                lblTitle.setText("Nouvelle activité (ajout rapide)");
            }
            if (lblSubtitle != null) {
                lblSubtitle.setText("Champs essentiels uniquement — enregistrement rapide (sans pack ni GPS).");
            }
            if (btnSave != null) {
                btnSave.setText("Enregistrer");
            }
            selectPackId(0);
            if (tfLat != null) {
                tfLat.clear();
            }
            if (tfLng != null) {
                tfLng.clear();
            }
        }
        return true;
    }

    @FXML
    private void initialize() {
        if (cbType != null) {
            cbType.getItems().setAll("SPORT", "CAMPING", "INTELECTUEL", "CULTUREL");
            cbType.getSelectionModel().selectFirst();
        }
        if (cbCategorie != null) {
            cbCategorie.getItems().setAll(
                    "FITNESS", "RUNNING", "FOOTBALL", "BASKETBALL", "TENNIS", "NATATION",
                    "RANDONNEE", "CYCLISME", "YOGA", "AUTRE");
            cbCategorie.getSelectionModel().selectFirst();
        }
        if (cbNiveau != null) {
            cbNiveau.getItems().setAll("DEBUTANT", "INTERMEDIAIRE", "AVANCE");
            cbNiveau.getSelectionModel().selectFirst();
        }
        if (cbStatut != null) {
            cbStatut.getItems().setAll("DISPONIBLE", "INDISPONIBLE");
            cbStatut.getSelectionModel().selectFirst();
        }
        wirePackCombo();
        loadPackComboItems();
    }

    private void loadPackComboItems() {
        packLabelById.clear();
        packLabelById.put(0, "— Aucune formule —");
        try {
            List<Pack> packs = packService.getAll();
            if (packs != null) {
                for (Pack p : packs) {
                    if (p != null && p.getIdPack() > 0) {
                        String n = p.getNom();
                        packLabelById.put(p.getIdPack(), (n == null || n.isBlank()) ? ("Pack #" + p.getIdPack()) : n.trim());
                    }
                }
            }
        } catch (Exception ignored) {
            /* catalogue optionnel */
        }
        if (cbPack != null) {
            Integer previous = cbPack.getValue();
            cbPack.getItems().setAll(packLabelById.keySet());
            if (previous != null && packLabelById.containsKey(previous)) {
                cbPack.setValue(previous);
            } else {
                cbPack.setValue(0);
            }
        }
    }

    private void wirePackCombo() {
        if (cbPack == null) {
            return;
        }
        StringConverter<Integer> conv = new StringConverter<>() {
            @Override
            public String toString(Integer id) {
                if (id == null) {
                    return "";
                }
                return packLabelById.getOrDefault(id, "—");
            }

            @Override
            public Integer fromString(String s) {
                return 0;
            }
        };
        cbPack.setConverter(conv);
        var cellFactory = (javafx.util.Callback<javafx.scene.control.ListView<Integer>, ListCell<Integer>>) lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                } else {
                    setText(packLabelById.getOrDefault(id, "—"));
                }
            }
        };
        cbPack.setButtonCell(cellFactory.call(null));
        cbPack.setCellFactory(cellFactory);
    }

    private void applyActiviteToForm(Activite a) {
        if (tfNom != null) {
            tfNom.setText(a.getNom() != null ? a.getNom() : "");
        }
        if (tfPrix != null) {
            tfPrix.setText(String.format(Locale.FRANCE, "%s", stripTrailingZeros(a.getPrix())));
        }
        if (tfImage != null) {
            tfImage.setText(a.getImageUrl() != null ? a.getImageUrl() : "");
        }
        selectCombo(cbType, a.getTypeActivite());
        selectCombo(cbCategorie, a.getCategorieAct());
        selectCombo(cbNiveau, a.getNiveauAct());
        selectCombo(cbStatut, a.getStatut());
        selectPackId(Math.max(0, a.getIdPack()));
        if (tfLat != null) {
            Double la = a.getLatitude();
            tfLat.setText(la == null ? "" : String.valueOf(la));
        }
        if (tfLng != null) {
            Double lo = a.getLongitude();
            tfLng.setText(lo == null ? "" : String.valueOf(lo));
        }
    }

    private static String stripTrailingZeros(double v) {
        if (Math.rint(v) == v) {
            return String.valueOf((long) v);
        }
        return String.format(Locale.FRANCE, "%.2f", v);
    }

    private static void selectCombo(ComboBox<String> cb, String raw) {
        if (cb == null) {
            return;
        }
        if (raw == null || raw.isBlank()) {
            cb.getSelectionModel().selectFirst();
            return;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        for (String it : cb.getItems()) {
            if (it != null && it.equalsIgnoreCase(u)) {
                cb.setValue(it);
                return;
            }
        }
        cb.getSelectionModel().selectFirst();
    }

    private void selectPackId(int idPack) {
        if (cbPack == null) {
            return;
        }
        if (packLabelById.containsKey(idPack)) {
            cbPack.setValue(idPack);
        } else {
            cbPack.setValue(0);
        }
    }

    @FXML
    private void onSave() {
        String nom = tfNom != null ? tfNom.getText().trim() : "";
        if (nom.isEmpty()) {
            DialogUtils.showWarning("Champ requis", "Indiquez le nom de l'activité.");
            return;
        }
        if (cbType.getValue() == null || cbCategorie.getValue() == null
                || cbNiveau.getValue() == null || cbStatut.getValue() == null) {
            DialogUtils.showWarning("Champs requis", "Renseignez type, catégorie, niveau et statut.");
            return;
        }
        double prix;
        try {
            prix = Double.parseDouble(tfPrix != null ? tfPrix.getText().trim().replace(',', '.') : "");
            if (prix < 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            DialogUtils.showWarning("Prix", "Saisissez un nombre positif (TND).");
            return;
        }
        String img = tfImage != null ? tfImage.getText().trim() : "";

        int idPack = 0;
        Double lat = null;
        Double lng = null;
        if (mode == ActiviteQuickAdd.FormMode.ADD_PRO || mode == ActiviteQuickAdd.FormMode.EDIT) {
            idPack = cbPack != null && cbPack.getValue() != null ? cbPack.getValue() : 0;
            try {
                if (tfLat != null && !tfLat.getText().trim().isEmpty()) {
                    lat = Double.parseDouble(tfLat.getText().trim().replace(',', '.'));
                }
                if (tfLng != null && !tfLng.getText().trim().isEmpty()) {
                    lng = Double.parseDouble(tfLng.getText().trim().replace(',', '.'));
                }
            } catch (Exception e) {
                DialogUtils.showWarning("Coordonnées", "Latitude ou longitude invalide (utilisez un nombre décimal).");
                return;
            }
        }

        try {
            if (mode == ActiviteQuickAdd.FormMode.EDIT && editingId > 0) {
                Activite candidate = new Activite(
                        editingId,
                        nom,
                        cbType.getValue(),
                        cbCategorie.getValue(),
                        cbNiveau.getValue(),
                        prix,
                        cbStatut.getValue(),
                        img,
                        idPack,
                        lat,
                        lng
                );
                if (snapshotFromDb != null && !hasMeaningfulChanges(snapshotFromDb, candidate)) {
                    DialogUtils.showInfo("Aucune modification", "Les valeurs sont identiques à la base — rien à enregistrer.");
                    return;
                }
                activiteService.update(candidate);
                String summary = buildChangeSummary(snapshotFromDb, candidate);
                DialogUtils.showInfo("Activité mise à jour", summary.isBlank() ? "Enregistrement effectué." : summary);
            } else {
                Activite a = new Activite(
                        0,
                        nom,
                        cbType.getValue(),
                        cbCategorie.getValue(),
                        cbNiveau.getValue(),
                        prix,
                        cbStatut.getValue(),
                        img,
                        idPack,
                        lat,
                        lng
                );
                int id = activiteService.addAndReturnId(a);
                DialogUtils.showInfo("Activité créée", "Référence #" + id + " — « " + nom + " ».");
            }
            if (onSaved != null) {
                onSaved.run();
            }
            close();
        } catch (Exception ex) {
            ex.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible d'enregistrer :\n" + ex.getMessage());
        }
    }

    private static boolean hasMeaningfulChanges(Activite before, Activite after) {
        if (before == null) {
            return true;
        }
        if (!Objects.equals(safeTrim(before.getNom()), safeTrim(after.getNom()))) {
            return true;
        }
        if (!Objects.equals(safeUpper(before.getTypeActivite()), safeUpper(after.getTypeActivite()))) {
            return true;
        }
        if (!Objects.equals(safeUpper(before.getCategorieAct()), safeUpper(after.getCategorieAct()))) {
            return true;
        }
        if (!Objects.equals(safeUpper(before.getNiveauAct()), safeUpper(after.getNiveauAct()))) {
            return true;
        }
        if (Double.compare(before.getPrix(), after.getPrix()) != 0) {
            return true;
        }
        if (!Objects.equals(safeUpper(before.getStatut()), safeUpper(after.getStatut()))) {
            return true;
        }
        if (!Objects.equals(safeTrim(before.getImageUrl()), safeTrim(after.getImageUrl()))) {
            return true;
        }
        if (before.getIdPack() != after.getIdPack()) {
            return true;
        }
        if (!Objects.equals(before.getLatitude(), after.getLatitude())) {
            return true;
        }
        if (!Objects.equals(before.getLongitude(), after.getLongitude())) {
            return true;
        }
        return false;
    }

    private String buildChangeSummary(Activite before, Activite after) {
        if (before == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (!Objects.equals(safeTrim(before.getNom()), safeTrim(after.getNom()))) {
            parts.add("nom");
        }
        if (!Objects.equals(safeUpper(before.getTypeActivite()), safeUpper(after.getTypeActivite()))) {
            parts.add("type");
        }
        if (!Objects.equals(safeUpper(before.getCategorieAct()), safeUpper(after.getCategorieAct()))) {
            parts.add("catégorie");
        }
        if (!Objects.equals(safeUpper(before.getNiveauAct()), safeUpper(after.getNiveauAct()))) {
            parts.add("niveau");
        }
        if (Double.compare(before.getPrix(), after.getPrix()) != 0) {
            parts.add("prix");
        }
        if (!Objects.equals(safeUpper(before.getStatut()), safeUpper(after.getStatut()))) {
            parts.add("statut");
        }
        if (!Objects.equals(safeTrim(before.getImageUrl()), safeTrim(after.getImageUrl()))) {
            parts.add("image");
        }
        if (before.getIdPack() != after.getIdPack()) {
            parts.add("formule (pack)");
        }
        if (!Objects.equals(before.getLatitude(), after.getLatitude())
                || !Objects.equals(before.getLongitude(), after.getLongitude())) {
            parts.add("localisation");
        }
        if (parts.isEmpty()) {
            return "";
        }
        return "Champs mis à jour : " + String.join(", ", parts) + ".";
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safeUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        if (tfNom != null && tfNom.getScene() != null && tfNom.getScene().getWindow() != null) {
            ((Stage) tfNom.getScene().getWindow()).close();
        }
    }
}
