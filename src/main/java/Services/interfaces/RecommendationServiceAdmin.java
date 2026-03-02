package Services.interfaces;

import Entities.Seance;
import Entities.UserApp;
import Services.SeanceService;
import Services.UserService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationServiceAdmin {

    private final SeanceService seanceService = new SeanceService();
    private final UserService userService = new UserService();
    public String getBestDayRecommendation() throws Exception {

        List<Seance> seances = seanceService.getAll();

        Map<DayOfWeek, List<Double>> map = new HashMap<>();

        for (Seance s : seances) {

            if (s.getDateSeance().isBefore(LocalDate.now())) {

                double taux = seanceService
                        .calculateTauxRemplissage(s.getIdSeance());

                map.computeIfAbsent(
                        s.getDateSeance().getDayOfWeek(),
                        k -> new ArrayList<>()
                ).add(taux);
            }
        }

        DayOfWeek bestDay = null;
        double bestScore = 0;

        for (var entry : map.entrySet()) {

            double moyenne = entry.getValue()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

            if (moyenne > bestScore) {
                bestScore = moyenne;
                bestDay = entry.getKey();
            }
        }

        if (bestDay == null)
            return "🔥 Aucune donnée d’historique disponible.";

        // 🔥 Conversion en français
        String dayFr = bestDay.getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.FRENCH
        );

        // Première lettre majuscule
        dayFr = dayFr.substring(0,1).toUpperCase() + dayFr.substring(1);

        return "🔥 Créneau le plus performant : "
                + dayFr
                + "\n   → Taux moyen : "
                + Math.round(bestScore)
                + "%";
    }
    public String getBestCoachRecommendation() throws Exception {

        List<Seance> seances = seanceService.getAll();

        Map<Integer, List<Double>> coachMap = new HashMap<>();

        for (Seance s : seances) {

            if (s.getDateSeance().isBefore(LocalDate.now())) {

                double taux = seanceService.calculateTauxRemplissage(s.getIdSeance());

                coachMap.computeIfAbsent(
                        s.getIdCoach(),
                        k -> new ArrayList<>()
                ).add(taux);
            }
        }

        int bestCoachId = -1;
        double bestScore = 0;

        for (var entry : coachMap.entrySet()) {

            double moyenne = entry.getValue()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

            if (moyenne > bestScore) {
                bestScore = moyenne;
                bestCoachId = entry.getKey();
            }
        }

        UserApp coach = userService.getById(bestCoachId);

        if (coach == null)
            return "👨‍🏫 Aucun coach performant détecté.";

        return "👨‍🏫 Coach recommandé : "
                + coach.getNom()
                + " "
                + coach.getPrenom()
                + " ("
                + Math.round(bestScore)
                + "%)";
    }
    public String getBestMonthRecommendation() throws Exception {

        List<Seance> seances = seanceService.getAll();

        Map<Month, List<Double>> map = new HashMap<>();

        for (Seance s : seances) {

            if (s.getDateSeance().isBefore(LocalDate.now())) {

                double taux = seanceService
                        .calculateTauxRemplissage(s.getIdSeance());

                map.computeIfAbsent(
                        s.getDateSeance().getMonth(),
                        k -> new ArrayList<>()
                ).add(taux);
            }
        }

        Month bestMonth = null;
        double bestScore = 0;

        for (var entry : map.entrySet()) {

            double moyenne = entry.getValue()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

            if (moyenne > bestScore) {
                bestScore = moyenne;
                bestMonth = entry.getKey();
            }
        }

        if (bestMonth == null)
            return "📈 Aucune donnée mensuelle disponible.";

        // 🔥 Mois en français
        String monthFr = bestMonth.getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.FRENCH
        );

        // Première lettre majuscule
        monthFr = monthFr.substring(0,1).toUpperCase() + monthFr.substring(1);

        // 🔥 Niveau performance
        String niveau;

        if (bestScore >= 80)
            niveau = "⭐ Très performant";
        else if (bestScore >= 60)
            niveau = "👍 Bon rendement";
        else
            niveau = "⚠ À optimiser";

        return "📈 Mois le plus performant : "
                + monthFr
                + "\n   → Taux moyen : "
                + Math.round(bestScore)
                + "%"
                + "\n   → Niveau : "
                + niveau;
    }
}

