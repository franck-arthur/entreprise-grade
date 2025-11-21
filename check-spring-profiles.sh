#!/bin/bash

echo "=== Vérification des profils Spring Boot ==="
echo ""

# Vérifier le profil par défaut dans application.yml
echo "1️⃣  Profil par défaut (application.yml):"
grep "active:" backend/src/main/resources/application.yml | head -1
echo ""

# Vérifier si SPRING_PROFILES_ACTIVE est défini
echo "2️⃣  Variable d'environnement SPRING_PROFILES_ACTIVE:"
if [ -z "$SPRING_PROFILES_ACTIVE" ]; then
    echo "   ❌ Non définie → utilisera 'dev' par défaut"
else
    echo "   ✅ Définie: $SPRING_PROFILES_ACTIVE"
fi
echo ""

# Vérifier les profils disponibles
echo "3️⃣  Profils disponibles:"
ls -1 backend/src/main/resources/application-*.yml | sed 's/.*application-/   - /' | sed 's/.yml//'
echo ""

# Vérifier le profil Docker
echo "4️⃣  Profil utilisé dans Docker:"
grep "SPRING_PROFILES_ACTIVE" docker-compose.yml | grep -v "#" | head -1
echo ""

echo "=== Résumé ==="
echo "• En LOCAL (sans Docker) : profil 'dev' → localhost:5432, localhost:6379, localhost:8180"
echo "• En DOCKER : profil 'docker' → postgres:5432, redis:6379, keycloak:8080"
echo ""
echo "Pour démarrer en local, assurez-vous que:"
echo "  1. PostgreSQL écoute sur localhost:5432"
echo "  2. Redis écoute sur localhost:6379"
echo "  3. Elasticsearch écoute sur localhost:9200"
echo "  4. Keycloak écoute sur localhost:8180"
echo ""
echo "Ou démarrez tout avec: docker-compose up -d"
