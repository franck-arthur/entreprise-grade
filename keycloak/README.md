# Keycloak Configuration

This directory contains the Keycloak realm configuration that is automatically imported when the Keycloak container starts.

## Files

- **`realm.json`** - Complete realm configuration including clients, roles, and test users

## Realm: enterprise-realm

### Clients

1. **enterprise-backend** (Confidential)
   - Client Secret: `enterprise-backend-secret-key-2024`
   - Used by: Spring Boot backend API
   - Service account enabled

2. **enterprise-frontend** (Public)
   - PKCE enabled (SHA-256)
   - Used by: Angular frontend application

### Roles

- **ADMIN** - Full system access
- **MANAGER** - Elevated permissions
- **USER** - Standard user access
- **VIEWER** - Read-only access

### Test Users

| Username | Password | Roles | Email |
|----------|----------|-------|-------|
| admin | admin123 | ADMIN, MANAGER, USER | admin@enterprise.local |
| manager | manager123 | MANAGER, USER | manager@enterprise.local |
| user | user123 | USER | user@enterprise.local |
| viewer | viewer123 | VIEWER | viewer@enterprise.local |

⚠️ **WARNING**: These credentials are for development only. **CHANGE THEM IN PRODUCTION**.

## Automatic Import

The realm is automatically imported when Keycloak starts via:

```yaml
# docker-compose.yml
keycloak:
  command: start-dev --import-realm
  volumes:
    - ./keycloak/realm.json:/opt/keycloak/data/import/realm.json:ro
```

## Testing Authentication

Get an access token:

```bash
curl -X POST http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=enterprise-backend" \
  -d "client_secret=enterprise-backend-secret-key-2024" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password"
```

## Documentation

See [KEYCLOAK_CONFIGURATION.md](../docs/KEYCLOAK_CONFIGURATION.md) for complete documentation.

## Access

- **Admin Console**: http://localhost:8180/admin
  - Username: `admin`
  - Password: `admin`

- **Realm**: http://localhost:8180/realms/enterprise-realm
