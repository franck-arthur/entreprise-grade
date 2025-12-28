# Migration UUID vers Long - Services et DTOs

## Contexte

Migration des identifiants UUID vers Long dans les services de l'application suite à la refactorisation des entités de domaine.

## Fichiers Modifiés

### 1. FormationService.java
**Chemin**: `/backend/src/main/java/com/enterprise/app/application/service/FormationService.java`

**Modifications effectuées**:
- Suppression de l'import `java.util.UUID`
- Modification des signatures de méthodes :
  - `getFormationById(UUID)` → `getFormationById(Long)`
  - `updateFormation(UUID, Formation)` → `updateFormation(Long, Formation)`
  - `deleteFormation(UUID)` → `deleteFormation(Long)`
  - `inscrireUtilisateur(UUID, UUID)` → `inscrireUtilisateur(Long, Long)`
  - `desinscrireUtilisateur(UUID, UUID)` → `desinscrireUtilisateur(Long, Long)`
  - `getParticipantsFormation(UUID)` → `getParticipantsFormation(Long)`
  - `getFormationsUtilisateur(UUID, Pageable)` → `getFormationsUtilisateur(Long, Pageable)`
  - `marquerPresence(UUID, UUID, boolean)` → `marquerPresence(Long, Long, boolean)`
  - `getFormationProjectionById(UUID)` → `getFormationProjectionById(Long)`

### 2. BatchImportService.java
**Chemin**: `/backend/src/main/java/com/enterprise/app/application/service/BatchImportService.java`

**Modifications effectuées**:
- Remplacement de l'import wildcard `java.util.*` par des imports spécifiques sans UUID
- Modification des signatures de méthodes :
  - `processFileAsync(UUID, MultipartFile)` → `processFileAsync(Long, MultipartFile)`
  - `updateBatchImportResults(UUID, List<BatchImportLine>)` → `updateBatchImportResults(Long, List<BatchImportLine>)`
  - `failBatchImport(UUID, String)` → `failBatchImport(Long, String)`
  - `getBatchImportById(UUID)` → `getBatchImportById(Long)`
  - `getBatchImportsByUser(UUID, Pageable)` → `getBatchImportsByUser(Long, Pageable)`
  - `cancelBatchImport(UUID)` → `cancelBatchImport(Long)`

### 3. AuditQueryService.java
**Chemin**: `/backend/src/main/java/com/enterprise/app/application/service/audit/AuditQueryService.java`

**Modifications effectuées**:
- Suppression de l'import `java.util.UUID`
- Modification des signatures de méthodes :
  - `findEventsByUser(UUID, Pageable)` → `findEventsByUser(Long, Pageable)`
  - `findEventsByTargetEntity(String, UUID, Pageable)` → `findEventsByTargetEntity(String, Long, Pageable)`

### 4. AuditEventQuery.java
**Chemin**: `/backend/src/main/java/com/enterprise/app/application/dto/audit/AuditEventQuery.java`

**Modifications effectuées**:
- Suppression de l'import `java.util.UUID`
- Modification des propriétés :
  - `private UUID userId` → `private Long userId`
  - `private UUID targetEntityId` → `private Long targetEntityId`

## Impact

- **Compilation** : ✅ Réussie sans erreurs après modifications
- **Cohérence** : Alignement complet avec le modèle de domaine utilisant des identifiants Long
- **Performance** : Amélioration potentielle due à l'utilisation d'identifiants plus simples

## Prochaines Étapes

1. Mettre à jour les tests unitaires et d'intégration pour utiliser des identifiants Long
2. Vérifier les contrôleurs REST pour s'assurer de la cohérence des types
3. Mettre à jour la documentation API si nécessaire
4. Tester l'ensemble de l'application pour s'assurer du bon fonctionnement

## Notes Techniques

- Tous les imports UUID ont été supprimés pour éviter les références inutiles
- Les modifications sont rétro-compatibles au niveau de la logique métier
- Aucune modification de la logique interne des méthodes n'était nécessaire