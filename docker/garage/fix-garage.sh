#!/bin/bash

# Script de réparation Garage S3
# Résout les problèmes de layout et d'API obsolète

set -e  # Arrêter le script en cas d'erreur

echo "🔧 Script de réparation Garage S3"
echo "================================="

# Vérifier que Garage fonctionne
if ! docker ps | grep -q enterprise-garage; then
    echo "❌ Le conteneur Garage n'est pas en cours d'exécution"
    echo "💡 Lancez d'abord: docker-compose up -d garage"
    exit 1
fi

# Afficher le statut actuel
echo "📊 Statut actuel de Garage:"
docker exec enterprise-garage /garage -c /etc/garage.toml status

# Attendre que Garage soit complètement prêt
echo "⏳ Vérification de la disponibilité de Garage..."
for i in {1..30}; do
    if docker exec enterprise-garage /garage -c /etc/garage.toml status &>/dev/null; then
        echo "✅ Garage est prêt!"
        break
    elif [ $i -eq 30 ]; then
        echo "❌ Garage n'est pas disponible après 30 tentatives"
        exit 1
    else
        echo "Tentative $i/30..."
        sleep 2
    fi
done

# Récupérer l'ID du nœud de manière plus robuste
echo "🔍 Détection de l'ID du nœud..."
NODE_ID=$(docker exec enterprise-garage /garage -c /etc/garage.toml status 2>/dev/null | \
    grep -E "^[a-f0-9]{16}" | head -1 | awk '{print $1}')

if [ -z "$NODE_ID" ]; then
    # Fallback: essayer de récupérer depuis les logs ou une autre méthode
    NODE_ID=$(docker exec enterprise-garage /garage -c /etc/garage.toml status 2>/dev/null | \
        awk '/HEALTHY NODES/,/^$/ {if(/^[a-f0-9]{16}/) print $1}' | head -1)
fi

echo "📋 ID du nœud détecté: $NODE_ID"

if [ -z "$NODE_ID" ]; then
    echo "❌ Impossible de détecter l'ID du nœud"
    echo "📋 Sortie complète du statut:"
    docker exec enterprise-garage /garage -c /etc/garage.toml status
    exit 1
fi

# Vérifier si le nœud a un rôle assigné
echo "🔍 Vérification du rôle du nœud..."
if docker exec enterprise-garage /garage -c /etc/garage.toml status | grep -q "NO ROLE ASSIGNED"; then
    echo "⚠️  Le nœud n'a pas de rôle assigné. Attribution en cours..."

    # Assigner un rôle au nœud avec gestion d'erreur
    if docker exec enterprise-garage /garage -c /etc/garage.toml layout assign $NODE_ID -z default -c 1G; then
        echo "✅ Rôle assigné avec succès"
    else
        echo "❌ Échec de l'assignation du rôle"
        exit 1
    fi

    # Vérifier le layout avant application
    echo "📋 Layout proposé:"
    docker exec enterprise-garage /garage -c /etc/garage.toml layout show

    # Appliquer le layout
    echo "📐 Application du layout..."
    if docker exec enterprise-garage /garage -c /etc/garage.toml layout apply --version 1; then
        echo "✅ Layout appliqué avec succès"
    else
        echo "❌ Échec de l'application du layout"
        exit 1
    fi

    # Attendre l'application avec vérification progressive
    echo "⏳ Attente de l'application du layout..."
    for i in {1..20}; do
        if ! docker exec enterprise-garage /garage -c /etc/garage.toml status | grep -q "NO ROLE ASSIGNED"; then
            echo "✅ Layout appliqué (${i}s)"
            break
        elif [ $i -eq 20 ]; then
            echo "⚠️  Layout en cours d'application (peut prendre plus de temps)"
        fi
        sleep 1
    done

    # Vérifier le résultat
    echo "✅ Nouveau statut:"
    docker exec enterprise-garage /garage -c /etc/garage.toml status
else
    echo "✅ Le nœud a déjà un rôle assigné"
fi

