# Refactoring : Secteur et Région en Tables Séparées

## Contexte
Transformation du système de formations pour utiliser des tables Secteur et Région au lieu de simples chaînes de caractères.

## Modifications Apportées

### 1. Entités Domain
- **Créé** : `Secteur.java` - Entité représentant un secteur d'activité
- **Créé** : `Region.java` - Entité représentant une région géographique
- **Modifié** : `Formation.java` - Utilise maintenant des relations @ManyToOne vers Secteur et Region

### 2. Repositories
- **Créé** : `SecteurRepository.java` - Interface repository pour Secteur
- **Créé** : `RegionRepository.java` - Interface repository pour Region
- **Créé** : `JpaSecteurRepository.java` - Interface JPA pour Secteur
- **Créé** : `JpaRegionRepository.java` - Interface JPA pour Region
- **Créé** : `SecteurRepositoryImpl.java` - Implémentation repository Secteur
- **Créé** : `RegionRepositoryImpl.java` - Implémentation repository Region

### 3. DTOs
- **Créé** : `SecteurDTO.java` - DTO pour Secteur
- **Créé** : `RegionDTO.java` - DTO pour Region
- **Modifié** : `FormationDTO.java` - Utilise maintenant SecteurDTO et RegionDTO
- **Modifié** : `CreateFormationRequest.java` - Utilise secteurId et regionId (Long)
- **Modifié** : `UpdateFormationRequest.java` - Utilise secteurId et regionId (Long)

### 4. Mappers
- **Créé** : `SecteurMapper.java` - Mapper pour Secteur
- **Créé** : `RegionMapper.java` - Mapper pour Region
- **Modifié** : `FormationMapper.java` - Utilise les nouveaux mappers et ignore secteur/region lors du mapping

### 5. Services
- **Modifié** : `FormationService.java` - Ajout de dépendances SecteurRepository et RegionRepository
- **Ajouté** : Méthodes `createFormationWithIds()` et `updateFormationWithIds()` pour gérer les IDs

### 6. Controllers
- **Créé** : `SecteurController.java` - Controller REST pour la gestion des secteurs
- **Créé** : `RegionController.java` - Controller REST pour la gestion des régions
- **Modifié** : `FormationController.java` - Utilise les nouvelles méthodes du service avec IDs

### 7. Projections
- **Modifié** : `FormationProjection.java` - Retourne maintenant des objets Secteur et Region

### 8. Migration Base de Données
- **Créé** : `V008__Create_Secteur_Region_Tables.sql`
  - Création des tables `secteurs` et `regions`
  - Ajout des colonnes `secteur_id` et `region_id` dans `formations`
  - Migration des données existantes
  - Suppression des anciennes colonnes `secteur` et `region` (String)
  - Mise à jour des index

## Données de Test
Les tables Secteur et Region sont pré-remplies avec des données d'exemple :

### Secteurs
- IT (Informatique)
- FIN (Finance)
- RH (Ressources Humaines)
- MKT (Marketing)
- OPE (Opérations)

### Régions
- IDF (Île-de-France)
- PACA (Provence-Alpes-Côte d'Azur)
- ARA (Auvergne-Rhône-Alpes)
- NAQ (Nouvelle-Aquitaine)
- OCC (Occitanie)
- HDF (Hauts-de-France)

## Impacts

### API Changes
- Les endpoints de création/modification de formations utilisent maintenant `secteurId` et `regionId`
- Nouveaux endpoints : `/api/v1/secteurs` et `/api/v1/regions`
- Les réponses JSON incluent maintenant des objets complets Secteur et Region

### Tests à Mettre à Jour
- Tous les tests utilisant des formations doivent être mis à jour
- Tests de controllers : mise à jour des requêtes JSON
- Tests de services : gestion des nouvelles dépendances
- Tests de mappers : nouveaux mappings

## Avantages
1. **Normalisation des données** : Évite la duplication et les incohérences
2. **Intégrité référentielle** : Garantie par les clés étrangères
3. **Évolutivité** : Facilite l'ajout de nouvelles propriétés aux secteurs/régions
4. **Performance** : Requêtes optimisées avec jointures
5. **Maintenance** : Gestion centralisée des secteurs et régions