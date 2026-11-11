package com.devsphere;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/devsphere";

    private DatabaseConnection() {
        // Utility class; do not create instances.
    }

    public static Connection getConnection() throws SQLException {
        String url = getEnvironmentValue("DEVSPHERE_DB_URL", DEFAULT_URL);
        String user = getRequiredEnvironmentValue("DEVSPHERE_DB_USER");
        String password = getRequiredEnvironmentValue("DEVSPHERE_DB_PASSWORD");

        return DriverManager.getConnection(url, user, password);
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (SQLException exception) {
            System.err.println("Database connection failed. SQL state: " + exception.getSQLState()
                    + ", error code: " + exception.getErrorCode());
            return false;
        }
    }

    private static String getEnvironmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String getRequiredEnvironmentValue(String name) throws SQLException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new SQLException("Missing required environment variable: " + name, "08001");
        }
        return value;
    }
}
