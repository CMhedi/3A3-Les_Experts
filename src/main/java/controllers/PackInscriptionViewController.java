package controllers;

import Entities.Pack;
import Entities.Activite;
import Entities.Inscription;
import Entities.Session;
import Entities.UserApp;
import enums.StatutPack;
import enums.TypePack;
import Services.PackService;
import Services.PackServiceUser;
import Services.InscriptionService;
import Services.PromoEngineService;
import GUI.utils.SceneUtils;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class PackInscriptionViewController implements Initializable {

    private static final Logger LOGGER =
            Logger.getLogger(PackInscriptionViewController.class.getName());

    // ══════════════════════ FXML ══════════════════════
    @FXML private Button   btnBack;
    @FXML private GridPane packsGrid;

    @FXML private Label lblKpiResults;
    @FXML private Label lblKpiTypes;
    @FXML private Label lblKpiFilters;
    @FXML private TextField txtSearchPack;
    @FXML private ComboBox<String> comboPackType;
    @FXML private ComboBox<String> comboPackStatut;
    @FXML private ComboBox<String> comboPackSort;
    @FXML private Label lblPackFilterIntersection;

    @FXML private VBox  detailsSection;
    @FXML private Label lblPackName;
    @FXML private Label lblPackDescription;
    @FXML private Label lblPriceBase;
    @FXML private Label lblReduction;
    @FXML private Label lblPriceTotal;
    @FXML private VBox  activitiesList;

    @FXML private VBox      personalInfoSection;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private Spinner<Integer> spnbPersonnes;
    @FXML private TextField txtPromoCode;

    @FXML private VBox        paymentSection;
    @FXML private RadioButton rbCardPayment;
    @FXML private RadioButton rbMobilePayment;
    @FXML private RadioButton rbBankTransfer;
    @FXML private VBox        cardPaymentPanel;
    @FXML private TextField   txtCardNumber;
    @FXML private TextField   txtExpiry;
    @FXML private TextField   txtCvv;

    @FXML private Label summaryPack;
    @FXML private Label summaryAmount;
    @FXML private Label summaryStatus;

    @FXML private Button btnContinue;
    @FXML private Button btnCancel;

    // ══════════════════════ SERVICES ══════════════════════
    private final PackService        packService        = new PackService();
    private final PackServiceUser    packServiceUser    = new PackServiceUser();
    private final InscriptionService inscriptionService = new InscriptionService();
    private final PromoEngineService promoService       = new PromoEngineService();

    // ══════════════════════ ÉTAT ══════════════════════
    private Pack       selectedPack = null;
    private int        nbPersonnes  = 1;
    private BigDecimal currentTotal = BigDecimal.ZERO;
    private List<Pack> allPacks     = new ArrayList<>();
    private UserApp    currentUser  = null;

    private static final String FILTER_ALL = "Tous";

    private static final String SORT_NAME_ASC  = "Nom (A → Z)";
    private static final String SORT_NAME_DESC = "Nom (Z → A)";
    private static final String SORT_PRICE_ASC = "Prix (croissant)";
    private static final String SORT_PRICE_DESC = "Prix (décroissant)";
    private static final String SORT_ACT_ASC  = "Activités (moins → plus)";
    private static final String SORT_ACT_DESC = "Activités (plus → moins)";

    // ══════════════════════ INIT ══════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideForm();
        setupButtons();
        setupPaymentToggle();
        setupSpinner();
        setupValidation();
        setupFilterCombos();
        loadPacks();
        setCurrentUser(Session.getConnectedUser());
    }

    public void setCurrentUser(UserApp user) {
        this.currentUser = user;
        if (user != null) {
            txtLastName.setText(user.getNom()       != null ? user.getNom()       : "");
            txtFirstName.setText(user.getPrenom()   != null ? user.getPrenom()    : "");
            txtEmail.setText(user.getEmail()        != null ? user.getEmail()     : "");
            txtPhone.setText(user.getTelephone()    != null ? user.getTelephone() : "");
        }
    }

    // ══════════════════════ CHARGEMENT PACKS ══════════════════════

    private void loadPacks() {
        Label loading = new Label("⏳  Chargement des packs...");
        loading.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
        packsGrid.add(loading, 0, 0);

        new Thread(() -> {
            try {
                allPacks = packService.getAll();

                // ── LOG DE DÉBOGAGE ──
                System.out.println("✅ Packs chargés depuis la BDD : " + allPacks.size());
                allPacks.forEach(p -> System.out.println(
                        "   → id=" + p.getIdPack() +
                                " | nom=" + p.getNom() +
                                " | statut=" + p.getStatutPack() +
                                " | statutName=" + (p.getStatutPack() != null ? p.getStatutPack().name() : "NULL")
                ));

                Platform.runLater(this::displayPacks);

            } catch (Exception e) {
                LOGGER.severe("Erreur chargement packs : " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    packsGrid.getChildren().clear();
                    Label err = new Label("❌ Erreur : " + e.getMessage());
                    err.setStyle("-fx-font-size: 13px; -fx-text-fill: #EF4444;");
                    packsGrid.add(err, 0, 0);
                });
            }
        }).start();
    }

    private void setupFilterCombos() {
        comboPackType.getItems().setAll(
                FILTER_ALL,
                labelType(TypePack.INDIVIDUEL),
                labelType(TypePack.GROUPE),
                labelType(TypePack.ENTREPRISE),
                labelType(TypePack.LOISIR)
        );
        comboPackStatut.getItems().setAll(
                FILTER_ALL,
                labelStatut(StatutPack.ACTIF),
                labelStatut(StatutPack.INACTIF)
        );
        comboPackSort.getItems().setAll(
                SORT_NAME_ASC, SORT_NAME_DESC, SORT_PRICE_ASC, SORT_PRICE_DESC,
                SORT_ACT_ASC, SORT_ACT_DESC
        );
        comboPackType.getSelectionModel().select(0);
        comboPackStatut.getSelectionModel().select(0);
        comboPackSort.getSelectionModel().select(0);
    }

    @FXML
    private void onApplyPackFilters() {
        displayPacks();
    }

    @FXML
    private void onResetPackFilters() {
        txtSearchPack.setText("");
        comboPackType.getSelectionModel().select(0);
        comboPackStatut.getSelectionModel().select(0);
        comboPackSort.getSelectionModel().select(0);
        displayPacks();
    }

    private String labelType(TypePack t) {
        if (t == null) return "";
        switch (t) {
            case INDIVIDUEL: return "Individuel";
            case GROUPE: return "Groupe";
            case ENTREPRISE: return "Entreprise";
            case LOISIR: return "Loisir";
            default: return t.name();
        }
    }

    private String labelStatut(StatutPack s) {
        if (s == null) return "";
        switch (s) {
            case ACTIF: return "Actif";
            case INACTIF: return "Inactif";
            default: return s.name();
        }
    }

    private TypePack parseTypeFilter(String sel) {
        if (sel == null || FILTER_ALL.equals(sel)) return null;
        for (TypePack t : TypePack.values()) {
            if (labelType(t).equals(sel)) return t;
        }
        return null;
    }

    private StatutPack parseStatutFilter(String sel) {
        if (sel == null || FILTER_ALL.equals(sel)) return null;
        for (StatutPack s : StatutPack.values()) {
            if (labelStatut(s).equals(sel)) return s;
        }
        return null;
    }

    private Comparator<Pack> comparatorForSort() {
        String sel = comboPackSort.getSelectionModel().getSelectedItem();
        if (sel == null) sel = SORT_NAME_ASC;
        Comparator<Pack> byName = Comparator.comparing(
                p -> p.getNom() != null ? p.getNom() : "", String.CASE_INSENSITIVE_ORDER);
        Comparator<Pack> byPrice = Comparator.comparing(
                p -> p.getPrixBase() != null ? p.getPrixBase() : BigDecimal.ZERO);
        Comparator<Pack> byAct = Comparator.comparingInt(Pack::getNbActivitesMax);
        switch (sel) {
            case SORT_NAME_DESC:
                return byName.reversed();
            case SORT_PRICE_ASC:
                return byPrice;
            case SORT_PRICE_DESC:
                return byPrice.reversed();
            case SORT_ACT_ASC:
                return byAct;
            case SORT_ACT_DESC:
                return byAct.reversed();
            case SORT_NAME_ASC:
            default:
                return byName;
        }
    }

    private List<Pack> buildFilteredList() {
        String q = txtSearchPack.getText() == null ? "" : txtSearchPack.getText().trim().toLowerCase(Locale.ROOT);
        TypePack typeFilter = parseTypeFilter(comboPackType.getSelectionModel().getSelectedItem());
        StatutPack statutFilter = parseStatutFilter(comboPackStatut.getSelectionModel().getSelectedItem());
        List<Pack> out = new ArrayList<>();
        for (Pack p : allPacks) {
            if (typeFilter != null && p.getTypePack() != typeFilter) continue;
            if (statutFilter != null && p.getStatutPack() != statutFilter) continue;
            if (!q.isEmpty()) {
                String nom = p.getNom() != null ? p.getNom() : "";
                String blob = (nom + " " + labelType(p.getTypePack()) + " " + labelStatut(p.getStatutPack()))
                        .toLowerCase(Locale.ROOT);
                if (!blob.contains(q)) continue;
            }
            out.add(p);
        }
        return out;
    }

    private boolean isDefaultSort() {
        String s = comboPackSort.getSelectionModel().getSelectedItem();
        return s == null || SORT_NAME_ASC.equals(s);
    }

    private int countActiveFilters() {
        int n = 0;
        if (txtSearchPack != null && !txtSearchPack.getText().trim().isEmpty()) n++;
        String t = comboPackType.getSelectionModel().getSelectedItem();
        if (t != null && !FILTER_ALL.equals(t)) n++;
        String st = comboPackStatut.getSelectionModel().getSelectedItem();
        if (st != null && !FILTER_ALL.equals(st)) n++;
        if (!isDefaultSort()) n++;
        return n;
    }

    private void updateFilterMeta(List<Pack> visible) {
        lblKpiResults.setText(String.valueOf(visible.size()));
        long typeCount = visible.stream().map(Pack::getTypePack).filter(Objects::nonNull).distinct().count();
        lblKpiTypes.setText(String.valueOf(typeCount));
        lblKpiFilters.setText(String.valueOf(countActiveFilters()));

        StringBuilder sb = new StringBuilder("Intersection actuelle · ");
        List<String> parts = new ArrayList<>();
        String q = txtSearchPack.getText() == null ? "" : txtSearchPack.getText().trim();
        if (!q.isEmpty()) parts.add("recherche « " + q + " »");
        String ts = comboPackType.getSelectionModel().getSelectedItem();
        if (ts != null && !FILTER_ALL.equals(ts)) parts.add("type " + ts);
        String ss = comboPackStatut.getSelectionModel().getSelectedItem();
        if (ss != null && !FILTER_ALL.equals(ss)) parts.add("statut " + ss.toLowerCase(Locale.ROOT));
        String sort = comboPackSort.getSelectionModel().getSelectedItem();
        if (sort != null && !isDefaultSort()) parts.add("tri : " + sort);
        if (parts.isEmpty()) {
            sb.append("aucun filtre appliqué (tri par défaut).");
        } else {
            sb.append(String.join(" · ", parts)).append(".");
        }
        lblPackFilterIntersection.setText(sb.toString());
    }

    private void displayPacks() {
        packsGrid.getChildren().clear();

        if (allPacks.isEmpty()) {
            lblKpiResults.setText("0");
            lblKpiTypes.setText("0");
            lblKpiFilters.setText(String.valueOf(countActiveFilters()));
            lblPackFilterIntersection.setText("Intersection actuelle · aucun pack en base.");
            Label none = new Label("Aucun pack disponible pour le moment.");
            none.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
            packsGrid.add(none, 0, 0);
            return;
        }

        List<Pack> visible = buildFilteredList();
        visible.sort(comparatorForSort());
        updateFilterMeta(visible);

        if (visible.isEmpty()) {
            Label none = new Label("Aucun pack ne correspond à ces filtres.");
            none.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
            packsGrid.add(none, 0, 0);
            if (selectedPack != null) {
                hideForm();
                selectedPack = null;
            }
            return;
        }

        if (selectedPack != null && visible.stream().noneMatch(p -> p.getIdPack() == selectedPack.getIdPack())) {
            hideForm();
            selectedPack = null;
        }

        int col = 0, row = 0;
        for (Pack pack : visible) {
            VBox card = createPackCard(pack);
            packsGrid.add(card, col, row);
            if (selectedPack != null && selectedPack.getIdPack() == pack.getIdPack()) {
                setCardSelected(card, true);
            }
            col++;
            if (col > 2) { col = 0; row++; }
        }
    }

    private void setCardSelected(VBox card, boolean selected) {
        card.getStyleClass().remove("pi-pack-card-selected");
        if (selected) {
            if (!card.getStyleClass().contains("pi-pack-card")) {
                card.getStyleClass().add("pi-pack-card");
            }
            card.getStyleClass().add("pi-pack-card-selected");
        } else if (!card.getStyleClass().contains("pi-pack-card")) {
            card.getStyleClass().add("pi-pack-card");
        }
    }

    // ══════════════════════ CARTE PACK ══════════════════════

    private VBox createPackCard(Pack pack) {
        VBox card = new VBox(12);
        card.setUserData(pack);
        card.setPrefWidth(Double.MAX_VALUE);
        card.getStyleClass().setAll("pi-pack-card");

        String badgeText = labelType(pack.getTypePack());
        if (badgeText.isEmpty()) badgeText = "Pack";
        Label badge = new Label(badgeText.toUpperCase(Locale.ROOT));
        badge.getStyleClass().add("pi-pack-badge");

        Label nom = new Label(pack.getNom());
        nom.getStyleClass().add("pi-pack-name");
        nom.setWrapText(true);

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.getStyleClass().add("pi-pack-sep");

        BigDecimal base = pack.getPrixBase() != null ? pack.getPrixBase() : BigDecimal.ZERO;
        BigDecimal red = pack.getReduction() != null ? pack.getReduction() : BigDecimal.ZERO;

        HBox prixBox = new HBox(8);
        prixBox.setAlignment(Pos.CENTER_LEFT);
        Label prixLabel = new Label("TND " + String.format(Locale.ROOT, "%.2f", base));
        prixLabel.getStyleClass().add("pi-pack-price");
        Label redLabel = new Label("− " + String.format(Locale.ROOT, "%.2f", red) + " TND");
        redLabel.getStyleClass().add("pi-pack-discount");
        prixBox.getChildren().addAll(prixLabel, redLabel);

        Label actLabel = new Label("↗  " + pack.getNbActivitesMax() + " activités max");
        actLabel.getStyleClass().add("pi-pack-meta");

        BigDecimal net = base.subtract(red);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
        Label netLabel = new Label("Prix net : TND " + String.format(Locale.ROOT, "%.2f", net) + " / pers.");
        netLabel.getStyleClass().add("pi-pack-net");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnInscrire = new Button("S'inscrire au pack");
        btnInscrire.getStyleClass().add("pi-pack-cta");
        btnInscrire.setMaxWidth(Double.MAX_VALUE);
        btnInscrire.setOnAction(e -> selectPack(pack, card));

        card.getChildren().addAll(badge, nom, sep, prixBox, actLabel, netLabel, spacer, btnInscrire);
        return card;
    }

    // ══════════════════════ SÉLECTION PACK ══════════════════════

    private void selectPack(Pack pack, VBox card) {
        selectedPack = pack;
        packsGrid.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                setCardSelected((VBox) node, false);
            }
        });
        setCardSelected(card, true);
        updateFormDetails(pack);
        showFormAnimated();
        loadActivities(pack);
    }

    // ══════════════════════ DÉTAILS FORMULAIRE ══════════════════════

    private void updateFormDetails(Pack pack) {
        lblPackName.setText(pack.getNom());
        lblPackDescription.setText(
                "Pack " + labelType(pack.getTypePack()) +
                        " — " + pack.getNbActivitesMax() + " activités max"
        );
        lblPriceBase.setText("TND " + String.format("%.2f", pack.getPrixBase()));
        lblReduction.setText("-" + String.format("%.2f", pack.getReduction()) + " TND");
        recalcTotal();
    }

    private void recalcTotal() {
        if (selectedPack == null) return;
        BigDecimal net = selectedPack.getPrixBase().subtract(selectedPack.getReduction());
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
        currentTotal = net.multiply(new BigDecimal(nbPersonnes));
        lblPriceTotal.setText("TND " + String.format("%.2f", currentTotal));
        updateSummary();
    }

    // ══════════════════════ ACTIVITÉS ══════════════════════

    private void loadActivities(Pack pack) {
        activitiesList.getChildren().clear();
        Label loading = new Label("Chargement des activités...");
        loading.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        activitiesList.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<Activite> acts = packServiceUser.getActivitesByPack(pack.getIdPack());
                Platform.runLater(() -> {
                    activitiesList.getChildren().clear();
                    if (acts.isEmpty()) {
                        Label none = new Label("Aucune activité disponible.");
                        none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
                        activitiesList.getChildren().add(none);
                    } else {
                        acts.forEach(a -> activitiesList.getChildren().add(createActivityRow(a)));
                    }
                });
            } catch (Exception e) {
                LOGGER.severe("Erreur activités : " + e.getMessage());
                Platform.runLater(() -> activitiesList.getChildren().clear());
            }
        }).start();
    }

    private HBox createActivityRow(Activite a) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 6; -fx-padding: 8 12;");

        String categorieStr = (a.getCategorieAct() != null) ? a.getCategorieAct().name() : "";
        String niveauStr    = (a.getNiveauAct()    != null) ? a.getNiveauAct().name()    : "";

        Label check = new Label("✓");
        check.setStyle("-fx-text-fill: #10B981; -fx-font-weight: 700;");

        VBox info = new VBox(1);
        Label name = new Label(a.getNom());
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");
        Label sub = new Label(categorieStr + "  ·  " + niveauStr);
        sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        info.getChildren().addAll(name, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label prix = new Label("TND " + String.format("%.2f", a.getPrix()));
        prix.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #10B981;");

        row.getChildren().addAll(check, info, spacer, prix);
        return row;
    }

    // ══════════════════════ INSCRIPTION ══════════════════════

    private void processInscription() {
        if (!validateForm()) return;

        Inscription insc = new Inscription();
        insc.setIdPack(selectedPack.getIdPack());
        insc.setNomPack(selectedPack.getNom());
        insc.setDateInscription(LocalDateTime.now());
        insc.setStatutInscr("EN_ATTENTE");
        insc.setMontantTotal(currentTotal);
        insc.setNomUser(txtFirstName.getText().trim() + " " + txtLastName.getText().trim());
        if (currentUser != null) insc.setIdUser(currentUser.getIdUser());

        if (rbCardPayment.isSelected())        insc.setPaymentGateway("CARD");
        else if (rbMobilePayment.isSelected()) insc.setPaymentGateway("MOBILE");
        else if (rbBankTransfer.isSelected())  insc.setPaymentGateway("BANK");
        insc.setPaymentStatus("PENDING");

        btnContinue.setDisable(true);
        btnContinue.setText("⏳  Traitement...");

        new Thread(() -> {
            try {
                int inscriptionId = inscriptionService.add(insc, selectedPack);
                Platform.runLater(() -> {
                    btnContinue.setDisable(false);
                    btnContinue.setText("✅  Confirmer l'inscription");
                    showAlert(Alert.AlertType.INFORMATION,
                            "Inscription réussie !",
                            "ID Inscription : #" + inscriptionId + "\n" +
                                    "Pack       : " + selectedPack.getNom() + "\n" +
                                    "Personnes  : " + nbPersonnes + "\n" +
                                    "Montant    : TND " + String.format("%.2f", currentTotal) + "\n" +
                                    "Statut     : EN_ATTENTE"
                    );
                    hideForm();
                    resetCardStyles();
                    selectedPack = null;
                });
            } catch (Exception e) {
                LOGGER.severe("Erreur inscription : " + e.getMessage());
                Platform.runLater(() -> {
                    btnContinue.setDisable(false);
                    btnContinue.setText("✅  Confirmer l'inscription");
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible de créer l'inscription :\n" + e.getMessage());
                });
            }
        }).start();
    }

    // ══════════════════════ CODE PROMO ══════════════════════

    private void validatePromoCode(String code) {
        new Thread(() -> {
            try {
                boolean ok = promoService.validateAndApply(code, null, 0, LocalDate.now()).valid;
                Platform.runLater(() -> {
                    if (ok) showAlert(Alert.AlertType.INFORMATION, "Code valide !", "Réduction appliquée.");
                    else    showAlert(Alert.AlertType.WARNING, "Code invalide", "Code promo non valide.");
                });
            } catch (Exception e) { LOGGER.warning("Promo : " + e.getMessage()); }
        }).start();
    }

    // ══════════════════════ VALIDATION ══════════════════════

    private boolean validateForm() {
        List<String> errors = new ArrayList<>();
        if (txtFirstName.getText().trim().isEmpty()) errors.add("• Prénom requis.");
        if (txtLastName.getText().trim().isEmpty())  errors.add("• Nom requis.");
        if (txtEmail.getText().trim().isEmpty())     errors.add("• Email requis.");
        else if (!txtEmail.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$"))
            errors.add("• Email invalide.");
        if (txtPhone.getText().trim().isEmpty())     errors.add("• Téléphone requis.");
        if (selectedPack == null)                    errors.add("• Aucun pack sélectionné.");
        if (!errors.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Champs manquants", String.join("\n", errors));
            return false;
        }
        return true;
    }

    // ══════════════════════ SETUP ══════════════════════

    private void setupButtons() {
        btnBack.setOnAction(e -> goBack());
        btnContinue.setOnAction(e -> processInscription());
        btnCancel.setOnAction(e -> {
            hideForm();
            resetCardStyles();
            selectedPack = null;
        });
        txtPromoCode.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.trim().length() >= 4)
                validatePromoCode(val.trim());
        });
    }

    private void setupPaymentToggle() {
        ToggleGroup group = new ToggleGroup();
        rbCardPayment.setToggleGroup(group);
        rbMobilePayment.setToggleGroup(group);
        rbBankTransfer.setToggleGroup(group);
        rbCardPayment.selectedProperty().addListener((obs, old, val) -> {
            cardPaymentPanel.setVisible(val);
            cardPaymentPanel.setManaged(val);
        });
        rbCardPayment.setSelected(true);
        cardPaymentPanel.setVisible(true);
        cardPaymentPanel.setManaged(true);
    }

    private void setupSpinner() {
        spnbPersonnes.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1)
        );
        spnbPersonnes.valueProperty().addListener((obs, old, val) -> {
            nbPersonnes = val;
            recalcTotal();
        });
    }

    private void setupValidation() {
        Pattern emailPat = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
        txtEmail.textProperty().addListener((obs, old, val) ->
                txtEmail.setStyle("-fx-border-color: " +
                        (!val.isEmpty() && !emailPat.matcher(val).matches() ? "#EF4444" : "#E2E8F0") +
                        "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 9 12; -fx-font-size: 13px;")
        );
        txtPhone.textProperty().addListener((obs, old, val) ->
                txtPhone.setStyle("-fx-border-color: " +
                        (!val.isEmpty() && !val.matches("^[0-9+\\s-]{8,}$") ? "#EF4444" : "#E2E8F0") +
                        "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 9 12; -fx-font-size: 13px;")
        );
    }

    // ══════════════════════ RÉSUMÉ ══════════════════════

    private void updateSummary() {
        if (selectedPack != null) {
            summaryPack.setText(selectedPack.getNom() + "  ×" + nbPersonnes);
            summaryAmount.setText("TND " + String.format("%.2f", currentTotal));
            summaryStatus.setText("EN_ATTENTE");
        }
    }

    // ══════════════════════ UI HELPERS ══════════════════════

    private void showFormAnimated() {
        detailsSection.setVisible(true);
        detailsSection.setManaged(true);
        detailsSection.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), detailsSection);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), detailsSection);
        tt.setFromY(20); tt.setToY(0); tt.play();
    }

    private void hideForm() {
        detailsSection.setVisible(false);
        detailsSection.setManaged(false);
    }

    private void resetCardStyles() {
        packsGrid.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                setCardSelected((VBox) node, false);
            }
        });
    }

    private void goBack() {
        try { SceneUtils.switchScene("Menu.fxml"); }
        catch (Exception e) { LOGGER.severe("Navigation : " + e.getMessage()); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}