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

    public Inscription() {}

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

    @Override
    public String toString() {
        return "Inscription #" + idInscription;
    }
}
