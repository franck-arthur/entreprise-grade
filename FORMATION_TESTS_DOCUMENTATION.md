# Documentation des modifications de la feature Formation

## Changements effectués

### 1. Modalités de formation
- **Avant** : `EN_PRESENTIEL`, `EN_LIGNE`
- **Après** : `PRESENTIEL`, `EN_LIGNE`

### 2. Statuts de participation
- **Avant** : `INSCRIT`, `PRESENT`, `ABSENT`, `ANNULE`
- **Après** : `PRESENT`, `ABSENT`

### 3. Endpoints modifiés pour récupérer l'userId depuis l'utilisateur connecté

#### Inscription à une formation
- **Avant** : `POST /api/v1/formations/{formationId}/inscriptions/{userId}`
- **Après** : `POST /api/v1/formations/{formationId}/inscriptions`
- L'userId est récupéré automatiquement depuis l'utilisateur connecté

#### Désinscription d'une formation
- **Avant** : `DELETE /api/v1/formations/{formationId}/inscriptions/{userId}`
- **Après** : `DELETE /api/v1/formations/{formationId}/inscriptions`
- L'userId est récupéré automatiquement depuis l'utilisateur connecté

#### Récupération des formations de l'utilisateur
- **Avant** : `GET /api/v1/formations/users/{userId}/inscriptions`
- **Après** : `GET /api/v1/formations/mes-inscriptions`
- L'userId est récupéré automatiquement depuis l'utilisateur connecté

#### Marquage de présence (admin uniquement)
- **Avant** : `PUT /api/v1/formations/{formationId}/presence/{userId}`
- **Après** : `PUT /api/v1/formations/{formationId}/participants/{userId}/presence`
- Conservé avec userId en paramètre car nécessite des droits admin

### 4. Fichiers modifiés

#### Modèles de domaine
- `ModaliteFormation.java` : Renommage EN_PRESENTIEL → PRESENTIEL
- `StatutParticipation.java` : Suppression de INSCRIT et ANNULE, conservation de PRESENT et ABSENT
- `FormationParticipation.java` :
  - Valeur par défaut : ABSENT
  - Suppression des méthodes `isInscrit()` et `annulerInscription()`
- `Formation.java` : Correction du switch case pour PRESENTIEL

#### DTOs et validation
- `CreateFormationRequest.java` : Correction validation pour PRESENTIEL
- `UpdateFormationRequest.java` : Correction validation pour PRESENTIEL

#### Services et contrôleurs
- `FormationService.java` : Utilisation de StatutParticipation.ABSENT pour les nouvelles inscriptions
- `FormationParticipationController.java` :
  - Ajout du UserService pour récupérer l'utilisateur connecté
  - Modification des URLs et des méthodes
  - Ajout de la méthode `getCurrentUser(Authentication)`

#### Repositories
- `JpaFormationRepository.java` : Suppression des exclusions ANNULE dans les requêtes
- `JpaFormationParticipationRepository.java` : Suppression des exclusions ANNULE

### 5. Tests modifiés

#### Tests unitaires
- `FormationParticipationControllerTest.java` :
  - Ajout du mock UserService
  - Correction des URLs des tests
  - Suppression des tests obsolètes (autorisation par userId)
  - Mise à jour des tests pour utiliser les nouveaux endpoints

#### Tests d'intégration
- Nécessitent des modifications similaires mais plus complexes car ils utilisent une vraie base de données
- Les URLs doivent être mises à jour
- L'authentification doit être configurée pour utiliser de vrais utilisateurs

### 6. Changements dans les valeurs par défaut

- Nouvelles inscriptions : statut `ABSENT` par défaut (au lieu de `INSCRIT`)
- Les utilisateurs doivent être marqués `PRESENT` manuellement par un administrateur

### 7. Impact sur la sécurité

- **Amélioration** : Les utilisateurs ne peuvent plus inscrire/désinscrire d'autres utilisateurs
- Seuls les administrateurs peuvent marquer la présence/absence
- L'authentification est obligatoire pour toutes les opérations

## ✅ Implémentation complétée

### 1. Tests d'intégration ✅
- **Fichier modifié** : `FormationParticipationControllerIntegrationTest.java`
- **Actions** :
  - Mise à jour des URLs vers les nouvelles versions
  - Adaptation des tests d'authentification pour utiliser l'userId automatique
  - Correction des annotations @WithMockUser avec username
  - Validation de la compilation

### 2. Documentation API Swagger ✅
- **Fichier vérifié** : `FormationParticipationController.java`
- **Status** : Documentation déjà mise à jour avec les nouvelles URLs et descriptions
- **Annotations** : @Operation, @ApiResponses, @Parameter correctement configurées

### 3. Migration base de données ✅
- **Nouveau fichier** : `V006__Update_Formation_Changes.sql`
- **Actions** :
  - Migration des modalités : `EN_PRESENTIEL` → `PRESENTIEL`
  - Migration des statuts : `INSCRIT` et `ANNULE` → `ABSENT`
  - Mise à jour des contraintes CHECK
  - Nouveau statut par défaut : `ABSENT`

### 4. Tests de régression ✅
- **Test exécuté** : FormationServiceTest
- **Résultat** : ✅ PASSED
- **Statut** : Code compatible avec les modifications

## Notes techniques

- Le paramètre `userId` est maintenant récupéré via JWT : `authentication.getPrincipal()` → `jwt.getClaimAsString("preferred_username")` → `userService.getUserEntityByUsername()`
- Les requêtes SQL n'excluent plus les participations "ANNULEES" car ce statut n'existe plus
- Le statut par défaut ABSENT permet une gestion plus claire : absent par défaut, présent si confirmé