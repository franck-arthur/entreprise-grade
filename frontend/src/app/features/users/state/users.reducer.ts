import { createReducer, on } from '@ngrx/store';
import { EntityState, EntityAdapter, createEntityAdapter } from '@ngrx/entity';
import { User } from '@app/core/models/user.model';
import * as UsersActions from './users.actions';

/**
 * Users state interface using NgRx Entity.
 */
export interface UsersState extends EntityState<User> {
  selectedUserId: string | null;
  loading: boolean;
  error: string | null;
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

/**
 * Entity adapter for users.
 */
export const usersAdapter: EntityAdapter<User> = createEntityAdapter<User>({
  selectId: (user: User) => user.id,
  sortComparer: false,
});

/**
 * Initial users state.
 */
export const initialState: UsersState = usersAdapter.getInitialState({
  selectedUserId: null,
  loading: false,
  error: null,
  totalElements: 0,
  totalPages: 0,
  currentPage: 0,
});

/**
 * Users reducer.
 */
export const usersReducer = createReducer(
  initialState,

  // Load users
  on(UsersActions.loadUsers, (state, { page }) => ({
    ...state,
    loading: true,
    error: null,
    currentPage: page,
  })),

  on(UsersActions.loadUsersSuccess, (state, { users, totalElements, totalPages }) =>
    usersAdapter.setAll(users, {
      ...state,
      loading: false,
      totalElements,
      totalPages,
    })
  ),

  on(UsersActions.loadUsersFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Load single user
  on(UsersActions.loadUser, state => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(UsersActions.loadUserSuccess, (state, { user }) =>
    usersAdapter.upsertOne(user, {
      ...state,
      loading: false,
      selectedUserId: user.id,
    })
  ),

  on(UsersActions.loadUserFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Create user
  on(UsersActions.createUser, state => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(UsersActions.createUserSuccess, (state, { user }) =>
    usersAdapter.addOne(user, {
      ...state,
      loading: false,
    })
  ),

  on(UsersActions.createUserFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Update user
  on(UsersActions.updateUser, state => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(UsersActions.updateUserSuccess, (state, { user }) =>
    usersAdapter.updateOne({ id: user.id, changes: user }, {
      ...state,
      loading: false,
    })
  ),

  on(UsersActions.updateUserFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Delete user
  on(UsersActions.deleteUser, state => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(UsersActions.deleteUserSuccess, (state, { id }) =>
    usersAdapter.removeOne(id, {
      ...state,
      loading: false,
    })
  ),

  on(UsersActions.deleteUserFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Clear error
  on(UsersActions.clearError, state => ({
    ...state,
    error: null,
  }))
);
