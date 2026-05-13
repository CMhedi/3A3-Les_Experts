package Services;

import Entities.Inscription;
import Entities.Pack;
import Utiles.MyDB2;

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

    // cache simple pour les valeurs autorisées
    private List<String> cachedAllowedStatuts = null;

    /* =========================
       READ
       ========================= */

    public List<Inscription> getAll() {
        MyDB2.getInstance();
        List<Inscription> list = new ArrayList<>();
        String sql = "SELECT * FROM inscription ORDER BY date_inscription DESC";

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Inscription i = new Inscription();
                i.setIdInscription(rs.getInt("id_inscription"));

                Timestamp ts = rs.getTimestamp("date_inscription");
                i.setDateInscription(ts != null ? ts.toLocalDateTime() : null);

                i.setStatutInscr(rs.getString("statut_inscr"));
                i.setMontantTotal(rs.getBigDecimal("montant_total"));
                i.setNomUser(rs.getString("nom_user"));
                i.setNomPack(rs.getString("nom_pack"));
                i.setIdUser(rs.getInt("id_user"));
                i.setIdPack(rs.getInt("id_pack"));

                try { i.setPaymentGateway(rs.getString("payment_gateway")); }   catch (Exception ignored) {}
                try { i.setPaymentReference(rs.getString("payment_reference")); } catch (Exception ignored) {}
                try { i.setPaymentOrderId(rs.getString("payment_order_id")); }   catch (Exception ignored) {}
                try { i.setPaymentStatus(rs.getString("payment_status")); }      catch (Exception ignored) {}
                try {
                    Timestamp paidTs = rs.getTimestamp("paid_at");
                    i.setPaidAt(paidTs != null ? paidTs.toLocalDateTime() : null);
                } catch (Exception ignored) {}
                try { i.setCardImage(rs.getString("card_image")); } catch (Exception ignored) {}

                list.add(i);
            }
            System.out.println("✅ Inscriptions chargées : " + list.size());

        } catch (Exception e) {
            System.out.println("❌ InscriptionService.getAll : " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /* =========================
       CREATE
       ========================= */

    /**
     * Insère une inscription et retourne l'ID généré.
     * Les colonnes paiement sont insérées en même temps si déjà renseignées sur l'objet.
     */
    public int add(Inscription insc, Pack pack) {
        MyDB2.getInstance();

        if (insc.getDateInscription() == null) insc.setDateInscription(LocalDateTime.now());
        insc.setMontantTotal(computeMontant(pack, insc.getIdUser()));

        String statutDb = coerceStatutToDB(insc.getStatutInscr());

        String sql = """
                INSERT INTO inscription
                  (date_inscription, statut_inscr, montant_total,
                   nom_user, nom_pack, id_user, id_pack,
                   payment_gateway, payment_reference, payment_order_id,
                   payment_status, paid_at, card_image)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;

        int generatedId = -1;
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setTimestamp(1,  Timestamp.valueOf(insc.getDateInscription()));
            ps.setString(2,     statutDb);
            ps.setBigDecimal(3, insc.getMontantTotal());
            ps.setString(4,     insc.getNomUser());
            ps.setString(5,     insc.getNomPack());
            setIntOrNull(ps, 6, insc.getIdUser());
            setIntOrNull(ps, 7, insc.getIdPack());
            ps.setString(8,     insc.getPaymentGateway());
            ps.setString(9,     insc.getPaymentReference());
            ps.setString(10,    insc.getPaymentOrderId());
            ps.setString(11,    insc.getPaymentStatus());
            ps.setTimestamp(12, toTimestamp(insc.getPaidAt()));
            ps.setString(13,    insc.getCardImage());

            int rows = ps.executeUpdate();
            System.out.println("✅ Inscription insérée, lignes : " + rows);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                    insc.setIdInscription(generatedId);
                    System.out.println("✅ ID généré : " + generatedId);
                }
            }

        } catch (Exception e) {
            System.out.println("❌ InscriptionService.add : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return generatedId;
    }

    /* =========================
       UPDATE
       ========================= */

    /**
     * Met à jour les champs métier + les colonnes paiement si elles sont renseignées.
     */
    public void update(Inscription insc, Pack pack) {
        MyDB2.getInstance();

        insc.setMontantTotal(computeMontant(pack, insc.getIdUser()));
        String statutDb = coerceStatutToDB(insc.getStatutInscr());

        String sql = """
                UPDATE inscription SET
                    statut_inscr      = ?,
                    montant_total     = ?,
                    nom_user          = ?,
                    nom_pack          = ?,
                    id_user           = ?,
                    id_pack           = ?,
                    payment_gateway   = ?,
                    payment_reference = ?,
                    payment_order_id  = ?,
                    payment_status    = ?,
                    paid_at           = ?,
                    card_image        = ?
                WHERE id_inscription  = ?
                """;

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1,     statutDb);
            ps.setBigDecimal(2, insc.getMontantTotal());
            ps.setString(3,     insc.getNomUser());
            ps.setString(4,     insc.getNomPack());
            setIntOrNull(ps, 5, insc.getIdUser());
            setIntOrNull(ps, 6, insc.getIdPack());
            ps.setString(7,     insc.getPaymentGateway());
            ps.setString(8,     insc.getPaymentReference());
            ps.setString(9,     insc.getPaymentOrderId());
            ps.setString(10,    insc.getPaymentStatus());
            ps.setTimestamp(11, toTimestamp(insc.getPaidAt()));
            ps.setString(12,    insc.getCardImage());
            ps.setInt(13,       insc.getIdInscription());

            ps.executeUpdate();
            System.out.println("✅ Inscription #" + insc.getIdInscription() + " mise à jour.");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Met à jour uniquement les colonnes paiement (après confirmation sandbox).
     * Appelée depuis InscriptionFormController une fois le paiement simulé réussi.
     */
    public void updatePayment(int idInscription,
                              String gateway,
                              String reference,
                              String orderId,
                              String paymentStatus,
                              LocalDateTime paidAt,
                              String cardImage) {
        MyDB2.getInstance();

        String sql = """
                UPDATE inscription SET
                    payment_gateway   = ?,
                    payment_reference = ?,
                    payment_order_id  = ?,
                    payment_status    = ?,
                    paid_at           = ?,
                    card_image        = ?
                WHERE id_inscription  = ?
                """;

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1,     gateway);
            ps.setString(2,     reference);
            ps.setString(3,     orderId);
            ps.setString(4,     paymentStatus);
            ps.setTimestamp(5,  toTimestamp(paidAt));
            ps.setString(6,     cardImage);
            ps.setInt(7,        idInscription);

            int rows = ps.executeUpdate();
            System.out.println("✅ Paiement mis à jour pour inscription #" + idInscription
                    + " (" + rows + " ligne(s))");

        } catch (Exception e) {
            System.out.println("❌ updatePayment error : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /* =========================
       DELETE
       ========================= */

    public void delete(int id) {
        MyDB2.getInstance();
        String sql = "DELETE FROM inscription WHERE id_inscription=?";
        try (Connection cnx = MyDB2.getConnection();
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
        return prix.subtract(discount).max(BigDecimal.ZERO);
    }

    /* =========================
       STATUT — lecture BDD + coercition
       ========================= */

    public List<String> getAllowedStatutsFromDB() {
        MyDB2.getInstance();
        List<String> values = new ArrayList<>();

        // 1) Lire la définition ENUM si applicable
        String sql = "SHOW COLUMNS FROM inscription LIKE 'statut_inscr'";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String type = rs.getString("Type");
                if (type != null && type.toLowerCase().startsWith("enum(")) {
                    Matcher m = Pattern.compile("'([^']*)'").matcher(type);
                    while (m.find()) values.add(m.group(1));
                }
            }
        } catch (Exception ignored) {}

        // 2) Fallback : valeurs distinctes existantes
        if (values.isEmpty()) {
            String sql2 = "SELECT DISTINCT statut_inscr FROM inscription WHERE statut_inscr IS NOT NULL";
            try (Connection cnx = MyDB2.getConnection();
                 PreparedStatement ps = cnx.prepareStatement(sql2);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String s = rs.getString(1);
                    if (s != null && !s.isBlank()) values.add(s);
                }
            } catch (Exception ignored) {}
        }

        // 3) Fallback final : trois valeurs métier
        if (values.isEmpty())
            values = new ArrayList<>(List.of("EN_ATTENTE", "CONFIRMEE", "ANNULEE"));

        // S'assurer que les trois valeurs métier sont toujours présentes
        for (String required : List.of("EN_ATTENTE", "CONFIRMEE", "ANNULEE")) {
            boolean present = values.stream().anyMatch(v -> v.equalsIgnoreCase(required));
            if (!present) values.add(required);
        }

        return values;
    }

    private String coerceStatutToDB(String wanted) {
        if (wanted == null) wanted = "";
        if (cachedAllowedStatuts == null) {
            cachedAllowedStatuts = getAllowedStatutsFromDB();
            System.out.println("✅ Statuts autorisés (BDD) = " + cachedAllowedStatuts);
        }

        String w = wanted.trim();
        if (w.isEmpty()) return cachedAllowedStatuts.get(0);
        if (cachedAllowedStatuts.contains(w)) return w;

        for (String a : cachedAllowedStatuts)
            if (a != null && a.equalsIgnoreCase(w)) return a;

        String nw = normalize(w);
        for (String a : cachedAllowedStatuts)
            if (a != null && normalize(a).equals(nw)) return a;

        System.out.println("⚠ Statut '" + wanted + "' non autorisé → " + cachedAllowedStatuts.get(0));
        return cachedAllowedStatuts.get(0);
    }

    private String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", "");
        n = n.replaceAll("[^A-Za-z0-9]", "");
        return n.toUpperCase();
    }

    /* =========================
       UTILITAIRES PRIVÉS
       ========================= */

    private Timestamp toTimestamp(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime ldt)     return Timestamp.valueOf(ldt);
        if (o instanceof Timestamp ts)          return ts;
        return null;
    }

    private void setIntOrNull(PreparedStatement ps, int idx, int value) throws SQLException {
        if (value > 0) ps.setInt(idx, value);
        else           ps.setNull(idx, Types.INTEGER);
    }
}