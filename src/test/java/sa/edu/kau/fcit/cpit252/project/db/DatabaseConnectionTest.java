package sa.edu.kau.fcit.cpit252.project.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

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

    private void setDatabaseConnection(Connection connection) throws Exception {
        Field field = DatabaseConnection.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(null, connection);
    }
}
