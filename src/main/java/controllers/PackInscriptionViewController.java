package controllers;

import Entities.Pack;
import Entities.Activite;
import Entities.Inscription;
import Entities.Session;
import Entities.UserApp;
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

    // ══════════════════════ INIT ══════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideForm();
        setupButtons();
        setupPaymentToggle();
        setupSpinner();
        setupValidation();
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

    private void displayPacks() {
        packsGrid.getChildren().clear();
        int col = 0, row = 0;

        // FIX : SUPPRESSION DU FILTRE SUR statutPack
        // Cause du bug : la BDD contient 'Actif' mais l'enum mappait vers null silencieusement
        // → le filtre éliminait tous les packs
        // Solution : afficher TOUS les packs retournés par la BDD
        if (allPacks.isEmpty()) {
            Label none = new Label("Aucun pack disponible pour le moment.");
            none.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
            packsGrid.add(none, 0, 0);
            return;
        }

        for (Pack pack : allPacks) {
            packsGrid.add(createPackCard(pack), col, row);
            col++;
            if (col > 2) { col = 0; row++; }
        }
    }

    // ══════════════════════ CARTE PACK ══════════════════════

    private VBox createPackCard(Pack pack) {
        VBox card = new VBox(12);
        card.setPrefWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14;" +
                        "-fx-border-color: #E2E8F0; -fx-border-width: 1.5; -fx-border-radius: 14;" +
                        "-fx-padding: 18; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);"
        );

        // Badge type
        String typeStr = (pack.getTypePack() != null) ? pack.getTypePack().name() : "Pack";
        Label badge = new Label(typeStr.toUpperCase());
        badge.setStyle(
                "-fx-background-color: #ECFDF5; -fx-text-fill: #059669;" +
                        "-fx-font-size: 10px; -fx-font-weight: 700;" +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 20;"
        );

        // Nom
        Label nom = new Label(pack.getNom());
        nom.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        nom.setWrapText(true);

        Separator sep = new Separator();

        // Prix + réduction
        HBox prixBox = new HBox(8);
        prixBox.setAlignment(Pos.CENTER_LEFT);
        Label prixLabel = new Label("TND " + String.format("%.2f", pack.getPrixBase()));
        prixLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #10B981;");
        Label redLabel = new Label("− " + String.format("%.2f", pack.getReduction()) + " TND");
        redLabel.setStyle(
                "-fx-background-color: #FEF9C3; -fx-text-fill: #92400E;" +
                        "-fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-padding: 3 8; -fx-background-radius: 6;"
        );
        prixBox.getChildren().addAll(prixLabel, redLabel);

        // Activités max
        Label actLabel = new Label("↗  " + pack.getNbActivitesMax() + " activités max");
        actLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        // Prix net
        BigDecimal net = pack.getPrixBase().subtract(pack.getReduction());
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
        Label netLabel = new Label("Prix net : TND " + String.format("%.2f", net) + " / pers.");
        netLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bouton S'inscrire
        Button btnInscrire = new Button("🎒  S'inscrire");
        btnInscrire.setMaxWidth(Double.MAX_VALUE);
        btnInscrire.setStyle(
                "-fx-background-color: #143D30; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700;" +
                        "-fx-background-radius: 9; -fx-padding: 10 0; -fx-cursor: hand;"
        );
        btnInscrire.setOnMouseEntered(e -> btnInscrire.setStyle(
                "-fx-background-color: #10B981; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700;" +
                        "-fx-background-radius: 9; -fx-padding: 10 0; -fx-cursor: hand;"
        ));
        btnInscrire.setOnMouseExited(e -> {
            String bg = (selectedPack != null && selectedPack.getIdPack() == pack.getIdPack())
                    ? "#10B981" : "#143D30";
            btnInscrire.setStyle(
                    "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
                            "-fx-font-size: 13px; -fx-font-weight: 700;" +
                            "-fx-background-radius: 9; -fx-padding: 10 0; -fx-cursor: hand;"
            );
        });
        btnInscrire.setOnAction(e -> selectPack(pack, card));

        card.getChildren().addAll(badge, nom, sep, prixBox, actLabel, netLabel, spacer, btnInscrire);
        return card;
    }

    // ══════════════════════ SÉLECTION PACK ══════════════════════

    private void selectPack(Pack pack, VBox card) {
        selectedPack = pack;

        // Reset toutes les cartes
        packsGrid.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 14;" +
                                "-fx-border-color: #E2E8F0; -fx-border-width: 1.5; -fx-border-radius: 14;" +
                                "-fx-padding: 18; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);"
                );
            }
        });

        // Surbrillance
        card.setStyle(
                "-fx-background-color: #F0FDF4; -fx-background-radius: 14;" +
                        "-fx-border-color: #10B981; -fx-border-width: 2.5; -fx-border-radius: 14;" +
                        "-fx-padding: 18; -fx-effect: dropshadow(gaussian,rgba(16,185,129,0.25),14,0,0,4);"
        );

        updateFormDetails(pack);
        showFormAnimated();
        loadActivities(pack);
    }

    // ══════════════════════ DÉTAILS FORMULAIRE ══════════════════════

    private void updateFormDetails(Pack pack) {
        lblPackName.setText(pack.getNom());
        lblPackDescription.setText(
                "Pack " + (pack.getTypePack() != null ? pack.getTypePack().name() : "") +
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
                node.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 14;" +
                                "-fx-border-color: #E2E8F0; -fx-border-width: 1.5; -fx-border-radius: 14;" +
                                "-fx-padding: 18; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);"
                );
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