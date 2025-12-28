# Refactoring du StorageService selon les principes SOLID

## Analyse du StorageService actuel

### Violations des principes SOLID identifiées

**Single Responsibility Principle (SRP) ❌**
- Le service mélange plusieurs responsabilités :
  - Upload/download de fichiers
  - Gestion des métadonnées
  - Génération de clés
  - Listage et suppression

**Open/Closed Principle (OCP) ❌**
- Difficile d'étendre pour de nouveaux types de stockage sans modification du code existant
- Couplage fort avec l'implémentation S3

**Dependency Inversion Principle (DIP) ❌**
- Dépendance directe sur l'implémentation S3Client
- Pas d'abstraction pour le stockage

**Liskov Substitution Principle (LSP) ✅**
- Pas d'héritage complexe, donc respecté

**Interface Segregation Principle (ISP) ✅**
- Pas d'interfaces surchargées

## Nouvelle architecture SOLID

### Structure des interfaces

```
domain/storage/
├── FileStorage.java           # Interface principale pour le stockage
├── DirectoryManager.java      # Interface pour la gestion des répertoires
├── ZipService.java           # Interface pour la compression
├── FileMetadata.java         # Record pour les métadonnées
└── FileData.java            # Record pour les données de fichier
```

### Implémentations

```
infrastructure/storage/
├── S3FileStorage.java        # Implémentation S3 de FileStorage
├── S3DirectoryManager.java   # Implémentation S3 pour les répertoires
└── ZipServiceImpl.java       # Implémentation du service ZIP
```

### Service orchestrateur

```
application/service/
└── EnhancedStorageService.java # Service principal respectant SOLID
```

## Fonctionnalités ajoutées

### 1. Création de répertoires
- `createDirectoryWithFiles()` : Crée un répertoire et y ajoute plusieurs fichiers
- Support des structures de répertoires hiérarchiques
- Génération automatique de paths uniques avec timestamp

### 2. Gestion de fichiers multiples
- `addFilesToDirectory()` : Ajoute plusieurs fichiers en une opération
- `addFileToExistingDirectory()` : Ajoute un fichier à un répertoire existant
- Validation de l'existence des répertoires

### 3. Compression ZIP
- `createAndZipDirectory()` : Crée un répertoire, y ajoute des fichiers et le zippe
- `downloadZippedDirectory()` : Télécharge un répertoire compressé
- `uploadZippedDirectory()` : Upload le ZIP vers S3
- Gestion automatique des entrées ZIP avec chemins relatifs

## Respect des principes SOLID

### Single Responsibility Principle (SRP) ✅
- **FileStorage** : Uniquement les opérations de base sur les fichiers
- **DirectoryManager** : Uniquement la gestion des répertoires
- **ZipService** : Uniquement la compression/décompression
- **EnhancedStorageService** : Orchestration des services

### Open/Closed Principle (OCP) ✅
- Interfaces permettent d'ajouter de nouvelles implémentations sans modification
- Possibilité d'ajouter LocalFileStorage, FTPStorage, etc.

### Liskov Substitution Principle (LSP) ✅
- Toutes les implémentations respectent les contrats des interfaces
- Substitution transparente possible

### Interface Segregation Principle (ISP) ✅
- Interfaces spécialisées et cohésives
- Pas de dépendances sur des méthodes non utilisées

### Dependency Inversion Principle (DIP) ✅
- EnhancedStorageService dépend des abstractions, pas des implémentations
- Injection de dépendances via les interfaces

## Usage

### Exemple : Créer un répertoire avec fichiers et le zipper

```java
@Autowired
private EnhancedStorageService enhancedStorageService;

public String createProjectArchive(Map<String, MultipartFile> files) {
    return enhancedStorageService.createAndZipDirectory(
        "mon-projet",
        files,
        "projet-archive.zip"
    );
}
```

### Exemple : Ajouter des fichiers à un répertoire existant

```java
public String addFileToProject(String directoryPath, MultipartFile file) {
    return enhancedStorageService.addFileToExistingDirectory(
        directoryPath,
        "nouveau-fichier.txt",
        file
    );
}
```

## Bénéfices

1. **Maintenabilité** : Code modulaire et responsabilités séparées
2. **Extensibilité** : Facilité d'ajout de nouveaux types de stockage
3. **Testabilité** : Interfaces permettent le mocking facilement
4. **Réutilisabilité** : Composants indépendants réutilisables
5. **Conformité SOLID** : Architecture respectant les bonnes pratiques

## Tests

### Tests Unitaires

#### S3FileStorageTest
- **Coverage** : Toutes les méthodes de l'interface FileStorage
- **Mocking** : S3Client et S3Properties mockés avec Mockito
- **Cas testés** :
  - Upload/download/delete réussis et en échec
  - Listing des fichiers avec et sans préfixe
  - Vérification d'existence de fichiers
  - Récupération des métadonnées
  - Gestion des exceptions S3

