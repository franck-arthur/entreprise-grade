# Plan de Charge Tech Lead - Application Enterprise-Grade

## Vue d'ensemble

Ce document décrit le plan de charge initial pour le démarrage du projet, la répartition des tâches par compétences, et les templates pour le rôle de Tech Lead.

---

## Équipe Projet (6 personnes)

### Composition

| Rôle | Nom | Niveau | Spécialité |
|------|-----|--------|-----------|
| Tech Lead | Vous | Senior | Full-Stack, Architecture |
| Backend Senior | Dev 1 | Senior | Spring Boot, Microservices |
| Frontend Senior | Dev 2 | Senior | Angular, NgRx |
| Full-Stack Intermédiaire | Dev 3 | Intermédiaire | Java + Angular |
| Full-Stack Intermédiaire | Dev 4 | Intermédiaire | Spring + TypeScript |
| Frontend Junior | Dev 5 | Junior | Angular, DSFR |

---

## Sprint 0 : Mise en place (2 semaines)

### Objectifs

- ✅ Architecture technique définie
- ✅ Environnements de développement opérationnels
- ✅ Pipeline CI/CD fonctionnelle
- ✅ Socle technique validé (backend + frontend)

### Répartition des Tâches

#### Backend (3 personnes : Tech Lead + Senior Backend + Intermédiaire 1)

| Tâche | Responsable | Durée | Complexité |
|-------|-------------|-------|-----------|
| Configuration Spring Boot (Security, Keycloak) | Senior Backend | 3j | Haute |
| Architecture hexagonale (Domain, Application) | Senior Backend + Tech Lead | 2j | Haute |
| Repository JPA + migrations Flyway | Intermédiaire 1 | 2j | Moyenne |
| API REST + OpenAPI | Tech Lead | 2j | Moyenne |
| Integration Elasticsearch | Senior Backend | 2j | Moyenne |
| Tests unitaires (JUnit + Mockito) | Tous | 1j | Basse |
| Tests d'intégration (Testcontainers) | Senior Backend | 2j | Moyenne |

**Charge totale backend:** 14 jours-homme

#### Frontend (3 personnes : Senior Frontend + Intermédiaire 2 + Junior)

| Tâche | Responsable | Durée | Complexité |
|-------|-------------|-------|-----------|
| Configuration Angular 17+ (standalone) | Senior Frontend | 1j | Moyenne |
| NgRx Store (setup, root state) | Senior Frontend | 2j | Haute |
| Routing + Guards (auth, roles) | Senior Frontend | 1j | Moyenne |
| Services HTTP + Interceptors | Intermédiaire 2 | 2j | Moyenne |
| Composants DSFR de base | Intermédiaire 2 + Junior | 3j | Basse |
| Pages Auth (login, unauthorized) | Junior | 2j | Basse |
| Dashboard page | Intermédiaire 2 | 1j | Basse |
| Tests unitaires Jest | Tous | 1j | Basse |

**Charge totale frontend:** 13 jours-homme

#### DevOps & Infrastructure (Tech Lead)

| Tâche | Durée | Complexité |
|-------|-------|-----------|
| Docker Compose (tous services) | 1j | Moyenne |
| Dockerfiles (backend + frontend) | 0.5j | Basse |
| GitLab CI/CD pipeline | 2j | Haute |
| Manifests Kubernetes (dev) | 1j | Moyenne |
| Configuration Terraform (base) | 1j | Moyenne |

**Charge totale DevOps:** 5.5 jours-homme

#### Documentation (Tous)

| Tâche | Responsable | Durée |
|-------|-------------|-------|
| Architecture C4 (diagrammes) | Tech Lead | 0.5j |
| README principal | Tech Lead | 0.5j |
| Documentation API (Swagger) | Senior Backend | 0.5j |
| Guide développeur frontend | Senior Frontend | 0.5j |

**Charge totale documentation:** 2 jours-homme

### Total Sprint 0: ~34.5 jours-homme (≈ 6 personnes × 2 semaines)

---

## Matrice de Responsabilités (RACI)

| Activité | Tech Lead | Backend Senior | Frontend Senior | Intermédiaire | Junior |
|----------|-----------|----------------|-----------------|---------------|--------|
| **Architecture globale** | **R/A** | C | C | I | I |
| **Spring Security + Keycloak** | C | **R/A** | I | C | I |
| **Architecture hexagonale** | **R/A** | **R** | I | C | I |
| **API REST + OpenAPI** | **R/A** | C | I | C | I |
| **NgRx Store** | C | I | **R/A** | C | C |
| **DSFR Components** | I | I | **R/A** | **R** | **R** |
| **Tests unitaires** | **A** | **R** | **R** | **R** | **R** |
| **GitLab CI/CD** | **R/A** | C | C | I | I |
| **Kubernetes** | **R/A** | C | C | I | I |
| **Documentation technique** | **A** | **R** | **R** | **R** | C |
| **Code review** | **R/A** | **R** | **R** | C | I |
| **Réunions quotidiennes** | **R/A** | **R** | **R** | **R** | **R** |

