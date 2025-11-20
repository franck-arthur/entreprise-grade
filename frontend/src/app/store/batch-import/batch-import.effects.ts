import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { BatchImportService } from '../../core/services/batch-import.service';
import * as BatchImportActions from './batch-import.actions';

@Injectable()
export class BatchImportEffects {
  constructor(
    private actions$: Actions,
    private batchImportService: BatchImportService
  ) {}

  uploadCsvFile$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BatchImportActions.uploadCsvFile),
      switchMap(({ file }) =>
        this.batchImportService.uploadCsvFile(file).pipe(
          map((batchImport) =>
            BatchImportActions.uploadCsvFileSuccess({ batchImport })
          ),
          catchError((error) =>
            of(
              BatchImportActions.uploadCsvFileFailure({
                error: error.error?.message || 'Failed to upload file'
              })
            )
          )
        )
      )
    )
  );

  loadBatchImports$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BatchImportActions.loadBatchImports),
      switchMap(({ page, size }) =>
        this.batchImportService.getAllBatchImports(page, size).pipe(
          map((response) =>
            BatchImportActions.loadBatchImportsSuccess({
              imports: response.content,
              totalElements: response.totalElements,
              totalPages: response.totalPages
            })
          ),
          catchError((error) =>
            of(
              BatchImportActions.loadBatchImportsFailure({
                error: error.error?.message || 'Failed to load batch imports'
              })
            )
          )
        )
      )
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
