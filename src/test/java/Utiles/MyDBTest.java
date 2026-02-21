package Utiles;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class MyDBTest {

    @Test
    void singletonInstanceShouldNotBeNull() {
        MyDB db1 = MyDB.getInstance();
        MyDB db2 = MyDB.getInstance();

        assertNotNull(db1);
        assertSame(db1, db2);
    }

    @Test
    void connectionShouldBeValidWhenDatabaseIsRunning() throws SQLException {
        MyDB.getInstance();
        Connection cnx = MyDB.getConnection();

        Assumptions.assumeTrue(cnx != null, "Database not available: skipping integration check");

        assertDoesNotThrow(() -> cnx.isValid(2));
        assertTrue(cnx.isValid(2));
    }
}
