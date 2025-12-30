
# Contexte
Je dois mettre en place un système d'export hebdomadaire de 3 fichiers CSV à partir de questionnaires stockés dans PostgreSQL. Les exports contiennent respectivement ~144, ~14 et ~500 colonnes.

# Contraintes techniques
- Base de données : PostgreSQL 15+
- Framework : Spring Boot 3.x + Spring Batch 5.x
- Langage : Java 21+
- Modèle de données : ~30 tables avec relations complexes (personne, adresse, formation, questionnaire, reponse, etc.)
- Volumétrie : Plusieurs milliers de questionnaires par semaine
- Planification : Export automatique tous les lundis à 2h du matin
- Pas de Python : Tout doit être fait en Java/Spring et PostgreSQL

# Architecture : Séparation Métier / Technique

## Principe fondamental
Le système sépare clairement deux responsabilités :

### 1. Dictionnaire MÉTIER (géré par les fonctionnels via Excel)
Le fichier Excel contient UNIQUEMENT les informations métier :
- `code_variable` : Nom de la colonne CSV (ex: Q_SEXE)
- `libelle` : Description fonctionnelle
- `format` : Format attendu (NUM, TEXTE, DATE)
- `type_question` : Type de question (LISTE, TEXTE, BOOLEEN, NUMERIQUE, CALC)
- `regle_gestion` : Règle de transformation (POSITION_VALEUR_POSSIBLE, TEXTE_DIRECT, etc.)
- `valeur_possible_1`, `valeur_possible_2`, ... `valeur_possible_N` : Valeurs possibles pour les listes

**IMPORTANT** : 
- ❌ Il n'y a PAS de colonne `ordre_colonne` dans l'Excel
- ✅ L'ordre des colonnes dans le CSV doit respecter STRICTEMENT l'ordre des lignes dans l'Excel (ligne 2, puis ligne 3, puis ligne 4...)
- ✅ La première ligne (row 0) contient les en-têtes et doit être ignorée

**L'Excel NE CONTIENT PAS** : source_table, source_column, sql_expression, join_tables

### 2. Mapping TECHNIQUE (géré par les développeurs en base de données)
Une table PostgreSQL `column_sql_mapping` qui fait le lien entre les `code_variable` et la technique :
- `code_variable` : Clé de liaison avec le dictionnaire métier
- `source_table` : Table PostgreSQL source (ex: personne)
- `source_column` : Colonne simple (ex: sexe)
- `sql_expression` : Expression SQL complète pour les cas complexes (sous-requêtes, agrégations)
- `join_tables` : Array des tables à joindre

**Contrainte** : Soit (source_table + source_column) soit sql_expression, jamais les deux.

## Structure du fichier Excel

Exemple de contenu (3 feuilles : CSV_1, CSV_2, CSV_3) :

**⚠️ IMPORTANT : L'ordre des lignes Excel = L'ordre des colonnes CSV**

| code_variable | libelle | format | type_question | regle_gestion | valeur_possible_1 | valeur_possible_2 | valeur_possible_3 |
|---------------|---------|--------|---------------|---------------|-------------------|-------------------|-------------------|
| Q_ID | Identifiant | NUM | NUMERIQUE | NUMERIQUE_DIRECT | | | |
| Q_SEXE | Sexe | NUM | LISTE | POSITION_VALEUR_POSSIBLE | Homme | Femme | Autre |
| Q_EMAIL | Email | TEXTE | TEXTE | TEXTE_DIRECT | | | |
| Q_VILLE | Ville | TEXTE | TEXTE | TEXTE_DIRECT | | | |
| Q_NB_FORMATIONS | Nb formations | NUM | CALC | NUMERIQUE_DIRECT | | | |
| Q_AGE | Âge | NUM | CALC | NUMERIQUE_DIRECT | | | |

→ Le CSV final aura les colonnes dans cet ordre : Q_ID, Q_SEXE, Q_EMAIL, Q_VILLE, Q_NB_FORMATIONS, Q_AGE

