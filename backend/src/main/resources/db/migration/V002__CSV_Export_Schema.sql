-- Migration V002 : Schéma pour le système d'export CSV
-- Création du schéma dédié et des tables pour la gestion des exports CSV

-- ====================================
-- 1. CRÉATION DU SCHÉMA
-- ====================================
CREATE SCHEMA IF NOT EXISTS export_csv;

-- ====================================
-- 2. TABLE DE STAGING (Import Excel)
-- ====================================
CREATE TABLE export_csv.excel_csv_definition_stg (
    id BIGSERIAL PRIMARY KEY,
    nom_csv TEXT NOT NULL,
    excel_row_number INTEGER NOT NULL,  -- Ligne Excel (2, 3, 4...) pour préserver l'ordre
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

-- ====================================
-- 3. TABLE DES DÉFINITIONS MÉTIER
-- ====================================
CREATE TABLE export_csv.csv_column_definition (
    id BIGSERIAL PRIMARY KEY,
    nom_csv TEXT NOT NULL,
    ordre_colonne INTEGER NOT NULL,  -- Calculé à partir de excel_row_number
    code_variable TEXT NOT NULL,
    libelle TEXT,
    format TEXT,
    type_question TEXT,
    regle_gestion TEXT,
    actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT NOW(),
    date_modification TIMESTAMP DEFAULT NOW(),

    CONSTRAINT uk_csv_column_nom_code UNIQUE(nom_csv, code_variable),
    CONSTRAINT uk_csv_column_nom_ordre UNIQUE(nom_csv, ordre_colonne)  -- Garantir l'unicité de l'ordre
);

-- ====================================
-- 4. TABLE DES VALEURS POSSIBLES
-- ====================================
CREATE TABLE export_csv.csv_column_values (
    id BIGSERIAL PRIMARY KEY,
    column_definition_id BIGINT NOT NULL,
    position INTEGER NOT NULL,
    valeur_possible TEXT NOT NULL,
    date_creation TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_csv_values_column_def
        FOREIGN KEY (column_definition_id)
        REFERENCES export_csv.csv_column_definition(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_csv_values_col_pos
        UNIQUE(column_definition_id, position)
);

-- ====================================
-- 5. TABLE DE MAPPING TECHNIQUE SQL
-- ====================================
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

-- ====================================
-- 6. TABLE DES DÉFINITIONS DE JOINTURES
-- ====================================
CREATE TABLE export_csv.join_definition (
    table_name TEXT PRIMARY KEY,
    join_sql TEXT NOT NULL,
    depends_on TEXT[],
    priority INTEGER NOT NULL
);

-- ====================================
-- 7. TABLE DE TRACKING DES EXPORTS
-- ====================================
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

-- ====================================
-- 8. INDEX POUR PERFORMANCE
-- ====================================
CREATE INDEX idx_csv_def_nom_ordre ON export_csv.csv_column_definition(nom_csv, ordre_colonne);
CREATE INDEX idx_csv_values_col_pos ON export_csv.csv_column_values(column_definition_id, position);
CREATE INDEX idx_column_mapping_code ON export_csv.column_sql_mapping(code_variable);
CREATE INDEX idx_export_exec_date ON export_csv.export_execution(date_debut DESC);
CREATE INDEX idx_export_exec_csv_date ON export_csv.export_execution(nom_csv, date_debut DESC);

-- ====================================
-- 9. VUE COMBINÉE MÉTIER + TECHNIQUE
-- ====================================
CREATE OR REPLACE VIEW export_csv.v_complete_column_definition AS
SELECT
    cd.id,
    cd.nom_csv,
    cd.ordre_colonne,  -- Ordre préservé depuis Excel
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
    sm.requires_subquery, sm.is_aggregation
ORDER BY cd.nom_csv, cd.ordre_colonne;

-- ====================================
-- 10. DONNÉES D'EXEMPLE POUR JOINTURES
-- ====================================
INSERT INTO export_csv.join_definition (table_name, join_sql, depends_on, priority) VALUES
('personne', 'LEFT JOIN personne p ON p.questionnaire_id = q.id', ARRAY[]::TEXT[], 1),
('adresse', 'LEFT JOIN adresse a ON a.personne_id = p.id', ARRAY['personne'], 2),
('formation_participation', 'LEFT JOIN formation_participation fp ON fp.questionnaire_id = q.id', ARRAY[]::TEXT[], 1),
('formation', 'LEFT JOIN formation f ON f.id = fp.formation_id', ARRAY['formation_participation'], 2),
('secteur', 'LEFT JOIN secteur s ON s.id = f.secteur_id', ARRAY['formation'], 3),
('region', 'LEFT JOIN region r ON r.id = f.region_id', ARRAY['formation'], 3);

-- ====================================
-- 11. PROCÉDURE DE NORMALISATION
-- ====================================
CREATE OR REPLACE PROCEDURE export_csv.normalize_definitions()
LANGUAGE plpgsql
AS $$
DECLARE
    rec RECORD;
BEGIN
    -- Désactiver les anciennes définitions pour les CSV présents dans le staging
    UPDATE export_csv.csv_column_definition
    SET actif = false
    WHERE nom_csv IN (SELECT DISTINCT nom_csv FROM export_csv.excel_csv_definition_stg);

    -- Insérer/mettre à jour les définitions de colonnes
    -- ordre_colonne est calculé via ROW_NUMBER() sur excel_row_number
    INSERT INTO export_csv.csv_column_definition
    (nom_csv, ordre_colonne, code_variable, libelle, format, type_question, regle_gestion, actif)
    SELECT
        nom_csv,
        ROW_NUMBER() OVER (PARTITION BY nom_csv ORDER BY excel_row_number) as ordre_colonne,  -- Préserve l'ordre Excel
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
        ORDER BY cd.nom_csv, cd.ordre_colonne  -- Ordre préservé
    LOOP
        RAISE WARNING '  - [%] % : %', rec.ordre_colonne, rec.code_variable, rec.libelle;
    END LOOP;

    RAISE NOTICE 'Normalisation terminée';
END;
$$;

-- ====================================
-- 12. FONCTION DE GÉNÉRATION DE REQUÊTE DYNAMIQUE
-- ====================================
CREATE OR REPLACE FUNCTION export_csv.build_export_query(p_nom_csv TEXT)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_query TEXT := '';
    v_select_clause TEXT := '';
    v_from_clause TEXT := 'FROM questionnaire q';
    v_join_clause TEXT := '';
    v_needed_tables TEXT[] := ARRAY[]::TEXT[];
    v_processed_tables TEXT[] := ARRAY[]::TEXT[];
    col_rec RECORD;
    join_rec RECORD;
    table_name TEXT;
BEGIN
    -- Construire la clause SELECT et identifier les tables nécessaires
    v_select_clause := 'SELECT q.id as questionnaire_id';

    FOR col_rec IN
        SELECT ordre_colonne, code_variable, source_table, source_column, sql_expression, join_tables
        FROM export_csv.v_complete_column_definition
        WHERE nom_csv = p_nom_csv
        ORDER BY ordre_colonne
    LOOP
        v_select_clause := v_select_clause || ',\n       ';

        IF col_rec.sql_expression IS NOT NULL THEN
            -- Expression SQL complexe (sous-requête/calcul)
            v_select_clause := v_select_clause || '(' || col_rec.sql_expression || ') AS ' || col_rec.code_variable;
        ELSE
            -- Colonne simple
            IF col_rec.source_table IS NOT NULL AND col_rec.source_column IS NOT NULL THEN
                v_select_clause := v_select_clause ||
                    CASE col_rec.source_table
                        WHEN 'questionnaire' THEN 'q'
                        ELSE SUBSTRING(col_rec.source_table, 1, 1)
                    END || '.' || col_rec.source_column || ' AS ' || col_rec.code_variable;

                -- Ajouter la table aux tables nécessaires si ce n'est pas questionnaire
                IF col_rec.source_table != 'questionnaire' THEN
                    v_needed_tables := v_needed_tables || col_rec.source_table;
                END IF;
            ELSE
                -- Colonne sans mapping -> NULL
                v_select_clause := v_select_clause || 'NULL AS ' || col_rec.code_variable;
            END IF;
        END IF;

        -- Ajouter les tables des jointures si spécifiées
        IF col_rec.join_tables IS NOT NULL THEN
            v_needed_tables := v_needed_tables || col_rec.join_tables;
        END IF;
    END LOOP;

    -- Construire les jointures en respectant les dépendances
    WHILE array_length(v_needed_tables, 1) > 0 LOOP
        DECLARE
            found_table BOOLEAN := FALSE;
        BEGIN
            FOR join_rec IN
                SELECT table_name, join_sql, depends_on, priority
                FROM export_csv.join_definition
                WHERE table_name = ANY(v_needed_tables)
                AND NOT (table_name = ANY(v_processed_tables))
                AND (depends_on IS NULL OR depends_on <@ v_processed_tables)
                ORDER BY priority
            LOOP
                v_join_clause := v_join_clause || '\n' || join_rec.join_sql;
                v_processed_tables := v_processed_tables || join_rec.table_name;
                v_needed_tables := array_remove(v_needed_tables, join_rec.table_name);
                found_table := TRUE;
                EXIT; -- Traiter une table à la fois pour respecter les dépendances
            END LOOP;

            -- Protection contre les boucles infinies
            IF NOT found_table THEN
                RAISE WARNING 'Impossible de résoudre les dépendances pour les tables: %', v_needed_tables;
                EXIT;
            END IF;
        END;
    END LOOP;

    -- Construire la requête finale
    v_query := v_select_clause || '\n' || v_from_clause || v_join_clause;
    v_query := v_query || '\nWHERE q.date_creation BETWEEN :dateDebut AND :dateFin';
    v_query := v_query || '\nORDER BY q.id';

    RETURN v_query;
END;
$$;

-- ====================================
-- 13. FONCTION DE TRANSFORMATION DES VALEURS
-- ====================================
CREATE OR REPLACE FUNCTION export_csv.transform_value(
    p_raw_value TEXT,
    p_code_variable TEXT,
    p_regle_gestion TEXT,
    p_valeurs_possibles TEXT[]
)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_raw_value IS NULL THEN
        RETURN NULL;
    END IF;

    CASE p_regle_gestion
        WHEN 'POSITION_VALEUR_POSSIBLE' THEN
            -- Transformer la valeur texte en position (1, 2, 3...)
            FOR i IN 1..array_length(p_valeurs_possibles, 1) LOOP
                IF p_valeurs_possibles[i] = p_raw_value THEN
                    RETURN i::TEXT;
                END IF;
            END LOOP;
            RETURN NULL; -- Valeur non trouvée

        WHEN 'TEXTE_DIRECT' THEN
            RETURN p_raw_value;

        WHEN 'NUMERIQUE_DIRECT' THEN
            RETURN p_raw_value;

        WHEN 'DATE_ISO' THEN
            -- Formater la date au format ISO
            BEGIN
                RETURN to_char(p_raw_value::DATE, 'YYYY-MM-DD');
            EXCEPTION
                WHEN OTHERS THEN
                    RETURN p_raw_value; -- Retourner la valeur originale si échec parsing
            END;

        ELSE
            RETURN p_raw_value; -- Règle inconnue -> valeur brute
    END CASE;
END;
$$;

-- Commentaire sur la migration
COMMENT ON SCHEMA export_csv IS 'Schéma dédié au système d''export CSV avec gestion des définitions métier et technique';
COMMENT ON TABLE export_csv.csv_column_definition IS 'Définitions des colonnes CSV (partie métier depuis Excel)';
COMMENT ON TABLE export_csv.column_sql_mapping IS 'Mapping technique SQL (géré par les développeurs)';
COMMENT ON FUNCTION export_csv.build_export_query(TEXT) IS 'Génère dynamiquement la requête SQL d''export pour un CSV donné';