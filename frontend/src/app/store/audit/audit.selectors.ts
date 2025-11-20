import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AuditState } from './audit.reducer';

export const selectAuditState = createFeatureSelector<AuditState>('audit');

export const selectAuditEvents = createSelector(
  selectAuditState,
  (state) => state.events
);

export const selectAuditStatistics = createSelector(
  selectAuditState,
  (state) => state.statistics
);

export const selectAuditCurrentQuery = createSelector(
  selectAuditState,
  (state) => state.currentQuery
);

export const selectAuditLoading = createSelector(
  selectAuditState,
  (state) => state.loading
);

export const selectAuditLoadingStatistics = createSelector(
  selectAuditState,
  (state) => state.loadingStatistics
);

export const selectAuditError = createSelector(
  selectAuditState,
  (state) => state.error
);

export const selectAuditPagination = createSelector(
  selectAuditState,
  (state) => ({
    totalElements: state.totalElements,
    totalPages: state.totalPages,
    currentPage: state.currentPage
  })
);
