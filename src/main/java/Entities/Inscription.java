package Entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Inscription {
    private int idInscription;
    private LocalDateTime dateInscription;
    private String statutInscr;
    private BigDecimal montantTotal;
    private int idUser;
    private int idPack;

    // ✅ Zid hedhom bech t-affichi el assemi fil-Tableau
    private String nomUser;
    private String nomPack;

    // ✅ NOUVEAUX CHAMPS DE PAIEMENT (13-05-2026)
    private String paymentGateway;      // ex: "CARD", "MOBILE", "BANK"
    private String paymentReference;   // ex: "TXN-123456789"
    private String paymentOrderId;     // ex: "ORDER-987654"
    private String paymentStatus;      // ex: "paid", "pending", "failed"
    private LocalDateTime paidAt;      // Date/heure du paiement
    private String cardImage;          // Image ou hash de la carte

    public Inscription() {}

    // ✅ GETTERS & SETTERS - CHAMPS ORIGINAUX
    public String getNomUser() { return nomUser; }
    public void setNomUser(String nomUser) { this.nomUser = nomUser; }

    public String getNomPack() { return nomPack; }
    public void setNomPack(String nomPack) { this.nomPack = nomPack; }

    public int getIdInscription() { return idInscription; }
    public void setIdInscription(int idInscription) { this.idInscription = idInscription; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    public String getStatutInscr() { return statutInscr; }
    public void setStatutInscr(String statutInscr) { this.statutInscr = statutInscr; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public int getIdPack() { return idPack; }
    public void setIdPack(int idPack) { this.idPack = idPack; }

    // ✅ GETTERS & SETTERS - CHAMPS DE PAIEMENT (NOUVEAUX)
    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getPaymentOrderId() { return paymentOrderId; }
    public void setPaymentOrderId(String paymentOrderId) { this.paymentOrderId = paymentOrderId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getCardImage() { return cardImage; }
    public void setCardImage(String cardImage) { this.cardImage = cardImage; }

    @Override
    public String toString() {
        return "Inscription #" + idInscription + " - " + statutInscr + " - TND " + montantTotal;
    }
}
