package Services;

public class RiskScoreService {

    public static class RiskResult {
        public final int score; // 0..100
        public final String level; // LOW/MED/HIGH
        public final String recommendation;

        public RiskResult(int score, String level, String recommendation) {
            this.score = score;
            this.level = level;
            this.recommendation = recommendation;
        }
    }

    public RiskResult score(int cancels, int noShows, int lastMinuteCancels, int complaints) {

        int s = 100;
        s -= cancels * 8;
        s -= noShows * 25;
        s -= lastMinuteCancels * 12;
        s -= complaints * 10;

        if (s < 0) s = 0;
        if (s > 100) s = 100;

        String level;
        String reco;

        if (s >= 70) { level = "LOW";  reco = "OK: normal workflow"; }
        else if (s >= 40) { level = "MED"; reco = "Require confirmation / shorter deadline"; }
        else { level = "HIGH"; reco = "Require deposit / restrict last-minute bookings"; }

        return new RiskResult(s, level, reco);
    }
}