package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.domain.storage.FileWorkflowManager;
import com.enterprise.app.testing.helpers.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@TestPropertySource(properties = {
    "app.storage.temp.directory=${java.io.tmpdir}/test-enterprise-app"
})
class FileWorkflowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileWorkflowManagerImpl workflowManager;

    @Autowired
    private FileStorage fileStorage;


    @Test
    void shouldExecuteCompleteFileWorkflow() throws IOException {
        // Given
        String projectName = "integration-test-project";
        Set<String> fileNames = Set.of("report.csv", "summary.txt", "data.json");

        // Étape 1: Créer le job avec fichiers vides
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        assertThat(job.status()).isEqualTo(FileWorkflowManager.FileJobStatus.CREATED);
        assertThat(job.localDirectory()).exists().isDirectory();

        // Vérifier que les fichiers vides existent
        for (String fileName : fileNames) {
            Path filePath = job.localDirectory().resolve(fileName);
            assertThat(filePath).exists().isRegularFile();
            assertThat(Files.size(filePath)).isZero();
        }

        // Vérifier que les fichiers ne sont pas encore prêts
        assertThat(workflowManager.areFilesReady(job.jobId())).isFalse();

        // Étape 2: Simuler le remplissage des fichiers par un batch externe (cp)
        Files.writeString(job.localDirectory().resolve("report.csv"), "Name,Age,City\nJohn,30,Paris\nJane,25,Lyon");
        Files.writeString(job.localDirectory().resolve("summary.txt"), "Summary report for integration test");
        Files.writeString(job.localDirectory().resolve("data.json"), "{\"test\": \"data\", \"count\": 42}");

        // Étape 3: Vérifier que les fichiers sont maintenant prêts
        assertThat(workflowManager.areFilesReady(job.jobId())).isTrue();

        FileWorkflowManager.FileJob updatedJob = workflowManager.getJobInfo(job.jobId());
        assertThat(updatedJob.status()).isEqualTo(FileWorkflowManager.FileJobStatus.FILES_READY);

        // Étape 4: Exécuter le workflow complet (upload + cleanup)
        List<String> uploadedKeys = workflowManager.processCompleteWorkflow(job.jobId());

        // Then
        assertThat(uploadedKeys).hasSize(3);
        assertThat(uploadedKeys).allMatch(key -> key.contains("projects/"));

        // Vérifier que le job est dans l'état final
        FileWorkflowManager.FileJob finalJob = workflowManager.getJobInfo(job.jobId());
        assertThat(finalJob.status()).isEqualTo(FileWorkflowManager.FileJobStatus.CLEANED_UP);

        // Vérifier que les fichiers locaux ont été supprimés
        assertThat(job.localDirectory()).doesNotExist();

        // Vérifier que les fichiers existent sur S3
        for (String uploadedKey : uploadedKeys) {
            assertThat(fileStorage.fileExists(uploadedKey)).isTrue();
        }
    }

    @Test
    void shouldHandleMultipleJobsConcurrently() throws IOException {
        // Given
        FileWorkflowManager.FileJob job1 = workflowManager.createFileJob("project-1", Set.of("file1.txt"));
        FileWorkflowManager.FileJob job2 = workflowManager.createFileJob("project-2", Set.of("file2.txt"));

        // When - Préparer les deux jobs
        Files.writeString(job1.localDirectory().resolve("file1.txt"), "Content for project 1");
        Files.writeString(job2.localDirectory().resolve("file2.txt"), "Content for project 2");

        // Then - Les deux jobs doivent être gérés indépendamment
        List<String> keys1 = workflowManager.processCompleteWorkflow(job1.jobId());
        List<String> keys2 = workflowManager.processCompleteWorkflow(job2.jobId());

        assertThat(keys1).hasSize(1);
        assertThat(keys2).hasSize(1);
        assertThat(keys1.get(0)).isNotEqualTo(keys2.get(0));

        // Vérifier que les deux jobs sont nettoyés
        assertThat(job1.localDirectory()).doesNotExist();
        assertThat(job2.localDirectory()).doesNotExist();
    }

    @Test
    void shouldCleanupJobResourcesOnDeletion() throws IOException {
        // Given
        FileWorkflowManager.FileJob job = workflowManager.createFileJob("test-project", Set.of("file.txt"));
        Path localDirectory = job.localDirectory();

        assertThat(localDirectory).exists();

        // When
        workflowManager.deleteJob(job.jobId());

        // Then
        assertThat(localDirectory).doesNotExist();
        assertThatThrownBy(() -> workflowManager.getJobInfo(job.jobId()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldListActiveJobsCorrectly() {
        // Given
        List<FileWorkflowManager.FileJob> initialJobs = workflowManager.listActiveJobs();

        // When
        FileWorkflowManager.FileJob job1 = workflowManager.createFileJob("project-1", Set.of("file1.txt"));
        FileWorkflowManager.FileJob job2 = workflowManager.createFileJob("project-2", Set.of("file2.txt"));

        List<FileWorkflowManager.FileJob> afterCreation = workflowManager.listActiveJobs();

        workflowManager.deleteJob(job1.jobId());
        List<FileWorkflowManager.FileJob> afterDeletion = workflowManager.listActiveJobs();

        // Then
        assertThat(afterCreation).hasSize(initialJobs.size() + 2);
        assertThat(afterDeletion).hasSize(initialJobs.size() + 1);
        assertThat(afterDeletion).extracting(FileWorkflowManager.FileJob::jobId)
            .contains(job2.jobId())
            .doesNotContain(job1.jobId());
    }

    @Test
    void shouldHandleWorkflowFailureGracefully() throws IOException {
        // Given
        FileWorkflowManager.FileJob job = workflowManager.createFileJob("failing-project", Set.of("file.txt"));

        // When - Essayer de traiter le workflow sans remplir les fichiers
        assertThatThrownBy(() -> workflowManager.processCompleteWorkflow(job.jobId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Files are not ready for job: " + job.jobId());

        // Then - Le job doit encore exister et être accessible
        FileWorkflowManager.FileJob failedJob = workflowManager.getJobInfo(job.jobId());
        assertThat(failedJob).isNotNull();
        assertThat(failedJob.localDirectory()).exists(); // Le répertoire ne doit pas être supprimé
    }

    @Test
    void shouldGenerateUniqueDirectoryPaths() {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file.txt");

        // When
        FileWorkflowManager.FileJob job1 = workflowManager.createFileJob(projectName, fileNames);
        FileWorkflowManager.FileJob job2 = workflowManager.createFileJob(projectName, fileNames);

        // Then
        assertThat(job1.localDirectory()).isNotEqualTo(job2.localDirectory());
        assertThat(job1.localDirectory()).exists();
        assertThat(job2.localDirectory()).exists();
    }
}