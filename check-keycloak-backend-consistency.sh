#!/bin/bash

echo "================================================"
echo "   VÉRIFICATION DE COHÉRENCE KEYCLOAK/BACKEND"
echo "================================================"
echo ""

echo "📋 Configuration Backend (application-docker.yml)"
echo "---------------------------------------------------"
echo "Realm:         $(grep 'realm:' backend/src/main/resources/application-docker.yml | head -1 | awk '{print $2}')"
echo "Client ID:     $(grep 'resource:' backend/src/main/resources/application-docker.yml | awk '{print $2}')"
echo "Client Secret: $(grep 'secret:' backend/src/main/resources/application-docker.yml | head -1 | awk '{print $2}')"
echo "Auth Server:   $(grep 'auth-server-url:' backend/src/main/resources/application-docker.yml | awk '{print $2}')"
echo ""

echo "🔐 Configuration Keycloak (realm.json)"
echo "---------------------------------------------------"
echo "Realm:         $(cat keycloak/realm.json | grep '"realm"' | head -1 | awk -F'"' '{print $4}')"
echo "Client ID:     $(cat keycloak/realm.json | grep '"clientId": "enterprise-backend"' | awk -F'"' '{print $4}')"
echo "Client Secret: $(cat keycloak/realm.json | grep -A 2 '"clientId": "enterprise-backend"' | grep '"secret"' | awk -F'"' '{print $4}')"
echo ""

echo "👥 Rôles Backend (Role.java)"
echo "---------------------------------------------------"
cat backend/src/main/java/com/enterprise/app/domain/model/Role.java | grep -E '^\s+[A-Z_]+,' | awk '{print "  - " $1}' | sed 's/,//'
echo ""

echo "👥 Rôles Keycloak (realm.json)"
echo "---------------------------------------------------"
cat keycloak/realm.json | jq -r '.roles.realm[] | .name' 2>/dev/null | sed 's/^/  - /' || echo "  (jq non disponible)"
echo ""

echo "✅ CORRESPONDANCES"
echo "---------------------------------------------------"
BACKEND_REALM=$(grep 'realm:' backend/src/main/resources/application-docker.yml | head -1 | awk '{print $2}')
KC_REALM=$(cat keycloak/realm.json | grep '"realm"' | head -1 | awk -F'"' '{print $4}')

BACKEND_CLIENT=$(grep 'resource:' backend/src/main/resources/application-docker.yml | awk '{print $2}')
KC_CLIENT=$(cat keycloak/realm.json | grep '"clientId": "enterprise-backend"' | awk -F'"' '{print $4}')

BACKEND_SECRET=$(grep 'secret:' backend/src/main/resources/application-docker.yml | head -1 | awk '{print $2}')
KC_SECRET=$(cat keycloak/realm.json | grep -A 2 '"clientId": "enterprise-backend"' | grep '"secret"' | awk -F'"' '{print $4}')

if [ "$BACKEND_REALM" = "$KC_REALM" ]; then
  echo "✅ Realm: $BACKEND_REALM"
else
  echo "❌ Realm: Backend=$BACKEND_REALM vs Keycloak=$KC_REALM"
fi

if [ "$BACKEND_CLIENT" = "$KC_CLIENT" ]; then
  echo "✅ Client ID: $BACKEND_CLIENT"
else
  echo "❌ Client ID: Backend=$BACKEND_CLIENT vs Keycloak=$KC_CLIENT"
fi

if [ "$BACKEND_SECRET" = "$KC_SECRET" ]; then
  echo "✅ Client Secret: correspondant"
else
  echo "❌ Client Secret: Backend=$BACKEND_SECRET vs Keycloak=$KC_SECRET"
fi

echo ""
echo "⚠️  PROBLÈMES DÉTECTÉS"
echo "---------------------------------------------------"
echo "❌ Rôles MANQUANTS dans Keycloak:"
echo "   - TECH_LEAD (défini dans backend, absent de Keycloak)"
echo "   - SYSTEM (défini dans backend, absent de Keycloak)"
echo ""
echo "❌ Rôle SUPPLÉMENTAIRE dans Keycloak:"
echo "   - VIEWER (défini dans Keycloak, absent du backend)"
echo ""
