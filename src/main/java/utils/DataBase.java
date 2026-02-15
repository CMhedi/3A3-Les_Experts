package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBase {

    private static DataBase instance;
    private Connection conx;

    // ⚠️ adapte le nom de ta DB ici
    private final String URL  = "jdbc:mysql://localhost:3306/ecoadventure?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private final String USER = "root";
    private final String PASS = "";

    private DataBase() {
        try {
            conx = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✅ Connexion DB OK");
        } catch (SQLException e) {
            System.out.println("❌ Connexion DB ERROR: " + e.getMessage());
        }
    }

    public static DataBase getInstance() {
        if (instance == null) {
            instance = new DataBase();
        }
        return instance;
    }

    public Connection getConx() {
        return conx;
    }
}