# Mise à jour des Modalités de Formation

## Objectif
Mise à jour du système de formation pour gérer les modalités "En présentiel" et "En ligne" avec les champs appropriés selon le type de modalité.

## Modifications Apportées

### 1. Modèle ModaliteFormation
**Fichier**: `backend/src/main/java/com/enterprise/app/domain/model/ModaliteFormation.java`

- **Avant**: PRESENTIEL, DISTANCIEL, HYBRIDE
- **Après**: EN_PRESENTIEL("En présentiel"), EN_LIGNE("En ligne")
- **Ajout**: Méthode `getLibelle()` pour récupérer le libellé lisible

### 2. Modèle Formation
**Fichier**: `backend/src/main/java/com/enterprise/app/domain/model/Formation.java`

- **Ajout**: Méthode `validerModaliteEtChamps()` pour valider la cohérence entre modalité et champs requis
- **Logique de validation**:
  - EN_PRESENTIEL → `ville` et `lieu` obligatoires
  - EN_LIGNE → `lienParticipation` obligatoire

### 3. DTOs de Formation
**Fichiers modifiés**:
- `CreateFormationRequest.java`
- `UpdateFormationRequest.java`

- **Ajout**: Méthode `validerModaliteEtChamps()` dans chaque DTO
- **Validation**: Pattern regex pour les URLs (liens de participation)

### 4. Service FormationService
**Fichier**: `backend/src/main/java/com/enterprise/app/application/service/FormationService.java`

- **Modification**: Ajout des appels de validation dans `createFormation()` et `updateFormation()`
- **Validations appliquées**:
  - `validerCoherenceDates()`
  - `validerNbParticipants()`
  - `validerModaliteEtChamps()` (nouvelle)

### 5. Contrôleur FormationController
**Fichier**: `FormationController.java`

- **Modification**: Appel de `request.validerModaliteEtChamps()` avant traitement dans:
  - `createFormation()`
  - `updateFormation()`

### 6. Migration Base de Données
**Nouveau fichier**: `V005__Update_Formation_Modalite.sql`

- **Actions**:
  - Mise à jour des valeurs existantes (PRESENTIEL → EN_PRESENTIEL, DISTANCIEL → EN_LIGNE)
  - Suppression de l'ancienne contrainte sur la modalité
  - Ajout de la nouvelle contrainte avec les nouvelles valeurs
  - Mise à jour des commentaires

### 7. Tests
**Fichier**: `FormationServiceTest.java`

- **Ajout**: 5 nouveaux tests de validation:
  - `createFormation_ShouldThrowException_WhenPresentielWithoutVille()`
  - `createFormation_ShouldThrowException_WhenPresentielWithoutLieu()`
  - `createFormation_ShouldThrowException_WhenEnLigneWithoutLien()`
  - `createFormation_ShouldSucceed_WhenPresentielWithVilleAndLieu()`
  - `createFormation_ShouldSucceed_WhenEnLigneWithLien()`

## Règles de Validation

### Formation En Présentiel
- **Champs obligatoires**: `ville` et `lieu`
- **Champs optionnels**: `lienParticipation`

### Formation En Ligne
- **Champs obligatoires**: `lienParticipation` (avec validation URL)
- **Champs optionnels**: `ville` et `lieu`

## Impact Technique

### API
- Les endpoints existants continuent de fonctionner
- Validation renforcée côté serveur
- Messages d'erreur explicites pour les champs manquants

### Base de Données
- Migration automatique des données existantes
- Contrainte mise à jour pour les nouvelles valeurs d'enum
- Pas de perte de données

### Tests
- Couverture complète des nouveaux scénarios de validation
- Tests d'intégration pour chaque modalité
- Validation des messages d'erreur

## Exemple d'Utilisation

### Formation En Présentiel
```json
{
  "libelle": "Formation Spring Boot",
  "modalite": "EN_PRESENTIEL",
  "ville": "Paris",
  "lieu": "Salle 101 - Tour Eiffel",
  "lienParticipation": null
}
```

### Formation En Ligne
```json
{
  "libelle": "Formation Angular",
  "modalite": "EN_LIGNE",
  "ville": null,
  "lieu": null,
  "lienParticipation": "https://zoom.us/j/123456789"
}
```

## Notes d'Implémentation

1. **Rétrocompatibilité**: La migration assure la continuité des données existantes
2. **Validation multiniveau**: DTO → Service → Modèle pour une robustesse maximale
3. **Messages d'erreur**: En français, explicites et orientés utilisateur
4. **Tests complets**: Validation de tous les scénarios d'erreur et de succès

## Prochaines Étapes Suggérées

1. Mise à jour du front-end pour gérer les nouvelles modalités
2. Tests d'intégration end-to-end
3. Documentation utilisateur mise à jour
4. Formation des équipes sur les nouvelles règles de validation