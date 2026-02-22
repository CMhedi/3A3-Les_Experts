package Services.interfaces;

import Entities.NutritionLog;
import Utiles.MyDB;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NutritionLogService {

    private Connection conx= MyDB.getConnection();

    public void add(NutritionLog log) throws SQLException {

        String sql =
                "INSERT INTO nutrition_log " +
                        "(user_id, food_name, calories, protein, fat, carbs, log_date) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps =
                conx.prepareStatement(sql);

        ps.setInt(1, log.getUserId());
        ps.setString(2, log.getFoodName());
        ps.setDouble(3, log.getCalories());
        ps.setDouble(4, log.getProtein());
        ps.setDouble(5, log.getFat());
        ps.setDouble(6, log.getCarbs());
        ps.setDate(7, Date.valueOf(log.getLogDate()));

        ps.executeUpdate();
    }

    public List<NutritionLog> getByUser(int userId) throws SQLException {

        List<NutritionLog> list = new ArrayList<>();

        String sql = "SELECT * FROM nutrition_log WHERE user_id=?";

        PreparedStatement ps =
                conx.prepareStatement(sql);

        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            NutritionLog log =
                    new NutritionLog(
                            rs.getInt("user_id"),
                            rs.getString("food_name"),
                            rs.getDouble("calories"),
                            rs.getDouble("protein"),
                            rs.getDouble("fat"),
                            rs.getDouble("carbs"),
                            rs.getDate("log_date").toLocalDate()
                    );

            list.add(log);
        }

        return list;
    }

    public double getTodayTotal(int userId) throws SQLException {

        String sql =
                "SELECT SUM(calories) as total " +
                        "FROM nutrition_log " +
                        "WHERE user_id=? AND log_date=?";

        PreparedStatement ps =
                conx.prepareStatement(sql);

        ps.setInt(1, userId);
        ps.setDate(2, Date.valueOf(LocalDate.now()));

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble("total");
        }

        return 0;
    }
}