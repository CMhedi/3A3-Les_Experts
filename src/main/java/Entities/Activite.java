package Entities;

import enums.CategorieActivite;
import enums.NiveauActivite;

import java.math.BigDecimal;

public class Activite {

    private int idActivite;
    private String nom;
    private String typeActivite;
    private CategorieActivite categorieAct;
    private NiveauActivite niveauAct;
    private BigDecimal prix;
    private String statut;
    private String imageUrl;
    private int idPack;

    public Activite() {}

    // ===== Getters & Setters =====

    public int getIdActivite() {
        return idActivite;
    }

    public void setIdActivite(int idActivite) {
        this.idActivite = idActivite;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getTypeActivite() {
        return typeActivite;
    }

    public void setTypeActivite(String typeActivite) {
        this.typeActivite = typeActivite;
    }

    public CategorieActivite getCategorieAct() {
        return categorieAct;
    }

    public void setCategorieAct(CategorieActivite categorieAct) {
        this.categorieAct = categorieAct;
    }

    public NiveauActivite getNiveauAct() {
        return niveauAct;
    }

    public void setNiveauAct(NiveauActivite niveauAct) {
        this.niveauAct = niveauAct;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getIdPack() {
        return idPack;
    }

    public void setIdPack(int idPack) {
        this.idPack = idPack;
    }

    // ===== toString =====
    @Override
    public String toString() {
        return nom + " | " + categorieAct + " | " + niveauAct;
    }
}
