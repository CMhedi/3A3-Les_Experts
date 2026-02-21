package Utiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private static final String URL = "jdbc:mysql://localhost:3308/ecoadventure?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;
    private static MyDB instance;

    private MyDB() {
        connect();
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        } else {
            try {
                if (connection == null || connection.isClosed()) {
                    instance.connect();
                }
            } catch (SQLException e) {
                instance.connect();
            }
        }
        return instance;
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to database successfully");
        } catch (SQLException e) {
            System.out.println("❌ Database connection error: " + e.getMessage());
            connection = null;
        }
    }

    public static Connection getConnection() {
        MyDB.getInstance();
        return connection;
    }
}
