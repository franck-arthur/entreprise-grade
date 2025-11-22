#!/bin/bash

echo "=============================================="
echo "  TEST AUTHENTIFICATION BACKEND"
echo "=============================================="
echo ""

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Keycloak accessible
echo "Test 1: Keycloak accessible"
if curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "http://localhost:8180/realms/enterprise-realm" | grep -q "200"; then
    echo -e "  ${GREEN}✅ Keycloak accessible${NC}"
    KEYCLOAK_OK=true
else
    echo -e "  ${RED}❌ Keycloak NON accessible${NC}"
    echo "     Démarrez avec: docker compose up -d keycloak"
    KEYCLOAK_OK=false
fi
echo ""

# Test 2: Backend accessible
echo "Test 2: Backend accessible"
BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "http://localhost:8080/actuator/health" 2>/dev/null)
if [ "$BACKEND_STATUS" = "200" ]; then
    echo -e "  ${GREEN}✅ Backend accessible (http://localhost:8080)${NC}"
    BACKEND_OK=true
elif [ "$BACKEND_STATUS" = "000" ]; then
    echo -e "  ${RED}❌ Backend NON accessible${NC}"
    echo "     Démarrez avec: docker compose up -d backend"
    echo "     OU lancez depuis votre IDE"
    BACKEND_OK=false
else
    echo -e "  ${YELLOW}⚠️  Backend répond mais status: $BACKEND_STATUS${NC}"
    BACKEND_OK=true
fi
echo ""

# Test 3: Appel API sans token
echo "Test 3: Appel API sans token (doit retourner 401)"
API_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "http://localhost:8080/api/v1/users" 2>/dev/null)
if [ "$API_STATUS" = "401" ]; then
    echo -e "  ${GREEN}✅ 401 Unauthorized (normal sans token)${NC}"
elif [ "$API_STATUS" = "000" ]; then
    echo -e "  ${RED}❌ Backend non accessible${NC}"
else
    echo -e "  ${YELLOW}⚠️  Status inattendu: $API_STATUS${NC}"
fi
echo ""

# Test 4: Avec token (si fourni)
if [ -n "$1" ]; then
    echo "Test 4: Appel API avec token fourni"
    TOKEN="$1"
    RESPONSE=$(curl -s -w "\n%{http_code}" --connect-timeout 5 \
        -H "Authorization: Bearer $TOKEN" \
        "http://localhost:8080/api/v1/users" 2>/dev/null)

    STATUS=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | head -n -1)

    if [ "$STATUS" = "200" ]; then
        echo -e "  ${GREEN}✅ 200 OK - Token valide !${NC}"
        echo "  Réponse: $(echo "$BODY" | head -c 100)..."
    elif [ "$STATUS" = "401" ]; then
        echo -e "  ${RED}❌ 401 Unauthorized${NC}"
        echo "  Causes possibles:"
        echo "    1. Token expiré"
        echo "    2. Keycloak non accessible par le backend"
        echo "    3. Token pour le mauvais client (doit être pour enterprise-frontend)"
        echo "    4. Realm ou audience incorrects"
    else
        echo -e "  ${YELLOW}⚠️  Status: $STATUS${NC}"
        echo "  Réponse: $BODY"
    fi
else
    echo "Test 4: Pas de token fourni"
    echo "  Usage: $0 <votre-token-jwt>"
    echo "  Récupérez un token depuis la console navigateur après connexion"
fi
echo ""

# Résumé
echo "=============================================="
echo "  RÉSUMÉ"
echo "=============================================="
if [ "$KEYCLOAK_OK" = true ] && [ "$BACKEND_OK" = true ]; then
    echo -e "${GREEN}✅ Tous les services sont accessibles${NC}"
    echo ""
    echo "Pour tester avec un token:"
    echo "  1. Connectez-vous à http://localhost"
    echo "  2. Ouvrez la console (F12) → Network"
    echo "  3. Copiez le header 'Authorization: Bearer xxx'"
    echo "  4. Lancez: $0 'votre-token'"
else
    echo -e "${RED}❌ Des services ne sont pas accessibles${NC}"
    echo ""
    echo "Démarrez tous les services:"
    echo "  docker compose up -d"
fi
