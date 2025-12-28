# Migration des contrôleurs : UUID vers Long

## Résumé des modifications

Cette migration remplace tous les types UUID par Long dans les contrôleurs de l'API REST pour une meilleure cohérence avec la base de données et les performances.

## Fichiers modifiés

### 1. UserController.java (`/presentation/controller/v1/UserController.java`)

**Modifications apportées :**
- Suppression de l'import `java.util.UUID`
- Remplacement du paramètre UUID par Long dans les méthodes :
  - `getUserById(@PathVariable UUID id)` → `getUserById(@PathVariable Long id)`
  - `updateUser(@PathVariable UUID id, ...)` → `updateUser(@PathVariable Long id, ...)`
  - `deleteUser(@PathVariable UUID id)` → `deleteUser(@PathVariable Long id)`
  - `activateUser(@PathVariable UUID id)` → `activateUser(@PathVariable Long id)`
  - `deactivateUser(@PathVariable UUID id)` → `deactivateUser(@PathVariable Long id)`

**Impact :** 5 méthodes modifiées

### 2. FormationController.java (`/presentation/controller/v1/FormationController.java`)

**Modifications apportées :**
- Suppression de l'import `java.util.UUID`
- Remplacement du paramètre UUID par Long dans les méthodes :
  - `getFormationById(@PathVariable UUID id)` → `getFormationById(@PathVariable Long id)`
  - `updateFormation(@PathVariable UUID id, ...)` → `updateFormation(@PathVariable Long id, ...)`
  - `deleteFormation(@PathVariable UUID id)` → `deleteFormation(@PathVariable Long id)`
  - `getFormationParticipants(@PathVariable UUID id)` → `getFormationParticipants(@PathVariable Long id)`

**Impact :** 4 méthodes modifiées

### 3. FormationParticipationController.java (`/presentation/controller/v1/FormationParticipationController.java`)

**Modifications apportées :**
- Suppression de l'import `java.util.UUID`
- Remplacement des paramètres UUID par Long dans les méthodes :
  - `inscrireUtilisateur(@PathVariable UUID formationId, ...)` → `inscrireUtilisateur(@PathVariable Long formationId, ...)`
  - `desinscrireUtilisateur(@PathVariable UUID formationId, ...)` → `desinscrireUtilisateur(@PathVariable Long formationId, ...)`
  - `marquerPresence(@PathVariable UUID formationId, @PathVariable UUID userId, ...)` → `marquerPresence(@PathVariable Long formationId, @PathVariable Long userId, ...)`

**Impact :** 3 méthodes modifiées avec 4 paramètres au total

### 4. AuditController.java (`/presentation/controller/v1/AuditController.java`)

**Modifications apportées :**
- Suppression de l'import `java.util.UUID`
- Remplacement des paramètres UUID par Long dans les méthodes :
  - `@RequestParam(required = false) UUID userId` → `@RequestParam(required = false) Long userId`
  - `@RequestParam(required = false) UUID targetEntityId` → `@RequestParam(required = false) Long targetEntityId`
  - `getAuditEventsByUser(@PathVariable UUID userId, ...)` → `getAuditEventsByUser(@PathVariable Long userId, ...)`
  - `getAuditEventsByEntity(..., @PathVariable UUID entityId, ...)` → `getAuditEventsByEntity(..., @PathVariable Long entityId, ...)`

**Impact :** 4 paramètres modifiés dans 3 méthodes

### 5. BatchImportController.java (`/presentation/controller/v1/BatchImportController.java`)

**Modifications apportées :**
- Suppression de l'import `java.util.UUID`
- Remplacement du paramètre UUID par Long dans les méthodes :
  - `getBatchImportById(@PathVariable UUID id)` → `getBatchImportById(@PathVariable Long id)`
  - `getBatchImportStatus(@PathVariable UUID id)` → `getBatchImportStatus(@PathVariable Long id)`
  - `cancelBatchImport(@PathVariable UUID id)` → `cancelBatchImport(@PathVariable Long id)`

**Impact :** 3 méthodes modifiées

### 6. UserControllerV2.java (`/presentation/controller/v2/UserControllerV2.java`)

**Modifications apportées :**
- Suppression de l'import `java.util.UUID`
- Remplacement du paramètre UUID par Long dans les méthodes :
  - `getUserByIdV2(@PathVariable UUID id)` → `getUserByIdV2(@PathVariable Long id)`
  - `patchUserV2(@PathVariable UUID id, ...)` → `patchUserV2(@PathVariable Long id, ...)`
  - `updateUserV2(@PathVariable UUID id, ...)` → `updateUserV2(@PathVariable Long id, ...)`
  - `deleteUserV2(@PathVariable UUID id)` → `deleteUserV2(@PathVariable Long id)`

**Impact :** 4 méthodes modifiées

### 7. StorageController.java (`/presentation/controller/v1/StorageController.java`)

**Modifications apportées :** Aucune modification nécessaire
- Le contrôleur ne contenait pas d'usage d'UUID comme paramètre d'API
- Seules des mentions UUID dans les commentaires (format des noms de fichiers)

## Statistiques globales

- **Fichiers modifiés :** 6 sur 7 contrôleurs
- **Imports supprimés :** 6 imports `java.util.UUID`
- **Paramètres modifiés :** 23 paramètres UUID → Long
- **Méthodes impactées :** 22 méthodes

## Points d'attention

1. **Compatibilité API :** Ces modifications changent les signatures des endpoints REST. Les clients doivent être mis à jour pour utiliser Long au lieu d'UUID.

2. **Tests :** Tous les tests utilisant ces contrôleurs doivent être mis à jour pour passer des Long au lieu d'UUID.

3. **Services sous-jacents :** Il faut s'assurer que les services appelés (UserService, FormationService, etc.) acceptent également des Long en paramètre.

4. **Documentation OpenAPI :** La documentation Swagger sera automatiquement mise à jour grâce aux annotations @Parameter.

## Vérifications effectuées

✅ Suppression de tous les imports `java.util.UUID` dans les contrôleurs
✅ Remplacement de tous les `@PathVariable UUID` par `@PathVariable Long`
✅ Remplacement de tous les `@RequestParam UUID` par `@RequestParam Long`
✅ Aucune référence UUID résiduelle dans les signatures des méthodes des contrôleurs

## Date de modification

27 décembre 2025

## Développeur

Claude Code (Assistant IA)