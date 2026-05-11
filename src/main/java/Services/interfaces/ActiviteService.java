package Services.interfaces;
import Models.Activite;
import Services.interfaces.IGenericService;
import Models.TopActivite;

import Utiles.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteService implements IGenericService<Activite> {

    private final Connection cnx = MyDB.getInstance().getConnection();

    // ENUM NORMALIZERS
    private String normalizeType(String v) {
        if (v == null) return "SPORT";
        String s = v.trim().toUpperCase().replace(" ", "_");
        return switch (s) {
            case "SPORT", "CAMPING", "INTELECTUEL", "CULTUREL" -> s;
            default -> "SPORT";
        };
    }

    private String normalizeCategorie(String v) {
        if (v == null) return "AUTRE";
        String s = v.trim().toUpperCase().replace(" ", "_");
        return switch (s) {
            case "FITNESS","RUNNING","FOOTBALL","BASKETBALL","TENNIS","NATATION","RANDONNEE","CYCLISME","YOGA","AUTRE" -> s;
            default -> "AUTRE";
        };
    }

    private String normalizeNiveau(String v) {
        if (v == null) return "DEBUTANT";
        String s = v.trim().toUpperCase().replace(" ", "_");
        return switch (s) {
            case "DEBUTANT","INTERMEDIAIRE","AVANCE" -> s;
            default -> "DEBUTANT";
        };
    }

    private String normalizeStatut(String v) {
        if (v == null) return "DISPONIBLE";
        String s = v.trim().toUpperCase().replace(" ", "_");
        return switch (s) {
            case "DISPONIBLE","INDISPONIBLE" -> s;
            default -> "DISPONIBLE";
        };
    }

    // crud

    @Override
    public void add(Activite a) {
        String sql = "INSERT INTO activite (nom, type_activite, categorie_act, niveau_act, prix, statut, image_url, id_pack, date_reservation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, a.getNom());
            pst.setString(2, normalizeType(a.getTypeActivite()));
            pst.setString(3, normalizeCategorie(a.getCategorieAct()));
            pst.setString(4, normalizeNiveau(a.getNiveauAct()));
            pst.setBigDecimal(5, java.math.BigDecimal.valueOf(a.getPrix()));
            pst.setString(6, normalizeStatut(a.getStatut()));
            pst.setString(7, a.getImageUrl()); // can be null
            pst.setNull(8, Types.INTEGER);
            pst.setDate(9, a.getDate());// id_pack is nullable (we ignore it in your model)
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur add activite: " + e.getMessage(), e);
        }
    }

    /** Useful for tests: returns generated id_activite */
    public int addAndReturnId(Activite a) {
        String sql = "INSERT INTO activite (nom, type_activite, categorie_act, niveau_act, prix, statut, image_url, id_pack, date_reservation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, a.getNom());
            pst.setString(2, normalizeType(a.getTypeActivite()));
            pst.setString(3, normalizeCategorie(a.getCategorieAct()));
            pst.setString(4, normalizeNiveau(a.getNiveauAct()));
            pst.setBigDecimal(5, java.math.BigDecimal.valueOf(a.getPrix()));
            pst.setString(6, normalizeStatut(a.getStatut()));
            pst.setString(7, a.getImageUrl());
            pst.setNull(8, Types.INTEGER);
            pst.setDate(9, (Date) a.getDate());
            pst.executeUpdate();

            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new RuntimeException("No generated key returned (id_activite).");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur addAndReturnId activite: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Activite a) {
        String sql = "UPDATE activite " +
                "SET nom=?, type_activite=?, categorie_act=?, niveau_act=?, prix=?, statut=?, image_url=?, date_reservation=? " +
                "WHERE id_activite=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, a.getNom());
            pst.setString(2, normalizeType(a.getTypeActivite()));
            pst.setString(3, normalizeCategorie(a.getCategorieAct()));
            pst.setString(4, normalizeNiveau(a.getNiveauAct()));
            pst.setBigDecimal(5, java.math.BigDecimal.valueOf(a.getPrix()));
            pst.setString(6, normalizeStatut(a.getStatut()));
            pst.setString(7, a.getImageUrl());
            pst.setInt(8, a.getIdActivite());
            pst.setDate(9, (Date) a.getDate());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update activite: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM activite WHERE id_activite=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete activite: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Activite> getAll() {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT id_activite, nom, type_activite, categorie_act, niveau_act, prix, statut, image_url, date_reservation FROM activite";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Activite a = new Activite(
                        rs.getInt("id_activite"),
                        rs.getString("nom"),
                        rs.getString("type_activite"),
                        rs.getString("categorie_act"),
                        rs.getString("niveau_act"),
                        rs.getBigDecimal("prix").doubleValue(),
                        rs.getString("statut"),
                        rs.getString("image_url"),
                        rs.getDate("date_reservation")
                );
                list.add(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getAll activites: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public Activite getById(int id) {
        String sql = "SELECT id_activite, nom, type_activite, categorie_act, niveau_act, prix, statut, image_url, date_reservation " +
                "FROM activite WHERE id_activite=?";

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Activite(
                            rs.getInt("id_activite"),
                            rs.getString("nom"),
                            rs.getString("type_activite"),
                            rs.getString("categorie_act"),
                            rs.getString("niveau_act"),
                            rs.getBigDecimal("prix").doubleValue(),
                            rs.getString("statut"),
                            rs.getString("image_url"),
                            rs.getDate("date_reservation")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getById activite: " + e.getMessage(), e);
        }

        return null;
    }
    public List<TopActivite> getTop3Activites() {
        List<TopActivite> top = new ArrayList<>();

        String sql =
                "SELECT a.id_activite, a.nom, a.type_activite, a.categorie_act, a.niveau_act, " +
                        "       COUNT(r.id_res_act) AS nb_res " +
                        "FROM activite a " +
                        "JOIN reservation_activite r ON r.id_activite = a.id_activite " +
                        "GROUP BY a.id_activite, a.nom, a.type_activite, a.categorie_act, a.niveau_act " +
                        "ORDER BY nb_res DESC " +
                        "LIMIT 3";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                top.add(new TopActivite(
                        rs.getInt("id_activite"),
                        rs.getString("nom"),
                        rs.getString("type_activite"),
                        rs.getString("categorie_act"),
                        rs.getString("niveau_act"),
                        rs.getInt("nb_res")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getTop3Activites: " + e.getMessage(), e);
        }

        return top;
    }


    public List<Activite> searchByFilters(String type, String categorie, String niveau, Double prixMax, Integer limit) {
        List<Activite> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT id_activite, nom, type_activite, categorie_act, niveau_act, prix, statut, image_url, date_reservation " +
                        "FROM activite WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if (type != null && !type.isBlank()) {
            sql.append(" AND type_activite = ?");
            params.add(normalizeType(type));
        }
        if (categorie != null && !categorie.isBlank()) {
            sql.append(" AND categorie_act = ?");
            params.add(normalizeCategorie(categorie));
        }
        if (niveau != null && !niveau.isBlank()) {
            sql.append(" AND niveau_act = ?");
            params.add(normalizeNiveau(niveau));
        }
        if (prixMax != null) {
            sql.append(" AND prix <= ?");
            params.add(prixMax);
        }

        sql.append(" ORDER BY prix ASC LIMIT ?");
        int lim = (limit == null ? 5 : Math.max(1, Math.min(limit, 20)));
        params.add(lim);

        try (PreparedStatement pst = cnx.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pst.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Activite a = new Activite(
                            rs.getInt("id_activite"),
                            rs.getString("nom"),
                            rs.getString("type_activite"),
                            rs.getString("categorie_act"),
                            rs.getString("niveau_act"),
                            rs.getBigDecimal("prix").doubleValue(),
                            rs.getString("statut"),
                            rs.getString("image_url"),
                            rs.getDate("date_reservation")
                    );
                    list.add(a);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur searchByFilters: " + e.getMessage(), e);
        }

        return list;
    }


    //
    public void ajouter(Activite a) { add(a); }
    public void modifier(Activite a) { update(a); }
    public void supprimer(int id) { delete(id); }
    public List<Activite> afficher() { return getAll(); }
}

