# Architecture C4 - Application Enterprise-Grade

## Vue d'ensemble

Ce document décrit l'architecture complète de l'application selon le modèle C4 (Context, Container, Component, Code).

---

## Niveau 1 : Diagramme de Contexte (Context)

Le diagramme de contexte montre comment le système interagit avec les utilisateurs et les systèmes externes.

```mermaid
graph TB
    User[👤 Utilisateur Final<br/>Employé, Manager, Admin]
    Admin[👤 Administrateur Système<br/>Tech Lead, DevOps]

    System[🏢 Application Enterprise<br/>Plateforme de gestion stratégique]

    Keycloak[🔐 Keycloak<br/>Identity & Access Management]
    Postgres[🗄️ PostgreSQL<br/>Base de données relationnelle]
    Elastic[🔍 Elasticsearch<br/>Moteur de recherche]
    Monitoring[📊 Prometheus/Grafana<br/>Monitoring & Observabilité]

    User -->|Utilise via navigateur| System
    Admin -->|Administre et monitore| System

    System -->|Authentification/Autorisation| Keycloak
    System -->|Stockage des données| Postgres
    System -->|Recherche avancée| Elastic
    System -->|Métriques et logs| Monitoring

    style System fill:#4A90E2,stroke:#2E5C8A,stroke-width:3px,color:#fff
    style User fill:#50C878,stroke:#2E7D4E,color:#fff
    style Admin fill:#50C878,stroke:#2E7D4E,color:#fff
    style Keycloak fill:#E74C3C,stroke:#A93226,color:#fff
    style Postgres fill:#336791,stroke:#1A3A5C,color:#fff
    style Elastic fill:#FEC514,stroke:#C9980F,color:#000
    style Monitoring fill:#E96443,stroke:#A53F2A,color:#fff
```

**Acteurs :**
- **Utilisateurs finaux** : Employés, managers, administrateurs métier
- **Administrateurs système** : Tech Leads, DevOps, SRE

**Systèmes externes :**
- **Keycloak** : Gestion centralisée de l'authentification et des autorisations (OAuth2/OIDC)
- **PostgreSQL** : Base de données relationnelle principale
- **Elasticsearch** : Moteur de recherche pour requêtes complexes
- **Prometheus/Grafana** : Stack de monitoring et observabilité

---

## Niveau 2 : Diagramme de Conteneurs (Container)

Le diagramme de conteneurs décompose le système en conteneurs logiques (applications, services).

```mermaid
graph TB
    User[👤 Utilisateur]

    subgraph "Frontend [Node.js 20 + Nginx]"
        Angular[🅰️ Angular 17+<br/>Standalone Components<br/>NgRx Store<br/>DSFR Components]
    end

    subgraph "Backend [Java 17 + Spring Boot 3]"
        API[🚀 API REST<br/>Spring Boot<br/>Spring Security<br/>OpenAPI 3]

        Business[💼 Services Métier<br/>Architecture Hexagonale<br/>Use Cases<br/>Domain Logic]
    end

    subgraph "Persistence"
        Postgres[(🗄️ PostgreSQL 15<br/>Données relationnelles)]
        Elastic[(🔍 Elasticsearch 8<br/>Recherche full-text)]
        Cache[(⚡ Redis<br/>Cache distribué)]
    end

    subgraph "Sécurité & Monitoring"
        Keycloak[🔐 Keycloak 23<br/>OAuth2/OIDC<br/>Gestion des rôles]
        Prometheus[📊 Prometheus<br/>Métriques]
        Grafana[📈 Grafana<br/>Dashboards]
        ELK[📝 ELK Stack<br/>Logs centralisés]
    end

    User -->|HTTPS| Angular
    Angular -->|REST API<br/>JWT Token| API

    API -->|JWT Validation| Keycloak
    API -->|Queries/Transactions| Postgres
    API -->|Full-text Search| Elastic
    API -->|Cache Read/Write| Cache

    Business -->|Data Access| Postgres
    Business -->|Search| Elastic

    API -->|Metrics| Prometheus
    API -->|Logs| ELK
    Prometheus -->|Visualization| Grafana

    style Angular fill:#DD0031,stroke:#B00020,color:#fff
    style API fill:#6DB33F,stroke:#4A7C2F,color:#fff
    style Business fill:#6DB33F,stroke:#4A7C2F,color:#fff
    style Postgres fill:#336791,stroke:#1A3A5C,color:#fff
    style Elastic fill:#FEC514,stroke:#C9980F,color:#000
    style Cache fill:#DC382D,stroke:#A52820,color:#fff
    style Keycloak fill:#E74C3C,stroke:#A93226,color:#fff
```

