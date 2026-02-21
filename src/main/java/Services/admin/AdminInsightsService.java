package Services.admin;

import Services.admin.dto.AdminOverview;
import Services.admin.dto.AlertItem;
import Services.admin.dto.DailyStat;
import Services.admin.dto.TopPackStat;
import Utiles.MyDB;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminInsightsService {

    private final Connection cnx;

    public AdminInsightsService() {
        this.cnx = MyDB.getConnection();
    }

    // ========= PUBLIC API (ADMIN) =========

    public AdminOverview getOverview(LocalDate from, LocalDate to, int topLimit) throws SQLException {
        AdminOverview ov = new AdminOverview();

        fillCountsAndRevenue(ov, from, to);
        ov.setRevenueTrendPercent(calcRevenueTrend(from, to, ov.getTotalRevenue()));

        ov.setTopPacks(getTopPacks(from, to, topLimit));
        ov.setDailySeries(getDailySeries(from, to));

        return ov;
    }

    public List<AlertItem> getAlerts(LocalDate from, LocalDate to, int minRisk, int limit) throws SQLException {
        // ملاحظة: هانا نحسبو alerts “ديناميك” من غير table جديدة.
        String sql =
                "SELECT i.id_inscription, i.date_inscription, i.statut_inscr, i.montant_total, i.id_user, i.id_pack, " +
                        "       u.prenom, u.nom, p.nom AS pack_nom, " +
                        "       (SELECT COUNT(*) FROM inscription i2 " +
                        "         WHERE i2.id_user=i.id_user AND i2.date_inscription >= DATE_SUB(i.date_inscription, INTERVAL 90 DAY)) AS user_total_90d, " +
                        "       (SELECT COUNT(*) FROM inscription i2 " +
                        "         WHERE i2.id_user=i.id_user AND i2.statut_inscr='CANCELED' AND i2.date_inscription >= DATE_SUB(i.date_inscription, INTERVAL 90 DAY)) AS user_canceled_90d, " +
                        "       (SELECT AVG(i3.montant_total) FROM inscription i3 " +
                        "         WHERE i3.id_pack=i.id_pack AND i3.statut_inscr='CONFIRMED' AND i3.date_inscription >= DATE_SUB(i.date_inscription, INTERVAL 30 DAY)) AS pack_avg_30d " +
                        "FROM inscription i " +
                        "JOIN user_app u ON u.id_user=i.id_user " +
                        "JOIN pack p ON p.id_pack=i.id_pack " +
                        "WHERE DATE(i.date_inscription) BETWEEN ? AND ? " +
                        "ORDER BY i.date_inscription DESC " +
                        "LIMIT ?";

        List<AlertItem> alerts = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int inscId = rs.getInt("id_inscription");
                    String statut = rs.getString("statut_inscr");
                    BigDecimal montant = rs.getBigDecimal("montant_total");
                    LocalDateTime dateIns = toLdt(rs.getTimestamp("date_inscription"));

                    int userId = rs.getInt("id_user");
                    int packId = rs.getInt("id_pack");
                    String userName = safe(rs.getString("prenom")) + " " + safe(rs.getString("nom"));
                    String packName = rs.getString("pack_nom");

                    int userTotal90 = rs.getInt("user_total_90d");
                    int userCanceled90 = rs.getInt("user_canceled_90d");
                    BigDecimal packAvg30 = rs.getBigDecimal("pack_avg_30d");

                    RiskResult rr = computeRisk(statut, montant, userTotal90, userCanceled90, packAvg30);

                    if (rr.score >= minRisk) {
                        AlertItem a = new AlertItem();
                        a.setInscriptionId(inscId);
                        a.setRiskScore(rr.score);
                        a.setLevel(levelFromScore(rr.score));
                        a.setSignals(rr.signals);
                        a.setDateInscription(dateIns);

                        a.setStatut(statut);
                        a.setMontant(montant);

                        a.setUserId(userId);
                        a.setUserName(userName.trim());

                        a.setPackId(packId);
                        a.setPackName(packName);

                        alerts.add(a);
                    }
                }
            }
        }

        return alerts;
    }

    // ========= INTERNAL (ANALYTICS) =========

    private void fillCountsAndRevenue(AdminOverview ov, LocalDate from, LocalDate to) throws SQLException {
        String sql =
                "SELECT " +
                        "  COUNT(*) AS total, " +
                        "  SUM(CASE WHEN statut_inscr='CONFIRMED' THEN 1 ELSE 0 END) AS confirmed, " +
                        "  SUM(CASE WHEN statut_inscr='PENDING' THEN 1 ELSE 0 END) AS pending, " +
                        "  SUM(CASE WHEN statut_inscr='CANCELED' THEN 1 ELSE 0 END) AS canceled, " +
                        "  COALESCE(SUM(CASE WHEN statut_inscr='CONFIRMED' THEN montant_total ELSE 0 END), 0) AS revenue " +
                        "FROM inscription " +
                        "WHERE DATE(date_inscription) BETWEEN ? AND ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ov.setTotal(rs.getInt("total"));
                    ov.setConfirmed(rs.getInt("confirmed"));
                    ov.setPending(rs.getInt("pending"));
                    ov.setCanceled(rs.getInt("canceled"));
                    ov.setTotalRevenue(rs.getBigDecimal("revenue"));
                }
            }
        }
    }

    private double calcRevenueTrend(LocalDate from, LocalDate to, BigDecimal currentRevenue) throws SQLException {
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = from.minusDays(days);

        BigDecimal prevRevenue = getRevenue(prevFrom, prevTo);

        if (prevRevenue == null || prevRevenue.compareTo(BigDecimal.ZERO) <= 0) return 0.0;

        BigDecimal diff = currentRevenue.subtract(prevRevenue);
        BigDecimal pct = diff
                .multiply(BigDecimal.valueOf(100))
                .divide(prevRevenue, 2, RoundingMode.HALF_UP);

        return pct.doubleValue();
    }

    private BigDecimal getRevenue(LocalDate from, LocalDate to) throws SQLException {
        String sql =
                "SELECT COALESCE(SUM(CASE WHEN statut_inscr='CONFIRMED' THEN montant_total ELSE 0 END), 0) AS revenue " +
                        "FROM inscription " +
                        "WHERE DATE(date_inscription) BETWEEN ? AND ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("revenue");
            }
        }
        return BigDecimal.ZERO;
    }

    private List<DailyStat> getDailySeries(LocalDate from, LocalDate to) throws SQLException {
        String sql =
                "SELECT DATE(date_inscription) AS d, " +
                        "       COUNT(*) AS c, " +
                        "       COALESCE(SUM(CASE WHEN statut_inscr='CONFIRMED' THEN montant_total ELSE 0 END), 0) AS r " +
                        "FROM inscription " +
                        "WHERE DATE(date_inscription) BETWEEN ? AND ? " +
                        "GROUP BY DATE(date_inscription) " +
                        "ORDER BY d ASC";

        List<DailyStat> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = rs.getDate("d").toLocalDate();
                    int c = rs.getInt("c");
                    BigDecimal r = rs.getBigDecimal("r");
                    list.add(new DailyStat(d, c, r));
                }
            }
        }

        return list;
    }

    private List<TopPackStat> getTopPacks(LocalDate from, LocalDate to, int limit) throws SQLException {
        String sql =
                "SELECT p.id_pack, p.nom AS pack_nom, p.type_pack, " +
                        "       COUNT(*) AS cnt, " +
                        "       COALESCE(SUM(CASE WHEN i.statut_inscr='CONFIRMED' THEN i.montant_total ELSE 0 END), 0) AS revenue " +
                        "FROM inscription i " +
                        "JOIN pack p ON p.id_pack=i.id_pack " +
                        "WHERE DATE(i.date_inscription) BETWEEN ? AND ? " +
                        "GROUP BY p.id_pack, p.nom, p.type_pack " +
                        "ORDER BY cnt DESC " +
                        "LIMIT ?";

        List<TopPackStat> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id_pack");
                    String nom = rs.getString("pack_nom");
                    String type = rs.getString("type_pack");
                    long cnt = rs.getLong("cnt");
                    BigDecimal revenue = rs.getBigDecimal("revenue");
                    list.add(new TopPackStat(id, nom, type, cnt, revenue));
                }
            }
        }

        return list;
    }

    // ========= INTERNAL (RISK ENGINE) =========

    private static class RiskResult {
        int score;
        String signals;
        RiskResult(int score, String signals) {
            this.score = score;
            this.signals = signals;
        }
    }

    private RiskResult computeRisk(String statut, BigDecimal montant, int userTotal90, int userCanceled90, BigDecimal packAvg30) {
        int score = 0;
        List<String> sig = new ArrayList<>();

        // 1) Pending
        if ("PENDING".equalsIgnoreCase(statut)) {
            score += 10;
            sig.add("PENDING");
        }

        // 2) Montant invalid
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            score += 60;
            sig.add("AMOUNT_INVALID");
        }

        // 3) New user (few inscriptions)
        if (userTotal90 <= 1) {
            score += 10;
            sig.add("NEW_USER");
        }

        // 4) Cancel rate
        double cancelRate = (userTotal90 <= 0) ? 0.0 : ((double) userCanceled90 / (double) userTotal90);
        if (cancelRate > 0.60) {
            score += 35;
            sig.add("VERY_HIGH_CANCEL_RATE");
        } else if (cancelRate > 0.40) {
            score += 25;
            sig.add("HIGH_CANCEL_RATE");
        }

        // 5) Outlier compared to pack avg last 30d
        if (montant != null && packAvg30 != null && packAvg30.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal low = packAvg30.multiply(BigDecimal.valueOf(0.60));
            BigDecimal high = packAvg30.multiply(BigDecimal.valueOf(2.00));
            if (montant.compareTo(low) < 0) {
                score += 30;
                sig.add("AMOUNT_TOO_LOW_VS_AVG");
            } else if (montant.compareTo(high) > 0) {
                score += 20;
                sig.add("AMOUNT_TOO_HIGH_VS_AVG");
            }
        }

        // clamp 0..100
        if (score > 100) score = 100;
        if (score < 0) score = 0;

        return new RiskResult(score, String.join(", ", sig));
    }

    private String levelFromScore(int score) {
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private static LocalDateTime toLdt(Timestamp ts) {
        return (ts == null) ? null : ts.toLocalDateTime();
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}
