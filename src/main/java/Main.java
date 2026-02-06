

import Utiles.MyDB;
import java.sql.Connection;

public class Main {

    public static void main(String[] args) {

        try {
            Connection cnx = MyDB.getInstance().getConnection();

            if (cnx != null && !cnx.isClosed()) {
                System.out.println("✅ Connexion à la base de données réussie !");
            } else {
                System.out.println("❌ Connexion échouée !");
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de la connexion");
            e.printStackTrace();
        }
    }
}
