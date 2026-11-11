package com.devsphere.model;

import java.util.Map;

public record ContributionSummary(int totalScore, int totalActivities, Map<String, Integer> breakdown) { }
