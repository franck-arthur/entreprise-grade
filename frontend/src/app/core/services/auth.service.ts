import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { BehaviorSubject } from 'rxjs';

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<UserProfile | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private _isLoggedIn = false;
  private _token: string | null = null;

  constructor(private keycloak: KeycloakService) {
    this.initAuth();
  }

  private async initAuth(): Promise<void> {
    try {
      this._isLoggedIn = await this.keycloak.isLoggedIn();
      if (this._isLoggedIn) {
        this._token = await this.keycloak.getToken();
        await this.loadUserProfile();
      }
    } catch (e) {
      console.error('Auth init error:', e);
    }
  }

  private async loadUserProfile(): Promise<void> {
    try {
      // Try to load full profile first
      const profile = await this.keycloak.loadUserProfile();
      const roles = this.keycloak.getUserRoles();

      const user: UserProfile = {
        id: profile.id || '',
        username: profile.username || '',
        email: profile.email || '',
        firstName: profile.firstName,
        lastName: profile.lastName,
        roles: roles
      };

      this.currentUserSubject.next(user);
    } catch (e) {
      console.warn('Could not load full user profile, using token claims instead:', e);

      // Fallback: extract user info from JWT token claims
      try {
        const tokenParsed = this.keycloak.getKeycloakInstance().tokenParsed;
        const roles = this.keycloak.getUserRoles();

        if (tokenParsed) {
          const user: UserProfile = {
            id: tokenParsed['sub'] || '',
            username: tokenParsed['preferred_username'] || tokenParsed['name'] || '',
            email: tokenParsed['email'] || '',
            firstName: tokenParsed['given_name'],
            lastName: tokenParsed['family_name'],
            roles: roles
          };

          this.currentUserSubject.next(user);
          console.log('User profile loaded from token claims:', user);
        }
      } catch (fallbackError) {
        console.error('Failed to extract user info from token:', fallbackError);
      }
    }
  }

  isAuthenticated(): boolean {
    return this._isLoggedIn;
  }

  async isAuthenticatedAsync(): Promise<boolean> {
    return this.keycloak.isLoggedIn();
  }

  getToken(): string | null {
    return this._token;
  }

  async getTokenAsync(): Promise<string> {
    return this.keycloak.getToken();
  }

  async updateToken(minValidity: number = 5): Promise<boolean> {
    try {
      const refreshed = await this.keycloak.updateToken(minValidity);
      if (refreshed) {
        this._token = await this.keycloak.getToken();
      }
      return refreshed;
    } catch {
      return false;
    }
  }

  getCurrentUser(): UserProfile | null {
    return this.currentUserSubject.value;
  }

  hasRole(role: string): boolean {
    return this.keycloak.isUserInRole(role);
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  login(): void {
    this.keycloak.login();
  }

  logout(): void {
    this._isLoggedIn = false;
    this._token = null;
    this.currentUserSubject.next(null);
    this.keycloak.logout(window.location.origin + '/login');
  }

  register(): void {
    this.keycloak.register();
  }
}
