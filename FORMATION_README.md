# Module de Gestion des Formations

## 🎯 Aperçu
Ce module permet la gestion complète des formations en entreprise : création, modification, inscription des utilisateurs et suivi de la présence.

## 🚀 Démarrage rapide

### 1. Migration de base de données
```bash
# Exécuter la migration V004
# Les tables formations et formation_participations seront créées automatiquement
```

### 2. Exemples d'API

#### Récupérer toutes les formations
```bash
curl -H "Authorization: Bearer <token>" \
     "http://localhost:8080/api/v1/formations?secteur=IT&statut=A_VENIR"
```

#### Créer une formation (ADMIN/MANAGER)
```bash
curl -X POST \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <token>" \
     -d '{
       "libelle": "Formation Docker & Kubernetes",
       "formateurs": "Expert DevOps",
       "description": "Formation pratique containerisation",
       "dateFormation": "2024-03-15",
       "heureDebut": "09:00",
       "heureFin": "17:00",
       "secteur": "IT",
       "region": "Île-de-France",
       "modalite": "HYBRIDE",
       "nbParticipants": 16,
       "lieu": "Campus Paris",
       "ville": "Paris",
       "lienParticipation": "https://zoom.us/j/987654321"
     }' \
     "http://localhost:8080/api/v1/formations"
```

#### S'inscrire à une formation
```bash
curl -X POST \
     -H "Authorization: Bearer <token>" \
     "http://localhost:8080/api/v1/formations/{formationId}/inscriptions/{userId}"
```

#### Marquer la présence (ADMIN/MANAGER/TECH_LEAD)
```bash
curl -X PUT \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <token>" \
     -d '{
       "present": true,
       "commentaire": "Participant très actif"
     }' \
     "http://localhost:8080/api/v1/formations/{formationId}/presence/{userId}"
```

## 📋 Fonctionnalités

### ✅ Gestion des formations
- ✅ Créer, modifier, supprimer une formation
- ✅ Recherche avec filtres (secteur, région, modalité, date, statut)
- ✅ Calcul automatique du statut (à venir, en cours, terminée)
- ✅ Contrôle du nombre de participants

### ✅ Inscriptions
- ✅ Inscription/désinscription utilisateur
- ✅ Vérification de disponibilité (places restantes)
- ✅ Protection contre les doublons d'inscription
- ✅ Restriction selon le statut de la formation

### ✅ Suivi de présence
- ✅ Marquage présent/absent
- ✅ Mise à jour automatique de `date_derniere_formation`
- ✅ Historique des participations

## 🔐 Sécurité et rôles

| Rôle | Permissions |
|------|-------------|
| **USER** | Voir formations, s'inscrire/désinscrire |
| **TECH_LEAD** | + Marquer présence, voir participants |
| **MANAGER** | + Créer/modifier formations |
| **ADMIN** | Accès complet |

## 📊 Statuts des formations

| Statut | Condition |
|--------|-----------|
| **A_VENIR** | Date de début > maintenant |
| **EN_COURS** | Date début ≤ maintenant < date fin |
| **TERMINEE** | Date de fin < maintenant |

## 🛠️ Règles métier implémentées

### Inscriptions
- ❌ **Formation terminée** → Inscription impossible
- ❌ **Formation complète** → Inscription impossible
- ❌ **Déjà inscrit** → Erreur de duplication
- ❌ **Formation commencée** → Désinscription impossible

### Présence/Absence
- ✅ **Présent** → `date_derniere_formation` mise à jour
- ❌ **Absent** → `date_derniere_formation` remise à null

### Modifications
- ❌ **Formation terminée** → Modification impossible
- ❌ **Formation en cours** → Suppression impossible

## 🗄️ Structure de données

### Formation
```json
{
  "id": "uuid",
  "libelle": "Formation Spring Boot",
  "formateurs": "Jean Dupont, Marie Martin",
  "description": "...",
  "dateFormation": "2024-02-15",
  "heureDebut": "09:00",
  "heureFin": "17:00",
  "secteur": "IT",
  "region": "Île-de-France",
  "modalite": "HYBRIDE",
  "nbParticipants": 20,
  "lieu": "Campus Paris",
  "ville": "Paris",
  "lienParticipation": "https://zoom.us/j/...",
  "statut": "A_VENIR",
  "nbParticipantsInscrits": 15,
  "complet": false
}
```

## 🧪 Tests

```bash
# Exécuter les tests
mvn test -Dtest=FormationServiceTest
mvn test -Dtest=FormationTest
```

## 📈 Optimisations de performance

### Index créés
- `formations(date_formation)` - Recherche par date
- `formations(secteur, region)` - Filtrage géographique
- `formation_participations(formation_id, statut_participation)` - Comptage participants

### Pagination
- Toutes les listes sont paginées (défaut: 20 éléments)
- Recherche optimisée avec filtres

## 🔮 Évolutions possibles

- 📧 **Notifications** par email/SMS
- ⭐ **Évaluations** post-formation
- 📜 **Certificats** automatiques
- 📅 **Gestion conflits** de planning
- 🎯 **Liste d'attente** formations complètes
- 🔄 **Formations récurrentes**
- 🎥 **Intégration visio** (Teams, Zoom)

## 💡 Exemples d'usage

### Cas d'usage typique : Formation interne
1. Manager crée formation "Sécurité IT"
2. Employés s'inscrivent via l'API
3. Jour J : Tech Lead marque présences
4. `date_derniere_formation` mise à jour automatiquement

### Recherche avancée
```bash
# Formations IT à venir en Île-de-France
GET /api/v1/formations?secteur=IT&region=Île-de-France&statut=A_VENIR

# Formations hybrides avec places disponibles
GET /api/v1/formations?modalite=HYBRIDE&complet=false
```

---

📞 **Support** : Pour toute question, consulter `FORMATION_DOCUMENTATION_TECHNIQUE.md`