import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuditEvent,
  AuditEventPage,
  AuditEventQuery,
  AuditEventType,
  AuditStatistics
} from '../models/audit.model';

/**
 * Service for audit operations (CQRS Query Side).
 * Handles all read operations for audit events.
 */
@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly apiUrl = `${environment.apiUrl}/audit`;

  constructor(private http: HttpClient) {}

  /**
   * Get audit events with filters.
   */
  getAuditEvents(query: AuditEventQuery, page: number = 0, size: number = 50): Observable<AuditEventPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'timestamp,desc');

    // Add filters
    if (query.eventTypes && query.eventTypes.length > 0) {
      query.eventTypes.forEach(type => {
        params = params.append('eventTypes', type);
      });
    }

    if (query.eventCategory) {
      params = params.set('eventCategory', query.eventCategory);
    }

    if (query.userId) {
      params = params.set('userId', query.userId);
    }

    if (query.username) {
      params = params.set('username', query.username);
    }

    if (query.targetEntityType) {
      params = params.set('targetEntityType', query.targetEntityType);
    }

    if (query.targetEntityId) {
      params = params.set('targetEntityId', query.targetEntityId);
    }

    if (query.success !== undefined && query.success !== null) {
      params = params.set('success', query.success.toString());
    }

    if (query.fromDate) {
      params = params.set('fromDate', query.fromDate);
    }

    if (query.toDate) {
      params = params.set('toDate', query.toDate);
    }

    if (query.ipAddress) {
      params = params.set('ipAddress', query.ipAddress);
    }

    return this.http.get<AuditEventPage>(this.apiUrl, { params });
  }

  /**
   * Get audit events for a specific user.
   */
  getAuditEventsByUser(userId: string, page: number = 0, size: number = 50): Observable<AuditEventPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'timestamp,desc');

    return this.http.get<AuditEventPage>(`${this.apiUrl}/user/${userId}`, { params });
  }

  /**
   * Get audit events by type.
   */
  getAuditEventsByType(eventType: AuditEventType, page: number = 0, size: number = 50): Observable<AuditEventPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'timestamp,desc');

    return this.http.get<AuditEventPage>(`${this.apiUrl}/type/${eventType}`, { params });
  }

  /**
   * Get audit events for a specific entity.
   */
  getAuditEventsByEntity(
    entityType: string,
    entityId: string,
    page: number = 0,
    size: number = 50
  ): Observable<AuditEventPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'timestamp,desc');

    return this.http.get<AuditEventPage>(`${this.apiUrl}/entity/${entityType}/${entityId}`, { params });
  }

  /**
   * Get audit statistics.
   */
  getStatistics(): Observable<AuditStatistics> {
    return this.http.get<AuditStatistics>(`${this.apiUrl}/statistics`);
  }

  /**
   * Get audit statistics for date range.
   */
  getStatisticsForDateRange(fromDate: string, toDate: string): Observable<AuditStatistics> {
    const params = new HttpParams()
      .set('fromDate', fromDate)
      .set('toDate', toDate);

    return this.http.get<AuditStatistics>(`${this.apiUrl}/statistics/range`, { params });
  }

  /**
   * Get hourly statistics for a specific date.
   */
  getHourlyStatistics(date: string): Observable<Record<string, number>> {
    const params = new HttpParams().set('date', date);
    return this.http.get<Record<string, number>>(`${this.apiUrl}/statistics/hourly`, { params });
  }
}
