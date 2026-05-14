package controllers;

import Entities.Pack;
import Entities.Session;
import Entities.UserApp;
import GUI.utils.ActiviteQuickAdd;
import GUI.utils.DialogUtils;
import Models.Activite;
import Models.Reservation;
import enums.RoleUser;
import Services.PackService;
import Services.interfaces.ActiviteService;
import Services.interfaces.ReservationService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Catalogue & réservations d'activités — vue client (onglets + liste cartes classique).
 */
public class ReservationActiviteUserController implements Initializable {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy · HH:mm", Locale.FRENCH);

    @FXML
    private Label lblActiviteGestHint;
    @FXML
    private Label lblCatalogLead;
    @FXML
    private Label lblInfo;
    @FXML
    private TabPane tabs;
    @FXML
    private TextField searchAct;
    @FXML
    private TextField searchRes;
    @FXML
    private ComboBox<String> comboType;
    @FXML
    private ComboBox<String> comboStatut;
    @FXML
    private ComboBox<String> comboSort;
    @FXML
    private ListView<Activite> listActivites;
    @FXML
    private ListView<Reservation> listReservations;
    @FXML
    private VBox paneEmptyAct;
    @FXML
    private VBox paneEmptyRes;

    @FXML
    private Button btnAjoutActiviteBasique;
    @FXML
    private Button btnAjoutActivitePro;
    @FXML
    private Button btnToggleCreateAct;
    @FXML
    private VBox boxCreateActForm;
    @FXML
    private TextField newActNom;
    @FXML
    private ComboBox<String> newActType;
    @FXML
    private ComboBox<String> newActCategorie;
    @FXML
    private ComboBox<String> newActNiveau;
    @FXML
    private TextField newActPrix;
    @FXML
    private ComboBox<String> newActStatut;
    @FXML
    private TextField newActImage;

    private boolean catalogCreateFormOpen;

    private final ActiviteService activiteService = new ActiviteService();
    private final ReservationService reservationService = new ReservationService();
    private final PackService packService = new PackService();

    private final ObservableList<Activite> activiteRows = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservationRows = FXCollections.observableArrayList();
    private FilteredList<Activite> filteredActivites;
    private SortedList<Activite> sortedActivites;
    private FilteredList<Reservation> filteredReservations;

    private Map<Integer, String> packNames = Map.of();
    private Map<Integer, String> activiteNamesById = Map.of();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initFilterCombos();

        filteredActivites = new FilteredList<>(activiteRows, a -> true);
        sortedActivites = new SortedList<>(filteredActivites);
        if (comboSort != null) {
            sortedActivites.comparatorProperty().bind(
                    Bindings.createObjectBinding(this::buildSortComparator, comboSort.valueProperty()));
        } else {
            sortedActivites.setComparator(Comparator.comparing(
                    a -> safeTrim(a.getNom(), ""), String.CASE_INSENSITIVE_ORDER));
        }

