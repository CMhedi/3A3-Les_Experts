package controllers;

import GUI.utils.DialogUtils;
import GUI.utils.SceneUtils;
import Services.CancellationPolicyService;
import Services.PricingSimService;
import Services.PromoEngineService;
import Services.RiskScoreService;
import Services.WorkflowService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class PackInscriptionPlanBController {

    // Pricing
    @FXML private TextField pBase, pOcc, pDay, pLead;
    @FXML private Label pOut;
    @FXML private ListView<String> pRules;

    // Promo
    @FXML private TextField cUser, cAmount, cCode, cDate;
    @FXML private Label cOut;

    // Workflow
    @FXML private ComboBox<WorkflowService.State> wState;
    @FXML private ComboBox<WorkflowService.Action> wAction;
    @FXML private Label wOut;

    // Policy
    @FXML private TextField kHours;
    @FXML private Label kOut;

    // Risk
    @FXML private TextField rCancels, rNoShows, rLast, rComplaints;
    @FXML private Label rOut;

    // Services
    private final PricingSimService pricing = new PricingSimService();
    private final PromoEngineService promo = new PromoEngineService();
    private final WorkflowService workflow = new WorkflowService();
    private final CancellationPolicyService policy = new CancellationPolicyService();
    private final RiskScoreService risk = new RiskScoreService();

    private PromoEngineService.PromoResult lastPromo; // for consume button

    @FXML
    public void initialize() {
        // Defaults
        pBase.setText("50");
        pOcc.setText("0.75");
        pDay.setText("SATURDAY");
        pLead.setText("12");

        cUser.setText("user#1");
        cAmount.setText("62.5");
        cCode.setText("ECO10");
        cDate.setText(LocalDate.now().toString());

        wState.setItems(FXCollections.observableArrayList(WorkflowService.State.values()));
        wAction.setItems(FXCollections.observableArrayList(WorkflowService.Action.values()));
        wState.setValue(WorkflowService.State.DRAFT);
        wAction.setValue(WorkflowService.Action.SUBMIT);

        kHours.setText("20");

        rCancels.setText("2");
        rNoShows.setText("1");
        rLast.setText("1");
        rComplaints.setText("0");

        pRules.setItems(FXCollections.observableArrayList());
    }

    @FXML
    void doPricing() {
        try {
            double base = Double.parseDouble(pBase.getText().trim());
            double occ = Double.parseDouble(pOcc.getText().trim());
            int lead = Integer.parseInt(pLead.getText().trim());

            String dayRaw = pDay.getText().trim().toUpperCase();
            DayOfWeek day = DayOfWeek.valueOf(dayRaw);

            PricingSimService.Quote q = pricing.simulate(base, occ, day, lead);

            pOut.setText("Base=" + q.basePrice + " | Final=" + q.finalPrice + " | Occ=" + q.occupancy);
            pRules.getItems().setAll(q.rules.stream().map(Object::toString).toList());

        } catch (Exception e) {
            DialogUtils.showError("Erreur Pricing", "Vérifie les valeurs (day=SATURDAY, occupancy 0..1).");
        }
    }

    @FXML
    void doPromo() {
        try {
            String user = cUser.getText().trim();
            double amount = Double.parseDouble(cAmount.getText().trim());
            String code = cCode.getText().trim();
            LocalDate date = LocalDate.parse(cDate.getText().trim());

            lastPromo = promo.validateAndApply(code, user, amount, date);

            if (lastPromo.valid) {
                cOut.setText("OK: -" + lastPromo.discountAmount + " => " + lastPromo.finalAmount);
            } else {
                cOut.setText("NO: " + lastPromo.reason);
            }
        } catch (Exception e) {
            DialogUtils.showError("Erreur Promo", "Vérifie amount/date/code.");
        }
    }

    @FXML
    void consumePromo() {
        try {
            if (lastPromo == null || !lastPromo.valid) {
                DialogUtils.showWarning("Info", "Valide un code promo d'abord.");
                return;
            }
            String user = cUser.getText().trim();
            String code = cCode.getText().trim();
            promo.consume(code, user);
            DialogUtils.showInfo("OK", "Promo consommé (usage +1) pour " + user + ".");
        } catch (Exception e) {
            DialogUtils.showError("Erreur", "Impossible de consommer.");
        }
    }

    @FXML
    void doWorkflow() {
        try {
            WorkflowService.State st = wState.getValue();
            WorkflowService.Action act = wAction.getValue();

            WorkflowService.TransitionResult r = workflow.transition(st, act);

            if (r.allowed) {
                wOut.setText("✅ " + r.from + " --(" + act + ")--> " + r.to);
                wState.setValue(r.to);
            } else {
                wOut.setText("❌ " + r.from + " --(" + act + ")--> NOT ALLOWED (" + r.reason + ")");
            }
        } catch (Exception e) {
            DialogUtils.showError("Erreur", "Workflow invalid.");
        }
    }

    @FXML
    void doPolicy() {
        try {
            int hours = Integer.parseInt(kHours.getText().trim());
            CancellationPolicyService.PolicyResult r = policy.compute(hours);

            kOut.setText("Penalty=" + r.penaltyPercent + "% | Refund=" + r.refundEligible + " | Rule=" + r.rule);
        } catch (Exception e) {
            DialogUtils.showError("Erreur Policy", "Hours must be integer.");
        }
    }

    @FXML
    void doRisk() {
        try {
            int cancels = Integer.parseInt(rCancels.getText().trim());
            int noShows = Integer.parseInt(rNoShows.getText().trim());
            int last = Integer.parseInt(rLast.getText().trim());
            int complaints = Integer.parseInt(rComplaints.getText().trim());

            RiskScoreService.RiskResult rr = risk.score(cancels, noShows, last, complaints);
            rOut.setText("Score=" + rr.score + " | " + rr.level + " | " + rr.recommendation);
        } catch (Exception e) {
            DialogUtils.showError("Erreur Risk", "All values must be integers.");
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        // عدّل المسار حسب layout متاعك
        SceneUtils.loadScene("/GUI/MainLayout.fxml", (Node) event.getSource());
    }
}