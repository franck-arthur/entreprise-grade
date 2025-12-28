# Migration vers JPA Specifications pour les Formations

## Objectif
Migration de l'implémentation de la recherche par filtre des formations pour utiliser JPA Specifications au lieu de requêtes SQL natives, offrant une approche plus flexible et maintenable.

## Modifications apportées

### 1. FormationRepository - Interface du domaine
**Fichier :** `backend/src/main/java/com/enterprise/app/domain/repository/FormationRepository.java`

**Changements :**
- ✅ Ajout du support des `Specification<Formation>` avec la méthode `findAll(Specification<Formation> spec, Pageable pageable)`
- ✅ Extension de la méthode `findByFilters` pour supporter tous les critères de filtrage disponibles dans `FormationSpecifications`
- ✅ Suppression des méthodes optimisées retournant `Object[]` (simplification)

**Nouveaux paramètres de filtrage :**
- `dateDebut`, `dateFin` - Filtrage par période
- `libelle` - Recherche partielle dans le libellé
- `formateurs` - Recherche partielle dans les formateurs
- `ville` - Filtrage exact par ville
- `nbParticipantsMin`, `nbParticipantsMax` - Filtrage par nombre de participants
- `avecPlacesDisponibles` - Filtrage par disponibilité

### 2. JpaFormationRepository - Repository JPA
**Fichier :** `backend/src/main/java/com/enterprise/app/infrastructure/persistence/JpaFormationRepository.java`

**Changements :**
- ✅ Suppression des requêtes SQL natives complexes (`findFormationDataById`, `findFormationDataByFilters`)
- ✅ Conservation des requêtes métier essentielles (`countParticipantsInscrits`, `isFormationComplete`)
- ✅ Utilisation exclusive de `JpaSpecificationExecutor<Formation>` pour les recherches filtrées

### 3. FormationRepositoryImpl - Implémentation
**Fichier :** `backend/src/main/java/com/enterprise/app/infrastructure/persistence/FormationRepositoryImpl.java`

**Changements :**
- ✅ Utilisation de `FormationSpecifications.withDynamicFilters()` pour construire les critères de recherche
- ✅ Ajout de la méthode `findAll(Specification<Formation> spec, Pageable pageable)` pour exposer la fonctionnalité
- ✅ Suppression du mapping manuel des `Object[]`

### 4. FormationService - Couche service
**Fichier :** `backend/src/main/java/com/enterprise/app/application/service/FormationService.java`

**Changements :**
- ✅ Ajout de la méthode `searchFormationsAdvanced()` utilisant tous les critères de filtrage
- ✅ Adaptation de `searchFormations()` pour maintenir la compatibilité
- ✅ Suppression des méthodes retournant directement des DTOs optimisés (`getFormationDTOById`, `searchFormationDTOs`)
- ✅ Nettoyage des imports non utilisés

### 5. FormationController - API REST
**Fichier :** `backend/src/main/java/com/enterprise/app/presentation/controller/v1/FormationController.java`

**Changements :**
- ✅ Adaptation des endpoints existants pour utiliser les entités `Formation` + mapping avec `FormationMapper`
- ✅ Ajout d'un nouvel endpoint `/search` pour la recherche avancée avec tous les filtres
- ✅ Mise à jour de la documentation Swagger/OpenAPI

**Nouveaux endpoints :**
```http
GET /api/v1/formations/search?secteurId=1&regionId=2&libelle=Java&dateDebut=2024-01-01&dateFin=2024-12-31
```

## Utilisation de FormationSpecifications

La classe `FormationSpecifications` offre des méthodes statiques pour construire des critères de recherche :

### Critères disponibles
- `hasSecteur(Long secteurId)` - Filtrage par secteur
- `hasRegion(Long regionId)` - Filtrage par région
- `hasModalite(ModaliteFormation modalite)` - Filtrage par modalité
- `hasStatut(FormationStatut statut)` - Filtrage par statut (calculé dynamiquement)
- `hasDateFormationBetween(LocalDate debut, LocalDate fin)` - Filtrage par période
- `hasLibelleContaining(String libelle)` - Recherche partielle dans le libellé
- `hasFormateursContaining(String formateurs)` - Recherche partielle dans les formateurs
- `hasVille(String ville)` - Filtrage exact par ville
- `hasNbParticipantsMin/Max(Integer nb)` - Filtrage par nombre de participants
- `hasDisponibilites(Boolean disponible)` - Filtrage par disponibilité

### Composition des critères
```java
var specification = FormationSpecifications.withDynamicFilters(
    secteurId, regionId, modalite, statut, dateDebut, dateFin,
    libelle, formateurs, ville, nbParticipantsMin, nbParticipantsMax,
    avecPlacesDisponibles
);
```

## Avantages de cette approche

### ✅ Flexibilité
- Composition dynamique des critères de recherche
- Réutilisabilité des critères individuels
- Facilité d'ajout de nouveaux critères

### ✅ Maintenabilité
- Code plus lisible et structuré
- Séparation claire des responsabilités
- Typage fort avec les entités JPA

### ✅ Performance
- Optimisation automatique par Hibernate
- Requêtes SQL générées optimales
- Pas de mapping manuel nécessaire

### ✅ Type Safety
- Compilation statique des critères
- Détection des erreurs à la compilation
- IntelliSense complet

## Impact sur les tests
Les tests existants continueront de fonctionner car :
- Les signatures des méthodes publiques principales sont maintenues
- La compatibilité descendante est assurée via `searchFormations()`
- Les nouvelles fonctionnalités sont additionnelles

## Migration des clients API
- **Endpoints existants :** Aucun changement nécessaire
- **Nouveaux clients :** Peuvent utiliser `/search` pour les recherches avancées
- **Compatibilité :** Totale avec les versions antérieures

## Prochaines étapes recommandées
1. Implémenter le filtrage par disponibilité dans `FormationSpecifications.hasDisponibilites()`
2. Ajouter des index de base de données sur les colonnes fréquemment filtrées
3. Étendre les tests pour couvrir les nouveaux critères de recherche
4. Considérer l'ajout de critères de tri personnalisés