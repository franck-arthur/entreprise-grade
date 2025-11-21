import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { BehaviorSubject, Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';

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

  constructor(private keycloak: KeycloakService) {
    this.loadUserProfile();
  }

  private async loadUserProfile(): Promise<void> {
    if (await this.keycloak.isLoggedIn()) {
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
    }
  }

  isAuthenticated(): boolean {
    return this.keycloak.isLoggedIn() as unknown as boolean;
  }

  async isAuthenticatedAsync(): Promise<boolean> {
    return this.keycloak.isLoggedIn();
  }

  getToken(): string | null {
    return this.keycloak.getToken() as unknown as string | null;
  }

  async getTokenAsync(): Promise<string> {
    return this.keycloak.getToken();
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
    this.keycloak.logout(window.location.origin);
  }

  register(): void {
    this.keycloak.register();
  }
}