## Schéma PostgreSQL attendu
```sql
-- Schéma dédié
CREATE SCHEMA IF NOT EXISTS export_csv;

-- Table 1 : Staging pour import Excel (volatile)
-- ✅ AJOUT d'une colonne excel_row_number pour préserver l'ordre
CREATE TABLE export_csv.excel_csv_definition_stg (
    id BIGSERIAL PRIMARY KEY,
    nom_csv TEXT NOT NULL,
    excel_row_number INTEGER NOT NULL,  -- ✅ Ligne Excel (2, 3, 4...) pour préserver l'ordre
    code_variable TEXT NOT NULL,
    libelle TEXT,
    format TEXT,
    type_question TEXT,
    regle_gestion TEXT,
    index_valeur INTEGER,
    valeur_possible TEXT,
    date_import TIMESTAMP DEFAULT NOW(),
    fichier_source TEXT
);

-- Index sur l'ordre Excel
CREATE INDEX idx_excel_stg_row_order ON export_csv.excel_csv_definition_stg(nom_csv, excel_row_number);

-- Table 2 : Définitions métier normalisées (depuis Excel)
-- ✅ AJOUT de ordre_colonne calculé depuis excel_row_number
CREATE TABLE export_csv.csv_column_definition (
    id BIGSERIAL PRIMARY KEY,
    nom_csv TEXT NOT NULL,
    ordre_colonne INTEGER NOT NULL,  -- ✅ Calculé à partir de excel_row_number
    code_variable TEXT NOT NULL,
    libelle TEXT,
    format TEXT,
    type_question TEXT,
    regle_gestion TEXT,
    actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT NOW(),
    date_modification TIMESTAMP DEFAULT NOW(),
    UNIQUE(nom_csv, code_variable),
    UNIQUE(nom_csv, ordre_colonne)  -- ✅ Garantir l'unicité de l'ordre
);

-- Table 3 : Valeurs possibles (depuis Excel)
CREATE TABLE export_csv.csv_column_values (
    id BIGSERIAL PRIMARY KEY,
    column_definition_id BIGINT REFERENCES export_csv.csv_column_definition(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    valeur_possible TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT NOW(),
    UNIQUE(column_definition_id, position)
);

-- Table 4 : Mapping technique SQL (géré par les devs)
CREATE TABLE export_csv.column_sql_mapping (
    id BIGSERIAL PRIMARY KEY,
    code_variable TEXT UNIQUE NOT NULL,
    
    -- Option 1 : Colonne simple
    source_table TEXT,
    source_column TEXT,
    
    -- Option 2 : Expression SQL complexe
    sql_expression TEXT,
    
    -- Métadonnées
    join_tables TEXT[],
    requires_subquery BOOLEAN DEFAULT FALSE,
    is_aggregation BOOLEAN DEFAULT FALSE,
    description TEXT,
    exemple_valeur TEXT,
    
    date_creation TIMESTAMP DEFAULT NOW(),
    date_modification TIMESTAMP DEFAULT NOW(),
    
    -- Contrainte : soit (table + colonne) soit sql_expression
    CONSTRAINT check_mapping_method CHECK (
        (source_table IS NOT NULL AND source_column IS NOT NULL AND sql_expression IS NULL)
        OR (source_table IS NULL AND source_column IS NULL AND sql_expression IS NOT NULL)
    )
);

-- Table 5 : Définitions des jointures réutilisables
CREATE TABLE export_csv.join_definition (
    table_name TEXT PRIMARY KEY,
    join_sql TEXT NOT NULL,
    depends_on TEXT[],
    priority INTEGER NOT NULL
);

-- Table 6 : Tracking des exports
CREATE TABLE export_csv.export_execution (
    id BIGSERIAL PRIMARY KEY,
    job_execution_id BIGINT,
    nom_csv TEXT NOT NULL,
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP,
    statut TEXT,
    nb_lignes_exportees INTEGER,
    chemin_fichier TEXT,
    message_erreur TEXT,
    duree_secondes NUMERIC
);

-- Vue combinée : Métier + Technique
CREATE OR REPLACE VIEW export_csv.v_complete_column_definition AS
SELECT 
    cd.id,
    cd.nom_csv,
    cd.ordre_colonne,  -- ✅ Ordre préservé depuis Excel
    cd.code_variable,
    cd.libelle,
    cd.format,
    cd.type_question,
    cd.regle_gestion,
    cd.actif,
    
    -- Mapping technique
    sm.source_table,
    sm.source_column,
    sm.sql_expression,
    sm.join_tables,
    sm.requires_subquery,
    sm.is_aggregation,
    
    -- Valeurs possibles agrégées
    ARRAY_AGG(cv.valeur_possible ORDER BY cv.position) 
        FILTER (WHERE cv.valeur_possible IS NOT NULL) as valeurs_possibles
FROM export_csv.csv_column_definition cd
LEFT JOIN export_csv.column_sql_mapping sm ON sm.code_variable = cd.code_variable
LEFT JOIN export_csv.csv_column_values cv ON cv.column_definition_id = cd.id
WHERE cd.actif = true
GROUP BY 
    cd.id, cd.nom_csv, cd.ordre_colonne, cd.code_variable, cd.libelle, 
    cd.format, cd.type_question, cd.regle_gestion, cd.actif,
    sm.source_table, sm.source_column, sm.sql_expression, sm.join_tables, 
    sm.requires_subquery, sm.is_aggregation;

-- Index pour performance
CREATE INDEX idx_csv_def_nom_ordre ON export_csv.csv_column_definition(nom_csv, ordre_colonne);
CREATE INDEX idx_csv_values_col_pos ON export_csv.csv_column_values(column_definition_id, position);
CREATE INDEX idx_column_mapping_code ON export_csv.column_sql_mapping(code_variable);
CREATE INDEX idx_export_exec_date ON export_csv.export_execution(date_debut DESC);
```

