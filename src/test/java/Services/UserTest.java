package Services;

import Entities.UserApp;
import Services.interfaces.UserService;
import enums.RoleUser;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;

public class UserTest {
    static UserService us = new UserService();

    @Test
    @Order(1)
    void testAjoutUser() throws SQLException {
        UserApp u = new UserApp();
        u.setNom("TestNom");
        u.setPrenom("TestPrenom");
        u.setEmail("test@esprit.tn");
        u.setRole(RoleUser.USER_SIMPLE);
        u.setMotDePasse("123456");

        us.add(u);
        // Thabbet ken tzed f el list
        assertTrue(us.getAll().stream().anyMatch(user -> user.getEmail().equals("test@esprit.tn")));
    }
}