**Conteneurs principaux :**

### Frontend
- **Technologie** : Angular 17+ avec standalone components
- **État** : NgRx Store pour la gestion d'état centralisée
- **UI** : DSFR (Système de Design de l'État Français)
- **Accessibilité** : RGAA compliant
- **Déploiement** : Build optimisé servi par Nginx

### Backend
- **Technologie** : Spring Boot 3 + Java 17
- **Architecture** : Hexagonale (Ports & Adapters)
- **Sécurité** : Spring Security + OAuth2 Resource Server
- **API** : REST avec OpenAPI 3 (Swagger)
- **Résilience** : Resilience4j (Circuit Breaker, Rate Limiter)

### Persistence
- **PostgreSQL** : Base de données principale (ACID)
- **Elasticsearch** : Recherche full-text et analytics
- **Redis** : Cache distribué et sessions

---

## Niveau 3 : Diagramme de Composants (Component)

### 3.1 Composants Backend (Architecture Hexagonale)

```mermaid
graph TB
    subgraph "Presentation Layer"
        RestController[REST Controllers<br/>@RestController<br/>OpenAPI Annotations]
        ExceptionHandler[Global Exception Handler<br/>@RestControllerAdvice]
        DTOs[DTOs & Mappers<br/>Request/Response objects]
    end

    subgraph "Application Layer"
        UseCases[Use Cases<br/>Business Orchestration<br/>@Service]
        EventHandlers[Event Handlers<br/>Domain Events]
    end

    subgraph "Domain Layer"
        Entities[Domain Entities<br/>JPA Entities<br/>Value Objects]
        DomainServices[Domain Services<br/>Core Business Logic]
        Repositories[Repository Interfaces<br/>Ports]
    end

    subgraph "Infrastructure Layer"
        JpaRepos[JPA Repositories<br/>@Repository<br/>Spring Data]
        ElasticAdapter[Elasticsearch Adapter<br/>Search Implementation]
        KeycloakAdapter[Keycloak Adapter<br/>User Management]
        CacheAdapter[Cache Adapter<br/>Redis Integration]
    end

    subgraph "External Systems"
        Postgres[(PostgreSQL)]
        Elastic[(Elasticsearch)]
        Keycloak[Keycloak]
        Redis[(Redis)]
    end

    RestController --> UseCases
    RestController --> DTOs
    ExceptionHandler -.->|Handles errors| RestController

    UseCases --> DomainServices
    UseCases --> Repositories
    UseCases --> EventHandlers

    DomainServices --> Entities
    Repositories -.->|Implemented by| JpaRepos

    JpaRepos --> Postgres
    ElasticAdapter --> Elastic
    KeycloakAdapter --> Keycloak
    CacheAdapter --> Redis

    UseCases --> ElasticAdapter
    UseCases --> KeycloakAdapter
    UseCases --> CacheAdapter

    style RestController fill:#6DB33F,stroke:#4A7C2F,color:#fff
    style UseCases fill:#6DB33F,stroke:#4A7C2F,color:#fff
    style DomainServices fill:#FF6B6B,stroke:#CC5555,color:#fff
    style Entities fill:#FF6B6B,stroke:#CC5555,color:#fff
    style JpaRepos fill:#4ECDC4,stroke:#3DA39C,color:#fff
```

**Couches de l'architecture hexagonale :**

1. **Presentation (Adapters)** : Controllers REST, DTOs, Exception handlers
2. **Application** : Use cases, orchestration, coordination
3. **Domain (Core)** : Logique métier, entités, value objects, interfaces
4. **Infrastructure (Adapters)** : Implémentations concrètes (DB, Search, Cache)

### 3.2 Composants Frontend (Angular)

```mermaid
graph TB
    subgraph "Presentation Components"
        Pages[Smart Components<br/>Pages/Containers<br/>Connected to Store]
        UI[Dumb Components<br/>DSFR UI Components<br/>Presentational]
    end

    subgraph "State Management (NgRx)"
        Store[NgRx Store<br/>Centralized State]
        Actions[Actions<br/>User Events]
        Reducers[Reducers<br/>State Updates]
        Effects[Effects<br/>Side Effects<br/>HTTP Calls]
        Selectors[Selectors<br/>State Queries]
    end

    subgraph "Services"
        HttpServices[HTTP Services<br/>API Communication]
        AuthService[Auth Service<br/>Token Management]
        A11yService[Accessibility Service<br/>RGAA Support]
    end

    subgraph "Guards & Interceptors"
        AuthGuard[Auth Guard<br/>Route Protection]
        RoleGuard[Role Guard<br/>Authorization]
        AuthInterceptor[Auth Interceptor<br/>JWT Injection]
        ErrorInterceptor[Error Interceptor<br/>Error Handling]
    end

    subgraph "Backend API"
        API[REST API<br/>Spring Boot]
    end

    Pages -->|Dispatch| Actions
    Pages -->|Select| Selectors
    UI -->|Events| Pages

    Actions --> Reducers
    Actions --> Effects
    Reducers --> Store
    Selectors --> Store

    Effects --> HttpServices
    HttpServices --> AuthInterceptor
    HttpInterceptor --> ErrorInterceptor
    ErrorInterceptor --> API

    AuthGuard --> AuthService
    RoleGuard --> AuthService
    Pages -.->|Protected by| AuthGuard
    Pages -.->|Protected by| RoleGuard

    style Pages fill:#DD0031,stroke:#B00020,color:#fff
    style Store fill:#764ABC,stroke:#5E3A96,color:#fff
    style Effects fill:#764ABC,stroke:#5E3A96,color:#fff
    style HttpServices fill:#4A90E2,stroke:#2E5C8A,color:#fff
    style API fill:#6DB33F,stroke:#4A7C2F,color:#fff
```

**Composants clés :**

1. **Smart Components** : Pages connectées au store NgRx
2. **Dumb Components** : Composants DSFR réutilisables
3. **NgRx Store** : Gestion d'état centralisée (Actions, Reducers, Effects, Selectors)
4. **Services HTTP** : Communication avec le backend
5. **Guards** : Protection des routes (authentification, rôles)
6. **Interceptors** : Injection JWT, gestion d'erreurs

---

## Niveau 4 : Diagramme de Code (Code)

### 4.1 Exemple : Architecture d'un Use Case Backend

```mermaid
classDiagram
    class UserController {
        -UserService userService
        +getUsers(Pageable) ResponseEntity~Page~UserDTO~~
        +getUserById(UUID) ResponseEntity~UserDTO~
        +createUser(CreateUserRequest) ResponseEntity~UserDTO~
        +updateUser(UUID, UpdateUserRequest) ResponseEntity~UserDTO~
        +deleteUser(UUID) ResponseEntity~Void~
    }

    class UserService {
        -UserRepository userRepository
        -UserMapper userMapper
        -KeycloakAdapter keycloakAdapter
        +findAll(Pageable) Page~User~
        +findById(UUID) Optional~User~
        +create(User) User
        +update(UUID, User) User
        +delete(UUID) void
    }

    class UserRepository {
        <<interface>>
        +findAll(Pageable) Page~User~
        +findById(UUID) Optional~User~
        +save(User) User
        +deleteById(UUID) void
        +existsByEmail(String) boolean
    }

    class User {
        -UUID id
        -String username
        -String email
        -String firstName
        -String lastName
        -Set~Role~ roles
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +isActive() boolean
        +hasRole(Role) boolean
    }

    class JpaUserRepository {
        <<implementation>>
        +findAll(Pageable) Page~User~
        +findById(UUID) Optional~User~
        +save(User) User
        +deleteById(UUID) void
        +existsByEmail(String) boolean
    }

    class KeycloakAdapter {
        -Keycloak keycloak
        +createUser(User) String
        +updateUser(String, User) void
        +deleteUser(String) void
        +assignRoles(String, Set~Role~) void
    }

    UserController --> UserService
    UserService --> UserRepository
    UserService --> KeycloakAdapter
    UserRepository <|.. JpaUserRepository
    UserService --> User
```

### 4.2 Exemple : NgRx Store pour les Users (Frontend)

```mermaid
classDiagram
    class UserActions {
        <<actions>>
        +loadUsers()
        +loadUsersSuccess(users)
        +loadUsersFailure(error)
        +createUser(user)
        +createUserSuccess(user)
        +updateUser(id, changes)
        +deleteUser(id)
    }

    class UserReducer {
        <<reducer>>
        +initialState UserState
        +on(loadUsersSuccess) State
        +on(createUserSuccess) State
        +on(updateUser) State
        +on(deleteUser) State
    }

    class UserEffects {
        -actions$ Actions
        -userService UserService
        +loadUsers$
        +createUser$
        +updateUser$
        +deleteUser$
    }

    class UserSelectors {
        <<selectors>>
        +selectUserState
        +selectAllUsers
        +selectUserById(id)
        +selectLoading
        +selectError
    }

    class UserService {
        -http HttpClient
        +getUsers() Observable~User[]~
        +getUserById(id) Observable~User~
        +createUser(user) Observable~User~
        +updateUser(id, user) Observable~User~
        +deleteUser(id) Observable~void~
    }

    class UserListComponent {
        -store Store
        +users$ Observable~User[]~
        +loading$ Observable~boolean~
        +ngOnInit() void
        +onDelete(id) void
    }

    UserEffects --> UserActions
    UserEffects --> UserService
    UserReducer --> UserActions
    UserListComponent --> UserSelectors
    UserListComponent --> UserActions
    UserSelectors ..> UserReducer : queries
```

---

## Flux de données complets

### Flux d'authentification OAuth2/OIDC

```mermaid
sequenceDiagram
    actor User
    participant Angular
    participant Keycloak
    participant API as Spring Boot API
    participant DB as PostgreSQL

    User->>Angular: Accès à l'application
    Angular->>Keycloak: Redirection vers /auth
    Keycloak->>User: Page de login
    User->>Keycloak: Credentials (username/password)
    Keycloak->>Keycloak: Validation credentials
    Keycloak->>Angular: Redirection avec authorization code
    Angular->>Keycloak: Échange code contre tokens
    Keycloak->>Angular: Access Token + Refresh Token + ID Token
    Angular->>Angular: Stockage tokens (memory/session)

    Angular->>API: GET /api/v1/users (+ Bearer token)
    API->>API: Validation JWT signature
    API->>Keycloak: Introspection token (optionnel)
    Keycloak->>API: Token valid + claims
    API->>API: Extraction roles/permissions
    API->>DB: Query users
    DB->>API: Users data
    API->>Angular: 200 OK + Users JSON
    Angular->>User: Affichage liste utilisateurs
```

### Flux CRUD avec NgRx

```mermaid
sequenceDiagram
    participant Component
    participant Store
    participant Actions
    participant Effects
    participant Service
    participant API

    Component->>Store: dispatch(loadUsers())
    Store->>Actions: loadUsers action
    Actions->>Effects: loadUsers$ effect triggered
    Effects->>Service: getUsers()
    Service->>API: HTTP GET /api/v1/users
    API->>Service: 200 OK + Users[]
    Service->>Effects: Observable<Users[]>
    Effects->>Actions: loadUsersSuccess(users)
    Actions->>Store: Update state via reducer
    Store->>Component: New state via selector
    Component->>Component: Re-render UI
```

### Flux de recherche Elasticsearch

```mermaid
sequenceDiagram
    participant User
    participant Angular
    participant API
    participant Elastic as Elasticsearch
    participant Postgres as PostgreSQL

    User->>Angular: Saisie recherche "Spring Boot"
    Angular->>Angular: Debounce (300ms)
    Angular->>API: GET /api/v1/search?q=Spring+Boot
    API->>Elastic: Search query (match, fuzzy)
    Elastic->>API: Hits with IDs + scores
    API->>Postgres: Get full entities by IDs
    Postgres->>API: Entities with relations
    API->>Angular: 200 OK + Enriched results
    Angular->>User: Affichage résultats paginés
```

---

## Architecture de déploiement (Kubernetes)

```mermaid
graph TB
    subgraph "Kubernetes Cluster"
        subgraph "Ingress"
            Ingress[Nginx Ingress Controller<br/>TLS Termination<br/>Rate Limiting]
        end

        subgraph "Frontend Namespace"
            FrontPod1[Angular Pod 1<br/>Nginx]
            FrontPod2[Angular Pod 2<br/>Nginx]
            FrontService[Frontend Service<br/>ClusterIP]
        end

        subgraph "Backend Namespace"
            BackPod1[Spring Boot Pod 1<br/>Java 17]
            BackPod2[Spring Boot Pod 2<br/>Java 17]
            BackPod3[Spring Boot Pod 3<br/>Java 17]
            BackService[Backend Service<br/>ClusterIP]
        end

        subgraph "Data Namespace"
            PostgresPod[PostgreSQL StatefulSet<br/>Primary + Replicas]
            RedisPod[Redis StatefulSet<br/>Cluster Mode]
            ElasticPod[Elasticsearch StatefulSet<br/>3 nodes cluster]
        end

        subgraph "Security Namespace"
            KeycloakPod[Keycloak StatefulSet<br/>HA Mode]
        end

        subgraph "Monitoring Namespace"
            PrometheusPod[Prometheus<br/>Metrics Storage]
            GrafanaPod[Grafana<br/>Dashboards]
            ElkPod[ELK Stack<br/>Centralized Logs]
        end

        subgraph "Persistent Storage"
            PV1[PersistentVolume<br/>PostgreSQL Data]
            PV2[PersistentVolume<br/>Elasticsearch Data]
            PV3[PersistentVolume<br/>Redis Data]
        end
    end

    Internet[🌐 Internet] --> Ingress

    Ingress -->|/| FrontService
    Ingress -->|/api| BackService

    FrontService --> FrontPod1
    FrontService --> FrontPod2

    BackService --> BackPod1
    BackService --> BackPod2
    BackService --> BackPod3

    BackPod1 --> PostgresPod
    BackPod2 --> PostgresPod
    BackPod3 --> PostgresPod

    BackPod1 --> RedisPod
    BackPod2 --> RedisPod
    BackPod3 --> RedisPod

    BackPod1 --> ElasticPod
    BackPod2 --> ElasticPod
    BackPod3 --> ElasticPod

    BackPod1 --> KeycloakPod
    BackPod2 --> KeycloakPod
    BackPod3 --> KeycloakPod

    PostgresPod --> PV1
    ElasticPod --> PV2
    RedisPod --> PV3

    BackPod1 -.->|Metrics| PrometheusPod
    BackPod2 -.->|Metrics| PrometheusPod
    BackPod3 -.->|Metrics| PrometheusPod

    PrometheusPod --> GrafanaPod

    BackPod1 -.->|Logs| ElkPod
    BackPod2 -.->|Logs| ElkPod
    BackPod3 -.->|Logs| ElkPod

    style Ingress fill:#00BCD4,stroke:#0097A7,color:#fff
    style FrontPod1 fill:#DD0031,stroke:#B00020,color:#fff
    style FrontPod2 fill:#DD0031,stroke:#B00020,color:#fff
    style BackPod1 fill:#6DB33F,stroke:#4A7C2F,color:#fff
    style BackPod2 fill:#6DB33F,stroke:#4A7C2F,color:#fff
    style BackPod3 fill:#6DB33F,stroke:#4A7C2F,color:#fff
```

**Caractéristiques clés :**
- **Haute disponibilité** : Réplication des pods (3 replicas backend)
- **Load balancing** : Service Kubernetes avec distribution automatique
- **Persistent storage** : StatefulSets pour données critiques
- **Isolation** : Namespaces par couche applicative
- **Monitoring** : Prometheus + Grafana intégrés
- **Logs** : Centralisation avec ELK Stack

---

## Pipeline CI/CD GitLab

```mermaid
graph LR
    subgraph "GitLab CI/CD Pipeline"
        Commit[Git Commit<br/>Push to branch]

        subgraph "Stage: Build"
            BuildBack[Build Backend<br/>Maven compile]
            BuildFront[Build Frontend<br/>npm run build]
        end

        subgraph "Stage: Test"
            TestBack[Test Backend<br/>JUnit + Integration<br/>Testcontainers]
            TestFront[Test Frontend<br/>Jest + Cypress]
            Lint[Lint & Format<br/>Checkstyle + ESLint]
        end

        subgraph "Stage: Quality"
            Sonar[SonarQube Analysis<br/>Code Quality<br/>Security Scan]
        end

        subgraph "Stage: Package"
            DockerBack[Build Docker Image<br/>Backend]
            DockerFront[Build Docker Image<br/>Frontend]
            PushRegistry[Push to Registry<br/>GitLab Container Registry]
        end

        subgraph "Stage: Deploy"
            DeployDev[Deploy to Dev<br/>Kubernetes]
            DeployStaging[Deploy to Staging<br/>Manual Approval]
            DeployProd[Deploy to Production<br/>Manual Approval]
        end
    end

    Commit --> BuildBack
    Commit --> BuildFront

    BuildBack --> TestBack
    BuildFront --> TestFront
    BuildBack --> Lint
    BuildFront --> Lint

    TestBack --> Sonar
    TestFront --> Sonar

    Sonar --> DockerBack
    Sonar --> DockerFront

    DockerBack --> PushRegistry
    DockerFront --> PushRegistry

    PushRegistry --> DeployDev
    DeployDev -.->|Manual| DeployStaging
    DeployStaging -.->|Manual| DeployProd

    style Commit fill:#FC6D26,stroke:#E24329,color:#fff
    style Sonar fill:#4E9BCD,stroke:#3A7BA8,color:#fff
    style DeployProd fill:#28A745,stroke:#1E7E34,color:#fff
```

**Étapes du pipeline :**
1. **Build** : Compilation backend (Maven) et frontend (npm)
2. **Test** : Tests unitaires, intégration, E2E, linting
3. **Quality** : Analyse SonarQube (code smells, vulnerabilités, coverage)
4. **Package** : Build images Docker multi-stage
5. **Deploy** : Déploiement automatique en dev, manuel en staging/prod

---

## Décisions architecturales clés

### ADR 001 : Architecture Hexagonale
- **Décision** : Adopter l'architecture hexagonale pour le backend
- **Raison** : Indépendance de la logique métier vis-à-vis de l'infrastructure
- **Conséquence** : Meilleure testabilité, maintenabilité, évolutivité

### ADR 002 : Standalone Components Angular
- **Décision** : Utiliser exclusivement les standalone components
- **Raison** : Recommandation officielle Angular 17+, simplification
- **Conséquence** : Moins de boilerplate, lazy loading simplifié

### ADR 003 : NgRx pour la gestion d'état
- **Décision** : NgRx Store comme solution de state management
- **Raison** : Prévisibilité, DevTools, écosystème mature
- **Conséquence** : Courbe d'apprentissage, mais scalabilité garantie

### ADR 004 : Keycloak pour l'IAM
- **Décision** : Keycloak comme Identity Provider
- **Raison** : Open-source, support OAuth2/OIDC, SSO, gestion des rôles
- **Conséquence** : Configuration initiale complexe, mais sécurité renforcée

### ADR 005 : Kubernetes comme orchestrateur
- **Décision** : Déploiement sur Kubernetes
- **Raison** : Scalabilité horizontale, self-healing, écosystème riche
- **Conséquence** : Complexité opérationnelle, mais production-ready

---

## Matrice de responsabilités (RACI)

| Composant | Tech Lead | Senior Backend | Senior Frontend | Intermédiaire | Junior |
|-----------|-----------|----------------|-----------------|---------------|--------|
| Architecture globale | **R/A** | C | C | I | I |
| Spring Security + Keycloak | C | **R/A** | I | C | I |
| Architecture hexagonale | **R/A** | **R** | I | C | I |
| API REST + OpenAPI | C | **R/A** | C | C | I |
| NgRx Store | C | I | **R/A** | C | C |
| DSFR Components | I | I | **R/A** | **R** | C |
| Tests unitaires | C | **R/A** | **R/A** | **R** | **R** |
| GitLab CI/CD | **R/A** | C | C | I | I |
| Kubernetes | **R/A** | C | C | I | I |
| Documentation | **A** | **R** | **R** | **R** | C |

**Légende :**
- **R** : Responsible (Réalise)
- **A** : Accountable (Responsable final)
- **C** : Consulted (Consulté)
- **I** : Informed (Informé)

---

## Métriques de qualité

### Objectifs de qualité

| Métrique | Objectif | Mesure |
|----------|----------|--------|
| Couverture de code | > 80% | JaCoCo + Jest |
| Code smells | < 50 | SonarQube |
| Bugs critiques | 0 | SonarQube |
| Vulnérabilités | 0 | SonarQube + Dependabot |
| Duplications | < 3% | SonarQube |
| Complexité cyclomatique | < 10 | SonarQube |
| Performance API (p95) | < 200ms | Prometheus |
| Disponibilité | > 99.9% | Prometheus |
| Build time | < 10 min | GitLab CI/CD |

---

## Prochaines étapes

1. ✅ Architecture C4 définie
2. ⏭️ Création de la structure de projet
3. ⏭️ Implémentation backend (Spring Boot)
4. ⏭️ Implémentation frontend (Angular)
5. ⏭️ Configuration infrastructure (Docker, K8s)
6. ⏭️ Pipeline CI/CD
7. ⏭️ Documentation technique
8. ⏭️ Plan de charge Tech Lead
