# Documentation Technique - Module Formation

## 📋 Vue d'Ensemble

Le module Formation est un système complet de gestion des formations d'entreprise permettant de créer, gérer et suivre les formations et leurs participants. Il suit les principes de l'architecture hexagonale et offre une API RESTful sécurisée.

## 🏗️ Architecture

### Architecture Hexagonale
Le module Formation respecte parfaitement l'architecture hexagonale avec une séparation claire des responsabilités :

```
Formation Module
├── Domain Layer (Cœur métier)
│   ├── Entités métier
│   ├── Enums de domaine
│   ├── Ports (interfaces)
│   └── Exceptions métier
├── Application Layer (Use Cases)
│   ├── Services applicatifs
│   ├── DTOs de transfert
│   └── Mappers
├── Infrastructure Layer (Adapters)
│   ├── Repositories JPA
│   ├── Adapters hexagonaux
│   └── Projections optimisées
└── Presentation Layer (API)
    ├── Controllers REST
    ├── Sécurité & autorisations
    └── Documentation OpenAPI
```

## 📊 Modèle de Données

### Entités Principales

#### 1. Formation (`formations`)
**Localisation :** `domain/model/Formation.java`

```java
@Entity
@Table(name = "formations")
public class Formation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(nullable = false, length = 500)
    private String formateurs;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_formation", nullable = false)
    private LocalDate dateFormation;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(nullable = false, length = 100)
    private String secteur;

    @Column(nullable = false, length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModaliteFormation modalite;

    @Column(name = "nb_participants", nullable = false)
    private Integer nbParticipants;

    @Column(length = 200)
    private String lieu;

    @Column(length = 100)
    private String ville;

    @Column(name = "lien_participation", length = 500)
    private String lienParticipation;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version; // Optimistic locking
}
```

**Index de Performance :**
- `idx_formation_date` : Optimise les recherches par date
- `idx_formation_secteur` : Optimise les recherches par secteur
- `idx_formation_region` : Optimise les recherches par région
- `idx_formation_modalite` : Optimise les recherches par modalité
- `idx_formation_date_secteur` : Optimise les recherches combinées
- `idx_formation_date_region` : Optimise les recherches combinées

#### 2. FormationParticipation (`formation_participations`)
**Localisation :** `domain/model/FormationParticipation.java`

```java
@Entity
@Table(name = "formation_participations")
public class FormationParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formation_id", nullable = false)
    private Formation formation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_participation", nullable = false)
    @Builder.Default
    private StatutParticipation statutParticipation = StatutParticipation.INSCRIT;

    @Column(name = "date_inscription", nullable = false)
    private LocalDateTime dateInscription;

    @Column(name = "date_presence")
    private LocalDateTime datePresence;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    // Contraintes
    @UniqueConstraint(name = "uk_formation_user",
                     columnNames = {"formation_id", "user_id"})
}
```

### Enums de Domaine

#### ModaliteFormation
**Localisation :** `domain/model/ModaliteFormation.java`

```java
public enum ModaliteFormation {
    EN_PRESENTIEL("En présentiel"),    // Nécessite lieu + ville
    EN_LIGNE("En ligne");              // Nécessite lienParticipation

    private final String libelle;
}
```

#### FormationStatut (Calculé Dynamiquement)
**Localisation :** `domain/model/FormationStatut.java`

```java
public enum FormationStatut {
    A_VENIR,    // dateFormation > now()
    EN_COURS,   // heureDebut <= now() <= heureFin (même jour)
    TERMINEE    // heureFin < now()
}
```

#### StatutParticipation
**Localisation :** `domain/model/StatutParticipation.java`

```java
public enum StatutParticipation {
    INSCRIT,    // État initial
    PRESENT,    // Marqué présent (met à jour User.dateDerniereFormation)
    ABSENT,     // Marqué absent
    ANNULE      // Inscription annulée
}
```

## 🔧 Couches Applicatives

### 1. Services de Domaine

#### FormationService
**Localisation :** `application/service/FormationService.java`

**Responsabilités :**
- Gestion CRUD des formations
- Logique métier d'inscription/désinscription
- Validation des règles business
- Gestion des participations et présences

**Méthodes Principales :**

