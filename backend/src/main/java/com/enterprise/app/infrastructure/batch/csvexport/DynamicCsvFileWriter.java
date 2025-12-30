package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.model.csvexport.CsvColumnDefinition;
import com.enterprise.app.domain.repository.csvexport.CsvColumnDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writer qui génère des fichiers CSV avec en-tête dynamique.
 * Le nom du fichier et l'en-tête sont construits dynamiquement selon les définitions en base.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class DynamicCsvFileWriter extends FlatFileItemWriter<String[]> {

    private final CsvColumnDefinitionRepository columnRepository;

    @Value("#{jobParameters['nomCsv']}")
    private String nomCsv;

    @Value("#{jobParameters['outputDirectory'] ?: '/tmp/csv-exports'}")
    private String outputDirectory;

    @Value("#{jobParameters['dateDebut'] ?: T(java.time.LocalDate).now().minusDays(7)}")
    private LocalDate dateDebut;

    @Value("#{jobParameters['dateFin'] ?: T(java.time.LocalDate).now()}")
    private LocalDate dateFin;

    private int totalLinesWritten = 0;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("Initialisation du writer CSV pour: {}", nomCsv);

        try {
            // Construire le nom du fichier
            String fileName = buildFileName();
            FileSystemResource resource = new FileSystemResource(fileName);

            log.info("Fichier de sortie: {}", fileName);

            // Configurer le writer
            setResource(resource);
            setEncoding("UTF-8");

            // Configurer l'agrégateur de lignes (délimiteur CSV)
            DelimitedLineAggregator<String[]> aggregator = new DelimitedLineAggregator<>();
            aggregator.setDelimiter(";"); // Utilisation du point-virgule pour éviter les problèmes avec les décimales
            setLineAggregator(aggregator);

            // Générer et configurer l'en-tête
            String[] headers = buildHeaders();
            setHeaderCallback(writer -> {
                String headerLine = String.join(";", headers);
                writer.write(headerLine);
                log.info("En-tête CSV écrit: {} colonnes", headers.length);
            });

            // Optionnel: footer avec statistiques
            setFooterCallback(writer -> {
                String footerLine = String.format("# Total lignes: %d, Généré le: %s",
                        totalLinesWritten, LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                writer.write(footerLine);
            });

            // Stocker le chemin du fichier dans le contexte pour le tracking
            stepExecution.getExecutionContext().putString("csv.outputFile", fileName);

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du writer CSV", e);
            throw new IllegalStateException("Erreur initialisation writer: " + e.getMessage(), e);
        }
    }

    /**
     * Construit le nom du fichier de sortie.
     */
    private String buildFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateRange = String.format("%s_%s",
                dateDebut.format(formatter),
                dateFin.format(formatter));

        return String.format("%s/%s_%s.csv", outputDirectory, nomCsv, dateRange);
    }

    /**
     * Construit l'en-tête CSV à partir des définitions de colonnes.
     */
    private String[] buildHeaders() {
        List<CsvColumnDefinition> columns = columnRepository
                .findByNomCsvAndActifTrueOrderByOrdreColonneAsc(nomCsv);

        if (columns.isEmpty()) {
            log.error("Aucune colonne trouvée pour le CSV: {}", nomCsv);
            throw new IllegalArgumentException("Aucune colonne définie pour: " + nomCsv);
        }

        String[] headers = columns.stream()
                .map(CsvColumnDefinition::getCodeVariable)
                .toArray(String[]::new);

        log.debug("En-têtes générés: {}", String.join(", ", headers));
        return headers;
    }

    @Override
    public void write(org.springframework.batch.item.Chunk<? extends String[]> chunk) throws Exception {
        super.write(chunk);
        totalLinesWritten += chunk.size();

        if (totalLinesWritten % 1000 == 0) {
            log.info("Lignes écrites: {}", totalLinesWritten);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        super.update(executionContext);
        executionContext.putInt("csv.totalLinesWritten", totalLinesWritten);
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            super.close();
            log.info("Fichier CSV fermé. Total lignes écrites: {}", totalLinesWritten);
        } catch (Exception e) {
            log.error("Erreur lors de la fermeture du fichier CSV", e);
            throw new ItemStreamException("Erreur fermeture CSV: " + e.getMessage(), e);
        }
    }
}