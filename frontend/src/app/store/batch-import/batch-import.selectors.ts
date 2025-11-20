import { createFeatureSelector, createSelector } from '@ngrx/store';
import { BatchImportState } from './batch-import.reducer';

export const selectBatchImportState = createFeatureSelector<BatchImportState>('batchImport');

export const selectAllBatchImports = createSelector(
  selectBatchImportState,
  (state) => state.imports
);

export const selectSelectedBatchImport = createSelector(
  selectBatchImportState,
  (state) => state.selectedImport
);

export const selectBatchImportLoading = createSelector(
  selectBatchImportState,
  (state) => state.loading
);

export const selectBatchImportUploadingFile = createSelector(
  selectBatchImportState,
  (state) => state.uploadingFile
);

export const selectBatchImportError = createSelector(
  selectBatchImportState,
  (state) => state.error
);

export const selectBatchImportPagination = createSelector(
  selectBatchImportState,
  (state) => ({
    totalElements: state.totalElements,
    totalPages: state.totalPages,
    currentPage: state.currentPage
  })
);

export const selectBatchImportById = (id: string) => createSelector(
  selectAllBatchImports,
  (imports) => imports.find(imp => imp.id === id)
);
