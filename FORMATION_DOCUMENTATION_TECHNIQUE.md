# Documentation Technique - Module de Gestion des Formations

## Vue d'ensemble

Le module de gestion des formations permet de créer, modifier, supprimer des formations, et de gérer les inscriptions des utilisateurs avec suivi de la présence/absence.

## Architecture

### Modèle de données

#### Entités principales

##### 1. Formation (formations)
```java
@Entity
@Table(name = "formations")
public class Formation {
    private UUID id;
    private String libelle;
    private String formateurs;
    private String description;
    private LocalDate dateFormation;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String secteur;
    private String region;
    private ModaliteFormation modalite;
    private Integer nbParticipants;
    private String lieu;
    private String ville;
    private String lienParticipation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
```

**Index optimisés :**
- `idx_formation_date` sur `date_formation`
- `idx_formation_secteur` sur `secteur`
- `idx_formation_region` sur `region`
- `idx_formation_modalite` sur `modalite`
- `idx_formation_date_secteur` sur `date_formation,secteur`
- `idx_formation_date_region` sur `date_formation,region`

##### 2. FormationParticipation (formation_participations)
```java
@Entity
@Table(name = "formation_participations")
public class FormationParticipation {
    private UUID id;
    private Formation formation;
    private User user;
    private StatutParticipation statutParticipation;
    private LocalDateTime dateInscription;
    private LocalDateTime datePresence;
    private String commentaire;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
```

**Index optimisés :**
- `idx_formation_participation_formation` sur `formation_id`
- `idx_formation_participation_user` sur `user_id`
- `idx_formation_participation_statut` sur `statut_participation`
- `idx_formation_participation_formation_statut` sur `formation_id,statut_participation`
- `uk_formation_user` contrainte unique sur `(formation_id, user_id)`

##### 3. User (mise à jour)
Ajout du champ `date_derniere_formation` de type `LocalDateTime` pour traquer la dernière formation suivie.

#### Énumérations

##### ModaliteFormation
- `PRESENTIEL`
- `DISTANCIEL`
- `HYBRIDE`

##### FormationStatut
- `A_VENIR` : date de début > date système
- `EN_COURS` : date de début <= date système < date de fin
- `TERMINEE` : date de fin < date système

##### StatutParticipation
- `INSCRIT` : inscription confirmée
- `PRESENT` : présence marquée
- `ABSENT` : absence marquée
- `ANNULE` : inscription annulée

## Services métier

### FormationService

#### Méthodes principales

##### Gestion des formations
- `createFormation(Formation)` - Création d'une formation
- `updateFormation(UUID, Formation)` - Modification (interdit si terminée)
- `deleteFormation(UUID)` - Suppression (interdit si en cours)
- `getFormationById(UUID)` - Récupération par ID
- `searchFormations(...)` - Recherche avec filtres

##### Gestion des inscriptions
- `inscrireUtilisateur(UUID formationId, UUID userId)` - Inscription
- `desinscrireUtilisateur(UUID formationId, UUID userId)` - Désinscription
- `marquerPresence(UUID formationId, UUID userId, boolean present)` - Gestion présence

#### Règles métier

##### Inscriptions
1. Vérification du nombre de participants maximum avant inscription
2. Inscription impossible si formation complète ou terminée
3. Un utilisateur ne peut s'inscrire qu'une fois à une formation
4. Désinscription impossible si formation commencée/terminée

##### Présence/Absence
1. **Présence marquée** :
   - Statut → `PRESENT`
   - `datePresence` → date système
   - `user.dateDerniereFormation` → date système

2. **Absence marquée** :
   - Statut → `ABSENT`
   - `datePresence` → null
   - `user.dateDerniereFormation` → null

## API REST

### Endpoints formations (/api/v1/formations)

#### GET /api/v1/formations
Récupération des formations avec filtres optionnels
- **Paramètres** : secteur, region, modalite, dateDebut, dateFin, statut, search
- **Autorisation** : Tous les utilisateurs authentifiés
- **Réponse** : `Page<FormationDTO>`

#### GET /api/v1/formations/{id}
Récupération d'une formation par ID
- **Autorisation** : Tous les utilisateurs authentifiés
- **Réponse** : `FormationDTO`

#### POST /api/v1/formations
Création d'une formation
- **Autorisation** : `ADMIN`, `MANAGER`
- **Body** : `CreateFormationRequest`
- **Réponse** : `FormationDTO` (201)

#### PUT /api/v1/formations/{id}
Modification d'une formation
- **Autorisation** : `ADMIN`, `MANAGER`
- **Body** : `UpdateFormationRequest`
- **Réponse** : `FormationDTO`

#### DELETE /api/v1/formations/{id}
Suppression d'une formation
- **Autorisation** : `ADMIN`, `MANAGER`
- **Réponse** : 204

#### GET /api/v1/formations/{id}/participants
Liste des participants d'une formation
- **Autorisation** : `ADMIN`, `MANAGER`, `TECH_LEAD`
- **Réponse** : `List<FormationParticipationDTO>`

### Endpoints inscriptions (/api/v1/formations)

