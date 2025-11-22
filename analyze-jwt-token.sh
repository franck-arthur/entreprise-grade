#!/bin/bash

echo "=============================================="
echo "  ANALYSE TOKEN JWT"
echo "=============================================="
echo ""

if [ -z "$1" ]; then
    echo "Usage: $0 <jwt-token>"
    echo ""
    echo "Exemple:"
    echo "  $0 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...'"
    exit 1
fi

TOKEN="$1"

# Remove 'bearer ' prefix if present (case insensitive)
TOKEN=$(echo "$TOKEN" | sed -E 's/^[Bb]earer //g')

# Extract JWT parts
HEADER=$(echo "$TOKEN" | cut -d. -f1)
PAYLOAD=$(echo "$TOKEN" | cut -d. -f2)

# Decode (add padding if needed)
decode_base64() {
    local len=$((${#1} % 4))
    local result="$1"
    if [ $len -eq 2 ]; then result="$1"'=='
    elif [ $len -eq 3 ]; then result="$1"'='
    fi
    echo "$result" | tr '_-' '/+' | base64 -d 2>/dev/null
}

echo "📋 HEADER"
echo "----------------------------------------------"
decode_base64 "$HEADER" | python3 -m json.tool 2>/dev/null || decode_base64 "$HEADER" | jq . 2>/dev/null || decode_base64 "$HEADER"
echo ""

echo "📋 PAYLOAD"
echo "----------------------------------------------"
DECODED_PAYLOAD=$(decode_base64 "$PAYLOAD")
echo "$DECODED_PAYLOAD" | python3 -m json.tool 2>/dev/null || echo "$DECODED_PAYLOAD" | jq . 2>/dev/null || echo "$DECODED_PAYLOAD"
echo ""

echo "🔍 DIAGNOSTIC"
echo "----------------------------------------------"

# Extract key fields
ISS=$(echo "$DECODED_PAYLOAD" | grep -o '"iss":"[^"]*"' | cut -d'"' -f4)
AZP=$(echo "$DECODED_PAYLOAD" | grep -o '"azp":"[^"]*"' | cut -d'"' -f4)
EXP=$(echo "$DECODED_PAYLOAD" | grep -o '"exp":[0-9]*' | cut -d: -f2)
IAT=$(echo "$DECODED_PAYLOAD" | grep -o '"iat":[0-9]*' | cut -d: -f2)
ROLES=$(echo "$DECODED_PAYLOAD" | grep -o '"roles":\[[^]]*\]')

echo "Issuer (iss):        $ISS"
echo "Client (azp):        $AZP"
echo "Roles:               $ROLES"

if [ -n "$EXP" ]; then
    NOW=$(date +%s)
    if [ "$EXP" -lt "$NOW" ]; then
        echo "Expiration:          ❌ EXPIRÉ ($(date -d @$EXP 2>/dev/null || date -r $EXP 2>/dev/null))"
    else
        REMAINING=$((EXP - NOW))
        echo "Expiration:          ✅ Valide encore ${REMAINING}s ($(date -d @$EXP 2>/dev/null || date -r $EXP 2>/dev/null))"
    fi
fi

echo ""
echo "⚠️  PROBLÈMES DÉTECTÉS"
echo "----------------------------------------------"

# Check issuer
if echo "$ISS" | grep -q "localhost:8180"; then
    echo "❌ PROBLÈME D'ISSUER:"
    echo "   Token émis par:     $ISS"
    echo "   Backend Docker attend: http://keycloak:8080/realms/enterprise-realm"
    echo ""
    echo "   Explication:"
    echo "   Le token a été généré par Keycloak sur localhost:8180"
    echo "   Mais le backend Docker valide via keycloak:8080"
    echo "   → L'issuer ne correspond pas → 401 Unauthorized"
    echo ""
    echo "   Solutions:"
    echo "   1. Frontend doit utiliser http://keycloak:8080 (mode Docker)"
    echo "   2. Backend doit accepter localhost:8180 comme issuer"
    echo "   3. Utiliser le backend en mode 'dev' qui accepte localhost:8180"
elif echo "$ISS" | grep -q "keycloak:8080"; then
    echo "✅ Issuer correct pour backend Docker: $ISS"
else
    echo "⚠️  Issuer inattendu: $ISS"
fi

# Check client
if [ "$AZP" = "enterprise-frontend" ]; then
    echo "✅ Client ID correct: $AZP"
elif [ -n "$AZP" ]; then
    echo "❌ Client ID incorrect: $AZP (doit être: enterprise-frontend)"
else
    echo "⚠️  Client ID manquant"
fi

echo ""
echo "💡 RECOMMANDATIONS"
echo "----------------------------------------------"

if echo "$ISS" | grep -q "localhost:8180"; then
    echo "Option 1 (RECOMMANDÉ) : Utiliser le backend en mode dev"
    echo "  export SPRING_PROFILES_ACTIVE=dev"
    echo "  # Puis redémarrer le backend"
    echo ""
    echo "Option 2 : Reconfigurer le frontend pour Docker"
    echo "  # Dans frontend/src/environments/environment.ts:"
    echo "  keycloak: {"
    echo "    url: 'http://keycloak:8080',  // Au lieu de localhost:8180"
    echo "  }"
else
    echo "Token semble configuré pour Docker."
    echo "Vérifiez que le backend utilise le profil 'docker'."
fi
