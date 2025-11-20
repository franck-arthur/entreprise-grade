# Application Enterprise-Grade

Application stratégique avec architecture enterprise-grade complète utilisant Spring Boot, Angular 17+, NgRx, DSFR, et infrastructure cloud-native.

## 🏗️ Architecture

### Stack Technologique

**Backend:**
- Java 17
- Spring Boot 3.2.1
- Architecture hexagonale (Ports & Adapters)
- Spring Security + OAuth2 Resource Server
- PostgreSQL 15
- Keycloak 23 (IAM)
- Elasticsearch 8 (Recherche)
- Redis (Cache)
- Flyway (Migrations DB)
- Resilience4j (Circuit Breaker, Rate Limiting)

**Frontend:**
- Angular 17+ (Standalone Components)
- NgRx (State Management)
- DSFR (Système de Design de l'État)
- RxJS
- TypeScript 5.3
- Jest (Tests unitaires)
- Cypress (Tests E2E)

**Infrastructure:**
- Docker & Docker Compose
- Kubernetes
- GitLab CI/CD
- Terraform (Infrastructure as Code)
- Ansible (Configuration Management)
- Prometheus & Grafana (Monitoring)
- ELK Stack (Logs)

## 📁 Structure du Projet

```
entreprise-grade/
├── backend/                 # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/enterprise/app/
│   │   │   │   ├── domain/          # Couche domaine (Entities, Repositories)
│   │   │   │   ├── application/     # Couche application (Use Cases, DTOs)
│   │   │   │   ├── infrastructure/  # Couche infrastructure (JPA, Keycloak, Security)
│   │   │   │   └── presentation/    # Couche présentation (Controllers)
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/    # Migrations Flyway
│   │   └── test/                    # Tests (JUnit, Testcontainers)
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                # Angular frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/               # Services, Interceptors, Guards
│   │   │   │   ├── guards/
│   │   │   │   ├── interceptors/
│   │   │   │   ├── services/
│   │   │   │   └── state/          # NgRx root state
│   │   │   ├── features/           # Feature modules (lazy-loaded)
│   │   │   │   ├── auth/
│   │   │   │   ├── dashboard/
│   │   │   │   └── users/
│   │   │   │       ├── pages/
│   │   │   │       ├── services/
│   │   │   │       └── state/      # NgRx feature state
│   │   │   └── shared/             # Composants réutilisables
│   │   ├── environments/
│   │   └── styles.scss
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── angular.json
│
├── k8s/                     # Manifests Kubernetes
│   ├── dev/
│   ├── staging/
│   └── prod/
│       ├── backend-deployment.yaml
│       ├── frontend-deployment.yaml
│       └── ingress.yaml
│
├── terraform/               # Infrastructure as Code
│   ├── main.tf
│   ├── variables.tf
│   └── outputs.tf
│
├── docs/                    # Documentation
│   └── ARCHITECTURE.md      # Architecture C4
│
├── docker-compose.yml       # Docker Compose pour développement local
├── .gitlab-ci.yml          # Pipeline CI/CD
└── README.md
```

## 🚀 Démarrage Rapide

### Prérequis

- Docker & Docker Compose
- Java 17+
- Node.js 20+
- Maven 3.9+
- kubectl (pour Kubernetes)
- Terraform (pour infrastructure)

### Lancer l'application localement

1. **Cloner le repository**

```bash
git clone https://gitlab.com/project/entreprise-grade.git
cd entreprise-grade
```

2. **Démarrer avec Docker Compose**

```bash
docker-compose up -d
```

Cela démarre tous les services :
- PostgreSQL (port 5432)
- Keycloak (port 8180)
- Redis (port 6379)
- Elasticsearch (port 9200)
- Backend Spring Boot (port 8080)
- Frontend Angular (port 80)

3. **Accéder à l'application**

- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Keycloak**: http://localhost:8180 (admin/admin)
- **Actuator**: http://localhost:8080/actuator

### Développement local (sans Docker)

**Backend:**

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**Frontend:**

```bash
cd frontend
npm install
npm start
```

## 🧪 Tests

### Backend

```bash
cd backend

# Tests unitaires
mvn test

# Tests d'intégration (Testcontainers)
mvn verify

# Couverture de code
mvn test jacoco:report
# Rapport: target/site/jacoco/index.html

# Linting
mvn checkstyle:check
```

### Frontend

```bash
cd frontend

# Tests unitaires (Jest)
npm run test

# Tests en mode watch
npm run test:watch

# Couverture de code
npm run test:coverage
# Rapport: coverage/index.html

# Tests E2E (Cypress)
npm run e2e

# Linting
npm run lint
npm run lint:fix

# Formatage
npm run format
npm run format:check
```

## 📦 Build & Déploiement

### Build des images Docker

```bash
# Backend
docker build -t enterprise-backend:latest ./backend

# Frontend
docker build -t enterprise-frontend:latest ./frontend
```

### Déploiement Kubernetes

```bash
# Development
kubectl apply -f k8s/dev/ --namespace=dev

# Staging
kubectl apply -f k8s/staging/ --namespace=staging

# Production
kubectl apply -f k8s/prod/ --namespace=production
```

### Infrastructure Terraform

```bash
cd terraform

# Initialiser
terraform init

# Planifier
terraform plan

# Appliquer
terraform apply

# Détruire
terraform destroy
```

## 🔐 Sécurité

### Authentification & Autorisation

L'application utilise **Keycloak** comme Identity Provider avec OAuth2/OIDC.

**Rôles disponibles:**
- `USER`: Utilisateur standard
- `MANAGER`: Manager (lecture utilisateurs)
- `TECH_LEAD`: Tech Lead (gestion utilisateurs)
- `ADMIN`: Administrateur (accès complet)

### Configuration Keycloak

1. Accéder à http://localhost:8180
2. Login: admin/admin
3. Créer le realm `enterprise-realm`
4. Créer les clients:
   - `backend-client` (confidential)
   - `frontend-client` (public)
5. Créer les rôles: USER, ADMIN, TECH_LEAD, MANAGER
6. Créer des utilisateurs de test

### Headers de sécurité

- HTTPS obligatoire (TLS 1.3)
- HSTS
- X-Frame-Options: SAMEORIGIN
- X-Content-Type-Options: nosniff
- X-XSS-Protection
- CSP (Content Security Policy)

## 📊 Monitoring & Observabilité

### Métriques (Prometheus)

```bash
# Métriques backend
curl http://localhost:8080/actuator/prometheus
```

### Health Checks

```bash
# Backend
curl http://localhost:8080/actuator/health

# Liveness
curl http://localhost:8080/actuator/health/liveness

# Readiness
curl http://localhost:8080/actuator/health/readiness
```

### Logs

Les logs sont au format JSON structuré (Logstash encoder) pour faciliter l'ingestion dans ELK Stack.

## 🔄 CI/CD Pipeline

La pipeline GitLab CI/CD inclut :

1. **Build**: Compilation backend (Maven) et frontend (npm)
2. **Test**: Tests unitaires et d'intégration avec couverture
3. **Quality**: Analyse SonarQube
4. **Package**: Build des images Docker
5. **Deploy**: Déploiement automatique en dev, manuel en staging/prod

### Variables d'environnement requises

```bash
CI_REGISTRY
CI_REGISTRY_USER
CI_REGISTRY_PASSWORD
SONAR_TOKEN
KUBE_CONFIG (base64)
```

## 📚 Documentation

- **Architecture C4**: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Backend README**: [backend/README.md](backend/README.md)
- **Frontend README**: [frontend/README.md](frontend/README.md)

## 🛠️ Troubleshooting

### L'application ne démarre pas

```bash
# Vérifier les logs
docker-compose logs backend
docker-compose logs frontend

# Vérifier les services
docker-compose ps

# Recréer les conteneurs
docker-compose down
docker-compose up -d --build
```

### Erreurs de base de données

```bash
# Accéder à PostgreSQL
docker exec -it enterprise-postgres psql -U user -d appdb

# Vérifier les migrations Flyway
\dt flyway_schema_history
```

### Erreurs Keycloak

```bash
# Vérifier les logs
docker-compose logs keycloak

# Recréer la base Keycloak
docker-compose down
docker volume rm entreprise-grade_postgres_data
docker-compose up -d
```

## 👥 Équipe & Contribution

### Équipe

- **Tech Lead**: Responsable de l'architecture et de la qualité du code
- **Backend Developers**: Développement Spring Boot
- **Frontend Developers**: Développement Angular
- **DevOps**: Infrastructure et CI/CD

### Contribution

1. Créer une branche feature depuis `develop`
2. Développer et tester localement
3. Lancer les tests : `mvn test` et `npm test`
4. Créer une Pull Request vers `develop`
5. Code review obligatoire
6. Merge après validation

## 📄 License

Proprietary - Enterprise Team

## 📞 Support

Pour toute question ou problème :
- Email: tech@enterprise.com
- Slack: #enterprise-app
- GitLab Issues: https://gitlab.com/project/entreprise-grade/issues