# Vérifier et créer la clé si nécessaire
echo "🔑 Vérification de la clé d'accès..."
if ! docker exec enterprise-garage /garage -c /etc/garage.toml key list | grep -q "enterprise-app"; then
    echo "🔧 Création de la clé d'accès..."
    if docker exec enterprise-garage /garage -c /etc/garage.toml key create enterprise-app; then
        echo "✅ Clé créée avec succès"
    else
        echo "❌ Échec de la création de la clé"
        exit 1
    fi

    # Récupérer l'ID de la clé avec vérification
    sleep 2  # Attendre que la clé soit disponible
    KEY_ID=$(docker exec enterprise-garage /garage -c /etc/garage.toml key list | grep enterprise-app | awk '{print $1}')

    if [ -z "$KEY_ID" ]; then
        echo "❌ Impossible de récupérer l'ID de la clé"
        exit 1
    fi

    echo "📋 ID de la clé: $KEY_ID"

    # Configurer les credentials avec gestion d'erreur
    if docker exec enterprise-garage /garage -c /etc/garage.toml key import \
        "$KEY_ID" \
        "enterprise-app-secret" \
        -n enterprise-app \
        --yes; then
        echo "✅ Credentials configurés avec succès"
    else
        echo "❌ Échec de la configuration des credentials"
        exit 1
    fi
else
    echo "✅ La clé d'accès existe déjà"
    KEY_ID=$(docker exec enterprise-garage /garage -c /etc/garage.toml key list | grep enterprise-app | awk '{print $1}')
    echo "📋 ID de la clé existante: $KEY_ID"
fi

# Vérifier que KEY_ID est défini
if [ -z "$KEY_ID" ]; then
    echo "❌ Aucune clé disponible"
    exit 1
fi

# Vérifier et créer le bucket si nécessaire
echo "🪣 Vérification du bucket..."
if ! docker exec enterprise-garage /garage -c /etc/garage.toml bucket list | grep -q "enterprise-storage"; then
    echo "🔧 Création du bucket..."
    if docker exec enterprise-garage /garage -c /etc/garage.toml bucket create enterprise-storage; then
        echo "✅ Bucket créé avec succès"
    else
        echo "❌ Échec de la création du bucket"
        exit 1
    fi

    # Donner les permissions avec gestion d'erreur
    echo "🔐 Attribution des permissions..."
    if docker exec enterprise-garage /garage -c /etc/garage.toml bucket allow \
        --read \
        --write \
        enterprise-storage \
        --key "$KEY_ID"; then
        echo "✅ Permissions attribuées avec succès"
    else
        echo "❌ Échec de l'attribution des permissions"
        exit 1
    fi
else
    echo "✅ Le bucket existe déjà"
    # Vérifier les permissions existantes
    echo "🔍 Vérification des permissions..."
    if docker exec enterprise-garage /garage -c /etc/garage.toml bucket info enterprise-storage | grep -q "$KEY_ID"; then
        echo "✅ Permissions déjà configurées"
    else
        echo "🔧 Attribution des permissions manquantes..."
        docker exec enterprise-garage /garage -c /etc/garage.toml bucket allow \
            --read \
            --write \
            enterprise-storage \
            --key "$KEY_ID"
    fi
fi

echo ""
echo "🎉 Réparation terminée!"
echo "======================="
echo "📊 Statut final:"
docker exec enterprise-garage /garage -c /etc/garage.toml status
echo ""
echo "🪣 Buckets disponibles:"
docker exec enterprise-garage /garage -c /etc/garage.toml bucket list
echo ""
echo "🔑 Clés disponibles:"
docker exec enterprise-garage /garage -c /etc/garage.toml key list
echo ""
echo "📝 Informations de connexion S3:"
echo "  - Endpoint: http://localhost:3900"
echo "  - Access Key: enterprise-app-key"
echo "  - Secret Key: enterprise-app-secret"
echo "  - Bucket: enterprise-storage"
echo "  - Region: garage"
echo ""
echo "🌐 Interface admin: http://localhost:3902"
echo "✅ Garage S3 est maintenant opérationnel!"