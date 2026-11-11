package com.devsphere.config;

import com.devsphere.DatabaseConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class DatabaseInitializer {

    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                full_name VARCHAR(150) NOT NULL,
                username VARCHAR(50) NOT NULL UNIQUE,
                email VARCHAR(254) NOT NULL UNIQUE,
                password_hash VARCHAR(60) NOT NULL,
                skills TEXT NULL,
                professional_headline VARCHAR(180) NULL,
                biography TEXT NULL,
                github_url VARCHAR(500) NULL,
                linkedin_url VARCHAR(500) NULL,
                portfolio_url VARCHAR(500) NULL,
                profile_visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PRIVATE',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String[][] PROFILE_COLUMNS = {
            {"professional_headline", "VARCHAR(180) NULL"},
            {"biography", "TEXT NULL"},
            {"github_url", "VARCHAR(500) NULL"},
            {"linkedin_url", "VARCHAR(500) NULL"},
            {"portfolio_url", "VARCHAR(500) NULL"},
            {"profile_visibility", "ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PRIVATE'"}
    };

    private static final String CREATE_PROJECTS_TABLE = """
            CREATE TABLE IF NOT EXISTS projects (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(150) NOT NULL,
                description TEXT NULL,
                category VARCHAR(100) NULL,
                technologies TEXT NULL,
                github_url VARCHAR(500) NULL,
                owner_id BIGINT NOT NULL,
                status ENUM('PLANNING', 'ACTIVE', 'COMPLETED', 'ON_HOLD') NOT NULL DEFAULT 'PLANNING',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id),
                INDEX idx_projects_owner (owner_id),
                INDEX idx_projects_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String CREATE_PROJECT_MEMBERS_TABLE = """
            CREATE TABLE IF NOT EXISTS project_members (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                project_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                role ENUM('OWNER', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
                joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT uq_project_member UNIQUE (project_id, user_id),
                CONSTRAINT fk_project_members_project FOREIGN KEY (project_id)
                    REFERENCES projects(id) ON DELETE CASCADE,
                CONSTRAINT fk_project_members_user FOREIGN KEY (user_id)
                    REFERENCES users(id),
                INDEX idx_project_members_user (user_id),
                INDEX idx_project_members_project (project_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String[][] PROJECT_COLUMNS = {
            {"category", "VARCHAR(100) NULL"},
            {"technologies", "TEXT NULL"},
            {"github_url", "VARCHAR(500) NULL"}
    };

    private static final String CREATE_PROJECT_FILES_TABLE = """
            CREATE TABLE IF NOT EXISTS project_files (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                project_id BIGINT NOT NULL,
                original_name VARCHAR(255) NOT NULL,
                stored_name VARCHAR(255) NOT NULL UNIQUE,
                relative_path VARCHAR(700) NOT NULL,
                file_size BIGINT NOT NULL,
                file_extension VARCHAR(20) NULL,
                description VARCHAR(1000) NULL,
                uploaded_by BIGINT NOT NULL,
                uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT fk_project_files_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                CONSTRAINT fk_project_files_user FOREIGN KEY (uploaded_by) REFERENCES users(id),
                INDEX idx_project_files_project (project_id),
                INDEX idx_project_files_uploader (uploaded_by)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String CREATE_ISSUES_TABLE = """
            CREATE TABLE IF NOT EXISTS issues (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                project_id BIGINT NOT NULL,
                title VARCHAR(200) NOT NULL,
                description TEXT NULL,
                issue_type ENUM('BUG', 'TASK', 'FEATURE') NOT NULL,
                priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
                status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
                reporter_id BIGINT NOT NULL,
                assignee_id BIGINT NULL,
                due_date DATE NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT fk_issues_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
                CONSTRAINT fk_issues_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
                INDEX idx_issues_project (project_id), INDEX idx_issues_status (status),
                INDEX idx_issues_priority (priority), INDEX idx_issues_assignee (assignee_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String CREATE_ISSUE_COMMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS issue_comments (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                issue_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                comment TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_issue_comments_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
                CONSTRAINT fk_issue_comments_user FOREIGN KEY (user_id) REFERENCES users(id),
                INDEX idx_issue_comments_issue (issue_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String CREATE_ISSUE_ACTIVITY_TABLE = """
            CREATE TABLE IF NOT EXISTS issue_activity (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                issue_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                action_type VARCHAR(50) NOT NULL,
                old_value VARCHAR(500) NULL,
                new_value VARCHAR(500) NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_issue_activity_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
                CONSTRAINT fk_issue_activity_user FOREIGN KEY (user_id) REFERENCES users(id),
                INDEX idx_issue_activity_issue (issue_id), INDEX idx_issue_activity_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String CREATE_CONTRIBUTION_TABLE = """
            CREATE TABLE IF NOT EXISTS contribution_activities (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                project_id BIGINT NULL,
                issue_id BIGINT NULL,
                activity_type VARCHAR(50) NOT NULL,
                points INT NOT NULL,
                event_key VARCHAR(180) NOT NULL UNIQUE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_contribution_user FOREIGN KEY (user_id) REFERENCES users(id),
                CONSTRAINT fk_contribution_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                CONSTRAINT fk_contribution_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
                INDEX idx_contribution_user_date (user_id, created_at),
                INDEX idx_contribution_project_date (project_id, created_at),
                INDEX idx_contribution_date (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private DatabaseInitializer() {
    }

    public static void createUsersTableIfNeeded() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(CREATE_USERS_TABLE)) {
            statement.executeUpdate();

            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            for (String[] column : PROFILE_COLUMNS) {
                if (!columnExists(metadata, catalog, "users", column[0])) {
                    String migration = "ALTER TABLE users ADD COLUMN " + column[0] + " " + column[1];
                    try (PreparedStatement migrationStatement = connection.prepareStatement(migration)) {
                        migrationStatement.executeUpdate();
                    }
                }
            }
        }
    }

    public static void createProjectTablesIfNeeded() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement projectsStatement = connection.prepareStatement(CREATE_PROJECTS_TABLE);
             PreparedStatement membersStatement = connection.prepareStatement(CREATE_PROJECT_MEMBERS_TABLE)) {
            projectsStatement.executeUpdate();
            membersStatement.executeUpdate();
            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            for (String[] column : PROJECT_COLUMNS) {
                if (!columnExists(metadata, catalog, "projects", column[0])) {
                    try (PreparedStatement migration = connection.prepareStatement("ALTER TABLE projects ADD COLUMN " + column[0] + " " + column[1])) {
                        migration.executeUpdate();
                    }
                }
            }
            try (PreparedStatement files = connection.prepareStatement(CREATE_PROJECT_FILES_TABLE)) {
                files.executeUpdate();
            }
        }
    }

    public static void createIssueTablesIfNeeded() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement issues = connection.prepareStatement(CREATE_ISSUES_TABLE);
             PreparedStatement comments = connection.prepareStatement(CREATE_ISSUE_COMMENTS_TABLE);
             PreparedStatement activity = connection.prepareStatement(CREATE_ISSUE_ACTIVITY_TABLE)) {
            issues.executeUpdate();
            comments.executeUpdate();
            activity.executeUpdate();
        }
    }

    public static void createContributionTablesIfNeeded() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(CREATE_CONTRIBUTION_TABLE)) {
            statement.executeUpdate();
        }
    }

    private static boolean columnExists(DatabaseMetaData metadata, String catalog, String tableName,
                                        String columnName) throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, null, tableName, columnName)) {
            return columns.next();
        }
    }
}
