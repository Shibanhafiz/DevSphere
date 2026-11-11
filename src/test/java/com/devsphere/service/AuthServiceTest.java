package com.devsphere.service;

import com.devsphere.dao.UserDao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    private final AuthService service = new AuthService(new UserDao());

    @Test
    void rejectsInvalidEmailBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.register(
                "Test User", "test-user", "not-an-email",
                "secret123".toCharArray(), "secret123".toCharArray(), "Java"));
    }

    @Test
    void rejectsMismatchedPasswordsBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.register(
                "Test User", "test-user", "test@example.com",
                "secret123".toCharArray(), "different".toCharArray(), "Java"));
    }
}
