package GUI;

import Entities.Reclamation;
import Entities.Session;
import Services.ReclamationService;
import enums.StatutReclamation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class UserReclamationController {
    @FXML private Label lblSentiment;
    @FXML private ProgressBar progressChar;
    @FXML private Label lblCharCount;
    // --- Components FXML ---
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea txtContenu;
    @FXML private Button btnEnvoyer;
    @FXML private ListView<Reclamation> listMyRecs;
    @FXML private Label lblAutoReply;
    private ReclamationService rs = new ReclamationService();
    private int currentUserId = Session.getConnectedUser().getIdUser();
    @FXML private TextField txtSearch;
    private javafx.collections.transformation.FilteredList<Reclamation> filteredData;
    @FXML
    public void initialize() {
        if (comboType != null) {
            comboType.setItems(FXCollections.observableArrayList("TECHNIQUE", "SERVICE", "PAIEMENT", "AUTRE"));
        }

        if (txtContenu != null) {
            txtContenu.textProperty().addListener((observable, oldValue, newValue) -> {
                updateSmartFeatures(newValue);
            });
        }

        if (listMyRecs != null) {
            setupListView();
            loadData();
        }
    }




    private void updateSmartFeatures(String text) {
        if (text == null) text = "";
        int length = text.length();
        String input = text.toLowerCase();
        // 1. Counter (1000 kima fel label mte3ek)
        lblCharCount.setText(length + "/1000");

        // 2. Progress Calculation (Forci el double bech yet7arek el khatt)
        double progressValue = (double) length / 1000.0;
        progressChar.setProgress(progressValue);
// Dans updateSmartFeatures
        if (length > 200) {
            lblCharCount.setStyle("-fx-text-fill: #ef4444;"); // Rouge alerte
        } else {
            lblCharCount.setStyle("-fx-text-fill: #94a3b8;");
        }

// Désactiver le bouton si vide
        btnEnvoyer.setDisable(text.trim().isEmpty() || comboType.getValue() == null);
        // 3. Force Color Change (Bel -fx-accent)
        if (length > 200) {

            progressChar.setStyle("-fx-accent: #ef4444; -fx-control-inner-background: #fee2e2;");
        } else {
            // A5dher EcoAdventure kenou 3adi
            progressChar.setStyle("-fx-accent: #143D30; -fx-control-inner-background: #f1f5f9;");
        }
        // 1. Définir l'emoji et la couleur
        String emoji = "😐";
        String colorHex = "#e2e8f0"; // Gris par défaut

        if (input.contains("merci") || input.contains("top") || input.contains("super")) {
            emoji = "😎";
            colorHex = "#4ade80"; // Vert
        } else if (input.contains("nul") || input.contains("faddit") || input.contains("mauvais")) {
            emoji = "😡";
            colorHex = "#f87171"; // Rouge
        } else if (input.contains("urgent") || input.contains("vite")) {
            emoji = "🆘";
            colorHex = "#fbbf24"; // Gold
        }

        // 2. Appliquer l'emoji
        lblSentiment.setText(emoji);

        // 3. Appliquer le Glow dynamiquement
        javafx.scene.paint.Color color = javafx.scene.paint.Color.web(colorHex);

        // On crée un nouvel effet à chaque fois pour éviter les erreurs de cast
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setRadius(25);
        glow.setSpread(0.15);
        glow.setColor(color);

        lblSentiment.setEffect(glow);

        // 4. Animation de pulsation
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(250), lblSentiment);
        st.setFromX(0.8); st.setFromY(0.8);
        st.setToX(1.0);   st.setToY(1.0);
        st.play();
        handleAutoReply(text);
    }

    // 1. Déclarer la liste des connaissances (FAQ)
    private final java.util.Map<String, String> autoAnswers = new java.util.HashMap<>() {{
        put("mot de passe", "🔑 Pour changer votre mot de passe, allez dans 'Mon Profil' > 'Sécurité'.");
        put("paiement", "💳 Les paiements sont sécurisés via Stripe. Nous acceptons Visa et Mastercard.");
        put("remboursement", "💰 Les remboursements prennent entre 5 et 10 jours ouvrables.");
        put("coach", "🏋️ Vous pouvez voir la disponibilité des coachs dans la section 'Séances'.");
        put("planning", "📅 Le planning est mis à jour chaque dimanche à 20h00.");
        put("urgent", "🚨 Pour les cas urgents, notre équipe vous répondra en moins de 2 heures.");
    }};

    private void handleAutoReply(String text) {
        if (text == null || text.length() < 3) {
            lblAutoReply.setVisible(false);
            lblAutoReply.setManaged(false);
            return;
        }

        String input = text.toLowerCase();
        boolean found = false;

        // 2. Recherche par mot-clé
        for (java.util.Map.Entry<String, String> entry : autoAnswers.entrySet()) {
            if (input.contains(entry.getKey())) {
                lblAutoReply.setText(entry.getValue());
                lblAutoReply.setVisible(true);
                lblAutoReply.setManaged(true);

                // Faza sghira: Animation FadeIn
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), lblAutoReply);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.play();

                found = true;
                break; // On s'arrête au premier mot-clé trouvé
            }
        }

        if (!found) {
            lblAutoReply.setVisible(false);
            lblAutoReply.setManaged(false);
        }
    }
    private void setupListView() {
        listMyRecs.setCellFactory(param -> new ListCell<Reclamation>() {
            @Override
            protected void updateItem(Reclamation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    // 1. El Card el kbir
                    VBox card = new VBox(15);
                    card.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-background-radius: 15; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 15, 0, 0, 5); " +
                            "-fx-border-color: #f1f5f9; -fx-border-width: 1; -fx-border-radius: 15;");

                    // 2. Header (Type + Timeline)
                    HBox header = new HBox();
                    header.setAlignment(Pos.CENTER_LEFT);

                    Label type = new Label(item.getType());
                    type.setStyle("-fx-font-weight: 900; -fx-font-size: 18px; -fx-text-fill: #143D30; -fx-letter-spacing: 1;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Node timeline = createTimeline(item.getStatut());
                    header.getChildren().addAll(type, spacer, timeline);

                    // 3. Contenu (Text)
                    Label contenu = new Label(item.getContenu());
                    contenu.setWrapText(true);
                    contenu.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-line-spacing: 5;");
                    contenu.setMaxWidth(850); // Bech mayfoutch el card

                    // 4. Reponse (ken fama)
                    VBox responseContainer = new VBox();
                    if (item.getReponse() != null && !item.getReponse().isEmpty()) {
                        Label repTitle = new Label("Réponse de l'admin :");
                        repTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #0369A1;");

                        Label repBody = new Label(item.getReponse());
                        repBody.setStyle("-fx-text-fill: #334155; -fx-background-color: #f0f9ff; -fx-padding: 12; -fx-background-radius: 10; -fx-wrap-text: true;");
                        responseContainer.getChildren().addAll(repTitle, repBody);
                        responseContainer.setSpacing(5);
                    }

                    // 5. Buttons (Modif/Supp)
                    HBox actions = new HBox(12);
                    actions.setAlignment(Pos.CENTER_RIGHT);

                    if (item.getStatut().toString().equals("EN_ATTENTE")) {
                        Button btnModif = new Button("Modifier");
                        btnModif.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #143D30; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 5 15; -fx-cursor: hand;");
                        btnModif.setOnAction(e -> handleUpdateFromCard(item));

                        Button btnSupp = new Button("Supprimer");
                        btnSupp.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #e11d48; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 5 15; -fx-cursor: hand;");
                        btnSupp.setOnAction(e -> handleDeleteFromCard(item));

                        actions.getChildren().addAll(btnModif, btnSupp);
                    }

                    // Montage final
                    card.getChildren().addAll(header, contenu);
                    if (!responseContainer.getChildren().isEmpty()) card.getChildren().add(responseContainer);
                    card.getChildren().add(actions);

                    // Animation FadeIn ki t-loadi el list
                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), card);
                    ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();

                    setGraphic(card);
                }
            }
        });
    }

    private void handleDeleteFromCard(Reclamation rec) {
        listMyRecs.getSelectionModel().select(rec);
        handleDelete();
    }

    private void handleUpdateFromCard(Reclamation rec) {
        listMyRecs.getSelectionModel().select(rec);
        handleUpdate(null);
    }
    private void loadData() {
        Task<List<Reclamation>> loadTask = new Task<>() {
            @Override
            protected List<Reclamation> call() throws Exception {
                return rs.afficherParUser(currentUserId);
            }
        };

        loadTask.setOnSucceeded(e -> {
            if (listMyRecs != null) {
                // 1. N-7ottou el data l-kol f ObservableList
                javafx.collections.ObservableList<Reclamation> masterData = FXCollections.observableArrayList(loadTask.getValue());

                // 2. N-sna3ou FilteredList m-rabta bel masterData
                filteredData = new javafx.collections.transformation.FilteredList<>(masterData, p -> true);

                // 3. N-rabtou el TextField bel FilteredList
                txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                    filteredData.setPredicate(rec -> {
                        if (newValue == null || newValue.isEmpty()) return true;

                        String lowerCaseFilter = newValue.toLowerCase();
                        // Recherche fil contenu walla fil type
                        if (rec.getContenu().toLowerCase().contains(lowerCaseFilter)) return true;
                        if (rec.getType().toLowerCase().contains(lowerCaseFilter)) return true;

                        return false; // ma l9ach
                    });
                });

                // 4. N-affichou el filteredData fil ListView
                listMyRecs.setItems(filteredData);
            }
        });

        new Thread(loadTask).start();
    }
    @FXML
    void handleEnvoyer(ActionEvent event) {
        String typeValue = (comboType != null) ? comboType.getValue() : null;
        String contenuValue = (txtContenu != null) ? txtContenu.getText() : "";

        if (typeValue == null || contenuValue.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir les champs !").show();
            return;
        }

        Reclamation r = new Reclamation();
        r.setType(typeValue);
        r.setContenu(contenuValue);
        r.setStatut(StatutReclamation.EN_ATTENTE);
        r.setIdUser(currentUserId);

        // نثبتو قبل ما نبعثو
        System.out.println("Tentative d'envoi de la réclamation...");

        Task<Void> sendTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // هوني المشكلة: إذا الـ MySQL مسكر، باش يخرج Erreur
                rs.ajouter(r);
                return null;
            }
        };

        sendTask.setOnSucceeded(e -> {
            System.out.println("Réclamation envoyée avec succès !");
            Platform.runLater(() -> switchToList(event));
        });

        sendTask.setOnFailed(e -> {
            // هوني وين يظهر مساج الـ "Connection Closed"
            Throwable ex = sendTask.getException();
            ex.printStackTrace(); // شوف الـ Console متاع الـ IDE
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur Database: " + ex.getMessage());
                alert.show();
            });
        });

        new Thread(sendTask).start();
    }
    @FXML
    void handleDelete() {
        Reclamation sel = listMyRecs.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        try {
            rs.supprimer(sel.getIdReclamation());
            loadData();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleUpdate(ActionEvent event) {
        Reclamation sel = listMyRecs.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modification");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/GUI/style_reclamation.css").toExternalForm());

        VBox rootContainer = new VBox(20);
        rootContainer.getStyleClass().add("dialog-content-box");
        rootContainer.setPrefWidth(450);

        // Titre de la Popup
        Label header = new Label("Modifier la réclamation");
        header.getStyleClass().add("label-header");

        // Formulaire en Grid (Kima el Event Form)
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        Label lblType = new Label("Catégorie:");
        lblType.getStyleClass().add("label-field");
        ComboBox<String> editType = new ComboBox<>(FXCollections.observableArrayList("TECHNIQUE", "SERVICE", "PAIEMENT", "AUTRE"));
        editType.setValue(sel.getType());
        editType.getStyleClass().add("combo-box");
        editType.setPrefWidth(250);

        Label lblDesc = new Label("Description:");
        lblDesc.getStyleClass().add("label-field");
        TextArea editContenu = new TextArea(sel.getContenu());
        editContenu.setWrapText(true);
        editContenu.setPrefHeight(100);
        editContenu.getStyleClass().add("text-area");
        editContenu.setPrefWidth(250);

        grid.add(lblType, 0, 0);
        grid.add(editType, 1, 0);
        grid.add(lblDesc, 0, 1);
        grid.add(editContenu, 1, 1);

        rootContainer.getChildren().addAll(header, grid);
        dialog.getDialogPane().setContent(rootContainer);

        // Boutons
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelType, saveType);

        Button btnSave = (Button) dialog.getDialogPane().lookupButton(saveType);
        btnSave.getStyleClass().add("btn-save-modern");

        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(cancelType);
        btnCancel.getStyleClass().add("btn-cancel-modern");

        dialog.showAndWait().ifPresent(res -> {
            if (res == saveType) {
                try {
                    sel.setType(editType.getValue());
                    sel.setContenu(editContenu.getText());
                    rs.modifier(sel);
                    loadData();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }
    private Node createTimeline(StatutReclamation currentStatut) {
        HBox timeline = new HBox(5);
        timeline.setAlignment(Pos.CENTER_LEFT);
        timeline.setStyle("-fx-padding: 5 0;");

        // 1. Definir les étapes visuelles
        String[] etapes = {"Soumise", "En cours", "Terminée"};
        int activeStep = 0;

        // 2. Mapping intelligent du statut vers l'index de la timeline
        if (currentStatut == StatutReclamation.EN_ATTENTE) {
            activeStep = 0; // Loula tech3el
        } else if (currentStatut == StatutReclamation.EN_COURS) {
            activeStep = 1; // Ethanya tech3el (Admin bda ye5dem)
        } else if (currentStatut == StatutReclamation.TRAITEE || currentStatut == StatutReclamation.REJETEE) {
            activeStep = 2; // El le5ra tech3el
        }

        // 3. Boucle pour construire les cercles et les lignes
        for (int i = 0; i < etapes.length; i++) {
            // --- Création du Cercle ---
            Label circle = new Label(String.valueOf(i + 1));
            circle.setPrefSize(24, 24);
            circle.setMinSize(24, 24);
            circle.setAlignment(Pos.CENTER);

            // --- Logique des Couleurs ---
            if (i < activeStep) {
                // Étape déjà terminée (Vert "Success")
                circle.setStyle("-fx-background-color: #143D30; -fx-text-fill: white; -fx-background-radius: 50; -fx-font-size: 10px; -fx-font-weight: bold;");
            } else if (i == activeStep) {
                // Étape actuelle (Couleur dynamique selon le statut)
                String currentColor = "#F37021"; // Orange par défaut (Soumise)
                if (currentStatut == StatutReclamation.EN_COURS) currentColor = "#0369A1"; // Azreq (En cours)
                else if (currentStatut == StatutReclamation.TRAITEE) currentColor = "#28a745"; // A5dher (Terminée)
                else if (currentStatut == StatutReclamation.REJETEE) currentColor = "#dc3545"; // A7mer (Refusée)

                circle.setStyle("-fx-background-color: " + currentColor + "; -fx-text-fill: white; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 11px;");
                // Effet de lueur pour l'étape active
                circle.setEffect(new javafx.scene.effect.DropShadow(5, javafx.scene.paint.Color.web(currentColor, 0.4)));
            } else {
                // Étape future (Gris clair)
                circle.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #94a3b8; -fx-background-radius: 50; -fx-font-size: 10px;");
            }

            // --- Label de l'étape ---
            Label labelEtape = new Label(etapes[i]);
            labelEtape.setStyle("-fx-font-size: 10px; -fx-font-weight: " + (i == activeStep ? "bold" : "normal") +
                    "; -fx-text-fill: " + (i <= activeStep ? "#1e293b" : "#94a3b8") + ";");

            // Ajouter au HBox
            timeline.getChildren().addAll(circle, labelEtape);

            // --- Création de la Ligne de connexion ---
            if (i < etapes.length - 1) {
                Region line = new Region();
                line.setPrefHeight(2);
                line.setMinWidth(25);
                // La ligne devient foncée si l'étape suivante est atteinte ou en cours
                String lineColor = (i < activeStep) ? "#143D30" : "#e2e8f0";
                line.setStyle("-fx-background-color: " + lineColor + "; -fx-background-radius: 2;");
                HBox.setMargin(line, new javafx.geometry.Insets(0, 5, 0, 5));
                timeline.getChildren().add(line);
            }
        }

        return timeline;
    }
    @FXML void switchToList(ActionEvent event) { navigateTo("/GUI/ListReclamation.fxml", event); }
    @FXML void switchToForm(ActionEvent event) { navigateTo("/GUI/AddReclamation.fxml", event); }

    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            BorderPane mainPane = (BorderPane) ((Node) event.getSource()).getScene().lookup("#mainPaneUser");
            if (mainPane != null) {
                Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
                mainPane.setCenter(page);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}