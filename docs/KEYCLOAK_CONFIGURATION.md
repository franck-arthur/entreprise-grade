# Configuration Keycloak - Import Automatique du Realm

## Vue d'ensemble

Ce document explique la configuration automatisée de Keycloak via l'import d'un fichier `realm.json` au démarrage du conteneur Docker. Cette approche garantit une configuration reproductible et facilite le déploiement dans différents environnements.

## Architecture

```
keycloak/
└── realm.json              # Configuration complète du realm

docker-compose.yml          # Monte realm.json dans Keycloak
```

## Fichier realm.json

### Localisation

`keycloak/realm.json`

### Contenu

Le fichier realm.json contient la configuration complète pour le realm `enterprise-realm` :

#### 1. Configuration du Realm

```json
{
  "realm": "enterprise-realm",
  "enabled": true,
  "displayName": "Enterprise Application Realm"
}
```

**Paramètres de sécurité :**
- **Brute Force Protection** : 5 tentatives échouées → verrouillage 15 minutes
- **Password Policy** : Minimum 8 caractères, 1 chiffre, 1 majuscule, 1 minuscule
- **SSL Required** : "external" (SSL requis pour connexions externes)
- **Token Lifespans** :
  - Access Token : 5 minutes (300s)
  - SSO Session Idle : 30 minutes (1800s)
  - SSO Session Max : 10 heures (36000s)

**Internationalisation :**
- Langues supportées : Anglais (en), Français (fr)
- Langue par défaut : Anglais

#### 2. Rôles Définis

| Rôle | Description | Permissions |
|------|-------------|-------------|
| **ADMIN** | Administrateur système | Accès complet |
| **MANAGER** | Gestionnaire | Permissions élevées |
| **USER** | Utilisateur standard | Accès de base |
| **VIEWER** | Lecteur seul | Lecture uniquement |

#### 3. Clients OAuth2/OIDC

##### Backend API (`enterprise-backend`)

```json
{
  "clientId": "enterprise-backend",
  "clientAuthenticatorType": "client-secret",
  "secret": "enterprise-backend-secret-key-2024",
  "publicClient": false,
  "serviceAccountsEnabled": true,
  "directAccessGrantsEnabled": true
}
```

**Configuration :**
- **Client Secret** : `enterprise-backend-secret-key-2024` ⚠️ À changer en production
- **Service Account** : Activé (pour appels machine-to-machine)
- **Direct Access Grants** : Activé (Resource Owner Password Credentials)
- **Access Token Lifespan** : 5 minutes

**Redirect URIs :**
- `http://localhost:8080/*`
- `http://localhost:4200/*`
- `http://localhost/*`

**Web Origins :**
- `http://localhost:8080`
- `http://localhost:4200`
- `http://localhost`

##### Frontend Application (`enterprise-frontend`)

```json
{
  "clientId": "enterprise-frontend",
  "publicClient": true,
  "directAccessGrantsEnabled": true,
  "pkce.code.challenge.method": "S256"
}
```

**Configuration :**
- **Public Client** : Oui (pas de secret, application frontend)
- **PKCE** : Activé avec SHA-256 (sécurité accrue)
- **Direct Access Grants** : Activé

**Redirect URIs :**
- `http://localhost:4200/*`
- `http://localhost/*`

#### 4. Utilisateurs de Test

| Username | Password | Email | Rôles | Description |
|----------|----------|-------|-------|-------------|
| **admin** | `admin123` | admin@enterprise.local | ADMIN, MANAGER, USER | Administrateur système |
| **manager** | `manager123` | manager@enterprise.local | MANAGER, USER | Gestionnaire |
| **user** | `user123` | user@enterprise.local | USER | Utilisateur standard |
| **viewer** | `viewer123` | viewer@enterprise.local | VIEWER | Lecteur seul |

⚠️ **IMPORTANT** : Ces mots de passe sont pour le développement uniquement. **CHANGEZ-LES EN PRODUCTION**.

#### 5. Scopes et Claims

**Scope "roles" personnalisé :**
- Ajoute les rôles dans les tokens JWT
- Claims ajoutés :
  - `realm_access.roles` - Rôles du realm
  - `resource_access.${client_id}.roles` - Rôles spécifiques au client

**Mappers de protocole :**
- `realm roles` - Mappe les rôles du realm dans le token
- `client roles` - Mappe les rôles des clients

#### 6. Sécurité Avancée

**Algorithmes de signature :**
- RSA-256 pour les tokens (clé de 2048 bits)
- HMAC-SHA256 pour les secrets

