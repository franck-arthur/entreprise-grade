#!/bin/sh

# Script d'initialisation de Garage S3 pour Docker Compose
# Ce script utilise l'API admin de Garage via HTTP

ACCESS_KEY="enterprise-app-key"
SECRET_KEY="enterprise-app-secret"
BUCKET_NAME="enterprise-storage"
GARAGE_HOST="enterprise-garage"
ADMIN_API_PORT="3903"
ADMIN_TOKEN="WE8bo3VL/Rf13Bu+h7vSX8P/yrbwaT3sJ6LQ2W0zrXU="

echo "🚀 Initialisation de Garage S3..."

# Fonction pour appeler l'API admin
garage_api() {
    local endpoint="$1"
    local method="${2:-GET}"
    local data="$3"

    if [ -n "$data" ]; then
        wget -qO- --method="$method" \
            --header="Authorization: Bearer $ADMIN_TOKEN" \
            --header="Content-Type: application/json" \
            --body-data="$data" \
            "http://$GARAGE_HOST:$ADMIN_API_PORT$endpoint" 2>/dev/null
    else
        wget -qO- --method="$method" \
            --header="Authorization: Bearer $ADMIN_TOKEN" \
            "http://$GARAGE_HOST:$ADMIN_API_PORT$endpoint" 2>/dev/null
    fi
}

# Attendre que Garage soit prêt
echo "⏳ Attente de la disponibilité de Garage..."
for i in $(seq 1 60); do
    if garage_api "/status" >/dev/null 2>&1; then
        echo "✅ Garage est disponible!"
        break
    elif [ $i -eq 60 ]; then
        echo "❌ Garage n'est pas disponible après 60 tentatives"
        exit 1
    else
        echo "Tentative $i/60..."
        sleep 2
    fi
done

# 1. Afficher le statut du cluster
echo "🔍 Vérification du statut du cluster..."
STATUS_RESPONSE=$(garage_api "/status")
echo "$STATUS_RESPONSE" | head -20

# 2. Récupérer l'ID du nœud depuis le statut
NODE_ID=$(echo "$STATUS_RESPONSE" | grep -E '"id"' | head -1 | sed 's/.*"id":"\([^"]*\)".*/\1/')

echo "📋 ID du nœud détecté: $NODE_ID"

if [ -z "$NODE_ID" ]; then
    echo "❌ Impossible de récupérer l'ID du nœud"
    exit 1
fi

# 3. Vérifier le layout actuel
LAYOUT_RESPONSE=$(garage_api "/layout")
if echo "$LAYOUT_RESPONSE" | grep -q "\"zone\":null"; then
    echo "⚠️  Le nœud n'a pas de rôle assigné. Attribution en cours..."

    # Assigner un rôle au nœud
    ASSIGN_DATA="{\"id\":\"$NODE_ID\",\"zone\":\"default\",\"capacity\":1073741824,\"tags\":[]}"
    echo "📐 Attribution du rôle..."
    garage_api "/layout" "POST" "$ASSIGN_DATA"

    # Appliquer le layout (version 1)
    echo "📐 Application du layout..."
    garage_api "/layout/apply" "POST" '{"version":1}'

    # Attendre l'application
    echo "⏳ Attente de l'application du layout..."
    for i in $(seq 1 30); do
        LAYOUT_CHECK=$(garage_api "/layout")
        if ! echo "$LAYOUT_CHECK" | grep -q "\"zone\":null"; then
            echo "✅ Layout appliqué (${i}s)"
            break
        elif [ $i -eq 30 ]; then
            echo "⚠️  Layout en cours d'application (peut prendre plus de temps)"
        fi
        sleep 1
    done
else
    echo "✅ Le nœud a déjà un rôle assigné"
fi

# 4. Vérifier et créer la clé si nécessaire
echo "🔑 Vérification de la clé d'accès..."
KEYS_RESPONSE=$(garage_api "/key")
if ! echo "$KEYS_RESPONSE" | grep -q "enterprise-app"; then
    echo "🔧 Création de la clé d'accès..."
    KEY_DATA="{\"name\":\"enterprise-app\"}"
    KEY_CREATION=$(garage_api "/key" "POST" "$KEY_DATA")

    # Récupérer l'ID de la clé créée
    sleep 2
    KEYS_RESPONSE=$(garage_api "/key")
    KEY_ID=$(echo "$KEYS_RESPONSE" | grep -A 10 -B 10 "enterprise-app" | grep '"accessKeyId"' | sed 's/.*"accessKeyId":"\([^"]*\)".*/\1/')

    if [ -n "$KEY_ID" ]; then
        echo "📋 ID de la clé: $KEY_ID"

        # Configurer le secret key
        SECRET_DATA="{\"secretAccessKey\":\"$SECRET_KEY\"}"
        garage_api "/key/$KEY_ID" "POST" "$SECRET_DATA"
        echo "✅ Credentials configurés"
    else
        echo "❌ Impossible de récupérer l'ID de la clé"
        exit 1
    fi
else
    echo "✅ La clé d'accès existe déjà"
    KEY_ID=$(echo "$KEYS_RESPONSE" | grep -A 10 -B 10 "enterprise-app" | grep '"accessKeyId"' | sed 's/.*"accessKeyId":"\([^"]*\)".*/\1/')
fi

# 5. Vérifier et créer le bucket si nécessaire
echo "🪣 Vérification du bucket..."
BUCKET_RESPONSE=$(garage_api "/bucket")
if ! echo "$BUCKET_RESPONSE" | grep -q "$BUCKET_NAME"; then
    echo "🔧 Création du bucket..."
    BUCKET_DATA="{\"globalAlias\":\"$BUCKET_NAME\"}"
    BUCKET_CREATION=$(garage_api "/bucket" "POST" "$BUCKET_DATA")

    # Récupérer l'ID du bucket
    sleep 2
    BUCKET_RESPONSE=$(garage_api "/bucket")
    BUCKET_ID=$(echo "$BUCKET_RESPONSE" | grep -A 10 -B 10 "$BUCKET_NAME" | grep '"id"' | sed 's/.*"id":"\([^"]*\)".*/\1/')

    if [ -n "$BUCKET_ID" ]; then
        # Donner les permissions
        echo "🔐 Attribution des permissions..."
        PERMISSION_DATA="{\"accessKeyId\":\"$KEY_ID\",\"permissions\":{\"read\":true,\"write\":true,\"owner\":false}}"
        garage_api "/bucket/$BUCKET_ID/allow" "POST" "$PERMISSION_DATA"
        echo "✅ Bucket créé et permissions attribuées"
    else
        echo "❌ Impossible de récupérer l'ID du bucket"
        exit 1
    fi
else
    echo "✅ Le bucket existe déjà"
fi

echo ""
echo "🎉 Configuration terminée!"
echo ""
echo "📝 Informations de connexion S3:"
echo "  - Endpoint: http://localhost:3900"
echo "  - Access Key: $ACCESS_KEY"
echo "  - Secret Key: $SECRET_KEY"
echo "  - Bucket: $BUCKET_NAME"
echo "  - Region: garage"
echo ""
echo "🌐 Interface admin disponible sur: http://localhost:3902"