package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.application.dto.csvexport.ExcelRowDTO;
import com.enterprise.app.domain.model.csvexport.ExcelDefinitionStaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processor qui transforme une ligne Excel (ExcelRowDTO) en plusieurs enregistrements de staging.
 * Une ligne Excel peut générer plusieurs enregistrements si elle contient des valeurs possibles.
 */
@Slf4j
@Component
public class ExcelRowProcessor implements ItemProcessor<ExcelRowDTO, List<ExcelDefinitionStaging>> {

    @Override
    public List<ExcelDefinitionStaging> process(ExcelRowDTO item) throws Exception {
        List<ExcelDefinitionStaging> stagingRecords = new ArrayList<>();

        if (!item.hasValeursPossibles()) {
            // Pas de valeurs possibles : 1 seul enregistrement
            ExcelDefinitionStaging staging = createBaseStagingRecord(item);
            stagingRecords.add(staging);

            log.debug("Ligne sans valeurs possibles: {}", item.toLogString());

        } else {
            // Avec valeurs possibles : 1 enregistrement par valeur
            for (Map.Entry<Integer, String> entry : item.valeursPossibles().entrySet()) {
                ExcelDefinitionStaging staging = createBaseStagingRecord(item);
                staging.setIndexValeur(entry.getKey());
                staging.setValeurPossible(entry.getValue());

                stagingRecords.add(staging);

                log.debug("Ligne avec valeur possible: {} -> position={}, valeur={}",
                        item.toLogString(), entry.getKey(), entry.getValue());
            }
        }

        log.debug("Ligne Excel transformée en {} enregistrements de staging", stagingRecords.size());
        return stagingRecords;
    }

    /**
     * Crée un enregistrement de staging de base à partir d'une ligne Excel.
     */
    private ExcelDefinitionStaging createBaseStagingRecord(ExcelRowDTO item) {
        return ExcelDefinitionStaging.builder()
                .nomCsv(item.nomCsv())
                .excelRowNumber(item.excelRowNumber())  // PRÉSERVER L'ORDRE
                .codeVariable(item.codeVariable())
                .libelle(item.libelle())
                .format(item.format())
                .typeQuestion(item.typeQuestion())
                .regleGestion(normalizeRegleGestion(item.regleGestion()))
                .dateImport(LocalDateTime.now())
                .build();
    }

    /**
     * Normalise la règle de gestion en appliquant les valeurs par défaut.
     */
    private String normalizeRegleGestion(String regleGestion) {
        if (regleGestion == null || regleGestion.trim().isEmpty()) {
            return "POSITION_VALEUR_POSSIBLE"; // Valeur par défaut
        }

        String normalized = regleGestion.trim().toUpperCase();

        // Validation des règles supportées
        return switch (normalized) {
            case "POSITION_VALEUR_POSSIBLE", "TEXTE_DIRECT", "NUMERIQUE_DIRECT", "DATE_ISO" -> normalized;
            default -> {
                log.warn("Règle de gestion non reconnue: '{}', utilisation de POSITION_VALEUR_POSSIBLE", regleGestion);
                yield "POSITION_VALEUR_POSSIBLE";
            }
        };
    }
}