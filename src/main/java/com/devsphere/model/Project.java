package com.devsphere.model;

import java.time.LocalDateTime;

public record Project(
        long id,
        String name,
        String description,
        String category,
        String technologies,
        String githubUrl,
        long ownerId,
        String ownerUsername,
        ProjectStatus status,
        int memberCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
