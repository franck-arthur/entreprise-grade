#!/bin/bash

# Script de lancement simplifié pour Garage S3
# Ce script démarre Garage et l'initialise automatiquement

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Démarrage de l'environnement Garage S3..."

# 1. Vérifier si Docker est en cours d'exécution
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker n'est pas en cours d'exécution. Veuillez démarrer Docker Desktop."
    exit 1
fi

# 2. Aller au répertoire racine du projet
cd "$SCRIPT_DIR/../.."

# 3. Démarrer les services avec docker-compose
echo "📦 Démarrage des containers..."
docker-compose up -d garage

# 4. Attendre que Garage soit complètement démarré
echo "⏳ Attente du démarrage de Garage (30 secondes)..."
sleep 30

# 5. Exécuter le script d'initialisation et capturer les credentials
echo "🔧 Initialisation de Garage..."
cd "$SCRIPT_DIR"
chmod +x init-garage.sh

# Capturer la sortie du script d'initialisation pour extraire les vraies credentials
INIT_OUTPUT=$(./init-garage.sh 2>&1)
echo "$INIT_OUTPUT"

# Extraire l'Access Key ID et la Secret Key de la sortie
ACCESS_KEY_ID=$(echo "$INIT_OUTPUT" | grep -A 1 "Key ID:" | tail -1 | awk '{print $1}' | tr -d ' ')
SECRET_KEY=$(echo "$INIT_OUTPUT" | grep -A 1 "Secret key:" | tail -1 | awk '{print $1}' | tr -d ' ')

echo ""
echo "🔑 Credentials générées:"
echo "  Access Key ID: $ACCESS_KEY_ID"
echo "  Secret Key: $SECRET_KEY"

# 6. Mettre à jour le fichier .awsrc avec les vraies credentials
echo "📝 Mise à jour des credentials AWS..."
cd "$SCRIPT_DIR/../.."
cat > .awsrc << EOF
export AWS_ACCESS_KEY_ID='$ACCESS_KEY_ID'
export AWS_SECRET_ACCESS_KEY='$SECRET_KEY'
export AWS_DEFAULT_REGION='garage'
export AWS_ENDPOINT_URL='http://localhost:3900'

#aws --version
EOF

# 7. Mettre à jour le fichier .env.s3 avec les vraies credentials
echo "📝 Mise à jour du fichier .env.s3..."
cat > docker/garage/.env.s3 << EOF
# Configuration S3 pour l'application
export AWS_ACCESS_KEY_ID=$ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$SECRET_KEY
export AWS_ENDPOINT_URL=http://localhost:3900
export AWS_S3_BUCKET=enterprise-storage
export AWS_REGION=garage
export S3_PATH_STYLE_ACCESS=true
EOF

# 8. Tester la connexion S3
echo "🧪 Test de la connexion S3..."
source .awsrc
if aws s3 ls --endpoint-url $AWS_ENDPOINT_URL >/dev/null 2>&1; then
    echo "✅ Test de connexion S3 réussi!"
    aws s3 ls --endpoint-url $AWS_ENDPOINT_URL
else
    echo "⚠️  La connexion S3 n'est pas encore prête. Vous pouvez retester avec :"
    echo "   source .awsrc && aws s3 ls --endpoint-url \$AWS_ENDPOINT_URL"
fi

echo ""
echo "🎉 Garage S3 est prêt!"
echo ""
echo "📝 Pour utiliser S3, chargez les variables d'environnement :"
echo "   source .awsrc"
echo ""
echo "🧪 Pour tester S3 :"
echo "   cd docker/garage && ./test-s3.sh"
echo ""
echo "🌐 Interface admin : http://localhost:3902"
echo "🔑 Token admin : WE8bo3VL/Rf13Bu+h7vSX8P/yrbwaT3sJ6LQ2W0zrXU="