package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests ZipServiceImpl")
class ZipServiceImplTest extends BaseUnitTest {

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private ZipServiceImpl zipService;

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{fileStorage};
    }

    @Test
    @DisplayName("Doit créer un ZIP à partir d'un répertoire avec succès")
    void shouldCreateZipFromDirectorySuccessfully() throws IOException {
        String directoryPath = "test/directory";
        List<String> files = Arrays.asList(
                "test/directory/file1.txt",
                "test/directory/file2.txt",
                "test/directory/.directory"
        );

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        when(fileStorage.downloadFile("test/directory/file1.txt"))
                .thenReturn(new ByteArrayInputStream("content1".getBytes()));
        when(fileStorage.downloadFile("test/directory/file2.txt"))
                .thenReturn(new ByteArrayInputStream("content2".getBytes()));

        InputStream zipStream = zipService.createZipFromDirectory(directoryPath);

        assertThat(zipStream).isNotNull();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry1 = zis.getNextEntry();
            assertThat(entry1.getName()).isEqualTo("file1.txt");

            ZipEntry entry2 = zis.getNextEntry();
            assertThat(entry2.getName()).isEqualTo("file2.txt");

            ZipEntry entry3 = zis.getNextEntry();
            assertThat(entry3).isNull();
        }

        verify(fileStorage).listFiles("test/directory/");
        verify(fileStorage).downloadFile("test/directory/file1.txt");
        verify(fileStorage).downloadFile("test/directory/file2.txt");
        verify(fileStorage, never()).downloadFile("test/directory/.directory");
    }

    @Test
    @DisplayName("Doit retourner un ZIP vide quand aucun fichier dans le répertoire")
    void shouldReturnEmptyZipWhenNoFilesInDirectory() {
        String directoryPath = "test/empty";
        when(fileStorage.listFiles("test/empty/")).thenReturn(List.of());

        InputStream zipStream = zipService.createZipFromDirectory(directoryPath);

        assertThat(zipStream).isNotNull();
        assertThat(zipStream).isInstanceOf(ByteArrayInputStream.class);

        verify(fileStorage).listFiles("test/empty/");
        verify(fileStorage, never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("Doit gérer un répertoire avec seulement le marqueur de répertoire")
    void shouldHandleDirectoryWithOnlyDirectoryMarker() throws IOException {
        String directoryPath = "test/directory";
        List<String> files = List.of("test/directory/.directory");

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);

        InputStream zipStream = zipService.createZipFromDirectory(directoryPath);

        assertThat(zipStream).isNotNull();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry = zis.getNextEntry();
            assertThat(entry).isNull();
        }

        verify(fileStorage).listFiles("test/directory/");
        verify(fileStorage, never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("Doit lancer une exception quand la liste des fichiers échoue")
    void shouldThrowExceptionWhenFileListingFails() {
        String directoryPath = "test/directory";
        when(fileStorage.listFiles("test/directory/"))
                .thenThrow(new RuntimeException("Listing failed"));

        assertThatThrownBy(() -> zipService.createZipFromDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create ZIP archive");
    }

    @Test
    @DisplayName("Doit lancer une exception quand le téléchargement de fichier échoue")
    void shouldThrowExceptionWhenFileDownloadFails() {
        String directoryPath = "test/directory";
        List<String> files = List.of("test/directory/file1.txt");

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        when(fileStorage.downloadFile("test/directory/file1.txt"))
                .thenThrow(new RuntimeException("Download failed"));

        assertThatThrownBy(() -> zipService.createZipFromDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create ZIP archive");
    }

    @Test
    @DisplayName("Doit uploader un répertoire zippé avec succès")
    void shouldUploadZippedDirectorySuccessfully() {
        String directoryPath = "test/directory";
        String zipFileName = "test-archive";
        List<String> files = List.of("test/directory/file1.txt");

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        when(fileStorage.downloadFile("test/directory/file1.txt"))
                .thenReturn(new ByteArrayInputStream("content1".getBytes()));
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), eq("application/zip"), anyLong()))
                .thenReturn("archives/2024/12/27/12345678-test-archive.zip");

        String result = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(result).isEqualTo("archives/2024/12/27/12345678-test-archive.zip");
        verify(fileStorage).uploadFile(any(InputStream.class), anyString(), eq("application/zip"), anyLong());
    }

    @Test
    @DisplayName("Doit ajouter l'extension .zip si elle n'est pas présente")
    void shouldAddZipExtensionIfNotPresent() {
        String directoryPath = "test/directory";
        String zipFileName = "test-archive";
        when(fileStorage.listFiles("test/directory/")).thenReturn(List.of());
        when(fileStorage.uploadFile(any(InputStream.class), contains("test-archive.zip"), eq("application/zip"), anyLong()))
                .thenReturn("archives/2024/12/27/12345678-test-archive.zip");

        String result = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(result).contains("test-archive.zip");
        verify(fileStorage).uploadFile(any(InputStream.class), contains("test-archive.zip"), eq("application/zip"), anyLong());
    }

    @Test
    @DisplayName("Ne doit pas ajouter l'extension .zip si elle est déjà présente")
    void shouldNotAddZipExtensionIfAlreadyPresent() {
        String directoryPath = "test/directory";
        String zipFileName = "test-archive.zip";
        when(fileStorage.listFiles("test/directory/")).thenReturn(List.of());
        when(fileStorage.uploadFile(any(InputStream.class), contains("test-archive.zip"), eq("application/zip"), anyLong()))
                .thenReturn("archives/2024/12/27/12345678-test-archive.zip");

        String result = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(result).contains("test-archive.zip");
        verify(fileStorage).uploadFile(any(InputStream.class), contains("test-archive.zip"), eq("application/zip"), anyLong());
    }

    @Test
    @DisplayName("Doit nettoyer les noms de fichiers avec des caractères spéciaux")
    void shouldCleanFilenameWithSpecialCharacters() {
        String directoryPath = "test/directory";
        String zipFileName = "test@#$%archive.zip";
        when(fileStorage.listFiles("test/directory/")).thenReturn(List.of());
        when(fileStorage.uploadFile(any(InputStream.class), contains("test____archive.zip"), eq("application/zip"), anyLong()))
                .thenReturn("archives/2024/12/27/12345678-test____archive.zip");

        String result = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(result).contains("test____archive.zip");
        verify(fileStorage).uploadFile(any(InputStream.class), contains("test____archive.zip"), eq("application/zip"), anyLong());
    }

    @Test
    @DisplayName("Doit lancer une exception quand l'upload échoue")
    void shouldThrowExceptionWhenUploadFails() {
        String directoryPath = "test/directory";
        String zipFileName = "test-archive";
        when(fileStorage.listFiles("test/directory/")).thenReturn(List.of());
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), eq("application/zip"), anyLong()))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> zipService.uploadZippedDirectory(directoryPath, zipFileName))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload zipped directory");
    }

    @Test
    @DisplayName("Doit gérer une structure de répertoire imbriquée")
    void shouldHandleNestedDirectoryStructure() throws IOException {
        String directoryPath = "test/directory";
        List<String> files = Arrays.asList(
                "test/directory/subdir/file1.txt",
                "test/directory/file2.txt"
        );

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        when(fileStorage.downloadFile("test/directory/subdir/file1.txt"))
                .thenReturn(new ByteArrayInputStream("content1".getBytes()));
        when(fileStorage.downloadFile("test/directory/file2.txt"))
                .thenReturn(new ByteArrayInputStream("content2".getBytes()));

        InputStream zipStream = zipService.createZipFromDirectory(directoryPath);

        assertThat(zipStream).isNotNull();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry1 = zis.getNextEntry();
            assertThat(entry1.getName()).isEqualTo("subdir/file1.txt");

            ZipEntry entry2 = zis.getNextEntry();
            assertThat(entry2.getName()).isEqualTo("file2.txt");
        }
    }

    @Test
    @DisplayName("Doit gérer un chemin de répertoire vide")
    void shouldHandleEmptyDirectoryPath() {
        String directoryPath = "";
        when(fileStorage.listFiles("")).thenReturn(List.of("file.txt"));
        when(fileStorage.downloadFile("file.txt"))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        InputStream result = zipService.createZipFromDirectory(directoryPath);

        assertThat(result).isNotNull();
        verify(fileStorage).listFiles("");
    }

    @Test
    @DisplayName("Doit gérer un chemin de répertoire null")
    void shouldHandleNullDirectoryPath() {
        String directoryPath = null;
        when(fileStorage.listFiles("")).thenReturn(List.of("file.txt"));
        when(fileStorage.downloadFile("file.txt"))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        InputStream result = zipService.createZipFromDirectory(directoryPath);

        assertThat(result).isNotNull();
        verify(fileStorage).listFiles("");
    }
}