import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Authentication service.
 *
 * Manages user authentication state, token storage, and role checking.
 * In a real application, this would integrate with Keycloak or another OAuth2 provider.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';

  private currentUserSubject = new BehaviorSubject<any>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor() {}

  /**
   * Check if user is authenticated.
   */
  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }

    // Check if token is expired
    // In production, you'd decode the JWT and check the 'exp' claim
    return !this.isTokenExpired(token);
  }

  /**
   * Get current auth token.
   */
  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Set auth token.
   */
  setToken(token: string): void {
    sessionStorage.setItem(this.TOKEN_KEY, token);
  }

  /**
   * Remove auth token.
   */
  removeToken(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
  }

  /**
   * Get current user.
   */
  getCurrentUser(): any {
    return this.currentUserSubject.value;
  }

  /**
   * Set current user.
   */
  setCurrentUser(user: any): void {
    sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  /**
   * Get user from storage.
   */
  private getUserFromStorage(): any {
    const userJson = sessionStorage.getItem(this.USER_KEY);
    return userJson ? JSON.parse(userJson) : null;
  }

  /**
   * Check if token is expired.
   * This is a simplified version - in production, decode the JWT properly.
   */
  private isTokenExpired(token: string): boolean {
    try {
      // Check if token has valid JWT structure (three parts separated by dots)
      const parts = token.split('.');
      if (parts.length !== 3) {
        return true;
      }

      // Parse JWT payload
      const payload = JSON.parse(atob(parts[1]));
      const expiry = payload.exp;

      // If no expiry claim, consider token valid
      if (!expiry) {
        return false;
      }

      const now = Math.floor(Date.now() / 1000);
      return expiry < now;
    } catch (error) {
      console.error('Error checking token expiration:', error);
      return true;
    }
  }

  /**
   * Check if user has a specific role.
   */
  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    if (!user || !user.roles) {
      return false;
    }

    return user.roles.includes(role);
  }

  /**
   * Check if user has any of the specified roles.
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  /**
   * Login (mock implementation).
   * In production, this would redirect to Keycloak or call an auth endpoint.
   */
  login(username: string, password: string): Observable<any> {
    // Mock implementation
    // In production: return this.http.post('/auth/login', { username, password });

    const mockUser = {
      id: '123',
      username: username,
      email: `${username}@example.com`,
      roles: ['USER', 'ADMIN'],
    };

    // Create a mock JWT token with valid structure (header.payload.signature)
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({
      sub: mockUser.id,
      username: mockUser.username,
      exp: Math.floor(Date.now() / 1000) + 3600 // 1 hour from now
    }));
    const mockToken = `${header}.${payload}.mock-signature`;

    this.setToken(mockToken);
    this.setCurrentUser(mockUser);

    return new BehaviorSubject(mockUser).asObservable();
  }

  /**
   * Logout.
   */
  logout(): void {
    this.removeToken();
    sessionStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);

    // In production with Keycloak, you'd also redirect to Keycloak logout URL
  }
}
