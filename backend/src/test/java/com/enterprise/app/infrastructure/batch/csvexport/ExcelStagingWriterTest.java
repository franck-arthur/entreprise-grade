package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.model.csvexport.ExcelDefinitionStaging;
import com.enterprise.app.domain.repository.csvexport.ExcelDefinitionStagingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelStagingWriter - Écriture des données de staging Excel")
class ExcelStagingWriterTest {

    @Mock
    private ExcelDefinitionStagingRepository stagingRepository;

    @InjectMocks
    private ExcelStagingWriter excelStagingWriter;

    private List<ExcelDefinitionStaging> stagingData;

    @BeforeEach
    void setUp() {
        stagingData = Arrays.asList(
                createStagingRecord("CSV_1", 1, "COL1", "Colonne 1", null, null),
                createStagingRecord("CSV_1", 2, "COL2", "Colonne 2", 1, "Valeur 1"),
                createStagingRecord("CSV_1", 2, "COL2", "Colonne 2", 2, "Valeur 2")
        );
    }

    @Test
    @DisplayName("Devrait écrire les données de staging avec succès")
    void shouldWriteStagingDataSuccessfully() throws Exception {
        // Given
        Chunk<List<ExcelDefinitionStaging>> chunk = new Chunk<>(Arrays.asList(stagingData));

        when(stagingRepository.saveAll(anyList())).thenReturn(stagingData);

        // When
        excelStagingWriter.write(chunk);

        // Then
        verify(stagingRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Devrait gérer un chunk vide")
    void shouldHandleEmptyChunk() throws Exception {
        // Given
        Chunk<List<ExcelDefinitionStaging>> emptyChunk = new Chunk<>();

        // When
        excelStagingWriter.write(emptyChunk);

        // Then
        // Aucune interaction avec le repository ne devrait avoir lieu car le chunk est vide
        verify(stagingRepository, org.mockito.Mockito.never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Devrait écrire plusieurs listes de données de staging")
    void shouldWriteMultipleStagingDataLists() throws Exception {
        // Given
        List<ExcelDefinitionStaging> secondStagingData = Arrays.asList(
                createStagingRecord("CSV_2", 3, "COL3", "Colonne 3", null, null)
        );

        Chunk<List<ExcelDefinitionStaging>> chunk = new Chunk<>(Arrays.asList(stagingData, secondStagingData));

        when(stagingRepository.saveAll(anyList())).thenReturn(stagingData);

        // When
        excelStagingWriter.write(chunk);

        // Then
        verify(stagingRepository, org.mockito.Mockito.times(2)).saveAll(anyList());
    }

    private ExcelDefinitionStaging createStagingRecord(String nomCsv, int rowNumber, String codeVariable,
                                                      String libelle, Integer indexValeur, String valeurPossible) {
        return ExcelDefinitionStaging.builder()
                .nomCsv(nomCsv)
                .excelRowNumber(rowNumber)
                .codeVariable(codeVariable)
                .libelle(libelle)
                .format("TEXT")
                .typeQuestion("SIMPLE")
                .regleGestion("TEXTE_DIRECT")
                .indexValeur(indexValeur)
                .valeurPossible(valeurPossible)
                .dateImport(LocalDateTime.now())
                .build();
    }
}