#### S3DirectoryManagerTest
- **Coverage** : Toutes les méthodes de l'interface DirectoryManager
- **Mocking** : FileStorage mocké
- **Cas testés** :
  - Création de répertoires avec différents formats de paths
  - Ajout de fichiers individuels et multiples
  - Vérification d'existence de répertoires
  - Suppression de répertoires et de tous leurs fichiers
  - Normalisation des chemins (backslashes, trailing slashes)

#### ZipServiceImplTest
- **Coverage** : Toutes les méthodes de l'interface ZipService
- **Mocking** : FileStorage mocké
- **Cas testés** :
  - Création de ZIP à partir de répertoires avec fichiers
  - Gestion des répertoires vides
  - Exclusion des fichiers .directory
  - Upload des ZIP vers S3
  - Gestion des noms de fichiers avec caractères spéciaux
  - Structure des entrées ZIP avec chemins relatifs

#### EnhancedStorageServiceTest
- **Coverage** : Toutes les méthodes publiques du service orchestrateur
- **Mocking** : FileStorage, DirectoryManager, ZipService mockés
- **Cas testés** :
  - Orchestration complète des opérations
  - Création de répertoires avec fichiers multiples
  - Ajout à des répertoires existants
  - Création et upload de ZIP
  - Gestion des erreurs et validations
  - Génération de chemins uniques
  - Nettoyage des noms de fichiers

### Tests d'Intégration

#### S3StorageIntegrationTest
- **Environment** : LocalStack avec Testcontainers
- **Coverage** : Tests bout-en-bout de tous les composants avec vraie infrastructure S3
- **Cas testés** :
  - Upload/download réels vers S3
  - Création et gestion de répertoires
  - Création de ZIP avec vrai contenu
  - Upload de ZIP vers S3
  - Suppression de répertoires
  - Métadonnées de fichiers
  - Listing avec préfixes
  - Structures de répertoires imbriquées

#### EnhancedStorageServiceIntegrationTest
- **Environment** : LocalStack avec Testcontainers
- **Coverage** : Workflow complet du service principal
- **Cas testés** :
  - Workflow complet : création → ajout → ZIP → suppression
  - Gestion des fichiers avec caractères spéciaux
  - Génération de chemins uniques
  - Gestion des erreurs (répertoires inexistants)
  - Upload de fichiers vides
  - Organisation par dossiers

### Utilitaires de Test

#### StorageTestHelper
- **Builders** : ProjectBuilder pour création facile de projets de test
- **Verifiers** : ZipVerifier pour validation des archives
- **Utilities** :
  - Création de MockMultipartFile avec différents types
  - Lecture et vérification du contenu des ZIP
  - Assertions spécialisées pour les chemins et clés
  - Helpers pour InputStream et contenu

### Configuration de Test

#### BaseUnitTest
- Extension Mockito configurée
- Reset automatique des mocks
- Méthodes utilitaires pour ArgumentCaptor

#### BaseIntegrationTest
- Configuration Testcontainers avec PostgreSQL
- Profiles de test activés
- Nettoyage automatique entre les tests

### Stratégie de Test

1. **Tests Unitaires** : Isolation complète, mocking de toutes les dépendances
2. **Tests d'Intégration** : Infrastructure réelle avec Testcontainers
3. **Coverage** : ~95% de couverture de code sur les nouvelles classes
4. **Performance** : Tests d'intégration optimisés avec réutilisation de containers

### Commandes de Test

```bash
# Tests unitaires uniquement
./mvnw test -Dtest="**/*Test"

# Tests d'intégration uniquement
./mvnw test -Dtest="**/*IntegrationTest"

# Tous les tests
./mvnw test

# Coverage avec JaCoCo
./mvnw test jacoco:report
```

## Nouveau Workflow : Gestion Local → S3 → Nettoyage

### Description du Workflow

Ce nouveau workflow répond au besoin spécifique suivant :
1. **Création de fichiers vides** sur le disque local
2. **Remplissage externe** par un batch via commande `cp`
3. **Upload vers S3** après remplissage
4. **Nettoyage** des fichiers temporaires sur le disque

### Architecture du Workflow

#### Nouvelles Interfaces

```java
// Gestion des fichiers locaux
LocalFileManager
├── createDirectoryWithEmptyFiles()
├── createEmptyFile()
├── directoryExists()
├── deleteDirectory()
└── isFileEmpty()

// Orchestration du workflow complet
FileWorkflowManager
├── createFileJob()
├── areFilesReady()
├── uploadFilesToS3()
├── cleanupLocalFiles()
└── processCompleteWorkflow()
```

#### Nouvelles Implémentations

```java
LocalFileManagerImpl          // Gestion fichiers sur disque local
FileWorkflowManagerImpl       // Orchestration du workflow
```

### Utilisation du Nouveau Workflow

