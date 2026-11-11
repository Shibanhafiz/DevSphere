package com.devsphere.service;

import com.devsphere.dao.UserDao;
import com.devsphere.model.Session;
import com.devsphere.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void register(String fullName, String username, String email,
                         char[] password, char[] confirmPassword, String skills)
            throws SQLException, DuplicateUserException {
        validateRegistration(fullName, username, email, password, confirmPassword);

        String passwordHash = BCrypt.hashpw(new String(password), BCrypt.gensalt(12));
        try {
            if (userDao.findByUsernameOrEmail(username.trim()) != null
                    || userDao.findByUsernameOrEmail(email.trim().toLowerCase()) != null) {
                throw new DuplicateUserException();
            }

            try {
                userDao.createUser(fullName.trim(), username.trim(), email.trim().toLowerCase(),
                        passwordHash, skills == null ? "" : skills.trim());
            } catch (SQLException exception) {
                if ("23000".equals(exception.getSQLState())) {
                    throw new DuplicateUserException();
                }
                throw exception;
            }
        } finally {
            passwordHash = null;
        }
    }

    public Session login(String usernameOrEmail, char[] password) throws SQLException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()
                || password == null || password.length == 0) {
            return null;
        }

        User user = userDao.findByUsernameOrEmail(usernameOrEmail.trim());
        if (user == null || !BCrypt.checkpw(new String(password), user.passwordHash())) {
            return null;
        }

        return new Session(user.id(), user.fullName(), user.username(), user.email(), user.skills());
    }

    private void validateRegistration(String fullName, String username, String email,
                                      char[] password, char[] confirmPassword) {
        if (isBlank(fullName) || isBlank(username) || isBlank(email)
                || password == null || password.length == 0
                || confirmPassword == null || confirmPassword.length == 0) {
            throw new IllegalArgumentException("Please complete all required fields.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (!Arrays.equals(password, confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
