package com.devsphere.model;

import java.time.LocalDateTime;

public record ProjectFile(long id, long projectId, String originalName, String storedName,
                          String relativePath, long fileSize, String extension, String description,
                          long uploadedBy, String uploaderUsername, LocalDateTime uploadedAt) { }