**Légende:**
- **R**: Responsible (Réalise la tâche)
- **A**: Accountable (Responsable final)
- **C**: Consulted (Consulté pour avis)
- **I**: Informed (Informé des résultats)

---

## Templates Tech Lead

### Template : Revue de Code

```markdown
# Checklist Revue de Code

**PR #:** [Numéro]
**Auteur:** [Nom]
**Reviewer:** [Nom]
**Date:** [Date]

## ✅ Qualité du Code

- [ ] Respect des conventions (Checkstyle, ESLint)
- [ ] Pas de code dupliqué
- [ ] Complexité cyclomatique acceptable (< 10)
- [ ] Nommage clair et explicite
- [ ] Commentaires pertinents (JSDoc/Javadoc)

## ✅ Tests

- [ ] Couverture > 80%
- [ ] Tests unitaires pertinents
- [ ] Tests d'intégration si nécessaire
- [ ] Pas de tests désactivés

## ✅ Sécurité

- [ ] Pas de secrets hardcodés
- [ ] Validation des inputs (Bean Validation, Joi)
- [ ] Gestion des erreurs appropriée
- [ ] Pas de vulnérabilités OWASP

## ✅ Performance

- [ ] Pas de N+1 queries
- [ ] Utilisation du cache si pertinent
- [ ] Optimisation des requêtes DB
- [ ] Pagination pour les listes

## ✅ Architecture

- [ ] Respect de l'architecture hexagonale
- [ ] Séparation des responsabilités (SRP)
- [ ] Dépendances correctes entre couches
- [ ] Pas de couplage fort

## ✅ Documentation

- [ ] Javadoc/JSDoc complet
- [ ] README à jour si besoin
- [ ] Swagger/OpenAPI à jour
- [ ] Changelog mis à jour

## 📝 Commentaires

[Vos commentaires ici]

## 🚀 Décision

- [ ] ✅ Approuvé
- [ ] 🔄 Modifications requises
- [ ] ❌ Rejeté

**Signature:** [Nom] - [Date]
```

---

### Template : Devis Technique

```markdown
# Devis Technique - [Nom de la Fonctionnalité]

**Demandeur:** [Nom]
**Rédigé par:** [Tech Lead]
**Date:** [Date]
**Version:** 1.0

---

## 📋 Résumé Exécutif

[Description courte de la fonctionnalité en 2-3 phrases]

---

## 🎯 Objectifs

### Objectifs Fonctionnels

- Objectif 1
- Objectif 2
- Objectif 3

### Objectifs Techniques

- Objectif technique 1
- Objectif technique 2

---

## 🔍 Analyse Fonctionnelle

### Cas d'Usage

**UC-001: [Nom du cas d'usage]**

- **Acteur:** [Rôle]
- **Préconditions:** [Liste]
- **Flux nominal:**
  1. Étape 1
  2. Étape 2
  3. Étape 3
- **Flux alternatif:** [Décrire]
- **Postconditions:** [Résultat attendu]

---

## 🏗️ Conception Technique

### Architecture Impactée

- **Couche Domain:** [Modifications]
- **Couche Application:** [Modifications]
- **Couche Infrastructure:** [Modifications]
- **Couche Présentation:** [Modifications]

### Composants à Créer/Modifier

| Composant | Type | Action | Complexité |
|-----------|------|--------|-----------|
| UserEntity | Domain | Modifier | Basse |
| CreateUserUseCase | Application | Créer | Moyenne |
| UserController | Presentation | Modifier | Basse |

### Dépendances Externes

- Dépendance 1: [Description]
- Dépendance 2: [Description]

### Base de Données

**Nouvelles Tables:**

```sql
CREATE TABLE new_table (
  id UUID PRIMARY KEY,
  field1 VARCHAR(100),
  created_at TIMESTAMP
);
```

**Migrations:**

- V2__add_new_table.sql

---

## 📊 Estimation (Jours-Homme)

| Tâche | Backend | Frontend | Tests | Documentation | Total |
|-------|---------|----------|-------|---------------|-------|
| Analyse détaillée | 0.5 | 0.5 | - | 0.5 | 1.5 |
| Modèle de données | 1 | - | - | - | 1 |
| API REST | 2 | - | 1 | 0.5 | 3.5 |
| Services métier | 3 | - | 1 | 0.5 | 4.5 |
| Composants UI | - | 3 | 1 | - | 4 |
| NgRx Store | - | 2 | 1 | - | 3 |
| Intégration E2E | 1 | 1 | 1 | - | 3 |
| **TOTAL** | **7.5** | **6.5** | **5** | **1.5** | **20.5** |

**Marge de sécurité (20%):** +4 jours

**Total avec marge:** **24.5 jours-homme**

**Estimation calendaire:**
- Avec 2 développeurs full-time : **~2.5 semaines**
- Avec toute l'équipe : **~1 semaine**

---

## ⚠️ Risques & Mitigations

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|-----------|
| Complexité Keycloak sous-estimée | Moyenne | Élevé | Prévoir 1j supplémentaire pour R&D |
| Performance Elasticsearch | Faible | Moyen | Tests de charge dès le début |
| Changement de spec | Moyenne | Élevé | Validation fréquente avec PO |

---

## 🚦 Dépendances & Prérequis

- [ ] Keycloak configuré et opérationnel
- [ ] Base de données accessible
- [ ] Environnement de développement prêt
- [ ] Maquettes UI validées

---

## 📅 Planning Proposé

**Sprint 1 (Semaine 1-2):**
- Backend: Modèle + API
- Frontend: Maquettes + composants de base

**Sprint 2 (Semaine 3):**
- Backend: Services métier + tests
- Frontend: NgRx + intégration API

**Sprint 3 (Semaine 4):**
- Tests E2E
- Documentation
- Déploiement en staging

---

## ✅ Critères d'Acceptation

- [ ] L'utilisateur peut [action]
- [ ] Les données sont validées correctement
- [ ] La page se charge en moins de 2 secondes
- [ ] Couverture de code > 80%
- [ ] Documentation à jour
- [ ] Tests E2E passent
- [ ] Pas de régression détectée

---

## 💡 Recommandations Tech Lead

1. **Recommandation 1:** [Détails]
2. **Recommandation 2:** [Détails]
3. **Alternative proposée:** [Si applicable]

---

## 📝 Validation

- [ ] **Product Owner:** [Nom] - [Date]
- [ ] **Tech Lead:** [Nom] - [Date]
- [ ] **CTO/Directeur Technique:** [Nom] - [Date]

---

**Signatures:**

Product Owner: _______________ Date: _______________

Tech Lead: _______________ Date: _______________
```

