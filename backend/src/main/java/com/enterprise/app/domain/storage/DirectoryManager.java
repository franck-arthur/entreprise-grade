package com.enterprise.app.domain.storage;

import java.io.InputStream;
import java.util.Map;

public interface DirectoryManager {
    void createDirectory(String directoryPath);
    void addFileToDirectory(String directoryPath, String fileName, InputStream content, String contentType);
    void addFilesToDirectory(String directoryPath, Map<String, FileData> files);
    boolean directoryExists(String directoryPath);
    void deleteDirectory(String directoryPath);
}