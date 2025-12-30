-- Migration V003 : Initialisation du mapping technique pour les exports CSV
-- Ce script contient des exemples de mappings techniques pour démarrer

-- ====================================
-- 1. EXEMPLES DE MAPPINGS SIMPLES
-- ====================================

-- Colonnes de base du questionnaire
INSERT INTO export_csv.column_sql_mapping
(code_variable, source_table, source_column, description, exemple_valeur, join_tables)
VALUES
('Q_ID', 'questionnaire', 'id', 'Identifiant unique du questionnaire', '12345', ARRAY[]::TEXT[]),
('Q_DATE_CREATION', 'questionnaire', 'date_creation', 'Date de création du questionnaire', '2024-01-15', ARRAY[]::TEXT[]),
('Q_STATUT', 'questionnaire', 'statut', 'Statut du questionnaire', 'TERMINE', ARRAY[]::TEXT[]);

-- Colonnes de la personne (nécessite jointure avec personne)
INSERT INTO export_csv.column_sql_mapping
(code_variable, source_table, source_column, description, exemple_valeur, join_tables)
VALUES
('Q_SEXE', 'personne', 'sexe', 'Sexe de la personne', 'Homme', ARRAY['personne']),
('Q_EMAIL', 'personne', 'email', 'Email de la personne', 'jean.dupont@example.com', ARRAY['personne']),
('Q_PRENOM', 'personne', 'prenom', 'Prénom de la personne', 'Jean', ARRAY['personne']),
('Q_NOM', 'personne', 'nom', 'Nom de famille de la personne', 'Dupont', ARRAY['personne']),
('Q_DATE_NAISSANCE', 'personne', 'date_naissance', 'Date de naissance', '1985-06-15', ARRAY['personne']);

-- Colonnes de l'adresse (nécessite jointure avec personne et adresse)
INSERT INTO export_csv.column_sql_mapping
(code_variable, source_table, source_column, description, exemple_valeur, join_tables)
VALUES
('Q_VILLE', 'adresse', 'ville', 'Ville de résidence', 'Paris', ARRAY['personne', 'adresse']),
('Q_CODE_POSTAL', 'adresse', 'code_postal', 'Code postal', '75001', ARRAY['personne', 'adresse']),
('Q_PAYS', 'adresse', 'pays', 'Pays de résidence', 'France', ARRAY['personne', 'adresse']);

-- ====================================
-- 2. EXEMPLES DE MAPPINGS COMPLEXES (EXPRESSIONS SQL)
-- ====================================

-- Calcul de l'âge
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_AGE',
 'EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance))',
 'Âge calculé à partir de la date de naissance',
 '38',
 false,
 false);

-- Nombre total de formations suivies
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_NB_FORMATIONS',
 '(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id)',
 'Nombre total de formations suivies par la personne',
 '3',
 true,
 true);

-- Formation principale (la plus récente)
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_FORMATION_PRINCIPALE',
 '(SELECT f.titre FROM formation_participation fp JOIN formation f ON f.id = fp.formation_id WHERE fp.questionnaire_id = q.id ORDER BY fp.date_debut DESC LIMIT 1)',
 'Titre de la formation la plus récente',
 'Management Avancé',
 true,
 false);

-- Secteur principal (le plus fréquent)
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_SECTEUR_PRINCIPAL',
 '(SELECT s.nom FROM formation_participation fp JOIN formation f ON f.id = fp.formation_id JOIN secteur s ON s.id = f.secteur_id WHERE fp.questionnaire_id = q.id GROUP BY s.nom ORDER BY COUNT(*) DESC LIMIT 1)',
 'Secteur le plus fréquent dans les formations',
 'Informatique',
 true,
 true);

-- Durée totale de formation en heures
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_DUREE_TOTALE_FORMATION',
 '(SELECT COALESCE(SUM(f.duree_heures), 0) FROM formation_participation fp JOIN formation f ON f.id = fp.formation_id WHERE fp.questionnaire_id = q.id)',
 'Durée totale en heures de toutes les formations',
 '120',
 true,
 true);

-- Note moyenne des formations
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_NOTE_MOYENNE',
 '(SELECT ROUND(AVG(fp.note_finale), 2) FROM formation_participation fp WHERE fp.questionnaire_id = q.id AND fp.note_finale IS NOT NULL)',
 'Note moyenne sur toutes les formations évaluées',
 '16.75',
 true,
 true);

-- Dernière connexion (si on a une table d'audit)
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_DERNIERE_CONNEXION',
 '(SELECT MAX(ae.date_creation) FROM audit_event ae WHERE ae.user_id = p.user_id AND ae.event_type = ''LOGIN'')',
 'Date de la dernière connexion de l''utilisateur',
 '2024-01-20 14:30:00',
 true,
 true);

-- Indicateur de formation récente (dans les 6 derniers mois)
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_FORMATION_RECENTE',
 'CASE WHEN EXISTS(SELECT 1 FROM formation_participation fp WHERE fp.questionnaire_id = q.id AND fp.date_debut >= CURRENT_DATE - INTERVAL ''6 months'') THEN ''OUI'' ELSE ''NON'' END',
 'Indique si la personne a suivi une formation dans les 6 derniers mois',
 'OUI',
 false,
 false);

-- ====================================
-- 3. MAPPINGS POUR DES DONNÉES CALCULÉES
-- ====================================

-- Tranche d'âge
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_TRANCHE_AGE',
 'CASE
    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance)) < 25 THEN ''18-24''
    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance)) < 35 THEN ''25-34''
    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance)) < 45 THEN ''35-44''
    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance)) < 55 THEN ''45-54''
    ELSE ''55+''
 END',
 'Tranche d''âge de la personne',
 '35-44',
 false,
 false);

-- Niveau de satisfaction moyen
INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
('Q_SATISFACTION_MOYENNE',
 '(SELECT ROUND(AVG(fp.note_satisfaction), 1) FROM formation_participation fp WHERE fp.questionnaire_id = q.id AND fp.note_satisfaction IS NOT NULL)',
 'Note moyenne de satisfaction sur toutes les formations',
 '4.2',
 true,
 true);

-- ====================================
-- 4. COMMENTAIRES ET DOCUMENTATION
-- ====================================

COMMENT ON TABLE export_csv.column_sql_mapping IS 'Mapping technique entre les codes variables métier et les requêtes SQL';

-- Statistiques après insertion
DO $$
DECLARE
    nb_mappings_simples INTEGER;
    nb_mappings_complexes INTEGER;
    nb_total INTEGER;
BEGIN
    SELECT COUNT(*) INTO nb_mappings_simples
    FROM export_csv.column_sql_mapping
    WHERE source_table IS NOT NULL;

    SELECT COUNT(*) INTO nb_mappings_complexes
    FROM export_csv.column_sql_mapping
    WHERE sql_expression IS NOT NULL;

    SELECT COUNT(*) INTO nb_total
    FROM export_csv.column_sql_mapping;

    RAISE NOTICE 'Mappings techniques initialisés:';
    RAISE NOTICE '- Mappings simples (table + colonne): %', nb_mappings_simples;
    RAISE NOTICE '- Mappings complexes (expression SQL): %', nb_mappings_complexes;
    RAISE NOTICE '- Total: %', nb_total;
END $$;