```java
@Service
@Transactional
public class FormationService {

    // CRUD Operations
    Formation createFormation(Formation formation)
    Formation updateFormation(UUID id, Formation formationData)
    void deleteFormation(UUID id)

    // Lecture avec optimisations
    @Transactional(readOnly = true)
    Formation getFormationById(UUID id)
    Page<Formation> searchFormations(String secteur, String region,
                                    ModaliteFormation modalite,
                                    FormationStatut statut, Pageable pageable)

    // Gestion des participations
    FormationParticipation inscrireUtilisateur(UUID formationId, UUID userId)
    void desinscrireUtilisateur(UUID formationId, UUID userId)
    FormationParticipation marquerPresence(UUID formationId, UUID userId, boolean present)

    // Projections optimisées
    @Transactional(readOnly = true)
    FormationProjection getFormationProjectionById(UUID id)
    Page<FormationProjection> searchFormationProjections(...)
}
```

**Validations Métier :**
```java
// Dans Formation.java
public void validerCoherenceDates() {
    if (heureFin.isBefore(heureDebut) || heureFin.equals(heureDebut)) {
        throw new IllegalArgumentException("L'heure de fin doit être postérieure à l'heure de début");
    }
}

public void validerModaliteEtChamps() {
    switch (modalite) {
        case EN_PRESENTIEL:
            if (ville == null || lieu == null) {
                throw new IllegalArgumentException("Ville et lieu obligatoires pour le présentiel");
            }
            break;
        case EN_LIGNE:
            if (lienParticipation == null) {
                throw new IllegalArgumentException("Lien de participation obligatoire pour formation en ligne");
            }
            break;
    }
}
```

### 2. DTOs et Mapping

#### DTOs Principaux
**Localisations :** `application/dto/`

```java
// FormationDTO.java - Pour les réponses API
@Data
@Builder
public class FormationDTO {
    private UUID id;
    private String libelle;
    private String formateurs;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFormation;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureDebut;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureFin;

    private String secteur;
    private String region;
    private ModaliteFormation modalite;
    private Integer nbParticipants;
    private String lieu;
    private String ville;
    private String lienParticipation;
    private FormationStatut statut;           // Calculé dynamiquement
    private Integer nbParticipantsInscrits;   // Calculé dynamiquement
    private Boolean complet;                  // Calculé dynamiquement

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}

// CreateFormationRequest.java - Pour la création
@Data
public class CreateFormationRequest {
    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 200)
    private String libelle;

    @NotBlank(message = "Les formateurs sont obligatoires")
    @Size(max = 500)
    private String formateurs;

    // ... autres champs avec validations

    public void validerModaliteEtChamps() {
        // Validation cross-field selon modalité
    }
}

// UpdateFormationRequest.java - Pour la mise à jour
// FormationParticipationDTO.java - Pour les participations
```

#### Mappers MapStruct
**Localisation :** `application/mapper/FormationMapper.java`

```java
@Mapper(componentModel = "spring")
public interface FormationMapper {

    // Entity <-> DTO mapping
    FormationDTO toDTO(Formation formation);
    FormationDTO toDTO(FormationProjection projection);
    Formation toEntity(CreateFormationRequest request);

    // Update mapping avec @MappingTarget
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(UpdateFormationRequest request, @MappingTarget Formation formation);

    // Mapping avec calculs dynamiques
    @Mapping(target = "statut", expression = "java(formation.getStatut())")
    @Mapping(target = "complet", expression = "java(formation.isComplet())")
    FormationDTO toDTOWithCalculatedFields(Formation formation);
}
```

### 3. Couche Infrastructure

#### Repositories

**JPA Repository :** `infrastructure/persistence/JpaFormationRepository.java`
```java
@Repository
public interface JpaFormationRepository extends JpaRepository<Formation, UUID> {

    // Requêtes optimisées avec @Query
    @Query("""
        SELECT f FROM Formation f
        WHERE (:secteur IS NULL OR f.secteur = :secteur)
        AND (:region IS NULL OR f.region = :region)
        AND (:modalite IS NULL OR f.modalite = :modalite)
        ORDER BY f.dateFormation ASC
    """)
    Page<Formation> findByFilters(@Param("secteur") String secteur,
                                 @Param("region") String region,
                                 @Param("modalite") ModaliteFormation modalite,
                                 Pageable pageable);

    // Projection optimisée pour les listes
    @Query("""
        SELECT new com.enterprise.app.infrastructure.persistence.projection.FormationProjection(
            f.id, f.libelle, f.formateurs, f.dateFormation, f.heureDebut, f.heureFin,
            f.secteur, f.region, f.modalite, f.nbParticipants, f.lieu, f.ville,
            f.lienParticipation, f.createdAt, f.updatedAt,
            (SELECT COUNT(fp) FROM FormationParticipation fp
             WHERE fp.formation.id = f.id AND fp.statutParticipation = 'INSCRIT')
        )
        FROM Formation f
    """)
    List<FormationProjection> findAllProjections();

    // Comptage des participants
    @Query("""
        SELECT COUNT(fp) FROM FormationParticipation fp
        WHERE fp.formation.id = :formationId
        AND fp.statutParticipation = 'INSCRIT'
    """)
    int countParticipantsInscrits(@Param("formationId") UUID formationId);
}
```

