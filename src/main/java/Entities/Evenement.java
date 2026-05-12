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
    private double prix; // Prix de l'événement (DECIMAL 10,2)
    private double relevanceScore = 0.0; // Transient field for recommendation scoring

    public Evenement() {
    }

    // ===== Getters & Setters =====

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

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

    private int nbOccupiedPlaces = 0; // Calculé dynamiquement via les réservations

    public int getNbOccupiedPlaces() {
        return nbOccupiedPlaces;
    }

    public void setNbOccupiedPlaces(int nbOccupiedPlaces) {
        this.nbOccupiedPlaces = nbOccupiedPlaces;
    }

    public int getPlacesRestantes() {
        return Math.max(0, nbPlaces - nbOccupiedPlaces);
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
