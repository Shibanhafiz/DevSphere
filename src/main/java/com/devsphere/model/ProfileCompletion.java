package com.devsphere.model;

import java.util.List;

public record ProfileCompletion(int percentage, List<String> completedFields, List<String> missingFields) {
}
