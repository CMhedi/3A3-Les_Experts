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
        String sql = "INSERT INTO reservation_evenement (date_reservation, statut_res, nb_billets, id_evenement) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatutRes().name());
            ps.setInt(3, r.getNbBillets());
            ps.setInt(4, r.getIdEvenement());
            ps.executeUpdate();
        }
    }

    public void update(ReservationEvenement r) throws Exception {
        String sql = "UPDATE reservation_evenement " +
                "SET date_reservation=?, statut_res=?, nb_billets=?, id_evenement=? " +
                "WHERE id_res_evt=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(r.getDateReservation()));
            ps.setString(2, r.getStatutRes().name());
            ps.setInt(3, r.getNbBillets());
            ps.setInt(4, r.getIdEvenement());
            ps.setInt(5, r.getIdResEvt());
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
        String sql = "SELECT r.*, e.titre FROM reservation_evenement r " +
                "JOIN evenement e ON r.id_evenement = e.id_evenement " +
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
                r.setNomEvenement(rs.getString("titre"));
                list.add(r);
            }
        }
        return list;
    }

    public ReservationEvenement getById(int id) throws Exception {
        String sql = "SELECT id_res_evt, date_reservation, statut_res, nb_billets, id_evenement " +
                "FROM reservation_evenement WHERE id_res_evt=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;

                ReservationEvenement r = new ReservationEvenement();
                r.setIdResEvt(rs.getInt("id_res_evt"));

                Timestamp ts = rs.getTimestamp("date_reservation");
                r.setDateReservation(ts == null ? null : ts.toLocalDateTime());

                String statut = rs.getString("statut_res");
                r.setStatutRes(statut == null ? null : StatutReservation.valueOf(statut));

                r.setNbBillets(rs.getInt("nb_billets"));
                r.setIdEvenement(rs.getInt("id_evenement"));
                return r;
            }
        }
    }
}
