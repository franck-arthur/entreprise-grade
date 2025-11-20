import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { TranslateModule } from '@ngx-translate/core';
import { loadAuditStatistics } from '../../../../store/audit/audit.actions';
import {
  selectAuditStatistics,
  selectAuditLoadingStatistics
} from '../../../../store/audit/audit.selectors';
import { AuditStatistics } from '../../../../core/models/audit.model';

/**
 * Component for displaying audit statistics dashboard.
 * Demonstrates CQRS query side with aggregations.
 */
@Component({
  selector: 'app-audit-dashboard',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './audit-dashboard.component.html',
  styleUrls: ['./audit-dashboard.component.scss']
})
export class AuditDashboardComponent implements OnInit {
  statistics$ = this.store.select(selectAuditStatistics);
  loading$ = this.store.select(selectAuditLoadingStatistics);

  constructor(private store: Store) {}

  ngOnInit(): void {
    this.loadStatistics();
  }

  /**
   * Load audit statistics.
   */
  loadStatistics(): void {
    this.store.dispatch(loadAuditStatistics());
  }

  /**
   * Calculate success rate percentage.
   */
  calculateSuccessRate(stats: AuditStatistics): number {
    if (stats.totalEvents === 0) {
      return 0;
    }
    return (stats.successfulEvents / stats.totalEvents) * 100;
  }

  /**
   * Get top N entries from a record.
   */
  getTopEntries(record: Record<string, number> | undefined, limit: number = 5): Array<{key: string, value: number}> {
    if (!record) {
      return [];
    }

    return Object.entries(record)
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => b.value - a.value)
      .slice(0, limit);
  }

  /**
   * Calculate percentage for a value.
   */
  calculatePercentage(value: number, total: number): number {
    if (total === 0) {
      return 0;
    }
    return (value / total) * 100;
  }

  /**
   * Get color class for event category.
   */
  getCategoryColorClass(category: string): string {
    switch (category) {
      case 'USER':
        return 'stat-bar--blue';
      case 'AUTH':
        return 'stat-bar--green';
      case 'BATCH':
        return 'stat-bar--purple';
      case 'SECURITY':
        return 'stat-bar--red';
      case 'SYSTEM':
        return 'stat-bar--grey';
      default:
        return 'stat-bar--default';
    }
  }
}
