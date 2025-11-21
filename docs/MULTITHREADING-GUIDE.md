# Guide Multithreading pour Tech Lead Java/Angular

## Vue d'ensemble

Ce guide couvre les concepts essentiels du multithreading pour une application enterprise-grade avec un backend Java Spring Boot et un frontend Angular.

---

## 1. Multithreading Java (Backend)

### 1.1 Concepts fondamentaux

| Concept | Description |
|---------|-------------|
| **Thread** | Unité d'exécution légère au sein d'un processus |
| **Runnable** | Interface fonctionnelle pour définir une tâche |
| **Callable<T>** | Comme Runnable mais retourne un résultat |
| **Future<T>** | Représente le résultat d'une opération asynchrone |
| **CompletableFuture<T>** | Future enrichi avec chaînage et composition |

### 1.2 Thread Pools (ExecutorService)

```java
// Configuration recommandée pour Spring Boot
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

### 1.3 Types de Thread Pools

| Type | Cas d'usage |
|------|-------------|
| **FixedThreadPool** | Charge prévisible, nombre fixe de threads |
| **CachedThreadPool** | Tâches courtes et nombreuses |
| **ScheduledThreadPool** | Tâches planifiées/périodiques |
| **WorkStealingPool** | Tâches parallèles avec fork/join |
| **Virtual Threads (Java 21+)** | I/O-bound, haute concurrence |

### 1.4 @Async avec Spring

```java
@Service
public class BatchImportService {

    @Async("csvProcessorExecutor")
    public CompletableFuture<ImportResult> processFileAsync(UUID batchId, byte[] content) {
        // Traitement asynchrone
        return CompletableFuture.completedFuture(result);
    }
}
```

**Points d'attention :**
- `@Async` ne fonctionne pas sur les appels internes (même classe)
- Toujours spécifier l'executor pour éviter le SimpleAsyncTaskExecutor
- Gérer les exceptions avec `AsyncUncaughtExceptionHandler`

### 1.5 Synchronisation et Thread Safety

```java
// Outils de synchronisation
private final ReentrantLock lock = new ReentrantLock();
private final AtomicInteger counter = new AtomicInteger(0);
private final ConcurrentHashMap<String, Data> cache = new ConcurrentHashMap<>();

// Collections thread-safe
List<T> list = Collections.synchronizedList(new ArrayList<>());
List<T> copyOnWrite = new CopyOnWriteArrayList<>(); // Lectures fréquentes
BlockingQueue<T> queue = new LinkedBlockingQueue<>(); // Producer/Consumer
```

### 1.6 Patterns de concurrence

#### Producer-Consumer
```java
@Component
public class EventProcessor {
    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>(1000);

    public void produce(Event event) throws InterruptedException {
        queue.put(event); // Bloque si queue pleine
    }

    @Scheduled(fixedDelay = 100)
    public void consume() {
        Event event = queue.poll();
        if (event != null) process(event);
    }
}
```

#### Fork/Join pour traitement parallèle
```java
List<Result> results = data.parallelStream()
    .map(this::processItem)
    .collect(Collectors.toList());
```

### 1.7 Virtual Threads (Java 21+)

```java
// Recommandé pour I/O-bound tasks
@Bean(name = "virtualThreadExecutor")
public Executor virtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}

// Configuration Spring Boot 3.2+
spring.threads.virtual.enabled=true
```

---

## 2. Gestion asynchrone Angular (Frontend)

### 2.1 RxJS et Observables

```typescript
// Service avec gestion d'erreurs et retry
@Injectable({ providedIn: 'root' })
export class DataService {

  getData(): Observable<Data[]> {
    return this.http.get<Data[]>('/api/data').pipe(
      retry(3),
      catchError(this.handleError),
      shareReplay(1) // Cache le résultat
    );
  }
}
```

### 2.2 Opérateurs essentiels

| Opérateur | Usage |
|-----------|-------|
| `switchMap` | Annule requête précédente (recherche) |
| `mergeMap` | Exécution parallèle |
| `concatMap` | Exécution séquentielle |
| `exhaustMap` | Ignore si en cours (submit form) |
| `debounceTime` | Délai avant émission |
| `distinctUntilChanged` | Évite doublons consécutifs |

### 2.3 Web Workers pour calculs lourds

```typescript
// app.component.ts
if (typeof Worker !== 'undefined') {
  const worker = new Worker(new URL('./app.worker', import.meta.url));
  worker.onmessage = ({ data }) => console.log('Result:', data);
  worker.postMessage({ data: largeDataset });
}

