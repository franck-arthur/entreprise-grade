import { Routes } from '@angular/router';

/**
 * Formations feature routes.
 */
export const FORMATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/formations-list/formations-list.component').then(
        m => m.FormationsListComponent
      ),
    title: 'Formations',
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/formation-detail/formation-detail.component').then(
        m => m.FormationDetailComponent
      ),
    title: 'Détail Formation',
  },
];