# Contexte
Je dois mettre en place un système d'export hebdomadaire de 3 fichiers CSV à partir de questionnaires stockés dans PostgreSQL. Les exports contiennent respectivement ~144, ~14 et ~500 colonnes.

# Contraintes techniques
- Base de données : PostgreSQL 14+
- Framework : Spring Boot 3.x + Spring Batch 5.x
- Langage : Java 17+
- Modèle de données : ~30 tables avec relations complexes (personne, adresse, formation, questionnaire, reponse, etc.)
- Volumétrie : Plusieurs milliers de questionnaires par semaine
- Planification : Export automatique tous les lundis à 2h du matin
- Pas de Python : Tout doit être fait en Java/Spring et PostgreSQL

# Architecture : Séparation Métier / Technique

## Principe fondamental
Le système sépare clairement deux responsabilités :

### 1. Dictionnaire MÉTIER (géré par les fonctionnels via Excel)
Le fichier Excel contient UNIQUEMENT les informations métier :
- `ordre_colonne` : Position de la colonne dans le CSV
- `code_variable` : Nom de la colonne CSV (ex: Q_SEXE)
- `libelle` : Description fonctionnelle
- `format` : Format attendu (NUM, TEXTE, DATE)
- `type_question` : Type de question (LISTE, TEXTE, BOOLEEN, NUMERIQUE, CALC)
- `regle_gestion` : Règle de transformation (POSITION_VALEUR_POSSIBLE, TEXTE_DIRECT, etc.)
- `valeur_possible_1`, `valeur_possible_2`, ... `valeur_possible_N` : Valeurs possibles pour les listes

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

| ordre_colonne | code_variable | libelle | format | type_question | regle_gestion | valeur_possible_1 | valeur_possible_2 | valeur_possible_3 |
|---------------|---------------|---------|--------|---------------|---------------|-------------------|-------------------|-------------------|
| 1 | Q_ID | Identifiant | NUM | NUMERIQUE | NUMERIQUE_DIRECT | | | |
| 2 | Q_SEXE | Sexe | NUM | LISTE | POSITION_VALEUR_POSSIBLE | Homme | Femme | Autre |
| 3 | Q_EMAIL | Email | TEXTE | TEXTE | TEXTE_DIRECT | | | |
| 4 | Q_VILLE | Ville | TEXTE | TEXTE | TEXTE_DIRECT | | | |
| 5 | Q_NB_FORMATIONS | Nb formations | NUM | CALC | NUMERIQUE_DIRECT | | | |
| 6 | Q_AGE | Âge | NUM | CALC | NUMERIQUE_DIRECT | | | |

## Schéma PostgreSQL attendu
```sql
-- Schéma dédié
CREATE SCHEMA IF NOT EXISTS export_csv;

-- Table 1 : Staging pour import Excel (volatile)
CREATE TABLE export_csv.excel_csv_definition_stg (
    id BIGSERIAL PRIMARY KEY,
    nom_csv TEXT NOT NULL,
    ordre_colonne INTEGER NOT NULL,
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

-- Table 2 : Définitions métier normalisées (depuis Excel)
CREATE TABLE export_csv.csv_column_definition (
    id BIGSERIAL PRIMARY KEY,
    nom_csv TEXT NOT NULL,
    ordre_colonne INTEGER NOT NULL,
    code_variable TEXT NOT NULL,
    libelle TEXT,
    format TEXT,
    type_question TEXT,
    regle_gestion TEXT,
    actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT NOW(),
    date_modification TIMESTAMP DEFAULT NOW(),
    UNIQUE(nom_csv, code_variable)
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
    cd.ordre_colonne,
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

## Exemples de mapping technique
```sql
-- Initialisation du mapping technique (fait 1 fois par les devs)
INSERT INTO export_csv.join_definition (table_name, join_sql, depends_on, priority) VALUES
('personne', 'LEFT JOIN personne p ON p.questionnaire_id = q.id', ARRAY[]::TEXT[], 1),
('adresse', 'LEFT JOIN adresse a ON a.personne_id = p.id', ARRAY['personne'], 2),
('formation_participation', 'LEFT JOIN formation_participation fp ON fp.questionnaire_id = q.id', ARRAY[]::TEXT[], 1),
('formation', 'LEFT JOIN formation f ON f.id = fp.formation_id', ARRAY['formation_participation'], 2),
('reponse', 'LEFT JOIN reponse r ON r.questionnaire_id = q.id', ARRAY[]::TEXT[], 1);

