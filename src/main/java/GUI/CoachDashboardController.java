package GUI;

import Entities.Planning;
import Entities.Seance;
import Entities.UserApp;
import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.interfaces.PlanningService;
import Services.interfaces.ReservationSeanceService;
import Services.interfaces.SeanceService;
import Services.interfaces.UserService;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import enums.RoleUser;
import enums.StatutPresence;
import enums.StatutSeance;
import Entities.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.shape.Circle;

import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import Entities.Session;
import Entities.UserApp;
import javax.management.relation.Role;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoachDashboardController {

    // ================= UI =================
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;

    @FXML private Label lblTotal;
    @FXML private Label lblToday;
    @FXML private Label lblTerminee;

    // ================= SERVICES =================
    private final SeanceService seanceService = new SeanceService();
    private final PlanningService planningService = new PlanningService();

    private final Map<Integer, String> planningMap = new HashMap<>();

    private UserApp connectedCoach;
    private int coachId; // ⚠️ login plus tard

    private List<Seance> masterData;

    // =================================================
    // INITIALISATION
    // =================================================
    @FXML
    public void initialize() {

        connectedCoach = Session.getConnectedUser();

        if (connectedCoach == null) {
            DialogUtils.showError("Erreur", "Utilisateur non connecté.");
            return;
        }

        // ✅ Vérification du rôle ICI
        if (connectedCoach.getRole() != RoleUser.COACH) {
            DialogUtils.showError(
                    "Accès refusé",
                    "Cette page est réservée aux coachs."
            );
            return;
        }

        coachId = connectedCoach.getIdUser();

        configureFilters();

        searchField.textProperty().addListener((obs, o, n) -> refreshCards());
        statutFilter.valueProperty().addListener((obs, o, n) -> refreshCards());

        loadPlannings();
        loadData();
    }

    // =================================================
    // CONFIG FILTER
    // =================================================
    private void configureFilters() {

        statutFilter.getItems().addAll(
                "Tous",
                "PLANIFIEE",
                "TERMINEE",
                "ANNULEE"
        );

        statutFilter.setValue("Tous");
        statutFilter.getStyleClass().add("combo-pro");

        searchField.getStyleClass().add("search-field-pro");
        searchField.setPromptText("🔍 Rechercher une séance...");
    }

    // =================================================
    // LOAD DATA
    // =================================================
    private void loadData() {

        try {

            masterData = seanceService.getByCoach(coachId);

            updateStats(masterData);
            refreshCards();

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les séances."
            );
        }
    }

    private void loadPlannings() {

        try {

            List<Planning> list = planningService.getAll();

            for (Planning p : list) {
                planningMap.put(
                        p.getIdPlanning(),
                        p.getPeriode()
                );
            }

        } catch (Exception e) {

            DialogUtils.showError(
                    "Erreur",
                    "Impossible de charger les plannings."
            );
        }
    }

    // =================================================
    // CARD SYSTEM (PRO VERSION CLEAN)
    // =================================================
    private void refreshCards() {

        cardContainer.getChildren().clear();

        if (masterData == null) return;

        String search = searchField.getText() == null ?
                "" : searchField.getText().toLowerCase();

        String statutSelected = statutFilter.getValue();

        for (Seance s : masterData) {

            boolean matchesSearch =
                    s.getNom().toLowerCase().contains(search)
                            || s.getDateSeance().toString().contains(search);

            boolean matchesStatut =
                    statutSelected.equals("Tous")
                            || s.getStatutSeance().name().equals(statutSelected);

            if (matchesSearch && matchesStatut) {
                cardContainer.getChildren().add(createCard(s));
            }
        }
    }

    private VBox createCard(Seance s) {

        VBox card = new VBox(16);
        card.getStyleClass().add("coach-card-modern");
        card.setPrefWidth(300);
        card.setPadding(new Insets(18));

        // ================= HEADER =================
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(s.getNom());
        title.getStyleClass().add("card-title-modern");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Circle statusDot = new Circle(6);

        switch (s.getStatutSeance()) {
            case PLANIFIEE -> statusDot.setStyle("-fx-fill: #1565c0;");
            case TERMINEE -> statusDot.setStyle("-fx-fill: #43a047;");
            case ANNULEE -> statusDot.setStyle("-fx-fill: #e53935;");
        }

        header.getChildren().addAll(title, spacer, statusDot);

        // ================= INFO =================
        VBox infoBox = new VBox(6);

        Label dateLabel = new Label("📅  " + s.getDateSeance());
        dateLabel.getStyleClass().add("card-info-modern");

        Label timeLabel = new Label("⏰  "
                + s.getHeureDebut() + " - " + s.getHeureFin());
        timeLabel.getStyleClass().add("card-info-modern");

        Label planningLabel = new Label("📁  "
                + planningMap.getOrDefault(
                s.getIdPlanning(), "Inconnu"));
        planningLabel.getStyleClass().add("card-info-modern");

        infoBox.getChildren().addAll(dateLabel, timeLabel, planningLabel);

        // ================= PARTICIPANTS =================
        int reserved = 0;
        try {
            ReservationSeanceService reservationService = new ReservationSeanceService();
            reserved = reservationService
                    .countReservations(s.getIdSeance());
        } catch (Exception ignored) {}

        int capacite = s.getCapacite();
        double taux = capacite == 0 ? 0 :
                (double) reserved / capacite;

        ProgressBar progressBar = new ProgressBar(taux);
        progressBar.setPrefHeight(8);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("progress-modern");

        Label participantsLabel =
                new Label(reserved + " / " + capacite + " participants");
        participantsLabel.getStyleClass().add("participants-label");

        // ================= BADGE =================
        Label badge = new Label(s.getStatutSeance().name());
        badge.getStyleClass().add("badge-modern");

        switch (s.getStatutSeance()) {
            case PLANIFIEE -> badge.getStyleClass().add("badge-blue-modern");
            case TERMINEE -> badge.getStyleClass().add("badge-grey-modern");
            case ANNULEE -> badge.getStyleClass().add("badge-red-modern");
        }

        // ================= ACTIONS =================
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        boolean isFuture = s.getDateSeance().isAfter(LocalDate.now());

        if (s.getStatutSeance() == StatutSeance.ANNULEE) {

            Label info = new Label("Séance annulée");
            info.setStyle("-fx-text-fill: #999;");
            actions.getChildren().add(info);

        }
        else if (isFuture) {

            Button btnPresence = new Button("Appel (indisponible)");
            btnPresence.setDisable(true);
            btnPresence.getStyleClass().add("btn-disabled");

            actions.getChildren().add(btnPresence);

        }
        else {

            Button btnPresence = new Button("Faire l'appel");
            btnPresence.getStyleClass().add("btn-presence-modern");

            btnPresence.setOnAction(e -> ouvrirFenetrePresence(s));

            actions.getChildren().add(btnPresence);
        }

        // ================= FOOTER =================
        VBox footer = new VBox(8);
        footer.getChildren().addAll(progressBar, participantsLabel, badge, actions);

        card.getChildren().addAll(header, infoBox, footer);

        return card;
    }
    // =================================================
    // STATS
    // =================================================
    private void updateStats(List<Seance> list) {

        lblTotal.setText(String.valueOf(list.size()));

        long today = list.stream()
                .filter(s -> s.getDateSeance().equals(LocalDate.now()))
                .count();

        lblToday.setText(String.valueOf(today));

        long terminees = list.stream()
                .filter(s -> s.getStatutSeance() == StatutSeance.TERMINEE)
                .count();

        lblTerminee.setText(String.valueOf(terminees));
    }

    // =================================================
    // NAVIGATION
    // =================================================
    @FXML
    private void handleRetour(ActionEvent event) {

        SceneUtils.loadScene(
                "/gui/MainLayoutcoach.fxml",
                (Node) event.getSource()
        );
    }
    @FXML
    private void handleExportPDF() {

        try {

            List<Seance> seances = seanceService.getByCoach(coachId);

            if (seances.isEmpty()) {
                DialogUtils.showWarning(
                        "Export PDF",
                        "Aucune séance à exporter."
                );
                return;
            }

            String filePath = "seances_coach.pdf";

            generatePDF(seances, filePath);

            DialogUtils.showInfo(
                    "Succès",
                    "PDF généré avec succès !"
            );

            Desktop.getDesktop().open(
                    new File(filePath)
            );

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showError(
                    "Erreur",
                    "Impossible de générer le PDF."
            );
        }
    }
    private void generatePDF(List<Seance> seances, String filePath) throws Exception {

        Document document = new Document(PageSize.A4, 40, 40, 110, 60);

        PdfWriter writer = PdfWriter.getInstance(
                document,
                new FileOutputStream(filePath)
        );

        writer.setPageEvent(new PdfPageEventHelperCustom());
        document.open();

        // ================= HEADER =================
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(
                new Phrase("  RAPPORT ECOADVENTURE",
                        new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.WHITE))
        );

        headerCell.setBackgroundColor(new BaseColor(25, 95, 60));
        headerCell.setPadding(18);
        headerCell.setBorder(Rectangle.NO_BORDER);
        header.addCell(headerCell);

        document.add(header);
        // ================= COACH + DATE =================
        UserApp coach = Session.getConnectedUser();

        String coachName = "Inconnu";
        if (coach != null) {
            coachName = coach.getNom() + " " + coach.getPrenom();
        }

        String today = java.time.LocalDate.now().toString();

        Paragraph coachInfo = new Paragraph(
                "Coach : " + coachName + "\n"
                        + "Date de génération : " + today,
                new Font(Font.FontFamily.HELVETICA, 11)
        );

        coachInfo.setSpacingBefore(15);
        coachInfo.setSpacingAfter(20);

        document.add(coachInfo);

        // ================= TITRE =================
        Font titleFont = new Font(
                Font.FontFamily.HELVETICA,
                20,
                Font.BOLD,
                new BaseColor(25, 95, 60)
        );

        Paragraph title = new Paragraph("Rapport des Séances", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(20);
        title.setSpacingAfter(20);
        document.add(title);

        // ================= TABLE =================
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        float[] columnWidths = {2.5f, 2f, 2f, 1.8f, 1.2f, 1.2f, 2f, 1.5f};
        table.setWidths(columnWidths);

        String[] headers = {
                "Séance",
                "Date",
                "Horaire",
                "Statut",
                "Capacité",
                "Inscrits",
                "Présence (P/A/N)",
                "Taux Présence"
        };

        Font headerFont = new Font(
                Font.FontFamily.HELVETICA,
                11,
                Font.BOLD,
                BaseColor.WHITE
        );

        for (String h : headers) {

            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new BaseColor(41, 128, 185));
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        int totalParticipants = 0;
        int totalCapacite = 0;

        boolean alternate = false;

        ReservationSeanceService reservationService =
                new ReservationSeanceService();

        for (Seance s : seances) {

            int reserved =
                    reservationService.countReservations(s.getIdSeance());

            int capacite = s.getCapacite();

            int presents =
                    reservationService.countByPresence(
                            s.getIdSeance(),
                            StatutPresence.PRESENT
                    );

            int absents =
                    reservationService.countByPresence(
                            s.getIdSeance(),
                            StatutPresence.ABSENT
                    );

            int nonMarques =
                    reservationService.countByPresence(
                            s.getIdSeance(),
                            StatutPresence.NON_MARQUE
                    );

            double tauxPresence = reserved == 0
                    ? 0
                    : (double) presents / reserved * 100;

            totalParticipants += reserved;
            totalCapacite += capacite;

            BaseColor rowColor = alternate
                    ? new BaseColor(245, 245, 245)
                    : BaseColor.WHITE;

            // Séance
            table.addCell(createCell(s.getNom(), rowColor));

            // Date
            table.addCell(createCell(s.getDateSeance().toString(), rowColor));

            // Horaire
            table.addCell(createCell(
                    s.getHeureDebut() + " - " + s.getHeureFin(),
                    rowColor
            ));

            // Statut coloré
            Font statutFont;

            switch (s.getStatutSeance()) {
                case PLANIFIEE ->
                        statutFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(52, 152, 219));
                case TERMINEE ->
                        statutFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(39, 174, 96));
                case ANNULEE ->
                        statutFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(231, 76, 60));
                default ->
                        statutFont = new Font(Font.FontFamily.HELVETICA, 10);
            }

            PdfPCell statutCell =
                    new PdfPCell(new Phrase(
                            s.getStatutSeance().name(),
                            statutFont
                    ));

            statutCell.setBackgroundColor(rowColor);
            statutCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(statutCell);

            // Capacité
            table.addCell(createCenterCell(
                    String.valueOf(capacite),
                    rowColor
            ));

            // Inscrits
            table.addCell(createCenterCell(
                    String.valueOf(reserved),
                    rowColor
            ));

            // Présence regroupée
            String presenceResume =
                    presents + " / " + absents + " / " + nonMarques;

            table.addCell(createCenterCell(
                    presenceResume,
                    rowColor
            ));

            // Taux présence coloré
            Font tauxFont;

            if (tauxPresence >= 80)
                tauxFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(39,174,96));
            else if (tauxPresence >= 50)
                tauxFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(243,156,18));
            else
                tauxFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(231,76,60));

            PdfPCell tauxCell =
                    new PdfPCell(new Phrase(
                            String.format("%.1f %%", tauxPresence),
                            tauxFont
                    ));

            tauxCell.setBackgroundColor(rowColor);
            tauxCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(tauxCell);

            alternate = !alternate;
        }

        document.add(table);

        // ================= STATISTIQUES =================
        document.add(new Paragraph("\n"));

        double tauxGlobal = totalCapacite == 0
                ? 0
                : (double) totalParticipants / totalCapacite * 100;

        PdfPTable statsTable = new PdfPTable(1);
        statsTable.setWidthPercentage(100);

        PdfPCell statsCell = new PdfPCell(
                new Phrase(
                        "STATISTIQUES GLOBALES\n\n" +
                                "Total Séances : " + seances.size() + "\n" +
                                "Total Participants : " + totalParticipants + "\n" +
                                "Capacité Totale : " + totalCapacite + "\n" +
                                "Taux Global de Remplissage : "
                                + String.format("%.1f %%", tauxGlobal),
                        new Font(Font.FontFamily.HELVETICA, 11)
                )
        );

        statsCell.setPadding(15);
        statsCell.setBackgroundColor(new BaseColor(230, 240, 230));
        statsCell.setBorderColor(new BaseColor(25, 95, 60));
        statsCell.setBorderWidth(1.5f);

        statsTable.addCell(statsCell);
        document.add(statsTable);

        document.close();
    }
    class PdfPageEventHelperCustom extends PdfPageEventHelper {

        private Image watermark;

        public PdfPageEventHelperCustom() {
            try {

                InputStream stream =
                        getClass().getResourceAsStream("/images/logo_pdf.png");

                if (stream != null) {

                    watermark = Image.getInstance(stream.readAllBytes());

                    // 🔥 Taille watermark
                    watermark.scaleToFit(350, 180);

                    // 🔥 Transparence élégante
                    watermark.setTransparency(new int[]{30, 30});
                }

            } catch (Exception ignored) {}
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {

            // ================= WATERMARK =================
            if (watermark != null) {

                try {

                    PdfContentByte canvas =
                            writer.getDirectContentUnder();

                    float x = (document.getPageSize().getWidth()
                            - watermark.getScaledWidth()) / 2;

                    float y = (document.getPageSize().getHeight()
                            - watermark.getScaledHeight()) / 2;

                    watermark.setAbsolutePosition(x, y);

                    canvas.addImage(watermark);

                } catch (Exception ignored) {}
            }

            // ================= FOOTER =================
            Font footerFont =
                    new Font(
                            Font.FontFamily.HELVETICA,
                            9,
                            Font.ITALIC,
                            new BaseColor(120, 120, 120)
                    );

            Phrase footer =
                    new Phrase(
                            "EcoAdventure © 2026   |   Rapport Confidentiel   |   Page "
                                    + writer.getPageNumber(),
                            footerFont
                    );

            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 20,
                    0
            );
        }
    }
    private PdfPCell createCell(String text, BaseColor bg) {

        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell createCenterCell(String text, BaseColor bg) {

        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        return cell;
    }
    private void ouvrirFenetrePresence(Seance s) {

        // 🔒 1️⃣ Bloquer si annulée
        if (s.getStatutSeance() == StatutSeance.ANNULEE) {

            DialogUtils.showWarning(
                    "Présence",
                    "Impossible de faire l'appel pour une séance annulée."
            );
            return;
        }

        // 🔒 2️⃣ Bloquer si séance future
        if (s.getDateSeance().isAfter(LocalDate.now())) {

            DialogUtils.showWarning(
                    "Présence",
                    "L'appel de présence est disponible uniquement le jour J."
            );
            return;
        }

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/PresenceView.fxml")
            );

            Parent root = loader.load();

            PresenceController controller = loader.getController();
            controller.setSeance(s);

            Stage stage = new Stage();
            stage.setTitle("Appel de présence - " + s.getNom());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // 🔥 bloque arrière-plan
            stage.setResizable(false);
            stage.showAndWait();

            // 🔄 Refresh après fermeture
            refreshCards();

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showError(
                    "Erreur",
                    "Impossible d'ouvrir la fenêtre de présence."
            );
        }
    }
}
