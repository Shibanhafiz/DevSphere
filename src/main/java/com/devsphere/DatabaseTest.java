package com.devsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseTest {

    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT DATABASE()");
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                String databaseName = resultSet.getString(1);

                if ("devsphere".equalsIgnoreCase(databaseName)) {
                    System.out.println("Database connection successful. Connected to database: devsphere");
                } else {
                    System.err.println("Connection succeeded, but the selected database is '"
                            + databaseName + "' instead of 'devsphere'.");
                    System.exit(1);
                }
            } else {
                System.err.println("Connection succeeded, but SELECT DATABASE() returned no result.");
                System.exit(1);
            }
        } catch (SQLException exception) {
            if ("08001".equals(exception.getSQLState())) {
                System.err.println("Database configuration error: " + exception.getMessage());
            } else {
                System.err.println("Database connection test failed. Check that MySQL is running,"
                        + " the database exists, and the DEVSPHERE_DB_* environment variables are correct.");
            }
            System.err.println("SQL state: " + exception.getSQLState()
                    + ", error code: " + exception.getErrorCode());
            System.exit(1);
        }
    }
}
