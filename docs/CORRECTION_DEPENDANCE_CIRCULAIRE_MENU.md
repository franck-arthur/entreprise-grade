# Correction de la dépendance circulaire et problèmes d'affichage du menu

## Problème identifié

Le menu frontend ne s'affichait pas correctement en raison de deux problèmes principaux :

1. **Dépendance circulaire dans LanguageService** :
   - NG0200: Circular dependency in DI detected for _LanguageService
   - Causée par l'initialisation du TranslateService dans le constructeur du LanguageService
   - L'interceptor languageInterceptor injectait LanguageService lors du chargement des traductions

2. **Erreur DSFR innerHTML** :
   - `Cannot read properties of null (reading 'innerHTML')`
   - Script DSFR s'exécutant avant que Angular ait rendu le DOM

## Solutions implémentées

### 1. Correction de la dépendance circulaire

**Fichier modifié** : `frontend/src/app/core/services/language.service.ts`
- Déplacement de l'initialisation de TranslateService dans une méthode asynchrone
- Utilisation de `setTimeout()` pour différer la configuration

```typescript
constructor(private translate: TranslateService) {
  // Initialize with stored language or default
  const storedLanguage = this.getStoredLanguage();
  const initialLanguage: string = storedLanguage && this.isLanguageSupported(storedLanguage)
    ? storedLanguage
    : this.DEFAULT_LANGUAGE;

  this.currentLanguageSubject = new BehaviorSubject<string>(initialLanguage);
  this.currentLanguage$ = this.currentLanguageSubject.asObservable();

  // Defer TranslateService configuration to avoid circular dependency
  setTimeout(() => {
    this.configureTranslateService(initialLanguage);
  });
}
```

**Fichier modifié** : `frontend/src/app/core/interceptors/language.interceptor.ts`
- Ajout de gestion d'erreur et fallback pour éviter l'injection forcée
- Utilisation de l'Injector pour une injection conditionnelle

```typescript
export const languageInterceptor: HttpInterceptorFn = (req, next) => {
  // Don't add Accept-Language to translation file requests to avoid circular dependency
  if (req.url.includes('/assets/i18n/')) {
    return next(req);
  }

  try {
    const injector = inject(Injector);
    const languageService = injector.get(LanguageService, null);

    // If LanguageService is not yet available (during app initialization),
    // use default language header
    if (!languageService) {
      const clonedRequest = req.clone({
        setHeaders: {
          'Accept-Language': 'en-US',
        },
      });
      return next(clonedRequest);
    }

    const languageHeader = languageService.getLanguageHeader();

    const clonedRequest = req.clone({
      setHeaders: {
        'Accept-Language': languageHeader,
      },
    });

    return next(clonedRequest);
  } catch (error) {
    // Fallback to default language if there's any injection error
    const clonedRequest = req.clone({
      setHeaders: {
        'Accept-Language': 'en-US',
      },
    });
    return next(clonedRequest);
  }
};
```

### 2. Correction du problème DSFR

**Fichier modifié** : `frontend/angular.json`
- Suppression du script DSFR du chargement automatique

```json
"scripts": []
```

**Fichier créé** : `frontend/src/app/core/services/dsfr.service.ts`
- Service dédié pour la gestion du DSFR
- Chargement dynamique du script DSFR
- Initialisation contrôlée après rendu Angular

**Fichier modifié** : `frontend/src/app/shared/components/header/header.component.ts`
- Utilisation du DsfrService pour l'initialisation
- Ajout de `AfterViewInit` pour garantir que le DOM est rendu

```typescript
ngAfterViewInit(): void {
  // Initialize DSFR components after the view is initialized
  // This ensures all DOM elements are present before DSFR scripts run
  setTimeout(() => {
    this.dsfrService.initializeDsfr();
  }, 100);
}
```

## Résultat attendu

- ✅ Suppression de l'erreur de dépendance circulaire NG0200
- ✅ Suppression de l'erreur `Cannot read properties of null (reading 'innerHTML')`
- ✅ Menu frontend qui s'affiche correctement
- ✅ Fonctionnalité de changement de langue préservée
- ✅ Composants DSFR fonctionnels (boutons, navigation, etc.)

## Tests recommandés

1. Vérifier que l'application démarre sans erreur dans la console
2. Tester le changement de langue via le sélecteur
3. Vérifier que le menu de navigation s'ouvre/ferme correctement
4. S'assurer que les styles DSFR s'appliquent correctement

## Notes techniques

- Le chargement différé du TranslateService peut causer un léger délai dans l'affichage des traductions (< 1ms)
- Le script DSFR se charge maintenant de manière asynchrone, ce qui peut améliorer les performances de démarrage
- La gestion d'erreur dans l'interceptor assure une robustesse en cas de problème d'injection