---

### Template : Daily Standup

```markdown
# Daily Standup - [Date]

## ⏰ Informations

- **Date:** [JJ/MM/AAAA]
- **Heure:** 9h00 - 9h15
- **Participants:** [Liste]

---

## 🔄 Tour de Table (3 questions par personne)

### [Nom Dev 1]

1. **Hier:** [Tâche complétée]
2. **Aujourd'hui:** [Tâche prévue]
3. **Blocages:** [Aucun / Décrire]

### [Nom Dev 2]

1. **Hier:** [Tâche complétée]
2. **Aujourd'hui:** [Tâche prévue]
3. **Blocages:** [Aucun / Décrire]

### [Nom Dev 3]

1. **Hier:** [Tâche complétée]
2. **Aujourd'hui:** [Tâche prévue]
3. **Blocages:** [Aucun / Décrire]

---

## 🚧 Blocages Identifiés

| Blocage | Responsable | Action | Échéance |
|---------|-------------|--------|----------|
| Accès base de données de test | Tech Lead | Créer compte | Aujourd'hui |
| Doute sur spec UX | Frontend Senior | Contacter PO | Demain |

---

## 📊 Métriques Sprint

- **Vélocité actuelle:** [X] points
- **Burndown:** [Statut]
- **Objectif sprint:** [X] points

---

## 📅 Prochaine Réunion

- **Date:** [JJ/MM/AAAA]
- **Heure:** 9h00

---

**Notes additionnelles:** [Si nécessaire]
```

---

## Bonnes Pratiques Tech Lead

### Communication

- ✅ Daily standup quotidien (max 15 min)
- ✅ Code review systématique (< 24h)
- ✅ Feedback constructif et bienveillant
- ✅ Transparence sur les décisions techniques
- ✅ Documentation à jour

### Management Technique

- ✅ Définir les conventions de code (Checkstyle, ESLint)
- ✅ Automatiser les contrôles qualité (CI/CD)
- ✅ Privilégier les tests automatisés
- ✅ Faire de la veille technologique
- ✅ Partager les connaissances (tech talks)

### Architecture & Qualité

- ✅ Respecter SOLID et Clean Code
- ✅ Privilégier la simplicité (KISS)
- ✅ Éviter la sur-ingénierie (YAGNI)
- ✅ Refactoring continu
- ✅ Dette technique maîtrisée

### Développement d'Équipe

- ✅ Pair programming pour les sujets complexes
- ✅ Mentoring des juniors
- ✅ Montée en compétence progressive
- ✅ Valoriser les succès
- ✅ Apprendre des erreurs

---

## Ressources Utiles

### Documentation

- [Architecture C4](./ARCHITECTURE.md)
- [README Principal](../README.md)
- [Backend README](../backend/README.md)
- [Frontend README](../frontend/README.md)

### Outils

- **GitLab**: https://gitlab.com/project/entreprise-grade
- **SonarQube**: http://sonarqube:9000
- **Keycloak**: http://localhost:8180
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### Communauté

- **Slack**: #enterprise-app
- **Confluence**: https://confluence.enterprise.com/app
- **Jira**: https://jira.enterprise.com/app

---

**Auteur:** Tech Lead
**Dernière mise à jour:** [Date]
**Version:** 1.0
