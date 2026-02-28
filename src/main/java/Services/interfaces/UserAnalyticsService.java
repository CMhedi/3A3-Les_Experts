package Services.interfaces;

import Entities.Seance;
import model.analytics.UserProfile;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserAnalyticsService {

    private final ReservationSeanceService reservationService = new ReservationSeanceService ();
    private final SeanceService seanceService = new SeanceService();

    public UserProfile analyzeUser(int userId) throws Exception {

        List<Seance> reserved =
                reservationService.getConfirmedSeancesByUser(userId);

        if (reserved.isEmpty()) return null;

        Map<Integer, Long> coachCount =
                reserved.stream()
                        .collect(Collectors.groupingBy(
                                Seance::getIdCoach,
                                Collectors.counting()
                        ));

        int favoriteCoach =
                coachCount.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue())
                        .get()
                        .getKey();

        long morning =
                reserved.stream()
                        .filter(s -> s.getHeureDebut().isBefore(LocalTime.NOON))
                        .count();

        long afternoon =
                reserved.size() - morning;

        String preferredTime =
                (morning >= afternoon) ? "MATIN" : "APRES_MIDI";

        return new UserProfile(
                userId,
                favoriteCoach,
                preferredTime
        );
    }
}