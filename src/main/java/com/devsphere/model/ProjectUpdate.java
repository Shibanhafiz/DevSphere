package com.devsphere.model;

public record ProjectUpdate(String name, String description, String category,
                            String technologies, String githubUrl, ProjectStatus status) {
}
