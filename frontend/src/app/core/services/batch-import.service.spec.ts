import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BatchImportService } from './batch-import.service';
import {
  BatchImport,
  BatchImportDetail,
  BatchImportPage,
  BatchImportStatus
} from '../models/batch-import.model';
import { environment } from '../../../environments/environment';

describe('BatchImportService', () => {
  let service: BatchImportService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/batch-imports`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BatchImportService]
    });
    service = TestBed.inject(BatchImportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('uploadCsvFile', () => {
    it('should upload a CSV file and return batch import', () => {
      const mockFile = new File(['username,email\ntest,test@test.com'], 'users.csv', {
        type: 'text/csv'
      });
      const mockResponse: BatchImport = {
        id: 'batch-123',
        fileName: 'users.csv',
        fileSize: 1024,
        status: BatchImportStatus.PENDING,
        totalLines: 1,
        processedLines: 0,
        successLines: 0,
        failedLines: 0,
        progressPercentage: 0,
        errorMessage: undefined,
        initiatedByUserId: 'user-1',
        initiatedByUsername: 'admin',
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:00:00',
        startedAt: undefined,
        completedAt: undefined
      };

      service.uploadCsvFile(mockFile).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.id).toBe('batch-123');
        expect(response.status).toBe(BatchImportStatus.PENDING);
        expect(response.fileName).toBe('users.csv');
      });

      const req = httpMock.expectOne(`${apiUrl}/upload`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBeTruthy();
      req.flush(mockResponse);
    });

    it('should handle upload errors', () => {
      const mockFile = new File(['invalid'], 'invalid.txt', { type: 'text/plain' });

      service.uploadCsvFile(mockFile).subscribe(
        () => fail('should have failed'),
        error => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne(`${apiUrl}/upload`);
      req.flush('Invalid file format', { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('getBatchImportById', () => {
    it('should retrieve batch import details by ID', () => {
      const batchId = 'batch-456';
      const mockDetail: BatchImportDetail = {
        id: batchId,
        fileName: 'users.csv',
        fileSize: 2048,
        status: BatchImportStatus.COMPLETED,
        totalLines: 10,
        processedLines: 10,
        successLines: 8,
        failedLines: 2,
        progressPercentage: 50,
        errorMessage: undefined,
        initiatedByUserId: 'user-1',
        initiatedByUsername: 'admin',
        lines: [
          {
            id: 'line-1',
            lineNumber: 1,
            rawData: 'user1,user1@test.com,User,One',
            success: true,
            createdUsername: 'user1',
            errorMessage: undefined,
            createdUserId: 'user-created-1',
          }
        ],
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:10:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: '2024-01-01T10:10:00'
      };

      service.getBatchImportById(batchId).subscribe(response => {
        expect(response).toEqual(mockDetail);
        expect(response.id).toBe(batchId);
        expect(response.lines.length).toBe(1);
        expect(response.status).toBe(BatchImportStatus.COMPLETED);
      });

      const req = httpMock.expectOne(`${apiUrl}/${batchId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDetail);
    });

    it('should handle not found errors', () => {
      const batchId = 'non-existent';

      service.getBatchImportById(batchId).subscribe(
        () => fail('should have failed'),
        error => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne(`${apiUrl}/${batchId}`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getBatchImportStatus', () => {
    it('should retrieve lightweight status for polling', () => {
      const batchId = 'batch-789';
      const mockStatus: BatchImport = {
        id: batchId,
        fileName: 'users.csv',
        fileSize: 1024,
        status: BatchImportStatus.PROCESSING,
        totalLines: 100,
        processedLines: 50,
        successLines: 48,
        failedLines: 2,
        progressPercentage: 50,
        errorMessage: undefined,
        initiatedByUserId: 'user-1',
        initiatedByUsername: 'admin',
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:05:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: undefined
      };

      service.getBatchImportStatus(batchId).subscribe(response => {
        expect(response.status).toBe(BatchImportStatus.PROCESSING);
        expect(response.processedLines).toBe(50);
        expect(response.totalLines).toBe(100);
      });

      const req = httpMock.expectOne(`${apiUrl}/${batchId}/status`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStatus);
    });
  });

  describe('getAllBatchImports', () => {
    it('should retrieve all batch imports with pagination', () => {
      const mockPage: BatchImportPage = {
        content: [
          {
            id: 'batch-1',
            fileName: 'file1.csv',
            fileSize: 1024,
            status: BatchImportStatus.COMPLETED,
            totalLines: 10,
            processedLines: 10,
            successLines: 10,
            failedLines: 0,
        progressPercentage: 0,
            errorMessage: undefined,
            initiatedByUserId: 'user-1', initiatedByUsername: 'admin',
            createdAt: '2024-01-01T10:00:00',
            updatedAt: '2024-01-01T10:10:00',
            startedAt: '2024-01-01T10:01:00',
            completedAt: '2024-01-01T10:10:00'
          }
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0
      };

      service.getAllBatchImports(0, 20).subscribe(response => {
        expect(response).toEqual(mockPage);
        expect(response.content.length).toBe(1);
        expect(response.totalElements).toBe(1);
      });

      const req = httpMock.expectOne(request =>
        request.url === apiUrl &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '20' &&
        request.params.get('sort') === 'createdAt,desc'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });

    it('should use default pagination values', () => {
      const mockPage: BatchImportPage = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0
      };

      service.getAllBatchImports().subscribe(response => {
        expect(response.content.length).toBe(0);
      });

      const req = httpMock.expectOne(request =>
        request.url === apiUrl &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '20'
      );
      req.flush(mockPage);
    });
  });

  describe('getMyBatchImports', () => {
    it('should retrieve current user batch imports', () => {
      const mockPage: BatchImportPage = {
        content: [
          {
            id: 'batch-my-1',
            fileName: 'my-file.csv',
            fileSize: 512,
            status: BatchImportStatus.PROCESSING,
            totalLines: 5,
            processedLines: 3,
            successLines: 3,
            failedLines: 0,
        progressPercentage: 0,
            errorMessage: undefined,
            initiatedByUserId: 'user-1', initiatedByUsername: 'me',
            createdAt: '2024-01-01T11:00:00',
            updatedAt: '2024-01-01T11:05:00',
            startedAt: '2024-01-01T11:01:00',
            completedAt: undefined
          }
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0
      };

      service.getMyBatchImports(0, 20).subscribe(response => {
        expect(response.content.length).toBe(1);
        expect(response.content[0].initiatedByUsername).toBe('me');
      });

      const req = httpMock.expectOne(request =>
        request.url === `${apiUrl}/my-imports`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });
  });

  describe('cancelBatchImport', () => {
    it('should cancel a batch import', () => {
      const batchId = 'batch-to-cancel';
      const mockResponse: BatchImport = {
        id: batchId,
        fileName: 'cancelled.csv',
        fileSize: 1024,
        status: BatchImportStatus.CANCELLED,
        totalLines: 100,
        processedLines: 20,
        successLines: 18,
        failedLines: 2,
        progressPercentage: 50,
        errorMessage: undefined,
        initiatedByUserId: 'user-1', initiatedByUsername: 'admin',
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:05:00',
        startedAt: '2024-01-01T10:01:00',
        completedAt: '2024-01-01T10:05:00'
      };

      service.cancelBatchImport(batchId).subscribe(response => {
        expect(response.status).toBe(BatchImportStatus.CANCELLED);
        expect(response.id).toBe(batchId);
      });

      const req = httpMock.expectOne(`${apiUrl}/${batchId}/cancel`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockResponse);
    });

    it('should handle cancel errors for completed imports', () => {
      const batchId = 'completed-batch';

      service.cancelBatchImport(batchId).subscribe(
        () => fail('should have failed'),
        error => {
          expect(error.status).toBe(400);
        }
      );

      const req = httpMock.expectOne(`${apiUrl}/${batchId}/cancel`);
      req.flush('Cannot cancel completed import', { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('Integration Scenarios', () => {
    it('should handle full upload to completion flow', (done) => {
      const mockFile = new File(['data'], 'test.csv', { type: 'text/csv' });
      const batchId = 'batch-flow';

      // Step 1: Upload
      const uploadResponse: BatchImport = {
        id: batchId,
        fileName: 'test.csv',
        fileSize: 100,
        status: BatchImportStatus.PENDING,
        totalLines: 10,
        processedLines: 0,
        successLines: 0,
        failedLines: 0,
        progressPercentage: 0,
        errorMessage: undefined,
        initiatedByUserId: 'user-1', initiatedByUsername: 'admin',
        createdAt: '2024-01-01T10:00:00',
        updatedAt: '2024-01-01T10:00:00',
        startedAt: undefined,
        completedAt: undefined
      };

      service.uploadCsvFile(mockFile).subscribe(upload => {
        expect(upload.status).toBe(BatchImportStatus.PENDING);

        // Step 2: Poll status
        const statusResponse = { ...uploadResponse, status: BatchImportStatus.PROCESSING, processedLines: 5 };
        service.getBatchImportStatus(batchId).subscribe(status => {
          expect(status.status).toBe(BatchImportStatus.PROCESSING);
          expect(status.processedLines).toBe(5);
          done();
        });

        const statusReq = httpMock.expectOne(`${apiUrl}/${batchId}/status`);
        statusReq.flush(statusResponse);
      });

      const uploadReq = httpMock.expectOne(`${apiUrl}/upload`);
      uploadReq.flush(uploadResponse);
    });
  });
});
