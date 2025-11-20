import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, interval } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import {
  loadBatchImports,
  refreshBatchImportStatus,
  cancelBatchImport
} from '../../../../store/batch-import/batch-import.actions';
import {
  selectAllBatchImports,
  selectBatchImportLoading,
  selectBatchImportPagination
} from '../../../../store/batch-import/batch-import.selectors';
import { BatchImport, BatchImportStatus } from '../../../../core/models/batch-import.model';

/**
 * Component for displaying list of batch imports with status and progress.
 * Includes automatic status polling for in-progress imports.
 */
@Component({
  selector: 'app-batch-import-list',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './batch-import-list.component.html',
  styleUrls: ['./batch-import-list.component.scss']
})
export class BatchImportListComponent implements OnInit, OnDestroy {
  imports$ = this.store.select(selectAllBatchImports);
  loading$ = this.store.select(selectBatchImportLoading);
  pagination$ = this.store.select(selectBatchImportPagination);

  private destroy$ = new Subject<void>();
  currentPage = 0;
  pageSize = 20;

  BatchImportStatus = BatchImportStatus; // Expose enum to template

  constructor(
    private store: Store,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Load initial batch imports
    this.loadBatchImports();

    // Poll for updates every 3 seconds for in-progress imports
    interval(3000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshInProgressImports();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load batch imports from server.
   */
  loadBatchImports(): void {
    this.store.dispatch(
      loadBatchImports({ page: this.currentPage, size: this.pageSize })
    );
  }

  /**
   * Refresh status of in-progress imports.
   */
  private refreshInProgressImports(): void {
    this.imports$
      .pipe(
        takeUntil(this.destroy$),
        filter((imports) => imports.length > 0)
      )
      .subscribe((imports) => {
        imports
          .filter((imp) => imp.status === BatchImportStatus.PROCESSING)
          .forEach((imp) => {
            this.store.dispatch(refreshBatchImportStatus({ id: imp.id }));
          });
      });
  }

  /**
   * Navigate to batch import details.
   */
  viewDetails(importId: string): void {
    this.router.navigate(['/batch-imports', importId]);
  }

  /**
   * Cancel a batch import.
   */
  cancelImport(importId: string, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure you want to cancel this import?')) {
      this.store.dispatch(cancelBatchImport({ id: importId }));
    }
  }

  /**
   * Handle page change.
   */
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadBatchImports();
  }

  /**
   * Get status badge class.
   */
  getStatusBadgeClass(status: BatchImportStatus): string {
    switch (status) {
      case BatchImportStatus.PENDING:
        return 'fr-badge--info';
      case BatchImportStatus.PROCESSING:
        return 'fr-badge--warning';
      case BatchImportStatus.COMPLETED:
        return 'fr-badge--success';
      case BatchImportStatus.COMPLETED_WITH_ERRORS:
        return 'fr-badge--warning';
      case BatchImportStatus.FAILED:
        return 'fr-badge--error';
      case BatchImportStatus.CANCELLED:
        return 'fr-badge--new';
      default:
        return '';
    }
  }

  /**
   * Get progress bar class.
   */
  getProgressBarClass(batchImport: BatchImport): string {
    if (batchImport.status === BatchImportStatus.FAILED) {
      return 'progress-bar--error';
    }
    if (batchImport.status === BatchImportStatus.COMPLETED_WITH_ERRORS) {
      return 'progress-bar--warning';
    }
    if (batchImport.status === BatchImportStatus.COMPLETED) {
      return 'progress-bar--success';
    }
    return 'progress-bar--info';
  }

  /**
   * Check if import can be cancelled.
   */
  canBeCancelled(status: BatchImportStatus): boolean {
    return status === BatchImportStatus.PROCESSING || status === BatchImportStatus.PENDING;
  }

  /**
   * Format date.
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  /**
   * Get file size in human-readable format.
   */
  getFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}