#### POST /api/v1/formations/{formationId}/inscriptions/{userId}
Inscription d'un utilisateur
- **Autorisation** : `ADMIN`, `MANAGER`, `TECH_LEAD` ou utilisateur lui-même
- **Réponse** : `FormationParticipationDTO` (201)

#### DELETE /api/v1/formations/{formationId}/inscriptions/{userId}
Désinscription d'un utilisateur
- **Autorisation** : `ADMIN`, `MANAGER`, `TECH_LEAD` ou utilisateur lui-même
- **Réponse** : 204

#### GET /api/v1/formations/users/{userId}/inscriptions
Formations d'un utilisateur
- **Autorisation** : `ADMIN`, `MANAGER`, `TECH_LEAD` ou utilisateur lui-même
- **Réponse** : `Page<FormationParticipationDTO>`

#### PUT /api/v1/formations/{formationId}/presence/{userId}
Marquage présence/absence
- **Autorisation** : `ADMIN`, `MANAGER`, `TECH_LEAD`
- **Body** : `PresenceRequest`
- **Réponse** : `FormationParticipationDTO`

## Gestion des erreurs

### Exceptions métier
- `ResourceNotFoundException` : Ressource non trouvée
- `DuplicateResourceException` : Ressource déjà existante
- `BusinessException` : Violation de règle métier

### Codes d'erreur HTTP
- **400** : Données invalides, règle métier violée
- **401** : Non authentifié
- **403** : Non autorisé
- **404** : Ressource non trouvée
- **409** : Conflit (ex: utilisateur déjà inscrit)

## Optimisations performances

### Index de base de données
Les index ont été optimisés pour les requêtes fréquentes :
- Recherche par date de formation
- Filtrage par secteur/région
- Comptage des participants par formation
- Recherche des formations d'un utilisateur

### Requêtes optimisées
- Utilisation de requêtes natives pour le comptage des participants
- Index composites pour les filtres combinés
- Pagination systématique pour les listes

## Sécurité

### Contrôle d'accès (RBAC)
- **ADMIN** : Accès complet
- **MANAGER** : Gestion des formations, visualisation participants
- **TECH_LEAD** : Gestion présence, visualisation participants
- **USER** : Inscription/désinscription personnelle, visualisation formations

### Validation des données
- Validation Jakarta (Bean Validation) sur tous les DTOs
- Validation métier dans les services
- Protection contre les injections SQL (requêtes paramétrées)

## Tests

### Tests unitaires
- `FormationServiceTest` : Tests du service métier
- `FormationTest` : Tests des règles métier de l'entité

### Couverture
- Tests des règles métier principales
- Tests des cas d'erreur
- Tests de validation des données

## Déploiement

### Base de données
Ajout des nouvelles tables et index via migration Liquibase :
```sql
-- Création table formations
CREATE TABLE formations (...);

-- Création table formation_participations
CREATE TABLE formation_participations (...);

-- Ajout colonne User
ALTER TABLE users ADD COLUMN date_derniere_formation TIMESTAMP;

-- Création des index optimisés
CREATE INDEX idx_formation_date ON formations(date_formation);
-- ... autres index
```

### Configuration
Aucune configuration supplémentaire requise. Le module utilise l'infrastructure Spring existante.

## Surveillance et métriques

### Logs applicatifs
- Inscription/désinscription utilisateurs
- Création/modification formations
- Marquage présence/absence

### Métriques recommandées
- Nombre d'inscriptions par formation
- Taux de présence par formation
- Nombre de formations par secteur/région

## Évolutions futures possibles

1. **Notifications** : Email/SMS pour inscriptions, rappels
2. **Évaluations** : Note et commentaires post-formation
3. **Certificats** : Génération automatique de certificats
4. **Planning** : Gestion des conflits de planning
5. **Waiting list** : Liste d'attente pour formations complètes
6. **Récurrence** : Formations récurrentes
7. **Salle virtuelle** : Intégration avec outils de visioconférence

## Annexes

### Exemple de payload JSON

#### CreateFormationRequest
```json
{
  "libelle": "Formation Spring Boot",
  "formateurs": "Jean Dupont, Marie Martin",
  "description": "Formation pratique sur Spring Boot et microservices",
  "dateFormation": "2024-02-15",
  "heureDebut": "09:00",
  "heureFin": "17:00",
  "secteur": "IT",
  "region": "Île-de-France",
  "modalite": "HYBRIDE",
  "nbParticipants": 20,
  "lieu": "Centre de formation Paris",
  "ville": "Paris",
  "lienParticipation": "https://zoom.us/j/123456789"
}
```

#### FormationDTO (réponse)
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "libelle": "Formation Spring Boot",
  "formateurs": "Jean Dupont, Marie Martin",
  "description": "Formation pratique sur Spring Boot",
  "dateFormation": "2024-02-15",
  "heureDebut": "09:00",
  "heureFin": "17:00",
  "secteur": "IT",
  "region": "Île-de-France",
  "modalite": "HYBRIDE",
  "nbParticipants": 20,
  "lieu": "Centre de formation Paris",
  "ville": "Paris",
  "lienParticipation": "https://zoom.us/j/123456789",
  "statut": "A_VENIR",
  "nbParticipantsInscrits": 15,
  "complet": false,
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```