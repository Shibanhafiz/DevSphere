package com.devsphere.model;

public record ProfileUpdate(
        String fullName,
        String professionalHeadline,
        String biography,
        String skills,
        String githubUrl,
        String linkedinUrl,
        String portfolioUrl,
        ProfileVisibility visibility
) {
}
