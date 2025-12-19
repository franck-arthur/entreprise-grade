# Garage S3 - Configuration Locale

Ce répertoire contient la configuration pour le stockage S3 local utilisant Garage.

## 🚀 Démarrage

1. **Lancer les services:**
   ```bash
   docker-compose up -d garage
   ```

2. **Initialiser Garage:**
   ```bash
   ./docker/garage/init-garage.sh
   ```

3. **Tester la configuration:**
   ```bash
   ./docker/garage/test-s3.sh
   ```

## 📁 Fichiers

- `garage.toml` - Configuration principale de Garage
- `init-garage.sh` - Script d'initialisation des clés et buckets
- `test-s3.sh` - Script de test de la configuration
- `.env.s3` - Variables d'environnement pour l'application

## 🔧 Configuration

### Endpoints disponibles

- **API S3:** http://localhost:3900
- **Interface admin:** http://localhost:3902
- **RPC:** localhost:3901

### Credentials par défaut

- **Access Key:** `enterprise-app-key`
- **Secret Key:** `enterprise-app-secret`
- **Bucket:** `enterprise-storage`
- **Region:** `garage`

### Token admin

- **Admin Token:** `garageadmintoken123`

## 🐳 Intégration Docker

Le service Garage est automatiquement intégré au docker-compose.yml principal avec:
- Healthcheck automatique
- Volume persistant
- Réseau partagé avec les autres services

## 📝 Utilisation dans l'application

Pour utiliser S3 dans votre application Spring Boot, ajoutez ces variables d'environnement:

```yaml
environment:
  AWS_ACCESS_KEY_ID: enterprise-app-key
  AWS_SECRET_ACCESS_KEY: enterprise-app-secret
  AWS_ENDPOINT_URL: http://garage:3900
  AWS_S3_BUCKET: enterprise-storage
  AWS_REGION: garage
```

## 🔍 Monitoring

- Interface web: http://localhost:3902
- Métriques disponibles avec le token: `metricstoken123`