# Configuration Garage S3 pour Kubernetes

## Vue d'ensemble

Ce document décrit la configuration de Garage S3 (alternative open-source à AWS S3) dans un environnement Kubernetes de production.

## Architecture

### Composants déployés

1. **ConfigMap** (`garage-config`) : Configuration Garage et script d'initialisation
2. **Secret** (`garage-secret`) : Clés d'accès S3 et token admin
3. **PersistentVolumeClaim** (`garage-data`) : Stockage persistant pour les données Garage
4. **Deployment** (`garage`) : Déploiement Garage avec conteneur d'initialisation
5. **Service** (`garage-service`) : Exposition des services Garage

### Ports exposés

- **3900** : API S3 (endpoint principal)
- **3902** : Interface web S3
- **3903** : Interface d'administration

## Configuration du script d'initialisation

### Fonctionnement de l'InitContainer

L'InitContainer utilise le script `init-garage.sh` pour :

1. **Attendre la disponibilité** de Garage
2. **Vérifier le statut** du cluster
3. **Assigner un rôle** au nœud si nécessaire
4. **Créer les clés d'accès** S3
5. **Créer le bucket** de stockage
6. **Configurer les permissions**

### Variables d'environnement

Le script utilise les variables suivantes :

```bash
GARAGE_HOST="http://localhost:3900"
ADMIN_TOKEN="WE8bo3VL/Rf13Bu+h7vSX8P/yrbwaT3sJ6LQ2W0zrXU="
ACCESS_KEY="enterprise-app-key"
SECRET_KEY="enterprise-app-secret"
BUCKET_NAME="enterprise-storage"
```

## Déploiement

### 1. Appliquer les configurations Garage

```bash
kubectl apply -f k8s/prod/garage-deployment.yaml
```

### 2. Vérifier le déploiement

```bash
# Vérifier le statut du pod
kubectl get pods -n production -l app=garage

# Vérifier les logs d'initialisation
kubectl logs -n production deployment/garage -c garage-init

# Vérifier les logs du conteneur principal
kubectl logs -n production deployment/garage -c garage
```

### 3. Tester la connectivité S3

```bash
# Port-forward pour tester localement
kubectl port-forward -n production svc/garage-service 3900:3900

# Test avec curl
curl -I http://localhost:3900
```

## Intégration avec l'application backend

### Variables d'environnement ajoutées

Le backend Spring Boot a été configuré avec les variables suivantes :

```yaml
env:
- name: S3_ENDPOINT
  value: "http://garage-service:3900"
- name: S3_ACCESS_KEY
  valueFrom:
    secretKeyRef:
      name: garage-secret
      key: access-key
- name: S3_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: garage-secret
      key: secret-key
- name: S3_BUCKET_NAME
  value: "enterprise-storage"
- name: S3_REGION
  value: "garage"
```

### Configuration Spring Boot

Dans votre `application.yml`, ajoutez :

```yaml
cloud:
  aws:
    s3:
      endpoint: ${S3_ENDPOINT:http://garage-service:3900}
      region: ${S3_REGION:garage}
    credentials:
      access-key: ${S3_ACCESS_KEY}
      secret-key: ${S3_SECRET_KEY}
    stack:
      auto: false
    region:
      static: ${S3_REGION:garage}

storage:
  s3:
    bucket-name: ${S3_BUCKET_NAME:enterprise-storage}
```

## Accès aux interfaces

### Interface d'administration Garage

- **URL** : `https://garage-admin.example.com`
- **Token** : `WE8bo3VL/Rf13Bu+h7vSX8P/yrbwaT3sJ6LQ2W0zrXU=`

### API S3

- **Endpoint** : `http://garage-service:3900` (interne au cluster)
- **Access Key** : `enterprise-app-key`
- **Secret Key** : `enterprise-app-secret`
- **Bucket** : `enterprise-storage`
- **Region** : `garage`

## Sécurité

### Secrets Kubernetes

Les informations sensibles sont stockées dans le Secret `garage-secret` :

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: garage-secret
  namespace: production
type: Opaque
data:
  access-key: ZW50ZXJwcmlzZS1hcHAta2V5    # enterprise-app-key
  secret-key: ZW50ZXJwcmlzZS1hcHAtc2VjcmV0  # enterprise-app-secret
  admin-token: V0U4Ym8zVkwvUmYxM0J1K2g3dlNYOFAveXJid2FUM3NKNkxRMlcwenJYVT0=
```

### Permissions de sécurité

Le pod Garage s'exécute avec :

- `runAsUser: 1000`
- `runAsGroup: 1000`
- `allowPrivilegeEscalation: false`

## Maintenance

### Commandes utiles

```bash
# Accéder au pod Garage
kubectl exec -it -n production deployment/garage -- /bin/bash

# Vérifier le statut du cluster Garage
kubectl exec -n production deployment/garage -- /garage -c /etc/garage.toml status

# Vérifier les buckets
kubectl exec -n production deployment/garage -- /garage -c /etc/garage.toml bucket list

# Vérifier les clés
kubectl exec -n production deployment/garage -- /garage -c /etc/garage.toml key list
```

### Sauvegarde des données

Les données Garage sont stockées dans le PVC `garage-data`. Assurez-vous de :

1. Configurer des snapshots réguliers du PVC
2. Mettre en place une stratégie de sauvegarde des métadonnées
3. Tester régulièrement la restauration

## Scaling et haute disponibilité

### Configuration single-node

La configuration actuelle utilise un seul nœud Garage (`replicas: 1`) adapté pour un environnement de développement/test.

### Pour la production

Pour une installation en production, considérez :

1. **Multi-nœud** : Déployer 3+ instances de Garage
2. **Load balancing** : Utiliser un LoadBalancer pour l'API S3
3. **Stockage distribué** : Configurer la réplication des données
4. **Monitoring** : Ajouter des métriques Prometheus

## Dépannage

### Problèmes courants

1. **Pod en CrashLoopBackOff**
   - Vérifiez les logs : `kubectl logs -n production deployment/garage`
   - Vérifiez que le PVC est correctement monté

2. **Init container en échec**
   - Vérifiez les logs : `kubectl logs -n production deployment/garage -c garage-init`
   - Vérifiez la connectivité réseau

3. **Erreurs S3 depuis l'application**
   - Vérifiez la résolution DNS du service `garage-service`
   - Vérifiez les credentials dans le Secret

### Logs et monitoring

```bash
# Logs en temps réel
kubectl logs -n production deployment/garage -f

# Événements du namespace
kubectl get events -n production --sort-by=.metadata.creationTimestamp

# Statut des ressources
kubectl get all -n production -l app=garage
```

## Références

- [Documentation officielle Garage](https://garagehq.deuxfleurs.fr/)
- [Configuration Garage](https://garagehq.deuxfleurs.fr/documentation/reference-manual/configuration/)
- [API S3 Garage](https://garagehq.deuxfleurs.fr/documentation/reference-manual/s3-api/)