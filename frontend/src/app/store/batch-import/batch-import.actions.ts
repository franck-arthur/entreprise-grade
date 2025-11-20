import { createAction, props } from '@ngrx/store';
import { BatchImport, BatchImportDetail } from '../../core/models/batch-import.model';

/**
 * Batch Import Actions
 */

// Upload CSV file
export const uploadCsvFile = createAction(
  '[Batch Import] Upload CSV File',
  props<{ file: File }>()
);

export const uploadCsvFileSuccess = createAction(
  '[Batch Import] Upload CSV File Success',
  props<{ batchImport: BatchImport }>()
);

export const uploadCsvFileFailure = createAction(
  '[Batch Import] Upload CSV File Failure',
  props<{ error: string }>()
);

// Load batch imports
export const loadBatchImports = createAction(
  '[Batch Import] Load Batch Imports',
  props<{ page: number; size: number }>()
);

export const loadBatchImportsSuccess = createAction(
  '[Batch Import] Load Batch Imports Success',
  props<{ imports: BatchImport[]; totalElements: number; totalPages: number }>()
);

export const loadBatchImportsFailure = createAction(
  '[Batch Import] Load Batch Imports Failure',
  props<{ error: string }>()
);

// Load batch import detail
export const loadBatchImportDetail = createAction(
  '[Batch Import] Load Batch Import Detail',
  props<{ id: string }>()
);

export const loadBatchImportDetailSuccess = createAction(
  '[Batch Import] Load Batch Import Detail Success',
  props<{ batchImport: BatchImportDetail }>()
);

export const loadBatchImportDetailFailure = createAction(
  '[Batch Import] Load Batch Import Detail Failure',
  props<{ error: string }>()
);

// Refresh batch import status (for polling)
export const refreshBatchImportStatus = createAction(
  '[Batch Import] Refresh Batch Import Status',
  props<{ id: string }>()
);

export const refreshBatchImportStatusSuccess = createAction(
  '[Batch Import] Refresh Batch Import Status Success',
  props<{ batchImport: BatchImport }>()
);

export const refreshBatchImportStatusFailure = createAction(
  '[Batch Import] Refresh Batch Import Status Failure',
  props<{ error: string }>()
);

// Cancel batch import
export const cancelBatchImport = createAction(
  '[Batch Import] Cancel Batch Import',
  props<{ id: string }>()
);

export const cancelBatchImportSuccess = createAction(
  '[Batch Import] Cancel Batch Import Success',
  props<{ batchImport: BatchImport }>()
);

export const cancelBatchImportFailure = createAction(
  '[Batch Import] Cancel Batch Import Failure',
  props<{ error: string }>()
);

// Clear error
export const clearError = createAction(
  '[Batch Import] Clear Error'
);
