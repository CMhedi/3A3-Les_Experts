package Services.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AlertItem {
    private int inscriptionId;
    private int riskScore;          // 0..100
    private String level;           // LOW / MEDIUM / HIGH
    private String signals;         // "NEW_USER, HIGH_CANCEL_RATE, AMOUNT_OUTLIER"
    private LocalDateTime dateInscription;

    private String statut;
    private BigDecimal montant;

    private int userId;
    private String userName;

    private int packId;
    private String packName;

    public AlertItem() {}

    public int getInscriptionId() { return inscriptionId; }
    public void setInscriptionId(int inscriptionId) { this.inscriptionId = inscriptionId; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSignals() { return signals; }
    public void setSignals(String signals) { this.signals = signals; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getPackId() { return packId; }
    public void setPackId(int packId) { this.packId = packId; }

    public String getPackName() { return packName; }
    public void setPackName(String packName) { this.packName = packName; }

    @Override
    public String toString() {
        return "Alert(insc#" + inscriptionId + ", risk=" + riskScore + ", level=" + level + ", signals=" + signals + ")";
    }
}