## Procédure de normalisation (MISE À JOUR)
```sql
-- ✅ Procédure qui calcule ordre_colonne depuis excel_row_number
CREATE OR REPLACE PROCEDURE export_csv.normalize_definitions()
LANGUAGE plpgsql
AS $$
BEGIN
    -- Désactiver les anciennes définitions pour les CSV présents dans le staging
    UPDATE export_csv.csv_column_definition
    SET actif = false
    WHERE nom_csv IN (SELECT DISTINCT nom_csv FROM export_csv.excel_csv_definition_stg);
    
    -- Insérer/mettre à jour les définitions de colonnes
    -- ✅ ordre_colonne est calculé via ROW_NUMBER() sur excel_row_number
    INSERT INTO export_csv.csv_column_definition 
    (nom_csv, ordre_colonne, code_variable, libelle, format, type_question, regle_gestion, actif)
    SELECT 
        nom_csv,
        ROW_NUMBER() OVER (PARTITION BY nom_csv ORDER BY excel_row_number) as ordre_colonne,  -- ✅ Préserve l'ordre Excel
        code_variable,
        libelle,
        format,
        type_question,
        COALESCE(regle_gestion, 'POSITION_VALEUR_POSSIBLE') as regle_gestion,
        true as actif
    FROM (
        SELECT DISTINCT ON (nom_csv, code_variable)
            nom_csv,
            excel_row_number,
            code_variable,
            libelle,
            format,
            type_question,
            regle_gestion
        FROM export_csv.excel_csv_definition_stg
        ORDER BY nom_csv, code_variable, excel_row_number
    ) sub
    ON CONFLICT (nom_csv, code_variable) 
    DO UPDATE SET
        ordre_colonne = EXCLUDED.ordre_colonne,
        libelle = EXCLUDED.libelle,
        format = EXCLUDED.format,
        type_question = EXCLUDED.type_question,
        regle_gestion = EXCLUDED.regle_gestion,
        actif = true,
        date_modification = NOW();
    
    -- Supprimer les anciennes valeurs possibles
    DELETE FROM export_csv.csv_column_values
    WHERE column_definition_id IN (
        SELECT cd.id 
        FROM export_csv.csv_column_definition cd
        WHERE cd.nom_csv IN (SELECT DISTINCT nom_csv FROM export_csv.excel_csv_definition_stg)
    );
    
    -- Insérer les nouvelles valeurs possibles
    INSERT INTO export_csv.csv_column_values (column_definition_id, position, valeur_possible)
    SELECT 
        cd.id,
        stg.index_valeur,
        stg.valeur_possible
    FROM export_csv.excel_csv_definition_stg stg
    JOIN export_csv.csv_column_definition cd 
        ON cd.nom_csv = stg.nom_csv 
        AND cd.code_variable = stg.code_variable
    WHERE stg.valeur_possible IS NOT NULL
    AND stg.index_valeur IS NOT NULL
    ORDER BY cd.id, stg.index_valeur;
    
    -- Vérifier les colonnes sans mapping technique
    RAISE NOTICE 'Colonnes sans mapping technique :';
    FOR rec IN 
        SELECT cd.code_variable, cd.libelle, cd.ordre_colonne
        FROM export_csv.csv_column_definition cd
        LEFT JOIN export_csv.column_sql_mapping sm ON sm.code_variable = cd.code_variable
        WHERE cd.actif = true
        AND sm.id IS NULL
        ORDER BY cd.nom_csv, cd.ordre_colonne  -- ✅ Ordre préservé
    LOOP
        RAISE WARNING '  - [%] % : %', rec.ordre_colonne, rec.code_variable, rec.libelle;
    END LOOP;
    
    RAISE NOTICE 'Normalisation terminée';
END;
$$;
```

