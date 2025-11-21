import { batchImportReducer, initialState, BatchImportState } from './batch-import.reducer';
import * as BatchImportActions from './batch-import.actions';
import { BatchImport, BatchImportDetail, BatchImportStatus } from '../../core/models/batch-import.model';

describe('BatchImport Reducer', () => {
  describe('unknown action', () => {
    it('should return the initial state', () => {
      const action = { type: 'Unknown' } as any;
      const result = batchImportReducer(initialState, action);

      expect(result).toBe(initialState);
    });
  });

  describe('uploadCsvFile', () => {
    it('should set uploadingFile to true', () => {
      const file = new File(['test'], 'test.csv', { type: 'text/csv' });
      const action = BatchImportActions.uploadCsvFile({ file });
      const result = batchImportReducer(initialState, action);

      expect(result.uploadingFile).toBe(true);
      expect(result.error).toBeNull();
    });
  });

  describe('uploadCsvFileSuccess', () => {
    it('should add batch import and clear uploading flag', () => {
      const batchImport: BatchImport = {
        id: 'batch-1',
        fileName: 'test.csv',
        fileSize: 1024,
        status: BatchImportStatus.PENDING,
        totalLines: 10,
        processedLines: 0,
        successLines: 0,
        failedLines: 0,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:00:00',
        startedAt: undefined,
        completedAt: null
      };
      const uploadingState: BatchImportState = { ...initialState, uploadingFile: true };
      const action = BatchImportActions.uploadCsvFileSuccess({ batchImport });
      const result = batchImportReducer(uploadingState, action);

      expect(result.uploadingFile).toBe(false);
      expect(result.imports).toEqual([batchImport]);
      expect(result.imports.length).toBe(1);
      expect(result.error).toBeNull();
    });

    it('should prepend new batch import to existing imports', () => {
      const existingImport: BatchImport = {
        id: 'batch-1',
        fileName: 'old.csv',
        fileSize: 512,
        status: BatchImportStatus.COMPLETED,
        totalLines: 5,
        processedLines: 5,
        successLines: 5,
        failedLines: 0,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        createdAt: '2024-01-01T09:00:00',
        updatedAt: '2024-01-01T09:10:00',
        startedAt: '2024-01-01T09:01:00',
        completedAt: '2024-01-01T09:10:00'
      };
      const newImport: BatchImport = {
        id: 'batch-2',
        fileName: 'new.csv',
        fileSize: 1024,
        status: BatchImportStatus.PENDING,
        totalLines: 10,
        processedLines: 0,
        successLines: 0,
        failedLines: 0,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:00:00',
        startedAt: undefined,
        completedAt: null
      };
      const stateWithImports: BatchImportState = { ...initialState, imports: [existingImport] };
      const action = BatchImportActions.uploadCsvFileSuccess({ batchImport: newImport });
      const result = batchImportReducer(stateWithImports, action);

      expect(result.imports.length).toBe(2);
      expect(result.imports[0]).toEqual(newImport); // New import first
      expect(result.imports[1]).toEqual(existingImport);
    });
  });

  describe('uploadCsvFileFailure', () => {
    it('should set error and clear uploading flag', () => {
      const uploadingState: BatchImportState = { ...initialState, uploadingFile: true };
      const errorMessage = 'Upload failed';
      const action = BatchImportActions.uploadCsvFileFailure({ error: errorMessage });
      const result = batchImportReducer(uploadingState, action);

      expect(result.uploadingFile).toBe(false);
      expect(result.error).toBe(errorMessage);
    });
  });

  describe('loadBatchImports', () => {
    it('should set loading and page', () => {
      const action = BatchImportActions.loadBatchImports({ page: 1, size: 20 });
      const result = batchImportReducer(initialState, action);

      expect(result.loading).toBe(true);
      expect(result.currentPage).toBe(1);
      expect(result.error).toBeNull();
    });
  });

  describe('loadBatchImportsSuccess', () => {
    it('should update imports and pagination', () => {
      const imports: BatchImport[] = [{
        id: 'batch-1',
        fileName: 'test.csv',
        fileSize: 1024,
        status: BatchImportStatus.COMPLETED,
        totalLines: 10,
        processedLines: 10,
        successLines: 10,
        failedLines: 0,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:10:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: '2024-01-01T10:10:00'
      }];
      const action = BatchImportActions.loadBatchImportsSuccess({
        imports,
        totalElements: 50,
        totalPages: 3
      });
      const result = batchImportReducer(initialState, action);

      expect(result.imports).toEqual(imports);
      expect(result.totalElements).toBe(50);
      expect(result.totalPages).toBe(3);
      expect(result.loading).toBe(false);
      expect(result.error).toBeNull();
    });
  });

  describe('loadBatchImportsFailure', () => {
    it('should set error and clear loading', () => {
      const loadingState: BatchImportState = { ...initialState, loading: true };
      const errorMessage = 'Failed to load imports';
      const action = BatchImportActions.loadBatchImportsFailure({ error: errorMessage });
      const result = batchImportReducer(loadingState, action);

      expect(result.loading).toBe(false);
      expect(result.error).toBe(errorMessage);
    });
  });

  describe('loadBatchImportDetail', () => {
    it('should set loading', () => {
      const action = BatchImportActions.loadBatchImportDetail({ id: 'batch-1' });
      const result = batchImportReducer(initialState, action);

      expect(result.loading).toBe(true);
      expect(result.error).toBeNull();
    });
  });

  describe('loadBatchImportDetailSuccess', () => {
    it('should set selected import', () => {
      const batchImport: BatchImportDetail = {
        id: 'batch-1',
        fileName: 'test.csv',
        fileSize: 1024,
        status: BatchImportStatus.COMPLETED,
        totalLines: 2,
        processedLines: 2,
        successLines: 2,
        failedLines: 0,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        lines: [
          {
            id: 'line-1',
            lineNumber: 1,
            username: 'user1',
            email: 'user1@test.com',
            firstName: 'User',
            lastName: 'One',
            phoneNumber: undefined,
            roles: 'USER',
            status: 'SUCCESS',
            errorMessage: undefined,
            createdUserId: 'created-1',
            processedAt: '2024-01-01T10:05:00'
          }
        ],
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:10:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: '2024-01-01T10:10:00'
      };
      const action = BatchImportActions.loadBatchImportDetailSuccess({ batchImport });
      const result = batchImportReducer(initialState, action);

      expect(result.selectedImport).toEqual(batchImport);
      expect(result.loading).toBe(false);
      expect(result.error).toBeNull();
    });
  });

  describe('refreshBatchImportStatusSuccess', () => {
    it('should update batch import in list', () => {
      const originalImport: BatchImport = {
        id: 'batch-1',
        fileName: 'test.csv',
        fileSize: 1024,
        status: BatchImportStatus.PROCESSING,
        totalLines: 100,
        processedLines: 50,
        successLines: 48,
        failedLines: 2,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:05:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: null
      };
      const stateWithImport: BatchImportState = { ...initialState, imports: [originalImport] };

      const updatedImport: BatchImport = {
        ...originalImport,
        processedLines: 100,
        successLines: 95,
        failedLines: 5,
        status: BatchImportStatus.COMPLETED,
        completedAt: '2024-01-01T10:10:00'
      };
      const action = BatchImportActions.refreshBatchImportStatusSuccess({ batchImport: updatedImport });
      const result = batchImportReducer(stateWithImport, action);

      expect(result.imports[0]).toEqual(updatedImport);
      expect(result.imports[0].status).toBe(BatchImportStatus.COMPLETED);
      expect(result.imports[0].processedLines).toBe(100);
    });

    it('should update selected import if same ID', () => {
      const originalImport: BatchImportDetail = {
        id: 'batch-1',
        fileName: 'test.csv',
        fileSize: 1024,
        status: BatchImportStatus.PROCESSING,
        totalLines: 100,
        processedLines: 50,
        successLines: 48,
        failedLines: 2,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        lines: [],
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:05:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: null
      };
      const stateWithSelected: BatchImportState = { ...initialState, selectedImport: originalImport };

      const updatedImport: BatchImport = {
        id: 'batch-1',
        fileName: 'test.csv',
        fileSize: 1024,
        status: BatchImportStatus.COMPLETED,
        totalLines: 100,
        processedLines: 100,
        successLines: 95,
        failedLines: 5,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:10:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: '2024-01-01T10:10:00'
      };
      const action = BatchImportActions.refreshBatchImportStatusSuccess({ batchImport: updatedImport });
      const result = batchImportReducer(stateWithSelected, action);

      expect(result.selectedImport?.status).toBe(BatchImportStatus.COMPLETED);
      expect(result.selectedImport?.processedLines).toBe(100);
    });

    it('should not update selected import if different ID', () => {
      const selectedImport: BatchImportDetail = {
        id: 'batch-1',
        fileName: 'test.csv',
        fileSize: 1024,
        status: BatchImportStatus.PROCESSING,
        totalLines: 100,
        processedLines: 50,
        successLines: 48,
        failedLines: 2,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        lines: [],
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:05:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: null
      };
      const stateWithSelected: BatchImportState = { ...initialState, selectedImport };

      const differentImport: BatchImport = {
        id: 'batch-2',
        fileName: 'other.csv',
        fileSize: 512,
        status: BatchImportStatus.COMPLETED,
        totalLines: 10,
        processedLines: 10,
        successLines: 10,
        failedLines: 0,
        errorMessage: undefined,
        initiatedBy: { id: 'user-2', username: 'user', email: 'user@test.com' },
        createdAt: '2024-01-01T11:00:00',
        updatedAt: '2024-01-01T11:05:00',
        startedAt: '2024-01-01T11:01:00',
        completedAt: '2024-01-01T11:05:00'
      };
      const action = BatchImportActions.refreshBatchImportStatusSuccess({ batchImport: differentImport });
      const result = batchImportReducer(stateWithSelected, action);

      expect(result.selectedImport).toEqual(selectedImport); // Unchanged
    });
  });

  describe('cancelBatchImport', () => {
    it('should set loading', () => {
      const action = BatchImportActions.cancelBatchImport({ id: 'batch-1' });
      const result = batchImportReducer(initialState, action);

      expect(result.loading).toBe(true);
      expect(result.error).toBeNull();
    });
  });

  describe('cancelBatchImportSuccess', () => {
    it('should update cancelled batch import in list', () => {
      const originalImport: BatchImport = {
        id: 'batch-1',
        fileName: 'test.csv',
        fileSize: 1024,
        status: BatchImportStatus.PROCESSING,
        totalLines: 100,
        processedLines: 50,
        successLines: 48,
        failedLines: 2,
        errorMessage: undefined,
        initiatedBy: { id: 'user-1', username: 'admin', email: 'admin@test.com' },
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:05:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: null
      };
      const stateWithImport: BatchImportState = { ...initialState, imports: [originalImport] };

      const cancelledImport: BatchImport = {
        ...originalImport,
        status: BatchImportStatus.CANCELLED,
        completedAt: '2024-01-01T10:06:00'
      };
      const action = BatchImportActions.cancelBatchImportSuccess({ batchImport: cancelledImport });
      const result = batchImportReducer(stateWithImport, action);

      expect(result.imports[0].status).toBe(BatchImportStatus.CANCELLED);
      expect(result.loading).toBe(false);
    });
  });

  describe('cancelBatchImportFailure', () => {
    it('should set error and clear loading', () => {
      const loadingState: BatchImportState = { ...initialState, loading: true };
      const errorMessage = 'Cannot cancel completed import';
      const action = BatchImportActions.cancelBatchImportFailure({ error: errorMessage });
      const result = batchImportReducer(loadingState, action);

      expect(result.loading).toBe(false);
      expect(result.error).toBe(errorMessage);
    });
  });

  describe('clearError', () => {
    it('should clear error message', () => {
      const errorState: BatchImportState = { ...initialState, error: 'Some error' };
      const action = BatchImportActions.clearError();
      const result = batchImportReducer(errorState, action);

      expect(result.error).toBeNull();
    });
  });

  describe('State Immutability', () => {
    it('should not mutate original state', () => {
      const originalState = { ...initialState };
      const action = BatchImportActions.loadBatchImports({ page: 0, size: 20 });
      const result = batchImportReducer(originalState, action);

      expect(result).not.toBe(originalState);
      expect(originalState.loading).toBe(false);
      expect(result.loading).toBe(true);
    });
  });
});
