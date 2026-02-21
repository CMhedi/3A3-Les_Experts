package Services;

import Entities.Activite;
import Entities.Pack;
import Utiles.MyDB;
import enums.StatutPack;
import enums.StatutReservation;
import enums.TypePack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class PackInscriptionMetierService {

    // Statuts inscription (String car ton Entity Inscription utilise String)
    public static final String INSCR_DRAFT = "EN_ATTENTE";
    public static final String INSCR_PENDING = "EN_ATTENTE";
    public static final String INSCR_PAID = "VALIDEE";
    public static final String INSCR_ACTIVE = "CONFIRMEE";
    public static final String INSCR_CANCELLED = "ANNULEE";

    public record PriceBreakdown(
            BigDecimal packBase,
            BigDecimal activitesTotal,
            BigDecimal discountPack,
            BigDecimal discountGroupe,
            BigDecimal discountCoupon,
            BigDecimal total,
            String details
    ) {}

    public PackInscriptionMetierService() {
        // assure init connexion
        MyDB.getInstance();
    }

    // =============================
    // DATA (packs/activites)
    // =============================
    public List<Pack> getActivePacks() throws SQLException {
        String sql = """
                SELECT id_pack, nom, type_pack, prix_base, reduction, nb_activites_max, statut_pack
                FROM pack
                WHERE statut_pack = 'ACTIF'
                ORDER BY id_pack DESC
                """;
        try (PreparedStatement ps = MyDB.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Pack> list = new ArrayList<>();
            while (rs.next()) {
                Pack p = new Pack();
                p.setIdPack(rs.getInt("id_pack"));
                p.setNom(rs.getString("nom"));
                p.setTypePack(TypePack.valueOf(rs.getString("type_pack")));
                p.setPrixBase(rs.getBigDecimal("prix_base"));
                p.setReduction(rs.getBigDecimal("reduction"));
                p.setNbActivitesMax(rs.getInt("nb_activites_max"));
                p.setStatutPack(StatutPack.valueOf(rs.getString("statut_pack")));
                list.add(p);
            }
            return list;
        }
    }

    public List<Activite> getActivitesByPack(int packId) throws SQLException {
        String sql = """
                SELECT id_activite, nom, type_activite, categorie_act, niveau_act, prix, statut, image_url, id_pack
                FROM activite
                WHERE id_pack = ?
                ORDER BY id_activite DESC
                """;
        try (PreparedStatement ps = MyDB.getConnection().prepareStatement(sql)) {
            ps.setInt(1, packId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Activite> list = new ArrayList<>();
                while (rs.next()) {
                    Activite a = new Activite();
                    a.setIdActivite(rs.getInt("id_activite"));
                    a.setNom(rs.getString("nom"));
                    a.setTypeActivite(rs.getString("type_activite"));

                    String cat = rs.getString("categorie_act");
                    if (cat != null && !cat.isBlank()) {
                        a.setCategorieAct(enums.CategorieActivite.valueOf(cat));
                    }

                    String niv = rs.getString("niveau_act");
                    if (niv != null && !niv.isBlank()) {
                        a.setNiveauAct(enums.NiveauActivite.valueOf(niv));
                    }

                    a.setPrix(rs.getBigDecimal("prix"));
                    a.setStatut(rs.getString("statut"));
                    a.setImageUrl(rs.getString("image_url"));
                    a.setIdPack(rs.getInt("id_pack"));

                    list.add(a);
                }
                return list;
            }
        }
    }

    // =============================
    // METIER 1: Pricing Engine
    // =============================
    public PriceBreakdown computePrice(int packId, List<Integer> activiteIds, int nbPersonnes, String couponCode) throws SQLException {
        if (nbPersonnes <= 0) throw new IllegalArgumentException("nbPersonnes doit être > 0");

        Pack pack = getPackById(packId);
        if (pack == null) throw new IllegalArgumentException("Pack introuvable: " + packId);
        if (pack.getStatutPack() != StatutPack.ACTIF) throw new IllegalStateException("Pack INACTIF: inscription impossible");

        List<Integer> safeIds = (activiteIds == null) ? List.of() : activiteIds;

        validateActivitiesBelongToPack(packId, safeIds);
        if (safeIds.size() > pack.getNbActivitesMax()) {
            throw new IllegalStateException("Max activités dépassé: " + pack.getNbActivitesMax());
        }

        BigDecimal packBase = nz(pack.getPrixBase());
        BigDecimal activitesTotal = sumActivitesPrice(safeIds);

        BigDecimal base = packBase.add(activitesTotal);

        // pack.reduction interprétée comme % (ex: 10 => 10%)
        BigDecimal discountPack = percentOf(base, nz(pack.getReduction()));

        // réduction groupe selon type
        BigDecimal discountGroupe = BigDecimal.ZERO;
        if (pack.getTypePack() == TypePack.GROUPE || pack.getTypePack() == TypePack.ENTREPRISE) {
            if (nbPersonnes >= 10) discountGroupe = percentOf(base, new BigDecimal("12"));
            else if (nbPersonnes >= 5) discountGroupe = percentOf(base, new BigDecimal("7"));
            else if (nbPersonnes >= 2) discountGroupe = percentOf(base, new BigDecimal("3"));
        }

        // coupon optionnel (si table coupon existe, sinon 0)
        BigDecimal discountCoupon = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            discountCoupon = getCouponDiscount(couponCode.trim(), base);
        }

        BigDecimal total = base.subtract(discountPack).subtract(discountGroupe).subtract(discountCoupon);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        String details = "base=" + base +
                " | -pack=" + discountPack +
                " | -groupe=" + discountGroupe +
                " | -coupon=" + discountCoupon;

        return new PriceBreakdown(packBase, activitesTotal, discountPack, discountGroupe, discountCoupon, total, details);
    }

    // =============================
    // METIER 1: Create draft + reservations auto
    // =============================
    public int createInscriptionDraft(int userId, int packId, List<Integer> activiteIds, int nbPersonnes, String couponCode) throws SQLException {

        if (hasOpenInscription(userId, packId)) {
            throw new IllegalStateException("Tu as déjà une inscription ouverte (DRAFT/PENDING) pour ce pack.");
        }

        PriceBreakdown pb = computePrice(packId, activiteIds, nbPersonnes, couponCode);

        Connection cnx = MyDB.getConnection();
        if (cnx == null) throw new SQLException("Connexion DB null (MyDB)");

        try {
            cnx.setAutoCommit(false);

            int idInscription = insertInscription(cnx, userId, packId, pb.total(), INSCR_DRAFT);

            // reservations auto -> EN_ATTENTE
            List<Integer> safeIds = (activiteIds == null) ? List.of() : activiteIds;
            for (Integer idAct : safeIds) {
                insertReservationActivite(cnx, userId, idAct, nbPersonnes, StatutReservation.EN_ATTENTE);
            }

            cnx.commit();
            return idInscription;

        } catch (Exception e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    // =============================
    // Helpers JDBC
    // =============================
    private Pack getPackById(int packId) throws SQLException {
        String sql = """
                SELECT id_pack, nom, type_pack, prix_base, reduction, nb_activites_max, statut_pack
                FROM pack
                WHERE id_pack = ?
                """;
        try (PreparedStatement ps = MyDB.getConnection().prepareStatement(sql)) {
            ps.setInt(1, packId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Pack p = new Pack();
                p.setIdPack(rs.getInt("id_pack"));
                p.setNom(rs.getString("nom"));
                p.setTypePack(TypePack.valueOf(rs.getString("type_pack")));
                p.setPrixBase(rs.getBigDecimal("prix_base"));
                p.setReduction(rs.getBigDecimal("reduction"));
                p.setNbActivitesMax(rs.getInt("nb_activites_max"));
                p.setStatutPack(StatutPack.valueOf(rs.getString("statut_pack")));
                return p;
            }
        }
    }

    private void validateActivitiesBelongToPack(int packId, List<Integer> activiteIds) throws SQLException {
        if (activiteIds == null || activiteIds.isEmpty()) return;

        Set<Integer> set = new HashSet<>(activiteIds);
        if (set.size() != activiteIds.size()) throw new IllegalStateException("Liste d'activités contient des doublons.");

        String in = String.join(",", Collections.nCopies(activiteIds.size(), "?"));
        String sql = "SELECT COUNT(*) AS c FROM activite WHERE id_pack=? AND id_activite IN (" + in + ")";

        try (PreparedStatement ps = MyDB.getConnection().prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, packId);
            for (Integer id : activiteIds) ps.setInt(i++, id);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt("c");
                if (count != activiteIds.size()) {
                    throw new IllegalStateException("Une ou plusieurs activités ne sont pas dans ce pack.");
                }
            }
        }
    }

    private BigDecimal sumActivitesPrice(List<Integer> activiteIds) throws SQLException {
        if (activiteIds == null || activiteIds.isEmpty()) return BigDecimal.ZERO;

        String in = String.join(",", Collections.nCopies(activiteIds.size(), "?"));
        String sql = "SELECT COALESCE(SUM(prix),0) AS s FROM activite WHERE id_activite IN (" + in + ")";

        try (PreparedStatement ps = MyDB.getConnection().prepareStatement(sql)) {
            int i = 1;
            for (Integer id : activiteIds) ps.setInt(i++, id);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return nz(rs.getBigDecimal("s"));
            }
        }
    }

    private boolean hasOpenInscription(int userId, int packId) throws SQLException {
        String sql = """
        SELECT COUNT(*) AS c
        FROM inscription
        WHERE id_user=? AND id_pack=? AND statut_inscr IN (?,?)
        """;
        try (PreparedStatement ps = MyDB.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, packId);
            ps.setString(3, "EN_ATTENTE");
            ps.setString(4, "VALIDEE"); // تعتبرها “ouverte” قبل CONFIRMEE
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("c") > 0;
            }
        }
    }

    private int insertInscription(Connection cnx, int userId, int packId, BigDecimal montant, String statut) throws SQLException {
        String sql = """
                INSERT INTO inscription(date_inscription, statut_inscr, montant_total, id_user, id_pack)
                VALUES(?,?,?,?,?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, statut);
            ps.setBigDecimal(3, montant);
            ps.setInt(4, userId);
            ps.setInt(5, packId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No generated key for inscription");
            }
        }
    }

    private void insertReservationActivite(Connection cnx, int userId, int idActivite, int nbPersonnes, StatutReservation statut) throws SQLException {
        String sql = """
                INSERT INTO reservation_activite(date_reservation, statut_res, nb_personnes, id_user, id_activite)
                VALUES(?,?,?,?,?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, statut.name());
            ps.setInt(3, nbPersonnes);
            ps.setInt(4, userId);
            ps.setInt(5, idActivite);
            ps.executeUpdate();
        }
    }

    // Coupon (OPTIONNEL) : si tu n’as pas la table => retourne 0
    private BigDecimal getCouponDiscount(String code, BigDecimal base) {
        try {
            String sql = "SELECT type, valeur FROM coupon WHERE code=? AND actif=1";
            try (PreparedStatement ps = MyDB.getConnection().prepareStatement(sql)) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return BigDecimal.ZERO;

                    String type = rs.getString("type"); // "PERCENT" ou "FIXED"
                    BigDecimal valeur = rs.getBigDecimal("valeur");

                    if ("PERCENT".equalsIgnoreCase(type)) return percentOf(base, valeur);
                    if ("FIXED".equalsIgnoreCase(type)) return nz(valeur);
                    return BigDecimal.ZERO;
                }
            }
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        if (percent == null) return BigDecimal.ZERO;
        if (percent.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return base.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}