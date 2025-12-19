#!/bin/sh

# Script d'initialisation de Garage S3 - exécuté dans le conteneur Garage
# Ce script utilise directement la commande garage locale

ACCESS_KEY="enterprise-app-key"
SECRET_KEY="enterprise-app-secret"
BUCKET_NAME="enterprise-storage"

echo "🚀 Initialisation de Garage S3..."

# Attendre que Garage soit complètement prêt
echo "⏳ Attente de la disponibilité de Garage..."
for i in $(seq 1 60); do
    if /garage -c /etc/garage.toml status >/dev/null 2>&1; then
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
/garage -c /etc/garage.toml status

# 2. Récupérer l'ID du nœud
NODE_ID=$(/garage -c /etc/garage.toml status 2>/dev/null | grep -E "^[a-f0-9]{16}" | head -1 | awk '{print $1}')

if [ -z "$NODE_ID" ]; then
    # Fallback: essayer de récupérer depuis les logs
    NODE_ID=$(/garage -c /etc/garage.toml status 2>/dev/null | awk '/HEALTHY NODES/,/^$/ {if(/^[a-f0-9]{16}/) print $1}' | head -1)
fi

echo "📋 ID du nœud détecté: $NODE_ID"

if [ -z "$NODE_ID" ]; then
    echo "❌ Impossible de récupérer l'ID du nœud"
    exit 1
fi

# 3. Vérifier si le nœud a un rôle assigné et l'assigner si nécessaire
if /garage -c /etc/garage.toml status | grep -q "NO ROLE ASSIGNED"; then
    echo "⚠️  Le nœud n'a pas de rôle assigné. Attribution en cours..."

    # Assigner un rôle au nœud
    /garage -c /etc/garage.toml layout assign "$NODE_ID" -z default -c 1G

    # Appliquer le layout
    echo "📐 Application du layout..."
    /garage -c /etc/garage.toml layout apply --version 1

    # Attendre l'application
    echo "⏳ Attente de l'application du layout..."
    for i in $(seq 1 30); do
        if ! /garage -c /etc/garage.toml status | grep -q "NO ROLE ASSIGNED"; then
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
if ! /garage -c /etc/garage.toml key list | grep -q "enterprise-app"; then
    echo "🔧 Création de la clé d'accès..."
    /garage -c /etc/garage.toml key create enterprise-app

    # Récupérer l'ID de la clé
    sleep 2
    KEY_ID=$(/garage -c /etc/garage.toml key list | grep enterprise-app | awk '{print $1}')

    if [ -n "$KEY_ID" ]; then
        echo "📋 ID de la clé: $KEY_ID"

        # Configurer les credentials
        /garage -c /etc/garage.toml key import "$KEY_ID" "$SECRET_KEY" -n enterprise-app --yes
        echo "✅ Credentials configurés"
    else
        echo "❌ Impossible de récupérer l'ID de la clé"
        exit 1
    fi
else
    echo "✅ La clé d'accès existe déjà"
    KEY_ID=$(/garage -c /etc/garage.toml key list | grep enterprise-app | awk '{print $1}')
fi

# 5. Vérifier et créer le bucket si nécessaire
echo "🪣 Vérification du bucket..."
if ! /garage -c /etc/garage.toml bucket list | grep -q "$BUCKET_NAME"; then
    echo "🔧 Création du bucket..."
    /garage -c /etc/garage.toml bucket create "$BUCKET_NAME"

    # Donner les permissions
    echo "🔐 Attribution des permissions..."
    /garage -c /etc/garage.toml bucket allow --read --write "$BUCKET_NAME" --key "$KEY_ID"
    echo "✅ Bucket créé et permissions attribuées"
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