package Services.interfaces;

import Entities.Conversation;
import Utiles.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceConversation {
    private Connection conn = MyDB.getInstance().getConnection();  // ← Fixed: getConnection()

    // READ : Récupérer toutes les conversations
    public List<Conversation> readAll() throws SQLException {
        List<Conversation> list = new ArrayList<>();
        String req = "SELECT * FROM conversation";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            list.add(new Conversation(
                    rs.getInt("id_conversation"),
                    rs.getString("titre"),
                    rs.getInt("est_groupe")
            ));
        }
        return list;
    }
}