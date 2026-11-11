package com.devsphere.model;

import java.time.LocalDate;
import java.util.List;

public record ContributionDay(LocalDate date, int activityCount, int points, List<String> activityTypes) {
    public ContributionDay {
        activityTypes = List.copyOf(activityTypes);
    }
}
