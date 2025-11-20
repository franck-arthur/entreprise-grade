import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

/**
 * Not Found page component (404).
 */
@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="fr-container">
      <div class="fr-grid-row fr-grid-row--center fr-mt-6w">
        <div class="fr-col-12 fr-col-md-8 fr-col-lg-6 text-center">
          <h1 class="fr-display--xs">404</h1>
          <h2 class="fr-h3">Page non trouvée</h2>
          <p class="fr-text--lead">La page que vous recherchez n'existe pas.</p>
          <div class="fr-mt-4w">
            <a routerLink="/" class="fr-btn"> Retour à l'accueil </a>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class NotFoundComponent {}
