import { createReducer, on } from '@ngrx/store';
import { BatchImport, BatchImportDetail } from '../../core/models/batch-import.model';
import * as BatchImportActions from './batch-import.actions';

export interface BatchImportState {
  imports: BatchImport[];
  selectedImport: BatchImportDetail | null;
  totalElements: number;
  totalPages: number;
  currentPage: number;
  loading: boolean;
  uploadingFile: boolean;
  error: string | null;
}

export const initialState: BatchImportState = {
  imports: [],
  selectedImport: null,
  totalElements: 0,
  totalPages: 0,
  currentPage: 0,
  loading: false,
  uploadingFile: false,
  error: null
};

export const batchImportReducer = createReducer(
  initialState,

  // Upload CSV file
  on(BatchImportActions.uploadCsvFile, (state) => ({
    ...state,
    uploadingFile: true,
    error: null
  })),

  on(BatchImportActions.uploadCsvFileSuccess, (state, { batchImport }) => ({
    ...state,
    imports: [batchImport, ...state.imports],
    uploadingFile: false,
    error: null
  })),

  on(BatchImportActions.uploadCsvFileFailure, (state, { error }) => ({
    ...state,
    uploadingFile: false,
    error
  })),

  // Load batch imports
  on(BatchImportActions.loadBatchImports, (state, { page }) => ({
    ...state,
    currentPage: page,
    loading: true,
    error: null
  })),

  on(BatchImportActions.loadBatchImportsSuccess, (state, { imports, totalElements, totalPages }) => ({
    ...state,
    imports,
    totalElements,
    totalPages,
    loading: false,
    error: null
  })),

  on(BatchImportActions.loadBatchImportsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Load batch import detail
  on(BatchImportActions.loadBatchImportDetail, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(BatchImportActions.loadBatchImportDetailSuccess, (state, { batchImport }) => ({
    ...state,
    selectedImport: batchImport,
    loading: false,
    error: null
  })),

  on(BatchImportActions.loadBatchImportDetailFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Refresh batch import status
  on(BatchImportActions.refreshBatchImportStatusSuccess, (state, { batchImport }) => {
    // Update the batch import in the list if it exists
    const updatedImports = state.imports.map(imp =>
      imp.id === batchImport.id ? batchImport : imp
    );

    // Update selected import if it's the same one
    const updatedSelectedImport = state.selectedImport?.id === batchImport.id
      ? { ...state.selectedImport, ...batchImport }
      : state.selectedImport;

    return {
      ...state,
      imports: updatedImports,
      selectedImport: updatedSelectedImport,
      error: null
    };
  }),

  // Cancel batch import
  on(BatchImportActions.cancelBatchImport, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(BatchImportActions.cancelBatchImportSuccess, (state, { batchImport }) => {
    // Update the batch import in the list
    const updatedImports = state.imports.map(imp =>
      imp.id === batchImport.id ? batchImport : imp
    );

    // Update selected import if it's the same one
    const updatedSelectedImport = state.selectedImport?.id === batchImport.id
      ? { ...state.selectedImport, ...batchImport }
      : state.selectedImport;

    return {
      ...state,
      imports: updatedImports,
      selectedImport: updatedSelectedImport,
      loading: false,
      error: null
    };
  }),

  on(BatchImportActions.cancelBatchImportFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Clear error
  on(BatchImportActions.clearError, (state) => ({
    ...state,
    error: null
  }))
);
