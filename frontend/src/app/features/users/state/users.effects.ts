import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';
import { UsersService } from '../services/users.service';
import * as UsersActions from './users.actions';

/**
 * Users effects for handling side effects.
 */
@Injectable()
export class UsersEffects {
  /**
   * Load users effect.
   */
  loadUsers$ = createEffect(() =>
    this.actions$.pipe(
      ofType(UsersActions.loadUsers),
      switchMap(({ page, size }) =>
        this.usersService.getUsers(page, size).pipe(
          map(response =>
            UsersActions.loadUsersSuccess({
              users: response.content,
              totalElements: response.totalElements,
              totalPages: response.totalPages,
            })
          ),
          catchError(error =>
            of(UsersActions.loadUsersFailure({ error: error.message }))
          )
        )
      )
    )
  );

  /**
   * Load user effect.
   */
  loadUser$ = createEffect(() =>
    this.actions$.pipe(
      ofType(UsersActions.loadUser),
      switchMap(({ id }) =>
        this.usersService.getUserById(id).pipe(
          map(user => UsersActions.loadUserSuccess({ user })),
          catchError(error =>
            of(UsersActions.loadUserFailure({ error: error.message }))
          )
        )
      )
    )
  );

  /**
   * Create user effect.
   */
  createUser$ = createEffect(() =>
    this.actions$.pipe(
      ofType(UsersActions.createUser),
      switchMap(({ request }) =>
        this.usersService.createUser(request).pipe(
          map(user => UsersActions.createUserSuccess({ user })),
          catchError(error =>
            of(UsersActions.createUserFailure({ error: error.message }))
          )
        )
      )
    )
  );

  /**
   * Update user effect.
   */
  updateUser$ = createEffect(() =>
    this.actions$.pipe(
      ofType(UsersActions.updateUser),
      switchMap(({ id, request }) =>
        this.usersService.updateUser(id, request).pipe(
          map(user => UsersActions.updateUserSuccess({ user })),
          catchError(error =>
            of(UsersActions.updateUserFailure({ error: error.message }))
          )
        )
      )
    )
  );

  /**
   * Delete user effect.
   */
  deleteUser$ = createEffect(() =>
    this.actions$.pipe(
      ofType(UsersActions.deleteUser),
      switchMap(({ id }) =>
        this.usersService.deleteUser(id).pipe(
          map(() => UsersActions.deleteUserSuccess({ id })),
          catchError(error =>
            of(UsersActions.deleteUserFailure({ error: error.message }))
          )
        )
      )
    )
  );

  constructor(
    private actions$: Actions,
    private usersService: UsersService
  ) {}
}
