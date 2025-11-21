import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuditDashboardComponent } from '../../components/audit-dashboard/audit-dashboard.component';
import { AuditLogsComponent } from '../../components/audit-logs/audit-logs.component';

/**
 * Page component for audit management.
 * Combines dashboard statistics and detailed audit logs.
 */
@Component({
  selector: 'app-audit-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AuditDashboardComponent,
    AuditLogsComponent
  ],
  template: `
    <div class="fr-container fr-my-4w">
      <div class="fr-grid-row">
        <div class="fr-col-12">
          <h1>Audit et journaux d'événements</h1>
          <p class="fr-text--lg">
            Consultez les statistiques d'audit et les événements système.
          </p>
        </div>
      </div>

      <!-- Dashboard Statistics Section -->
      <div class="fr-grid-row fr-mt-4w">
        <div class="fr-col-12">
          <div class="fr-card">
            <div class="fr-card__body">
              <div class="fr-card__content">
                <h2 class="fr-card__title">Statistiques</h2>
                <app-audit-dashboard></app-audit-dashboard>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Audit Logs Section -->
      <div class="fr-grid-row fr-mt-4w">
        <div class="fr-col-12">
          <div class="fr-card">
            <div class="fr-card__body">
              <div class="fr-card__content">
                <h2 class="fr-card__title">Journaux d'événements</h2>
                <app-audit-logs></app-audit-logs>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class AuditDashboardPageComponent {}
