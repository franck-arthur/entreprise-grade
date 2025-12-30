# Guide de création du mapping technique
## Pour les développeurs backend

---

## 1. Vue d'ensemble

### Responsabilité
Les développeurs techniques doivent créer le **mapping entre les code_variable Excel et les sources de données PostgreSQL**.

### Objectif
Pour chaque `code_variable` défini dans l'Excel par les fonctionnels, créer une correspondance vers :
- Soit une colonne simple d'une table (`source_table` + `source_column`)
- Soit une expression SQL complexe (`sql_expression`)

### Timing
Ce mapping est créé **une seule fois** lors de l'initialisation du projet, puis maintenu au fil de l'eau lors de l'ajout de nouvelles colonnes.

---

## 2. Pré-requis

### 2.1 Récupérer la liste des code_variable depuis l'Excel
```sql
-- Après import de l'Excel, lister tous les code_variable
SELECT DISTINCT 
    cd.nom_csv,
    cd.ordre_colonne,
    cd.code_variable,
    cd.libelle,
    cd.type_question
FROM export_csv.csv_column_definition cd
WHERE cd.actif = true
ORDER BY cd.nom_csv, cd.ordre_colonne;
```

**Résultat attendu :**
```
 nom_csv | ordre_colonne | code_variable      | libelle                    | type_question
---------|---------------|--------------------|-----------------------------|---------------
 CSV_1   | 1             | Q_ID               | Identifiant                | NUMERIQUE
 CSV_1   | 2             | Q_DATE_CREATION    | Date de création           | DATE
 CSV_1   | 3             | Q_STATUT           | Statut                     | LISTE
 CSV_1   | 4             | Q_SEXE             | Sexe                       | LISTE
 CSV_1   | 5             | Q_NOM              | Nom                        | TEXTE
 CSV_1   | 6             | Q_PRENOM           | Prénom                     | TEXTE
 CSV_1   | 7             | Q_EMAIL            | Email                      | TEXTE
 CSV_1   | 8             | Q_DATE_NAISSANCE   | Date de naissance          | DATE
 CSV_1   | 9             | Q_VILLE            | Ville                      | TEXTE
 CSV_1   | 10            | Q_CODE_POSTAL      | Code postal                | TEXTE
 CSV_1   | 11            | Q_REGION           | Région                     | TEXTE
 CSV_1   | 12            | Q_NB_FORMATIONS    | Nombre de formations       | CALC
 CSV_1   | 13            | Q_AGE              | Âge                        | CALC
 CSV_1   | 14            | Q_SECTEUR_PRINCIPAL| Secteur principal          | CALC
 ...
```

### 2.2 Analyser le modèle de données
```sql
-- Lister les tables disponibles
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Analyser une table spécifique
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'questionnaires'
ORDER BY ordinal_position;

-- Identifier les relations (foreign keys)
SELECT
    tc.table_name AS source_table,
    kcu.column_name AS source_column,
    ccu.table_name AS target_table,
    ccu.column_name AS target_column
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
ORDER BY tc.table_name;
```

---

## 3. Création du mapping : Étape par étape

### 3.1 Initialiser les définitions de jointures
```sql
-- Script: sql/01_init_join_definitions.sql

-- Insérer les définitions de jointures entre tables
-- ⚠️ ADAPTER selon votre modèle de données

INSERT INTO export_csv.join_definition (table_name, join_sql, depends_on, priority) VALUES
-- Jointures de niveau 1 (directement depuis questionnaires)
('personne', 
 'LEFT JOIN personne p ON p.questionnaire_id = q.id', 
 ARRAY[]::TEXT[], 
 1),

('reponse', 
 'LEFT JOIN reponse r ON r.questionnaire_id = q.id', 
 ARRAY[]::TEXT[], 
 1),

('formation_participation', 
 'LEFT JOIN formation_participation fp ON fp.questionnaire_id = q.id', 
 ARRAY[]::TEXT[], 
 1),

-- Jointures de niveau 2 (dépendent d'autres tables)
('adresse', 
 'LEFT JOIN adresse a ON a.personne_id = p.id', 
 ARRAY['personne'], 
 2),

('formation', 
 'LEFT JOIN formation f ON f.id = fp.formation_id', 
 ARRAY['formation_participation'], 
 2),

('entreprise',
 'LEFT JOIN entreprise e ON e.id = p.entreprise_id',
 ARRAY['personne'],
 2),

-- Jointures de niveau 3 (dépendent de niveau 2)
('formation_session',
 'LEFT JOIN formation_session fs ON fs.formation_id = f.id',
 ARRAY['formation_participation', 'formation'],
 3)

ON CONFLICT (table_name) DO UPDATE SET
    join_sql = EXCLUDED.join_sql,
    depends_on = EXCLUDED.depends_on,
    priority = EXCLUDED.priority;
```

