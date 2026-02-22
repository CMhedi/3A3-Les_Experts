package Services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

// ✅ IMPORTANT: mets ici le bon package de ta classe Database
import Utiles.MyDB;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.Base64;

public class TicketService {

    // ✅ Pas une API key externe: c’est TON secret interne
    private static final String TICKET_SECRET =
            System.getenv().getOrDefault("TICKET_SECRET", "CHANGE_ME_SUPER_SECRET_123");

    private final Connection cnx;

    public TicketService() {
        this.cnx = MyDB.getInstance().getConnection();
    }

    public TicketResult generateTicket(int reservationId) throws Exception {
        ReservationInfo info = loadReservationInfo(reservationId);

        // ✅ règle métier
        if (!"CONFIRMEE".equalsIgnoreCase(info.statut)) {
            throw new IllegalStateException("Ticket possible uniquement si réservation CONFIRMEE.");
        }

        String token = createSignedToken(reservationId, info.userId, info.activiteId);
        saveToken(reservationId, token);

        byte[] png = generateQrPngBytes("ECOA|" + token, 320, 320);

        return new TicketResult(reservationId, token, png);
    }

    public VerifyResult verifyAndCheckIn(String token) throws Exception {
        ParsedToken pt = verifySignedToken(token);

        String sql = """
            SELECT id_res_act, checked_in
            FROM reservation_activite
            WHERE id_res_act = ? AND ticket_token = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, pt.reservationId);
            ps.setString(2, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new VerifyResult(false, false, -1);

                boolean used = rs.getInt("checked_in") == 1;
                if (used) return new VerifyResult(true, true, pt.reservationId);
            }
        }

        String up = """
            UPDATE reservation_activite
            SET checked_in = 1, checkin_time = NOW()
            WHERE id_res_act = ? AND ticket_token = ? AND checked_in = 0
        """;

        try (PreparedStatement ps = cnx.prepareStatement(up)) {
            ps.setInt(1, pt.reservationId);
            ps.setString(2, token);
            int updated = ps.executeUpdate();
            if (updated == 0) return new VerifyResult(true, true, pt.reservationId);
        }

        return new VerifyResult(true, false, pt.reservationId);
    }

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

    // ===== Token signé =====
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

    // ===== QR PNG =====
    private byte[] generateQrPngBytes(String text, int w, int h) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, w, h);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        }
    }

    public record TicketResult(int reservationId, String token, byte[] pngBytes) {}
    public record VerifyResult(boolean valid, boolean alreadyUsed, int reservationId) {}

    private record ReservationInfo(int reservationId, String statut, int userId, int activiteId) {}
    private record ParsedToken(int reservationId) {}
}