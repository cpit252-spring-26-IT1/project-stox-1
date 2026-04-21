package sa.edu.kau.fcit.cpit252.project.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Create a file named "stox.db" in the project's root folder
    private static final String URL = "jdbc:sqlite:stox.db";
    private static Connection connection;

    private DatabaseConnection() {
    }// Private constructor to prevent instantiation (Singleton pattern)

    public static Connection getConnection() {
        if (connection == null) {
            try {
                if (connection == null || connection.isClosed()) {
                    // Load the SQLite driver
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection(URL);
                    System.out.println("SQLite Database connection established successfully!");
                }

            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Database connection failed.");
                e.printStackTrace();
            }
        }
        return connection;
    }
}