import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LanguageService } from '../services/language.service';

/**
 * Language interceptor for adding Accept-Language header to HTTP requests.
 *
 * This interceptor automatically adds the Accept-Language header based on
 * the current language selection, allowing the backend to return localized
 * error messages and content.
 */
export const languageInterceptor: HttpInterceptorFn = (req, next) => {
  const languageService = inject(LanguageService);

  // Don't add Accept-Language to translation file requests
  if (req.url.includes('/assets/i18n/')) {
    return next(req);
  }

  const languageHeader = languageService.getLanguageHeader();

  const clonedRequest = req.clone({
    setHeaders: {
      'Accept-Language': languageHeader,
    },
  });

  return next(clonedRequest);
};
