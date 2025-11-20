import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { User } from '@app/core/models/user.model';
import * as UsersActions from '../../state/users.actions';
import * as UsersSelectors from '../../state/users.selectors';

/**
 * Users list component.
 *
 * Displays a paginated list of users using DSFR table component.
 */
@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="fr-container">
      <div class="fr-mt-6w">
        <div class="fr-grid-row fr-grid-row--gutters">
          <div class="fr-col">
            <h1 class="fr-h2">Gestion des utilisateurs</h1>
          </div>
          <div class="fr-col-auto">
            <a routerLink="/users/create" class="fr-btn"> Créer un utilisateur </a>
          </div>
        </div>

        <div class="fr-mt-4w" *ngIf="loading$ | async">
          <div class="spinner-container">
            <p>Chargement...</p>
          </div>
        </div>

        <div class="fr-mt-4w" *ngIf="error$ | async as error">
          <div class="fr-alert fr-alert--error">
            <p>{{ error }}</p>
          </div>
        </div>

        <div class="fr-mt-4w" *ngIf="(users$ | async)?.length">
          <div class="fr-table">
            <table>
              <thead>
                <tr>
                  <th scope="col">Nom d'utilisateur</th>
                  <th scope="col">Email</th>
                  <th scope="col">Prénom Nom</th>
                  <th scope="col">Rôles</th>
                  <th scope="col">Statut</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let user of users$ | async">
                  <td>{{ user.username }}</td>
                  <td>{{ user.email }}</td>
                  <td>{{ user.firstName }} {{ user.lastName }}</td>
                  <td>
                    <span
                      *ngFor="let role of user.roles"
                      class="fr-badge fr-badge--sm fr-badge--blue-ecume"
                    >
                      {{ role }}
                    </span>
                  </td>
                  <td>
                    <span
                      class="fr-badge fr-badge--sm"
                      [class.fr-badge--success]="user.active"
                      [class.fr-badge--error]="!user.active"
                    >
                      {{ user.active ? 'Actif' : 'Inactif' }}
                    </span>
                  </td>
                  <td>
                    <a [routerLink]="['/users', user.id]" class="fr-btn fr-btn--sm"> Voir </a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="fr-mt-4w" *ngIf="pagination$ | async as pagination">
            <nav role="navigation" class="fr-pagination" aria-label="Pagination">
              <ul class="fr-pagination__list">
                <li *ngIf="pagination.currentPage > 0">
                  <a
                    class="fr-pagination__link fr-pagination__link--prev"
                    (click)="onPageChange(pagination.currentPage - 1)"
                    href="javascript:void(0)"
                  >
                    Page précédente
                  </a>
                </li>
                <li>
                  <span class="fr-pagination__link fr-pagination__link--current">
                    Page {{ pagination.currentPage + 1 }} / {{ pagination.totalPages }}
                  </span>
                </li>
                <li *ngIf="pagination.currentPage < pagination.totalPages - 1">
                  <a
                    class="fr-pagination__link fr-pagination__link--next"
                    (click)="onPageChange(pagination.currentPage + 1)"
                    href="javascript:void(0)"
                  >
                    Page suivante
                  </a>
                </li>
              </ul>
            </nav>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class UsersListComponent implements OnInit {
  users$: Observable<User[]> = this.store.select(UsersSelectors.selectAllUsers);
  loading$: Observable<boolean> = this.store.select(UsersSelectors.selectUsersLoading);
  error$: Observable<string | null> = this.store.select(UsersSelectors.selectUsersError);
  pagination$ = this.store.select(UsersSelectors.selectUsersPagination);

  constructor(private store: Store) {}

  ngOnInit() {
    this.loadUsers(0);
  }

  loadUsers(page: number) {
    this.store.dispatch(UsersActions.loadUsers({ page, size: 20 }));
  }

  onPageChange(page: number) {
    this.loadUsers(page);
  }
}
