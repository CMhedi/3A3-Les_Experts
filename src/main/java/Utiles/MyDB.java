package Utiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private static final String URL = "jdbc:mysql://localhost:3306/ecoadventure";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;
    private static MyDB instance;

    // Constructor privado
    private MyDB() {
        connect();
    }

    // دالة خاصة بالربط باش نجموا نعيطولها وقت الحاجة
    private void connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connected to database successfully");
            }
        } catch (SQLException e) {
            System.err.println("❌ Database connection error: " + e.getMessage());
        }
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }


    public static Connection getConnection() {
        try {

            if (instance == null) {
                instance = new MyDB();
            }

            if (connection == null || connection.isClosed()) {
                System.out.println("🔄 Connection lost! Reconnecting...");
                instance.connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}