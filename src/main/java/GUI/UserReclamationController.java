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

        // 3. Force Color Change (Bel -fx-accent)
        if (length > 200) {
            // A7mer ken fat el 900
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
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox card = new VBox(10);
                    card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-background-radius: 12; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-border-color: #e2e8f0;");

                    HBox header = new HBox();
                    Label type = new Label(item.getType());
                    type.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #143D30;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Node timeline = createTimeline(item.getStatut());
                    header.getChildren().addAll(type, spacer, timeline);

                    Label contenu = new Label(item.getContenu());
                    contenu.setWrapText(true);
                    contenu.setStyle("-fx-text-fill: #475569;");

                    HBox actions = new HBox(10);
                    actions.setAlignment(Pos.CENTER_RIGHT);

                    Button btnModif = new Button("✏️");
                    btnModif.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnModif.setOnAction(event -> handleUpdateFromCard(item)); // ميثود جديدة

                    Button btnSupp = new Button("🗑");
                    btnSupp.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnSupp.setOnAction(event -> handleDeleteFromCard(item)); // ميثود جديدة

                    if (item.getStatut().toString().equals("EN_ATTENTE")) {
                        actions.getChildren().addAll(btnModif, btnSupp);
                    }

                    card.getChildren().addAll(header, contenu);

                    if (item.getReponse() != null && !item.getReponse().isEmpty()) {
                        Label rep = new Label("💬 " + item.getReponse());
                        rep.setStyle("-fx-font-style: italic; -fx-text-fill: #64748b; -fx-background-color: #f8fafc; -fx-padding: 8; -fx-background-radius: 5;");
                        card.getChildren().add(rep);
                    }

                    card.getChildren().add(actions);
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
                listMyRecs.setItems(FXCollections.observableArrayList(loadTask.getValue()));
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
        dialog.setTitle("Modifier");
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");
        ComboBox<String> editType = new ComboBox<>(FXCollections.observableArrayList("TECHNIQUE", "SERVICE", "PAIEMENT", "AUTRE"));
        editType.setValue(sel.getType());
        TextArea editContenu = new TextArea(sel.getContenu());
        content.getChildren().addAll(new Label("Type:"), editType, new Label("Description:"), editContenu);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
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