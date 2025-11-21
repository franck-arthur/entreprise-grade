# Guide SSR (Server-Side Rendering) pour Tech Lead Java/Angular

## Vue d'ensemble

Le SSR permet de pré-rendre les pages Angular côté serveur avant de les envoyer au client, améliorant le SEO et le temps de chargement initial.

---

## 1. Pourquoi le SSR ?

### 1.1 Avantages

| Avantage | Description |
|----------|-------------|
| **SEO** | Contenu indexable par les moteurs de recherche |
| **Performance perçue** | First Contentful Paint (FCP) plus rapide |
| **Partage social** | Meta tags disponibles pour les crawlers |
| **Accessibilité** | Contenu visible sans JavaScript |
| **LCP optimisé** | Largest Contentful Paint amélioré |

### 1.2 Inconvénients

| Inconvénient | Mitigation |
|--------------|------------|
| Complexité infrastructure | Utiliser des solutions managées |
| Coût serveur supplémentaire | Cache agressif, CDN |
| Time To Interactive (TTI) potentiellement plus long | Hydratation progressive |
| APIs browser indisponibles côté serveur | Guards et injection de plateforme |

### 1.3 Quand utiliser le SSR ?

| Cas d'usage | SSR recommandé |
|-------------|----------------|
| Site e-commerce public | Oui |
| Blog / Site vitrine | Oui |
| Dashboard admin interne | Non |
| Application métier authentifiée | Non (sauf landing page) |
| PWA offline-first | Non |

---

## 2. Angular SSR (Angular 17+)

### 2.1 Installation

```bash
# Nouveau projet avec SSR
ng new my-app --ssr

# Ajouter SSR à un projet existant
ng add @angular/ssr
```

### 2.2 Structure du projet

```
src/
├── app/
│   ├── app.component.ts
│   ├── app.config.ts
│   ├── app.config.server.ts    # Config serveur
│   └── app.routes.ts
├── main.ts                      # Bootstrap client
└── main.server.ts               # Bootstrap serveur
server.ts                        # Serveur Express
```

### 2.3 Configuration serveur (server.ts)

```typescript
import { APP_BASE_HREF } from '@angular/common';
import { CommonEngine } from '@angular/ssr';
import express from 'express';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import bootstrap from './src/main.server';

export function app(): express.Express {
  const server = express();
  const serverDistFolder = dirname(fileURLToPath(import.meta.url));
  const browserDistFolder = resolve(serverDistFolder, '../browser');
  const indexHtml = join(serverDistFolder, 'index.server.html');

  const commonEngine = new CommonEngine();

  server.set('view engine', 'html');
  server.set('views', browserDistFolder);

  // Fichiers statiques
  server.get('*.*', express.static(browserDistFolder, { maxAge: '1y' }));

  // Routes Angular
  server.get('*', (req, res, next) => {
    commonEngine
      .render({
        bootstrap,
        documentFilePath: indexHtml,
        url: req.originalUrl,
        publicPath: browserDistFolder,
        providers: [{ provide: APP_BASE_HREF, useValue: req.baseUrl }],
      })
      .then((html) => res.send(html))
      .catch((err) => next(err));
  });

  return server;
}
```

### 2.4 Hydratation (Angular 16+)

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideClientHydration } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
    provideClientHydration()
  ]
};
```

#### Hydratation progressive (Angular 17+)

```typescript
import { provideClientHydration, withEventReplay, withIncrementalHydration } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
    provideClientHydration(
      withEventReplay(),           // Rejoue les événements pendant l'hydratation
      withIncrementalHydration()   // Hydratation à la demande
    )
  ]
};
```

```html
<!-- Template avec hydratation différée -->
@defer (hydrate on viewport) {
  <app-heavy-component />
}
```

---

## 3. Intégration avec Backend Java

### 3.1 Architecture recommandée

```
┌─────────────────────────────────────────────────────────────┐
│                         CDN / Cache                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Load Balancer / Gateway                   │
└─────────────────────────────────────────────────────────────┘
              │                                │
              ▼                                ▼
┌──────────────────────────┐    ┌──────────────────────────────┐
│   Node.js SSR Server     │    │    Spring Boot API Server    │
│   (Angular Universal)    │    │    (REST / GraphQL)          │
│   Port: 4000             │    │    Port: 8080                │
└──────────────────────────┘    └──────────────────────────────┘
              │                                │
              └────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │    Database     │
                    └─────────────────┘
```

### 3.2 Configuration Spring Gateway

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        # Routes API vers Spring Boot
        - id: api
          uri: http://localhost:8080
          predicates:
            - Path=/api/**

        # Routes SSR vers Node.js
        - id: ssr
          uri: http://localhost:4000
          predicates:
            - Path=/**
```

