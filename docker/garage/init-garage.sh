#!/bin/bash

# Script d'initialisation de Garage S3
# Ce script configure le cluster, les clés d'accès et crée les buckets nécessaires

GARAGE_HOST="http://localhost:3900"
ADMIN_TOKEN="WE8bo3VL/Rf13Bu+h7vSX8P/yrbwaT3sJ6LQ2W0zrXU="
ACCESS_KEY="enterprise-app-key"
SECRET_KEY="enterprise-app-secret"
BUCKET_NAME="enterprise-storage"

echo "🚀 Initialisation de Garage S3..."

# Attendre que Garage soit prêt
echo "⏳ Attente de la disponibilité de Garage..."
until curl -s $GARAGE_HOST > /dev/null; do
    echo "Garage n'est pas encore prêt, attente..."
    sleep 2
done

echo "✅ Garage est disponible!"

# 1. Vérifier le statut du cluster
echo "🔍 Vérification du statut du cluster..."
docker exec enterprise-garage /garage -c /etc/garage.toml status

# 2. Récupérer l'ID du nœud
NODE_ID=$(docker exec enterprise-garage /garage -c /etc/garage.toml status | grep -E "(NO ROLE ASSIGNED|HEALTHY NODES)" -A 1 | tail -n 1 | awk '{print $1}')
echo "📋 ID du nœud: $NODE_ID"

# Vérifier si le nœud a déjà un rôle
HAS_ROLE=$(docker exec enterprise-garage /garage -c /etc/garage.toml status | grep "$NODE_ID" | grep -v "NO ROLE ASSIGNED" | wc -l)

# 3. Assigner un rôle au nœud si nécessaire
if [ "$HAS_ROLE" -eq 0 ] && [ ! -z "$NODE_ID" ]; then
    echo "🔧 Attribution du rôle de stockage au nœud..."
    docker exec enterprise-garage /garage -c /etc/garage.toml layout assign $NODE_ID -z default -c 1G

    # 4. Appliquer la configuration du layout
    echo "📐 Application de la configuration du layout..."
    docker exec enterprise-garage /garage -c /etc/garage.toml layout apply --version 1

    # Attendre que le layout soit appliqué
    echo "⏳ Attente de l'application du layout..."
    sleep 15

    # Vérifier que le layout est bien appliqué
    echo "🔍 Vérification du layout..."
    docker exec enterprise-garage /garage -c /etc/garage.toml layout show
else
    echo "ℹ️  Le nœud a déjà un rôle assigné"
fi

# 5. Créer une clé d'accès
echo "🔑 Création de la clé d'accès..."
docker exec enterprise-garage /garage -c /etc/garage.toml key create enterprise-app

# 6. Récupérer l'ID de la clé créée
KEY_ID=$(docker exec enterprise-garage /garage -c /etc/garage.toml key list | grep enterprise-app | awk '{print $1}')
echo "📋 ID de la clé: $KEY_ID"

# 7. Importer les credentials pour la clé
echo "🔧 Configuration des credentials..."
docker exec enterprise-garage /garage -c /etc/garage.toml key import \
    $KEY_ID \
    $SECRET_KEY \
    -n enterprise-app \
    --yes

# 8. Créer un bucket
echo "🪣 Création du bucket '$BUCKET_NAME'..."
docker exec enterprise-garage /garage -c /etc/garage.toml bucket create $BUCKET_NAME

# 9. Donner les permissions sur le bucket
echo "🔐 Attribution des permissions..."
docker exec enterprise-garage /garage -c /etc/garage.toml bucket allow \
    --read \
    --write \
    $BUCKET_NAME \
    --key $KEY_ID

echo ""
echo "🎉 Configuration terminée!"
echo ""
echo "📝 Informations de connexion S3:"
echo "  - Endpoint: $GARAGE_HOST"
echo "  - Access Key: $ACCESS_KEY"
echo "  - Secret Key: $SECRET_KEY"
echo "  - Bucket: $BUCKET_NAME"
echo "  - Region: garage"
echo ""
echo "🌐 Interface admin disponible sur: http://localhost:3902"
echo "🔑 Token admin: $ADMIN_TOKEN"