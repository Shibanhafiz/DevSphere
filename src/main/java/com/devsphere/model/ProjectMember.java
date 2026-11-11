package com.devsphere.model;

import java.time.LocalDateTime;

public record ProjectMember(
        long userId,
        String fullName,
        String username,
        String email,
        String role,
        LocalDateTime joinedAt
) {
}
