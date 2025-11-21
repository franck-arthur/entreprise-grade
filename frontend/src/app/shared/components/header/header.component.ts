import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../language-selector/language-selector.component';
import { AuthService } from '@app/core/services/auth.service';
import { UserRole } from '@app/core/models/user.model';

/**
 * Header component with navigation and language selector.
 *
 * Uses DSFR header component for consistent government design.
 */
@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule, LanguageSelectorComponent],
  template: `
    <header role="banner" class="fr-header">
      <div class="fr-header__body">
        <div class="fr-container">
          <div class="fr-header__body-row">
            <div class="fr-header__brand fr-enlarge-link">
              <div class="fr-header__brand-top">
                <div class="fr-header__logo">
                  <p class="fr-logo">
                    République
                    <br />Française
                  </p>
                </div>
                <div class="fr-header__navbar">
                  <button
                    class="fr-btn--menu fr-btn"
                    data-fr-opened="false"
                    aria-controls="modal-header-navigation"
                    aria-haspopup="menu"
                    id="button-header-navigation"
                    title="Menu"
                  >
                    Menu
                  </button>
                </div>
              </div>
              <div class="fr-header__service">
                <a routerLink="/" title="Accueil - Application Enterprise">
                  <p class="fr-header__service-title">Application Enterprise</p>
                </a>
              </div>
            </div>
            <div class="fr-header__tools">
              <div class="fr-header__tools-links">
                <ul class="fr-btns-group">
                  <li>
                    <app-language-selector></app-language-selector>
                  </li>
                  <li *ngIf="isAuthenticated() && getCurrentUser()">
                    <span class="fr-text--sm fr-mr-2w">{{ getCurrentUser()?.username }}</span>
                  </li>
                  <li *ngIf="isAuthenticated()">
                    <button class="fr-btn fr-icon-logout-box-r-line" (click)="logout()">
                      {{ 'nav.logout' | translate }}
                    </button>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div
        class="fr-header__menu fr-modal"
        id="modal-header-navigation"
        aria-labelledby="button-header-navigation"
        *ngIf="isAuthenticated()"
      >
        <div class="fr-container">
          <button
            class="fr-btn--close fr-btn"
            aria-controls="modal-header-navigation"
            title="Fermer"
          >
            Fermer
          </button>
          <div class="fr-header__menu-links"></div>
          <nav
            class="fr-nav"
            id="navigation-header"
            role="navigation"
            aria-label="Menu principal"
          >
            <ul class="fr-nav__list">
              <li class="fr-nav__item">
                <a class="fr-nav__link" routerLink="/dashboard" routerLinkActive="fr-nav__link--active">
                  {{ 'nav.dashboard' | translate }}
                </a>
              </li>
              <li class="fr-nav__item">
                <a class="fr-nav__link" routerLink="/users" routerLinkActive="fr-nav__link--active">
                  {{ 'nav.users' | translate }}
                </a>
              </li>
              <li class="fr-nav__item" *ngIf="isAdmin()">
                <a class="fr-nav__link" routerLink="/batch-import" routerLinkActive="fr-nav__link--active">
                  {{ 'nav.batch_import' | translate }}
                </a>
              </li>
              <li class="fr-nav__item" *ngIf="isAdmin()">
                <a class="fr-nav__link" routerLink="/audit" routerLinkActive="fr-nav__link--active">
                  {{ 'nav.audit' | translate }}
                </a>
              </li>
              <li class="fr-nav__item">
                <a class="fr-nav__link" routerLink="/settings" routerLinkActive="fr-nav__link--active">
                  {{ 'nav.settings' | translate }}
                </a>
              </li>
            </ul>
          </nav>
        </div>
      </div>
    </header>
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
})
export class HeaderComponent {
  constructor(private authService: AuthService) {}

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  getCurrentUser() {
    return this.authService.getCurrentUser();
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.roles?.includes(UserRole.ADMIN) || false;
  }

  logout(): void {
    this.authService.logout();
  }
}