### 3.3 Reverse Proxy avec Nginx

```nginx
upstream ssr_server {
    server 127.0.0.1:4000;
    keepalive 64;
}

upstream api_server {
    server 127.0.0.1:8080;
    keepalive 64;
}

server {
    listen 80;
    server_name example.com;

    # Cache SSR
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=ssr_cache:10m max_size=1g inactive=60m;

    # API routes
    location /api/ {
        proxy_pass http://api_server;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }

    # SSR routes avec cache
    location / {
        proxy_pass http://ssr_server;
        proxy_http_version 1.1;
        proxy_set_header Connection "";

        # Cache configuration
        proxy_cache ssr_cache;
        proxy_cache_valid 200 10m;
        proxy_cache_use_stale error timeout updating;
        proxy_cache_bypass $cookie_session;
        add_header X-Cache-Status $upstream_cache_status;
    }
}
```

---

## 4. Gestion des APIs Browser

### 4.1 Vérification de plateforme

```typescript
import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { Component, Inject, PLATFORM_ID } from '@angular/core';

@Component({...})
export class MyComponent {
  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      // Code client uniquement
      window.localStorage.setItem('key', 'value');
    }
  }
}
```

### 4.2 Service d'abstraction

```typescript
// storage.service.ts
@Injectable({ providedIn: 'root' })
export class StorageService {
  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  getItem(key: string): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem(key);
    }
    return null;
  }

  setItem(key: string, value: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(key, value);
    }
  }
}
```

### 4.3 APIs à éviter côté serveur

| API | Alternative SSR |
|-----|-----------------|
| `window` | `DOCUMENT` injection |
| `document` | `@Inject(DOCUMENT)` |
| `localStorage` | Service avec guard |
| `sessionStorage` | Service avec guard |
| `navigator` | Guard `isPlatformBrowser` |
| `setTimeout/setInterval` | `NgZone` ou guard |

---

## 5. TransferState (Éviter double fetch)

### 5.1 Configuration

```typescript
// app.config.ts
import { provideClientHydration, withHttpTransferCacheOptions } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
    provideClientHydration(
      withHttpTransferCacheOptions({
        includePostRequests: true
      })
    )
  ]
};
```

### 5.2 Usage manuel (si nécessaire)

```typescript
import { TransferState, makeStateKey } from '@angular/core';

const DATA_KEY = makeStateKey<Data[]>('productData');

@Injectable({ providedIn: 'root' })
export class DataService {
  constructor(
    private http: HttpClient,
    private transferState: TransferState,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  getData(): Observable<Data[]> {
    // Vérifier si données déjà transférées
    if (this.transferState.hasKey(DATA_KEY)) {
      const data = this.transferState.get(DATA_KEY, []);
      this.transferState.remove(DATA_KEY);
      return of(data);
    }

    return this.http.get<Data[]>('/api/data').pipe(
      tap(data => {
        if (isPlatformServer(this.platformId)) {
          this.transferState.set(DATA_KEY, data);
        }
      })
    );
  }
}
```

---

## 6. Prerendering (SSG)

### 6.1 Configuration

```typescript
// angular.json
{
  "projects": {
    "my-app": {
      "architect": {
        "prerender": {
          "builder": "@angular/build:prerender",
          "options": {
            "routes": [
              "/",
              "/about",
              "/contact",
              "/products/*"
            ]
          }
        }
      }
    }
  }
}
```

### 6.2 Routes dynamiques

```typescript
// prerender-routes.ts
export async function getPrerenderRoutes(): Promise<string[]> {
  const response = await fetch('http://api.example.com/products');
  const products = await response.json();
  return products.map((p: any) => `/products/${p.slug}`);
}
```

### 6.3 Quand utiliser SSG vs SSR

| Critère | SSG (Prerendering) | SSR |
|---------|-------------------|-----|
| Contenu | Statique | Dynamique |
| Build time | Plus long | Normal |
| Serveur runtime | Non requis | Requis |
| Personnalisation | Impossible | Possible |
| Fraîcheur données | Build time | Temps réel |

---

## 7. Performance et Cache

### 7.1 Stratégies de cache

```typescript
// Cache-Control headers dans server.ts
server.get('*', (req, res, next) => {
  const isStaticPage = ['/about', '/contact'].includes(req.path);

  commonEngine.render({...})
    .then((html) => {
      if (isStaticPage) {
        res.set('Cache-Control', 'public, max-age=3600'); // 1h
      } else {
        res.set('Cache-Control', 'private, no-cache');
      }
      res.send(html);
    });
});
```

