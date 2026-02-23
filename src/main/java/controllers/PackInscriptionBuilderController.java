package controllers;

import Entities.Activite;
import Entities.Pack;
import GUI.utils.SceneUtils;
import Services.PackInscriptionMetierService;
import Services.PackInscriptionMetierService.PriceBreakdown;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PackInscriptionBuilderController {

    private static final int CURRENT_USER_ID = 1;

    private final PackInscriptionMetierService service = new PackInscriptionMetierService();

    @FXML private ComboBox<Pack> cbPack;
    @FXML private Spinner<Integer> spNbPersonnes;
    @FXML private TextField txtCoupon;

    @FXML private Label lblMaxAct;
    @FXML private Label lblPackBase;
    @FXML private Label lblActTotal;
    @FXML private Label lblDiscountPack;
    @FXML private Label lblDiscountGroupe;
    @FXML private Label lblDiscountCoupon;
    @FXML private Label lblTotal;
    @FXML private Label lblDetails;

    // ✅ Table Widget (بدون TableView)
    @FXML private VBox vbRows;          // rows container
    @FXML private Label lblSelected;    // "X selected" (اختياري)

    @FXML private Button btnCalculer;
    @FXML private Button btnInscrire;

    private final List<ActiviteRow> rows = new ArrayList<>();

    @FXML
    private void initialize() {

        spNbPersonnes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 1));
        spNbPersonnes.setEditable(true);

        // load packs
        try {
            cbPack.getItems().setAll(service.getActivePacks());
        } catch (Exception e) {
            showError("Erreur DB", "Impossible de charger les packs.\n" + e.getMessage());
        }

        cbPack.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                loadActivitesForPack(newV);
                clearPriceLabels();
            } else {
                clearRows();
                clearPriceLabels();
            }
        });

        spNbPersonnes.valueProperty().addListener((obs, o, n) -> safeRecalc());

        btnCalculer.setOnAction(e -> safeRecalc());
        btnInscrire.setOnAction(e -> onCreateDraftInscription());

        // disable inscription إذا ما فمّاش pack ولا اختيار
        btnInscrire.disableProperty().bind(
                cbPack.valueProperty().isNull()
                        .or(Bindings.createBooleanBinding(
                                () -> countSelected() < 1,
                                // re-evaluate on recalc triggers
                                spNbPersonnes.valueProperty(),
                                txtCoupon.textProperty()
                        ))
        );

        // recalc إذا coupon تبدّل
        txtCoupon.textProperty().addListener((obs, o, n) -> safeRecalc());
    }

    private void loadActivitesForPack(Pack pack) {
        try {
            List<Activite> list = service.getActivitesByPack(pack.getIdPack());

            clearRows();

            for (Activite a : list) {
                ActiviteRow row = new ActiviteRow(a);

                // enforce max activities
                row.chk.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal && countSelected() > pack.getNbActivitesMax()) {
                        row.chk.setSelected(false);
                        showWarning("Limite atteinte", "Max activités pour ce pack = " + pack.getNbActivitesMax());
                        return;
                    }
                    updateSelectedCount();
                    safeRecalc();
                });

                rows.add(row);
                vbRows.getChildren().add(row.root);
            }

            lblMaxAct.setText("Max activités : " + pack.getNbActivitesMax());
            updateSelectedCount();

        } catch (Exception e) {
            showError("Erreur DB", "Impossible de charger les activités.\n" + e.getMessage());
        }
    }

    private void clearRows() {
        rows.clear();
        if (vbRows != null) vbRows.getChildren().clear();
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        if (lblSelected != null) {
            lblSelected.setText(countSelected() + " sélectionnée(s)");
        }
    }

    private void safeRecalc() {
        try {
            recalc();
        } catch (Exception ex) {
            lblDetails.setText("⚠ " + ex.getMessage());
        }
    }

    private void recalc() throws Exception {
        Pack pack = cbPack.getValue();
        if (pack == null) return;

        int nb = spNbPersonnes.getValue() == null ? 1 : spNbPersonnes.getValue();
        String coupon = txtCoupon.getText();

        List<Integer> selectedActIds = rows.stream()
                .filter(r -> r.chk.isSelected())
                .map(r -> r.activite.getIdActivite())
                .collect(Collectors.toList());

        PriceBreakdown pb = service.computePrice(pack.getIdPack(), selectedActIds, nb, coupon);

        lblPackBase.setText(format(pb.packBase()));
        lblActTotal.setText(format(pb.activitesTotal()));
        lblDiscountPack.setText("- " + format(pb.discountPack()));
        lblDiscountGroupe.setText("- " + format(pb.discountGroupe()));
        lblDiscountCoupon.setText("- " + format(pb.discountCoupon()));
        lblTotal.setText(format(pb.total()));
        lblDetails.setText(pb.details());
    }

    private void onCreateDraftInscription() {
        Pack pack = cbPack.getValue();
        if (pack == null) {
            showWarning("Pack requis", "Choisis un pack d’abord.");
            return;
        }

        int nb = spNbPersonnes.getValue() == null ? 1 : spNbPersonnes.getValue();
        String coupon = txtCoupon.getText();

        List<Integer> selectedActIds = rows.stream()
                .filter(r -> r.chk.isSelected())
                .map(r -> r.activite.getIdActivite())
                .collect(Collectors.toList());

        if (selectedActIds.isEmpty()) {
            showWarning("Activité requise", "Sélectionne au moins une activité.");
            return;
        }

        if (selectedActIds.size() > pack.getNbActivitesMax()) {
            showWarning("Limite atteinte", "Max activités = " + pack.getNbActivitesMax());
            return;
        }

        try {
            int id = service.createInscriptionDraft(
                    CURRENT_USER_ID,
                    pack.getIdPack(),
                    selectedActIds,
                    nb,
                    coupon
            );

            showInfo("✅ Inscription créée",
                    "Inscription créée avec succès.\nID = " + id +
                            "\nStatut = EN_ATTENTE\nRéservations activités = EN_ATTENTE.");

        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    private int countSelected() {
        int c = 0;
        for (ActiviteRow r : rows) if (r.chk.isSelected()) c++;
        return c;
    }

    private void clearPriceLabels() {
        lblPackBase.setText("--");
        lblActTotal.setText("--");
        lblDiscountPack.setText("--");
        lblDiscountGroupe.setText("--");
        lblDiscountCoupon.setText("--");
        lblTotal.setText("--");
        lblDetails.setText("");
    }

    private String format(BigDecimal v) {
        if (v == null) return "0.00 DT";
        return v.toPlainString() + " DT";
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showWarning(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        SceneUtils.loadScene("/GUI/MainLayout.fxml", (Node) event.getSource());
    }

    // ===== Table Widget Row =====
    private static class ActiviteRow {
        final Activite activite;
        final CheckBox chk = new CheckBox();
        final HBox root = new HBox(12);

        ActiviteRow(Activite a) {
            this.activite = a;

            root.getStyleClass().add("tw-row");
            root.setAlignment(Pos.CENTER_LEFT);

            Label nom = new Label(nvl(a.getNom()));
            nom.getStyleClass().add("tw-cell");
            nom.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nom, Priority.ALWAYS);

            Label cat = new Label(a.getCategorieAct() == null ? "" : a.getCategorieAct().name());
            cat.getStyleClass().addAll("tw-cell", "tw-chip");

            Label niv = new Label(a.getNiveauAct() == null ? "" : a.getNiveauAct().name());
            niv.getStyleClass().addAll("tw-cell", "tw-chip");

            Label prix = new Label(a.getPrix() == null ? "0" : a.getPrix().toPlainString() + " DT");
            prix.getStyleClass().addAll("tw-cell", "tw-price");

            chk.getStyleClass().add("tw-check");

            root.getChildren().addAll(chk, nom, cat, niv, prix);
        }

        private static String nvl(String s) { return s == null ? "" : s; }
    }
}