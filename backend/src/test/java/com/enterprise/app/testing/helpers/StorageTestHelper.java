package com.enterprise.app.testing.helpers;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.*;

public class StorageTestHelper {

    public static MockMultipartFile createMockFile(String filename, String contentType, String content) {
        return new MockMultipartFile("file", filename, contentType, content.getBytes());
    }

    public static MockMultipartFile createMockTextFile(String filename, String content) {
        return createMockFile(filename, "text/plain", content);
    }

    public static MockMultipartFile createMockJsonFile(String filename, String jsonContent) {
        return createMockFile(filename, "application/json", jsonContent);
    }

    public static Map<String, MultipartFile> createFileMap(String filename, String content) {
        return Map.of(filename, createMockTextFile(filename, content));
    }

    public static Map<String, MultipartFile> createMultiFileMap() {
        return Map.of(
                "readme.txt", createMockTextFile("readme.txt", "# Project README"),
                "config.json", createMockJsonFile("config.json", "{\"version\": \"1.0\"}"),
                "script.py", createMockFile("script.py", "text/x-python", "print('Hello World')")
        );
    }

    public static void verifyZipContents(InputStream zipStream, String... expectedFileNames) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            int entryCount = 0;
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                assertThat(entry.getName()).isIn((String[]) expectedFileNames);
                zis.closeEntry();
            }

            assertThat(entryCount).isEqualTo(expectedFileNames.length);
        }
    }

    public static String readZipEntryContent(InputStream zipStream, String targetFileName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(targetFileName)) {
                    return new String(zis.readAllBytes());
                }
                zis.closeEntry();
            }

            throw new AssertionError("File not found in ZIP: " + targetFileName);
        }
    }

    public static void assertDirectoryPath(String directoryPath, String expectedProjectName) {
        assertThat(directoryPath).isNotNull();
        assertThat(directoryPath).startsWith("projects/");
        assertThat(directoryPath).contains(expectedProjectName);
        assertThat(directoryPath).matches("projects/\\d{4}/\\d{2}/\\d{2}/[a-zA-Z0-9]{8}-.*");
    }

    public static void assertZipKey(String zipKey, String expectedFileName) {
        assertThat(zipKey).isNotNull();
        assertThat(zipKey).startsWith("archives/");
        assertThat(zipKey).contains(expectedFileName);
        assertThat(zipKey).matches("archives/\\d{4}/\\d{2}/\\d{2}/[a-zA-Z0-9]{8}-.*\\.zip");
    }

    public static void assertFileKey(String fileKey, String expectedFolder, String expectedFileName) {
        assertThat(fileKey).isNotNull();

        if (expectedFolder != null) {
            assertThat(fileKey).startsWith(expectedFolder + "/");
        }

        assertThat(fileKey).contains(expectedFileName);
        assertThat(fileKey).matches(".*\\d{4}/\\d{2}/\\d{2}/[a-zA-Z0-9]{8}-.*");
    }

    public static InputStream createInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    public static String readInputStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes());
    }

    public static void assertFileContent(InputStream downloadedFile, String expectedContent) throws IOException {
        String actualContent = readInputStream(downloadedFile);
        assertThat(actualContent).isEqualTo(expectedContent);
    }

    public static class ProjectBuilder {
        private final Map<String, MultipartFile> files = new java.util.HashMap<>();
        private String projectName = "test-project";

        public static ProjectBuilder create() {
            return new ProjectBuilder();
        }

        public ProjectBuilder withName(String name) {
            this.projectName = name;
            return this;
        }

        public ProjectBuilder withFile(String filename, String content) {
            files.put(filename, createMockTextFile(filename, content));
            return this;
        }

        public ProjectBuilder withFile(String filename, String contentType, String content) {
            files.put(filename, createMockFile(filename, contentType, content));
            return this;
        }

        public ProjectBuilder withJsonFile(String filename, String jsonContent) {
            files.put(filename, createMockJsonFile(filename, jsonContent));
            return this;
        }

        public String getProjectName() {
            return projectName;
        }

        public Map<String, MultipartFile> getFiles() {
            return Map.copyOf(files);
        }
    }

    public static class ZipVerifier {
        private final InputStream zipStream;

        public ZipVerifier(InputStream zipStream) {
            this.zipStream = zipStream;
        }

        public static ZipVerifier of(InputStream zipStream) {
            return new ZipVerifier(zipStream);
        }

        public ZipVerifier hasEntryCount(int expectedCount) throws IOException {
            try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                int count = 0;
                while (zis.getNextEntry() != null) {
                    count++;
                    zis.closeEntry();
                }
                assertThat(count).isEqualTo(expectedCount);
            }
            return this;
        }

        public ZipVerifier hasEntry(String entryName) throws IOException {
            try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                ZipEntry entry;
                boolean found = false;

                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals(entryName)) {
                        found = true;
                        break;
                    }
                    zis.closeEntry();
                }

                assertThat(found).as("Entry '%s' should be present in ZIP", entryName).isTrue();
            }
            return this;
        }

        public ZipVerifier entryHasContent(String entryName, String expectedContent) throws IOException {
            try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                ZipEntry entry;

                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals(entryName)) {
                        String actualContent = new String(zis.readAllBytes());
                        assertThat(actualContent).isEqualTo(expectedContent);
                        return this;
                    }
                    zis.closeEntry();
                }

                fail("Entry '%s' not found in ZIP", entryName);
            }
            return this;
        }
    }
}