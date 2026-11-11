package com.devsphere.service;

import com.devsphere.dao.UserDao;
import com.devsphere.model.ProfileVisibility;
import com.devsphere.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProfileServiceTest {
    private final ProfileService service = new ProfileService(new UserDao());

    @Test
    void rejectsNonHttpSocialLinksBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(1,
                new com.devsphere.model.ProfileUpdate("Test", "", "", "",
                        "example.com/profile", "", "", ProfileVisibility.PRIVATE)));
    }

    @Test
    void completionExcludesAuthenticationFields() {
        User user = new User(1, "Test User", "tester", "tester@example.com", "$2a$hash",
                "Java", "Developer", "About", "https://github.com/test", "", "",
                ProfileVisibility.PUBLIC, LocalDateTime.now());

        var completion = service.calculateCompletion(user);

        assertEquals(7, completion.completedFields().size());
        assertFalse(completion.completedFields().contains("Password"));
        assertFalse(completion.missingFields().contains("Password"));
    }
}
