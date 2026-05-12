package sa.edu.kau.fcit.cpit252.project.api;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Utility to read API keys from a local .env file.
 */
public class EnvConfig {
    private static final Properties properties = new Properties();

    static {
        try {
            java.nio.file.Path envPath = Paths.get(".env");
            if (!Files.exists(envPath)) {
                envPath = Paths.get("project-stox-1", ".env");
            }
            if (Files.exists(envPath)) {
                properties.load(Files.newInputStream(envPath));
            } else {
                System.out.println("Notice: No .env file found, falling back to OS system environment variables.");
            }
        } catch (Exception e) {
            System.out.println("Notice: Error reading .env file: " + e.getMessage());
        }
    }

    /**
     * Gets a configuration value. It checks the .env file first,
     * then the OS system environment variables, and finally falls back to a default value.
     */
    public static String get(String key, String defaultValue) {
        String val = properties.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            val = System.getenv(key);
        }
        return (val != null && !val.trim().isEmpty()) ? val : defaultValue;
    }
}
