package Services;

public class CancellationPolicyService {

    public static class PolicyResult {
        public final double penaltyPercent;
        public final boolean refundEligible;
        public final String rule;

        public PolicyResult(double penaltyPercent, boolean refundEligible, String rule) {
            this.penaltyPercent = penaltyPercent;
            this.refundEligible = refundEligible;
            this.rule = rule;
        }
    }

    // hoursBefore: قداش ساعة قبل النشاط
    public PolicyResult compute(int hoursBefore) {
        if (hoursBefore >= 48) return new PolicyResult(0, true, "CANCEL_48H_PLUS");
        if (hoursBefore >= 24) return new PolicyResult(20, true, "CANCEL_24_TO_48H");
        if (hoursBefore >= 1)  return new PolicyResult(50, false, "CANCEL_1_TO_24H");
        return new PolicyResult(100, false, "SAME_DAY_OR_NO_SHOW");
    }
}