#!/bin/bash

# Script de test pour vérifier la configuration S3 Garage

source ./docker/garage/.env.s3

echo "🧪 Test de la configuration S3 avec Garage..."
echo ""

# Test 1: Vérifier la connectivité
echo "📡 Test 1: Connectivité S3..."
if curl -s "$AWS_ENDPOINT_URL" > /dev/null; then
    echo "✅ Garage S3 est accessible"
else
    echo "❌ Impossible de joindre Garage S3"
    exit 1
fi

# Test 2: Créer un fichier de test
echo ""
echo "📝 Test 2: Création d'un fichier de test..."
echo "Hello from Garage S3!" > /tmp/test-s3.txt

# Test 3: Upload avec AWS CLI (si disponible)
if command -v aws &> /dev/null; then
    echo ""
    echo "⬆️  Test 3: Upload du fichier..."
    aws s3 cp /tmp/test-s3.txt s3://$AWS_S3_BUCKET/test.txt \
        --endpoint-url=$AWS_ENDPOINT_URL \
        --region=$AWS_REGION

    echo ""
    echo "📋 Test 4: Listing des objets..."
    aws s3 ls s3://$AWS_S3_BUCKET/ \
        --endpoint-url=$AWS_ENDPOINT_URL \
        --region=$AWS_REGION

    echo ""
    echo "⬇️  Test 5: Download du fichier..."
    aws s3 cp s3://$AWS_S3_BUCKET/test.txt /tmp/test-s3-download.txt \
        --endpoint-url=$AWS_ENDPOINT_URL \
        --region=$AWS_REGION

    if cmp -s /tmp/test-s3.txt /tmp/test-s3-download.txt; then
        echo "✅ Test de upload/download réussi!"
    else
        echo "❌ Erreur lors du test upload/download"
    fi

    # Nettoyage
    rm -f /tmp/test-s3.txt /tmp/test-s3-download.txt
else
    echo "⚠️  AWS CLI non installé, tests limités"
fi

echo ""
echo "🎉 Tests terminés!"
echo ""
echo "📊 Informations utiles:"
echo "  - Endpoint S3: $AWS_ENDPOINT_URL"
echo "  - Bucket: $AWS_S3_BUCKET"
echo "  - Interface web: http://localhost:3902"