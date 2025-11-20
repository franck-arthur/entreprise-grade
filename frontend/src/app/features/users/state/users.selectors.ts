import { createFeatureSelector, createSelector } from '@ngrx/store';
import { UsersState, usersAdapter } from './users.reducer';

/**
 * Select users feature state.
 */
export const selectUsersState = createFeatureSelector<UsersState>('users');

/**
 * Entity selectors.
 */
const { selectIds, selectEntities, selectAll, selectTotal } = usersAdapter.getSelectors();

export const selectAllUsers = createSelector(selectUsersState, selectAll);

export const selectUserEntities = createSelector(selectUsersState, selectEntities);

export const selectUserIds = createSelector(selectUsersState, selectIds);

export const selectUserTotal = createSelector(selectUsersState, selectTotal);

/**
 * Select loading state.
 */
export const selectUsersLoading = createSelector(
  selectUsersState,
  (state: UsersState) => state.loading
);

/**
 * Select error state.
 */
export const selectUsersError = createSelector(
  selectUsersState,
  (state: UsersState) => state.error
);

/**
 * Select pagination info.
 */
export const selectUsersPagination = createSelector(selectUsersState, (state: UsersState) => ({
  totalElements: state.totalElements,
  totalPages: state.totalPages,
  currentPage: state.currentPage,
}));

/**
 * Select user by ID.
 */
export const selectUserById = (id: string) =>
  createSelector(selectUserEntities, entities => (id ? entities[id] : null));

/**
 * Select selected user.
 */
export const selectSelectedUser = createSelector(
  selectUsersState,
  selectUserEntities,
  (state, entities) => (state.selectedUserId ? entities[state.selectedUserId] : null)
);
