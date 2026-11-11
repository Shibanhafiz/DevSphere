package com.devsphere.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Issue(long id, long projectId, String projectName, String title, String description,
                    IssueType issueType, IssuePriority priority, IssueStatus status,
                    long reporterId, String reporterUsername, Long assigneeId, String assigneeUsername,
                    LocalDate dueDate, LocalDateTime createdAt, LocalDateTime updatedAt) { }
