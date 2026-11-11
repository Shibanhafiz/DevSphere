package com.devsphere.model;

import java.time.LocalDateTime;

public record IssueActivity(long id, long userId, String actorUsername, String actionType,
                            String oldValue, String newValue, LocalDateTime createdAt) { }
