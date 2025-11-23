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
      const roles = this.keycloak.getUserRoles();
      const tokenParsed = this.keycloak.getKeycloakInstance().tokenParsed;

      console.log('Token parsed content:', tokenParsed);

      // Try to load profile from Keycloak API
      let profile: any = null;
      try {
        profile = await this.keycloak.loadUserProfile();
        console.log('Keycloak profile loaded:', profile);
      } catch (e) {
        console.warn('Could not load user profile from Keycloak API (401 is normal):', e);
      }

      // Build user profile - prefer API data, fallback to token claims
      const user: UserProfile = {
        id: profile?.id || tokenParsed?.['sub'] || '',
        username: profile?.username || tokenParsed?.['preferred_username'] || tokenParsed?.['name'] || 'user',
        email: profile?.email || tokenParsed?.['email'] || '',
        firstName: profile?.firstName || tokenParsed?.['given_name'],
        lastName: profile?.lastName || tokenParsed?.['family_name'],
        roles: roles
      };

      console.log('Final user profile:', user);
      this.currentUserSubject.next(user);
    } catch (e) {
      console.error('Critical error loading user profile:', e);
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
