# Frontend Testing Guide

## Vue d'ensemble

Le frontend utilise **Jest** et **jest-preset-angular** pour les tests unitaires. Les tests couvrent les services, le store NgRx (reducers, selectors, effects), et les composants.

## Configuration Jest

La configuration Jest est définie dans `package.json` :

```json
{
  "jest": {
    "preset": "jest-preset-angular",
    "setupFilesAfterEnv": ["<rootDir>/setup-jest.ts"],
    "coverageThreshold": {
      "global": {
        "branches": 80,
        "functions": 80,
        "lines": 80,
        "statements": 80
      }
    }
  }
}
```

### Fichier de setup

Le fichier `setup-jest.ts` contient les mocks globaux nécessaires :
- `window.matchMedia` (pour les media queries)
- `IntersectionObserver` (pour les composants DSFR)

## Structure des tests

```
frontend/src/app/
├── core/
│   └── services/
│       ├── audit.service.ts
│       ├── audit.service.spec.ts              ✅ 200+ lignes
│       ├── batch-import.service.ts
│       └── batch-import.service.spec.ts       ✅ 250+ lignes
├── store/
│   ├── audit/
│   │   ├── audit.reducer.ts
│   │   ├── audit.reducer.spec.ts              ✅ 450+ lignes
│   │   ├── audit.selectors.ts
│   │   ├── audit.selectors.spec.ts            ✅ 150+ lignes
│   │   ├── audit.effects.ts
│   │   └── audit.effects.spec.ts              ✅ 200+ lignes
│   └── batch-import/
│       ├── batch-import.reducer.ts
│       └── batch-import.reducer.spec.ts       ✅ 400+ lignes
└── features/
    └── [composants avec leurs tests .spec.ts]
```

## Scripts de test

### Exécution des tests

```bash
# Tests unitaires (mode watch)
npm test

# Tests unitaires (exécution unique)
npm run test:ci

# Tests avec couverture
npm run test:coverage

# Tests en mode watch
npm run test:watch
```

### CI/CD

Pour l'intégration continue, utilisez :

```bash
npm run test:ci
```

Ce script :
- Exécute tous les tests
- Génère le rapport de couverture
- Utilise 2 workers maximum (optimisé pour CI)
- Mode `--ci` (désactive watch, sortie optimisée)

## Tests créés

### 1. Services HTTP

#### AuditService (`audit.service.spec.ts`)
Tests couverts :
- ✅ `getAuditEvents()` avec filtres complexes
- ✅ `getAuditEventsByUser()`
- ✅ `getAuditEventsByType()`
- ✅ `getAuditEventsByEntity()`
- ✅ `getStatistics()`
- ✅ `getStatisticsForDateRange()`
- ✅ `getHourlyStatistics()`
- ✅ Gestion des erreurs HTTP

**Exemple de test :**
```typescript
it('should retrieve audit events with filters', () => {
  const mockQuery: AuditEventQuery = {
    eventTypes: [AuditEventType.USER_CREATED],
    eventCategory: AuditEventCategory.USER,
    success: true
  };

  service.getAuditEvents(mockQuery, 0, 50).subscribe(response => {
    expect(response.content.length).toBe(1);
    expect(response.totalElements).toBe(1);
  });

  const req = httpMock.expectOne(request =>
    request.url === apiUrl &&
    request.params.get('eventCategory') === 'USER'
  );
  req.flush(mockResponse);
});
```

#### BatchImportService (`batch-import.service.spec.ts`)
Tests couverts :
- ✅ `uploadCsvFile()` avec FormData
- ✅ `getBatchImportById()`
- ✅ `getBatchImportStatus()` (polling)
- ✅ `getAllBatchImports()` avec pagination
- ✅ `getMyBatchImports()`
- ✅ `cancelBatchImport()`
- ✅ Scénario complet upload → polling → completion

**Exemple de test :**
```typescript
it('should upload a CSV file and return batch import', () => {
  const mockFile = new File(['username,email'], 'users.csv', {
    type: 'text/csv'
  });

  service.uploadCsvFile(mockFile).subscribe(response => {
    expect(response.status).toBe(BatchImportStatus.PENDING);
    expect(response.fileName).toBe('users.csv');
  });

  const req = httpMock.expectOne(`${apiUrl}/upload`);
  expect(req.request.body instanceof FormData).toBeTruthy();
  req.flush(mockResponse);
});
```

### 2. NgRx Store

#### Reducers

**Audit Reducer** (`audit.reducer.spec.ts`)
Tests couverts :
- ✅ `loadAuditEvents` - Set loading, store query
- ✅ `loadAuditEventsSuccess` - Update events, pagination
- ✅ `loadAuditEventsFailure` - Set error, clear loading
- ✅ `loadAuditEventsByUser` - User-specific events
- ✅ `loadAuditStatistics` - Statistics loading
- ✅ `setAuditFilter` - Update current query
- ✅ `clearAuditFilter` - Reset query
- ✅ Immutabilité du state
- ✅ Transitions d'état complexes

