package Services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

// ⚠️ change si ton package est différent
import Utiles.MyDB;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.Base64;

public class TicketService {

    private static final String TICKET_SECRET =
            System.getenv().getOrDefault("TICKET_SECRET", "CHANGE_ME_SUPER_SECRET_123");

    private final Connection cnx;

    public TicketService() {
        this.cnx = MyDB.getInstance().getConnection();
    }

    // ========================= GENERATE =========================

    public TicketResult generateTicket(int reservationId) throws Exception {
        ReservationInfo info = loadReservationInfo(reservationId);

        if (!"CONFIRMEE".equalsIgnoreCase(info.statut)) {
            throw new IllegalStateException("Ticket possible uniquement si réservation CONFIRMEE.");
        }

        String token = createSignedToken(reservationId, info.userId, info.activiteId);
        saveToken(reservationId, token);

        byte[] png = generateQrPngBytes("ECOA|" + token, 320, 320);
        return new TicketResult(reservationId, token, png);
    }

    // ========================= VERIFY + DETAILS =========================

    public VerifyDetailsResult verifyAndCheckInWithDetails(String token) throws Exception {
        ParsedToken pt = verifySignedToken(token);

        // 1) vérifier ticket_token existe + état used
        String checkSql = """
            SELECT ra.id_res_act, ra.checked_in
            FROM reservation_activite ra
            WHERE ra.id_res_act = ? AND ra.ticket_token = ?
        """;

        boolean alreadyUsed;
        try (PreparedStatement ps = cnx.prepareStatement(checkSql)) {
            ps.setInt(1, pt.reservationId);
            ps.setString(2, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new VerifyDetailsResult(false, false, null);
                }
                alreadyUsed = rs.getInt("checked_in") == 1;
            }
        }

        // 2) si pas utilisé -> check-in atomique
        if (!alreadyUsed) {
            String up = """
                UPDATE reservation_activite
                SET checked_in = 1, checkin_time = NOW()
                WHERE id_res_act = ? AND ticket_token = ? AND checked_in = 0
            """;
            try (PreparedStatement ps = cnx.prepareStatement(up)) {
                ps.setInt(1, pt.reservationId);
                ps.setString(2, token);
                int updated = ps.executeUpdate();
                if (updated == 0) alreadyUsed = true;
            }
        }

        // 3) charger détails (après checkin)
        ReservationDetails details = loadReservationDetails(pt.reservationId);

        return new VerifyDetailsResult(true, alreadyUsed, details);
    }

    // ========================= DB HELPERS =========================

    private ReservationInfo loadReservationInfo(int reservationId) throws SQLException {
        String sql = """
            SELECT id_res_act, statut_res, id_user, id_activite
            FROM reservation_activite
            WHERE id_res_act = ?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Réservation introuvable: " + reservationId);
                return new ReservationInfo(
                        rs.getInt("id_res_act"),
                        rs.getString("statut_res"),
                        rs.getInt("id_user"),
                        rs.getInt("id_activite")
                );
            }
        }
    }

    private void saveToken(int reservationId, String token) throws SQLException {
        String up = """
            UPDATE reservation_activite
            SET ticket_token = ?, ticket_generated_at = NOW()
            WHERE id_res_act = ?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(up)) {
            ps.setString(1, token);
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        }
    }

    private ReservationDetails loadReservationDetails(int reservationId) throws SQLException {

        String sql = """
            SELECT
                ra.id_res_act,
                ra.statut_res,
                ra.nb_personnes,
                ra.id_user,
                ra.id_activite,
                ra.checked_in,
                ra.checkin_time,
                a.nom AS activite_nom,
                a.type_activite,
                a.categorie_act,
                a.niveau_act,
                a.prix
            FROM reservation_activite ra
            JOIN activite a ON a.id_activite = ra.id_activite
            WHERE ra.id_res_act = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Détails introuvables pour id_res_act=" + reservationId);

                double prix = rs.getDouble("prix");
                int nb = rs.getInt("nb_personnes");
                double total = prix * nb;

                Timestamp t = rs.getTimestamp("checkin_time");
                String checkTime = (t == null) ? null : t.toString();

                return new ReservationDetails(
                        rs.getInt("id_res_act"),
                        rs.getString("statut_res"),
                        nb,
                        rs.getInt("id_user"),
                        rs.getInt("id_activite"),
                        rs.getString("activite_nom"),
                        rs.getString("type_activite"),
                        rs.getString("categorie_act"),
                        rs.getString("niveau_act"),
                        prix,
                        total,
                        rs.getInt("checked_in") == 1,
                        checkTime
                );
            }
        }
    }

    // ========================= TOKEN SIGNING =========================
    // token = base64url(payload).base64url(signature)
    // payload = rid|uid|aid|iat

    private String createSignedToken(int rid, int uid, int aid) throws Exception {
        long iat = Instant.now().getEpochSecond();
        String payload = rid + "|" + uid + "|" + aid + "|" + iat;

        String payloadB64 = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        String sigB64 = base64Url(hmacSha256(payloadB64.getBytes(StandardCharsets.UTF_8)));

        return payloadB64 + "." + sigB64;
    }

    private ParsedToken verifySignedToken(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 2) throw new IllegalArgumentException("Token invalide.");

        String payloadB64 = parts[0];
        String sigB64 = parts[1];

        String expectedSig = base64Url(hmacSha256(payloadB64.getBytes(StandardCharsets.UTF_8)));
        if (!constantTimeEquals(sigB64, expectedSig)) {
            throw new IllegalArgumentException("Signature invalide (token falsifié).");
        }

        String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
        String[] p = payload.split("\\|");
        if (p.length != 4) throw new IllegalArgumentException("Payload invalide.");

        int rid = Integer.parseInt(p[0]);
        return new ParsedToken(rid);
    }

    private byte[] hmacSha256(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TICKET_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(data);
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    // ========================= QR PNG =========================

    private byte[] generateQrPngBytes(String text, int w, int h) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, w, h);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        }
    }

    // ========================= DTOs =========================

    public record TicketResult(int reservationId, String token, byte[] pngBytes) {}

    public record VerifyDetailsResult(boolean valid, boolean alreadyUsed, ReservationDetails details) {}

    public record ReservationDetails(
            int reservationId,
            String statut,
            int nbPersonnes,
            int userId,
            int activiteId,
            String activiteNom,
            String typeActivite,
            String categorie,
            String niveau,
            double prixUnitaire,
            double total,
            boolean checkedIn,
            String checkinTime
    ) {}

    private record ReservationInfo(int reservationId, String statut, int userId, int activiteId) {}

    private record ParsedToken(int reservationId) {}
}