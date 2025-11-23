import { KeycloakService } from 'keycloak-angular';

export function initializeKeycloak(keycloak: KeycloakService): () => Promise<boolean> {
  return () =>
    keycloak.init({
      config: {
        url: 'http://localhost:8180',
        realm: 'enterprise-realm',
        clientId: 'enterprise-frontend'
      },
      initOptions: {
        onLoad: 'check-sso',
        checkLoginIframe: false,
        pkceMethod: 'S256'
      },
      // Disable Keycloak's built-in interceptor - we use our own custom interceptor
      // that properly handles async token retrieval
      enableBearerInterceptor: false
    });
}