**Exemple de test :**
```typescript
it('should set loading to true and store query', () => {
  const query = { eventTypes: [AuditEventType.USER_CREATED] };
  const action = AuditActions.loadAuditEvents({ query, page: 0, size: 50 });
  const result = auditReducer(initialState, action);

  expect(result.loading).toBe(true);
  expect(result.currentQuery).toEqual(query);
  expect(result.error).toBeNull();
});
```

**Batch Import Reducer** (`batch-import.reducer.spec.ts`)
Tests couverts :
- ✅ `uploadCsvFile` - Set uploading flag
- ✅ `uploadCsvFileSuccess` - Prepend new import to list
- ✅ `loadBatchImports` - Pagination
- ✅ `loadBatchImportDetail` - Selected import
- ✅ `refreshBatchImportStatusSuccess` - Update import in list et selected
- ✅ `cancelBatchImport` - Cancel processing
- ✅ Immutabilité du state

**Exemple de test :**
```typescript
it('should update batch import in list', () => {
  const originalImport: BatchImport = {
    id: 'batch-1',
    status: BatchImportStatus.PROCESSING,
    processedLines: 50
  };
  const stateWithImport = { ...initialState, imports: [originalImport] };

  const updatedImport = {
    ...originalImport,
    processedLines: 100,
    status: BatchImportStatus.COMPLETED
  };
  const action = BatchImportActions.refreshBatchImportStatusSuccess({
    batchImport: updatedImport
  });
  const result = batchImportReducer(stateWithImport, action);

  expect(result.imports[0].status).toBe(BatchImportStatus.COMPLETED);
  expect(result.imports[0].processedLines).toBe(100);
});
```

#### Selectors

**Audit Selectors** (`audit.selectors.spec.ts`)
Tests couverts :
- ✅ `selectAuditEvents` - Retrieve events array
- ✅ `selectAuditStatistics` - Retrieve statistics
- ✅ `selectAuditCurrentQuery` - Retrieve current query
- ✅ `selectAuditLoading` - Loading state
- ✅ `selectAuditLoadingStatistics` - Statistics loading
- ✅ `selectAuditError` - Error message
- ✅ `selectAuditPagination` - Computed pagination object
- ✅ Memoization des sélecteurs

**Exemple de test :**
```typescript
it('should select pagination info', () => {
  const result = fromAudit.selectAuditPagination(mockState);
  expect(result).toEqual({
    totalElements: 100,
    totalPages: 5,
    currentPage: 2
  });
});

it('should return same reference when state unchanged (memoization)', () => {
  const result1 = fromAudit.selectAuditEvents(mockState);
  const result2 = fromAudit.selectAuditEvents(mockState);
  expect(result1).toBe(result2); // Same reference
});
```

#### Effects

**Audit Effects** (`audit.effects.spec.ts`)
Tests couverts :
- ✅ `loadAuditEvents$` - Success et failure
- ✅ `loadAuditEventsByUser$` - User-specific events
- ✅ `loadAuditEventsByType$` - Type-specific events
- ✅ `loadAuditEventsByEntity$` - Entity-specific events
- ✅ `loadAuditStatistics$` - Statistics loading
- ✅ `loadAuditStatisticsForDateRange$` - Date range statistics
- ✅ Gestion des erreurs avec messages par défaut

**Exemple de test :**
```typescript
it('should return loadAuditEventsSuccess on success', (done) => {
  const query = { eventTypes: [AuditEventType.USER_CREATED] };
  const action = AuditActions.loadAuditEvents({ query, page: 0, size: 50 });
  const outcome = AuditActions.loadAuditEventsSuccess({
    events: mockPage.content,
    totalElements: mockPage.totalElements,
    totalPages: mockPage.totalPages
  });

  auditService.getAuditEvents.mockReturnValue(of(mockPage));
  actions$ = of(action);

  effects.loadAuditEvents$.subscribe(result => {
    expect(result).toEqual(outcome);
    expect(auditService.getAuditEvents).toHaveBeenCalledWith(query, 0, 50);
    done();
  });
});
```

## Couverture de code

Objectif : **80%** de couverture minimum sur :
- Branches
- Functions
- Lines
- Statements

### Rapport de couverture

Après `npm run test:coverage`, le rapport est généré dans :
```
frontend/coverage/
├── lcov-report/
│   └── index.html          # Rapport HTML interactif
├── lcov.info               # Format LCOV (pour CI)
└── cobertura-coverage.xml  # Format Cobertura (pour CI)
```

Ouvrir le rapport HTML :
```bash
cd frontend
open coverage/lcov-report/index.html
```

## Patterns et bonnes pratiques

### 1. Tests de services HTTP