**Adapter Hexagonal :** `infrastructure/persistence/FormationRepositoryImpl.java`
```java
@Repository
@RequiredArgsConstructor
public class FormationRepositoryImpl implements FormationRepository {

    private final JpaFormationRepository jpaRepository;

    @Override
    public Page<Formation> findByFilters(String secteur, String region,
                                        ModaliteFormation modalite,
                                        FormationStatut statut,
                                        Pageable pageable) {
        if (statut == null) {
            return jpaRepository.findByFilters(secteur, region, modalite, pageable);
        } else {
            // Filtrage statut côté application (car calculé dynamiquement)
            return jpaRepository.findByFilters(secteur, region, modalite, Pageable.unpaged())
                .stream()
                .filter(f -> f.getStatut() == statut)
                .collect(/* pagination manuelle */);
        }
    }
}
```

#### Projections Optimisées
**Localisation :** `infrastructure/persistence/projection/FormationProjection.java`

```java
public class FormationProjection {
    private UUID id;
    private String libelle;
    private String formateurs;
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
    private Integer nbParticipantsInscrits; // Calculé en base

    // Constructeur utilisé par @Query JPA
    public FormationProjection(UUID id, String libelle, String formateurs,
                              /* ... tous les champs ... */,
                              Long nbParticipantsInscrits) {
        // ...
        this.nbParticipantsInscrits = nbParticipantsInscrits.intValue();
    }

    // Méthodes calculées
    public FormationStatut getStatut() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime debut = LocalDateTime.of(dateFormation, heureDebut);
        LocalDateTime fin = LocalDateTime.of(dateFormation, heureFin);

        if (fin.isBefore(now)) return FormationStatut.TERMINEE;
        if (debut.isAfter(now)) return FormationStatut.A_VENIR;
        return FormationStatut.EN_COURS;
    }

    public boolean isComplet() {
        return nbParticipantsInscrits >= nbParticipants;
    }
}
```

## 🌐 API REST

### Controllers

#### FormationController (v1)
**Localisation :** `presentation/controller/v1/FormationController.java`

**Endpoints Principaux :**

```java
@RestController
@RequestMapping("/api/v1/formations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Formations", description = "Formation management API")
public class FormationController {

    // Lecture (accessible à tous les utilisateurs authentifiés)
    @GetMapping
    public ResponseEntity<Page<FormationDTO>> getAllFormations(
            @RequestParam(required = false) String secteur,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) ModaliteFormation modalite,
            @RequestParam(required = false) FormationStatut statut,
            @PageableDefault(size = 20) Pageable pageable) { }

    @GetMapping("/{id}")
    public ResponseEntity<FormationDTO> getFormationById(@PathVariable UUID id) { }

    // Gestion (ADMIN/MANAGER uniquement)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<FormationDTO> createFormation(
            @Valid @RequestBody CreateFormationRequest request) { }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<FormationDTO> updateFormation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFormationRequest request) { }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteFormation(@PathVariable UUID id) { }

    // Participants (ADMIN/MANAGER/TECH_LEAD)
    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECH_LEAD')")
    public ResponseEntity<List<FormationParticipationDTO>> getFormationParticipants(
            @PathVariable UUID id) { }
}
```

#### FormationParticipationController (v1)
**Localisation :** `presentation/controller/v1/FormationParticipationController.java`

```java
@RestController
@RequestMapping("/api/v1/formations/{formationId}/participations")
public class FormationParticipationController {

    // Inscription (accessible à l'utilisateur lui-même)
    @PostMapping
    @PreAuthorize("authentication.principal.userId == #userId")
    public ResponseEntity<FormationParticipationDTO> inscrire(
            @PathVariable UUID formationId,
            @RequestParam UUID userId) { }

    // Désinscription
    @DeleteMapping("/{userId}")
    @PreAuthorize("authentication.principal.userId == #userId or hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> desinscrire(
            @PathVariable UUID formationId,
            @PathVariable UUID userId) { }

    // Marquage de présence (ADMIN/MANAGER/TECH_LEAD)
    @PutMapping("/{userId}/presence")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECH_LEAD')")
    public ResponseEntity<FormationParticipationDTO> marquerPresence(
            @PathVariable UUID formationId,
            @PathVariable UUID userId,
            @RequestParam boolean present) { }
}
```

