import { createAction, props } from '@ngrx/store';
import {
  AuditEvent,
  AuditEventQuery,
  AuditEventType,
  AuditStatistics
} from '../../core/models/audit.model';

/**
 * Audit Actions (CQRS Query Side)
 */

// Load audit events
export const loadAuditEvents = createAction(
  '[Audit] Load Audit Events',
  props<{ query: AuditEventQuery; page: number; size: number }>()
);

export const loadAuditEventsSuccess = createAction(
  '[Audit] Load Audit Events Success',
  props<{ events: AuditEvent[]; totalElements: number; totalPages: number }>()
);

export const loadAuditEventsFailure = createAction(
  '[Audit] Load Audit Events Failure',
  props<{ error: string }>()
);

// Load events by user
export const loadAuditEventsByUser = createAction(
  '[Audit] Load Audit Events By User',
  props<{ userId: string; page: number; size: number }>()
);

export const loadAuditEventsByUserSuccess = createAction(
  '[Audit] Load Audit Events By User Success',
  props<{ events: AuditEvent[]; totalElements: number; totalPages: number }>()
);

export const loadAuditEventsByUserFailure = createAction(
  '[Audit] Load Audit Events By User Failure',
  props<{ error: string }>()
);

// Load events by type
export const loadAuditEventsByType = createAction(
  '[Audit] Load Audit Events By Type',
  props<{ eventType: AuditEventType; page: number; size: number }>()
);

export const loadAuditEventsByTypeSuccess = createAction(
  '[Audit] Load Audit Events By Type Success',
  props<{ events: AuditEvent[]; totalElements: number; totalPages: number }>()
);

export const loadAuditEventsByTypeFailure = createAction(
  '[Audit] Load Audit Events By Type Failure',
  props<{ error: string }>()
);

// Load events by entity
export const loadAuditEventsByEntity = createAction(
  '[Audit] Load Audit Events By Entity',
  props<{ entityType: string; entityId: string; page: number; size: number }>()
);

export const loadAuditEventsByEntitySuccess = createAction(
  '[Audit] Load Audit Events By Entity Success',
  props<{ events: AuditEvent[]; totalElements: number; totalPages: number }>()
);

export const loadAuditEventsByEntityFailure = createAction(
  '[Audit] Load Audit Events By Entity Failure',
  props<{ error: string }>()
);

// Load statistics
export const loadAuditStatistics = createAction(
  '[Audit] Load Audit Statistics'
);

export const loadAuditStatisticsSuccess = createAction(
  '[Audit] Load Audit Statistics Success',
  props<{ statistics: AuditStatistics }>()
);

export const loadAuditStatisticsFailure = createAction(
  '[Audit] Load Audit Statistics Failure',
  props<{ error: string }>()
);

// Load statistics for date range
export const loadAuditStatisticsForDateRange = createAction(
  '[Audit] Load Audit Statistics For Date Range',
  props<{ fromDate: string; toDate: string }>()
);

export const loadAuditStatisticsForDateRangeSuccess = createAction(
  '[Audit] Load Audit Statistics For Date Range Success',
  props<{ statistics: AuditStatistics }>()
);

export const loadAuditStatisticsForDateRangeFailure = createAction(
  '[Audit] Load Audit Statistics For Date Range Failure',
  props<{ error: string }>()
);

// Set filter
export const setAuditFilter = createAction(
  '[Audit] Set Audit Filter',
  props<{ query: AuditEventQuery }>()
);

// Clear filter
export const clearAuditFilter = createAction(
  '[Audit] Clear Audit Filter'
);

// Clear error
export const clearAuditError = createAction(
  '[Audit] Clear Audit Error'
);
