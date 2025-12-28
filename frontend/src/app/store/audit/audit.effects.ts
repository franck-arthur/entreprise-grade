import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { AuditService } from '../../core/services/audit.service';
import { AuthService } from '../../core/services/auth.service';
import * as AuditActions from './audit.actions';

@Injectable()
export class AuditEffects {
  constructor(
    private actions$: Actions,
    private auditService: AuditService,
    private authService: AuthService
  ) {}

  /**
   * Check authentication and redirect to login if not authenticated.
   */
  private checkAuthAndRedirect(): boolean {
    if (!this.authService.isAuthenticated()) {
      console.warn('User not authenticated for audit operation, redirecting to login');
      this.authService.login();
      return false;
    }
    return true;
  }

  loadAuditEvents$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuditActions.loadAuditEvents),
      switchMap(({ query, page, size }) => {
        // Check authentication before making API call
        if (!this.checkAuthAndRedirect()) {
          return of(AuditActions.loadAuditEventsFailure({
            error: 'Authentication required'
          }));
        }

        return this.auditService.getAuditEvents(query, page, size).pipe(
          map((response) =>
            AuditActions.loadAuditEventsSuccess({
              events: response.content,
              totalElements: response.totalElements,
              totalPages: response.totalPages
            })
          ),
          catchError((error) => {
            // Handle authentication errors
            if (error.message?.includes('401')) {
              console.warn('Authentication failed in audit events, redirecting to login');
              this.authService.login();
              return of(AuditActions.loadAuditEventsFailure({
                error: 'Authentication failed'
              }));
            }

            return of(AuditActions.loadAuditEventsFailure({
              error: error.error?.message || 'Failed to load audit events'
            }));
          })
        );
      })
    )
  );

  loadAuditEventsByUser$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuditActions.loadAuditEventsByUser),
      switchMap(({ userId, page, size }) =>
        this.auditService.getAuditEventsByUser(userId, page, size).pipe(
          map((response) =>
            AuditActions.loadAuditEventsByUserSuccess({
              events: response.content,
              totalElements: response.totalElements,
              totalPages: response.totalPages
            })
          ),
          catchError((error) =>
            of(
              AuditActions.loadAuditEventsByUserFailure({
                error: error.error?.message || 'Failed to load user audit events'
              })
            )
          )
        )
      )
    )
  );

  loadAuditEventsByType$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuditActions.loadAuditEventsByType),
      switchMap(({ eventType, page, size }) =>
        this.auditService.getAuditEventsByType(eventType, page, size).pipe(
          map((response) =>
            AuditActions.loadAuditEventsByTypeSuccess({
              events: response.content,
              totalElements: response.totalElements,
              totalPages: response.totalPages
            })
          ),
          catchError((error) =>
            of(
              AuditActions.loadAuditEventsByTypeFailure({
                error: error.error?.message || 'Failed to load audit events by type'
              })
            )
          )
        )
      )
    )
  );

  loadAuditEventsByEntity$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuditActions.loadAuditEventsByEntity),
      switchMap(({ entityType, entityId, page, size }) =>
        this.auditService.getAuditEventsByEntity(entityType, entityId, page, size).pipe(
          map((response) =>
            AuditActions.loadAuditEventsByEntitySuccess({
              events: response.content,
              totalElements: response.totalElements,
              totalPages: response.totalPages
            })
          ),
          catchError((error) =>
            of(
              AuditActions.loadAuditEventsByEntityFailure({
                error: error.error?.message || 'Failed to load audit events by entity'
              })
            )
          )
        )
      )
    )
  );

  loadAuditStatistics$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuditActions.loadAuditStatistics),
      switchMap(() => {
        // Check authentication before making API call
        if (!this.checkAuthAndRedirect()) {
          return of(AuditActions.loadAuditStatisticsFailure({
            error: 'Authentication required'
          }));
        }

        return this.auditService.getStatistics().pipe(
          map((statistics) =>
            AuditActions.loadAuditStatisticsSuccess({ statistics })
          ),
          catchError((error) => {
            // Handle authentication errors
            if (error.message?.includes('401')) {
              console.warn('Authentication failed in audit statistics, redirecting to login');
              this.authService.login();
              return of(AuditActions.loadAuditStatisticsFailure({
                error: 'Authentication failed'
              }));
            }

            return of(AuditActions.loadAuditStatisticsFailure({
              error: error.error?.message || 'Failed to load audit statistics'
            }));
          })
        );
      })
    )
  );

  loadAuditStatisticsForDateRange$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuditActions.loadAuditStatisticsForDateRange),
      switchMap(({ fromDate, toDate }) =>
        this.auditService.getStatisticsForDateRange(fromDate, toDate).pipe(
          map((statistics) =>
            AuditActions.loadAuditStatisticsForDateRangeSuccess({ statistics })
          ),
          catchError((error) =>
            of(
              AuditActions.loadAuditStatisticsForDateRangeFailure({
                error: error.error?.message || 'Failed to load audit statistics for date range'
              })
            )
          )
        )
      )
    )
  );
}