**Protection contre les attaques :**
- **Brute Force** : Activée
- **Max Failures** : 5 tentatives
- **Wait Time** : 15 minutes (900s)
- **Quick Login Window** : 1 seconde

**Password Policy :**
```
length(8) and digits(1) and lowerCase(1) and upperCase(1)
```
- Minimum 8 caractères
- Au moins 1 chiffre
- Au moins 1 minuscule
- Au moins 1 majuscule

#### 7. Événements et Audit

**Events Enabled :**
- LOGIN / LOGIN_ERROR
- LOGOUT / LOGOUT_ERROR
- REGISTER / REGISTER_ERROR
- UPDATE_PASSWORD / UPDATE_PASSWORD_ERROR
- UPDATE_PROFILE / UPDATE_PROFILE_ERROR

**Admin Events :**
- Activés avec détails complets
- Tous les changements administratifs sont loggés

## Configuration Docker Compose

### Modifications apportées

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  container_name: enterprise-keycloak
  command: start-dev --import-realm        # ← Import automatique
  volumes:
    - ./keycloak/realm.json:/opt/keycloak/data/import/realm.json:ro  # ← Montage du realm
  healthcheck:
    start_period: 90s                      # ← Plus de temps pour l'import
```

**Changements clés :**
1. **Command** : Ajout de `--import-realm` pour activer l'import
2. **Volume** : Montage de `realm.json` en lecture seule (`:ro`)
3. **Start Period** : Augmenté à 90s pour laisser le temps à l'import

### Mécanisme d'import

Lors du démarrage du conteneur Keycloak :

1. **Vérification** : Keycloak vérifie `/opt/keycloak/data/import/`
2. **Détection** : Trouve `realm.json`
3. **Import** : Crée le realm et toutes les ressources
4. **Idempotence** : Si le realm existe déjà, l'import est ignoré

⚠️ **Note** : L'import ne met PAS à jour un realm existant, il le crée uniquement s'il n'existe pas.

## Utilisation

### Premier démarrage

```bash
# Démarrer tous les services
docker-compose up -d

# Vérifier les logs Keycloak
docker-compose logs -f keycloak

# Attendre le message : "Imported realm 'enterprise-realm'"
```

### Accès à Keycloak

**Admin Console :**
- URL : http://localhost:8180/admin
- Username : `admin`
- Password : `admin`

**Realm "enterprise-realm" :**
- URL : http://localhost:8180/realms/enterprise-realm

### Tester l'authentification

#### 1. Via Password Grant (Direct Access)

```bash
curl -X POST http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=enterprise-backend" \
  -d "client_secret=enterprise-backend-secret-key-2024" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password"
```

**Réponse attendue :**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "...",
  "scope": "profile email"
}
```

#### 2. Décoder le JWT

```bash
# Récupérer le token
TOKEN=$(curl -s -X POST http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/token \
  -d "client_id=enterprise-backend" \
  -d "client_secret=enterprise-backend-secret-key-2024" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password" | jq -r .access_token)

# Décoder le payload (base64)
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq
```

**Payload JWT attendu :**
```json
{
  "exp": 1704070000,
  "iat": 1704069700,
  "auth_time": 1704069700,
  "jti": "...",
  "iss": "http://localhost:8180/realms/enterprise-realm",
  "aud": "account",
  "sub": "...",
  "typ": "Bearer",
  "azp": "enterprise-backend",
  "session_state": "...",
  "realm_access": {
    "roles": ["ADMIN", "MANAGER", "USER"]
  },
  "scope": "profile email",
  "email_verified": true,
  "name": "System Administrator",
  "preferred_username": "admin",
  "given_name": "System",
  "family_name": "Administrator",
  "email": "admin@enterprise.local"
}
```

### Tester avec chaque utilisateur

```bash
# Admin (tous les rôles)
curl -s -X POST http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/token \
  -d "client_id=enterprise-backend" \
  -d "client_secret=enterprise-backend-secret-key-2024" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password" | jq .

# Manager
curl -s -X POST http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/token \
  -d "client_id=enterprise-backend" \
  -d "client_secret=enterprise-backend-secret-key-2024" \
  -d "username=manager" \
  -d "password=manager123" \
  -d "grant_type=password" | jq .

# User
curl -s -X POST http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/token \
  -d "client_id=enterprise-backend" \
  -d "client_secret=enterprise-backend-secret-key-2024" \
  -d "username=user" \
  -d "password=user123" \
  -d "grant_type=password" | jq .

# Viewer
curl -s -X POST http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/token \
  -d "client_id=enterprise-backend" \
  -d "client_secret=enterprise-backend-secret-key-2024" \
  -d "username=viewer" \
  -d "password=viewer123" \
  -d "grant_type=password" | jq .
```

