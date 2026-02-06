package Entities;

import enums.CategorieEvenement;
import java.time.LocalDateTime;

public class Evenement {

    private int idEvenement;
    private String titre;
    private String description;
    private CategorieEvenement categorieEvt;
    private LocalDateTime dateEvent;
    private String lieu;
    private int nbPlaces;
    private String statut;
    private String imageUrl;

    public Evenement() {}

    // ===== Getters & Setters =====

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CategorieEvenement getCategorieEvt() {
        return categorieEvt;
    }

    public void setCategorieEvt(CategorieEvenement categorieEvt) {
        this.categorieEvt = categorieEvt;
    }

    public LocalDateTime getDateEvent() {
        return dateEvent;
    }

    public void setDateEvent(LocalDateTime dateEvent) {
        this.dateEvent = dateEvent;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public int getNbPlaces() {
        return nbPlaces;
    }

    public void setNbPlaces(int nbPlaces) {
        this.nbPlaces = nbPlaces;
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

    // ===== toString =====
    @Override
    public String toString() {
        return titre + " | " + categorieEvt + " | " + dateEvent;
    }
}
