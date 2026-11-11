package com.devsphere.dao;

import com.devsphere.DatabaseConnection;
import com.devsphere.model.ProfileUpdate;
import com.devsphere.model.ProfileVisibility;
import com.devsphere.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UserDao {

    private static final String USER_COLUMNS = """
            id, full_name, username, email, password_hash, skills,
            professional_headline, biography, github_url, linkedin_url,
            portfolio_url, profile_visibility, created_at
            """;

    private static final String FIND_BY_USERNAME_OR_EMAIL = """
            SELECT %s
            FROM users
            WHERE username = ? OR email = ?
            """.formatted(USER_COLUMNS);

    private static final String FIND_BY_ID = """
            SELECT %s
            FROM users
            WHERE id = ?
            """.formatted(USER_COLUMNS);

    private static final String INSERT_USER = """
            INSERT INTO users (full_name, username, email, password_hash, skills)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_PROFILE = """
            UPDATE users
            SET full_name = ?, professional_headline = ?, biography = ?, skills = ?,
                github_url = ?, linkedin_url = ?, portfolio_url = ?, profile_visibility = ?
            WHERE id = ?
            """;

    public User findByUsernameOrEmail(String usernameOrEmail) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_USERNAME_OR_EMAIL)) {
            statement.setString(1, usernameOrEmail);
            statement.setString(2, usernameOrEmail);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        }
    }

    public User findById(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        }
    }

    public User createUser(String fullName, String username, String email,
                           String passwordHash, String skills) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fullName);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, passwordHash);
            statement.setString(5, skills.isBlank() ? null : skills);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return findById(generatedKeys.getLong(1));
                }
            }
        }

        throw new SQLException("User ID was not returned after registration.");
    }

    public void updateProfile(long userId, ProfileUpdate update) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_PROFILE)) {
            statement.setString(1, update.fullName());
            statement.setString(2, nullable(update.professionalHeadline()));
            statement.setString(3, nullable(update.biography()));
            statement.setString(4, nullable(update.skills()));
            statement.setString(5, nullable(update.githubUrl()));
            statement.setString(6, nullable(update.linkedinUrl()));
            statement.setString(7, nullable(update.portfolioUrl()));
            statement.setString(8, update.visibility().name());
            statement.setLong(9, userId);
            statement.executeUpdate();
        }
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        LocalDateTime createdAtValue = createdAt == null ? null : createdAt.toLocalDateTime();
        String visibility = resultSet.getString("profile_visibility");
        ProfileVisibility profileVisibility = "PUBLIC".equalsIgnoreCase(visibility)
                ? ProfileVisibility.PUBLIC
                : ProfileVisibility.PRIVATE;

        return new User(
                resultSet.getLong("id"),
                resultSet.getString("full_name"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                resultSet.getString("skills"),
                resultSet.getString("professional_headline"),
                resultSet.getString("biography"),
                resultSet.getString("github_url"),
                resultSet.getString("linkedin_url"),
                resultSet.getString("portfolio_url"),
                profileVisibility,
                createdAtValue
        );
    }
}