**📝 Notes :**
- `table_name` : Nom de la table à joindre
- `join_sql` : Clause JOIN complète avec alias
- `depends_on` : Tables qui doivent être jointes AVANT celle-ci
- `priority` : Ordre d'exécution des jointures (1 = premier)

---

### 3.2 Créer le mapping pour les colonnes simples

#### Stratégie de mapping automatique par convention
```sql
-- Script: sql/02_auto_mapping_simple_columns.sql

-- Fonction helper pour détecter automatiquement les colonnes simples
CREATE OR REPLACE FUNCTION export_csv.auto_map_simple_columns()
RETURNS TABLE (
    code_variable TEXT,
    detected_table TEXT,
    detected_column TEXT,
    confidence TEXT
) AS $$
BEGIN
    RETURN QUERY
    
    -- Règle 1 : Q_{COLONNE} → questionnaires.colonne
    -- Exemple : Q_ID → questionnaires.id
    SELECT 
        cd.code_variable,
        'questionnaires'::TEXT as detected_table,
        LOWER(REPLACE(cd.code_variable, 'Q_', '')) as detected_column,
        CASE 
            WHEN EXISTS (
                SELECT 1 FROM information_schema.columns c
                WHERE c.table_name = 'questionnaires'
                AND c.column_name = LOWER(REPLACE(cd.code_variable, 'Q_', ''))
            ) THEN 'HIGH'
            ELSE 'LOW'
        END as confidence
    FROM export_csv.csv_column_definition cd
    WHERE cd.code_variable ~ '^Q_[A-Z_]+$'  -- Pattern Q_XXX
    AND cd.type_question IN ('NUMERIQUE', 'TEXTE', 'DATE', 'LISTE', 'BOOLEEN')
    AND NOT EXISTS (
        SELECT 1 FROM export_csv.column_sql_mapping sm 
        WHERE sm.code_variable = cd.code_variable
    )
    
    UNION ALL
    
    -- Règle 2 : Q_P_{COLONNE} → personne.colonne (pattern avec préfixe)
    -- Exemple : Q_P_NOM → personne.nom
    SELECT 
        cd.code_variable,
        'personne'::TEXT as detected_table,
        LOWER(REPLACE(cd.code_variable, 'Q_P_', '')) as detected_column,
        CASE 
            WHEN EXISTS (
                SELECT 1 FROM information_schema.columns c
                WHERE c.table_name = 'personne'
                AND c.column_name = LOWER(REPLACE(cd.code_variable, 'Q_P_', ''))
            ) THEN 'HIGH'
            ELSE 'LOW'
        END as confidence
    FROM export_csv.csv_column_definition cd
    WHERE cd.code_variable ~ '^Q_P_[A-Z_]+$'
    AND NOT EXISTS (
        SELECT 1 FROM export_csv.column_sql_mapping sm 
        WHERE sm.code_variable = cd.code_variable
    )
    
    UNION ALL
    
    -- Règle 3 : Recherche par correspondance de nom (fuzzy matching)
    -- Exemple : Q_SEXE pourrait correspondre à personne.sexe
    SELECT 
        cd.code_variable,
        c.table_name::TEXT as detected_table,
        c.column_name::TEXT as detected_column,
        'MEDIUM' as confidence
    FROM export_csv.csv_column_definition cd
    CROSS JOIN information_schema.columns c
    WHERE c.table_schema = 'public'
    AND (
        -- Correspondance exacte sur le nom
        LOWER(REPLACE(cd.code_variable, 'Q_', '')) = c.column_name
        OR 
        -- Correspondance sur le libellé
        LOWER(cd.libelle) LIKE '%' || c.column_name || '%'
    )
    AND cd.type_question IN ('NUMERIQUE', 'TEXTE', 'DATE', 'LISTE', 'BOOLEEN')
    AND NOT EXISTS (
        SELECT 1 FROM export_csv.column_sql_mapping sm 
        WHERE sm.code_variable = cd.code_variable
    );
    
END;
$$ LANGUAGE plpgsql;

-- Afficher les suggestions de mapping automatique
SELECT * FROM export_csv.auto_map_simple_columns()
WHERE confidence IN ('HIGH', 'MEDIUM')
ORDER BY confidence DESC, code_variable;
```