```typescript
describe('MyService', () => {
  let service: MyService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MyService]
    });
    service = TestBed.inject(MyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // Vérifie qu'aucune requête n'est en attente
  });

  it('should call API with correct params', () => {
    service.getData().subscribe();

    const req = httpMock.expectOne('/api/data');
    expect(req.request.method).toBe('GET');
    req.flush(mockData);
  });
});
```

### 2. Tests de reducers

```typescript
describe('My Reducer', () => {
  it('should update state immutably', () => {
    const originalState = { ...initialState };
    const action = MyActions.doSomething({ data: 'test' });
    const result = myReducer(originalState, action);

    expect(result).not.toBe(originalState); // Nouvelle référence
    expect(originalState.loading).toBe(false); // Original inchangé
    expect(result.loading).toBe(true);
  });
});
```

### 3. Tests de selectors

```typescript
describe('My Selectors', () => {
  it('should memoize selector results', () => {
    const result1 = selectMyData(mockState);
    const result2 = selectMyData(mockState); // Même state
    expect(result1).toBe(result2); // Même référence (memoization)
  });

  it('should recompute on state change', () => {
    const result1 = selectMyData(mockState);
    const newState = { ...mockState, data: newData };
    const result2 = selectMyData(newState);
    expect(result1).not.toBe(result2); // Différentes références
  });
});
```

### 4. Tests d'effects

```typescript
describe('My Effects', () => {
  let actions$: Observable<any>;
  let effects: MyEffects;
  let myService: jest.Mocked<MyService>;

  beforeEach(() => {
    const serviceMock = {
      getData: jest.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        MyEffects,
        provideMockActions(() => actions$),
        { provide: MyService, useValue: serviceMock }
      ]
    });

    effects = TestBed.inject(MyEffects);
    myService = TestBed.inject(MyService) as jest.Mocked<MyService>;
  });

  it('should dispatch success action', (done) => {
    const action = MyActions.load();
    const outcome = MyActions.loadSuccess({ data: mockData });

    myService.getData.mockReturnValue(of(mockData));
    actions$ = of(action);

    effects.load$.subscribe(result => {
      expect(result).toEqual(outcome);
      done();
    });
  });
});
```

## Debugging des tests

### Exécuter un seul fichier de test

```bash
npm test -- audit.service.spec.ts
```

### Exécuter un seul test

Dans le fichier `.spec.ts`, utilisez `fit()` ou `fdescribe()` :

```typescript
fit('should do something specific', () => {
  // Ce test sera le seul exécuté
});
```

### Voir la sortie console

```typescript
it('should debug data', () => {
  console.log('Debug data:', myData);
  // Les logs s'affichent dans le terminal
});
```

### Mode verbose

```bash
npm test -- --verbose
```

## Problèmes courants

### 1. "Cannot find module '@angular/core/testing'"

**Solution :** Vérifier que `@angular/core` est installé :
```bash
npm install
```

### 2. "Timeout of 5000ms exceeded"

**Solution :** Augmenter le timeout pour les tests asynchrones :
```typescript
it('should handle long operations', (done) => {
  // Test code
}, 10000); // 10 secondes timeout
```

### 3. "HttpTestingController - Expected no open requests"

**Solution :** Vérifier que toutes les requêtes HTTP sont mockées :
```typescript
afterEach(() => {
  httpMock.verify(); // Échoue si des requêtes ne sont pas mockées
});
```

### 4. Mock incomplet pour un service

**Solution :** Mocker toutes les méthodes utilisées :
```typescript
const myServiceMock = {
  method1: jest.fn(),
  method2: jest.fn(),
  // ... toutes les méthodes utilisées
};
```

## Métriques actuelles

**Tests créés :**
- ✅ 2 services (AuditService, BatchImportService)
- ✅ 2 reducers (audit, batch-import)
- ✅ 1 selectors (audit)
- ✅ 1 effects (audit)

**Total :**
- **6 fichiers de test**
- **~1650 lignes de tests**
- **100+ tests unitaires**

**Couverture estimée :**
- Services : ~95%
- Store NgRx : ~90%
- Composants : 0% (à compléter)

## Prochaines étapes

Pour atteindre 80% de couverture globale :

1. **Composants** (priorité haute)
   - AuditLogsComponent
   - AuditDashboardComponent
   - BatchImportUploadComponent
   - BatchImportListComponent

2. **Services restants**
   - UserService
   - LanguageService
   - AuthService

3. **Store NgRx restants**
   - batch-import.selectors
   - batch-import.effects

4. **Guards et Interceptors**
   - AuthGuard (si applicable)
   - ErrorInterceptor

## Ressources

- [Jest Documentation](https://jestjs.io/)
- [jest-preset-angular](https://github.com/thymikee/jest-preset-angular)
- [Angular Testing Guide](https://angular.io/guide/testing)
- [NgRx Testing Guide](https://ngrx.io/guide/store/testing)
- [Testing Best Practices](https://github.com/goldbergyoni/javascript-testing-best-practices)

---

**Auteur :** Équipe Entreprise Grade
**Dernière mise à jour :** 2024-01-20
