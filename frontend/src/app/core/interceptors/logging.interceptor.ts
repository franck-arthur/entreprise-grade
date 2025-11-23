import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

/**
 * Logging interceptor for debugging authentication issues.
 * Logs all outgoing requests with their headers.
 */
export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloak = inject(KeycloakService);

  console.group(`🌐 HTTP Request: ${req.method} ${req.url}`);
  console.log('Headers:', req.headers.keys().map(key => `${key}: ${req.headers.get(key)}`));
  console.log('Has Authorization header:', req.headers.has('Authorization'));

  // Log token status
  keycloak.getToken().then(token => {
    console.log('Keycloak token available:', !!token);
    if (token) {
      console.log('Token (first 50 chars):', token.substring(0, 50) + '...');
    }
  });

  console.groupEnd();

  return next(req);
};