#### Insertion manuelle du mapping pour colonnes simples
```sql
-- Script: sql/03_mapping_simple_columns.sql

-- ========================================
-- COLONNES DE LA TABLE QUESTIONNAIRES
-- ========================================

INSERT INTO export_csv.column_sql_mapping 
(code_variable, source_table, source_column, join_tables, description) VALUES

-- Colonnes directes de questionnaires (pas de jointure nécessaire)
('Q_ID', 'questionnaires', 'id', ARRAY[]::TEXT[], 'Identifiant unique du questionnaire'),
('Q_DATE_CREATION', 'questionnaires', 'date_creation', ARRAY[]::TEXT[], 'Date de création du questionnaire'),
('Q_DATE_MODIFICATION', 'questionnaires', 'date_modification', ARRAY[]::TEXT[], 'Date de dernière modification'),
('Q_STATUT', 'questionnaires', 'statut', ARRAY[]::TEXT[], 'Statut du questionnaire (BROUILLON, VALIDE, ARCHIVE)'),
('Q_TYPE', 'questionnaires', 'type', ARRAY[]::TEXT[], 'Type de questionnaire'),
('Q_ANNEE', 'questionnaires', 'annee', ARRAY[]::TEXT[], 'Année du questionnaire')

ON CONFLICT (code_variable) DO UPDATE SET
    source_table = EXCLUDED.source_table,
    source_column = EXCLUDED.source_column,
    join_tables = EXCLUDED.join_tables,
    description = EXCLUDED.description,
    date_modification = NOW();

-- ========================================
-- COLONNES DE LA TABLE PERSONNE
-- ========================================

INSERT INTO export_csv.column_sql_mapping 
(code_variable, source_table, source_column, join_tables, description) VALUES

-- Colonnes de personne (nécessite 1 jointure)
('Q_SEXE', 'personne', 'sexe', ARRAY['personne'], 'Sexe du participant'),
('Q_NOM', 'personne', 'nom', ARRAY['personne'], 'Nom du participant'),
('Q_PRENOM', 'personne', 'prenom', ARRAY['personne'], 'Prénom du participant'),
('Q_EMAIL', 'personne', 'email', ARRAY['personne'], 'Adresse email du participant'),
('Q_TELEPHONE', 'personne', 'telephone', ARRAY['personne'], 'Numéro de téléphone'),
('Q_DATE_NAISSANCE', 'personne', 'date_naissance', ARRAY['personne'], 'Date de naissance'),
('Q_NATIONALITE', 'personne', 'nationalite', ARRAY['personne'], 'Nationalité'),
('Q_SITUATION_FAMILIALE', 'personne', 'situation_familiale', ARRAY['personne'], 'Situation familiale')

ON CONFLICT (code_variable) DO UPDATE SET
    source_table = EXCLUDED.source_table,
    source_column = EXCLUDED.source_column,
    join_tables = EXCLUDED.join_tables,
    description = EXCLUDED.description,
    date_modification = NOW();

-- ========================================
-- COLONNES DE LA TABLE ADRESSE
-- ========================================

INSERT INTO export_csv.column_sql_mapping 
(code_variable, source_table, source_column, join_tables, description) VALUES

-- Colonnes d'adresse (nécessite 2 jointures : personne puis adresse)
('Q_VILLE', 'adresse', 'ville', ARRAY['personne', 'adresse'], 'Ville de résidence'),
('Q_CODE_POSTAL', 'adresse', 'code_postal', ARRAY['personne', 'adresse'], 'Code postal'),
('Q_REGION', 'adresse', 'region', ARRAY['personne', 'adresse'], 'Région de résidence'),
('Q_PAYS', 'adresse', 'pays', ARRAY['personne', 'adresse'], 'Pays de résidence'),
('Q_RUE', 'adresse', 'rue', ARRAY['personne', 'adresse'], 'Rue')

ON CONFLICT (code_variable) DO UPDATE SET
    source_table = EXCLUDED.source_table,
    source_column = EXCLUDED.source_column,
    join_tables = EXCLUDED.join_tables,
    description = EXCLUDED.description,
    date_modification = NOW();

-- ========================================
-- COLONNES DE LA TABLE ENTREPRISE
-- ========================================

INSERT INTO export_csv.column_sql_mapping 
(code_variable, source_table, source_column, join_tables, description) VALUES

('Q_ENTREPRISE_NOM', 'entreprise', 'nom', ARRAY['personne', 'entreprise'], 'Nom de l\'entreprise'),
('Q_ENTREPRISE_SECTEUR', 'entreprise', 'secteur', ARRAY['personne', 'entreprise'], 'Secteur d\'activité'),
('Q_ENTREPRISE_TAILLE', 'entreprise', 'taille', ARRAY['personne', 'entreprise'], 'Taille de l\'entreprise')

ON CONFLICT (code_variable) DO UPDATE SET
    source_table = EXCLUDED.source_table,
    source_column = EXCLUDED.source_column,
    join_tables = EXCLUDED.join_tables,
    description = EXCLUDED.description,
    date_modification = NOW();
```