-- Mapping pour colonnes simples
INSERT INTO export_csv.column_sql_mapping (code_variable, source_table, source_column, join_tables, description) VALUES
('Q_ID', 'questionnaires', 'id', ARRAY[]::TEXT[], 'Identifiant questionnaire'),
('Q_DATE_CREATION', 'questionnaires', 'date_creation', ARRAY[]::TEXT[], 'Date de création'),
('Q_SEXE', 'personne', 'sexe', ARRAY['personne'], 'Sexe du participant'),
('Q_NOM', 'personne', 'nom', ARRAY['personne'], 'Nom du participant'),
('Q_EMAIL', 'personne', 'email', ARRAY['personne'], 'Email du participant'),
('Q_VILLE', 'adresse', 'ville', ARRAY['personne', 'adresse'], 'Ville de résidence'),
('Q_CODE_POSTAL', 'adresse', 'code_postal', ARRAY['personne', 'adresse'], 'Code postal');

-- Mapping pour colonnes calculées (sous-requêtes)
INSERT INTO export_csv.column_sql_mapping (code_variable, sql_expression, requires_subquery, description) VALUES
('Q_NB_FORMATIONS', 
 '(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id)', 
 TRUE, 
 'Nombre de formations suivies'),

('Q_AGE', 
 'EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance))', 
 FALSE, 
 'Âge calculé à partir de la date de naissance'),

('Q_SECTEUR_PRINCIPAL', 
 $sql$(SELECT f.secteur 
       FROM formation_participation fp 
       JOIN formation f ON f.id = fp.formation_id 
       WHERE fp.questionnaire_id = q.id 
       GROUP BY f.secteur 
       ORDER BY COUNT(*) DESC 
       LIMIT 1)$sql$, 
 TRUE, 
 'Secteur de formation le plus fréquent');
```

## Fonctions PostgreSQL attendues

### Fonction 1 : Normalisation des données Excel → Tables métier
```sql
CREATE OR REPLACE PROCEDURE export_csv.normalize_definitions()
LANGUAGE plpgsql
AS $$
BEGIN
    -- Désactiver les anciennes définitions pour les CSV présents dans le staging
    UPDATE export_csv.csv_column_definition
    SET actif = false
    WHERE nom_csv IN (SELECT DISTINCT nom_csv FROM export_csv.excel_csv_definition_stg);
    
    -- Insérer/mettre à jour les définitions de colonnes
    INSERT INTO export_csv.csv_column_definition 
    (nom_csv, ordre_colonne, code_variable, libelle, format, type_question, regle_gestion, actif)
    SELECT DISTINCT
        nom_csv,
        ordre_colonne,
        code_variable,
        libelle,
        format,
        type_question,
        COALESCE(regle_gestion, 'POSITION_VALEUR_POSSIBLE') as regle_gestion,
        true as actif
    FROM export_csv.excel_csv_definition_stg
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
        SELECT cd.code_variable, cd.libelle
        FROM export_csv.csv_column_definition cd
        LEFT JOIN export_csv.column_sql_mapping sm ON sm.code_variable = cd.code_variable
        WHERE cd.actif = true
        AND sm.id IS NULL
        ORDER BY cd.nom_csv, cd.ordre_colonne
    LOOP
        RAISE WARNING '  - % : %', rec.code_variable, rec.libelle;
    END LOOP;
    
    RAISE NOTICE 'Normalisation terminée';
END;
$$;
```

### Fonction 2 : Construction dynamique de la requête SQL
```sql
CREATE OR REPLACE FUNCTION export_csv.build_export_query(p_nom_csv TEXT)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_select_clause TEXT;
    v_join_clause TEXT;
    v_required_joins TEXT[];
    v_query TEXT;
