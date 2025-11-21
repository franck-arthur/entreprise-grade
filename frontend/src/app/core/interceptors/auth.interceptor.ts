import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { from, switchMap } from 'rxjs';

/**
 * Auth interceptor for adding JWT token to HTTP requests.
 *
 * This is a functional interceptor (Angular 17+ style).
 * It automatically adds the Authorization header with Bearer token to all requests.
 * Uses Keycloak service directly to get the most up-to-date token.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloak = inject(KeycloakService);

  // Don't add token to these URLs
  if (req.url.includes('/assets') || req.url.includes('i18n')) {
    return next(req);
  }

  // Get fresh token from Keycloak
  return from(keycloak.getToken()).pipe(
    switchMap(token => {
      if (token) {
        console.log('🔑 Adding token to request:', req.url);
        const clonedRequest = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        });
        return next(clonedRequest);
      }

      console.warn('⚠️ No token available for request:', req.url);
      return next(req);
    })
  );
};
