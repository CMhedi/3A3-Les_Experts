package Entities;

import enums.TypePack;
import enums.StatutPack;

import java.math.BigDecimal;

public class Pack {

    private int idPack;
    private String nom;
    private TypePack typePack;
    private BigDecimal prixBase;
    private BigDecimal reduction;
    private int nbActivitesMax;
    private StatutPack statutPack;

    public Pack() {}

    // ===== Getters & Setters =====

    public int getIdPack() {
        return idPack;
    }

    public void setIdPack(int idPack) {
        this.idPack = idPack;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public TypePack getTypePack() {
        return typePack;
    }

    public void setTypePack(TypePack typePack) {
        this.typePack = typePack;
    }

    public BigDecimal getPrixBase() {
        return prixBase;
    }

    public void setPrixBase(BigDecimal prixBase) {
        this.prixBase = prixBase;
    }

    public BigDecimal getReduction() {
        return reduction;
    }

    public void setReduction(BigDecimal reduction) {
        this.reduction = reduction;
    }

    public int getNbActivitesMax() {
        return nbActivitesMax;
    }

    public void setNbActivitesMax(int nbActivitesMax) {
        this.nbActivitesMax = nbActivitesMax;
    }

    public StatutPack getStatutPack() {
        return statutPack;
    }

    public void setStatutPack(StatutPack statutPack) {
        this.statutPack = statutPack;
    }

    // ===== toString =====
    @Override
    public String toString() {
        return nom + " | " + typePack + " | " + statutPack;
    }
}