### Sécurité & Autorisations

**Matrice des Autorisations :**

| Action | USER | MANAGER | TECH_LEAD | ADMIN |
|--------|------|---------|-----------|-------|
| Lire formations | ✅ | ✅ | ✅ | ✅ |
| Créer formation | ❌ | ✅ | ❌ | ✅ |
| Modifier formation | ❌ | ✅ | ❌ | ✅ |
| Supprimer formation | ❌ | ✅ | ❌ | ✅ |
| S'inscrire | ✅ (soi-même) | ✅ | ✅ | ✅ |
| Se désinscrire | ✅ (soi-même) | ✅ | ✅ | ✅ |
| Voir participants | ❌ | ✅ | ✅ | ✅ |
| Marquer présence | ❌ | ✅ | ✅ | ✅ |

### Documentation OpenAPI

**Configuration Swagger :**
- **Base URL :** `/api/v1/formations`
- **Authentication :** Bearer Token (JWT)
- **Tags :** "Formations", "Formation Participations"

**Exemples de Réponses :**

```json
// GET /api/v1/formations
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "libelle": "Formation Angular Avancé",
      "formateurs": "Jean Dupont, Marie Martin",
      "description": "Formation complète sur Angular 17+",
      "dateFormation": "2024-12-25",
      "heureDebut": "09:00",
      "heureFin": "17:00",
      "secteur": "IT",
      "region": "Paris",
      "modalite": "EN_PRESENTIEL",
      "nbParticipants": 20,
      "lieu": "Salle de formation A",
      "ville": "Paris",
      "lienParticipation": null,
      "statut": "A_VENIR",
      "nbParticipantsInscrits": 12,
      "complet": false,
      "createdAt": "2024-12-19T10:30:00",
      "updatedAt": "2024-12-19T10:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

## 💾 Base de Données

### Schéma SQL

**Migration V004 :** `V004__Create_Formation_Tables.sql`

```sql
-- Table formations
CREATE TABLE formations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    libelle VARCHAR(200) NOT NULL,
    formateurs VARCHAR(500) NOT NULL,
    description TEXT,
    date_formation DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    secteur VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    modalite VARCHAR(20) NOT NULL CHECK (modalite IN ('EN_PRESENTIEL', 'EN_LIGNE')),
    nb_participants INTEGER NOT NULL CHECK (nb_participants > 0),
    lieu VARCHAR(200),
    ville VARCHAR(100),
    lien_participation VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Table participations
CREATE TABLE formation_participations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    formation_id UUID NOT NULL REFERENCES formations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    statut_participation VARCHAR(20) NOT NULL DEFAULT 'INSCRIT'
        CHECK (statut_participation IN ('INSCRIT', 'PRESENT', 'ABSENT', 'ANNULE')),
    date_inscription TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_presence TIMESTAMP,
    commentaire VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_formation_user UNIQUE (formation_id, user_id)
);

-- Index de performance
CREATE INDEX idx_formation_date ON formations(date_formation);
CREATE INDEX idx_formation_secteur ON formations(secteur);
CREATE INDEX idx_formation_region ON formations(region);
CREATE INDEX idx_formation_modalite ON formations(modalite);
CREATE INDEX idx_formation_date_secteur ON formations(date_formation, secteur);
CREATE INDEX idx_formation_date_region ON formations(date_formation, region);

CREATE INDEX idx_formation_participation_formation ON formation_participations(formation_id);
CREATE INDEX idx_formation_participation_user ON formation_participations(user_id);
CREATE INDEX idx_formation_participation_statut ON formation_participations(statut_participation);
CREATE INDEX idx_formation_participation_formation_statut
    ON formation_participations(formation_id, statut_participation);

-- Ajout à la table users
ALTER TABLE users ADD COLUMN date_derniere_formation TIMESTAMP;

-- Triggers pour updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_formations_updated_at
    BEFORE UPDATE ON formations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_formation_participations_updated_at
    BEFORE UPDATE ON formation_participations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

**Migration V005 :** Mise à jour des modalités

### Performance et Optimisations

**Stratégies d'Optimisation :**

1. **Projections JPA :** Utilisation de `FormationProjection` pour éviter le chargement d'entités complètes dans les listes
2. **Index Composites :** Index sur les combinaisons de champs fréquemment filtrés
3. **Lazy Loading :** Relations `@ManyToOne` en mode `LAZY`
4. **Pagination :** Toutes les listes utilisent `Pageable`
5. **Optimistic Locking :** Protection contre les modifications concurrentes avec `@Version`

