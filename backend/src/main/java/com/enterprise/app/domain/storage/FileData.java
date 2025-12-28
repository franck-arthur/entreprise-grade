package com.enterprise.app.domain.storage;

import java.io.InputStream;

public record FileData(
    InputStream content,
    String contentType,
    long contentLength
) {}