package com.devsphere.service;

import com.devsphere.dao.ProjectDao;
import com.devsphere.dao.UserDao;
import com.devsphere.model.ProjectStatus;
import com.devsphere.model.ProjectUpdate;
import com.devsphere.model.Session;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectServiceTest {
    private final ProjectService service = new ProjectService(new ProjectDao(), new UserDao());
    private final Session session = new Session(1, "Test", "tester", "tester@example.com", "Java");

    @Test
    void rejectsBlankProjectNameBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.createProject(session,
                new ProjectUpdate(" ", "", "", "", "", ProjectStatus.PLANNING)));
    }

    @Test
    void rejectsNonHttpGitHubUrlBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.createProject(session,
                new ProjectUpdate("DevSphere", "", "Tools", "Java", "github.com/devsphere", ProjectStatus.ACTIVE)));
    }
}
