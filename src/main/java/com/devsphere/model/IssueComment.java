package com.devsphere.model;

import java.time.LocalDateTime;

public record IssueComment(long id, long userId, String authorUsername, String comment,
                           LocalDateTime createdAt) { }
