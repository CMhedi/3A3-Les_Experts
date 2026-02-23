package controllers;

import Entities.Activite;
import Entities.Pack;
import Services.PackInscriptionMetierService;
import Services.PackInscriptionMetierService.PriceBreakdown;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.math.BigDecimal;
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

    @FXML private TableView<ActiviteSelection> tvActivites;
    @FXML private TableColumn<ActiviteSelection, Boolean> colSelect;
    @FXML private TableColumn<ActiviteSelection, String> colNom;
    @FXML private TableColumn<ActiviteSelection, String> colCategorie;
    @FXML private TableColumn<ActiviteSelection, String> colNiveau;
    @FXML private TableColumn<ActiviteSelection, BigDecimal> colPrix;

    @FXML private Button btnCalculer;
    @FXML private Button btnInscrire;

    private final ObservableList<ActiviteSelection> activitesVm = FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        // ✅ JavaFX 21: set policy here (NOT in FXML)
        if (tvActivites != null) {
            tvActivites.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        spNbPersonnes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 1));
        spNbPersonnes.setEditable(true);

        colSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));

        colNom.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getNom()));
        colCategorie.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getCategorie()));
        colNiveau.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getNiveau()));
        colPrix.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getPrix()));

        tvActivites.setItems(activitesVm);

        try {
            cbPack.getItems().setAll(service.getActivePacks());
        } catch (Exception e) {
            showError("Erreur DB", "Impossible de charger les packs.\n" + e.getMessage());
        }

        cbPack.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                loadActivitesForPack(newV);
                clearPriceLabels();
            }
        });

        spNbPersonnes.valueProperty().addListener((obs, o, n) -> safeRecalc());

        btnCalculer.setOnAction(e -> safeRecalc());
        btnInscrire.setOnAction(e -> onCreateDraftInscription());
    }

    private void loadActivitesForPack(Pack pack) {
        try {
            List<Activite> list = service.getActivitesByPack(pack.getIdPack());
            activitesVm.clear();

            for (Activite a : list) {
                ActiviteSelection vm = new ActiviteSelection(a);

                // ✅ enforce max activities
                vm.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal && countSelected() > pack.getNbActivitesMax()) {
                        vm.setSelected(false);
                        showWarning("Limite atteinte",
                                "Max activités pour ce pack = " + pack.getNbActivitesMax());
                    }
                });

                // ✅ recalc on selection change
                vm.selectedProperty().addListener((obs, oldVal, newVal) -> safeRecalc());

                activitesVm.add(vm);
            }

            lblMaxAct.setText("Max activités : " + pack.getNbActivitesMax());

        } catch (Exception e) {
            showError("Erreur DB", "Impossible de charger les activités.\n" + e.getMessage());
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

        List<Integer> selectedActIds = activitesVm.stream()
                .filter(ActiviteSelection::isSelected)
                .map(vm -> vm.getActivite().getIdActivite())
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

        List<Integer> selectedActIds = activitesVm.stream()
                .filter(ActiviteSelection::isSelected)
                .map(vm -> vm.getActivite().getIdActivite())
                .collect(Collectors.toList());

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

            // ✅ DB enum compatible: EN_ATTENTE / VALIDEE / ANNULEE / CONFIRMEE
            showInfo("✅ Inscription créée",
                    "Inscription créée avec succès.\nID = " + id +
                            "\nStatut = EN_ATTENTE\nRéservations activités = EN_ATTENTE.");

        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    private int countSelected() {
        int c = 0;
        for (ActiviteSelection vm : activitesVm) if (vm.isSelected()) c++;
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

    // ===== VM (TableView) =====
    public static class ActiviteSelection {
        private final Activite activite;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);

        public ActiviteSelection(Activite activite) {
            this.activite = activite;
        }

        public Activite getActivite() { return activite; }

        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean v) { selected.set(v); }

        public String getNom() { return activite.getNom(); }

        public String getCategorie() {
            return activite.getCategorieAct() == null ? "" : activite.getCategorieAct().name();
        }

        public String getNiveau() {
            return activite.getNiveauAct() == null ? "" : activite.getNiveauAct().name();
        }

        public BigDecimal getPrix() { return activite.getPrix(); }
    }
}