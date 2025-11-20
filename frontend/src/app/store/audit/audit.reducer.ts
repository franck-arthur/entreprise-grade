import { createReducer, on } from '@ngrx/store';
import { AuditEvent, AuditEventQuery, AuditStatistics } from '../../core/models/audit.model';
import * as AuditActions from './audit.actions';

export interface AuditState {
  events: AuditEvent[];
  statistics: AuditStatistics | null;
  currentQuery: AuditEventQuery;
  totalElements: number;
  totalPages: number;
  currentPage: number;
  loading: boolean;
  loadingStatistics: boolean;
  error: string | null;
}

export const initialState: AuditState = {
  events: [],
  statistics: null,
  currentQuery: {},
  totalElements: 0,
  totalPages: 0,
  currentPage: 0,
  loading: false,
  loadingStatistics: false,
  error: null
};

export const auditReducer = createReducer(
  initialState,

  // Load audit events
  on(AuditActions.loadAuditEvents, (state, { query, page }) => ({
    ...state,
    currentQuery: query,
    currentPage: page,
    loading: true,
    error: null
  })),

  on(AuditActions.loadAuditEventsSuccess, (state, { events, totalElements, totalPages }) => ({
    ...state,
    events,
    totalElements,
    totalPages,
    loading: false,
    error: null
  })),

  on(AuditActions.loadAuditEventsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Load events by user
  on(AuditActions.loadAuditEventsByUser, (state, { page }) => ({
    ...state,
    currentPage: page,
    loading: true,
    error: null
  })),

  on(AuditActions.loadAuditEventsByUserSuccess, (state, { events, totalElements, totalPages }) => ({
    ...state,
    events,
    totalElements,
    totalPages,
    loading: false,
    error: null
  })),

  on(AuditActions.loadAuditEventsByUserFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Load events by type
  on(AuditActions.loadAuditEventsByType, (state, { page }) => ({
    ...state,
    currentPage: page,
    loading: true,
    error: null
  })),

  on(AuditActions.loadAuditEventsByTypeSuccess, (state, { events, totalElements, totalPages }) => ({
    ...state,
    events,
    totalElements,
    totalPages,
    loading: false,
    error: null
  })),

  on(AuditActions.loadAuditEventsByTypeFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Load events by entity
  on(AuditActions.loadAuditEventsByEntity, (state, { page }) => ({
    ...state,
    currentPage: page,
    loading: true,
    error: null
  })),

  on(AuditActions.loadAuditEventsByEntitySuccess, (state, { events, totalElements, totalPages }) => ({
    ...state,
    events,
    totalElements,
    totalPages,
    loading: false,
    error: null
  })),

  on(AuditActions.loadAuditEventsByEntityFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Load statistics
  on(AuditActions.loadAuditStatistics, (state) => ({
    ...state,
    loadingStatistics: true,
    error: null
  })),

  on(AuditActions.loadAuditStatisticsSuccess, (state, { statistics }) => ({
    ...state,
    statistics,
    loadingStatistics: false,
    error: null
  })),

  on(AuditActions.loadAuditStatisticsFailure, (state, { error }) => ({
    ...state,
    loadingStatistics: false,
    error
  })),

  // Load statistics for date range
  on(AuditActions.loadAuditStatisticsForDateRange, (state) => ({
    ...state,
    loadingStatistics: true,
    error: null
  })),

  on(AuditActions.loadAuditStatisticsForDateRangeSuccess, (state, { statistics }) => ({
    ...state,
    statistics,
    loadingStatistics: false,
    error: null
  })),

  on(AuditActions.loadAuditStatisticsForDateRangeFailure, (state, { error }) => ({
    ...state,
    loadingStatistics: false,
    error
  })),

  // Set filter
  on(AuditActions.setAuditFilter, (state, { query }) => ({
    ...state,
    currentQuery: query
  })),

  // Clear filter
  on(AuditActions.clearAuditFilter, (state) => ({
    ...state,
    currentQuery: {},
    currentPage: 0
  })),

  // Clear error
  on(AuditActions.clearAuditError, (state) => ({
    ...state,
    error: null
  }))
);
