package Services;

import Entities.Inscription;
import Entities.Pack;
import Utiles.MyDB;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InscriptionService {

    private final LoyaltyService loyaltyService = new LoyaltyService();

    // cache بسيط للقيم المسموح بها
    private List<String> cachedAllowedStatuts = null;

    /* =========================
       READ
       ========================= */

    public List<Inscription> getAll() {
        MyDB.getInstance();

        List<Inscription> list = new ArrayList<>();
        String sql = "SELECT * FROM inscription ORDER BY date_inscription DESC";

        try (Connection cnx = MyDB.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Inscription i = new Inscription();
                i.setIdInscription(rs.getInt("id_inscription"));

                Timestamp ts = rs.getTimestamp("date_inscription");
                i.setDateInscription(ts != null ? ts.toLocalDateTime() : null);

                i.setStatutInscr(rs.getString("statut_inscr"));
                i.setMontantTotal(rs.getBigDecimal("montant_total"));

                i.setIdUser(rs.getInt("id_user"));
                i.setIdPack(rs.getInt("id_pack"));

                list.add(i);
            }

            System.out.println("✅ Inscriptions loaded: " + list.size());

        } catch (Exception e) {
            System.out.println("❌ InscriptionService.getAll error: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    /* =========================
       CREATE
       ========================= */

    public void add(Inscription insc, Pack pack) {
        MyDB.getInstance();

        if (insc.getDateInscription() == null) insc.setDateInscription(LocalDateTime.now());

        // montant auto
        insc.setMontantTotal(computeMontant(pack, insc.getIdUser()));

        // ✅ أهم سطر: نطابق statut مع DB
        String statutDb = coerceStatutToDB(insc.getStatutInscr());

        String sql = """
            INSERT INTO inscription(date_inscription, statut_inscr, montant_total, id_user, id_pack)
            VALUES (?,?,?,?,?)
        """;

        try (Connection cnx = MyDB.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(insc.getDateInscription()));
            ps.setString(2, statutDb); // ✅ القيمة اللي DB تقبلها
            ps.setBigDecimal(3, insc.getMontantTotal());
            ps.setInt(4, insc.getIdUser());
            ps.setInt(5, insc.getIdPack());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* =========================
       UPDATE
       ========================= */

    public void update(Inscription insc, Pack pack) {
        MyDB.getInstance();

        insc.setMontantTotal(computeMontant(pack, insc.getIdUser()));

        // ✅ نفس الفكرة في update
        String statutDb = coerceStatutToDB(insc.getStatutInscr());

        String sql = """
            UPDATE inscription
            SET statut_inscr=?, montant_total=?, id_user=?, id_pack=?
            WHERE id_inscription=?
        """;

        try (Connection cnx = MyDB.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, statutDb);
            ps.setBigDecimal(2, insc.getMontantTotal());
            ps.setInt(3, insc.getIdUser());
            ps.setInt(4, insc.getIdPack());
            ps.setInt(5, insc.getIdInscription());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        MyDB.getInstance();

        String sql = "DELETE FROM inscription WHERE id_inscription=?";
        try (Connection cnx = MyDB.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* =========================
       Loyalty / Montant
       ========================= */

    public int discountPercent(int userId) {
        return loyaltyService.discountPercentForUser(userId);
    }

    public BigDecimal computeMontant(Pack pack, int userId) {
        BigDecimal prix = (pack.getPrixBase() == null) ? BigDecimal.ZERO : pack.getPrixBase();
        int percent = discountPercent(userId);

        BigDecimal discount = prix.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal finalPrix = prix.subtract(discount);
        return finalPrix.max(BigDecimal.ZERO);
    }

    /* =========================
       ✅ STATUT: read allowed values from DB + mapping
       ========================= */

    public List<String> getAllowedStatutsFromDB() {
        MyDB.getInstance();

        List<String> values = new ArrayList<>();

        // 1) try read enum definition
        String sql = "SHOW COLUMNS FROM inscription LIKE 'statut_inscr'";
        try (Connection cnx = MyDB.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String type = rs.getString("Type"); // enum('A','B'...) أو varchar(10)...
                if (type != null && type.toLowerCase().startsWith("enum(")) {
                    Matcher m = Pattern.compile("'([^']*)'").matcher(type);
                    while (m.find()) values.add(m.group(1));
                }
            }
        } catch (Exception ignored) {}

        // 2) fallback: read existing distinct values
        if (values.isEmpty()) {
            String sql2 = "SELECT DISTINCT statut_inscr FROM inscription WHERE statut_inscr IS NOT NULL";
            try (Connection cnx = MyDB.getConnection();
                 PreparedStatement ps = cnx.prepareStatement(sql2);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String s = rs.getString(1);
                    if (s != null && !s.isBlank()) values.add(s);
                }
            } catch (Exception ignored) {}
        }

        // 3) ultimate fallback
        if (values.isEmpty()) values = new ArrayList<>(List.of("EN_ATTENTE"));

        return values;
    }

    private String coerceStatutToDB(String wanted) {
        if (wanted == null) wanted = "";

        // cache
        if (cachedAllowedStatuts == null) {
            cachedAllowedStatuts = getAllowedStatutsFromDB();
            System.out.println("✅ Allowed statuts (DB) = " + cachedAllowedStatuts);
        }

        String w = wanted.trim();
        if (w.isEmpty()) return cachedAllowedStatuts.get(0);

        // 1) exact match
        if (cachedAllowedStatuts.contains(w)) return w;

        // 2) case-insensitive match
        for (String a : cachedAllowedStatuts) {
            if (a != null && a.equalsIgnoreCase(w)) return a;
        }

        // 3) normalized match (remove accents, spaces, underscores, etc.)
        String nw = normalize(w);
        for (String a : cachedAllowedStatuts) {
            if (a == null) continue;
            if (normalize(a).equals(nw)) return a;
        }

        // 4) fallback
        System.out.println("⚠️ Statut '" + wanted + "' not allowed. Using: " + cachedAllowedStatuts.get(0));
        return cachedAllowedStatuts.get(0);
    }

    private String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", "");        // remove accents
        n = n.replaceAll("[^A-Za-z0-9]", "");  // remove non-alphanum
        return n.toUpperCase();
    }
}