BEGIN
    -- Construire la clause SELECT
    SELECT STRING_AGG(
        CASE 
            WHEN sql_expression IS NOT NULL THEN
                sql_expression || ' AS ' || code_variable
            WHEN source_table IS NOT NULL AND source_column IS NOT NULL THEN
                export_csv.get_table_alias(source_table) || '.' || source_column || ' AS ' || code_variable
            ELSE
                'NULL AS ' || code_variable || ' -- ⚠️ MAPPING MANQUANT'
        END,
        E',\n        '
        ORDER BY ordre_colonne
    )
    INTO v_select_clause
    FROM export_csv.v_complete_column_definition
    WHERE nom_csv = p_nom_csv;
    
    IF v_select_clause IS NULL THEN
        RAISE EXCEPTION 'Aucune colonne trouvée pour le CSV : %', p_nom_csv;
    END IF;
    
    -- Collecter les jointures nécessaires
    WITH required_joins AS (
        SELECT DISTINCT UNNEST(join_tables) as table_name
        FROM export_csv.v_complete_column_definition
        WHERE nom_csv = p_nom_csv
        AND join_tables IS NOT NULL
        
        UNION
        
        SELECT DISTINCT source_table
        FROM export_csv.v_complete_column_definition
        WHERE nom_csv = p_nom_csv
        AND source_table IS NOT NULL
        AND source_table != 'questionnaires'
    )
    SELECT ARRAY_AGG(table_name)
    INTO v_required_joins
    FROM required_joins;
    
    -- Construire les jointures
    IF v_required_joins IS NOT NULL THEN
        v_join_clause := export_csv.build_joins(v_required_joins);
    ELSE
        v_join_clause := '';
    END IF;
    
    -- Assembler la requête finale
    v_query := FORMAT(
        $query$
SELECT 
    q.id AS questionnaire_id,
    %s
FROM questionnaires q
%s
WHERE q.statut = 'VALIDE'
AND q.date_creation >= :dateDebut
AND q.date_creation <= :dateFin
ORDER BY q.id
        $query$,
        v_select_clause,
        v_join_clause
    );
    
    RETURN v_query;
END;
$$;

-- Fonction helper pour les alias de tables
CREATE OR REPLACE FUNCTION export_csv.get_table_alias(p_table_name TEXT)
RETURNS TEXT
IMMUTABLE
LANGUAGE sql
AS $$
    SELECT CASE p_table_name
        WHEN 'questionnaires' THEN 'q'
        WHEN 'personne' THEN 'p'
        WHEN 'adresse' THEN 'a'
        WHEN 'formation' THEN 'f'
        WHEN 'formation_participation' THEN 'fp'
        WHEN 'reponse' THEN 'r'
        ELSE SUBSTRING(p_table_name, 1, 1)
    END;
$$;

-- Fonction de construction des jointures
CREATE OR REPLACE FUNCTION export_csv.build_joins(p_tables TEXT[])
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_ordered_tables TEXT[];
    v_join_clause TEXT := '';
    v_table TEXT;
BEGIN
    -- Ordonner les jointures selon les dépendances
    SELECT ARRAY_AGG(table_name ORDER BY priority, table_name)
    INTO v_ordered_tables
    FROM export_csv.join_definition
    WHERE table_name = ANY(p_tables);
    
    -- Construire la clause JOIN
    FOREACH v_table IN ARRAY v_ordered_tables
    LOOP
        SELECT join_sql INTO v_join_clause_part
        FROM export_csv.join_definition
        WHERE table_name = v_table;
        
        v_join_clause := v_join_clause || E'\n' || v_join_clause_part;
    END LOOP;
    
    RETURN v_join_clause;
END;
$$;
```

### Fonction 3 : Transformation des valeurs (optionnelle, peut être en Java)
```sql
CREATE OR REPLACE FUNCTION export_csv.transform_value(
    p_raw_value ANYELEMENT,
    p_column_definition_id BIGINT
)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_regle_gestion TEXT;
    v_position INTEGER;
BEGIN
    -- Récupérer la règle de gestion
    SELECT regle_gestion 
    INTO v_regle_gestion
    FROM export_csv.csv_column_definition
    WHERE id = p_column_definition_id;
    
    -- Valeur NULL
    IF p_raw_value IS NULL THEN
        RETURN '';
    END IF;
    
    -- Appliquer la règle
    CASE v_regle_gestion
        WHEN 'POSITION_VALEUR_POSSIBLE' THEN
            -- Chercher la position de la valeur
            SELECT position 
            INTO v_position
            FROM export_csv.csv_column_values
            WHERE column_definition_id = p_column_definition_id
            AND LOWER(TRIM(valeur_possible)) = LOWER(TRIM(p_raw_value::TEXT));
            
            RETURN COALESCE(v_position::TEXT, '');
            
        WHEN 'TEXTE_DIRECT' THEN
            RETURN p_raw_value::TEXT;
            
        WHEN 'BOOLEAN_TO_NUMERIC' THEN
            RETURN CASE WHEN p_raw_value::BOOLEAN THEN '1' ELSE '0' END;
            
        WHEN 'FORMAT_DATE' THEN
            RETURN TO_CHAR(p_raw_value::DATE, 'DD/MM/YYYY');
            
        WHEN 'NUMERIQUE_DIRECT' THEN
            RETURN p_raw_value::TEXT;
            
        ELSE
            RETURN p_raw_value::TEXT;
    END CASE;
