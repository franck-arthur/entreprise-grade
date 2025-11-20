import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateModule } from '@ngx-translate/core';
import {
  loadAuditEvents,
  setAuditFilter,
  clearAuditFilter
} from '../../../../store/audit/audit.actions';
import {
  selectAuditEvents,
  selectAuditLoading,
  selectAuditPagination,
  selectAuditCurrentQuery
} from '../../../../store/audit/audit.selectors';
import {
  AuditEvent,
  AuditEventCategory,
  AuditEventType,
  AuditEventQuery
} from '../../../../core/models/audit.model';

/**
 * Component for displaying audit logs with advanced filtering.
 * Demonstrates CQRS query side with complex filters.
 */
@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.scss']
})
export class AuditLogsComponent implements OnInit {
  events$ = this.store.select(selectAuditEvents);
  loading$ = this.store.select(selectAuditLoading);
  pagination$ = this.store.select(selectAuditPagination);
  currentQuery$ = this.store.select(selectAuditCurrentQuery);

  // Filter form
  filterForm = {
    eventTypes: [] as AuditEventType[],
    eventCategory: '' as AuditEventCategory | '',
    username: '',
    targetEntityType: '',
    success: null as boolean | null,
    fromDate: '',
    toDate: ''
  };

  // Available options for filters
  eventTypeOptions = Object.values(AuditEventType);
  eventCategoryOptions: (AuditEventCategory | '')[] = ['', 'USER', 'AUTH', 'BATCH', 'SECURITY', 'SYSTEM'];

  currentPage = 0;
  pageSize = 50;

  // Expose enum to template
  AuditEventType = AuditEventType;

  constructor(
    private store: Store,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  /**
   * Load audit events with current filters.
   */
  loadEvents(): void {
    const query: AuditEventQuery = {
      eventTypes: this.filterForm.eventTypes.length > 0 ? this.filterForm.eventTypes : undefined,
      eventCategory: this.filterForm.eventCategory || undefined,
      username: this.filterForm.username || undefined,
      targetEntityType: this.filterForm.targetEntityType || undefined,
      success: this.filterForm.success !== null ? this.filterForm.success : undefined,
      fromDate: this.filterForm.fromDate || undefined,
      toDate: this.filterForm.toDate || undefined
    };

    this.store.dispatch(setAuditFilter({ query }));
    this.store.dispatch(
      loadAuditEvents({ query, page: this.currentPage, size: this.pageSize })
    );
  }

  /**
   * Apply filters.
   */
  applyFilters(): void {
    this.currentPage = 0;
    this.loadEvents();
  }

  /**
   * Clear all filters.
   */
  clearFilters(): void {
    this.filterForm = {
      eventTypes: [],
      eventCategory: '',
      username: '',
      targetEntityType: '',
      success: null,
      fromDate: '',
      toDate: ''
    };
    this.currentPage = 0;
    this.store.dispatch(clearAuditFilter());
    this.loadEvents();
  }

  /**
   * Toggle event type in filter.
   */
  toggleEventType(eventType: AuditEventType): void {
    const index = this.filterForm.eventTypes.indexOf(eventType);
    if (index > -1) {
      this.filterForm.eventTypes.splice(index, 1);
    } else {
      this.filterForm.eventTypes.push(eventType);
    }
  }

  /**
   * Check if event type is selected.
   */
  isEventTypeSelected(eventType: AuditEventType): boolean {
    return this.filterForm.eventTypes.includes(eventType);
  }

  /**
   * Handle page change.
   */
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadEvents();
  }

  /**
   * Get badge class for event type.
   */
  getEventTypeBadgeClass(eventType: AuditEventType): string {
    const typeStr = eventType.toString();
    if (typeStr.includes('CREATED') || typeStr.includes('SUCCESS')) {
      return 'fr-badge--success';
    } else if (typeStr.includes('UPDATED') || typeStr.includes('ACTIVATED')) {
      return 'fr-badge--info';
    } else if (typeStr.includes('DELETED') || typeStr.includes('FAILED') || typeStr.includes('DEACTIVATED')) {
      return 'fr-badge--error';
    } else if (typeStr.includes('UNAUTHORIZED') || typeStr.includes('FORBIDDEN')) {
      return 'fr-badge--warning';
    }
    return 'fr-badge';
  }

  /**
   * Get badge class for success status.
   */
  getSuccessBadgeClass(success: boolean): string {
    return success ? 'fr-badge--success' : 'fr-badge--error';
  }

  /**
   * Format date for display.
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  /**
   * Navigate to entity details.
   */
  viewEntityDetails(event: AuditEvent): void {
    if (event.targetEntityType && event.targetEntityId) {
      const entityType = event.targetEntityType.toLowerCase();
      this.router.navigate([`/${entityType}s`, event.targetEntityId]);
    }
  }

  /**
   * Navigate to user details.
   */
  viewUserDetails(userId: string): void {
    this.router.navigate(['/users', userId]);
  }
}
