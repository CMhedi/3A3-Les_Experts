package Services;

import Entities.Pack;
import Entities.Activite;
import Utiles.MyDB2;
import enums.StatutPack;
import enums.TypePack;
import enums.CategorieActivite;
import enums.NiveauActivite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PackServiceUser {

    // ─────────────────────────────────────────────────────────
    // Retourne tous les packs actifs (statut_pack = 'ACTIF')
    // Table : pack
    // ─────────────────────────────────────────────────────────
    public List<Pack> getActivePacks() throws SQLException {
        String sql = "SELECT * FROM pack WHERE UPPER(statut_pack)='ACTIF' ORDER BY id_pack DESC";
        List<Pack> out = new ArrayList<>();

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(mapPack(rs));
            }
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────
    // AJOUT : retourne les activités disponibles d'un pack
    // Table : activite
    // WHERE id_pack = ? AND UPPER(statut) = 'DISPONIBLE'
    // ─────────────────────────────────────────────────────────
    public List<Activite> getActivitesByPack(int idPack) throws SQLException {
        String sql = "SELECT * FROM activite " +
                "WHERE id_pack = ? AND UPPER(statut) = 'DISPONIBLE' " +
                "ORDER BY id_activite ASC";
        List<Activite> out = new ArrayList<>();

        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, idPack);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapActivite(rs));
                }
            }
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────
    // Compte les inscriptions pour un pack donné
    // Table : inscription
    // ─────────────────────────────────────────────────────────
    public int countInscriptionsForPack(int idPack) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inscription WHERE id_pack=?";
        try (Connection cnx = MyDB2.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPack);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // Helpers de mapping ResultSet → Entité
    // ─────────────────────────────────────────────────────────

    private Pack mapPack(ResultSet rs) throws SQLException {
        Pack p = new Pack();
        p.setIdPack(rs.getInt("id_pack"));
        p.setNom(rs.getString("nom"));
        p.setPrixBase(rs.getBigDecimal("prix_base"));
        p.setReduction(rs.getBigDecimal("reduction"));
        p.setNbActivitesMax(rs.getInt("nb_activites_max"));

        // TypePack : enum → valueOf avec protection
        try {
            p.setTypePack(TypePack.valueOf(rs.getString("type_pack").toUpperCase()));
        } catch (Exception ignored) {}

        // StatutPack : enum → valueOf avec protection
        try {
            p.setStatutPack(StatutPack.valueOf(rs.getString("statut_pack").toUpperCase()));
        } catch (Exception ignored) {}

        return p;
    }

    private Activite mapActivite(ResultSet rs) throws SQLException {
        Activite a = new Activite();
        a.setIdActivite(rs.getInt("id_activite"));
        a.setNom(rs.getString("nom"));
        a.setTypeActivite(rs.getString("type_activite"));
        a.setPrix(rs.getBigDecimal("prix"));
        a.setStatut(rs.getString("statut"));
        a.setImageUrl(rs.getString("image_url"));
        int idPack = rs.getInt("id_pack");
        if (rs.wasNull()) {
            a.setIdPack(0);
        } else {
            a.setIdPack(idPack);
        }
        a.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
        a.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);

        // CategorieActivite : enum → valueOf avec protection
        try {
            a.setCategorieAct(CategorieActivite.valueOf(
                    rs.getString("categorie_act").toUpperCase()
            ));
        } catch (Exception ignored) {}

        // NiveauActivite : enum → valueOf avec protection
        try {
            a.setNiveauAct(NiveauActivite.valueOf(
                    rs.getString("niveau_act").toUpperCase()
            ));
        } catch (Exception ignored) {}

        return a;
    }
}