---

### 3.3 Créer le mapping pour les colonnes calculées (sous-requêtes)
```sql
-- Script: sql/04_mapping_calculated_columns.sql

-- ========================================
-- COLONNES CALCULÉES SIMPLES
-- ========================================

INSERT INTO export_csv.column_sql_mapping 
(code_variable, sql_expression, requires_subquery, is_aggregation, description) VALUES

-- Calcul d'âge (nécessite la jointure vers personne, mais pas de sous-requête)
('Q_AGE', 
 'EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance))',
 FALSE,
 FALSE,
 'Âge calculé à partir de la date de naissance'),

-- Nombre de jours depuis la création
('Q_JOURS_DEPUIS_CREATION',
 'EXTRACT(DAY FROM CURRENT_DATE - q.date_creation)',
 FALSE,
 FALSE,
 'Nombre de jours depuis la création du questionnaire')

ON CONFLICT (code_variable) DO UPDATE SET
    sql_expression = EXCLUDED.sql_expression,
    requires_subquery = EXCLUDED.requires_subquery,
    is_aggregation = EXCLUDED.is_aggregation,
    description = EXCLUDED.description,
    date_modification = NOW();

-- ========================================
-- COLONNES AVEC SOUS-REQUÊTES (AGRÉGATIONS)
-- ========================================

INSERT INTO export_csv.column_sql_mapping 
(code_variable, sql_expression, requires_subquery, is_aggregation, description) VALUES

-- Nombre de formations suivies
('Q_NB_FORMATIONS',
 '(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id)',
 TRUE,
 TRUE,
 'Nombre total de formations suivies'),

-- Nombre de formations validées
('Q_NB_FORMATIONS_VALIDEES',
 '(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id AND fp.statut = ''VALIDE'')',
 TRUE,
 TRUE,
 'Nombre de formations avec statut validé'),

-- Taux de présence moyen
('Q_TAUX_PRESENCE_MOYEN',
 '(SELECT ROUND(AVG(CASE WHEN fp.presence = TRUE THEN 100 ELSE 0 END), 2) FROM formation_participation fp WHERE fp.questionnaire_id = q.id)',
 TRUE,
 TRUE,
 'Taux de présence moyen en pourcentage'),

-- Secteur de formation le plus fréquent
('Q_SECTEUR_PRINCIPAL',
 $sql$(SELECT f.secteur 
       FROM formation_participation fp 
       JOIN formation f ON f.id = fp.formation_id 
       WHERE fp.questionnaire_id = q.id 
       GROUP BY f.secteur 
       ORDER BY COUNT(*) DESC 
       LIMIT 1)$sql$,
 TRUE,
 TRUE,
 'Secteur de formation le plus fréquent'),

-- Dernière formation suivie
('Q_DERNIERE_FORMATION',
 $sql$(SELECT f.libelle 
       FROM formation_participation fp 
       JOIN formation f ON f.id = fp.formation_id 
       WHERE fp.questionnaire_id = q.id 
       ORDER BY f.date_formation DESC 
       LIMIT 1)$sql$,
 TRUE,
 FALSE,
 'Libellé de la dernière formation suivie'),

-- Nombre de réponses fournies
('Q_NB_REPONSES',
 '(SELECT COUNT(*) FROM reponse r WHERE r.questionnaire_id = q.id AND r.valeur IS NOT NULL)',
 TRUE,
 TRUE,
 'Nombre de réponses fournies (non NULL)'),

-- Liste des modalités suivies (concaténation)
('Q_MODALITES',
 $sql$(SELECT STRING_AGG(DISTINCT f.modalite, ', ' ORDER BY f.modalite)
       FROM formation_participation fp
       JOIN formation f ON f.id = fp.formation_id
       WHERE fp.questionnaire_id = q.id)$sql$,
 TRUE,
 TRUE,
 'Liste des modalités de formation (en présentiel, distanciel, etc.)'),

-- Durée totale de formation en heures
('Q_DUREE_TOTALE_HEURES',
 $sql$(SELECT SUM(EXTRACT(EPOCH FROM (f.heure_fin - f.heure_debut)) / 3600)
       FROM formation_participation fp
       JOIN formation f ON f.id = fp.formation_id
       WHERE fp.questionnaire_id = q.id)$sql$,
 TRUE,
 TRUE,
 'Durée totale de formation en heures')

ON CONFLICT (code_variable) DO UPDATE SET
    sql_expression = EXCLUDED.sql_expression,
    requires_subquery = EXCLUDED.requires_subquery,
    is_aggregation = EXCLUDED.is_aggregation,
    description = EXCLUDED.description,
    date_modification = NOW();
```

