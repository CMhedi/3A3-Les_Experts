package Services.interfaces;

import Entities.Seance;
import model.analytics.UserProfile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RecommendationServiceUser {

    private final ReservationSeanceService reservationService =
            new ReservationSeanceService();
    private final SeanceService seanceService =
            new SeanceService();
    private final UserAnalyticsService analyticsService =
            new UserAnalyticsService();

    // =================================================
    // 📦 RESULT OBJECT (Score + Reason)
    // =================================================
    public static class ScoredSeance {
        public Seance seance;
        public double score;
        public String reason;

        public ScoredSeance(Seance s, double score, String reason) {
            this.seance = s;
            this.score = score;
            this.reason = reason;
        }
    }

    // =================================================
    // 🔥 HYBRID RECOMMENDATION ENGINE (EXPLICABLE)
    // =================================================
    public List<ScoredSeance> recommendForUser(int userId) throws Exception {

        List<Seance> allSeances = seanceService.getAll();
        Map<Integer, Set<Integer>> userMap =
                reservationService.getAllUserReservations();

        UserProfile profile =
                analyticsService.analyzeUser(userId);

        Set<Integer> userSeances =
                userMap.getOrDefault(userId, new HashSet<>());

        List<ScoredSeance> results = new ArrayList<>();

        for (Seance s : allSeances) {

            // ❌ Déjà réservé
            if (userSeances.contains(s.getIdSeance()))
                continue;

            // ❌ Non planifiée
            if (s.getStatutSeance() != enums.StatutSeance.PLANIFIEE)
                continue;

            // 🔹 Sous-scores
            double sim = similarityScore(userId, s, userMap);
            double pop = popularityScore(s);
            double coach = coachScore(s, profile);
            double time = timeScore(s, profile);
            double rec = recencyScore(userId);

            // 🔹 Score final pondéré
            double finalScore =
                    0.35 * sim +
                            0.20 * pop +
                            0.20 * coach +
                            0.15 * time +
                            0.10 * rec;

            if (finalScore <= 0)
                continue;

            // 🔥 Génération dynamique de la raison
            String reason = buildReason(sim, pop, coach, time);

            results.add(new ScoredSeance(s, finalScore, reason));
        }

        return results.stream()
                .sorted((a, b) ->
                        Double.compare(b.score, a.score))
                .limit(3)
                .toList();
    }

    // =================================================
    // 🎯 REASON BUILDER (INTELLIGENT)
    // =================================================
    private String buildReason(double sim,
                               double pop,
                               double coach,
                               double time) {

        if (sim >= 0.4)
            return "Des utilisateurs similaires ont réservé cette séance";

        if (coach == 1)
            return "Vous aimez les séances avec ce coach";

        if (time == 1)
            return "Correspond à votre créneau préféré";

        if (pop >= 0.7)
            return "Séance très populaire auprès des utilisateurs";

        return "Suggestion personnalisée basée sur votre activité";
    }

    // =================================================
    // 1️⃣ SIMILARITÉ (JACCARD)
    // =================================================
    private double similarityScore(
            int userId,
            Seance candidate,
            Map<Integer, Set<Integer>> userMap) {

        Set<Integer> target =
                userMap.getOrDefault(userId, Set.of());

        double best = 0;

        for (var entry : userMap.entrySet()) {

            if (entry.getKey() == userId)
                continue;

            Set<Integer> other = entry.getValue();

            if (!other.contains(candidate.getIdSeance()))
                continue;

            double sim = computeJaccard(target, other);
            best = Math.max(best, sim);
        }

        return best;
    }

    private double computeJaccard(Set<Integer> a,
                                  Set<Integer> b) {

        if (a == null || b == null
                || a.isEmpty() || b.isEmpty())
            return 0;

        Set<Integer> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<Integer> union = new HashSet<>(a);
        union.addAll(b);

        return union.isEmpty()
                ? 0
                : (double) intersection.size() / union.size();
    }

    // =================================================
    // 2️⃣ POPULARITÉ
    // =================================================
    private double popularityScore(Seance s) {

        int count =
                reservationService.countReservations(
                        s.getIdSeance());

        return Math.min(count / 10.0, 1.0);
    }

    // =================================================
    // 3️⃣ COACH
    // =================================================
    private double coachScore(Seance s,
                              UserProfile profile) {

        if (profile == null)
            return 0;

        return s.getIdCoach()
                == profile.getFavoriteCoach() ? 1 : 0;
    }

    // =================================================
    // 4️⃣ HORAIRE
    // =================================================
    private double timeScore(Seance s,
                             UserProfile profile) {

        if (profile == null)
            return 0;

        boolean isMorning =
                s.getHeureDebut()
                        .isBefore(LocalTime.NOON);

        if (profile.getPreferredTime()
                .equals("MATIN") && isMorning)
            return 1;

        if (profile.getPreferredTime()
                .equals("APRES_MIDI") && !isMorning)
            return 1;

        return 0;
    }

    // =================================================
    // 5️⃣ RÉCENCE
    // =================================================
    private double recencyScore(int userId) {

        try {

            List<Seance> reserved =
                    reservationService
                            .getConfirmedSeancesByUser(userId);

            if (reserved.isEmpty())
                return 0;

            LocalDate latest =
                    reserved.stream()
                            .map(Seance::getDateSeance)
                            .max(LocalDate::compareTo)
                            .orElse(null);

            if (latest == null)
                return 0;

            long days =
                    ChronoUnit.DAYS.between(
                            latest,
                            LocalDate.now());

            if (days < 7) return 1;
            if (days < 30) return 0.6;

            return 0.3;

        } catch (Exception e) {
            return 0;
        }
    }
}