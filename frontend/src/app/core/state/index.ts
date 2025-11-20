import { ActionReducerMap, MetaReducer } from '@ngrx/store';
import { environment } from '@environments/environment';

/**
 * Root state interface.
 *
 * Add feature states here as the application grows.
 */
export interface AppState {}

/**
 * Root reducers.
 *
 * Combines all feature reducers into a single reducer map.
 */
export const reducers: ActionReducerMap<AppState> = {};

/**
 * Meta-reducers for cross-cutting concerns.
 *
 * Meta-reducers run before the actual reducers and can modify actions or state.
 */
export const metaReducers: MetaReducer<AppState>[] = !environment.production ? [] : [];