---

### 3.4 Créer le mapping pour les colonnes de type réponse (EAV)

Si vous avez un modèle Entity-Attribute-Value pour les réponses :
```sql
-- Script: sql/05_mapping_reponse_columns.sql

-- ========================================
-- COLONNES DEPUIS LA TABLE RÉPONSE
-- ========================================

-- Si modèle EAV : 1 ligne par question avec code_question + valeur
INSERT INTO export_csv.column_sql_mapping 
(code_variable, sql_expression, requires_subquery, description) VALUES

-- Question satisfaction formateur
('Q_SATISFACTION_FORMATEUR',
 $sql$(SELECT r.valeur 
       FROM reponse r 
       WHERE r.questionnaire_id = q.id 
       AND r.code_question = 'SATISFACTION_FORMATEUR')$sql$,
 TRUE,
 'Satisfaction vis-à-vis du formateur'),

-- Question satisfaction contenu
('Q_SATISFACTION_CONTENU',
 $sql$(SELECT r.valeur 
       FROM reponse r 
       WHERE r.questionnaire_id = q.id 
       AND r.code_question = 'SATISFACTION_CONTENU')$sql$,
 TRUE,
 'Satisfaction vis-à-vis du contenu'),

-- Commentaire libre
('Q_COMMENTAIRE',
 $sql$(SELECT r.valeur 
       FROM reponse r 
       WHERE r.questionnaire_id = q.id 
       AND r.code_question = 'COMMENTAIRE_LIBRE')$sql$,
 TRUE,
 'Commentaire libre du participant')

ON CONFLICT (code_variable) DO UPDATE SET
    sql_expression = EXCLUDED.sql_expression,
    requires_subquery = EXCLUDED.requires_subquery,
    description = EXCLUDED.description,
    date_modification = NOW();
```

