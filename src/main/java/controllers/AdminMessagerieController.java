package controllers;

import Entities.AdminConversationListItem;
import Services.interfaces.ConversationDAO;
import Services.interfaces.MessageDAO;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for AdminMessagerie.fxml
 * Manages conversations list, KPIs, charts and top-user panels.
 *
 * HOW TO WIRE YOUR SERVICE:
 *   Replace every "// TODO: replace with service" comment with a call to
 *   your existing service/DAO layer.  The ConversationRow model at the
 *   bottom of this file is a lightweight view-model; map your entities to it.
 */
public class AdminMessagerieController implements Initializable {

    // ── KPI labels ──────────────────────────────────────────
    @FXML private Label kpiConversations;
    @FXML private Label kpiGroupes;
    @FXML private Label kpiMessages7j;
    @FXML private Label kpiActifs;

    // ── Filter bar ──────────────────────────────────────────
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbType;
    @FXML private Label lblConvCount;

    // ── Conversations table ──────────────────────────────────
    @FXML private TableView<ConversationRow>         tblConversations;
    @FXML private TableColumn<ConversationRow, Integer> colId;
    @FXML private TableColumn<ConversationRow, String>  colTitre;
    @FXML private TableColumn<ConversationRow, String>  colType;
    @FXML private TableColumn<ConversationRow, String>  colCreateur;
    @FXML private TableColumn<ConversationRow, Integer> colNbParticipants;
    @FXML private TableColumn<ConversationRow, Integer> colNbMessages;
    @FXML private TableColumn<ConversationRow, String>  colDate;
    @FXML private TableColumn<ConversationRow, String>  colDernierMsg;
    @FXML private TableColumn<ConversationRow, Void>    colActions;

    // ── Top-user panels ──────────────────────────────────────
    @FXML private VBox vboxTopCreateurs;
    @FXML private VBox vboxTopCommunicateurs;

    // ── Charts ───────────────────────────────────────────────
    @FXML private PieChart                    chartConvTypes;
    @FXML private PieChart                    chartMsgTypes;
    @FXML private BarChart<String, Number>    chartMsgJour;

    // ── State ────────────────────────────────────────────────
    private final ObservableList<ConversationRow> masterList   = FXCollections.observableArrayList();
    private FilteredList<ConversationRow>          filteredList;

    private ConversationDAO conversationDAO;
    private MessageDAO      messageDAO;
    /** Messages sur les 7 derniers jours (KPI). */
    private long messagesLast7Days;
    private int[] dailyMessageCounts7d = new int[7];
    private Map<String, Long> typeCounts7d = new LinkedHashMap<>();
    private List<MessageDAO.TopUserMsg> topCommunicators = List.of();

    // ── Colors ───────────────────────────────────────────────
    private static final String[] AVATAR_COLORS = {
            "#3b82f6","#10b981","#f59e0b","#ef4444","#8b5cf6","#06b6d4","#ec4899"
    };

