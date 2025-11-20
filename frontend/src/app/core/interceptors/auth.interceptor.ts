import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Auth interceptor for adding JWT token to HTTP requests.
 *
 * This is a functional interceptor (Angular 17+ style).
 * It automatically adds the Authorization header with Bearer token to all requests.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Don't add token to login requests or public endpoints
  if (token && !req.url.includes('/login') && !req.url.includes('/public')) {
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    return next(clonedRequest);
  }

  return next(req);
};
