package com.devsphere.model;

public record IssueFilter(String search, IssueStatus status, IssuePriority priority,
                          IssueType issueType, Long assigneeId) { }
