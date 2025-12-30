Plan de mise en place - Export hebdomadaire piloté par Excel

  📋 Vue d'ensemble

  Objectif : Système d'export hebdomadaire de 3 CSV (144, 14, 500 colonnes) piloté par un fichier Excel contractuel, utilisant Spring Batch et
  PostgreSQL.

  🏗️ Architecture proposée

  Excel Contractuel → Import/Parse → Staging PostgreSQL → Spring Batch Jobs → CSV Export

  📝 Découpage en tâches

  Phase 1 : Infrastructure de données (1-2 jours)

  1.1 Modèle PostgreSQL

  - Créer les tables de staging Excel (excel_csv_definition_stg)
  - Créer les tables normalisées (csv_definition, valeur_possible)
  - Scripts Flyway de migration
  - Index pour performance

  1.2 Entités JPA

  - ExcelCsvDefinitionStg
  - CsvDefinition
  - ValeurPossible
  - Relations et mappings

  Phase 2 : Import et parsing Excel (2-3 jours)

  2.1 Service d'import Excel

  - Parseur Apache POI pour lire les 3 feuilles
  - Validation structure Excel
  - Transformation vers modèle staging
  - Gestion des erreurs de format

  2.2 Service de normalisation

  - Transformation staging → tables normalisées
  - Validation règles métier
  - Dédoublonnage et consolidation

  Phase 3 : Moteur de transformation (3-4 jours)

  3.1 Règles de gestion

  - Interface RegleGestion
  - Implémentation PositionValeurPossibleRegle
  - Factory pour résolution dynamique
  - Support extensibilité futures règles

  3.2 Service de mapping questionnaire

  - Récupération données questionnaires
  - Application règles par colonne
  - Transformation selon type_question
  - Cache pour performance

  Phase 4 : Jobs Spring Batch (2-3 jours)

  4.1 Job d'export principal

  - 3 steps (1 par CSV)
  - Reader : requêtes optimisées PostgreSQL
  - Processor : application transformation
  - Writer : génération CSV streaming

  4.2 Configuration et ordonnancement

  - JobParameters pour flexibilité
  - Scheduling hebdomadaire
  - Restart capability
  - Monitoring et alertes

  Phase 5 : Génération CSV (1-2 jours)

  5.1 Writer CSV optimisé

  - Streaming pour gros volumes (500 colonnes)
  - Respect ordre Excel (ordre_colonne)
  - Formatage selon format défini
  - Gestion encodage

  5.2 Stockage et distribution

  - Sauvegarde fichiers sur S3
  - Nommage avec timestamp
  - Rétention configurable

  Phase 6 : Qualité et tests (2-3 jours)

  6.1 Tests unitaires

  - Services de transformation
  - Règles de gestion
  - Parseur Excel
  - Génération CSV

  6.2 Tests d'intégration

  - End-to-end avec données réelles
  - Performance gros volumes
  - Testcontainers PostgreSQL

  Phase 7 : Monitoring et exploitation (1-2 jours)

  7.1 Observabilité

  - Métriques Spring Batch
  - Logs structurés
  - Dashboard monitoring
  - Alertes échecs

  7.2 Documentation

  - Guide déploiement
  - Format Excel contractuel
  - Procédures d'exploitation

  🚀 Estimation totale : 12-18 jours

  📊 Points d'attention

  Performance

  - Index PostgreSQL optimisés
  - Streaming CSV pour 500 colonnes
  - Cache règles de gestion

  Robustesse

  - Validation Excel stricte
  - Retry automatique Spring Batch
  - Rollback en cas d'échec

  Maintenabilité

  - Règles externalisées dans Excel
  - Code générique extensible
  - Tests automatisés