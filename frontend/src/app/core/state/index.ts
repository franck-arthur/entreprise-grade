import { ActionReducerMap, MetaReducer } from '@ngrx/store';
import { environment } from '@environments/environment';
import { BatchImportState, batchImportReducer } from '../../store/batch-import/batch-import.reducer';
import { AuditState, auditReducer } from '../../store/audit/audit.reducer';

/**
 * Root state interface.
 *
 * Add feature states here as the application grows.
 */
export interface AppState {
  batchImport: BatchImportState;
  audit: AuditState;
}

/**
 * Root reducers.
 *
 * Combines all feature reducers into a single reducer map.
 */
export const reducers: ActionReducerMap<AppState> = {
  batchImport: batchImportReducer,
  audit: auditReducer
};

/**
 * Meta-reducers for cross-cutting concerns.
 *
 * Meta-reducers run before the actual reducers and can modify actions or state.
 */
export const metaReducers: MetaReducer<AppState>[] = !environment.production ? [] : [];