END;
$$;
```

## Architecture Spring Batch attendue

### Job 1 : Import du dictionnaire Excel

**Objectif** : Lire le fichier Excel et alimenter les tables métier PostgreSQL

**Composants** :
1. **ExcelItemReader** : Lit le fichier Excel (Apache POI)
2. **ExcelRowProcessor** : Transforme les lignes Excel en entités
3. **StagingItemWriter** : Écrit dans la table de staging
4. **TaskletNormalization** : Appelle la procédure `normalize_definitions()`

**Structure attendue** :
```java
@Configuration
public class ExcelImportJobConfig {
    
    @Bean
    public Job importExcelJob(JobRepository jobRepository, 
                             Step readExcelStep, 
                             Step normalizeStep) {
        return new JobBuilder("importExcelJob", jobRepository)
            .start(readExcelStep)
            .next(normalizeStep)
            .build();
    }
    
    @Bean
    public Step readExcelStep(/* params */) {
        return new StepBuilder("readExcelStep", jobRepository)
            .<ExcelRowDTO, ExcelDefinitionStaging>chunk(100, transactionManager)
            .reader(excelItemReader())
            .processor(excelRowProcessor())
            .writer(stagingItemWriter())
            .build();
    }
    
    @Bean
    public Step normalizeStep(/* params */) {
        return new StepBuilder("normalizeStep", jobRepository)
            .tasklet(normalizationTasklet(), transactionManager)
            .build();
    }
}
```

### Job 2 : Export CSV générique

**Objectif** : Générer les fichiers CSV à partir des requêtes dynamiques

**Composants** :
1. **DynamicQueryItemReader** : Exécute la requête générée par PostgreSQL
2. **CsvLineProcessor** : Transforme les données (optionnel)
3. **FlatFileItemWriter** : Écrit le CSV avec header dynamique

**Structure attendue** :
```java
@Configuration
public class CsvExportJobConfig {
    
    @Bean
    public Job exportCsvJob(JobRepository jobRepository, 
                           Step exportStep) {
        return new JobBuilder("exportCsvJob", jobRepository)
            .start(exportStep)
            .listener(exportJobListener())
            .build();
    }
    
    @Bean
    @StepScope
    public Step exportStep(@Value("#{jobParameters['nomCsv']}") String nomCsv,
                          JobRepository jobRepository,
                          PlatformTransactionManager transactionManager) {
        return new StepBuilder("exportStep_" + nomCsv, jobRepository)
            .<Map<String, Object>, String[]>chunk(1000, transactionManager)
            .reader(dynamicQueryItemReader(nomCsv))
            .processor(csvLineProcessor(nomCsv))
            .writer(csvFileItemWriter(nomCsv))
            .build();
    }
}
```

## Questions pour l'implémentation

### 1. Import Excel avec Spring Batch

**Q1.1** : Comment lire un fichier Excel multi-feuilles avec Apache POI dans un ItemReader ?
- Faut-il un Reader par feuille ou un seul Reader qui itère sur toutes les feuilles ?
- Comment gérer la première ligne (header) à ignorer ?

**Q1.2** : Structure du DTO pour transporter les données Excel ?
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

**Q1.3** : Comment mapper dynamiquement les colonnes valeur_possible_1..N vers une Map ?

### 2. Génération de requête dynamique

**Q2.1** : Comment récupérer la requête SQL générée par PostgreSQL et l'injecter dans un JdbcCursorItemReader ?
```java
@Bean
@StepScope
public JdbcCursorItemReader<Map<String, Object>> dynamicQueryItemReader(
    @Value("#{jobParameters['nomCsv']}") String nomCsv) {
    
    // Comment appeler build_export_query(nomCsv) et utiliser le résultat ?
}
```

**Q2.2** : Comment passer les paramètres `:dateDebut` et `:dateFin` à la requête générée ?

**Q2.3** : Comment gérer les résultats avec 500 colonnes dans un Map<String, Object> ?
- Y a-t-il des limites de performance ?
- Faut-il utiliser une projection DTO ou rester sur Map ?

### 3. Transformation des valeurs

**Q3.1** : Où faire la transformation POSITION_VALEUR_POSSIBLE ?
- Option A : En PostgreSQL (via fonction transform_value dans la requête)
- Option B : En Java (via ItemProcessor)
- Quelle option est la plus performante ?

**Q3.2** : Si transformation en Java, comment récupérer dynamiquement les valeurs possibles ?
```java
@Component
public class CsvLineProcessor implements ItemProcessor<Map<String, Object>, String[]> {
    
