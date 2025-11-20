# Architecture Hexagonale (Ports & Adapters)

## Vue d'ensemble

Cette application suit les principes de l'**Architecture Hexagonale** (également appelée Ports & Adapters), proposée par Alistair Cockburn. L'objectif est de créer des applications faiblement couplées, testables et indépendantes des frameworks.

## Principes fondamentaux

### 1. Séparation en couches

```
┌─────────────────────────────────────────────────────────┐
│              PRESENTATION LAYER                          │
│  (Controllers, REST API, GraphQL, gRPC, etc.)           │
│                     ↓ ↑                                  │
│                  Adapters                                │
└─────────────────────────────────────────────────────────┘
                        ↓ ↑
┌─────────────────────────────────────────────────────────┐
│            APPLICATION LAYER                             │
│  (Use Cases, DTOs, Services, Orchestration)             │
│                     ↓ ↑                                  │
│                    Ports                                 │
└─────────────────────────────────────────────────────────┘
                        ↓ ↑
┌─────────────────────────────────────────────────────────┐
│               DOMAIN LAYER (Core)                        │
│  (Business Logic, Domain Models, Domain Services)       │
│          NO framework dependencies                       │
└─────────────────────────────────────────────────────────┘
                        ↓ ↑
┌─────────────────────────────────────────────────────────┐
│           INFRASTRUCTURE LAYER                           │
│  (JPA, Database, External APIs, Messaging, etc.)        │
│                     ↓ ↑                                  │
│                  Adapters                                │
└─────────────────────────────────────────────────────────┘
```

### 2. Règles de dépendance

- **Domain** : Ne dépend de RIEN (pur Java)
- **Application** : Dépend du Domain
- **Infrastructure** : Dépend du Domain et Application
- **Presentation** : Dépend de l'Application

Le Domain est le cœur de l'application et ne doit jamais dépendre des frameworks externes.

## État actuel de l'architecture

### ⚠️ Compromis pragmatique : Annotations JPA dans le Domain

**Situation actuelle :**
Les entités du domain (`User`, `BatchImport`, `AuditEventCommand`, etc.) contiennent des annotations JPA (@Entity, @Table, @Column, etc.).

```java
// backend/src/main/java/com/enterprise/app/domain/model/User.java
@Entity  // ❌ Annotation Spring/JPA dans le domain
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // ...
}
```

**Pourquoi ce compromis ?**

1. **Simplicité** : Pour des applications de taille moyenne, la séparation totale peut créer de la complexité inutile
2. **Productivité** : Évite la duplication entre objets domain et entités JPA
3. **Performance** : Pas de mapping systématique entre domain et persistence
4. **CRUD simple** : Pour des opérations basiques, l'overhead est minimal

**Quand est-ce acceptable ?**

- ✅ Logique métier simple à modérée
- ✅ Application CRUD avec peu de règles complexes
- ✅ Équipe de taille petite à moyenne
- ✅ Pas de changement fréquent de technologie de persistence

**Quand séparer absolument ?**

- ❌ Logique métier complexe avec invariants stricts
- ❌ Domain riche avec comportements sophistiqués
- ❌ Besoin de tester le domain sans framework
- ❌ Migration prévue vers une autre technologie de persistence
- ❌ Règles métier qui ne correspondent pas au modèle relationnel

## Architecture cible (Pure Hexagonal)

### Structure recommandée

```
backend/src/main/java/com/enterprise/app/
├── domain/
│   ├── model/           # PURE domain objects (NO annotations)
│   │   ├── User.java
│   │   ├── UserRole.java
│   │   └── AuditEvent.java
│   ├── port/
│   │   ├── in/         # Input ports (Use Cases interfaces)
│   │   │   ├── CreateUserUseCase.java
│   │   │   └── FindUserUseCase.java
│   │   └── out/        # Output ports (Repository interfaces)
│   │       ├── UserRepository.java
│   │       └── AuditRepository.java
│   └── service/        # Domain services (business logic)
│       └── UserDomainService.java
│
├── application/
│   ├── service/        # Use case implementations (orchestration)
│   │   └── UserApplicationService.java
│   └── dto/            # Data Transfer Objects
│       ├── CreateUserRequest.java
│       └── UserDTO.java
│
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/     # JPA entities (with @Entity, @Table, etc.)
│   │   │   ├── UserEntity.java
│   │   │   └── AuditEventEntity.java
│   │   ├── mapper/     # Domain ↔ Entity mappers
│   │   │   └── UserMapper.java
│   │   └── adapter/    # Repository implementations
│   │       └── UserRepositoryAdapter.java
│   └── external/       # External API adapters
│       └── KeycloakAdapter.java
│
└── presentation/
    └── controller/     # REST controllers
        └── UserController.java
```

### Exemple de séparation complète

#### 1. Domain pur (AUCUNE annotation)

```java
// domain/model/User.java
package com.enterprise.app.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Pure domain object - NO framework dependencies.
 * Contains ONLY business logic and invariants.
 */
public class User {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean active;
    private Set<UserRole> roles;
    private LocalDateTime createdAt;

    // Constructor enforcing invariants
    public User(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email must be valid");
        }
        this.id = UUID.randomUUID();
        this.username = username.trim();
        this.email = email.toLowerCase().trim();
        this.active = true;
        this.roles = new HashSet<>();
        this.createdAt = LocalDateTime.now();
    }

    // Business logic methods
    public void activate() {
        if (this.active) {
            throw new IllegalStateException("User is already active");
        }
        this.active = true;
    }

    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException("User is already inactive");
        }
        if (hasRole(UserRole.SYSTEM_ADMIN)) {
            throw new IllegalStateException("Cannot deactivate system admin");
        }
        this.active = false;
    }

    public void grantRole(UserRole role) {
        if (roles.contains(role)) {
            throw new IllegalArgumentException("User already has role: " + role);
        }
        roles.add(role);
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    // Getters only (immutability preferred)
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public Set<UserRole> getRoles() { return new HashSet<>(roles); } // defensive copy
}
```