## Modification de la configuration

### Mise à jour du realm

Pour modifier la configuration existante :

1. **Via Admin Console** (recommandé pour le développement) :
   - Se connecter à http://localhost:8180/admin
   - Modifier le realm `enterprise-realm`
   - Exporter le realm : Realm Settings → Action → Partial export

2. **Via fichier realm.json** :
   - Modifier `keycloak/realm.json`
   - **Supprimer le volume Keycloak** (si existant) :
     ```bash
     docker-compose down
     docker volume rm entreprise-grade_keycloak_data  # Si vous avez un volume persistant
     ```
   - Redémarrer :
     ```bash
     docker-compose up -d
     ```

### Exporter la configuration actuelle

```bash
# Se connecter au conteneur Keycloak
docker exec -it enterprise-keycloak bash

# Exporter le realm
/opt/keycloak/bin/kc.sh export \
  --file /tmp/realm-export.json \
  --realm enterprise-realm \
  --users realm_file

# Copier le fichier exporté
docker cp enterprise-keycloak:/tmp/realm-export.json ./keycloak/realm-backup.json
```

## Production Checklist

Avant de déployer en production, **CHANGEZ** :

- [ ] **Client Secret** (`enterprise-backend-secret-key-2024`) → Secret complexe
- [ ] **Tous les mots de passe utilisateurs**
- [ ] **Admin Keycloak** password (`admin/admin`)
- [ ] **SSL Required** → "all" (forcer HTTPS partout)
- [ ] **Redirect URIs** → URLs de production
- [ ] **Web Origins** → Origines de production
- [ ] **Password Policy** → Politique plus stricte si nécessaire
- [ ] **Token Lifespans** → Ajuster selon les besoins de sécurité
- [ ] Désactiver `start-dev` → utiliser `start` avec config production
- [ ] Activer HTTPS avec certificats
- [ ] Configurer la base de données PostgreSQL dédiée pour Keycloak
- [ ] Activer les backups réguliers de la DB Keycloak

## Intégration avec Spring Boot

Le backend Spring Boot est déjà configuré pour utiliser ce realm :

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/enterprise-realm
          jwk-set-uri: http://keycloak:8080/realms/enterprise-realm/protocol/openid-connect/certs
```

Les rôles sont automatiquement extraits et mappés grâce aux mappers configurés dans le realm.

## Intégration avec Angular

Le frontend Angular peut utiliser des bibliothèques comme `keycloak-angular` ou `angular-oauth2-oidc` :

```typescript
// Configuration OIDC
const config = {
  issuer: 'http://localhost:8180/realms/enterprise-realm',
  clientId: 'enterprise-frontend',
  redirectUri: window.location.origin,
  scope: 'openid profile email roles',
  responseType: 'code',
  usePkce: true
};
```

## Troubleshooting

### Le realm n'est pas importé

**Vérifier les logs :**
```bash
docker-compose logs keycloak | grep -i import
```

**Solutions :**
- Vérifier que `realm.json` existe dans `keycloak/`
- Vérifier les permissions du fichier
- Vérifier la syntaxe JSON avec `jq . keycloak/realm.json`
- Augmenter le `start_period` dans healthcheck

### Erreur "Realm already exists"

C'est normal ! L'import est **idempotent** - il ne crée le realm que s'il n'existe pas. Aucune action nécessaire.

### Client secret invalide

Vérifier que le secret dans votre application correspond à celui du realm.json :
```
enterprise-backend-secret-key-2024
```

### Token JWT invalide

**Causes possibles :**
- Token expiré (5 minutes)
- Issuer URI incorrect
- JWK Set URI incorrect
- Clés de signature changées

**Solution :**
- Redémarrer Keycloak
- Obtenir un nouveau token
- Vérifier les URLs dans la config Spring Boot

## Références

- [Keycloak Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)
- [Keycloak Docker Image](https://quay.io/repository/keycloak/keycloak)
- [Realm Import/Export](https://www.keycloak.org/docs/latest/server_admin/#_export_import)
- [OAuth 2.0 Grant Types](https://oauth.net/2/grant-types/)
- [JWT.io](https://jwt.io/) - Décodeur JWT en ligne

---

**Auteur :** Équipe Entreprise Grade
**Dernière mise à jour :** 2024-01-20