## ExcelItemReader attendu (Spring Batch)
```java
/**
 * ✅ ItemReader qui lit l'Excel et préserve l'ordre des lignes
 */
@Component
@StepScope
public class ExcelItemReader extends AbstractItemStreamItemReader<ExcelRowDTO> {
    
    @Value("#{jobParameters['excelFilePath']}")
    private String excelFilePath;
    
    private Iterator<Row> currentSheetIterator;
    private Iterator<Sheet> sheetIterator;
    private Workbook workbook;
    private String currentSheetName;
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            FileInputStream fis = new FileInputStream(excelFilePath);
            workbook = new XSSFWorkbook(fis);
            sheetIterator = workbook.iterator();
            
            // Passer à la première feuille
            if (sheetIterator.hasNext()) {
                Sheet sheet = sheetIterator.next();
                currentSheetName = sheet.getSheetName();
                currentSheetIterator = sheet.iterator();
                
                // ✅ IMPORTANT : Ignorer la première ligne (header)
                if (currentSheetIterator.hasNext()) {
                    currentSheetIterator.next(); // Skip header row
                }
            }
        } catch (IOException e) {
            throw new ItemStreamException("Erreur ouverture Excel", e);
        }
    }
    
    @Override
    public ExcelRowDTO read() {
        while (true) {
            // Si plus de lignes dans la feuille courante
            if (!currentSheetIterator.hasNext()) {
                // Passer à la feuille suivante
                if (sheetIterator.hasNext()) {
                    Sheet sheet = sheetIterator.next();
                    currentSheetName = sheet.getSheetName();
                    currentSheetIterator = sheet.iterator();
                    
                    // ✅ Skip header de la nouvelle feuille
                    if (currentSheetIterator.hasNext()) {
                        currentSheetIterator.next();
                    }
                    continue;
                } else {
                    // Fin de toutes les feuilles
                    return null;
                }
            }
            
            Row row = currentSheetIterator.next();
            
            // Ignorer les lignes vides
            if (isRowEmpty(row)) {
                continue;
            }
            
            // ✅ CRUCIAL : Capturer le numéro de ligne Excel
            int excelRowNumber = row.getRowNum();
            
            return mapRowToDTO(row, currentSheetName, excelRowNumber);
        }
    }
    
    private ExcelRowDTO mapRowToDTO(Row row, String sheetName, int excelRowNumber) {
        ExcelRowDTO dto = new ExcelRowDTO();
        dto.setNomCsv(sheetName);
        dto.setExcelRowNumber(excelRowNumber);  // ✅ Préserver l'ordre
        dto.setCodeVariable(getCellAsString(row, 0));
        dto.setLibelle(getCellAsString(row, 1));
        dto.setFormat(getCellAsString(row, 2));
        dto.setTypeQuestion(getCellAsString(row, 3));
        dto.setRegleGestion(getCellAsString(row, 4));
        
        // Extraire les valeurs possibles (colonnes 5+)
        Map<Integer, String> valeursPossibles = new HashMap<>();
        int position = 1;
        for (int colIndex = 5; colIndex < row.getLastCellNum(); colIndex++) {
            String valeur = getCellAsString(row, colIndex);
            if (valeur != null && !valeur.trim().isEmpty()) {
                valeursPossibles.put(position++, valeur);
            }
        }
        dto.setValeursPossibles(valeursPossibles);
        
        return dto;
    }
    
    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellAsString(row, i);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private String getCellAsString(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
    
    @Override
    public void close() throws ItemStreamException {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException e) {
                throw new ItemStreamException("Erreur fermeture Excel", e);
            }
        }
    }
}
```

