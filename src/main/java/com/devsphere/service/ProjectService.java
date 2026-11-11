package com.devsphere.service;

import com.devsphere.dao.ProjectDao;
import com.devsphere.dao.UserDao;
import com.devsphere.model.Project;
import com.devsphere.model.ProjectMember;
import com.devsphere.model.ProjectStatus;
import com.devsphere.model.ProjectUpdate;
import com.devsphere.model.Session;
import com.devsphere.model.User;

import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ProjectService {

    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    private final ProjectDao projectDao;
    private final UserDao userDao;
    private final ContributionService contributionService;

    public ProjectService(ProjectDao projectDao, UserDao userDao) {
        this(projectDao, userDao, new ContributionService(new com.devsphere.dao.ContributionDao()));
    }

    public ProjectService(ProjectDao projectDao, UserDao userDao, ContributionService contributionService) {
        this.projectDao = projectDao;
        this.userDao = userDao;
        this.contributionService = contributionService;
    }

    public Project createProject(Session session, ProjectUpdate update) throws SQLException {
        ProjectUpdate validated = validate(update);
        Project project = projectDao.createProject(session.userId(), validated);
        contributionService.projectCreated(session.userId(), project.id());
        return project;
    }

    public List<Project> getProjectsForUser(Session session) throws SQLException {
        return projectDao.findProjectsForUser(session.userId());
    }

    public Project getProjectForUser(Session session, long projectId)
            throws SQLException, ProjectAccessException {
        Project project = projectDao.findByIdForUser(projectId, session.userId());
        if (project == null) {
            throw new ProjectAccessException("You are not a member of this project.");
        }
        return project;
    }

    public List<ProjectMember> getMembersForUser(Session session, long projectId)
            throws SQLException, ProjectAccessException {
        getProjectForUser(session, projectId);
        return projectDao.findMembers(projectId, session.userId());
    }

    public void updateProject(Session session, long projectId, ProjectUpdate update)
            throws SQLException, ProjectAccessException {
        if (!projectDao.isOwner(projectId, session.userId())) {
            throw new ProjectAccessException("Only the project owner can edit this project.");
        }
        ProjectUpdate validated = validate(update);
        projectDao.updateProject(session.userId(), projectId, validated);
        if (validated.status() == com.devsphere.model.ProjectStatus.COMPLETED
                && projectDao.findByIdForUser(projectId, session.userId()).status()
                == com.devsphere.model.ProjectStatus.COMPLETED) {
            contributionService.projectCompleted(session.userId(), projectId);
        }
    }

    public void addMember(Session session, long projectId, String usernameOrEmail)
            throws SQLException, ProjectAccessException {
        requireOwner(session, projectId);
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new IllegalArgumentException("Enter a username or email address.");
        }

        User user = userDao.findByUsernameOrEmail(usernameOrEmail.trim());
        if (user == null) {
            throw new IllegalArgumentException("No registered user was found with that username or email.");
        }
        Project project = getProjectForUser(session, projectId);
        if (project.ownerId() == user.id()) {
            throw new IllegalArgumentException("The project owner is already a team member.");
        }
        if (projectDao.isMember(projectId, user.id())) {
            throw new IllegalArgumentException("That user is already a member of this project.");
        }

        try {
            projectDao.addMember(projectId, user.id());
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new IllegalArgumentException("That user is already a member of this project.");
            }
            throw exception;
        }
    }

    public void removeMember(Session session, long projectId, long memberUserId)
            throws SQLException, ProjectAccessException {
        requireOwner(session, projectId);
        Project project = getProjectForUser(session, projectId);
        if (project.ownerId() == memberUserId) {
            throw new IllegalArgumentException("The project owner cannot be removed.");
        }
        if (!projectDao.removeMember(projectId, memberUserId)) {
            throw new IllegalArgumentException("That user is not a member of this project.");
        }
    }

    public void deleteProject(Session session, long projectId)
            throws SQLException, ProjectAccessException {
        if (!projectDao.isOwner(projectId, session.userId())) {
            throw new ProjectAccessException("Only the project owner can delete this project.");
        }
        projectDao.deleteProject(session.userId(), projectId);
    }

    private void requireOwner(Session session, long projectId)
            throws SQLException, ProjectAccessException {
        if (!projectDao.isOwner(projectId, session.userId())) {
            throw new ProjectAccessException("Only the project owner can manage this project.");
        }
    }

    private ProjectUpdate validate(ProjectUpdate update) {
        if (update == null || update.name() == null || update.name().isBlank()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }
        if (update.name().trim().length() > 150) {
            throw new IllegalArgumentException("Project name cannot exceed 150 characters.");
        }
        if (update.description() != null && update.description().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Project description cannot exceed 5000 characters.");
        }
        if (update.status() == null) {
            throw new IllegalArgumentException("Choose a project status.");
        }
        validateUrl(update.githubUrl());
        return new ProjectUpdate(update.name().trim(),
                update.description() == null ? "" : update.description().trim(),
                update.category() == null ? "" : update.category().trim(),
                update.technologies() == null ? "" : update.technologies().trim(),
                update.githubUrl() == null ? "" : update.githubUrl().trim(), update.status());
    }

    private void validateUrl(String value) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = new URI(value.trim());
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())))
                throw new IllegalArgumentException("GitHub URL must be a valid HTTP or HTTPS URL.");
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("GitHub URL must be a valid HTTP or HTTPS URL.");
        }
    }
}
