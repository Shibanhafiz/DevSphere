package com.devsphere.model;

public record ProjectAnalytics(long projectId, int totalActivities, int contributingMembers,
                               int issuesCreated, int issuesResolved, String mostActiveMember) { }
