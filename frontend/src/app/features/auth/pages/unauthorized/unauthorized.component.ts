import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

/**
 * Unauthorized page component.
 */
@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="fr-container">
      <div class="fr-grid-row fr-grid-row--center fr-mt-6w">
        <div class="fr-col-12 fr-col-md-8 fr-col-lg-6 text-center">
          <h1 class="fr-h2">Accès refusé</h1>
          <p class="fr-text--lead">
            Vous n'avez pas les permissions nécessaires pour accéder à cette page.
          </p>
          <div class="fr-mt-4w">
            <a routerLink="/dashboard" class="fr-btn"> Retour au tableau de bord </a>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class UnauthorizedComponent {}
