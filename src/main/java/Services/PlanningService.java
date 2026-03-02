package Services;

import Entities.Planning;
import Services.validation.PlanningValidator;
import Utiles.MyDB;
import enums.StatutPlanning;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PlanningService implements IGenericService<Planning> {

    private Connection conx;

    public PlanningService() {
        MyDB.getInstance();
        conx = MyDB.getConnection();
    }

    // ==========================
    // ADD
    // ==========================
    @Override
    public void add(Planning p) throws SQLException {

        PlanningValidator.validate(p);

        String sql = "INSERT INTO planning (titre, date_debut, date_fin, statut, description) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, p.getTitre());
        ps.setDate(2, Date.valueOf(p.getDateDebut()));
        ps.setDate(3, Date.valueOf(p.getDateFin()));
        ps.setString(4, p.getStatut().name());
        ps.setString(5, p.getDescription());

        ps.executeUpdate();
    }

    // ==========================
    // UPDATE
    // ==========================
    @Override
    public void update(Planning p) throws SQLException {

        PlanningValidator.validate(p);

        String sql = "UPDATE planning SET titre=?, date_debut=?, date_fin=?, statut=?, description=? " +
                "WHERE id_planning=?";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, p.getTitre());
        ps.setDate(2, Date.valueOf(p.getDateDebut()));
        ps.setDate(3, Date.valueOf(p.getDateFin()));
        ps.setString(4, p.getStatut().name());
        ps.setString(5, p.getDescription());
        ps.setInt(6, p.getIdPlanning());

        ps.executeUpdate();
    }

    // ==========================
    // DELETE
    // ==========================
    @Override
    public void delete(int id) throws SQLException {

        String sql = "DELETE FROM planning WHERE id_planning=?";
        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ==========================
    // GET ALL
    // ==========================
    @Override
    public List<Planning> getAll() throws SQLException {

        List<Planning> plannings = new ArrayList<>();

        String sql = "SELECT * FROM planning ORDER BY date_debut DESC";
        Statement st = conx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            Planning p = new Planning();

            p.setIdPlanning(rs.getInt("id_planning"));
            p.setTitre(rs.getString("titre"));
            p.setDateDebut(rs.getDate("date_debut").toLocalDate());
            p.setDateFin(rs.getDate("date_fin").toLocalDate());
            p.setStatut(
                    StatutPlanning.valueOf(
                            rs.getString("statut")
                    )
            );
            p.setDescription(rs.getString("description"));

            plannings.add(p);
        }

        return plannings;
    }

    // ==========================
    // GET BY ID
    // ==========================
    @Override
    public Planning getById(int id) throws SQLException {

        String sql = "SELECT * FROM planning WHERE id_planning=?";
        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {

            Planning p = new Planning();

            p.setIdPlanning(rs.getInt("id_planning"));
            p.setTitre(rs.getString("titre"));
            p.setDateDebut(rs.getDate("date_debut").toLocalDate());
            p.setDateFin(rs.getDate("date_fin").toLocalDate());
            p.setStatut(
                    StatutPlanning.valueOf(
                            rs.getString("statut")
                    )
            );
            p.setDescription(rs.getString("description"));

            return p;
        }

        return null;
    }
    public List<Planning> getPlanningsActifs() throws SQLException {

        List<Planning> plannings = new ArrayList<>();

        String sql = "SELECT * FROM planning WHERE statut='ACTIF' ORDER BY date_debut DESC";

        Statement st = conx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            Planning p = new Planning();

            p.setIdPlanning(rs.getInt("id_planning"));
            p.setTitre(rs.getString("titre"));
            p.setDateDebut(rs.getDate("date_debut").toLocalDate());
            p.setDateFin(rs.getDate("date_fin").toLocalDate());
            p.setStatut(
                    StatutPlanning.valueOf(
                            rs.getString("statut")
                    )
            );
            p.setDescription(rs.getString("description"));

            plannings.add(p);
        }

        return plannings;
    }
}