// app.worker.ts
addEventListener('message', ({ data }) => {
  const result = heavyComputation(data);
  postMessage(result);
});
```

---

## 3. Bonnes pratiques

### 3.1 Backend Java

| Pratique | Raison |
|----------|--------|
| Toujours limiter la taille du pool | Évite l'épuisement des ressources |
| Utiliser des noms de threads explicites | Facilite le debugging |
| Configurer le shutdown graceful | Évite la perte de données |
| Monitorer les métriques du pool | Détecte les bottlenecks |
| Préférer l'immutabilité | Évite les race conditions |
| Éviter `synchronized` sur des objets partagés | Risque de deadlock |

### 3.2 Frontend Angular

| Pratique | Raison |
|----------|--------|
| Unsubscribe des Observables | Évite les memory leaks |
| Utiliser `async` pipe | Gestion automatique subscription |
| `takeUntilDestroyed()` (Angular 16+) | Cleanup automatique |
| Web Workers pour calculs > 50ms | Évite le blocage UI |
| `trackBy` dans ngFor | Optimise le rendu |

---

## 4. Debugging et monitoring

### 4.1 Outils Java

```java
// Métriques Micrometer pour thread pools
@Bean
public MeterBinder threadPoolMetrics(ThreadPoolTaskExecutor executor) {
    return registry -> {
        Gauge.builder("executor.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize)
            .register(registry);
        Gauge.builder("executor.queue.size", executor, e -> e.getThreadPoolExecutor().getQueue().size())
            .register(registry);
    };
}
```

### 4.2 Thread dumps

```bash
# Générer un thread dump
jstack <pid> > thread_dump.txt

# Avec JMC (Java Mission Control)
jcmd <pid> Thread.print
```

### 4.3 Problèmes courants

| Problème | Symptôme | Solution |
|----------|----------|----------|
| **Deadlock** | Application figée | Analyser thread dump, éviter locks imbriqués |
| **Race condition** | Données incohérentes | Synchronisation, AtomicXxx, immutabilité |
| **Thread starvation** | Latence élevée | Augmenter pool size, séparer les pools |
| **Memory leak** | OOM | Limiter queue, timeouts, monitoring |

---

## 5. Configuration recommandée (Production)

### application.yml

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
        keep-alive: 60s
      thread-name-prefix: app-exec-
      shutdown:
        await-termination: true
        await-termination-period: 60s
    scheduling:
      pool:
        size: 4
      thread-name-prefix: app-sched-
```

### Métriques à surveiller

- `executor.pool.size` - Taille actuelle du pool
- `executor.active` - Threads actifs
- `executor.queued` - Tâches en attente
- `executor.completed` - Tâches terminées

---

## 6. Checklist Tech Lead

### Avant mise en production

- [ ] Thread pools configurés avec limites explicites
- [ ] Shutdown graceful configuré
- [ ] Métriques exposées (Actuator/Prometheus)
- [ ] Tests de charge validés
- [ ] Thread dumps analysables
- [ ] Timeouts configurés sur toutes les opérations async
- [ ] Gestion d'erreurs dans les tâches async
- [ ] Documentation des flux asynchrones

### Code review

- [ ] Pas de `new Thread()` direct
- [ ] Pas de `Executors.newCachedThreadPool()` non borné
- [ ] Collections thread-safe si accès concurrent
- [ ] `@Async` avec executor explicite
- [ ] CompletableFuture avec timeout
- [ ] Observables correctement unsubscribed (Angular)

---

## Ressources

- [Java Concurrency in Practice](https://jcip.net/)
- [Spring Async Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [RxJS Documentation](https://rxjs.dev/guide/overview)
- [Java Virtual Threads (JEP 444)](https://openjdk.org/jeps/444)
