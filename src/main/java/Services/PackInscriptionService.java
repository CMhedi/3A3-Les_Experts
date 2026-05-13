package Services;

import Entities.Inscription;
import Entities.Pack;
import Entities.UserApp;
import Utiles.MyDB2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class PackInscriptionService {

    private InscriptionService inscriptionService = new InscriptionService();
    private PackService packService = new PackService();
    private UserService userService = new UserService();
    private PromoEngineService promoService = new PromoEngineService();
    private LoyaltyService loyaltyService = new LoyaltyService();

    public Inscription createInscription(
            int userId,
            int packId,
            int nbPersonnes,
            String promoCode,
            String paymentMethod
    ) throws Exception {

        UserApp user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Pack pack = packService.getById(packId);
        if (pack == null) {
            throw new IllegalArgumentException("Pack not found");
        }

        BigDecimal total = calculateTotal(pack, nbPersonnes, promoCode, userId);

        Inscription inscription = new Inscription();
        inscription.setIdUser(userId);
        inscription.setIdPack(packId);
        inscription.setDateInscription(LocalDateTime.now());
        inscription.setMontantTotal(total);
        inscription.setStatutInscr("EN_ATTENTE");
        inscription.setNomUser(user.getPrenom() + " " + user.getNom());
        inscription.setNomPack(pack.getNom());

        // Utiliser la méthode correcte du service d'inscription
        int inscriptionId = inscriptionService.add(inscription, pack);
        inscription.setIdInscription(inscriptionId);
        System.out.println("✅ Inscription créée avec l'ID: " + inscriptionId);

        loyaltyService.addPoints(userId, (int) total.doubleValue() / 10);

        return inscription;
    }

    public BigDecimal calculateTotal(Pack pack, int nbPersonnes, String promoCode, int userId) throws Exception {
        BigDecimal basePrice = pack.getPrixBase();
        BigDecimal discount = pack.getReduction();

        BigDecimal total = basePrice
                .subtract(discount)
                .multiply(new BigDecimal(nbPersonnes));

        if (promoCode != null && !promoCode.isEmpty()) {
            PromoEngineService.PromoResult pr = promoService.validateAndApply(
                    promoCode, String.valueOf(userId), total.doubleValue(), LocalDate.now());
            if (pr.valid) {
                total = BigDecimal.valueOf(pr.finalAmount).setScale(2, RoundingMode.HALF_UP);
            }
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotal(Pack pack, int nbPersonnes, String promoCode) throws Exception {
        return calculateTotal(pack, nbPersonnes, promoCode, 0);
    }

    public PaymentResult processPayment(
            int inscriptionId,
            String paymentMethod,
            Map<String, String> paymentDetails
    ) throws Exception {

        Inscription inscription = getInscription(inscriptionId);
        if (inscription == null) {
            throw new IllegalArgumentException("Inscription not found");
        }

        PaymentResult result = new PaymentResult();
        result.setInscriptionId(inscriptionId);
        result.setPaymentMethod(paymentMethod);
        result.setAmount(inscription.getMontantTotal());

        try {
            switch (paymentMethod.toUpperCase()) {
                case "CARD":
                    processCardPayment(inscription, paymentDetails, result);
                    break;
                case "MOBILE":
                    processMobilePayment(inscription, paymentDetails, result);
                    break;
                case "BANK":
                    processBankTransfer(inscription, paymentDetails, result);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid payment method");
            }

            if (result.isSuccess()) {
                updateInscriptionStatus(inscriptionId, "CONFIRMEE", result.getTransactionId());
                System.out.println("✅ Payment confirmed for inscription #" + inscriptionId);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }

        return result;
    }

    private void processCardPayment(Inscription insc, Map<String, String> details, PaymentResult result) {
        String cardNumber = details.get("cardNumber");
        String expiry = details.get("expiry");
        String cvv = details.get("cvv");

        if (!isValidCardNumber(cardNumber)) {
            throw new IllegalArgumentException("Invalid card number");
        }
        if (!isValidCVV(cvv)) {
            throw new IllegalArgumentException("Invalid CVV");
        }

        String transactionId = "CARD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        result.setSuccess(true);
        result.setTransactionId(transactionId);
        result.setTimestamp(LocalDateTime.now());
    }

    private void processMobilePayment(Inscription insc, Map<String, String> details, PaymentResult result) {
        String phone = details.get("phone");

        if (!isValidPhoneNumber(phone)) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        String transactionId = "MOBILE-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        result.setSuccess(true);
        result.setTransactionId(transactionId);
        result.setTimestamp(LocalDateTime.now());
    }

    private void processBankTransfer(Inscription insc, Map<String, String> details, PaymentResult result) {
        String reference = "BANK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        result.setSuccess(true);
        result.setTransactionId(reference);
        result.setTimestamp(LocalDateTime.now());
    }

    public Inscription getInscription(int id) throws Exception {
        MyDB2.getInstance();
        String sql = "SELECT * FROM inscription WHERE id_inscription = ?";

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Inscription insc = new Inscription();
                insc.setIdInscription(rs.getInt("id_inscription"));
                insc.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
                insc.setStatutInscr(rs.getString("statut_inscr"));
                insc.setMontantTotal(rs.getBigDecimal("montant_total"));
                insc.setIdUser(rs.getInt("id_user"));
                insc.setIdPack(rs.getInt("id_pack"));
                return insc;
            }
        }

        return null;
    }

    // DEPRECATED: Cette méthode n'est plus utilisée car elle ne gère pas l'AUTO_INCREMENT
    // Utilisez inscriptionService.add(inscription, pack) à la place
    /*
    private void saveInscription(Inscription insc) throws Exception {
        MyDB2.getInstance();
        String sql = """
            INSERT INTO inscription(date_inscription, statut_inscr, montant_total, id_user, id_pack, nom_user, nom_pack)
            VALUES (?,?,?,?,?,?,?)
        """;

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(insc.getDateInscription()));
            ps.setString(2, insc.getStatutInscr());
            ps.setBigDecimal(3, insc.getMontantTotal());
            ps.setInt(4, insc.getIdUser());
            ps.setInt(5, insc.getIdPack());
            ps.setString(6, insc.getNomUser());
            ps.setString(7, insc.getNomPack());

            ps.executeUpdate();
        }
    }
    */

    public void updateInscriptionStatus(int id, String status, String transactionId) throws Exception {
        MyDB2.getInstance();
        String sql = "UPDATE inscription SET statut_inscr = ?, payment_status = 'paid', paid_at = ? WHERE id_inscription = ?";

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, id);

            ps.executeUpdate();
        }
    }

    private boolean isValidCardNumber(String cardNumber) {
        return cardNumber != null && cardNumber.replaceAll("\\s", "").matches("\\d{13,19}");
    }

    private boolean isValidCVV(String cvv) {
        return cvv != null && cvv.matches("\\d{3,4}");
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.replaceAll("[^0-9+]", "").length() >= 8;
    }

    public static class PaymentResult {
        private int inscriptionId;
        private String paymentMethod;
        private BigDecimal amount;
        private boolean success;
        private String transactionId;
        private String error;
        private LocalDateTime timestamp;

        public int getInscriptionId() { return inscriptionId; }
        public void setInscriptionId(int id) { this.inscriptionId = id; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String method) { this.paymentMethod = method; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amt) { this.amount = amt; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean s) { this.success = s; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String id) { this.transactionId = id; }

        public String getError() { return error; }
        public void setError(String err) { this.error = err; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime ts) { this.timestamp = ts; }
    }
}
