package Services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class PromoEngineService {

    public enum DiscountType { PERCENT, FIXED }

    public static class Promo {
        public final String code;
        public final DiscountType type;
        public final double value;
        public final LocalDate start;
        public final LocalDate end;
        public final double minAmount;
        public final int maxUsesPerUser;

        public Promo(String code, DiscountType type, double value, LocalDate start, LocalDate end, double minAmount, int maxUsesPerUser) {
            this.code = code;
            this.type = type;
            this.value = value;
            this.start = start;
            this.end = end;
            this.minAmount = minAmount;
            this.maxUsesPerUser = maxUsesPerUser;
        }
    }

    public static class PromoResult {
        public final boolean valid;
        public final String reason;
        public final double discountAmount;
        public final double finalAmount;

        public PromoResult(boolean valid, String reason, double discountAmount, double finalAmount) {
            this.valid = valid;
            this.reason = reason;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
        }
    }

    // DB-free: promos hardcoded (تنجم تبدّلهم بسهولة)
    private final Map<String, Promo> promos = new HashMap<>();
    // DB-free usage tracking (runtime فقط)
    private final Map<String, Integer> perUserUsage = new HashMap<>();

    public PromoEngineService() {
        promos.put("ECO10", new Promo("ECO10", DiscountType.PERCENT, 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 30, 3));

        promos.put("WELCOME20", new Promo("WELCOME20", DiscountType.FIXED, 20,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 60, 1));

        promos.put("WEEKEND15", new Promo("WEEKEND15", DiscountType.PERCENT, 15,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 40, 2));
    }

    public PromoResult validateAndApply(String code, String userKey, double amount, LocalDate date) {
        if (code == null || code.trim().isEmpty()) {
            return new PromoResult(false, "EMPTY_CODE", 0, amount);
        }

        Promo p = promos.get(code.trim().toUpperCase());
        if (p == null) return new PromoResult(false, "UNKNOWN_CODE", 0, amount);

        if (date.isBefore(p.start) || date.isAfter(p.end)) return new PromoResult(false, "EXPIRED", 0, amount);
        if (amount < p.minAmount) return new PromoResult(false, "MIN_AMOUNT_NOT_REACHED", 0, amount);

        String usageKey = (userKey == null ? "ANON" : userKey) + "::" + p.code;
        int used = perUserUsage.getOrDefault(usageKey, 0);
        if (used >= p.maxUsesPerUser) return new PromoResult(false, "LIMIT_REACHED", 0, amount);

        double discount = 0;
        if (p.type == DiscountType.PERCENT) discount = amount * (p.value / 100.0);
        else discount = p.value;

        if (discount > amount) discount = amount;

        double finalAmount = round2(amount - discount);

        // We only “consume” when you explicitly call consume(...)
        return new PromoResult(true, "OK", round2(discount), finalAmount);
    }

    public void consume(String code, String userKey) {
        if (code == null || userKey == null) return;
        String usageKey = userKey + "::" + code.trim().toUpperCase();
        perUserUsage.put(usageKey, perUserUsage.getOrDefault(usageKey, 0) + 1);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}