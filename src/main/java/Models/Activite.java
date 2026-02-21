package Models;

import java.sql.Date;
import java.time.LocalDate;

public class Activite {
    private int idActivite;
    private String nom;
    private String typeActivite;
    private String categorieAct;
    private String niveauAct;
    private double prix;
    private String statut;
    private String imageUrl;
    private Date date;

    public Activite(int idActivite, String nom, String typeActivite, String categorieAct,
                    String niveauAct, double prix, String statut, String imageUrl, Date date) {
        this.idActivite = idActivite;
        this.nom = nom;
        this.typeActivite = typeActivite;
        this.categorieAct = categorieAct;
        this.niveauAct = niveauAct;
        this.prix = prix;
        this.statut = statut;
        this.imageUrl = imageUrl;
        this.date = date;
    }

    // ===== Getters =====
    public int getIdActivite() { return idActivite; }
    public String getNom() { return nom; }
    public String getTypeActivite() { return typeActivite; }
    public String getCategorieAct() { return categorieAct; }
    public String getNiveauAct() { return niveauAct; }
    public double getPrix() { return prix; }
    public String getStatut() { return statut; }
    public String getImageUrl() { return imageUrl; }
    public Date getDate() { return date; }

    // ===== Setters =====
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }
    public void setNom(String nom) { this.nom = nom; }
    public void setTypeActivite(String typeActivite) { this.typeActivite = typeActivite; }
    public void setCategorieAct(String categorieAct) { this.categorieAct = categorieAct; }
    public void setNiveauAct(String niveauAct) { this.niveauAct = niveauAct; }
    public void setPrix(double prix) { this.prix = prix; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDate(Date date) { this.date = date; }
}
