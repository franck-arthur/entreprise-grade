#!/bin/bash

echo "=============================================="
echo "  DIAGNOSTIC BACKEND JWT VALIDATION"
echo "=============================================="
echo ""

echo "📋 Configuration Backend (application-dev.yml)"
echo "----------------------------------------------"
grep -A 4 "jwt:" backend/src/main/resources/application-dev.yml
echo ""

echo "📋 Configuration Backend (application-docker.yml)"
echo "----------------------------------------------"
grep -A 4 "jwt:" backend/src/main/resources/application-docker.yml
echo ""

echo "🔍 Test de connectivité Keycloak"
echo "----------------------------------------------"

# Test localhost:8180 (dev)
echo "Test 1: http://localhost:8180/realms/enterprise-realm (profil dev)"
if curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://localhost:8180/realms/enterprise-realm" | grep -q "200"; then
    echo "  ✅ Keycloak accessible sur localhost:8180"
else
    echo "  ❌ Keycloak NON accessible sur localhost:8180"
    echo "     → Le backend en mode 'dev' ne peut pas valider les tokens !"
fi
echo ""

# Test JWK endpoint
echo "Test 2: http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/certs"
if curl -s --connect-timeout 3 "http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/certs" | grep -q "keys"; then
    echo "  ✅ JWK endpoint accessible"
else
    echo "  ❌ JWK endpoint NON accessible"
    echo "     → Le backend ne peut pas récupérer les clés publiques pour valider les tokens JWT !"
fi
echo ""

echo "💡 DIAGNOSTIC"
echo "----------------------------------------------"
echo "Pour que le backend valide les tokens JWT, il DOIT pouvoir accéder à:"
echo "  1. Issuer URI: http://localhost:8180/realms/enterprise-realm"
echo "  2. JWK Set URI: http://localhost:8180/realms/enterprise-realm/protocol/openid-connect/certs"
echo ""
echo "Si Keycloak n'est pas accessible, le backend rejette TOUS les tokens avec 401."
echo ""
echo "Solutions:"
echo "  • En local: Démarrer Keycloak sur localhost:8180"
echo "  • En Docker: Tout démarrer avec 'docker compose up -d'"
echo ""

echo "📝 Vérifier le profil Spring actif"
echo "----------------------------------------------"
if [ -z "$SPRING_PROFILES_ACTIVE" ]; then
    echo "Variable SPRING_PROFILES_ACTIVE: non définie → profil 'dev' par défaut"
    echo "  → Backend essaie de contacter http://localhost:8180"
else
    echo "Variable SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
    if [ "$SPRING_PROFILES_ACTIVE" = "docker" ]; then
        echo "  → Backend essaie de contacter http://keycloak:8080"
    elif [ "$SPRING_PROFILES_ACTIVE" = "dev" ]; then
        echo "  → Backend essaie de contacter http://localhost:8180"
    fi
fi