#### 2. Port de sortie (Repository interface dans domain)

```java
// domain/port/out/UserRepository.java
package com.enterprise.app.domain.port.out;

import com.enterprise.app.domain.model.User;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for user persistence.
 * Defined in domain, implemented in infrastructure.
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    void delete(UUID id);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
```

#### 3. Entité JPA (Infrastructure)

```java
// infrastructure/persistence/entity/UserEntity.java
package com.enterprise.app.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity for user persistence.
 * Contains ONLY persistence concerns, NO business logic.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<String> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;
}
```

#### 4. Mapper Domain ↔ Entity

```java
// infrastructure/persistence/mapper/UserMapper.java
package com.enterprise.app.infrastructure.persistence.mapper;

import com.enterprise.app.domain.model.User;
import com.enterprise.app.domain.model.UserRole;
import com.enterprise.app.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between pure domain objects and JPA entities.
 */
@Component
public class UserMapper {

    public UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        entity.setId(domain.getId());
        entity.setUsername(domain.getUsername());
        entity.setEmail(domain.getEmail());
        entity.setFirstName(domain.getFirstName());
        entity.setLastName(domain.getLastName());
        entity.setActive(domain.isActive());
        entity.setRoles(domain.getRoles().stream()
            .map(Enum::name)
            .collect(Collectors.toSet()));
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public User toDomain(UserEntity entity) {
        User user = new User(entity.getUsername(), entity.getEmail());
        // Use reflection or builder to set private fields
        // Or provide package-private setters for infrastructure
        user.setId(entity.getId());
        user.setFirstName(entity.getFirstName());
        user.setLastName(entity.getLastName());
        user.setActive(entity.isActive());
        entity.getRoles().forEach(roleStr ->
            user.grantRole(UserRole.valueOf(roleStr))
        );
        return user;
    }
}
```

#### 5. Adaptateur de repository

```java
// infrastructure/persistence/adapter/UserRepositoryAdapter.java
package com.enterprise.app.infrastructure.persistence.adapter;

import com.enterprise.app.domain.model.User;
import com.enterprise.app.domain.port.out.UserRepository;
import com.enterprise.app.infrastructure.persistence.entity.UserEntity;
import com.enterprise.app.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapts JPA repository to domain repository port.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaRepository; // Spring Data JPA repo
    private final UserMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username)
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    // ... other methods
}
```

## Migration progressive

### Stratégie recommandée

1. **Phase 1 : Documenter l'existant** ✅
   - Créer cette documentation
   - Identifier les compromis
   - Former l'équipe

2. **Phase 2 : Nouvelles features avec séparation**
   - Les nouvelles entités critiques utilisent la séparation complète
   - Créer des exemples de référence
   - Établir les patterns à suivre

3. **Phase 3 : Refactoring progressif** (optionnel)
   - Refactoriser les entités avec logique métier complexe
   - Commencer par les bounded contexts critiques
   - Mesurer l'impact sur la productivité

### Quand appliquer la séparation complète ?

**Critères de décision :**

| Critère | Garder annotations JPA | Séparer complètement |
|---------|------------------------|----------------------|
| Logique métier | Simple (CRUD) | Complexe avec invariants |
| Règles métier | < 5 règles par entité | > 5 règles par entité |
| Tests | Tests d'intégration suffisent | Tests unitaires domain requis |
| Équipe | < 5 développeurs | > 5 développeurs |
| Durée de vie | < 2 ans | > 2 ans |
| Changement techno | Improbable | Probable (multi-DB, NoSQL, etc.) |

## Avantages et inconvénients

### Approche actuelle (JPA dans domain)

**✅ Avantages :**
- Simplicité : moins de code
- Rapidité de développement
- Facile à comprendre pour débutants
- Pas de mapping overhead

**❌ Inconvénients :**
- Couplage au framework JPA
- Tests du domain nécessitent Spring
- Difficile de changer de technologie de persistence
- Mélange des responsabilités

### Approche pure (séparation complète)

**✅ Avantages :**
- Domain 100% testable sans framework
- Indépendance technologique totale
- Séparation claire des responsabilités
- Facilite DDD (Domain-Driven Design)
- Permet multi-persistence (SQL + NoSQL)

**❌ Inconvénients :**
- Plus de code (entités + domain + mappers)
- Courbe d'apprentissage plus raide
- Overhead de mapping
- Peut être overkill pour CRUD simple

## Conclusion

L'architecture actuelle représente un **compromis pragmatique** acceptable pour une application de taille moyenne avec logique métier modérée.

**Recommandations :**

1. **Court terme** : Garder l'architecture actuelle pour les entités CRUD simples
2. **Moyen terme** : Appliquer la séparation pour les nouvelles features complexes
3. **Long terme** : Évaluer le besoin de refactoring basé sur la croissance de la complexité

**Red flags pour refactoring :**
- 🚩 Méthodes de plus de 50 lignes dans les entités
- 🚩 Difficulté à tester la logique métier
- 🚩 Besoin de plusieurs technologies de persistence
- 🚩 Complexité cyclomatique élevée dans le domain

---

**Références :**
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) - Alistair Cockburn
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) - Robert C. Martin
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html) - Eric Evans
