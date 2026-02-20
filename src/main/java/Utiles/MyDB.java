package Utiles;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private final String URL = "jdbc:mysql://localhost:3306/ecoadventure";
    private final String USER = "root";
    private final String PASSWORD = "";

    private Connection connection; // Retirer le static ici pour une meilleure pratique Singleton
    private static MyDB instance;

    // Constructeur privé
    private MyDB() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to database successfully");
        } catch (SQLException e) {
            System.out.println("❌ Database connection error: " + e.getMessage());
        }
    }

    // Retourner l'instance unique et créer la connexion si elle n'existe pas
    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    // Retourner la connexion à partir de l'instance
    public Connection getConnection() {
        return connection;
    }
}