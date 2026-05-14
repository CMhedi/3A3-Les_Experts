package Services.interfaces;

import Models.Reservation;
import Models.TopActivite;
import Utiles.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationService implements IGenericService<Reservation> {

    private final Connection cnx = MyDB.getInstance().getConnection();

    /** DB : EN_ATTENTE / CONFIRMEE / ANNULEE / SCANNEE / … */
    private String normalizeStatut(String statut) {
        if (statut == null) return "EN_ATTENTE";

        String s = statut.trim();

        if (s.equalsIgnoreCase("En attente")) return "EN_ATTENTE";
        if (s.equalsIgnoreCase("Confirmée") || s.equalsIgnoreCase("Confirmee")) return "CONFIRMEE";
        if (s.equalsIgnoreCase("Annulée") || s.equalsIgnoreCase("Annulee")) return "ANNULEE";
        if (s.equalsIgnoreCase("Scannée") || s.equalsIgnoreCase("Scannee")) return "SCANNEE";

        s = s.toUpperCase().replace(" ", "_");
        if (s.equals("EN_ATTENTE") || s.equals("CONFIRMEE") || s.equals("ANNULEE")
                || s.equals("SCANNEE") || s.equals("TERMINEE") || s.equals("LISTE_ATTENTE")) {
            return s;
        }
        return "EN_ATTENTE";
    }

    private static Reservation mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("date_reservation");
        LocalDateTime dt = ts != null ? ts.toLocalDateTime() : null;
        return new Reservation(
                rs.getInt("id_res_act"),
                dt,
                rs.getString("statut_res"),
                rs.getInt("nb_personnes"),
                rs.getInt("id_user"),
                rs.getInt("id_activite"),
                rs.getString("ville_user")
        );
    }

    @Override
    public void add(Reservation r) {
        String sql = "INSERT INTO reservation_activite (statut_res, nb_personnes, id_user, id_activite, date_reservation, ville_user) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, normalizeStatut(r.getStatut()));
            pst.setInt(2, r.getNbPersonnes());
            pst.setInt(3, r.getIdUser());
            pst.setInt(4, r.getIdActivite());
            LocalDateTime when = r.getDateReservation() != null ? r.getDateReservation() : LocalDateTime.now();
            pst.setTimestamp(5, Timestamp.valueOf(when));
            pst.setString(6, r.getVilleUser());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur add reservation: " + e.getMessage(), e);
        }
    }

    public int addAndReturnId(Reservation r) {
        String sql = "INSERT INTO reservation_activite (statut_res, nb_personnes, id_user, id_activite, date_reservation, ville_user) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, normalizeStatut(r.getStatut()));
            pst.setInt(2, r.getNbPersonnes());
            pst.setInt(3, r.getIdUser());
            pst.setInt(4, r.getIdActivite());
            LocalDateTime when = r.getDateReservation() != null ? r.getDateReservation() : LocalDateTime.now();
            pst.setTimestamp(5, Timestamp.valueOf(when));
            pst.setString(6, r.getVilleUser());
            pst.executeUpdate();
            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new RuntimeException("No generated key returned (id_res_act).");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur addAndReturnId reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Reservation r) {
        String sql = "UPDATE reservation_activite SET statut_res=?, nb_personnes=?, id_user=?, id_activite=?, date_reservation=?, ville_user=? "
                + "WHERE id_res_act=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, normalizeStatut(r.getStatut()));
            pst.setInt(2, r.getNbPersonnes());
            pst.setInt(3, r.getIdUser());
            pst.setInt(4, r.getIdActivite());
            LocalDateTime when = r.getDateReservation() != null ? r.getDateReservation() : LocalDateTime.now();
            pst.setTimestamp(5, Timestamp.valueOf(when));
            pst.setString(6, r.getVilleUser());
            pst.setInt(7, r.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM reservation_activite WHERE id_res_act=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_activite";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getAll reservations: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Reservation getById(int id) {
        String sql = "SELECT * FROM reservation_activite WHERE id_res_act=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getById reservation: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Réservations d’activités pour un utilisateur (JOIN nom activité pour l’affichage client).
     */
    public List<Reservation> findByUserId(int userId) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.id_res_act, r.statut_res, r.nb_personnes, r.id_user, r.id_activite, "
                + "r.date_reservation, r.ville_user, a.nom AS activite_nom "
                + "FROM reservation_activite r "
                + "LEFT JOIN activite a ON a.id_activite = r.id_activite "
                + "WHERE r.id_user = ? "
                + "ORDER BY r.date_reservation DESC, r.id_res_act DESC";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Reservation r = mapRow(rs);
                    String an = rs.getString("activite_nom");
                    r.setActiviteNom(an != null && !an.isBlank() ? an : null);
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByUserId reservation: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<TopActivite> getTop3Activites() {
        return List.of();
    }

    public void ajouter(Reservation r) {
        add(r);
    }

    public void modifier(Reservation r) {
        update(r);
    }

    public void supprimer(int id) {
        delete(id);
    }

    public List<Reservation> afficher() {
        return getAll();
    }
}
