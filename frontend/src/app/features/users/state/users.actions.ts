import { createAction, props } from '@ngrx/store';
import { User, CreateUserRequest, UpdateUserRequest } from '@app/core/models/user.model';

// Load users
export const loadUsers = createAction('[Users] Load Users', props<{ page: number; size: number }>());

export const loadUsersSuccess = createAction(
  '[Users] Load Users Success',
  props<{ users: User[]; totalElements: number; totalPages: number }>()
);

export const loadUsersFailure = createAction(
  '[Users] Load Users Failure',
  props<{ error: string }>()
);

// Load single user
export const loadUser = createAction('[Users] Load User', props<{ id: string }>());

export const loadUserSuccess = createAction('[Users] Load User Success', props<{ user: User }>());

export const loadUserFailure = createAction(
  '[Users] Load User Failure',
  props<{ error: string }>()
);

// Create user
export const createUser = createAction(
  '[Users] Create User',
  props<{ request: CreateUserRequest }>()
);

export const createUserSuccess = createAction(
  '[Users] Create User Success',
  props<{ user: User }>()
);

export const createUserFailure = createAction(
  '[Users] Create User Failure',
  props<{ error: string }>()
);

// Update user
export const updateUser = createAction(
  '[Users] Update User',
  props<{ id: string; request: UpdateUserRequest }>()
);

export const updateUserSuccess = createAction(
  '[Users] Update User Success',
  props<{ user: User }>()
);

export const updateUserFailure = createAction(
  '[Users] Update User Failure',
  props<{ error: string }>()
);

// Delete user
export const deleteUser = createAction('[Users] Delete User', props<{ id: string }>());

export const deleteUserSuccess = createAction(
  '[Users] Delete User Success',
  props<{ id: string }>()
);

export const deleteUserFailure = createAction(
  '[Users] Delete User Failure',
  props<{ error: string }>()
);

// Clear error
export const clearError = createAction('[Users] Clear Error');
