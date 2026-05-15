package sa.edu.kau.fcit.cpit252.project;

import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import sa.edu.kau.fcit.cpit252.project.db.DatabaseConnection;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppJavaFxTest extends ApplicationTest {

    private Connection connection;
    private Stage stage;

    @Override
    public void init() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path tempDir = Files.createTempDirectory("stox-app-test");
        connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("app-test.db"));
        setDatabaseConnection(connection);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        new App().start(stage);
    }

    @AfterEach
    void tearDownDatabase() throws Exception {
        if (stage != null) {
            interact(stage::close);
        }
        setDatabaseConnection(null);
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void appStartInitializesPrimaryStageAndScene() {
        assertNotNull(stage.getScene());
        assertTrue(stage.isShowing());
    }

    private void setDatabaseConnection(Connection connection) throws Exception {
        Field field = DatabaseConnection.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(null, connection);
    }
}
