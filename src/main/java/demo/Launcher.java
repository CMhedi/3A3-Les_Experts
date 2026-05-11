package demo;

import Utiles.MyDB;
import javafx.application.Application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
        try {
            Connection cnx = MyDB.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement("SELECT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("✅ DB répond: " + rs.getInt(1));
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }
}
