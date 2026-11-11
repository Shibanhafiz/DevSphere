package com.devsphere.model;

import java.time.LocalDateTime;

public record ContributionActivity(long id, long userId, Long projectId, Long issueId,
                                   String activityType, int points, LocalDateTime createdAt) { }
