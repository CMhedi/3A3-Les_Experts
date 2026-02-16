package Services.interfaces;

import Entities.Planning;
import Services.interfaces.IGenericService;
import Services.interfaces.validation.PlanningValidator;
import Utiles.MyDB;

import java.sql.*;
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

        // 🔐 Validation métier AVANT insertion
        PlanningValidator.validate(p);

        String sql = "INSERT INTO planning (periode, description) VALUES (?, ?)";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, p.getPeriode());
        ps.setString(2, p.getDescription());
        ps.executeUpdate();
    }

    // ==========================
    // UPDATE
    // ==========================
    @Override
    public void update(Planning p) throws SQLException {

        // 🔐 Validation métier AVANT update
        PlanningValidator.validate(p);

        String sql = "UPDATE planning SET periode=?, description=? WHERE id_planning=?";

        PreparedStatement ps = conx.prepareStatement(sql);
        ps.setString(1, p.getPeriode());
        ps.setString(2, p.getDescription());
        ps.setInt(3, p.getIdPlanning());
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
        String sql = "SELECT * FROM planning";

        Statement st = conx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Planning p = new Planning();
            p.setIdPlanning(rs.getInt("id_planning"));
            p.setPeriode(rs.getString("periode"));
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
            p.setPeriode(rs.getString("periode"));
            p.setDescription(rs.getString("description"));
            return p;
        }
        return null;
    }
}
