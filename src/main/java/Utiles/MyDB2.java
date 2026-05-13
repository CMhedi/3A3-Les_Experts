package Utiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB2 {

    private static final String URL = "jdbc:mysql://localhost:3308/ecoadventure";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyDB2 instance;

    private MyDB2() {}

    public static MyDB2 getInstance() {
        if (instance == null) {
            instance = new MyDB2();
        }
        return instance;
    }

    /**
     * Retourne une nouvelle connexion indépendante à chaque appel.
     * Chaque appelant est responsable de fermer sa connexion (try-with-resources).
     * Cela évite les problèmes d'auto-commit partagé entre services.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
