package Services;

import Entities.ReservationEvenement;
import enums.StatutReservation;
import Utiles.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationEvenementService {

    private final Connection cnx;

    public ReservationEvenementService() {
        MyDB.getInstance();
        cnx = MyDB.getConnection();
    }

    public void add(ReservationEvenement r) throws Exception {
        String sql = "INSERT INTO reservation_evenement (date_reservation, statut_res, nb_billets, id_evenement, id_user) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatutRes().name());
            ps.setInt(3, r.getNbBillets());
            ps.setInt(4, r.getIdEvenement());
            ps.setInt(5, r.getIdUser());
            ps.executeUpdate();
        }
    }

    public void update(ReservationEvenement r) throws Exception {
        String sql = "UPDATE reservation_evenement " +
                "SET date_reservation=?, statut_res=?, nb_billets=?, id_evenement=?, id_user=? " +
                "WHERE id_res_evt=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatutRes().name());
            ps.setInt(3, r.getNbBillets());
            ps.setInt(4, r.getIdEvenement());
            ps.setInt(5, r.getIdUser());
            ps.setInt(6, r.getIdResEvt());
            ps.executeUpdate();
        }
    }

    public void updateNote(int idUser, int idEvent, int note) throws Exception {
        // Since notes are in a separate table 'event_rating'
        String sql = "INSERT INTO event_rating (id_user, id_evenement, note, created_at) VALUES (?, ?, ?, NOW()) " +
                     "ON DUPLICATE KEY UPDATE note = ?, created_at = NOW()";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idEvent);
            ps.setInt(3, note);
            ps.setInt(4, note);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws Exception {
        String sql = "DELETE FROM reservation_evenement WHERE id_res_evt=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<ReservationEvenement> getAll() throws Exception {
        List<ReservationEvenement> list = new ArrayList<>();
        String sql = "SELECT r.*, e.titre, e.prix, u.nom, u.prenom, " +
                "(SELECT er.note FROM event_rating er WHERE er.id_user = r.id_user AND er.id_evenement = r.id_evenement LIMIT 1) as note " +
                "FROM reservation_evenement r " +
                "JOIN evenement e ON r.id_evenement = e.id_evenement " +
                "JOIN user_app u ON r.id_user = u.id_user " +
                "ORDER BY r.date_reservation DESC";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                ReservationEvenement r = new ReservationEvenement();
                r.setIdResEvt(rs.getInt("id_res_evt"));

                Timestamp ts = rs.getTimestamp("date_reservation");
                r.setDateReservation(ts == null ? null : ts.toLocalDateTime());

                String statut = rs.getString("statut_res");
                r.setStatutRes(statut == null ? null : StatutReservation.valueOf(statut));

                r.setNbBillets(rs.getInt("nb_billets"));
                r.setIdEvenement(rs.getInt("id_evenement"));
                r.setIdUser(rs.getInt("id_user"));
                r.setNomEvenement(rs.getString("titre"));
                r.setPrixUnitaire(rs.getDouble("prix"));
                r.setNomUser(rs.getString("nom") + " " + rs.getString("prenom"));
                r.setNote(rs.getInt("note"));
                list.add(r);
            }
        }
        return list;
    }

    public List<ReservationEvenement> getByUser(int idUser) throws Exception {
        List<ReservationEvenement> list = new ArrayList<>();
        String sql = "SELECT r.*, e.titre, e.prix, u.nom, u.prenom, " +
                "(SELECT er.note FROM event_rating er WHERE er.id_user = r.id_user AND er.id_evenement = r.id_evenement LIMIT 1) as note " +
                "FROM reservation_evenement r " +
                "JOIN evenement e ON r.id_evenement = e.id_evenement " +
                "JOIN user_app u ON r.id_user = u.id_user " +
                "WHERE r.id_user = ? ORDER BY r.date_reservation DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationEvenement r = new ReservationEvenement();
                    r.setIdResEvt(rs.getInt("id_res_evt"));
                    Timestamp ts = rs.getTimestamp("date_reservation");
                    r.setDateReservation(ts == null ? null : ts.toLocalDateTime());
                    String statut = rs.getString("statut_res");
                    r.setStatutRes(statut == null ? null : StatutReservation.valueOf(statut));
                    r.setNbBillets(rs.getInt("nb_billets"));
                    r.setIdEvenement(rs.getInt("id_evenement"));
                    r.setIdUser(rs.getInt("id_user"));
                    r.setNomEvenement(rs.getString("titre"));
                    r.setPrixUnitaire(rs.getDouble("prix"));
                    r.setNomUser(rs.getString("nom") + " " + rs.getString("prenom"));
                    r.setNote(rs.getInt("note"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    public ReservationEvenement getById(int id) throws Exception {
        String sql = "SELECT r.*, e.titre, e.prix, u.nom, u.prenom, " +
                "(SELECT er.note FROM event_rating er WHERE er.id_user = r.id_user AND er.id_evenement = r.id_evenement LIMIT 1) as note " +
                "FROM reservation_evenement r " +
                "JOIN evenement e ON r.id_evenement = e.id_evenement " +
                "JOIN user_app u ON r.id_user = u.id_user " +
                "WHERE r.id_res_evt=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;

                ReservationEvenement r = new ReservationEvenement();
                r.setIdResEvt(rs.getInt("id_res_evt"));
                
                Timestamp ts = rs.getTimestamp("date_reservation");
                r.setDateReservation(ts != null ? ts.toLocalDateTime() : null);
                
                String statut = rs.getString("statut_res");
                r.setStatutRes(statut != null ? StatutReservation.valueOf(statut) : null);
                
                r.setNbBillets(rs.getInt("nb_billets"));
                r.setIdEvenement(rs.getInt("id_evenement"));
                r.setIdUser(rs.getInt("id_user"));
                r.setNomEvenement(rs.getString("titre"));
                r.setPrixUnitaire(rs.getDouble("prix"));
                r.setNomUser(rs.getString("nom") + " " + rs.getString("prenom"));
                return r;
            }
        }
    }

    public ReservationEvenement getByUserAndEvent(int userId, int eventId) throws Exception {
        String sql = "SELECT r.*, e.titre, e.prix, " +
                "(SELECT er.note FROM event_rating er WHERE er.id_user = r.id_user AND er.id_evenement = r.id_evenement LIMIT 1) as note " +
                "FROM reservation_evenement r " +
                "JOIN evenement e ON r.id_evenement = e.id_evenement " +
                "WHERE r.id_user = ? AND r.id_evenement = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ReservationEvenement r = new ReservationEvenement();
                    r.setIdResEvt(rs.getInt("id_res_evt"));
                    r.setIdUser(userId);
                    r.setIdEvenement(eventId);
                    r.setNbBillets(rs.getInt("nb_billets"));
                    r.setNomEvenement(rs.getString("titre"));
                    r.setPrixUnitaire(rs.getDouble("prix"));
                    return r;
                }
            }
        }
        return null;
    }

    public java.util.Map<String, Integer> getUserInterestCategories(int idUser) throws Exception {
        String sql = "SELECT e.categorie_evt, COUNT(*) as count FROM reservation_evenement r " +
                "JOIN evenement e ON r.id_evenement = e.id_evenement " +
                "WHERE r.id_user = ? GROUP BY e.categorie_evt";
        java.util.Map<String, Integer> interests = new java.util.HashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cat = rs.getString("categorie_evt");
                    if (cat != null)
                        interests.put(cat, rs.getInt("count"));
                }
            }
        }
        return interests;
    }

    public java.util.Map<String, Integer> getUserInterestLocations(int idUser) throws Exception {
        String sql = "SELECT e.lieu, COUNT(*) as count FROM reservation_evenement r " +
                "JOIN evenement e ON r.id_evenement = e.id_evenement " +
                "WHERE r.id_user = ? GROUP BY e.lieu";
        java.util.Map<String, Integer> locations = new java.util.HashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String lieu = rs.getString("lieu");
                    if (lieu != null)
                        locations.put(lieu, rs.getInt("count"));
                }
            }
        }
        return locations;
    }

    public java.util.Map<Integer, Integer> getGlobalPopularityMap() throws Exception {
        String sql = "SELECT id_evenement, SUM(nb_billets) as total FROM reservation_evenement GROUP BY id_evenement";
        java.util.Map<Integer, Integer> pop = new java.util.HashMap<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                pop.put(rs.getInt("id_evenement"), rs.getInt("total"));
            }
        }
        return pop;
    }

    public java.util.Map<Integer, Double> getAverageRatingsMap() throws Exception {
        String sql = "SELECT id_evenement, AVG(note) as avg_note FROM event_rating WHERE note > 0 GROUP BY id_evenement";
        java.util.Map<Integer, Double> ratings = new java.util.HashMap<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ratings.put(rs.getInt("id_evenement"), rs.getDouble("avg_note"));
            }
        }
        return ratings;
    }

    public java.util.Map<Integer, Integer> getReviewCountMap() throws Exception {
        String sql = "SELECT id_evenement, COUNT(note) as count FROM event_rating WHERE note > 0 GROUP BY id_evenement";
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(rs.getInt("id_evenement"), rs.getInt("count"));
            }
        }
        return counts;
    }
}
