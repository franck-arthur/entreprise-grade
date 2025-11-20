# Enterprise Application - Backend

Backend Spring Boot avec architecture hexagonale, sécurité OAuth2/Keycloak, et PostgreSQL.

## Technologies

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Security + OAuth2 Resource Server**
- **PostgreSQL 15**
- **Keycloak 23** (authentification/autorisation)
- **Elasticsearch 8** (recherche)
- **Redis** (cache)
- **Flyway** (migrations DB)
- **MapStruct** (mapping DTO)
- **Resilience4j** (circuit breaker, retry, rate limiting)
- **OpenAPI 3 / Swagger**
- **Testcontainers** (tests d'intégration)

## Architecture

### Architecture Hexagonale

```
com.enterprise.app/
├── domain/              # Logique métier pure
│   ├── model/          # Entités JPA
│   ├── repository/     # Ports (interfaces)
│   └── exception/      # Exceptions métier
├── application/         # Cas d'usage
│   ├── usecase/        # Services applicatifs
│   ├── dto/            # DTOs de transfert
│   └── mapper/         # Mappers MapStruct
├── infrastructure/      # Adapters techniques
│   ├── persistence/    # Implémentations JPA
│   ├── keycloak/       # Adapter Keycloak
│   ├── security/       # Configuration sécurité
│   └── config/         # Configurations Spring
└── presentation/        # Couche présentation
    ├── controller/     # REST Controllers
    └── exception/      # Gestion d'erreurs
```

## Démarrage rapide

### Prérequis

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15 (ou via Docker)
- Keycloak 23 (ou via Docker)

### Lancer l'application localement

1. **Démarrer les dépendances avec Docker Compose**

```bash
cd ..
docker-compose up -d postgres keycloak redis elasticsearch
```

2. **Configurer Keycloak**

- Accéder à http://localhost:8180
- Login: admin / admin
- Créer un realm `enterprise-realm`
- Créer un client `backend-client`
- Créer les rôles: USER, ADMIN, TECH_LEAD, MANAGER

3. **Compiler et lancer le backend**

```bash
mvn clean install
mvn spring-boot:run
```

4. **Accéder à l'API**

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator Health: http://localhost:8080/actuator/health

## Tests

### Tests unitaires

```bash
mvn test
```

### Tests d'intégration (Testcontainers)

```bash
mvn verify
```

### Couverture de code

```bash
mvn test jacoco:report
# Rapport dans target/site/jacoco/index.html
```

### Qualité avec SonarQube

```bash
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token
```

## Configuration

### Profils Spring

- `dev` : Développement local (par défaut)
- `prod` : Production

### Variables d'environnement (production)

```bash
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/appdb
DATABASE_USERNAME=user
DATABASE_PASSWORD=password

# Keycloak
KEYCLOAK_ISSUER_URI=https://keycloak.example.com/realms/enterprise-realm
KEYCLOAK_JWK_SET_URI=https://keycloak.example.com/realms/enterprise-realm/protocol/openid-connect/certs
KEYCLOAK_AUTH_SERVER_URL=https://keycloak.example.com
KEYCLOAK_CLIENT_SECRET=your-secret

# Redis
REDIS_HOST=redis
REDIS_PASSWORD=password

# Elasticsearch
ELASTICSEARCH_URIS=http://elasticsearch:9200
ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=password
```

## API Documentation

L'API est documentée avec OpenAPI 3 (Swagger).

### Accès

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

### Authentification

Toutes les routes (sauf `/actuator/health` et `/api-docs`) nécessitent un token JWT.

**Headers requis:**

```
Authorization: Bearer <JWT_TOKEN>
```

### Endpoints principaux

| Méthode | Endpoint | Description | Rôles requis |
|---------|----------|-------------|--------------|
| GET | `/api/v1/users` | Liste utilisateurs | ADMIN, TECH_LEAD, MANAGER |
| GET | `/api/v1/users/{id}` | Détails utilisateur | ADMIN, TECH_LEAD, MANAGER ou propriétaire |
| POST | `/api/v1/users` | Créer utilisateur | ADMIN, TECH_LEAD |
| PUT | `/api/v1/users/{id}` | Modifier utilisateur | ADMIN, TECH_LEAD |
| DELETE | `/api/v1/users/{id}` | Supprimer utilisateur | ADMIN |
| PATCH | `/api/v1/users/{id}/activate` | Activer utilisateur | ADMIN, TECH_LEAD |
| PATCH | `/api/v1/users/{id}/deactivate` | Désactiver utilisateur | ADMIN, TECH_LEAD |

## Build & Déploiement

### Build Docker

```bash
docker build -t enterprise-app-backend:latest .
```

### Run Docker

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/appdb \
  enterprise-app-backend:latest
```

### Déploiement Kubernetes

Voir les manifests dans `/k8s/backend/`

```bash
kubectl apply -f k8s/backend/
```

## Sécurité

### Configuration OAuth2 Resource Server

L'application utilise Spring Security avec OAuth2 Resource Server pour valider les tokens JWT émis par Keycloak.

### Rôles et permissions

- **USER** : Utilisateur standard (accès limité)
- **MANAGER** : Manager (lecture utilisateurs)
- **TECH_LEAD** : Tech Lead (gestion utilisateurs)
- **ADMIN** : Administrateur (accès total)

### Annotations de sécurité

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
```

## Monitoring

### Actuator Endpoints

- `/actuator/health` : Health check
- `/actuator/health/liveness` : Liveness probe
- `/actuator/health/readiness` : Readiness probe
- `/actuator/metrics` : Métriques Micrometer
- `/actuator/prometheus` : Métriques Prometheus

### Prometheus & Grafana

Les métriques sont exportées au format Prometheus sur `/actuator/prometheus`.

### Logging

Logs structurés au format JSON (Logstash encoder) pour faciliter l'ingestion dans ELK Stack.

## Performance & Résilience

### Circuit Breaker (Resilience4j)

Protection contre les défaillances de Keycloak et services externes.

### Rate Limiting

Limitation du taux de requêtes pour éviter les abus.

### Caching (Redis)

Cache distribué pour les requêtes fréquentes (utilisateurs).

### Connection Pooling (HikariCP)

Pool de connexions optimisé pour PostgreSQL.

## Troubleshooting

### L'application ne démarre pas

Vérifier :
- PostgreSQL est accessible
- Keycloak est accessible
- Les variables d'environnement sont correctes

### Erreur d'authentification

Vérifier :
- Le token JWT est valide
- L'issuer URI correspond à Keycloak
- Les rôles sont correctement configurés dans Keycloak

### Tests échouent

```bash
# Nettoyer le cache Maven
mvn clean

# Rebuild
mvn install

# Vérifier Testcontainers
docker ps
```

## Contribution

1. Créer une branche feature
2. Commiter les changements
3. Lancer les tests
4. Créer une Pull Request

## License

Proprietary - Enterprise Team
