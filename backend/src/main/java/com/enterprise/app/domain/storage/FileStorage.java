package com.enterprise.app.domain.storage;

import java.io.InputStream;
import java.util.List;

public interface FileStorage {
    String uploadFile(InputStream inputStream, String key, String contentType, long contentLength);
    InputStream downloadFile(String key);
    void deleteFile(String key);
    List<String> listFiles(String prefix);
    boolean fileExists(String key);
    FileMetadata getFileMetadata(String key);
}