package com.devsphere.model;

import java.time.LocalDate;

public record IssueUpdate(String title, String description, IssueType issueType,
                          IssuePriority priority, IssueStatus status, Long assigneeId,
                          LocalDate dueDate) { }