        if (listActivites != null) {
            listActivites.setItems(sortedActivites);
            listActivites.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            listActivites.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Activite item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    setGraphic(buildActivitePackCard(item));
                    setText(null);
                }
            });
        }

        filteredReservations = new FilteredList<>(reservationRows, r -> true);
        if (listReservations != null) {
            listReservations.setItems(filteredReservations);
            listReservations.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            listReservations.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Reservation item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    setGraphic(buildReservationPackCard(item));
                    setText(null);
                }
            });
        }

        Runnable refreshCat = this::refreshCatalogPredicate;
        if (searchAct != null) {
            searchAct.textProperty().addListener((o, a, b) -> refreshCat.run());
        }
        if (comboType != null) {
            comboType.valueProperty().addListener((o, a, b) -> refreshCat.run());
        }
        if (comboStatut != null) {
            comboStatut.valueProperty().addListener((o, a, b) -> refreshCat.run());
        }
        if (comboSort != null) {
            comboSort.valueProperty().addListener((o, a, b) -> updateCatalogSummary());
        }

        sortedActivites.addListener((ListChangeListener<Activite>) c -> updateCatalogSummary());
        filteredReservations.addListener((ListChangeListener<Reservation>) c -> {
            refreshEmptyPanels();
        });

        if (searchRes != null) {
            searchRes.textProperty().addListener((o, a, b) -> {
                filteredReservations.setPredicate(r -> matchReservation(r));
                refreshEmptyPanels();
            });
        }

        if (tabs != null) {
            tabs.getSelectionModel().selectedIndexProperty().addListener((obs, prev, cur) -> {
                if (cur != null && cur.intValue() == 0) {
                    refreshCatalogGestioneUi();
                    if (listActivites != null) {
                        listActivites.refresh();
                    }
                }
            });
        }

        initCatalogCreateFormForRole();
        reloadAll();
    }

    private boolean canCrudActiviteOnCatalogPage() {
        UserApp u = Session.getConnectedUser();
        if (u == null || u.getRole() == null) {
            return false;
        }
        RoleUser r = u.getRole();
        return r == RoleUser.ADMIN || r == RoleUser.USER_SIMPLE;
    }

    private void refreshCatalogGestioneUi() {
        boolean can = canCrudActiviteOnCatalogPage();
        UserApp u = Session.getConnectedUser();
        if (btnAjoutActiviteBasique != null) {
            btnAjoutActiviteBasique.setVisible(can);
            btnAjoutActiviteBasique.setManaged(can);
        }
        if (btnAjoutActivitePro != null) {
            btnAjoutActivitePro.setVisible(can);
            btnAjoutActivitePro.setManaged(can);
        }
        if (btnToggleCreateAct != null) {
            btnToggleCreateAct.setVisible(can);
            btnToggleCreateAct.setManaged(can);
        }
        if (lblActiviteGestHint != null) {
            if (can) {
                lblActiviteGestHint.setVisible(false);
                lblActiviteGestHint.setManaged(false);
            } else {
                lblActiviteGestHint.setManaged(true);
                lblActiviteGestHint.setVisible(true);
                if (u != null) {
                    RoleUser r = u.getRole();
                    String roleTxt = r != null ? r.name() : "inconnu";
                    lblActiviteGestHint.setText(
                            "La création / modification / suppression d’activités sur cette page est réservée aux comptes "
                                    + "« utilisateur simple » (client) ou administrateur. Rôle actuel : "
                                    + roleTxt + ".");
                } else {
                    lblActiviteGestHint.setText(
                            "Connectez-vous avec un compte client ou administrateur pour gérer le catalogue d’activités.");
                }
            }
        }
    }

    private void initCatalogCreateFormForRole() {
        refreshCatalogGestioneUi();
        boolean can = canCrudActiviteOnCatalogPage();
        if (!can && boxCreateActForm != null) {
            boxCreateActForm.setVisible(false);
            boxCreateActForm.setManaged(false);
            catalogCreateFormOpen = false;
            return;
        }
        if (newActType != null) {
            newActType.getItems().setAll("SPORT", "CAMPING", "INTELECTUEL", "CULTUREL");
            newActType.getSelectionModel().selectFirst();
        }
        if (newActCategorie != null) {
            newActCategorie.getItems().setAll(
                    "FITNESS", "RUNNING", "FOOTBALL", "BASKETBALL", "TENNIS", "NATATION",
                    "RANDONNEE", "CYCLISME", "YOGA", "AUTRE");
            newActCategorie.getSelectionModel().selectFirst();
        }
        if (newActNiveau != null) {
            newActNiveau.getItems().setAll("DEBUTANT", "INTERMEDIAIRE", "AVANCE");
            newActNiveau.getSelectionModel().selectFirst();
        }
        if (newActStatut != null) {
            newActStatut.getItems().setAll("DISPONIBLE", "INDISPONIBLE");
            newActStatut.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onAjoutActiviteBasique() {
        if (!canCrudActiviteOnCatalogPage()) {
            DialogUtils.showWarning("Accès refusé", "Connectez-vous avec un compte client ou administrateur pour ajouter une activité.");
            return;
        }
        if (tabs == null || tabs.getScene() == null) {
            return;
        }
        Window w = tabs.getScene().getWindow();
        ActiviteQuickAdd.open(w, ActiviteQuickAdd.FormMode.ADD_BASIC, null, this::reloadAll);
    }

    @FXML
    private void onAjoutActivitePro() {
        if (!canCrudActiviteOnCatalogPage()) {
            DialogUtils.showWarning("Accès refusé", "Connectez-vous avec un compte client ou administrateur pour ajouter une activité.");
            return;
        }
        if (tabs == null || tabs.getScene() == null) {
            return;
        }
        Window w = tabs.getScene().getWindow();
        ActiviteQuickAdd.open(w, ActiviteQuickAdd.FormMode.ADD_PRO, null, this::reloadAll);
    }

    private void openActiviteEditDialog(Activite a) {
        if (!canCrudActiviteOnCatalogPage() || a == null || a.getIdActivite() <= 0) {
            return;
        }
        if (tabs == null || tabs.getScene() == null) {
            return;
        }
        Window w = tabs.getScene().getWindow();
        ActiviteQuickAdd.open(w, ActiviteQuickAdd.FormMode.EDIT, a.getIdActivite(), this::reloadAll);
    }

    private void openActiviteDeleteDialog(Activite a) {
        if (!canCrudActiviteOnCatalogPage() || a == null || a.getIdActivite() <= 0) {
            return;
        }
        String nom = safeTrim(a.getNom(), "cette activité");
        if (!DialogUtils.showConfirmation(
                "Supprimer l’activité",
                "Confirmer la suppression définitive de « " + nom + " » (réf. #" + a.getIdActivite() + ") ?")) {
            return;
        }
        try {
            activiteService.delete(a.getIdActivite());
            DialogUtils.showInfo("Activité supprimée", "« " + nom + " » a été retirée du catalogue.");
            reloadAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible de supprimer l’activité :\n" + ex.getMessage());
        }
    }

    @FXML
    private void onToggleCreateActForm() {
        if (!canCrudActiviteOnCatalogPage() || boxCreateActForm == null) {
            return;
        }
        catalogCreateFormOpen = !catalogCreateFormOpen;
        boxCreateActForm.setVisible(catalogCreateFormOpen);
        boxCreateActForm.setManaged(catalogCreateFormOpen);
        if (btnToggleCreateAct != null) {
            btnToggleCreateAct.setText(catalogCreateFormOpen ? "▼  Masquer le formulaire" : "➕  Nouvelle activité");
        }
    }

    @FXML
    private void onResetNewActiviteForm() {
        if (newActNom != null) {
            newActNom.clear();
        }
        if (newActPrix != null) {
            newActPrix.clear();
        }
        if (newActImage != null) {
            newActImage.clear();
        }
        if (newActType != null) {
            newActType.getSelectionModel().selectFirst();
        }
        if (newActCategorie != null) {
            newActCategorie.getSelectionModel().selectFirst();
        }
        if (newActNiveau != null) {
            newActNiveau.getSelectionModel().selectFirst();
        }
        if (newActStatut != null) {
            newActStatut.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onSaveNewActivite() {
        if (!canCrudActiviteOnCatalogPage()) {
            DialogUtils.showWarning("Accès refusé", "Connectez-vous avec un compte client ou administrateur pour créer une activité depuis le catalogue.");
            return;
        }
        String nom = newActNom != null ? newActNom.getText().trim() : "";
        if (nom.isEmpty()) {
            DialogUtils.showWarning("Champ manquant", "Indiquez le nom de l’activité.");
            return;
        }
        if (newActType == null || newActCategorie == null || newActNiveau == null || newActStatut == null) {
            DialogUtils.showError("Erreur", "Interface formulaire incomplète.");
            return;
        }
        if (newActType.getValue() == null || newActCategorie.getValue() == null
                || newActNiveau.getValue() == null || newActStatut.getValue() == null) {
            DialogUtils.showWarning("Champs manquants", "Renseignez type, discipline, niveau et accès (statut).");
            return;
        }
        double prix;
        try {
            prix = Double.parseDouble(newActPrix != null ? newActPrix.getText().trim() : "");
            if (prix < 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            DialogUtils.showWarning("Tarif invalide", "Saisissez un nombre positif (ex. 25 ou 19.5).");
            return;
        }
        String img = newActImage != null ? newActImage.getText().trim() : "";

        try {
            Activite a = new Activite(
                    0,
                    nom,
                    newActType.getValue(),
                    newActCategorie.getValue(),
                    newActNiveau.getValue(),
                    prix,
                    newActStatut.getValue(),
                    img,
                    0,
                    null,
                    null
            );
            int id = activiteService.addAndReturnId(a);
            DialogUtils.showInfo(
                    "Activité créée",
                    "« " + nom + " » a été ajoutée au catalogue (réf. #" + id + ").");
            onResetNewActiviteForm();
            reloadAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            DialogUtils.showError("Erreur", "Impossible d’enregistrer l’activité :\n" + ex.getMessage());
        }
    }

    private void initFilterCombos() {
        if (comboType != null) {
            comboType.getItems().setAll(
                    "Tous les types", "Sport", "Camping", "Intellectuel", "Culturel");
            comboType.getSelectionModel().selectFirst();
        }
        if (comboStatut != null) {
            comboStatut.getItems().setAll(
                    "Tous les statuts", "Ouvertes", "Fermées");
            comboStatut.getSelectionModel().selectFirst();
        }
        if (comboSort != null) {
            comboSort.getItems().setAll(
                    "Pertinence (nom A-Z)",
                    "Prix croissant",
                    "Prix décroissant",
                    "Nom Z-A");
            comboSort.getSelectionModel().selectFirst();
        }
    }

    private Comparator<Activite> buildSortComparator() {
        if (comboSort == null || comboSort.getValue() == null) {
            return Comparator.comparing(a -> safeTrim(a.getNom(), ""), String.CASE_INSENSITIVE_ORDER);
        }
        String v = comboSort.getValue();
        if (v.contains("Prix croissant")) {
            return Comparator.comparingDouble(Activite::getPrix);
        }
        if (v.contains("Prix décroissant")) {
            return Comparator.comparingDouble(Activite::getPrix).reversed();
        }
        if (v.contains("Z-A")) {
            return Comparator.comparing((Activite a) -> safeTrim(a.getNom(), ""), String.CASE_INSENSITIVE_ORDER)
                    .reversed();
        }
        return Comparator.comparing(a -> safeTrim(a.getNom(), ""), String.CASE_INSENSITIVE_ORDER);
    }

    private void refreshCatalogPredicate() {
        if (filteredActivites == null) {
            return;
        }
        filteredActivites.setPredicate(a -> matchActiviteFilters(a));
        updateCatalogSummary();
        refreshEmptyPanels();
    }

    @FXML
    private void onApplyFilters() {
        refreshCatalogPredicate();
    }

    @FXML
    private void onResetFilters() {
        if (searchAct != null) {
            searchAct.clear();
        }
        if (comboType != null) {
            comboType.getSelectionModel().selectFirst();
        }
        if (comboStatut != null) {
            comboStatut.getSelectionModel().selectFirst();
        }
        if (comboSort != null) {
            comboSort.getSelectionModel().selectFirst();
        }
        refreshCatalogPredicate();
    }

    @FXML
    private void onRefresh() {
        reloadAll();
    }

    private VBox buildActivitePackCard(Activite a) {
        VBox card = new VBox(14);
        card.getStyleClass().add("ra-list-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label badge = new Label(labelTypeActivite(a.getTypeActivite()).toUpperCase(Locale.FRENCH));
        badge.getStyleClass().add("ra-card-badge");
        badge.setMaxWidth(Double.MAX_VALUE);
        badge.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(safeTrim(a.getNom(), "Activité"));
        title.getStyleClass().add("ra-card-title");
        title.setWrapText(true);

        Region sep = new Region();
        sep.getStyleClass().add("ra-card-sep");

        HBox priceRow = new HBox(16);
        priceRow.setAlignment(Pos.BASELINE_LEFT);
        VBox priceCol = new VBox(4);
        HBox.setHgrow(priceCol, Priority.ALWAYS);
        Label pl = new Label("Tarif affiché");
        pl.getStyleClass().add("ra-card-price-label");
        Label pv = new Label(String.format(Locale.FRANCE, "%.2f €", a.getPrix()));
        pv.getStyleClass().add("ra-card-price");
        priceCol.getChildren().addAll(pl, pv);
        String pack = resolvePackLabel(a);
        VBox side = new VBox(6);
        side.setAlignment(Pos.TOP_RIGHT);
        if (pack != null && !"—".equals(pack)) {
            Label pk = new Label("Formule : " + pack);
            pk.getStyleClass().add("ra-card-meta");
            pk.setWrapText(true);
            side.getChildren().add(pk);
        }
        priceRow.getChildren().addAll(priceCol, side);

        VBox meta = new VBox(6);
        meta.getChildren().add(rowMeta("Discipline", labelCategorie(a.getCategorieAct())));
        meta.getChildren().add(rowMeta("Niveau", labelNiveau(a.getNiveauAct())));
        meta.getChildren().add(rowMeta("Accès", labelStatutActivite(a.getStatut())));

        Button cta = new Button("Réserver cette activité");
        cta.getStyleClass().add("ra-btn-primary");
        cta.setMaxWidth(Double.MAX_VALUE);
        boolean can = a.getStatut() != null && a.getStatut().trim().equalsIgnoreCase("DISPONIBLE");
        cta.setDisable(!can);
        if (!can) {
            cta.setTooltip(new Tooltip("Cette activité n’est pas ouverte aux réservations pour le moment."));
        } else {
            Tooltip tip = new Tooltip("Ouvre le formulaire de demande (participants, ville).");
            tip.setShowDelay(Duration.millis(400));
            cta.setTooltip(tip);
        }
        cta.setOnAction(e -> openReservationDialog(a));

        VBox foot = new VBox(10);
        foot.setMaxWidth(Double.MAX_VALUE);
        foot.getChildren().add(cta);
        if (canCrudActiviteOnCatalogPage()) {
            HBox crud = new HBox(10);
            crud.setAlignment(Pos.CENTER_LEFT);
            crud.setMaxWidth(Double.MAX_VALUE);

            Button edit = new Button("Modifier");
            edit.getStyleClass().add("ra-btn-card-secondary");
            edit.setOnAction(e -> {
                e.consume();
                openActiviteEditDialog(a);
            });
            Tooltip tEdit = new Tooltip("Édition (données rechargées depuis la base).");
            tEdit.setShowDelay(Duration.millis(300));
            edit.setTooltip(tEdit);

            Button del = new Button("Supprimer");
            del.getStyleClass().add("ra-btn-card-danger");
            del.setOnAction(e -> {
                e.consume();
                openActiviteDeleteDialog(a);
            });
            Tooltip tDel = new Tooltip("Retirer définitivement cette fiche du catalogue.");
            tDel.setShowDelay(Duration.millis(300));
            del.setTooltip(tDel);

            crud.getChildren().addAll(edit, del);
            foot.getChildren().add(crud);
        }

        card.getChildren().addAll(badge, title, sep, priceRow, meta, foot);
        return card;
    }

    private static HBox rowMeta(String k, String v) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lk = new Label(k + " :");
        lk.getStyleClass().add("ra-card-meta");
        Label lv = new Label(v);
        lv.getStyleClass().add("ra-card-meta");
        lv.setWrapText(true);
        HBox.setHgrow(lv, Priority.ALWAYS);
        row.getChildren().addAll(lk, lv);
        return row;
    }

    private VBox buildReservationPackCard(Reservation r) {
        VBox card = new VBox(12);
        card.getStyleClass().add("ra-list-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label badge = new Label(labelStatutReservationShort(r.getStatut()).toUpperCase(Locale.FRENCH));
        badge.getStyleClass().add("ra-card-badge");

        Label title = new Label(resolveActiviteNom(r));
        title.getStyleClass().add("ra-card-title");
        title.setWrapText(true);

        Region sep = new Region();
        sep.getStyleClass().add("ra-card-sep");

        String nb = r.getNbPersonnes() == 1 ? "1 personne" : r.getNbPersonnes() + " personnes";
        String dt = r.getDateReservation() == null ? "—" : r.getDateReservation().format(DTF);

        VBox meta = new VBox(6);
        meta.getChildren().add(rowMeta("Statut", labelStatutReservation(r.getStatut())));
        meta.getChildren().add(rowMeta("Participants", nb));
        meta.getChildren().add(rowMeta("Horodatage", dt));
        meta.getChildren().add(rowMeta("Ville", rnv(r.getVilleUser())));

        card.getChildren().addAll(badge, title, sep, meta);
        return card;
    }

    private void refreshEmptyPanels() {
        if (paneEmptyAct != null) {
            boolean emptyA = sortedActivites == null || sortedActivites.isEmpty();
            paneEmptyAct.setVisible(emptyA);
            paneEmptyAct.setManaged(emptyA);
            paneEmptyAct.setMouseTransparent(!emptyA);
        }
        if (paneEmptyRes != null) {
            boolean emptyR = filteredReservations == null || filteredReservations.isEmpty();
            paneEmptyRes.setVisible(emptyR);
            paneEmptyRes.setManaged(emptyR);
            paneEmptyRes.setMouseTransparent(!emptyR);
        }
    }

    private void updateCatalogSummary() {
    }

    private void openReservationDialog(Activite sel) {
        UserApp u = Session.getConnectedUser();
        if (u == null) {
            DialogUtils.showError("Session", "Vous devez être connecté pour réserver.");
            return;
        }
        if (sel == null) {
            return;
        }
        String st = sel.getStatut();
        if (st == null || !st.trim().equalsIgnoreCase("DISPONIBLE")) {
            DialogUtils.showWarning(
                    "Indisponible",
                    "Cette activité n’est pas ouverte aux réservations pour le moment (" + labelStatutActivite(st) + ").");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle réservation");
        dialog.setHeaderText(safeTrim(sel.getNom(), "Activité")
                + "\n" + String.format(Locale.FRANCE, "Tarif affiché : %.2f €", sel.getPrix()));

        ButtonType ok = new ButtonType("Confirmer la demande", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        var css = getClass().getResource("/styles/app.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        var raCss = getClass().getResource("/styles/reservationActivitesUser.css");
        if (raCss != null) {
            dialog.getDialogPane().getStylesheets().add(raCss.toExternalForm());
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8, 0, 0, 0));

        DatePicker dpDate = new DatePicker(LocalDate.now());
        dpDate.setPromptText("Choisir la date");
        dpDate.setPrefWidth(220);

        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll(
                "EN_ATTENTE",
                "LISTE_ATTENTE",
                "CONFIRMEE");
        cbStatut.setValue("EN_ATTENTE");
        cbStatut.setPrefWidth(220);
        cbStatut.setTooltip(new Tooltip(
                "En attente : demande à valider par l’équipe. Liste d’attente : places complètes. Confirmée : si accord préalable."));

        Spinner<Integer> spNb = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        spNb.setEditable(true);
        spNb.setPrefWidth(120);
        TextField tfVille = new TextField();
        tfVille.setPromptText("Ex. Tunis, Sousse…");

        Label l0 = new Label("Date de réservation");
        l0.setStyle("-fx-font-weight:700;");
        Label lStat = new Label("Statut de la demande");
        lStat.setStyle("-fx-font-weight:700;");
        Label l1 = new Label("Nombre de participants");
        l1.setStyle("-fx-font-weight:700;");
        Label l2 = new Label("Ville de référence");
        l2.setStyle("-fx-font-weight:700;");

        grid.add(l0, 0, 0);
        grid.add(dpDate, 1, 0);
        grid.add(lStat, 0, 1);
        grid.add(cbStatut, 1, 1);
        grid.add(l1, 0, 2);
        grid.add(spNb, 1, 2);
        grid.add(l2, 0, 3);
        grid.add(tfVille, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ok) {
                return;
            }
            LocalDate chosen = dpDate.getValue();
            if (chosen == null) {
                DialogUtils.showWarning("Date requise", "Choisissez une date de réservation.");
                return;
            }
            String statutChoisi = cbStatut.getValue();
            if (statutChoisi == null || statutChoisi.isBlank()) {
                DialogUtils.showWarning("Statut", "Choisissez un statut pour la demande.");
                return;
            }
            spNb.commitValue();
            int nb = spNb.getValue();
            String ville = tfVille.getText() != null ? tfVille.getText().trim() : "";
            String villeDb = ville.isEmpty() ? null : ville;
            LocalDateTime dateReservation = LocalDateTime.of(chosen, LocalTime.NOON);

            try {
                Reservation res = new Reservation(
                        0,
                        dateReservation,
                        statutChoisi,
                        nb,
                        u.getIdUser(),
                        sel.getIdActivite(),
                        villeDb
                );
                reservationService.add(res);
                DialogUtils.showInfo(
                        "Demande enregistrée",
                        "Réservation pour « " + safeTrim(sel.getNom(), "l’activité")
                                + " » — date : " + chosen + ", statut : " + statutChoisi.replace('_', ' ')
                                + ". Suivi dans l’onglet « Mes réservations ».");
                reloadAll();
                if (tabs != null && tabs.getTabs().size() > 1) {
                    tabs.getSelectionModel().select(1);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                DialogUtils.showError("Erreur", "Impossible d’enregistrer la réservation :\n" + ex.getMessage());
            }
        });
    }

    private void reloadAll() {
        UserApp u = Session.getConnectedUser();

        Map<Integer, String> packs = new HashMap<>();
        try {
            List<Pack> plist = packService.getAll();
            if (plist != null) {
                for (Pack p : plist) {
                    if (p != null && p.getIdPack() > 0 && p.getNom() != null && !p.getNom().isBlank()) {
                        packs.put(p.getIdPack(), p.getNom().trim());
                    }
                }
            }
        } catch (Exception ignored) {
            /* optionnel */
        }
        packNames = packs;

        List<Activite> acts = activiteService.getAll();
        activiteRows.setAll(acts);

        Map<Integer, String> names = new HashMap<>();
        for (Activite a : acts) {
            if (a != null && a.getIdActivite() > 0) {
                String n = a.getNom();
                if (n != null && !n.isBlank()) {
                    names.put(a.getIdActivite(), n.trim());
                }
            }
        }
        activiteNamesById = names;

        if (lblCatalogLead != null) {
            lblCatalogLead.setText(
                    "Affinez par type, statut et tri — chaque carte regroupe les informations comme sur la vitrine « packs ».");
        }

        reservationRows.clear();
        if (u == null) {
            if (lblInfo != null) {
                lblInfo.setText("Connectez-vous pour voir vos réservations d’activités.");
            }
            filteredReservations.setPredicate(r -> matchReservation(r));
            refreshCatalogPredicate();
            refreshCatalogGestioneUi();
            if (listActivites != null) {
                listActivites.refresh();
            }
            return;
        }

        List<Reservation> mine = reservationService.findByUserId(u.getIdUser());
        reservationRows.setAll(mine);

        if (lblInfo != null) {
            lblInfo.setText("Vous avez " + mine.size() + " demande(s) — détail sous forme de cartes.");
        }

        filteredReservations.setPredicate(r -> matchReservation(r));
        refreshCatalogPredicate();
        refreshEmptyPanels();
        refreshCatalogGestioneUi();
        if (listActivites != null) {
            listActivites.refresh();
        }
    }

    private boolean matchActiviteFilters(Activite a) {
        if (a == null) {
            return false;
        }
        if (!matchSearchAct(a)) {
            return false;
        }
        String typeF = comboType != null ? comboType.getValue() : null;
        if (typeF != null && !typeF.startsWith("Tous")) {
            if (!labelTypeActivite(a.getTypeActivite()).equalsIgnoreCase(typeF.trim())) {
                return false;
            }
        }
        String statF = comboStatut != null ? comboStatut.getValue() : null;
        if (statF != null && !statF.startsWith("Tous")) {
            String raw = a.getStatut();
            if (statF.contains("Ouvert")) {
                if (raw == null || !raw.trim().equalsIgnoreCase("DISPONIBLE")) {
                    return false;
                }
            } else if (statF.contains("Ferm")) {
                if (raw == null || !raw.trim().equalsIgnoreCase("INDISPONIBLE")) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchSearchAct(Activite a) {
        String q = searchAct != null ? searchAct.getText() : "";
        if (q == null || q.isBlank()) {
            return true;
        }
        String s = normalizeQ(q);
        return contains(s, a.getNom())
                || contains(s, labelTypeActivite(a.getTypeActivite()))
                || contains(s, labelCategorie(a.getCategorieAct()))
                || contains(s, labelNiveau(a.getNiveauAct()))
                || contains(s, resolvePackLabel(a))
                || contains(s, labelStatutActivite(a.getStatut()));
    }

    private boolean matchReservation(Reservation r) {
        if (r == null) {
            return false;
        }
        String q = searchRes != null ? searchRes.getText() : "";
        if (q == null || q.isBlank()) {
            return true;
        }
        String s = normalizeQ(q);
        String stat = r.getStatut();
        return contains(s, resolveActiviteNom(r))
                || contains(s, labelStatutReservation(stat))
                || contains(s, labelStatutReservationShort(stat))
                || contains(s, rnv(r.getVilleUser()));
    }

    private static String normalizeQ(String q) {
        return q.trim().toLowerCase(Locale.FRENCH);
    }

    private static boolean contains(String needle, String hay) {
        if (hay == null || hay.isBlank()) {
            return false;
        }
        return hay.toLowerCase(Locale.FRENCH).contains(needle);
    }

    private String resolvePackLabel(Activite a) {
        if (a == null || a.getIdPack() <= 0) {
            return "—";
        }
        String n = packNames.get(a.getIdPack());
        if (n != null && !n.isBlank()) {
            return n;
        }
        return "Formule catalogue";
    }

    private String resolveActiviteNom(Reservation r) {
        if (r == null) {
            return "—";
        }
        String joined = r.getActiviteNom();
        if (joined != null && !joined.isBlank()) {
            return joined.trim();
        }
        String fromCache = activiteNamesById.get(r.getIdActivite());
        if (fromCache != null && !fromCache.isBlank()) {
            return fromCache;
        }
        Activite a = activiteService.getById(r.getIdActivite());
        if (a != null && a.getNom() != null && !a.getNom().isBlank()) {
            return a.getNom().trim();
        }
        return "Activité";
    }

    private static String safeTrim(String v, String fallback) {
        if (v == null || v.isBlank()) {
            return fallback;
        }
        return v.trim();
    }

    private static String rnv(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }

    private static String labelTypeActivite(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "SPORT" -> "Sport";
            case "CAMPING" -> "Camping";
            case "INTELECTUEL" -> "Intellectuel";
            case "CULTUREL" -> "Culturel";
            default -> humanizeToken(code);
        };
    }

    private static String labelCategorie(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "FITNESS" -> "Fitness";
            case "RUNNING" -> "Course à pied";
            case "FOOTBALL" -> "Football";
            case "BASKETBALL" -> "Basketball";
            case "TENNIS" -> "Tennis";
            case "NATATION" -> "Natation";
            case "RANDONNEE" -> "Randonnée";
            case "CYCLISME" -> "Cyclisme";
            case "YOGA" -> "Yoga";
            case "AUTRE" -> "Autre";
            default -> humanizeToken(code);
        };
    }

    private static String labelNiveau(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "DEBUTANT" -> "Débutant";
            case "INTERMEDIAIRE" -> "Intermédiaire";
            case "AVANCE" -> "Avancé";
            default -> humanizeToken(code);
        };
    }

    private static String labelStatutActivite(String code) {
        if (code == null || code.isBlank()) {
            return "Non précisé";
        }
        if (code.trim().equalsIgnoreCase("DISPONIBLE")) {
            return "Ouverte aux réservations";
        }
        if (code.trim().equalsIgnoreCase("INDISPONIBLE")) {
            return "Fermée temporairement";
        }
        return humanizeToken(code);
    }

    private static String labelStatutReservationShort(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        String u = code.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (u) {
            case "EN_ATTENTE" -> "En attente";
            case "CONFIRMEE", "CONFIRMÉE" -> "Confirmée";
            case "ANNULEE", "ANNULÉE" -> "Annulée";
            case "SCANNEE", "SCANNÉE" -> "Validée";
            case "TERMINEE", "TERMINÉE" -> "Terminée";
            case "LISTE_ATTENTE" -> "Liste d’attente";
            default -> labelStatutReservation(code);
        };
    }

    private static String labelStatutReservation(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        String u = code.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (u) {
            case "EN_ATTENTE" -> "En attente de confirmation";
            case "CONFIRMEE", "CONFIRMÉE" -> "Confirmée";
            case "ANNULEE", "ANNULÉE" -> "Annulée";
            case "SCANNEE", "SCANNÉE" -> "Validée sur place";
            case "TERMINEE", "TERMINÉE" -> "Terminée";
            case "LISTE_ATTENTE" -> "Liste d’attente";
            default -> humanizeToken(code);
        };
    }

    private static String humanizeToken(String raw) {
        String t = raw.trim().replace('_', ' ').toLowerCase(Locale.FRENCH);
        if (t.isEmpty()) {
            return "—";
        }
        return t.substring(0, 1).toUpperCase(Locale.FRENCH) + t.substring(1);
    }
}