## DTO attendu
```java
/**
 * ✅ DTO avec excel_row_number pour préserver l'ordre
 */
@Data
public class ExcelRowDTO {
    private String nomCsv;
    private Integer excelRowNumber;  // ✅ Numéro de ligne Excel (2, 3, 4...)
    private String codeVariable;
    private String libelle;
    private String format;
    private String typeQuestion;
    private String regleGestion;
    private Map<Integer, String> valeursPossibles;  // position → valeur
}
```

## Processor attendu
```java
/**
 * Transforme ExcelRowDTO en plusieurs entités de staging
 * (une par valeur possible si applicable)
 */
@Component
public class ExcelRowProcessor implements ItemProcessor<ExcelRowDTO, List<ExcelDefinitionStaging>> {
    
    @Override
    public List<ExcelDefinitionStaging> process(ExcelRowDTO item) {
        List<ExcelDefinitionStaging> stagingRecords = new ArrayList<>();
        
        if (item.getValeursPossibles().isEmpty()) {
            // Pas de valeurs possibles : 1 seul enregistrement
            ExcelDefinitionStaging staging = new ExcelDefinitionStaging();
            staging.setNomCsv(item.getNomCsv());
            staging.setExcelRowNumber(item.getExcelRowNumber());  // ✅
            staging.setCodeVariable(item.getCodeVariable());
            staging.setLibelle(item.getLibelle());
            staging.setFormat(item.getFormat());
            staging.setTypeQuestion(item.getTypeQuestion());
            staging.setRegleGestion(item.getRegleGestion());
            staging.setDateImport(LocalDateTime.now());
            
            stagingRecords.add(staging);
        } else {
            // Avec valeurs possibles : 1 enregistrement par valeur
            for (Map.Entry<Integer, String> entry : item.getValeursPossibles().entrySet()) {
                ExcelDefinitionStaging staging = new ExcelDefinitionStaging();
                staging.setNomCsv(item.getNomCsv());
                staging.setExcelRowNumber(item.getExcelRowNumber());  // ✅
                staging.setCodeVariable(item.getCodeVariable());
                staging.setLibelle(item.getLibelle());
                staging.setFormat(item.getFormat());
                staging.setTypeQuestion(item.getTypeQuestion());
                staging.setRegleGestion(item.getRegleGestion());
                staging.setIndexValeur(entry.getKey());
                staging.setValeurPossible(entry.getValue());
                staging.setDateImport(LocalDateTime.now());
                
                stagingRecords.add(staging);
            }
        }
        
        return stagingRecords;
    }
}
```

## Questions critiques sur la préservation de l'ordre

**Q1** : Validation de l'ordre
- Comment valider que l'ordre Excel est bien préservé jusqu'au CSV final ?
- Faut-il un test automatique qui vérifie l'ordre des colonnes ?

**Q2** : Gestion des modifications Excel
- Si l'utilisateur inverse 2 lignes dans l'Excel, l'ordre doit-il être mis à jour automatiquement ?
- Comment gérer le cas où une colonne change de position ?

**Q3** : Performance avec ORDER BY
```sql
SELECT ... 
FROM export_csv.v_complete_column_definition
WHERE nom_csv = 'CSV_1'
ORDER BY ordre_colonne  -- ✅ Coût de ce tri ?
```
- Est-ce que l'index sur `ordre_colonne` suffit pour de bonnes performances ?
- Faut-il matérialiser la vue ?

**Q4** : Génération du header CSV
```java
// Comment construire le header en respectant l'ordre ?
List<String> headers = columnRepository
    .findByNomCsvAndActifTrueOrderByOrdreColonneAsc(nomCsv)
    .stream()
    .map(CsvColumnDefinition::getCodeVariable)
    .toList();
```

