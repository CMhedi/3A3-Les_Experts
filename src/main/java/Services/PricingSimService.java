package Services;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class PricingSimService {

    public static class RuleImpact {
        public final String code;
        public final String impact;
        public RuleImpact(String code, String impact) {
            this.code = code;
            this.impact = impact;
        }
        @Override public String toString() { return code + " (" + impact + ")"; }
    }

    public static class Quote {
        public final double basePrice;
        public final double finalPrice;
        public final double occupancy;
        public final int leadHours;
        public final DayOfWeek day;
        public final List<RuleImpact> rules;

        public Quote(double basePrice, double finalPrice, double occupancy, int leadHours, DayOfWeek day, List<RuleImpact> rules) {
            this.basePrice = basePrice;
            this.finalPrice = finalPrice;
            this.occupancy = occupancy;
            this.leadHours = leadHours;
            this.day = day;
            this.rules = rules;
        }
    }

    public Quote simulate(double basePrice, double occupancy, DayOfWeek day, int leadHours) {
        double price = basePrice;
        List<RuleImpact> rules = new ArrayList<>();

        // Occupancy yield
        if (occupancy >= 0.80) { price *= 1.20; rules.add(new RuleImpact("OCCUPANCY_VERY_HIGH", "+20%")); }
        else if (occupancy >= 0.60) { price *= 1.10; rules.add(new RuleImpact("OCCUPANCY_HIGH", "+10%")); }
        else if (occupancy <= 0.20) { price *= 0.90; rules.add(new RuleImpact("OCCUPANCY_LOW", "-10%")); }

        // Weekend
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            price *= 1.10;
            rules.add(new RuleImpact("WEEKEND", "+10%"));
        }

        // Lead time (last-minute / early-bird)
        if (leadHours <= 12) { price *= 1.08; rules.add(new RuleImpact("LAST_MINUTE", "+8%")); }
        else if (leadHours >= 240) { price *= 0.92; rules.add(new RuleImpact("EARLY_BIRD", "-8%")); }

        return new Quote(basePrice, round2(price), occupancy, leadHours, day, rules);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}