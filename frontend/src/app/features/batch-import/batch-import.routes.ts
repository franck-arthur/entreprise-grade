import { Routes } from '@angular/router';
import { RoleGuard } from '../../core/guards/role.guard';
import { UserRole } from '../../core/models/user.model';

export const BATCH_IMPORT_ROUTES: Routes = [
  {
    path: '',
    canActivate: [RoleGuard],
    data: { roles: [UserRole.ADMIN] },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/batch-import-list/batch-import-list-page.component').then(
            (m) => m.BatchImportListPageComponent
          )
      }
    ]
  }
];
