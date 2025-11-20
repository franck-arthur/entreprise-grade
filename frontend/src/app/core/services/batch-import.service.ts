import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BatchImport,
  BatchImportDetail,
  BatchImportPage
} from '../models/batch-import.model';

/**
 * Service for batch import operations.
 * Handles file uploads and progress tracking with multithreading support.
 */
@Injectable({
  providedIn: 'root'
})
export class BatchImportService {
  private readonly apiUrl = `${environment.apiUrl}/batch-imports`;

  constructor(private http: HttpClient) {}

  /**
   * Upload a CSV file for batch import.
   */
  uploadCsvFile(file: File): Observable<BatchImport> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<BatchImport>(`${this.apiUrl}/upload`, formData);
  }

  /**
   * Get batch import by ID with full details.
   */
  getBatchImportById(id: string): Observable<BatchImportDetail> {
    return this.http.get<BatchImportDetail>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get batch import status (lightweight for polling).
   */
  getBatchImportStatus(id: string): Observable<BatchImport> {
    return this.http.get<BatchImport>(`${this.apiUrl}/${id}/status`);
  }

  /**
   * Get all batch imports with pagination.
   */
  getAllBatchImports(page: number = 0, size: number = 20): Observable<BatchImportPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');

    return this.http.get<BatchImportPage>(this.apiUrl, { params });
  }

  /**
   * Get batch imports for current user.
   */
  getMyBatchImports(page: number = 0, size: number = 20): Observable<BatchImportPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');

    return this.http.get<BatchImportPage>(`${this.apiUrl}/my-imports`, { params });
  }

  /**
   * Cancel a batch import.
   */
  cancelBatchImport(id: string): Observable<BatchImport> {
    return this.http.post<BatchImport>(`${this.apiUrl}/${id}/cancel`, {});
  }
}
