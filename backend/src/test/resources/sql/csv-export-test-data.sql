-- Données de test pour les tests d'intégration du système CSV Export
-- Ce fichier contient des données d'exemple pour tester l'ensemble du système

-- ====================================
-- 1. DONNÉES DE TEST POUR LES DÉFINITIONS DE COLONNES
-- ====================================

-- CSV de test avec colonnes dans un ordre spécifique
INSERT INTO export_csv.csv_column_definition
(nom_csv, ordre_colonne, code_variable, libelle, format, type_question, regle_gestion, actif)
VALUES
-- L'ordre est crucial pour les tests
('CSV_TEST', 1, 'Q_ID', 'Identifiant du questionnaire', 'NUM', 'NUMERIQUE', 'NUMERIQUE_DIRECT', true),
('CSV_TEST', 2, 'Q_SEXE', 'Sexe de la personne', 'NUM', 'LISTE', 'POSITION_VALEUR_POSSIBLE', true),
('CSV_TEST', 3, 'Q_EMAIL', 'Email de la personne', 'TEXTE', 'TEXTE', 'TEXTE_DIRECT', true),
('CSV_TEST', 4, 'Q_VILLE', 'Ville de résidence', 'TEXTE', 'TEXTE', 'TEXTE_DIRECT', true),
-- Colonne inactive pour tester le filtre
('CSV_TEST', 5, 'Q_INACTIVE', 'Colonne inactive', 'TEXTE', 'TEXTE', 'TEXTE_DIRECT', false);

-- ====================================
-- 2. VALEURS POSSIBLES POUR LES COLONNES DE TYPE LISTE
-- ====================================

-- Valeurs possibles pour Q_SEXE
INSERT INTO export_csv.csv_column_values
(column_definition_id, position, valeur_possible)
VALUES
((SELECT id FROM export_csv.csv_column_definition WHERE code_variable = 'Q_SEXE' AND nom_csv = 'CSV_TEST'), 1, 'Homme'),
((SELECT id FROM export_csv.csv_column_definition WHERE code_variable = 'Q_SEXE' AND nom_csv = 'CSV_TEST'), 2, 'Femme'),
((SELECT id FROM export_csv.csv_column_definition WHERE code_variable = 'Q_SEXE' AND nom_csv = 'CSV_TEST'), 3, 'Autre');

-- ====================================
-- 3. MAPPINGS TECHNIQUES SIMPLES
-- ====================================

INSERT INTO export_csv.column_sql_mapping
(code_variable, source_table, source_column, description, exemple_valeur, join_tables)
VALUES
-- Mapping simple pour Q_ID
('Q_ID', 'questionnaire', 'id', 'Identifiant unique du questionnaire', '12345', ARRAY[]::TEXT[]),

-- Mapping avec jointure pour Q_SEXE
('Q_SEXE', 'personne', 'sexe', 'Sexe de la personne', 'Homme', ARRAY['personne']),

-- Mapping avec jointure pour Q_EMAIL
('Q_EMAIL', 'personne', 'email', 'Email de la personne', 'test@example.com', ARRAY['personne']);

-- Note: Q_VILLE n'a volontairement pas de mapping pour tester les colonnes sans mapping

-- ====================================
-- 4. MAPPINGS TECHNIQUES COMPLEXES
-- ====================================

INSERT INTO export_csv.column_sql_mapping
(code_variable, sql_expression, description, exemple_valeur, requires_subquery, is_aggregation)
VALUES
-- Calcul de l'âge
('Q_AGE_CALC',
 'EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_naissance))',
 'Âge calculé à partir de la date de naissance',
 '35',
 false,
 false),

-- Nombre de formations (avec sous-requête)
('Q_NB_FORMATIONS_CALC',
 '(SELECT COUNT(*) FROM formation_participation fp WHERE fp.questionnaire_id = q.id)',
 'Nombre total de formations suivies',
 '3',
 true,
 true),

-- Formation la plus récente
('Q_DERNIERE_FORMATION',
 '(SELECT f.titre FROM formation_participation fp JOIN formation f ON f.id = fp.formation_id WHERE fp.questionnaire_id = q.id ORDER BY fp.date_debut DESC LIMIT 1)',
 'Titre de la formation la plus récente',
 'Formation Leadership',
 true,
 false);

-- ====================================
-- 5. DONNÉES DE TEST POUR D'AUTRES CSV
-- ====================================

-- Quelques colonnes pour CSV_1 (pour tester les statistiques)
INSERT INTO export_csv.csv_column_definition
(nom_csv, ordre_colonne, code_variable, libelle, format, type_question, regle_gestion, actif)
VALUES
('CSV_1', 1, 'Q_PARTICIPANT_ID', 'ID Participant', 'NUM', 'NUMERIQUE', 'NUMERIQUE_DIRECT', true),
('CSV_1', 2, 'Q_FORMATION_TITRE', 'Titre Formation', 'TEXTE', 'TEXTE', 'TEXTE_DIRECT', true);

-- Mapping pour CSV_1
INSERT INTO export_csv.column_sql_mapping
(code_variable, source_table, source_column, description, join_tables)
VALUES
('Q_PARTICIPANT_ID', 'questionnaire', 'id', 'Identifiant du participant', ARRAY[]::TEXT[]);

-- ====================================
-- 6. DONNÉES DE TEST POUR L'HISTORIQUE D'EXPORT
-- ====================================

-- Quelques exécutions d'export pour les tests
INSERT INTO export_csv.export_execution
(nom_csv, date_debut, date_fin, statut, nb_lignes_exportees, chemin_fichier, duree_secondes)
VALUES
('CSV_TEST', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day' + INTERVAL '5 minutes', 'TERMINE', 1500, '/tmp/csv-test-20241228.csv', 300),
('CSV_TEST', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '3 minutes', 'TERMINE', 1200, '/tmp/csv-test-20241227.csv', 180),
('CSV_1', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day' + INTERVAL '10 minutes', 'TERMINE', 5000, '/tmp/csv1-20241228.csv', 600),
-- Export en cours pour tester le monitoring
('CSV_TEST', NOW() - INTERVAL '30 minutes', NULL, 'EN_COURS', NULL, NULL, NULL);

-- ====================================
-- 7. COMMENTAIRES POUR LES TESTS
-- ====================================

-- Les données ci-dessus permettent de tester :
-- 1. L'ordre des colonnes (CSV_TEST avec ordres 1,2,3,4)
-- 2. Les valeurs possibles (Q_SEXE avec Homme/Femme/Autre)
-- 3. Les mappings simples et complexes
-- 4. Les colonnes sans mapping (Q_VILLE)
-- 5. Les colonnes inactives (Q_INACTIVE)
-- 6. L'historique d'export
-- 7. Les exports en cours