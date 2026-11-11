package com.devsphere.service;

import com.devsphere.dao.UserDao;
import com.devsphere.model.ProfileCompletion;
import com.devsphere.model.ProfileUpdate;
import com.devsphere.model.ProfileVisibility;
import com.devsphere.model.User;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfileService {

    private final UserDao userDao;

    public ProfileService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User getProfile(long userId) throws SQLException {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new SQLException("The authenticated user could not be found.");
        }
        return user;
    }

    public void updateProfile(long userId, ProfileUpdate update) throws SQLException {
        if (update == null || update.fullName() == null || update.fullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (update.visibility() == null) {
            throw new IllegalArgumentException("Choose a profile visibility setting.");
        }

        validateUrl(update.githubUrl(), "GitHub profile URL");
        validateUrl(update.linkedinUrl(), "LinkedIn profile URL");
        validateUrl(update.portfolioUrl(), "Portfolio URL");
        userDao.updateProfile(userId, update);
    }

    public ProfileCompletion calculateCompletion(User user) {
        List<String> completed = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        addStatus(completed, missing, "Full name", user.fullName());
        addStatus(completed, missing, "Username", user.username());
        addStatus(completed, missing, "Email", user.email());
        addStatus(completed, missing, "Professional headline", user.professionalHeadline());
        addStatus(completed, missing, "Biography", user.biography());
        addStatus(completed, missing, "Technical skills", user.skills());
        addStatus(completed, missing, "GitHub profile", user.githubUrl());
        addStatus(completed, missing, "LinkedIn profile", user.linkedinUrl());
        addStatus(completed, missing, "Portfolio", user.portfolioUrl());

        int totalFields = completed.size() + missing.size();
        int percentage = totalFields == 0 ? 0 : (completed.size() * 100) / totalFields;
        return new ProfileCompletion(percentage, List.copyOf(completed), List.copyOf(missing));
    }

    public boolean canBeViewedPublicly(User user) {
        return user != null && user.profileVisibility() == ProfileVisibility.PUBLIC;
    }

    private void addStatus(List<String> completed, List<String> missing,
                           String label, String value) {
        if (value != null && !value.isBlank()) {
            completed.add(label);
        } else {
            missing.add(label);
        }
    }

    private void validateUrl(String value, String label) {
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException(label + " must be a valid HTTP or HTTPS URL.");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(label + " must be a valid HTTP or HTTPS URL.");
        }
    }
}
