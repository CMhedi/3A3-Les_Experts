package Entities;

import enums.RoleUser;
import java.time.LocalDateTime;

public class UserApp {

    private int idUser;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String imageUrl;
    private RoleUser role;
    private String motDePasse;
    private LocalDateTime dateCreation;

    // ===== Jdid: Attributs Spécifiques Coach =====
    private int age;
    private String experience;
    private String specialite; // Tnajem t-rod'ha Enum ba3d ken t-7eb
    private String bioCertifs;
    private String disponibilite;
    private boolean faceAuthEnabled;
    private String zoomAccessToken;
    private String zoomRefreshToken;
    private long zoomTokenExpiry;
    public UserApp() {}

    // ===== Getters & Setters Standard =====

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public RoleUser getRole() { return role; }
    public void setRole(RoleUser role) { this.role = role; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    // ===== Jdid: Getters & Setters Coach =====

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getBioCertifs() { return bioCertifs; }
    public void setBioCertifs(String bioCertifs) { this.bioCertifs = bioCertifs; }

    public String getDisponibilite() { return disponibilite; }
    public void setDisponibilite(String disponibilite) { this.disponibilite = disponibilite; }
    public boolean isFaceAuthEnabled() { return faceAuthEnabled; }
    public void setFaceAuthEnabled(boolean faceAuthEnabled) { this.faceAuthEnabled = faceAuthEnabled; }
    // ===== toString =====
    @Override
    public String toString() {
        return prenom + " " + nom + " | " + role + (role == RoleUser.COACH ? " (" + specialite + ")" : "");
    }


    public String getImage_url() {
        return imageUrl;
    }
}