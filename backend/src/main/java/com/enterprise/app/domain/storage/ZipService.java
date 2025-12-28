package com.enterprise.app.domain.storage;

import java.io.InputStream;

public interface ZipService {
    InputStream createZipFromDirectory(String directoryPath);
    String uploadZippedDirectory(String directoryPath, String zipFileName);
}