    // ════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conversationDAO = new ConversationDAO();
        messageDAO = new MessageDAO();
        setupFilters();
        setupTable();
        loadData();
    }

    // ── Filter setup ─────────────────────────────────────────
    private void setupFilters() {
        cmbType.setItems(FXCollections.observableArrayList("Tous", "Privée", "Groupe"));
        cmbType.setValue("Tous");
        txtSearch.textProperty().addListener((o, ov, nv) -> applyFilter());
        cmbType.valueProperty().addListener((o, ov, nv)  -> applyFilter());
    }

    private void applyFilter() {
        String q    = txtSearch.getText().toLowerCase(Locale.ROOT).trim();
        String type = cmbType.getValue();
        filteredList.setPredicate(c -> {
            boolean matchQ = q.isEmpty()
                    || c.getTitre().toLowerCase().contains(q)
                    || c.getCreateurNom().toLowerCase().contains(q);
            boolean matchT = "Tous".equals(type) || c.getType().equalsIgnoreCase(type);
            return matchQ && matchT;
        });
        lblConvCount.setText(filteredList.size() + " conversation(s)");
    }

    // ── Table setup ──────────────────────────────────────────
    private void setupTable() {
        // Value factories
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCreateur.setCellValueFactory(new PropertyValueFactory<>("createurNom"));
        colNbParticipants.setCellValueFactory(new PropertyValueFactory<>("nbParticipants"));
        colNbMessages.setCellValueFactory(new PropertyValueFactory<>("nbMessages"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        colDernierMsg.setCellValueFactory(new PropertyValueFactory<>("dernierMessage"));

        // Titre cell: text + "New" badge
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                HBox box = new HBox(6);
                box.setAlignment(Pos.CENTER_LEFT);
                Label lbl = new Label(item);
                lbl.setStyle("-fx-font-size:13;-fx-text-fill:#0f172a;-fx-font-weight:bold;");
                box.getChildren().add(lbl);
                ConversationRow row = getTableView().getItems().get(getIndex());
                if (row != null && row.isNew()) {
                    Label badge = new Label("New");
                    badge.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;" +
                            "-fx-background-radius:10;-fx-padding:1 7;-fx-font-size:10;-fx-font-weight:bold;");
                    box.getChildren().add(badge);
                }
                setGraphic(box);
                setText(null);
            }
        });

        // Type cell: colored badge
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label b = new Label(item);
                String bg = item.equalsIgnoreCase("Groupe") ? "#3b82f6" : "#10b981";
                b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                        "-fx-background-radius:12;-fx-padding:2 10;-fx-font-size:11;-fx-font-weight:bold;");
                setGraphic(b);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Créateur cell: avatar circle + name
        colCreateur.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                HBox box = new HBox(7);
                box.setAlignment(Pos.CENTER_LEFT);
                Label av = avatarLabel(item, 26);
                Label nm = new Label(item);
                nm.setStyle("-fx-font-size:12;-fx-text-fill:#0f172a;");
                box.getChildren().addAll(av, nm);
                setGraphic(box);
                setText(null);
            }
        });

        // Dernier message cell: italic grey text
        colDernierMsg.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    return;
                }
                setStyle("-fx-font-style:italic;-fx-text-fill:#64748b;-fx-font-size:12;");
                setText(item.length() > 40 ? item.substring(0, 37) + "…" : item);
            }
        });

        // Actions cell: Detail / Ban / Delete
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnDetail = styledBtn("👁 Détail",  "#3b82f6");
            final Button btnBan    = styledBtn("🚫",         "#f59e0b");
            final Button btnDel    = styledBtn("🗑",         "#ef4444");
            final HBox   box       = new HBox(4, btnDetail, btnBan, btnDel);
            {
                box.setAlignment(Pos.CENTER);
                btnDetail.setOnAction(e -> openDetail(getTableRow().getItem()));
                btnBan   .setOnAction(e -> banConversation(getTableRow().getItem()));
                btnDel   .setOnAction(e -> deleteConversation(getTableRow().getItem()));
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Row hover style
        tblConversations.setRowFactory(tv -> {
            TableRow<ConversationRow> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) row.setStyle("-fx-background-color:#f8fafc;");
            });
            row.setOnMouseExited(e  -> row.setStyle(""));
            return row;
        });

        filteredList = new FilteredList<>(masterList, p -> true);
        tblConversations.setItems(filteredList);
    }

    // ════════════════════════════════════════════════════════
    //  DATA LOADING  — replace these with your service calls
    // ════════════════════════════════════════════════════════
    @FXML
    public void onRefresh() {
        loadData();
    }

    private void loadData() {
        masterList.clear();
        try {
            List<AdminConversationListItem> rows = conversationDAO.findAllForAdmin();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (AdminConversationListItem it : rows) {
                String type = it.estGroupe() == 1 ? "Groupe" : "Privée";
                String dateStr = it.dateCreation() != null ? it.dateCreation().format(dtf) : "";
                String last = it.dernierMessage() != null ? it.dernierMessage().trim() : "";
                if (last.length() > 500) {
                    last = last.substring(0, 497) + "…";
                }
                masterList.add(new ConversationRow(
                        it.idConversation(),
                        it.titre(),
                        type,
                        it.createurNom(),
                        it.nbParticipants(),
                        it.nbMessages(),
                        dateStr,
                        last,
                        false));
            }
            messagesLast7Days = messageDAO.countMessagesLastDays(7);
            dailyMessageCounts7d = messageDAO.getDailyMessageCountsLast7Days();
            typeCounts7d = messageDAO.countByTypeMessageLastDays(7);
            topCommunicators = messageDAO.topUsersByMessagesLastDays(7, 10);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible de charger les données messagerie : " + ex.getMessage());
            messagesLast7Days = 0;
            dailyMessageCounts7d = new int[7];
            typeCounts7d = new LinkedHashMap<>();
            topCommunicators = List.of();
        }

        updateKpis();
        applyFilter();
        updateTopUsers();
        updateCharts();
    }

    // ── KPIs ─────────────────────────────────────────────────
    private void updateKpis() {
        long nbGroupe = masterList.stream().filter(c -> "Groupe".equalsIgnoreCase(c.getType())).count();
        long actifs   = masterList.stream().map(ConversationRow::getCreateurNom).distinct().count();

        kpiConversations.setText(String.valueOf(masterList.size()));
        kpiGroupes.setText(String.valueOf(nbGroupe));
        kpiMessages7j.setText(String.valueOf(messagesLast7Days));
        kpiActifs.setText(String.valueOf(actifs));
    }

    // ── Top-user panels ──────────────────────────────────────
    private void updateTopUsers() {
        vboxTopCreateurs.getChildren().clear();
        vboxTopCommunicateurs.getChildren().clear();

        // Top actifs: count conversations per creator
        Map<String, Long> byCreator = masterList.stream()
                .collect(Collectors.groupingBy(ConversationRow::getCreateurNom, Collectors.counting()));

        byCreator.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> addUserRow(vboxTopCreateurs, e.getKey(), e.getValue().intValue(), true));

        if (topCommunicators == null || topCommunicators.isEmpty()) {
            Label empty = new Label("Aucun message sur les 7 derniers jours.");
            empty.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;-fx-padding:16;");
            vboxTopCommunicateurs.getChildren().add(empty);
        } else {
            for (MessageDAO.TopUserMsg t : topCommunicators) {
                addUserRow(vboxTopCommunicateurs, t.name(), (int) t.count(), true);
            }
        }
    }

    private void addUserRow(VBox container, String name, int count, boolean hasBanBtn) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 16, 9, 16));
        row.setStyle("-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#f8fafc;-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;"));

        Label av  = avatarLabel(name, 30);
        Label lbl = new Label(name);
        lbl.setStyle("-fx-font-size:13;-fx-text-fill:#0f172a;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label cnt = new Label(String.valueOf(count));
        cnt.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#64748b;-fx-background-radius:10;" +
                "-fx-padding:2 8;-fx-font-size:12;-fx-font-weight:bold;");

        Button btnBan  = iconBtn("🚫", "#fee2e2", "#ef4444");
        Button btnInfo = iconBtn("ℹ",  "#eff6ff",  "#3b82f6");
        btnBan.setOnAction(e -> banUser(name));

        row.getChildren().addAll(av, lbl, sp, cnt, btnBan, btnInfo);
        container.getChildren().add(row);
    }

    // ── Charts ───────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void updateCharts() {
        // Pie: distribution conv types
        long nbGroupe = masterList.stream().filter(c -> "Groupe".equalsIgnoreCase(c.getType())).count();
        long nbPrivee = masterList.stream().filter(c -> "Privée".equalsIgnoreCase(c.getType())).count();
        chartConvTypes.setData(FXCollections.observableArrayList(
                new PieChart.Data("Conversations privées " + nbPrivee, Math.max(nbPrivee, 0.01)),
                new PieChart.Data("Groupes " + nbGroupe, Math.max(nbGroupe, 0.01))
        ));

        // Bar: messages per day (last 7 days)
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Messages");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 6; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).format(fmt);
            int idx = 6 - i;
            int val = (dailyMessageCounts7d != null && idx < dailyMessageCounts7d.length)
                    ? dailyMessageCounts7d[idx] : 0;
            series.getData().add(new XYChart.Data<>(day, val));
        }
        chartMsgJour.getData().clear();
        chartMsgJour.getData().add(series);
        // Color bars after scene is rendered
        javafx.application.Platform.runLater(() ->
                chartMsgJour.lookupAll(".bar").forEach(node ->
                        node.setStyle("-fx-bar-fill:#3b82f6;")));

        // Pie: message types (last 7 days)
        long pieTotal = typeCounts7d == null ? 0 : typeCounts7d.values().stream().mapToLong(Long::longValue).sum();
        if (pieTotal == 0) {
            chartMsgTypes.setData(FXCollections.emptyObservableList());
        } else {
            List<PieChart.Data> slice = new ArrayList<>();
            for (Map.Entry<String, Long> e : typeCounts7d.entrySet()) {
                slice.add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", Math.max(e.getValue(), 0.01)));
            }
            chartMsgTypes.setData(FXCollections.observableArrayList(slice));
        }
    }

    // ════════════════════════════════════════════════════════
    //  ACTIONS
    // ════════════════════════════════════════════════════════

    /** Open the detail view for a conversation. */
    private void openDetail(ConversationRow conv) {
        if (conv == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AdminMessagerieDetail.fxml"));
            Parent root = loader.load();

            AdminMessagerieDetailController ctrl = loader.getController();

            // This is the clean, single line you need:
            ctrl.setConversation(conv.getId(), conv.getTitre(), conv.getType(), conv.getDateCreation());

            Stage stage = new Stage();
            stage.setTitle("Détail – " + conv.getTitre());
            stage.setScene(new Scene(root, 980, 660));
            stage.show();
        } catch (IOException ex) {
            showError("Impossible d'ouvrir le détail : " + ex.getMessage());
        }
    }
    /** Empty a conversation's messages. */
    private void banConversation(ConversationRow conv) {
        if (conv == null) return;
        Alert dlg = confirm("Bannir conversation",
                "Vider tous les messages de « " + conv.getTitre() + " » ?");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // TODO: service.banConversation(conv.getId());
                showInfo("Conversation vidée", "Les messages ont été supprimés.");
                loadData();
            }
        });
    }

    /** Delete a conversation entirely. */
    private void deleteConversation(ConversationRow conv) {
        if (conv == null) return;
        Alert dlg = confirm("Supprimer conversation",
                "Supprimer définitivement « " + conv.getTitre() + " » ?\nCette action est irréversible.");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (conversationDAO.deleteConversation(conv.getId())) {
                    loadData();
                } else {
                    showError("Impossible de supprimer la conversation en base.");
                }
            }
        });
    }

    /** Ban a user (from top-user panel). */
    private void banUser(String name) {
        Alert dlg = confirm("Bannir utilisateur",
                "Êtes-vous sûr de vouloir bannir « " + name + " » ?");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // TODO: service.banUser(name);
                showInfo("Utilisateur banni", name + " a été banni avec succès.");
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    private Label avatarLabel(String name, int size) {
        String letter = (name == null || name.isBlank()) ? "?" : name.substring(0, 1).toUpperCase();
        int colorIdx  = Math.abs(name.hashCode()) % AVATAR_COLORS.length;
        Label av = new Label(letter);
        String s = size + "px";
        av.setStyle("-fx-background-color:" + AVATAR_COLORS[colorIdx] + ";-fx-text-fill:white;" +
                "-fx-background-radius:50;-fx-min-width:" + s + ";-fx-min-height:" + s + ";" +
                "-fx-max-width:" + s + ";-fx-max-height:" + s + ";" +
                "-fx-alignment:center;-fx-font-weight:bold;-fx-font-size:" + (size / 2.2) + ";");
        av.setAlignment(Pos.CENTER);
        return av;
    }

    private Button styledBtn(String text, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                "-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12;-fx-padding:5 10;");
        return b;
    }

    private Button iconBtn(String icon, String bg, String fg) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12;-fx-padding:4 8;");
        return b;
    }

    private Alert confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(null);
        return a;
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.show();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.show();
    }

    // ════════════════════════════════════════════════════════
    //  VIEW-MODEL
    // ════════════════════════════════════════════════════════

    /**
     * Lightweight view-model for a conversation row.
     * Replace its constructor / factory method with your entity mapping.
     */
    public static class ConversationRow {
        private final IntegerProperty    id              = new SimpleIntegerProperty();
        private final StringProperty     titre           = new SimpleStringProperty();
        private final StringProperty     type            = new SimpleStringProperty();
        private final StringProperty     createurNom     = new SimpleStringProperty();
        private final IntegerProperty    nbParticipants  = new SimpleIntegerProperty();
        private final IntegerProperty    nbMessages      = new SimpleIntegerProperty();
        private final StringProperty     dateCreation    = new SimpleStringProperty();
        private final StringProperty     dernierMessage  = new SimpleStringProperty();
        private boolean isNew;

        public ConversationRow(int id, String titre, String type, String createurNom,
                               int nbParticipants, int nbMessages,
                               String dateCreation, String dernierMessage, boolean isNew) {
            this.id.set(id);
            this.titre.set(titre);
            this.type.set(type);
            this.createurNom.set(createurNom);
            this.nbParticipants.set(nbParticipants);
            this.nbMessages.set(nbMessages);
            this.dateCreation.set(dateCreation);
            this.dernierMessage.set(dernierMessage);
            this.isNew = isNew;
        }

        // Corrected Getters
        public int    getId()             { return id.get(); }
        public String getTitre()          { return titre.get(); }
        public String getType()        { return type.get(); }
        public String getCreateurNom()    { return createurNom.get(); }
        public int    getNbParticipants() { return nbParticipants.get(); }
        public int    getNbMessages()     { return nbMessages.get(); }
        public String getDateCreation()   { return dateCreation.get(); }
        public String getDernierMessage() { return dernierMessage.get(); }
        public boolean isNew()            { return isNew; }

        // Corrected Properties
        public IntegerProperty idProperty()             { return id; }
        public StringProperty  titreProperty()          { return titre; }
        public StringProperty  typeProperty()           { return type; } // Should be StringProperty
        public StringProperty  createurNomProperty()    { return createurNom; }
        public IntegerProperty nbParticipantsProperty() { return nbParticipants; }
        public IntegerProperty nbMessagesProperty()     { return nbMessages; }
        public StringProperty  dateCreationProperty()   { return dateCreation; }
        public StringProperty  dernierMessageProperty() { return dernierMessage; }
    }
}