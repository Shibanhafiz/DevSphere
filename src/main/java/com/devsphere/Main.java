package com.devsphere;

import com.devsphere.config.DatabaseInitializer;
import com.devsphere.dao.UserDao;
import com.devsphere.service.AuthService;
import com.devsphere.ui.AuthFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::startApplication);
    }

    private static void startApplication() {
        try {
            DatabaseInitializer.createUsersTableIfNeeded();
            DatabaseInitializer.createProjectTablesIfNeeded();
            DatabaseInitializer.createIssueTablesIfNeeded();
            DatabaseInitializer.createContributionTablesIfNeeded();
            new AuthFrame(new AuthService(new UserDao())).setVisible(true);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "MySQL connection succeeded, but a database migration failed.\n"
                            + "SQL state: " + exception.getSQLState()
                            + ", error code: " + exception.getErrorCode() + "\n"
                            + exception.getMessage(),
                    "Database Migration Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "DevSphere could not start because the database is unavailable.\n"
                            + "Check MySQL and the DEVSPHERE_DB_* environment variables.",
                    "Database Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
