import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';
import {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  PagedResponse,
} from '@app/core/models/user.model';

/**
 * Users API service.
 *
 * Handles all HTTP requests related to user management.
 */
@Injectable({
  providedIn: 'root',
})
export class UsersService {
  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  /**
   * Get all users with pagination.
   */
  getUsers(page: number = 0, size: number = 20): Observable<PagedResponse<User>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http.get<PagedResponse<User>>(this.apiUrl, { params });
  }

  /**
   * Get user by ID.
   */
  getUserById(id: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get user by username.
   */
  getUserByUsername(username: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/username/${username}`);
  }

  /**
   * Create new user.
   */
  createUser(request: CreateUserRequest): Observable<User> {
    return this.http.post<User>(this.apiUrl, request);
  }

  /**
   * Update user.
   */
  updateUser(id: string, request: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete user.
   */
  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Activate user.
   */
  activateUser(id: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/activate`, {});
  }

  /**
   * Deactivate user.
   */
  deactivateUser(id: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/deactivate`, {});
  }
}
