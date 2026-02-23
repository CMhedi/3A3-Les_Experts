package controllers;

import Entities.Inscription;
import Entities.Pack;
import Entities.UserApp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DashboardController {

    public List<PackStat> topActivatedPacks(List<Pack> packs, List<Inscription> inscriptions, int topN) {
        Map<Integer, Long> countByPackId = inscriptions.stream()
                .collect(Collectors.groupingBy(Inscription::getIdPack, Collectors.counting()));

        return packs.stream()
                .map(p -> new PackStat(
                        p.getIdPack(),
                        p.getNom(),
                        countByPackId.getOrDefault(p.getIdPack(), 0L),
                        safe(p.getPrixBase()),
                        safe(p.getReduction()),
                        p.getStatutPack() != null ? p.getStatutPack().toString() : "N/A",
                        p.getTypePack() != null ? p.getTypePack().toString() : "N/A"
                ))
                .sorted(Comparator.comparingLong(PackStat::getInscriptionCount).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    public Optional<UserFidelity> mostLoyalUser(List<UserApp> users, List<Inscription> inscriptions) {
        Map<Integer, Long> countByUserId = inscriptions.stream()
                .collect(Collectors.groupingBy(Inscription::getIdUser, Collectors.counting()));

        return users.stream()
                .filter(u -> countByUserId.containsKey(u.getIdUser()))
                .map(u -> new UserFidelity(
                        u.getIdUser(),
                        fullName(u),
                        u.getEmail(),
                        countByUserId.getOrDefault(u.getIdUser(), 0L)
                ))
                .max(Comparator.comparingLong(UserFidelity::getTotalInscriptions));
    }

    public List<PackSuccessScore> predictedBestSuccessPacks(List<Pack> packs, List<Inscription> inscriptions, int topN) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last30 = now.minusDays(30);

        Map<Integer, List<Inscription>> byPack = inscriptions.stream()
                .collect(Collectors.groupingBy(Inscription::getIdPack));

        return packs.stream()
                .map(pack -> {
                    List<Inscription> list = byPack.getOrDefault(pack.getIdPack(), List.of());
                    long total = list.size();

                    long converted = list.stream()
                            .filter(i -> isConvertedStatus(i.getStatutInscr()))
                            .count();

                    double conversionRate = total == 0 ? 0.0 : (converted * 1.0 / total);

                    long recent = list.stream()
                            .filter(i -> i.getDateInscription() != null && !i.getDateInscription().isBefore(last30))
                            .count();

                    double score = (0.60 * Math.log1p(total)) + (0.25 * conversionRate) + (0.15 * Math.log1p(recent));

                    BigDecimal revenuTotal = list.stream()
                            .map(Inscription::getMontantTotal)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new PackSuccessScore(
                            pack.getIdPack(),
                            pack.getNom(),
                            safe(pack.getPrixBase()),
                            safe(pack.getReduction()),
                            pack.getStatutPack() != null ? pack.getStatutPack().toString() : "N/A",
                            total,
                            round2(conversionRate),
                            recent,
                            revenuTotal,
                            round4(score)
                    );
                })
                .sorted(Comparator.comparingDouble(PackSuccessScore::getScore).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    public List<InscriptionFeed> lastInscriptions(List<UserApp> users, List<Pack> packs, List<Inscription> inscriptions, int limit) {
        Map<Integer, UserApp> userById = users.stream()
                .collect(Collectors.toMap(UserApp::getIdUser, Function.identity(), (a, b) -> a));
        Map<Integer, Pack> packById = packs.stream()
                .collect(Collectors.toMap(Pack::getIdPack, Function.identity(), (a, b) -> a));

        return inscriptions.stream()
                .sorted(Comparator.comparing(Inscription::getDateInscription, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(i -> {
                    UserApp u = userById.get(i.getIdUser());
                    Pack p = packById.get(i.getIdPack());
                    return new InscriptionFeed(
                            i.getIdInscription(),
                            u != null ? fullName(u) : "Unknown user",
                            u != null ? u.getEmail() : "N/A",
                            p != null ? p.getNom() : "Unknown pack",
                            i.getStatutInscr(),
                            i.getMontantTotal() != null ? i.getMontantTotal() : BigDecimal.ZERO,
                            i.getDateInscription()
                    );
                })
                .collect(Collectors.toList());
    }

    public KpiStats computeKpis(List<Inscription> inscriptions) {
        LocalDateTime last30 = LocalDateTime.now().minusDays(30);

        long total30 = inscriptions.stream()
                .filter(i -> i.getDateInscription() != null && !i.getDateInscription().isBefore(last30))
                .count();

        BigDecimal revenue = inscriptions.stream()
                .map(Inscription::getMontantTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new KpiStats(total30, revenue);
    }

    private static String fullName(UserApp u) {
        String p = u.getPrenom() == null ? "" : u.getPrenom().trim();
        String n = u.getNom() == null ? "" : u.getNom().trim();
        String fn = (p + " " + n).trim();
        return fn.isEmpty() ? "User#" + u.getIdUser() : fn;
    }

    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static boolean isConvertedStatus(String statutInscr) {
        if (statutInscr == null) return false;
        String s = statutInscr.trim().toUpperCase();
        return s.equals("PAYE") || s.equals("PAYÉ")
                || s.equals("CONFIRME") || s.equals("CONFIRMÉ")
                || s.equals("VALIDEE") || s.equals("VALIDÉE");
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    private static double round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    // ===== DTOs =====
    public static class KpiStats {
        private final long totalInscrLast30Days;
        private final BigDecimal revenueTotal;
        public KpiStats(long totalInscrLast30Days, BigDecimal revenueTotal) {
            this.totalInscrLast30Days = totalInscrLast30Days;
            this.revenueTotal = revenueTotal;
        }
        public long getTotalInscrLast30Days() { return totalInscrLast30Days; }
        public BigDecimal getRevenueTotal() { return revenueTotal; }
    }

    public static class PackStat {
        private final int idPack;
        private final String packName;
        private final long inscriptionCount;
        private final BigDecimal prixBase;
        private final BigDecimal reduction;
        private final String statutPack;
        private final String typePack;
        public PackStat(int idPack, String packName, long inscriptionCount,
                        BigDecimal prixBase, BigDecimal reduction, String statutPack, String typePack) {
            this.idPack = idPack; this.packName = packName; this.inscriptionCount = inscriptionCount;
            this.prixBase = prixBase; this.reduction = reduction; this.statutPack = statutPack; this.typePack = typePack;
        }
        public int getIdPack() { return idPack; }
        public String getPackName() { return packName; }
        public long getInscriptionCount() { return inscriptionCount; }
        public BigDecimal getPrixBase() { return prixBase; }
        public BigDecimal getReduction() { return reduction; }
        public String getStatutPack() { return statutPack; }
        public String getTypePack() { return typePack; }
    }

    public static class UserFidelity {
        private final int idUser;
        private final String fullName;
        private final String email;
        private final long totalInscriptions;
        public UserFidelity(int idUser, String fullName, String email, long totalInscriptions) {
            this.idUser = idUser; this.fullName = fullName; this.email = email; this.totalInscriptions = totalInscriptions;
        }
        public int getIdUser() { return idUser; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public long getTotalInscriptions() { return totalInscriptions; }
    }

    public static class PackSuccessScore {
        private final int idPack;
        private final String packName;
        private final BigDecimal prixBase;
        private final BigDecimal reduction;
        private final String statutPack;
        private final long totalInscriptions;
        private final double conversionRate;
        private final long recentInscriptions;
        private final BigDecimal revenuTotal;
        private final double score;

        public PackSuccessScore(int idPack, String packName,
                                BigDecimal prixBase, BigDecimal reduction, String statutPack,
                                long totalInscriptions, double conversionRate, long recentInscriptions,
                                BigDecimal revenuTotal, double score) {
            this.idPack = idPack; this.packName = packName; this.prixBase = prixBase; this.reduction = reduction;
            this.statutPack = statutPack; this.totalInscriptions = totalInscriptions; this.conversionRate = conversionRate;
            this.recentInscriptions = recentInscriptions; this.revenuTotal = revenuTotal; this.score = score;
        }

        public int getIdPack() { return idPack; }
        public String getPackName() { return packName; }
        public BigDecimal getPrixBase() { return prixBase; }
        public BigDecimal getReduction() { return reduction; }
        public String getStatutPack() { return statutPack; }
        public long getTotalInscriptions() { return totalInscriptions; }
        public double getConversionRate() { return conversionRate; }
        public long getRecentInscriptions() { return recentInscriptions; }
        public BigDecimal getRevenuTotal() { return revenuTotal; }
        public double getScore() { return score; }
    }

    public static class InscriptionFeed {
        private final int idInscription;
        private final String userFullName;
        private final String userEmail;
        private final String packName;
        private final String statutInscr;
        private final BigDecimal montantTotal;
        private final LocalDateTime dateInscription;

        public InscriptionFeed(int idInscription, String userFullName, String userEmail,
                               String packName, String statutInscr, BigDecimal montantTotal, LocalDateTime dateInscription) {
            this.idInscription = idInscription; this.userFullName = userFullName; this.userEmail = userEmail;
            this.packName = packName; this.statutInscr = statutInscr; this.montantTotal = montantTotal; this.dateInscription = dateInscription;
        }

        public int getIdInscription() { return idInscription; }
        public String getUserFullName() { return userFullName; }
        public String getUserEmail() { return userEmail; }
        public String getPackName() { return packName; }
        public String getStatutInscr() { return statutInscr; }
        public BigDecimal getMontantTotal() { return montantTotal; }
        public LocalDateTime getDateInscription() { return dateInscription; }
    }
}
