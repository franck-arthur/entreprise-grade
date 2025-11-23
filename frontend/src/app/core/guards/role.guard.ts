import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

/**
 * Role guard to protect routes that require specific roles.
 * Uses KeycloakService to check user roles.
 */
export const RoleGuard: CanActivateFn = async (route, state) => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  // First check if user is logged in
  const isLoggedIn = await keycloak.isLoggedIn();

  if (!isLoggedIn) {
    await keycloak.login({
      redirectUri: window.location.origin + state.url,
    });
    return false;
  }

  // Check if route requires specific roles
  const requiredRoles = route.data['roles'] as string[] | undefined;

  if (requiredRoles && requiredRoles.length > 0) {
    const userRoles = keycloak.getUserRoles();
    const hasRole = requiredRoles.some(role => userRoles.includes(role));

    if (!hasRole) {
      router.navigate(['/unauthorized']);
      return false;
    }
  }

  return true;
};
