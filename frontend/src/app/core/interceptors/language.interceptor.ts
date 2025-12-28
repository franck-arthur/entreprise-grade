import { HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { LanguageService } from '../services/language.service';

/**
 * Language interceptor for adding Accept-Language header to HTTP requests.
 *
 * This interceptor automatically adds the Accept-Language header based on
 * the current language selection, allowing the backend to return localized
 * error messages and content.
 */
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