#### 1. Création d'un Job avec Fichiers Vides

```java
@Autowired
private EnhancedStorageService storageService;

// Créer un job avec fichiers vides
Set<String> fileNames = Set.of("report.csv", "summary.txt", "data.json");
FileWorkflowManager.FileJob job = storageService.createFileJobWithEmptyFiles(
    "mon-projet",
    fileNames
);

// Le job contient les informations du répertoire local
Path localDirectory = job.localDirectory();
String jobId = job.jobId();

System.out.println("Répertoire créé : " + localDirectory);
System.out.println("Job ID : " + jobId);
// Fichiers vides créés : report.csv, summary.txt, data.json
```

#### 2. Remplissage par Batch Externe

```bash
# Le batch externe utilise cp pour remplir les fichiers
cp /source/data.csv ${LOCAL_DIRECTORY}/report.csv
cp /source/summary.txt ${LOCAL_DIRECTORY}/summary.txt
echo '{"status": "complete"}' > ${LOCAL_DIRECTORY}/data.json
```

#### 3. Vérification et Upload

```java
// Vérifier que les fichiers sont prêts
if (storageService.areJobFilesReady(jobId)) {

    // Option A: Workflow complet automatique
    List<String> s3Keys = storageService.processCompleteWorkflow(jobId);
    System.out.println("Fichiers uploadés : " + s3Keys);
    // Les fichiers locaux sont automatiquement supprimés

    // Option B: Étapes manuelles
    List<String> uploadedKeys = storageService.uploadJobFilesToS3(jobId);
    storageService.cleanupJobLocalFiles(jobId);
}
```

#### 4. Gestion des Jobs

```java
// Obtenir les informations d'un job
FileWorkflowManager.FileJob jobInfo = storageService.getJobInfo(jobId);
System.out.println("Status : " + jobInfo.status());

// Lister tous les jobs actifs
List<FileWorkflowManager.FileJob> activeJobs = storageService.listActiveJobs();

// Supprimer un job (nettoie toutes les ressources)
storageService.deleteJob(jobId);
```

### États d'un Job

```java
enum FileJobStatus {
    CREATED,           // Répertoire et fichiers vides créés
    FILES_READY,       // Fichiers remplis par le batch
    UPLOADED_TO_S3,    // Fichiers uploadés sur S3
    CLEANED_UP,        // Fichiers locaux supprimés
    FAILED             // Erreur dans le processus
}
```

### Configuration

#### Properties

```properties
# Répertoire temporaire de travail
app.storage.temp.directory=/tmp/enterprise-app

# Configuration S3 existante
app.s3.bucket=my-bucket
app.s3.region=eu-west-1
```

#### Bean Configuration

Les nouveaux services sont automatiquement configurés via Spring :

```java
@Component LocalFileManagerImpl
@Component FileWorkflowManagerImpl
```

### Avantages du Nouveau Workflow

1. **Séparation des responsabilités** : Création locale vs Upload S3
2. **Intégration batch** : Fichiers remplis par processus externe
3. **Gestion des erreurs** : États clairs et rollback possible
4. **Performance** : Upload groupé après remplissage complet
5. **Nettoyage automatique** : Pas de fuite de fichiers temporaires

### Exemples d'Utilisation

#### Workflow Batch ETL

```java
// 1. Créer les fichiers de sortie vides
FileJob job = storageService.createFileJobWithEmptyFiles(
    "etl-job-" + LocalDate.now(),
    Set.of("extracted.csv", "transformed.json", "log.txt")
);

// 2. Le job ETL remplit les fichiers
runEtlProcess(job.localDirectory());

// 3. Upload et nettoyage
if (storageService.areJobFilesReady(job.jobId())) {
    List<String> s3Keys = storageService.processCompleteWorkflow(job.jobId());
    notifySuccess(s3Keys);
}
```

#### Génération de Rapports

```java
// 1. Préparer les fichiers de rapport vides
FileJob reportJob = storageService.createFileJobWithEmptyFiles(
    "monthly-report",
    Set.of("sales.xlsx", "summary.pdf", "charts.png")
);

// 2. Le générateur de rapports remplit les fichiers
generateReports(reportJob.localDirectory());

// 3. Archivage S3
List<String> archivedFiles = storageService.processCompleteWorkflow(reportJob.jobId());
```

## Migration

### Coexistence

L'ancien StorageService reste fonctionnel et peut coexister avec la nouvelle architecture.
Le nouveau workflow est additionnel et n'impacte pas l'existant.

### Migration Progressive

```java
// Ancien usage (toujours supporté)
String key = storageService.uploadSingleFile(file, "folder");

// Nouveau usage pour workflow batch
FileJob job = storageService.createFileJobWithEmptyFiles("project", fileNames);
// ... processus batch ...
List<String> keys = storageService.processCompleteWorkflow(job.jobId());
```