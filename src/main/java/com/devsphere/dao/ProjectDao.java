package com.devsphere.dao;

import com.devsphere.DatabaseConnection;
import com.devsphere.model.Project;
import com.devsphere.model.ProjectMember;
import com.devsphere.model.ProjectStatus;
import com.devsphere.model.ProjectUpdate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectDao {

    private static final String PROJECT_COLUMNS = """
            p.id, p.name, p.description, p.category, p.technologies, p.github_url,
            p.owner_id, owner.username AS owner_username,
            p.status,
            (SELECT COUNT(*) FROM project_members member_count_rows
             WHERE member_count_rows.project_id = p.id) AS member_count,
            p.created_at, p.updated_at
            """;

    private static final String FIND_PROJECTS_FOR_USER = """
            SELECT %s
            FROM projects p
            JOIN users owner ON owner.id = p.owner_id
            JOIN project_members viewer ON viewer.project_id = p.id AND viewer.user_id = ?
            ORDER BY p.updated_at DESC, p.created_at DESC
            """.formatted(PROJECT_COLUMNS);

    private static final String FIND_PROJECT_FOR_USER = """
            SELECT %s
            FROM projects p
            JOIN users owner ON owner.id = p.owner_id
            WHERE p.id = ?
              AND EXISTS (
                  SELECT 1 FROM project_members viewer
                  WHERE viewer.project_id = p.id AND viewer.user_id = ?
              )
            """.formatted(PROJECT_COLUMNS);

    private static final String INSERT_PROJECT = """
            INSERT INTO projects (name, description, category, technologies, github_url, owner_id, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_MEMBER = """
            INSERT INTO project_members (project_id, user_id, role)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE_PROJECT = """
            UPDATE projects
            SET name = ?, description = ?, category = ?, technologies = ?, github_url = ?, status = ?
            WHERE id = ? AND owner_id = ?
            """;

    private static final String FIND_MEMBERS = """
            SELECT u.id, u.full_name, u.username, u.email, pm.role, pm.joined_at
            FROM project_members pm
            JOIN users u ON u.id = pm.user_id
            WHERE pm.project_id = ?
              AND EXISTS (
                  SELECT 1 FROM project_members viewer
                  WHERE viewer.project_id = pm.project_id AND viewer.user_id = ?
              )
            ORDER BY CASE WHEN pm.role = 'OWNER' THEN 0 ELSE 1 END, u.username
            """;

    private static final String IS_MEMBER = """
            SELECT 1 FROM project_members WHERE project_id = ? AND user_id = ?
            """;

    private static final String IS_OWNER = """
            SELECT 1 FROM projects WHERE id = ? AND owner_id = ?
            """;

    private static final String REMOVE_MEMBER = """
            DELETE FROM project_members
            WHERE project_id = ? AND user_id = ? AND role = 'MEMBER'
            """;

    public Project createProject(long ownerId, ProjectUpdate update) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long projectId;
                try (PreparedStatement projectStatement = connection.prepareStatement(
                        INSERT_PROJECT, Statement.RETURN_GENERATED_KEYS)) {
                    projectStatement.setString(1, update.name());
                    projectStatement.setString(2, nullable(update.description()));
                    projectStatement.setString(3, nullable(update.category()));
                    projectStatement.setString(4, nullable(update.technologies()));
                    projectStatement.setString(5, nullable(update.githubUrl()));
                    projectStatement.setLong(6, ownerId);
                    projectStatement.setString(7, update.status().name());
                    projectStatement.executeUpdate();

                    try (ResultSet keys = projectStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Project ID was not returned after creation.");
                        }
                        projectId = keys.getLong(1);
                    }
                }

                try (PreparedStatement memberStatement = connection.prepareStatement(INSERT_MEMBER)) {
                    memberStatement.setLong(1, projectId);
                    memberStatement.setLong(2, ownerId);
                    memberStatement.setString(3, "OWNER");
                    memberStatement.executeUpdate();
                }

                connection.commit();
                return findByIdForUser(projectId, ownerId);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Project> findProjectsForUser(long userId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_PROJECTS_FOR_USER)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projects.add(mapProject(resultSet));
                }
            }
        }
        return projects;
    }

    public Project findByIdForUser(long projectId, long userId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_PROJECT_FOR_USER)) {
            statement.setLong(1, projectId);
            statement.setLong(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapProject(resultSet) : null;
            }
        }
    }

    public List<ProjectMember> findMembers(long projectId, long viewerId) throws SQLException {
        List<ProjectMember> members = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_MEMBERS)) {
            statement.setLong(1, projectId);
            statement.setLong(2, viewerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    members.add(mapMember(resultSet));
                }
            }
        }
        return members;
    }

    public boolean isMember(long projectId, long userId) throws SQLException {
        return exists(IS_MEMBER, projectId, userId);
    }

    public boolean isOwner(long projectId, long userId) throws SQLException {
        return exists(IS_OWNER, projectId, userId);
    }

    public void updateProject(long ownerId, long projectId, ProjectUpdate update) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PROJECT)) {
            statement.setString(1, update.name());
            statement.setString(2, nullable(update.description()));
            statement.setString(3, nullable(update.category()));
            statement.setString(4, nullable(update.technologies()));
            statement.setString(5, nullable(update.githubUrl()));
            statement.setString(6, update.status().name());
            statement.setLong(7, projectId);
            statement.setLong(8, ownerId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Project was not found or the current user is not its owner.");
            }
        }
    }

    public void addMember(long projectId, long userId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_MEMBER)) {
            statement.setLong(1, projectId);
            statement.setLong(2, userId);
            statement.setString(3, "MEMBER");
            statement.executeUpdate();
        }
    }

    public boolean removeMember(long projectId, long userId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(REMOVE_MEMBER)) {
            statement.setLong(1, projectId);
            statement.setLong(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public void deleteProject(long ownerId, long projectId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement detach = connection.prepareStatement(
                        "UPDATE contribution_activities SET project_id=NULL, issue_id=NULL WHERE project_id=?")) {
                    detach.setLong(1, projectId);
                    detach.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM projects WHERE id=? AND owner_id=?")) {
                    delete.setLong(1, projectId);
                    delete.setLong(2, ownerId);
                    if (delete.executeUpdate() == 0) {
                        throw new SQLException("Project was not found or the current user is not its owner.");
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private boolean exists(String sql, long firstId, long secondId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, firstId);
            statement.setLong(2, secondId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Project mapProject(ResultSet resultSet) throws SQLException {
        return new Project(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("category"),
                resultSet.getString("technologies"),
                resultSet.getString("github_url"),
                resultSet.getLong("owner_id"),
                resultSet.getString("owner_username"),
                ProjectStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("member_count"),
                toLocalDateTime(resultSet.getTimestamp("created_at")),
                toLocalDateTime(resultSet.getTimestamp("updated_at"))
        );
    }

    private ProjectMember mapMember(ResultSet resultSet) throws SQLException {
        return new ProjectMember(
                resultSet.getLong("id"),
                resultSet.getString("full_name"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("role"),
                toLocalDateTime(resultSet.getTimestamp("joined_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