**Q5** : Transformation des données en respectant l'ordre
```java
@Component
public class CsvLineProcessor implements ItemProcessor<Map<String, Object>, String[]> {
    
    private List<CsvColumnDefinition> orderedColumns;
    
    @BeforeStep
    public void beforeStep(@Value("#{jobParameters['nomCsv']}") String nomCsv) {
        // ✅ Charger les colonnes dans l'ordre
        this.orderedColumns = columnRepository
            .findByNomCsvAndActifTrueOrderByOrdreColonneAsc(nomCsv);
    }
    
    @Override
    public String[] process(Map<String, Object> rawData) {
        // ✅ Construire le tableau dans l'ordre exact
        return orderedColumns.stream()
            .map(col -> transformValue(rawData.get(col.getCodeVariable()), col))
            .toArray(String[]::new);
    }
}
```

## Cas de test pour valider l'ordre
```java
@Test
void testExcelRowOrderIsPreserved() {
    // Given: Excel avec cet ordre
    // Ligne 2: Q_ID
    // Ligne 3: Q_SEXE
    // Ligne 4: Q_EMAIL
    // Ligne 5: Q_VILLE
    
    // When: Import + Normalisation
    excelImportService.importExcel(excelFile);
    
    // Then: Vérifier l'ordre en base
    List<CsvColumnDefinition> columns = columnRepository
        .findByNomCsvOrderByOrdreColonneAsc("CSV_1");
    
    assertThat(columns).extracting("codeVariable")
        .containsExactly("Q_ID", "Q_SEXE", "Q_EMAIL", "Q_VILLE");
    
    assertThat(columns).extracting("ordreColonne")
        .containsExactly(1, 2, 3, 4);
}

@Test
void testCsvHeaderMatchesExcelOrder() {
    // Given: Définitions en base
    
    // When: Génération du CSV
    String csvContent = exportService.exportToCsv("CSV_1");
    
    // Then: Première ligne du CSV = header dans l'ordre
    String header = csvContent.lines().findFirst().orElse("");
    assertThat(header).isEqualTo("Q_ID;Q_SEXE;Q_EMAIL;Q_VILLE");
}
```


### 6. Import Excel avec Spring Batch

**Q6.1** : Comment lire un fichier Excel multi-feuilles avec Apache POI dans un ItemReader ?
- Faut-il un Reader par feuille ou un seul Reader qui itère sur toutes les feuilles ?
- Comment gérer la première ligne (header) à ignorer ?

**Q6.2** : Structure du DTO pour transporter les données Excel ?
```java
public class ExcelRowDTO {
    private String nomCsv;
    private Integer ordreColonne;
    private String codeVariable;
    private String libelle;
    // ...
    private Map<Integer, String> valeursPossibles; // valeur_possible_1, 2, 3...
}
```

**Q6.3** : Comment mapper dynamiquement les colonnes valeur_possible_1..N vers une Map ?

### 7. Génération de requête dynamique

**Q7.1** : Comment récupérer la requête SQL générée par PostgreSQL et l'injecter dans un JdbcCursorItemReader ?
```java
@Bean
@StepScope
public JdbcCursorItemReader<Map<String, Object>> dynamicQueryItemReader(
    @Value("#{jobParameters['nomCsv']}") String nomCsv) {
    
    // Comment appeler build_export_query(nomCsv) et utiliser le résultat ?
}
```

**Q7.2** : Comment passer les paramètres `:dateDebut` et `:dateFin` à la requête générée ?

**Q7.3** : Comment gérer les résultats avec 500 colonnes dans un Map<String, Object> ?
- Y a-t-il des limites de performance ?
- Faut-il utiliser une projection DTO ou rester sur Map ?

### 8. Transformation des valeurs

**Q8.1** : Où faire la transformation POSITION_VALEUR_POSSIBLE ?
- Option A : En PostgreSQL (via fonction transform_value dans la requête)
- Option B : En Java (via ItemProcessor)
- Quelle option est la plus performante ?

**Q8.2** : Si transformation en Java, comment récupérer dynamiquement les valeurs possibles ?
```java
@Component
public class CsvLineProcessor implements ItemProcessor<Map<String, Object>, String[]> {
    
    // Comment charger les définitions et valeurs possibles une seule fois ?
    // Comment mapper les valeurs brutes vers les valeurs codifiées ?
}
```

