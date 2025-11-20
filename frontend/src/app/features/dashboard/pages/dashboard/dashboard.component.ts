import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '@app/core/services/auth.service';

/**
 * Dashboard component.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="fr-container">
      <div class="fr-mt-6w">
        <h1 class="fr-h2">Tableau de bord</h1>

        <div class="fr-mt-4w" *ngIf="currentUser$ | async as user">
          <p>Bienvenue, {{ user.username }} !</p>
        </div>

        <div class="fr-grid-row fr-grid-row--gutters fr-mt-4w">
          <div class="fr-col-12 fr-col-md-6 fr-col-lg-4">
            <div class="fr-card">
              <div class="fr-card__body">
                <div class="fr-card__content">
                  <h3 class="fr-card__title">
                    <a routerLink="/users" class="fr-card__link">Gestion des utilisateurs</a>
                  </h3>
                  <p class="fr-card__desc">
                    Créer, modifier et gérer les utilisateurs de l'application.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div class="fr-col-12 fr-col-md-6 fr-col-lg-4">
            <div class="fr-card">
              <div class="fr-card__body">
                <div class="fr-card__content">
                  <h3 class="fr-card__title">
                    <span class="fr-card__link">Statistiques</span>
                  </h3>
                  <p class="fr-card__desc">
                    Visualiser les statistiques et métriques de l'application.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div class="fr-col-12 fr-col-md-6 fr-col-lg-4">
            <div class="fr-card">
              <div class="fr-card__body">
                <div class="fr-card__content">
                  <h3 class="fr-card__title">
                    <span class="fr-card__link">Paramètres</span>
                  </h3>
                  <p class="fr-card__desc">Configurer les paramètres de l'application.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class DashboardComponent {
  currentUser$ = this.authService.currentUser$;

  constructor(private authService: AuthService) {}
}
