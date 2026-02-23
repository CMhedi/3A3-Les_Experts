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

    // --- Components FXML ---
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea txtContenu;
    @FXML private Button btnEnvoyer;
    @FXML private ListView<Reclamation> listMyRecs;

    private ReclamationService rs = new ReclamationService();
    private int currentUserId = Session.getConnectedUser().getIdUser();

    @FXML
    public void initialize() {
        // 1. Setup Form
        if (comboType != null) {
            comboType.setItems(FXCollections.observableArrayList("TECHNIQUE", "SERVICE", "PAIEMENT", "AUTRE"));
        }

        // 2. Setup List
        if (listMyRecs != null) {
            setupListView();
            loadData();
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
                    // 1. الحاوية الكبيرة (Card)
                    VBox card = new VBox(10);
                    card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-background-radius: 12; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-border-color: #e2e8f0;");

                    // 2. الهيدر (Type + Status)
                    HBox header = new HBox();
                    Label type = new Label(item.getType());
                    type.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #143D30;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label statut = new Label(item.getStatut().toString());
                    String color = item.getStatut().toString().equals("TRAITEE") ? "#143D30" : (item.getStatut().toString().equals("REJETEE") ? "#ef4444" : "#F37021");
                    statut.setStyle("-fx-text-fill: white; -fx-background-color: " + color + "; -fx-padding: 3 10; -fx-background-radius: 15; -fx-font-size: 11px;");

                    header.getChildren().addAll(type, spacer, statut);

                    // 3. المحتوى
                    Label contenu = new Label(item.getContenu());
                    contenu.setWrapText(true);
                    contenu.setStyle("-fx-text-fill: #475569;");

                    // 4. أزرار التحكم (Modifier / Supprimer) داخل الـ Card
                    HBox actions = new HBox(10);
                    actions.setAlignment(Pos.CENTER_RIGHT);

                    Button btnModif = new Button("✏️");
                    btnModif.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnModif.setOnAction(event -> handleUpdateFromCard(item)); // ميثود جديدة

                    Button btnSupp = new Button("🗑");
                    btnSupp.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnSupp.setOnAction(event -> handleDeleteFromCard(item)); // ميثود جديدة

                    // التعديل مسموح به فقط إذا كانت الريكلاماسيون في الانتظار (اختياري)
                    if (item.getStatut().toString().equals("EN_ATTENTE")) {
                        actions.getChildren().addAll(btnModif, btnSupp);
                    }

                    card.getChildren().addAll(header, contenu);

                    // إذا فما إجابة
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

    // ميثودات مساعدة للأزرار داخل الكارد
    private void handleDeleteFromCard(Reclamation rec) {
        listMyRecs.getSelectionModel().select(rec); // نختاروها باش نخدمو بالماكرو القديم
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

    @FXML void switchToList(ActionEvent event) { navigateTo("/gui/ListReclamation.fxml", event); }
    @FXML void switchToForm(ActionEvent event) { navigateTo("/gui/AddReclamation.fxml", event); }

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