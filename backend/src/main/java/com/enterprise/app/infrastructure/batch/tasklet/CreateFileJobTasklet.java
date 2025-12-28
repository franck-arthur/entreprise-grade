package com.enterprise.app.infrastructure.batch.tasklet;

import com.enterprise.app.domain.storage.FileWorkflowManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Tasklet pour créer un job de traitement de fichiers
 *
 * Cette étape initialise un nouveau job avec les fichiers vides dans un répertoire local.
 * Les paramètres du job (projectName, fileNames) sont passés via les JobParameters.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateFileJobTasklet implements Tasklet {

    private final FileWorkflowManager fileWorkflowManager;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Executing CreateFileJobTasklet");

        // Récupération des paramètres du job
        String projectName = chunkContext.getStepContext()
                .getStepExecution()
                .getJobParameters()
                .getString("projectName");

        String fileNamesParam = chunkContext.getStepContext()
                .getStepExecution()
                .getJobParameters()
                .getString("fileNames");

        if (projectName == null || fileNamesParam == null) {
            throw new IllegalArgumentException("Missing required parameters: projectName and/or fileNames");
        }

        // Conversion de la chaîne de noms de fichiers en Set
        Set<String> fileNames = Set.of(fileNamesParam.split(","));

        log.info("Creating file job for project: {} with files: {}", projectName, fileNames);

        // Création du job via FileWorkflowManager
        FileWorkflowManager.FileJob fileJob = fileWorkflowManager.createFileJob(projectName, fileNames);

        log.info("Created file job: {}", fileJob.jobId());

        // Stocker le jobId dans le contexte d'exécution pour les étapes suivantes
        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .putString("fileJobId", fileJob.jobId());

        log.info("CreateFileJobTasklet completed successfully");
        return RepeatStatus.FINISHED;
    }
}