---

## 4. Scripts de validation

### 4.1 Vérifier les colonnes sans mapping
```sql
-- Script: sql/90_validate_mapping.sql

-- Lister toutes les colonnes sans mapping technique
SELECT 
    cd.nom_csv,
    cd.ordre_colonne,
    cd.code_variable,
    cd.libelle,
    cd.type_question,
    '⚠️ MAPPING MANQUANT' as statut
FROM export_csv.csv_column_definition cd
LEFT JOIN export_csv.column_sql_mapping sm ON sm.code_variable = cd.code_variable
WHERE cd.actif = true
AND sm.id IS NULL
ORDER BY cd.nom_csv, cd.ordre_colonne;

-- Compter les colonnes mappées vs non mappées par CSV
SELECT 
    cd.nom_csv,
    COUNT(*) as total_colonnes,
    COUNT(sm.id) as colonnes_mappees,
    COUNT(*) - COUNT(sm.id) as colonnes_non_mappees,
    ROUND(100.0 * COUNT(sm.id) / COUNT(*), 2) || '%' as taux_completion
FROM export_csv.csv_column_definition cd
LEFT JOIN export_csv.column_sql_mapping sm ON sm.code_variable = cd.code_variable
WHERE cd.actif = true
GROUP BY cd.nom_csv
ORDER BY cd.nom_csv;
```

### 4.2 Tester la génération de requête
```sql
-- Tester la génération de requête pour CSV_1
SELECT export_csv.build_export_query('CSV_1');

-- Tester l'exécution de la requête générée (LIMIT 1 pour tester)
DO $$
DECLARE
    v_query TEXT;
BEGIN
    v_query := export_csv.build_export_query('CSV_1');
    
    -- Remplacer les paramètres par des valeurs de test
    v_query := REPLACE(v_query, ':dateDebut', '''2024-01-01''');
    v_query := REPLACE(v_query, ':dateFin', '''2024-12-31''');
    
    -- Ajouter LIMIT pour test
    v_query := v_query || ' LIMIT 1';
    
    RAISE NOTICE 'Requête générée : %', v_query;
    
    -- Exécuter pour tester
    EXECUTE 'EXPLAIN ANALYZE ' || v_query;
END $$;
```

### 4.3 Vérifier la validité des expressions SQL
```sql
-- Tester chaque expression SQL individuellement
DO $$
DECLARE
    r RECORD;
    v_test_query TEXT;
BEGIN
    FOR r IN 
        SELECT code_variable, sql_expression
        FROM export_csv.column_sql_mapping
        WHERE sql_expression IS NOT NULL
    LOOP
        BEGIN
            v_test_query := FORMAT(
                'SELECT %s AS test_value FROM questionnaires q LIMIT 1',
                r.sql_expression
            );
            
            EXECUTE v_test_query;
            RAISE NOTICE '✅ % : OK', r.code_variable;
            
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING '❌ % : ERREUR - %', r.code_variable, SQLERRM;
        END;
    END LOOP;
END $$;
```

---

