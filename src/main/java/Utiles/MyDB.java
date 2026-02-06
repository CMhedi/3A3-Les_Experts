package Utiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure";
    private final String USER = "root";
    private final String PASSWORD = "";

    private static Connection connection;
    private static MyDB instance;

    // constructeur privé (Singleton)
    private MyDB() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to database successfully");
        } catch (SQLException e) {
            System.out.println("❌ Database connection error: " + e.getMessage());
        }
    }

    // retourner l'instance unique
    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    // retourner la connexion
    public static Connection getConnection() {
        return connection;
    }
}
