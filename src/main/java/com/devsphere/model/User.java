package com.devsphere.model;

import java.time.LocalDateTime;

public record User(
        long id,
        String fullName,
        String username,
        String email,
        String passwordHash,
        String skills,
        String professionalHeadline,
        String biography,
        String githubUrl,
        String linkedinUrl,
        String portfolioUrl,
        ProfileVisibility profileVisibility,
        LocalDateTime createdAt
) {
}
