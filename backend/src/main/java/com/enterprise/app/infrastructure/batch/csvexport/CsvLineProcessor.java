package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.model.csvexport.CsvColumnDefinition;
import com.enterprise.app.domain.model.csvexport.CsvColumnValues;
import com.enterprise.app.domain.repository.csvexport.CsvColumnDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Processor qui transforme une ligne de données brutes en tableau de valeurs CSV formatées.
 * Applique les règles de transformation et maintient l'ordre des colonnes.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class CsvLineProcessor implements ItemProcessor<Map<String, Object>, String[]> {

    private final CsvColumnDefinitionRepository columnRepository;

    @Value("#{jobParameters['nomCsv']}")
    private String nomCsv;

    private List<CsvColumnDefinition> orderedColumns;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("Initialisation du processor pour CSV: {}", nomCsv);

        // Charger les colonnes dans l'ordre défini
        orderedColumns = columnRepository.findByNomCsvAndActifTrueOrderByOrdreColonneAsc(nomCsv);
        log.info("Colonnes chargées: {} colonnes pour {}", orderedColumns.size(), nomCsv);

        // Log des colonnes pour debug
        for (int i = 0; i < orderedColumns.size(); i++) {
            CsvColumnDefinition col = orderedColumns.get(i);
            log.debug("Colonne {}: {} -> {} ({})", i + 1, col.getCodeVariable(), col.getLibelle(), col.getRegleGestion());
        }
    }

    @Override
    public String[] process(Map<String, Object> rawData) throws Exception {
        if (orderedColumns == null || orderedColumns.isEmpty()) {
            log.error("Aucune colonne définie pour le CSV: {}", nomCsv);
            return new String[0];
        }

        // Construire le tableau dans l'ordre exact des colonnes
        String[] csvRow = new String[orderedColumns.size()];

        for (int i = 0; i < orderedColumns.size(); i++) {
            CsvColumnDefinition column = orderedColumns.get(i);
            Object rawValue = rawData.get(column.getCodeVariable());

            // Transformer la valeur selon la règle de gestion
            String transformedValue = transformValue(rawValue, column);
            csvRow[i] = transformedValue;

            log.trace("Colonne {}: {} = {} -> {}", i + 1, column.getCodeVariable(), rawValue, transformedValue);
        }

        log.trace("Ligne processée: {} valeurs", csvRow.length);
        return csvRow;
    }

    /**
     * Transforme une valeur selon la règle de gestion de la colonne.
     */
    private String transformValue(Object rawValue, CsvColumnDefinition column) {
        if (rawValue == null) {
            return ""; // Valeur vide pour les NULL
        }

        String rawString = rawValue.toString();
        String regleGestion = column.getRegleGestion();

        if (regleGestion == null) {
            return rawString;
        }

        return switch (regleGestion) {
            case "POSITION_VALEUR_POSSIBLE" -> transformPositionValeurPossible(rawString, column);
            case "TEXTE_DIRECT" -> rawString;
            case "NUMERIQUE_DIRECT" -> rawString;
            case "DATE_ISO" -> transformDateIso(rawString);
            default -> {
                log.warn("Règle de gestion inconnue: {} pour colonne {}", regleGestion, column.getCodeVariable());
                yield rawString;
            }
        };
    }

    /**
     * Transforme une valeur texte en position numérique selon les valeurs possibles.
     */
    private String transformPositionValeurPossible(String rawValue, CsvColumnDefinition column) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "";
        }

        // Rechercher la position de la valeur dans les valeurs possibles
        for (CsvColumnValues valeurPossible : column.getValeursPossibles()) {
            if (rawValue.equals(valeurPossible.getValeurPossible())) {
                return valeurPossible.getPosition().toString();
            }
        }

        log.warn("Valeur '{}' non trouvée dans les valeurs possibles pour la colonne {}",
                rawValue, column.getCodeVariable());
        return ""; // Valeur non trouvée
    }

    /**
     * Formate une date au format ISO.
     */
    private String transformDateIso(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "";
        }

        try {
            // Si c'est déjà une date correctement formatée, la retourner telle quelle
            if (rawValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return rawValue;
            }

            // Autres formats de date à gérer si nécessaire
            return rawValue; // Fallback
        } catch (Exception e) {
            log.warn("Erreur formatage date: {}", rawValue, e);
            return rawValue; // Retourner la valeur originale en cas d'erreur
        }
    }
}