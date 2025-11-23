import { Routes } from '@angular/router';
import { RoleGuard } from '../../core/guards/role.guard';
import { UserRole } from '../../core/models/user.model';

export const AUDIT_ROUTES: Routes = [
  {
    path: '',
    canActivate: [RoleGuard],
    data: { roles: [UserRole.ADMIN] },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/audit-dashboard/audit-dashboard-page.component').then(
            (m) => m.AuditDashboardPageComponent
          )
      }
    ]
  }
];
