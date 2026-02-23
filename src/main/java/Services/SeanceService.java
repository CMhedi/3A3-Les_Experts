package Services;

import Entities.Seance;
import enums.StatutSeance;
import Services.validation.SeanceValidator;
import Utiles.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeanceService implements IGenericService<Seance> {

    private Connection conx;

    public SeanceService() {
        MyDB.getInstance();
        conx = MyDB.getConnection();
    }

    // ==========================
    // ADD
    // ==========================
    @Override
    public void add(Seance s) throws SQLException {

        // 🔐 Validation métier AVANT insertion
        SeanceValidator.validate(s);

        String sql = """
    INSERT INTO seance
    (nom, date_seance, heure_debut, heure_fin, capacite,
     statut_seance, id_planning, id_coach)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
""";


        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, s.getNom());
        ps.setDate(2, Date.valueOf(s.getDateSeance()));
        ps.setTime(3, Time.valueOf(s.getHeureDebut()));
        ps.setTime(4, Time.valueOf(s.getHeureFin()));
        ps.setInt(5, s.getCapacite());
        ps.setString(6, s.getStatutSeance().name());
        ps.setInt(7, s.getIdPlanning());
        ps.setInt(8, s.getIdCoach());


        ps.executeUpdate();
    }

    // ==========================
    // UPDATE
    // ==========================
    @Override
    public void update(Seance s) throws SQLException {

        // 🔐 Validation métier AVANT update
        SeanceValidator.validate(s);
        String sql = """
    UPDATE seance SET
    nom=?,
    date_seance=?,
    heure_debut=?,
    heure_fin=?,
    capacite=?,
    statut_seance=?,
    id_planning=?,
    id_coach=?
    WHERE id_seance=?
""";


        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, s.getNom());
        ps.setDate(2, Date.valueOf(s.getDateSeance()));
        ps.setTime(3, Time.valueOf(s.getHeureDebut()));
        ps.setTime(4, Time.valueOf(s.getHeureFin()));
        ps.setInt(5, s.getCapacite());
        ps.setString(6, s.getStatutSeance().name());
        ps.setInt(7, s.getIdPlanning());
        ps.setInt(8, s.getIdCoach());
        ps.setInt(9, s.getIdSeance());

        ps.executeUpdate();
    }

    // ==========================
    // DELETE
    // ==========================
    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM seance WHERE id_seance=?";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ==========================
    // GET ALL
    // ==========================
    @Override
    public List<Seance> getAll() throws SQLException {
        List<Seance> seances = new ArrayList<>();
        String sql = "SELECT * FROM seance";

        Statement st = conx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Seance s = new Seance();
            s.setNom(rs.getString("nom"));
            s.setIdSeance(rs.getInt("id_seance"));
            s.setDateSeance(rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setStatutSeance(
                    StatutSeance.valueOf(rs.getString("statut_seance"))
            );
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setIdCoach(rs.getInt("id_coach"));

            seances.add(s);
        }
        return seances;
    }

    // ==========================
    // GET BY ID
    // ==========================
    @Override
    public Seance getById(int id) throws SQLException {
        String sql = "SELECT * FROM seance WHERE id_seance=?";
        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Seance s = new Seance();
          s.setNom(rs.getString("nom"));
            s.setIdSeance(rs.getInt("id_seance"));
            s.setDateSeance(rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setStatutSeance(
                    StatutSeance.valueOf(rs.getString("statut_seance"))
            );
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setIdCoach(rs.getInt("id_coach"));
            return s;
        }
        return null;
    }
    public List<Seance> getByPlanning(int planningId) throws SQLException {

        List<Seance> seances = new ArrayList<>();

        String sql = "SELECT * FROM seance WHERE id_planning = ?";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, planningId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Seance s = new Seance();
            s.setNom(rs.getString("nom"));
            s.setIdSeance(rs.getInt("id_seance"));
            s.setDateSeance(rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setStatutSeance(
                    StatutSeance.valueOf(
                            rs.getString("statut_seance")
                    )
            );
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setIdCoach(rs.getInt("id_coach"));

            seances.add(s);
        }

        return seances;
    }
    public boolean hasSeanceInPlanning(int planningId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM seance WHERE id_planning = ?";
        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, planningId);

        ResultSet rs = ps.executeQuery();
        rs.next();

        return rs.getInt(1) > 0;
    }
    public List<Seance> getByCoach(int coachId) throws Exception {
        List<Seance> list = new ArrayList<>();

        String sql = "SELECT * FROM seance WHERE id_coach=?";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, coachId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Seance s = new Seance();
            s.setNom(rs.getString("nom"));
            s.setIdSeance(rs.getInt("id_seance"));
            s.setDateSeance(
                    rs.getDate("date_seance").toLocalDate());
            s.setHeureDebut(
                    rs.getTime("heure_debut").toLocalTime());
            s.setHeureFin(
                    rs.getTime("heure_fin").toLocalTime());
            s.setCapacite(rs.getInt("capacite"));
            s.setStatutSeance(
                    StatutSeance.valueOf(
                            rs.getString("statut_seance")));
            s.setIdPlanning(rs.getInt("id_planning"));
            s.setIdCoach(rs.getInt("id_coach"));

            list.add(s);
        }

        return list;
    }


    public List<Seance> getSeancesByUser(int userTestId) {
        List<Seance> seances = new ArrayList<>();

        String sql = """
                SELECT s.* 
                FROM seance s
                JOIN reservation_seance r ON s.id_seance = r.id_seance
                WHERE r.id_user = ?
                AND r.statut = 'CONFIRMEE'
                """;

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userTestId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Seance s = new Seance();
                s.setNom(rs.getString("nom"));
                s.setIdSeance(rs.getInt("id_seance"));
                s.setDateSeance(rs.getDate("date_seance").toLocalDate());
                s.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
                s.setHeureFin(rs.getTime("heure_fin").toLocalTime());
                s.setCapacite(rs.getInt("capacite"));
                s.setStatutSeance(
                        StatutSeance.valueOf(rs.getString("statut_seance"))
                );
                s.setIdPlanning(rs.getInt("id_planning"));
                s.setIdCoach(rs.getInt("id_coach"));

                seances.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return seances;
    }
}
