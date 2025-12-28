import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { BatchImportService } from '../../core/services/batch-import.service';
import { AuthService } from '../../core/services/auth.service';
import * as BatchImportActions from './batch-import.actions';

@Injectable()
export class BatchImportEffects {
  constructor(
    private actions$: Actions,
    private batchImportService: BatchImportService,
    private authService: AuthService
  ) {}

  /**
   * Check authentication and redirect to login if not authenticated.
   */
  private checkAuthAndRedirect(): boolean {
    if (!this.authService.isAuthenticated()) {
      console.warn('User not authenticated for batch import operation, redirecting to login');
      this.authService.login();
      return false;
    }
    return true;
  }

  uploadCsvFile$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BatchImportActions.uploadCsvFile),
      switchMap(({ file }) => {
        // Check authentication before making API call
        if (!this.checkAuthAndRedirect()) {
          return of(BatchImportActions.uploadCsvFileFailure({
            error: 'Authentication required'
          }));
        }

        return this.batchImportService.uploadCsvFile(file).pipe(
          map((batchImport) =>
            BatchImportActions.uploadCsvFileSuccess({ batchImport })
          ),
          catchError((error) => {
            // Handle authentication errors
            if (error.message?.includes('401')) {
              console.warn('Authentication failed in file upload, redirecting to login');
              this.authService.login();
              return of(BatchImportActions.uploadCsvFileFailure({
                error: 'Authentication failed'
              }));
            }

            return of(BatchImportActions.uploadCsvFileFailure({
              error: error.error?.message || 'Failed to upload file'
            }));
          })
        );
      })
    )
  );

  loadBatchImports$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BatchImportActions.loadBatchImports),
      switchMap(({ page, size }) => {
        // Check authentication before making API call
        if (!this.checkAuthAndRedirect()) {
          return of(BatchImportActions.loadBatchImportsFailure({
            error: 'Authentication required'
          }));
        }

        return this.batchImportService.getAllBatchImports(page, size).pipe(
          map((response) =>
            BatchImportActions.loadBatchImportsSuccess({
              imports: response.content,
              totalElements: response.totalElements,
              totalPages: response.totalPages
            })
          ),
          catchError((error) => {
            // Handle authentication errors
            if (error.message?.includes('401')) {
              console.warn('Authentication failed in batch imports, redirecting to login');
              this.authService.login();
              return of(BatchImportActions.loadBatchImportsFailure({
                error: 'Authentication failed'
              }));
            }

            return of(BatchImportActions.loadBatchImportsFailure({
              error: error.error?.message || 'Failed to load batch imports'
            }));
          })
        );
      })
    )
  );

  loadBatchImportDetail$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BatchImportActions.loadBatchImportDetail),
      switchMap(({ id }) =>
        this.batchImportService.getBatchImportById(id).pipe(
          map((batchImport) =>
            BatchImportActions.loadBatchImportDetailSuccess({ batchImport })
          ),
          catchError((error) =>
            of(
              BatchImportActions.loadBatchImportDetailFailure({
                error: error.error?.message || 'Failed to load batch import details'
              })
            )
          )
        )
      )
    )
  );

  refreshBatchImportStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BatchImportActions.refreshBatchImportStatus),
      switchMap(({ id }) =>
        this.batchImportService.getBatchImportStatus(id).pipe(
          map((batchImport) =>
            BatchImportActions.refreshBatchImportStatusSuccess({ batchImport })
          ),
          catchError((error) =>
            of(
              BatchImportActions.refreshBatchImportStatusFailure({
                error: error.error?.message || 'Failed to refresh status'
              })
            )
          )
        )
      )
    )
  );

  cancelBatchImport$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BatchImportActions.cancelBatchImport),
      switchMap(({ id }) =>
        this.batchImportService.cancelBatchImport(id).pipe(
          map((batchImport) =>
            BatchImportActions.cancelBatchImportSuccess({ batchImport })
          ),
          catchError((error) =>
            of(
              BatchImportActions.cancelBatchImportFailure({
                error: error.error?.message || 'Failed to cancel batch import'
              })
            )
          )
        )
      )
    )
  );
}