**Requêtes Optimisées :**
- Calcul en base du nombre de participants inscrits
- Filtrage par secteur/région/modalité avec index
- Pagination native PostgreSQL

## 🧪 Tests

### Tests Unitaires

**FormationServiceTest :** `test/java/.../FormationServiceTest.java`
- ✅ Tests de création de formation
- ✅ Tests de mise à jour avec optimistic locking
- ✅ Tests d'inscription/désinscription
- ✅ Tests de marquage de présence
- ✅ Tests de validation métier
- ✅ Tests d'exceptions

**FormationMapperTest :** Tests des mappings MapStruct
- ✅ Tests de conversion Entity ↔ DTO
- ✅ Tests de mapping avec projections
- ✅ Tests de mise à jour avec `@MappingTarget`

### Tests d'Intégration

**Avec Testcontainers :**
```java
@Testcontainers
@SpringBootTest
class FormationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void testCompleteFormationWorkflow() {
        // Test complet : création → inscription → présence
    }
}
```

## 🚀 Déploiement & Configuration

### Profils Spring

**Configurations par environnement :**
- `dev` : Base H2 in-memory pour développement rapide
- `docker` : PostgreSQL via Docker Compose
- `prod` : PostgreSQL avec pooling et optimisations

### Variables d'Environnement

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/appdb}
    username: ${DATABASE_USERNAME:user}
    password: ${DATABASE_PASSWORD:password}
```

### Monitoring & Métriques

**Actuator Endpoints :**
- `/actuator/health` : Health check formation database
- `/actuator/metrics` : Métriques JVM et application
- `/actuator/prometheus` : Métriques Prometheus

**Métriques Métier :**
- Nombre de formations créées
- Taux d'inscription par formation
- Taux de présence par secteur/région

## 🔍 Observabilité

### Logging Structuré

```java
@Slf4j
public class FormationService {

    public Formation createFormation(Formation formation) {
        log.info("Création d'une nouvelle formation: libelle={}, secteur={}, modalite={}",
                formation.getLibelle(), formation.getSecteur(), formation.getModalite());
        // ...
        log.info("Formation créée avec succès: id={}, libelle={}",
                result.getId(), result.getLibelle());
    }

    public FormationParticipation marquerPresence(UUID formationId, UUID userId, boolean present) {
        log.info("Marquage de présence: formationId={}, userId={}, present={}",
                formationId, userId, present);
        // ...
    }
}
```

### Audit

**Audit Automatique :**
- `@CreatedDate` et `@LastModifiedDate` sur toutes les entités
- Traçabilité des inscriptions/désinscriptions
- Historique des présences avec timestamps

## 📈 Évolutions Futures

### Fonctionnalités Prévues

1. **Notifications :**
   - Email de confirmation d'inscription
   - Rappels avant formation
   - Notifications d'annulation

2. **Évaluations :**
   - Système d'évaluation des formations
   - Feedback formateurs et participants

3. **Certificats :**
   - Génération automatique de certificats de formation
   - Stockage dans le système de fichiers (Garage)

4. **Statistiques Avancées :**
   - Dashboard analytics pour ADMIN/MANAGER
   - Rapports de fréquentation par secteur/région
   - Prédictions d'affluence

### Améliorations Techniques

1. **Cache :** Mise en cache Redis pour les listes fréquemment consultées
2. **Elasticsearch :** Recherche full-text sur libellé et description
3. **Events :** Système d'événements pour découplage des services
4. **WebSockets :** Notifications temps réel des inscriptions

## 🔗 Intégrations

### Services Externes

1. **Keycloak :** Authentification et autorisation
2. **SMTP :** Envoi d'emails (à implémenter)
3. **File Storage (Garage)** : Stockage documents formation (à implémenter)

### API Externes

```java
// Future integration
@FeignClient(name = "notification-service")
public interface NotificationService {
    @PostMapping("/notifications/formation-inscription")
    void envoyerConfirmationInscription(@RequestBody InscriptionEvent event);
}
```

---

## 📝 Conclusion

Le module Formation constitue un exemple parfait d'implémentation d'architecture hexagonale avec :

- ✅ **Séparation claire des responsabilités** entre domaine, application, infrastructure et présentation
- ✅ **API REST sécurisée** avec autorisations granulaires par rôle
- ✅ **Performance optimisée** avec projections, index et pagination
- ✅ **Qualité enterprise** avec tests, audit, logging structuré
- ✅ **Évolutivité** préparée pour futures fonctionnalités

Cette implémentation peut servir de modèle pour d'autres modules métier de l'application.

---
*Documentation générée le 2024-12-19 - Version 1.0*