## 5. Script d'initialisation complet
```sql
-- Script: sql/99_init_all_mappings.sql

-- Ordre d'exécution des scripts
\echo '========================================='
\echo 'Initialisation du mapping technique'
\echo '========================================='

\echo ''
\echo '1. Définitions des jointures...'
\i sql/01_init_join_definitions.sql

\echo ''
\echo '2. Auto-détection des colonnes simples...'
\i sql/02_auto_mapping_simple_columns.sql

\echo ''
\echo '3. Mapping des colonnes simples...'
\i sql/03_mapping_simple_columns.sql

\echo ''
\echo '4. Mapping des colonnes calculées...'
\i sql/04_mapping_calculated_columns.sql

\echo ''
\echo '5. Mapping des colonnes de réponse...'
\i sql/05_mapping_reponse_columns.sql

\echo ''
\echo '6. Validation du mapping...'
\i sql/90_validate_mapping.sql

\echo ''
\echo '========================================='
\echo 'Initialisation terminée'
\echo '========================================='

-- Résumé final
SELECT 
    '✅ TOTAL' as statut,
    COUNT(*) as nb_colonnes,
    COUNT(CASE WHEN sm.id IS NOT NULL THEN 1 END) as nb_mappees,
    COUNT(CASE WHEN sm.id IS NULL THEN 1 END) as nb_non_mappees
FROM export_csv.csv_column_definition cd
LEFT JOIN export_csv.column_sql_mapping sm ON sm.code_variable = cd.code_variable
WHERE cd.actif = true;
```

---

## 6. Maintenance : Ajout d'une nouvelle colonne

### Workflow

1. **Fonctionnel** ajoute une ligne dans l'Excel
2. **Fonctionnel** réimporte l'Excel via l'interface
3. **Dev** reçoit une alerte "Colonne sans mapping"
4. **Dev** ajoute le mapping SQL

### Script d'ajout manuel
```sql
-- Template pour ajouter un nouveau mapping
-- ADAPTER selon votre besoin

-- Cas 1 : Colonne simple
INSERT INTO export_csv.column_sql_mapping 
(code_variable, source_table, source_column, join_tables, description) 
VALUES 
('Q_NOUVEAU_CHAMP', 'nom_table', 'nom_colonne', ARRAY['table_si_jointure'], 'Description');

-- Cas 2 : Colonne calculée
INSERT INTO export_csv.column_sql_mapping 
(code_variable, sql_expression, requires_subquery, is_aggregation, description) 
VALUES 
('Q_CALCUL', '(SELECT ... FROM ... WHERE ...)', TRUE, TRUE, 'Description');

-- Valider
SELECT export_csv.build_export_query('CSV_1');
```

---

## 7. Outils utiles

### 7.1 Générateur de mapping interactif (SQL)
```sql
-- Fonction helper pour générer le code INSERT
CREATE OR REPLACE FUNCTION export_csv.generate_mapping_insert(
    p_code_variable TEXT,
    p_source_table TEXT DEFAULT NULL,
    p_source_column TEXT DEFAULT NULL,
    p_sql_expression TEXT DEFAULT NULL
)
RETURNS TEXT AS $$
DECLARE
    v_insert TEXT;
    v_join_tables TEXT[];
BEGIN
    -- Détecter les jointures nécessaires
    IF p_source_table IS NOT NULL AND p_source_table != 'questionnaires' THEN
        v_join_tables := ARRAY[p_source_table];
        
        -- Ajouter les dépendances
        IF p_source_table = 'adresse' THEN
            v_join_tables := ARRAY['personne', 'adresse'];
        ELSIF p_source_table = 'formation' THEN
            v_join_tables := ARRAY['formation_participation', 'formation'];
        END IF;
    END IF;
    
    -- Générer l'INSERT
    IF p_sql_expression IS NOT NULL THEN
        v_insert := FORMAT(
            $sql$INSERT INTO export_csv.column_sql_mapping 
(code_variable, sql_expression, requires_subquery, description) 
VALUES 
('%s', '%s', TRUE, 'TODO: Ajouter description');$sql$,
            p_code_variable,
            p_sql_expression
        );
    ELSE
        v_insert := FORMAT(
            $sql$INSERT INTO export_csv.column_sql_mapping 
(code_variable, source_table, source_column, join_tables, description) 
VALUES 
('%s', '%s', '%s', ARRAY%s, 'TODO: Ajouter description');$sql$,
            p_code_variable,
            p_source_table,
            p_source_column,
            v_join_tables::TEXT
        );
    END IF;
    
    RETURN v_insert;
END;
$$ LANGUAGE plpgsql;

-- Utilisation
SELECT export_csv.generate_mapping_insert('Q_VILLE', 'adresse', 'ville');
SELECT export_csv.generate_mapping_insert('Q_NB_FORMATIONS', NULL, NULL, 
    '(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id)');
```

