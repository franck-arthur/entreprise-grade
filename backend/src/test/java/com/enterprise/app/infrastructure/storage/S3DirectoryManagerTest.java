package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileData;
import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests S3DirectoryManager")
class S3DirectoryManagerTest extends BaseUnitTest {

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private S3DirectoryManager s3DirectoryManager;

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{fileStorage};
    }

    @Test
    @DisplayName("Doit créer un répertoire avec succès")
    void shouldCreateDirectorySuccessfully() {
        String directoryPath = "test/directory";
        when(fileStorage.uploadFile(any(InputStream.class), eq("test/directory/.directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn("test/directory/.directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq("test/directory/.directory"),
                                     eq("application/directory"), eq(0L));
    }

    @Test
    @DisplayName("Doit créer un répertoire avec un slash de fin")
    void shouldCreateDirectoryWithTrailingSlash() {
        String directoryPath = "test/directory/";
        when(fileStorage.uploadFile(any(InputStream.class), eq("test/directory/.directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn("test/directory/.directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq("test/directory/.directory"),
                                     eq("application/directory"), eq(0L));
    }

    @Test
    @DisplayName("Doit gérer un chemin de répertoire vide")
    void shouldHandleEmptyDirectoryPath() {
        String directoryPath = "";
        when(fileStorage.uploadFile(any(InputStream.class), eq(".directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn(".directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq(".directory"),
                                     eq("application/directory"), eq(0L));
    }

    @Test
    @DisplayName("Doit lancer une exception quand la création du répertoire échoue")
    void shouldThrowExceptionWhenCreateDirectoryFails() {
        String directoryPath = "test/directory";
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> s3DirectoryManager.createDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create directory");
    }

    @Test
    @DisplayName("Doit ajouter un fichier au répertoire avec succès")
    void shouldAddFileToDirectorySuccessfully() {
        String directoryPath = "test/directory";
        String fileName = "test.txt";
        InputStream content = new ByteArrayInputStream("test content".getBytes());
        String contentType = "text/plain";

        when(fileStorage.uploadFile(eq(content), eq("test/directory/test.txt"),
                                  eq(contentType), anyLong()))
                .thenReturn("test/directory/test.txt");

        assertThatCode(() -> s3DirectoryManager.addFileToDirectory(directoryPath, fileName, content, contentType))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(eq(content), eq("test/directory/test.txt"),
                                     eq(contentType), anyLong());
    }

    @Test
    @DisplayName("Doit lancer une exception quand l'ajout de fichier au répertoire échoue")
    void shouldThrowExceptionWhenAddFileToDirectoryFails() {
        String directoryPath = "test/directory";
        String fileName = "test.txt";
        InputStream content = new ByteArrayInputStream("test content".getBytes());
        String contentType = "text/plain";

        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> s3DirectoryManager.addFileToDirectory(directoryPath, fileName, content, contentType))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to add file to directory");
    }

    @Test
    @DisplayName("Doit ajouter plusieurs fichiers au répertoire avec succès")
    void shouldAddMultipleFilesToDirectorySuccessfully() {
        String directoryPath = "test/directory";
        Map<String, FileData> files = Map.of(
                "file1.txt", new FileData(new ByteArrayInputStream("content1".getBytes()), "text/plain", 8L),
                "file2.txt", new FileData(new ByteArrayInputStream("content2".getBytes()), "text/plain", 8L)
        );

        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
                .thenReturn("uploaded-key");

        assertThatCode(() -> s3DirectoryManager.addFilesToDirectory(directoryPath, files))
                .doesNotThrowAnyException();

        verify(fileStorage, times(2)).uploadFile(any(InputStream.class), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Doit vérifier si le répertoire existe")
    void shouldCheckIfDirectoryExists() {
        String directoryPath = "test/directory";
        when(fileStorage.fileExists("test/directory/.directory")).thenReturn(true);

        boolean result = s3DirectoryManager.directoryExists(directoryPath);

        assertThat(result).isTrue();
        verify(fileStorage).fileExists("test/directory/.directory");
    }

    @Test
    @DisplayName("Doit retourner faux quand le répertoire n'existe pas")
    void shouldReturnFalseWhenDirectoryDoesNotExist() {
        String directoryPath = "test/directory";
        when(fileStorage.fileExists("test/directory/.directory")).thenReturn(false);

        boolean result = s3DirectoryManager.directoryExists(directoryPath);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Doit supprimer un répertoire avec succès")
    void shouldDeleteDirectorySuccessfully() {
        String directoryPath = "test/directory";
        List<String> files = List.of(
                "test/directory/.directory",
                "test/directory/file1.txt",
                "test/directory/file2.txt"
        );

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        doNothing().when(fileStorage).deleteFile(anyString());

        assertThatCode(() -> s3DirectoryManager.deleteDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(fileStorage).listFiles("test/directory/");
        verify(fileStorage, times(3)).deleteFile(anyString());
    }

    @Test
    @DisplayName("Doit lancer une exception quand la suppression du répertoire échoue")
    void shouldThrowExceptionWhenDeleteDirectoryFails() {
        String directoryPath = "test/directory";
        when(fileStorage.listFiles("test/directory/"))
                .thenThrow(new RuntimeException("List failed"));

        assertThatThrownBy(() -> s3DirectoryManager.deleteDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete directory");
    }

    @Test
    @DisplayName("Doit normaliser les chemins de répertoire correctement")
    void shouldNormalizeDirectoryPathsCorrectly() {
        // Test with backslashes
        String windowsPath = "test\\directory\\subdirectory";
        when(fileStorage.uploadFile(any(InputStream.class), eq("test/directory/subdirectory/.directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn("test/directory/subdirectory/.directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(windowsPath))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq("test/directory/subdirectory/.directory"),
                                     eq("application/directory"), eq(0L));
    }

    @Test
    @DisplayName("Doit gérer un chemin de répertoire null")
    void shouldHandleNullDirectoryPath() {
        when(fileStorage.uploadFile(any(InputStream.class), eq(".directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn(".directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(null))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq(".directory"),
                                     eq("application/directory"), eq(0L));
    }
}