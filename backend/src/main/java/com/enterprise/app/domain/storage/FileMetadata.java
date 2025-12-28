package com.enterprise.app.domain.storage;

import java.time.LocalDateTime;
import java.util.Map;

public record FileMetadata(
    String key,
    String contentType,
    long contentLength,
    LocalDateTime lastModified,
    Map<String, String> userMetadata
) {}