    // Comment charger les définitions et valeurs possibles une seule fois ?
    // Comment mapper les valeurs brutes vers les valeurs codifiées ?
}
```

### 4. Writer CSV avec header dynamique

**Q4.1** : Comment construire dynamiquement l'en-tête du CSV depuis les définitions en base ?
```java
@Bean
@StepScope
public FlatFileItemWriter<String[]> csvFileItemWriter(
    @Value("#{jobParameters['nomCsv']}") String nomCsv) {
    
    // Comment récupérer les code_variable ordonnés pour le header ?
    // Comment formatter le nom du fichier avec la date ?
}
```

**Q4.2** : Configuration optimale pour 500 colonnes ?
- Délimiteur : `;` ou `,` ?
- Encoding : UTF-8 ?
- Line separator : `\n` ou `\r\n` ?

### 5. Orchestration et planification

**Q5.1** : Comment déclencher séquentiellement les 3 exports (CSV_1, CSV_2, CSV_3) ?
```java
@Scheduled(cron = "0 0 2 ? * MON")
public void weeklyExport() {
    // Lancer exportCsvJob avec nomCsv = "CSV_1"
    // Puis nomCsv = "CSV_2"
    // Puis nomCsv = "CSV_3"
    // Comment gérer les JobParameters uniques ?
}
```

**Q5.2** : Stratégie de retry si un CSV échoue ?
- Relancer uniquement le CSV en échec ?
- Relancer tout le workflow ?

**Q5.3** : Comment logger les métriques (nb lignes, durée) dans la table export_execution ?

### 6. Initialisation du mapping technique

**Q6.1** : Comment initialiser les 100-200 lignes de mapping technique ?
- Script Flyway/Liquibase ?
- Job Spring Batch dédié ?
- Interface d'administration ?

**Q6.2** : Comment détecter automatiquement les colonnes sans mapping technique ?
```java
@Component
public class MappingValidator {
    // Vérifier que tous les code_variable ont un mapping
    // Logger les colonnes manquantes
}
```

### 7. Performance et optimisation

**Q7.1** : Taille de chunk recommandée pour 10 000 questionnaires avec 500 colonnes ?

**Q7.2** : Faut-il utiliser des index spécifiques sur les tables de métadonnées ?

**Q7.3** : Comment optimiser la requête générée pour éviter les N+1 queries ?
- Les LEFT JOIN suffisent-ils ?
- Faut-il utiliser des sous-requêtes LATERAL ?

**Q7.4** : Stratégie de cache pour les définitions de colonnes ?
- Les charger une fois au démarrage du Step ?
- Les recharger à chaque chunk ?

### 8. Tests

**Q8.1** : Comment tester unitairement la fonction `build_export_query` ?
```java
@Test
void testBuildExportQuery() {
    // Insérer des définitions de test
    // Appeler la fonction PostgreSQL
    // Vérifier que la requête générée est correcte
}
```

**Q8.2** : Comment tester l'import Excel avec un fichier de test ?

**Q8.3** : Comment tester un export complet end-to-end ?

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

## Contraintes de performance

- Export de 10 000 questionnaires : < 5 minutes par CSV
- Génération de requête SQL : < 1 seconde
- Import Excel : < 30 secondes
- Mémoire JVM : < 2 GB pendant l'export

Peux-tu me fournir une architecture complète, détaillée et prête à implémenter répondant à ce besoin ?