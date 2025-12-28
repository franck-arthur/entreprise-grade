import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

/**
 * Application routes configuration.
 *
 * Uses standalone components with lazy loading for optimal performance.
 */
export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login.component').then(m => m.LoginComponent),
    title: 'Connexion',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard/dashboard.component').then(
        m => m.DashboardComponent
      ),
    canActivate: [authGuard],
    title: 'Tableau de bord',
  },
  {
    path: 'users',
    loadChildren: () => import('./features/users/users.routes').then(m => m.USERS_ROUTES),
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'TECH_LEAD', 'MANAGER'] },
  },
  {
    path: 'batch-import',
    loadChildren: () => import('./features/batch-import/batch-import.routes').then(m => m.BATCH_IMPORT_ROUTES),
    canActivate: [authGuard],
    title: 'Imports par lot',
  },
  {
    path: 'audit',
    loadChildren: () => import('./features/audit/audit.routes').then(m => m.AUDIT_ROUTES),
    canActivate: [authGuard],
    title: 'Audit',
  },
  {
    path: 'formations',
    loadChildren: () => import('./features/formations/formations.routes').then(m => m.FORMATIONS_ROUTES),
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'MANAGER', 'TECH_LEAD'] },
    title: 'Formations',
  },
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./features/auth/pages/unauthorized/unauthorized.component').then(
        m => m.UnauthorizedComponent
      ),
    title: 'Accès refusé',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./shared/components/not-found/not-found.component').then(m => m.NotFoundComponent),
    title: 'Page non trouvée',
  },
];
