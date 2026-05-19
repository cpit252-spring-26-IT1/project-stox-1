package sa.edu.kau.fcit.cpit252.project.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class DatabaseConnectionTest {

    @TempDir
    Path tempDir;

    private Connection connection;

    @AfterEach
    void tearDown() throws Exception {
        setDatabaseConnection(null);
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void getConnectionReturnsExistingSingletonConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("stox-test.db"));
        setDatabaseConnection(connection);

        assertSame(connection, DatabaseConnection.getConnection());
    }

    @Test
    void privateConstructorCanBeInvokedReflectivelyForCoverageOnly() throws Exception {
        Constructor<DatabaseConnection> constructor = DatabaseConnection.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        constructor.newInstance();
    }

    @Test
    void getConnectionCreatesNewConnectionWhenFieldIsNull() throws Exception {
        // Set the static connection field to null to force the creation branch
        setDatabaseConnection(null);

        // getConnection() should load the driver and open a new SQLite connection
        java.sql.Connection conn = DatabaseConnection.getConnection();

        // Record the new connection for tearDown cleanup
        connection = conn;

        // The created connection must be non-null and open
        java.util.Objects.requireNonNull(conn, "Expected a non-null connection");
        assertFalse(conn.isClosed(), "Expected the connection to be open");
    }

    private void setDatabaseConnection(Connection connection) throws Exception {
        Field field = DatabaseConnection.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(null, connection);
    }
}