### 7.2 Redis Cache (Production)

```typescript
import Redis from 'ioredis';

const redis = new Redis(process.env.REDIS_URL);

server.get('*', async (req, res, next) => {
  const cacheKey = `ssr:${req.originalUrl}`;

  // Check cache
  const cached = await redis.get(cacheKey);
  if (cached) {
    res.set('X-Cache', 'HIT');
    return res.send(cached);
  }

  // Render and cache
  const html = await commonEngine.render({...});
  await redis.setex(cacheKey, 300, html); // 5 min TTL
  res.set('X-Cache', 'MISS');
  res.send(html);
});
```

### 7.3 Métriques à surveiller

| Métrique | Cible | Outil |
|----------|-------|-------|
| TTFB (Time To First Byte) | < 200ms | Lighthouse |
| FCP (First Contentful Paint) | < 1.8s | Core Web Vitals |
| LCP (Largest Contentful Paint) | < 2.5s | Core Web Vitals |
| TTI (Time To Interactive) | < 3.8s | Lighthouse |
| Cache Hit Ratio | > 80% | Prometheus/Grafana |

---

## 8. Déploiement

### 8.1 Docker

```dockerfile
# Dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build:ssr

FROM node:20-alpine AS runner
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
EXPOSE 4000
CMD ["node", "dist/server/server.mjs"]
```

### 8.2 Docker Compose (Dev)

```yaml
version: '3.8'
services:
  ssr:
    build: ./frontend
    ports:
      - "4000:4000"
    environment:
      - API_URL=http://api:8080
    depends_on:
      - api

  api:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
```

### 8.3 Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: angular-ssr
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: ssr
          image: my-app-ssr:latest
          ports:
            - containerPort: 4000
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          readinessProbe:
            httpGet:
              path: /health
              port: 4000
            initialDelaySeconds: 5
          livenessProbe:
            httpGet:
              path: /health
              port: 4000
            initialDelaySeconds: 10
```

---

## 9. Troubleshooting

### 9.1 Erreurs courantes

| Erreur | Cause | Solution |
|--------|-------|----------|
| `window is not defined` | API browser côté serveur | Guard `isPlatformBrowser` |
| `document is not defined` | Accès DOM côté serveur | Injection `DOCUMENT` |
| Hydration mismatch | HTML serveur ≠ client | Vérifier données async |
| Memory leak | État non nettoyé | Détruire subscriptions |
| Timeout SSR | Rendu trop long | Optimiser requêtes, ajouter timeout |

### 9.2 Debug

```typescript
// Activer les logs de debug
// server.ts
process.env['NG_DEBUG'] = 'true';

// Vérifier l'hydratation
// Dans la console browser
ng.getComponent(document.querySelector('app-root'))
```

### 9.3 Timeout configuration

```typescript
// server.ts
const RENDER_TIMEOUT = 10000; // 10 secondes

server.get('*', async (req, res, next) => {
  const timeoutPromise = new Promise((_, reject) =>
    setTimeout(() => reject(new Error('SSR Timeout')), RENDER_TIMEOUT)
  );

  try {
    const html = await Promise.race([
      commonEngine.render({...}),
      timeoutPromise
    ]);
    res.send(html);
  } catch (err) {
    // Fallback vers index.html statique (CSR)
    res.sendFile(join(browserDistFolder, 'index.html'));
  }
});
```

---

## 10. Checklist Tech Lead

### Avant mise en production

- [ ] Toutes les APIs browser protégées par guards
- [ ] TransferState configuré (évite double fetch)
- [ ] Cache strategy définie et documentée
- [ ] Health check endpoint configuré
- [ ] Métriques Core Web Vitals mesurées
- [ ] Fallback CSR en cas d'échec SSR
- [ ] Tests E2E avec SSR activé
- [ ] Memory profiling effectué
- [ ] Timeout SSR configuré

### Code review

- [ ] Pas d'accès direct à `window`, `document`, `localStorage`
- [ ] Services avec injection `PLATFORM_ID`
- [ ] `@defer` pour composants lourds
- [ ] Meta tags dynamiques via `Meta` service
- [ ] Pas de subscriptions non gérées
- [ ] Images avec `NgOptimizedImage`

---

## Ressources

- [Angular SSR Documentation](https://angular.dev/guide/ssr)
- [Angular Hydration Guide](https://angular.dev/guide/hydration)
- [Core Web Vitals](https://web.dev/vitals/)
- [Google SEO for JavaScript](https://developers.google.com/search/docs/crawling-indexing/javascript/javascript-seo-basics)
