import { Routes } from '@angular/router';

/**
 * Users feature routes.
 */
export const USERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/users-list/users-list.component').then(m => m.UsersListComponent),
    title: 'Utilisateurs',
  },
  {
    path: 'create',
    loadComponent: () =>
      import('./pages/user-create/user-create.component').then(m => m.UserCreateComponent),
    title: 'Créer un utilisateur',
    data: { roles: ['ADMIN', 'TECH_LEAD'] },
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/user-detail/user-detail.component').then(m => m.UserDetailComponent),
    title: 'Détail utilisateur',
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/user-edit/user-edit.component').then(m => m.UserEditComponent),
    title: 'Modifier utilisateur',
    data: { roles: ['ADMIN', 'TECH_LEAD'] },
  },
];
