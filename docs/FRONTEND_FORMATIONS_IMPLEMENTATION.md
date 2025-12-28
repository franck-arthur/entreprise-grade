# Implémentation Frontend des Formations

## Vue d'ensemble

Cette documentation décrit l'implémentation du frontend Angular pour la gestion des formations dans l'application enterprise-grade.

## Architecture

### Modèles de données
- `Formation` : Modèle principal représentant une formation
- `CreateFormationRequest` / `UpdateFormationRequest` : DTOs pour les opérations CRUD
- `FormationParticipation` : Modèle pour les participations aux formations
- `FormationFilters` : Interface pour les filtres de recherche
- Enums : `ModaliteFormation`, `FormationStatut`

### Services
- `FormationService` : Service principal pour les appels API formations
  - CRUD complet (Create, Read, Update, Delete)
  - Récupération des participants
  - Filtrage et pagination
  - Gestion des modalités et statuts

### Composants créés

#### 1. FormationsListComponent
- **Localisation** : `src/app/features/formations/pages/formations-list/`
- **Fonctionnalités** :
  - Liste paginée des formations
  - Filtres par modalité et statut
  - Actions : voir détail, modifier, supprimer
  - Design System Français (DSFR)

#### 2. FormationDetailComponent
- **Localisation** : `src/app/features/formations/pages/formation-detail/`
- **Fonctionnalités** :
  - Affichage détaillé d'une formation
  - Informations sur les participants
  - Statistiques des inscriptions
  - Actions d'édition et suppression

## Routing

Le système de routing utilise le lazy loading pour optimiser les performances :

```typescript
// Dans app.routes.ts
{
  path: 'formations',
  loadChildren: () => import('./features/formations/formations.routes').then(m => m.FORMATIONS_ROUTES),
  canActivate: [authGuard],
  data: { roles: ['ADMIN', 'MANAGER', 'TECH_LEAD'] },
  title: 'Formations',
}
```

Routes définies :
- `/formations` : Liste des formations
- `/formations/:id` : Détail d'une formation

## APIs consommées

### Endpoints utilisés
1. `GET /api/v1/formations` - Liste paginée avec filtres
2. `GET /api/v1/formations/{id}` - Détail d'une formation
3. `POST /api/v1/formations` - Création d'une formation
4. `PUT /api/v1/formations/{id}` - Mise à jour d'une formation
5. `DELETE /api/v1/formations/{id}` - Suppression d'une formation
6. `GET /api/v1/formations/{id}/participants` - Liste des participants

### Filtres supportés
- `secteurId` : Filtre par secteur
- `regionId` : Filtre par région
- `modalite` : Filtre par modalité (PRESENTIEL, DISTANCIEL, MIXTE)
- `statut` : Filtre par statut (PLANIFIE, EN_COURS, TERMINE, ANNULE)
- Pagination : `page`, `size`

## Sécurité

### Contrôle d'accès
- Authentification requise via `authGuard`
- Rôles autorisés : ADMIN, MANAGER, TECH_LEAD
- Actions CRUD limitées selon les permissions backend

### Validation
- Validation côté frontend via les formulaires Angular
- Gestion des erreurs HTTP
- Messages d'erreur utilisateur appropriés

## UI/UX

### Design System
- Utilisation du Système de Design de l'État français (DSFR)
- Composants responsive
- Accessibilité intégrée

### Fonctionnalités utilisateur
- Interface intuitive avec breadcrumbs
- États de chargement et d'erreur
- Confirmations pour les actions destructives
- Pagination pour les grandes listes

## Technologies utilisées

- **Angular 17+** : Framework principal
- **Standalone Components** : Architecture moderne Angular
- **RxJS** : Gestion des flux de données
- **DSFR** : Design system français
- **TypeScript** : Typage strict

## Structure des fichiers

```
src/app/
├── core/
│   ├── models/
│   │   └── formation.model.ts
│   └── services/
│       └── formation.service.ts
└── features/
    └── formations/
        ├── formations.routes.ts
        └── pages/
            ├── formations-list/
            │   └── formations-list.component.ts
            └── formation-detail/
                └── formation-detail.component.ts
```

## Points d'amélioration futurs

1. **Composants de création/édition** : Formulaires dédiés pour CRUD
2. **État global** : Intégration NgRx pour la gestion d'état
3. **Tests unitaires** : Couverture complète des composants
4. **Optimisations** : Mise en cache des données
5. **Notifications** : Toast messages pour les actions
6. **Exports** : Fonctionnalités d'export CSV/PDF

## Configuration

### Environment
Assurez-vous que `environment.apiUrl` pointe vers l'API backend correcte.

### Permissions
Les routes formations nécessitent les rôles appropriés configurés dans le système d'authentification.

## Utilisation

1. **Navigation** : Accéder via le menu ou directement à `/formations`
2. **Filtrage** : Utiliser les filtres en haut de la liste
3. **Détail** : Cliquer sur "Voir détails" depuis la liste
4. **Actions** : Modifier/supprimer depuis la liste ou le détail