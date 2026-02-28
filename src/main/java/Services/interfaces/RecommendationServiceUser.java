package Services.interfaces;

import Entities.Seance;
import Services.interfaces.SeanceService;
import Services.interfaces.ReservationSeanceService;
import model.analytics.UserProfile;
import Services.interfaces.UserAnalyticsService;
import java.time.LocalTime;
import java.util.*;

public class RecommendationServiceUser {

    private final ReservationSeanceService reservationService = new ReservationSeanceService();
    private final SeanceService seanceService = new SeanceService();
    private final UserAnalyticsService analyticsService = new UserAnalyticsService();

    public List<Seance> recommendForUser(int userId) throws Exception {

        Map<Integer, Set<Integer>> userMap =
                reservationService.getAllUserReservations();

        Set<Integer> targetSeances = userMap.get(userId);

        // 🟢 Cold start
        if (targetSeances == null || targetSeances.isEmpty()) {
            return seanceService.getMostReservedSeances();
        }

        Map<Integer, Double> similarityMap = new HashMap<>();

        for (var entry : userMap.entrySet()) {

            int otherUser = entry.getKey();
            if (otherUser == userId) continue;

            double similarity = computeJaccard(
                    targetSeances,
                    entry.getValue()
            );

            if (similarity > 0.3) {
                similarityMap.put(otherUser, similarity);
            }
        }

        Set<Integer> recommendedIds = new HashSet<>();

        for (Integer similarUser : similarityMap.keySet()) {

            Set<Integer> seances = userMap.get(similarUser);

            for (Integer s : seances) {
                if (!targetSeances.contains(s)) {
                    recommendedIds.add(s);
                }
            }
        }

        List<Seance> recommended =
                seanceService.getByIds(recommendedIds);

        return sortByUserPreference(recommended, userId);
    }

    private double computeJaccard(Set<Integer> a, Set<Integer> b) {

        Set<Integer> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<Integer> union = new HashSet<>(a);
        union.addAll(b);

        return union.isEmpty() ? 0 :
                (double) intersection.size() / union.size();
    }

    private List<Seance> sortByUserPreference(List<Seance> seances, int userId) throws Exception {

        UserProfile profile =
                analyticsService.analyzeUser(userId);

        if (profile == null) return seances;

        return seances.stream()
                .sorted((a, b) ->
                        score(b, profile) - score(a, profile))
                .collect(java.util.stream.Collectors.toList());
    }

    private int score(Seance s, UserProfile profile) {

        int score = 0;

        if (s.getIdCoach() == profile.getFavoriteCoach())
            score += 5;

        if (profile.getPreferredTime().equals("MATIN")
                && s.getHeureDebut().isBefore(LocalTime.NOON))
            score += 3;

        if (profile.getPreferredTime().equals("APRES_MIDI")
                && s.getHeureDebut().isAfter(LocalTime.NOON))
            score += 3;

        return score;
    }
}