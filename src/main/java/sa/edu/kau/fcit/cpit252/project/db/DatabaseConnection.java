package sa.edu.kau.fcit.cpit252.project.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Database credentials
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "1234";
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";

    private static Connection connection;

    private DatabaseConnection() {} // Private constructor to prevent instantiation (Singleton pattern)

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) { // Check if the connection is null OR if it has been closed
                Class.forName("org.postgresql.Driver"); // Ensure the PostgreSQL driver is loaded
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("Database connection established successfully!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver is not found. Include it in your library path.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database connection failed. Please check your credentials and database URL.");
            e.printStackTrace();
        }
        return connection;
    }
}