# Migration des interfaces Port : UUID vers Long

## Contexte
Migration des types UUID vers Long dans les interfaces ports pour corriger les erreurs de compilation liées au changement de type d'identifiant dans les entités.

## Fichiers modifiés

### 1. AuditEventQueryPort.java
**Chemin**: `/backend/src/main/java/com/enterprise/app/domain/port/AuditEventQueryPort.java`

**Modifications effectuées**:
- Suppression de l'import `java.util.UUID`
- Remplacement des paramètres UUID par Long dans les méthodes suivantes :
  - `findById(UUID id)` → `findById(Long id)`
  - `findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable)` → `findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable)`
  - `findByTargetEntityTypeAndTargetEntityIdOrderByTimestampDesc(String entityType, UUID entityId, Pageable pageable)` → `findByTargetEntityTypeAndTargetEntityIdOrderByTimestampDesc(String entityType, Long entityId, Pageable pageable)`
  - Dans `findByFilters(...)` : paramètres `UUID userId` et `UUID targetEntityId` → `Long userId` et `Long targetEntityId`

### 2. BatchImportPort.java
**Chemin**: `/backend/src/main/java/com/enterprise/app/domain/port/BatchImportPort.java`

**Modifications effectuées**:
- Suppression de l'import `java.util.UUID`
- Remplacement des paramètres UUID par Long dans les méthodes suivantes :
  - `findById(UUID id)` → `findById(Long id)`
  - `findByInitiatedByUserId(UUID userId, Pageable pageable)` → `findByInitiatedByUserId(Long userId, Pageable pageable)`
  - `existsById(UUID id)` → `existsById(Long id)`

## Validation
- ✅ Compilation du projet réussie
- ✅ Toutes les méthodes des interfaces mises à jour
- ✅ Imports UUID supprimés
- ✅ Cohérence avec les entités utilisant Long comme identifiant

## Impact
Cette modification assure la cohérence entre :
- Les entités du domaine (qui utilisent maintenant Long comme ID)
- Les interfaces ports
- Les implémentations des repositories
- Les services d'application

Date: 2025-12-27
Auteur: Claude Code