### 9. Writer CSV avec header dynamique

**Q9.1** : Comment construire dynamiquement l'en-tête du CSV depuis les définitions en base ?
```java
@Bean
@StepScope
public FlatFileItemWriter<String[]> csvFileItemWriter(
    @Value("#{jobParameters['nomCsv']}") String nomCsv) {
    
    // Comment récupérer les code_variable ordonnés pour le header ?
    // Comment formatter le nom du fichier avec la date ?
}
```

**Q9.2** : Configuration optimale pour 500 colonnes ?
- Délimiteur : `;` ou `,` ?
- Encoding : UTF-8 ?
- Line separator : `\n` ou `\r\n` ?

### 10. Orchestration et planification

**Q10.1** : Comment déclencher séquentiellement les 3 exports (CSV_1, CSV_2, CSV_3) ?
```java
@Scheduled(cron = "0 0 2 ? * MON")
public void weeklyExport() {
    // Lancer exportCsvJob avec nomCsv = "CSV_1"
    // Puis nomCsv = "CSV_2"
    // Puis nomCsv = "CSV_3"
    // Comment gérer les JobParameters uniques ?
}
```

**Q10.2** : Stratégie de retry si un CSV échoue ?
- Relancer uniquement le CSV en échec ?
- Relancer tout le workflow ?

**Q10.3** : Comment logger les métriques (nb lignes, durée) dans la table export_execution ?

### 11. Initialisation du mapping technique

**Q11.1** : Comment initialiser les 100-200 lignes de mapping technique ?
- Script Flyway/Liquibase ?
- Job Spring Batch dédié ?
- Interface d'administration ?

**Q11.2** : Comment détecter automatiquement les colonnes sans mapping technique ?
```java
@Component
public class MappingValidator {
    // Vérifier que tous les code_variable ont un mapping
    // Logger les colonnes manquantes
}
```

### 12. Performance et optimisation

**Q12.1** : Taille de chunk recommandée pour 10 000 questionnaires avec 500 colonnes ?

**Q12.2** : Faut-il utiliser des index spécifiques sur les tables de métadonnées ?

**Q12.3** : Comment optimiser la requête générée pour éviter les N+1 queries ?
- Les LEFT JOIN suffisent-ils ?
- Faut-il utiliser des sous-requêtes LATERAL ?

**Q12.4** : Stratégie de cache pour les définitions de colonnes ?
- Les charger une fois au démarrage du Step ?
- Les recharger à chaque chunk ?

### 13. Tests

**Q13.1** : Comment tester unitairement la fonction `build_export_query` ?
```java
@Test
void testBuildExportQuery() {
    // Insérer des définitions de test
    // Appeler la fonction PostgreSQL
    // Vérifier que la requête générée est correcte
}
```

**Q13.2** : Comment tester l'import Excel avec un fichier de test ?

**Q13.3** : Comment tester un export complet end-to-end ?

## Cas d'usage à gérer

### Cas 1 : Colonne simple (1 table)
```
Excel :
- code_variable: Q_SEXE
- regle_gestion: POSITION_VALEUR_POSSIBLE
- valeur_possible_1: Homme
- valeur_possible_2: Femme

Mapping technique (SQL) :
INSERT INTO column_sql_mapping VALUES
('Q_SEXE', 'personne', 'sexe', ARRAY['personne']);

Résultat attendu dans la requête :
p.sexe AS Q_SEXE

Transformation :
"Homme" → "1"
"Femme" → "2"
```

### Cas 2 : Colonne avec 2 jointures
```
Excel :
- code_variable: Q_VILLE
- regle_gestion: TEXTE_DIRECT

Mapping technique :
INSERT INTO column_sql_mapping VALUES
('Q_VILLE', 'adresse', 'ville', ARRAY['personne', 'adresse']);

Résultat attendu :
LEFT JOIN personne p ON p.questionnaire_id = q.id
LEFT JOIN adresse a ON a.personne_id = p.id
...
a.ville AS Q_VILLE
```

### Cas 3 : Sous-requête simple
```
Excel :
- code_variable: Q_NB_FORMATIONS
- regle_gestion: NUMERIQUE_DIRECT

Mapping technique :
INSERT INTO column_sql_mapping (code_variable, sql_expression) VALUES
('Q_NB_FORMATIONS', 
 '(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id)');

Résultat attendu :
(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id) AS Q_NB_FORMATIONS
```

