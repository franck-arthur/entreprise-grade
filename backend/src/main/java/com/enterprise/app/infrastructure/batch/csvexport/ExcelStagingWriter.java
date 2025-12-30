package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.model.csvexport.ExcelDefinitionStaging;
import com.enterprise.app.domain.repository.csvexport.ExcelDefinitionStagingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Writer qui sauvegarde les enregistrements de staging en base de données.
 * Utilise des chunks pour optimiser les performances.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelStagingWriter implements ItemWriter<List<ExcelDefinitionStaging>> {

    private final ExcelDefinitionStagingRepository stagingRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends List<ExcelDefinitionStaging>> chunk) throws Exception {
        int totalRecords = 0;

        for (List<ExcelDefinitionStaging> stagingList : chunk) {
            if (stagingList != null && !stagingList.isEmpty()) {
                stagingRepository.saveAll(stagingList);
                totalRecords += stagingList.size();

                log.debug("Sauvegardé {} enregistrements de staging", stagingList.size());
            }
        }

        log.info("Chunk sauvegardé: {} enregistrements de staging au total", totalRecords);
    }
}