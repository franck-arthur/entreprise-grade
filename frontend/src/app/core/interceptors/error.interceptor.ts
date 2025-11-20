import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Error interceptor for handling HTTP errors globally.
 *
 * Handles common error scenarios:
 * - 401: Redirect to login
 * - 403: Redirect to unauthorized page
 * - 500+: Log error and show user-friendly message
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An error occurred';

      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = `Error: ${error.error.message}`;
        console.error('Client-side error:', error.error.message);
      } else {
        // Server-side error
        errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
        console.error('Server-side error:', errorMessage);

        // Handle specific status codes
        switch (error.status) {
          case 401:
            // Unauthorized - redirect to login
            router.navigate(['/login']);
            break;
          case 403:
            // Forbidden - redirect to unauthorized page
            router.navigate(['/unauthorized']);
            break;
          case 404:
            errorMessage = 'Resource not found';
            break;
          case 500:
          case 502:
          case 503:
            errorMessage = 'Server error. Please try again later.';
            break;
        }
      }

      // You can dispatch an action to show a toast/notification here
      // this.store.dispatch(showErrorNotification({ message: errorMessage }));

      return throwError(() => new Error(errorMessage));
    })
  );
};