### Cas 4 : Agrégation complexe
```
Excel :
- code_variable: Q_SECTEUR_PRINCIPAL
- regle_gestion: TEXTE_DIRECT

Mapping technique :
INSERT INTO column_sql_mapping (code_variable, sql_expression) VALUES
('Q_SECTEUR_PRINCIPAL', 
 '(SELECT f.secteur FROM formation_participation fp JOIN formation f ON f.id = fp.formation_id WHERE fp.questionnaire_id = q.id GROUP BY f.secteur ORDER BY COUNT(*) DESC LIMIT 1)');
```

### Cas 5 : Calcul avec colonne d'une autre table
```
Excel :
- code_variable: Q_AGE
- regle_gestion: NUMERIQUE_DIRECT

Mapping technique :
INSERT INTO column_sql_mapping (code_variable, sql_expression, join_tables) VALUES
('Q_AGE', 
 'EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance))',
 ARRAY['personne']);

Note : Nécessite la jointure vers personne mais n'est pas une sous-requête
```

## Livrables attendus

1. **Schéma SQL complet** :
   - Tables avec contraintes et index
   - Procédure de normalisation
   - Fonctions de génération de requête
   - Script d'initialisation du mapping technique

2. **Configuration Spring Batch** :
   - application.yml (datasource, batch config)
   - Configuration des Jobs (Import Excel + Export CSV)
   - Configuration des Steps

3. **Classes Java principales** :
   - Entités JPA (CsvColumnDefinition, ColumnSqlMapping, etc.)
   - ExcelItemReader (lecture Excel)
   - DynamicQueryItemReader (exécution requête générée)
   - CsvLineProcessor (transformation optionnelle)
   - FlatFileItemWriter (écriture CSV)
   - Scheduler (@Scheduled pour les exports hebdomadaires)

4. **Services métier** :
   - ExcelImportService
   - QueryBuilderService (appelle les fonctions PostgreSQL)
   - TransformationService (règles de transformation)
   - ExportTrackingService (logs dans export_execution)

5. **Tests** :
   - Tests unitaires (fonctions PostgreSQL)
   - Tests d'intégration (Jobs Spring Batch)
   - Tests end-to-end (fichier Excel → CSV final)

6. **Documentation** :
   - Guide d'ajout d'une nouvelle colonne
   - Guide d'ajout d'un nouveau CSV
   - Guide de maintenance du mapping technique

## Critères de succès

✅ Import d'un fichier Excel avec 500 colonnes en < 30 secondes
✅ Génération de la requête SQL dynamique en < 1 seconde
✅ Export de 10 000 questionnaires avec 500 colonnes en < 5 minutes
✅ Ajout d'une colonne simple = modification Excel uniquement
✅ Ajout d'une colonne complexe = modification Excel + 1 ligne SQL de mapping
✅ Les 3 CSV sont exportés automatiquement chaque lundi à 2h
✅ Logs détaillés dans export_execution (durée, nb lignes, erreurs)
✅ Alerting en cas d'échec (email/Slack)
✅ Pas de code Python (100% Java + PostgreSQL)
✅ L'ordre des colonnes dans le CSV final correspond EXACTEMENT à l'ordre des lignes dans l'Excel
✅ Ajout d'une ligne en position N dans l'Excel → colonne en position N dans le CSV
✅ Import d'un fichier Excel avec 500 colonnes en < 30 secondes
✅ Génération de la requête SQL dynamique en < 1 seconde
✅ Export de 10 000 questionnaires avec 500 colonnes en < 5 minutes
✅ Les 3 CSV sont exportés automatiquement chaque lundi à 2h
✅ Mise en place des TU avec une couverture de 90%, validation des cas passants et non passants avec displayName en français

## Contraintes de performance

- Export de 10 000 questionnaires : < 5 minutes par CSV
- Génération de requête SQL : < 1 seconde
- Import Excel : < 30 secondes
- Mémoire JVM : < 2 GB pendant l'export


Peux-tu me fournir une architecture complète, détaillée et prête à implémenter répondant à ce besoin ?