### 7.2 Export du mapping vers fichier
```sql
-- Générer un script SQL de toutes les définitions actuelles
\copy (
    SELECT 
        FORMAT(
            'INSERT INTO export_csv.column_sql_mapping (code_variable, source_table, source_column, join_tables, description) VALUES (%L, %L, %L, %L, %L);',
            code_variable,
            source_table,
            source_column,
            join_tables::TEXT,
            description
        ) as insert_statement
    FROM export_csv.column_sql_mapping
    WHERE source_table IS NOT NULL
    
    UNION ALL
    
    SELECT 
        FORMAT(
            'INSERT INTO export_csv.column_sql_mapping (code_variable, sql_expression, requires_subquery, is_aggregation, description) VALUES (%L, %L, %s, %s, %L);',
            code_variable,
            sql_expression,
            requires_subquery,
            is_aggregation,
            description
        )
    FROM export_csv.column_sql_mapping
    WHERE sql_expression IS NOT NULL
) TO '/tmp/backup_mapping.sql';
```

---

## 8. Checklist finale

Avant de considérer le mapping comme complet :

- [ ] Toutes les définitions de jointures sont créées
- [ ] Toutes les colonnes ont un mapping (0 ligne dans la requête de validation)
- [ ] La génération de requête fonctionne pour les 3 CSV
- [ ] Les expressions SQL sont valides (test unitaire passé)
- [ ] La documentation est à jour
- [ ] Un backup du mapping a été créé
- [ ] L'équipe fonctionnelle a validé un export de test

---

## 9. Exemple complet pour un CSV de 50 colonnes
```sql
-- Exemple réaliste : CSV_1 avec 50 colonnes

-- Étape 1 : Jointures
-- (déjà fait dans 01_init_join_definitions.sql)

-- Étape 2 : Mapping des 50 colonnes
INSERT INTO export_csv.column_sql_mapping (code_variable, source_table, source_column, join_tables, description) VALUES
-- Questionnaires (10 colonnes)
('Q_ID', 'questionnaires', 'id', ARRAY[]::TEXT[], 'ID'),
('Q_DATE_CREATION', 'questionnaires', 'date_creation', ARRAY[]::TEXT[], 'Date création'),
-- ... 8 autres colonnes de questionnaires

-- Personne (15 colonnes)
('Q_SEXE', 'personne', 'sexe', ARRAY['personne'], 'Sexe'),
('Q_NOM', 'personne', 'nom', ARRAY['personne'], 'Nom'),
-- ... 13 autres colonnes de personne

-- Adresse (10 colonnes)
('Q_VILLE', 'adresse', 'ville', ARRAY['personne', 'adresse'], 'Ville'),
-- ... 9 autres colonnes d'adresse

ON CONFLICT (code_variable) DO NOTHING;

-- Colonnes calculées (15 colonnes)
INSERT INTO export_csv.column_sql_mapping (code_variable, sql_expression, requires_subquery, is_aggregation, description) VALUES
('Q_AGE', 'EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance))', FALSE, FALSE, 'Âge'),
('Q_NB_FORMATIONS', '(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id)', TRUE, TRUE, 'Nb formations'),
-- ... 13 autres colonnes calculées

ON CONFLICT (code_variable) DO NOTHING;

-- Validation
SELECT code_variable FROM export_csv.csv_column_definition 
WHERE nom_csv = 'CSV_1' AND actif = true
AND code_variable NOT IN (SELECT code_variable FROM export_csv.column_sql_mapping);
-- Résultat attendu : 0 lignes
```

---

Cette procédure fournit un guide complet pour créer le mapping technique. Les développeurs peuvent l'adapter selon leur modèle de données spécifique.