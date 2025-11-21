import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@app/core/services/auth.service';

/**
 * Login component.
 *
 * Redirects to Keycloak for authentication using DSFR design system.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <div class="fr-container">
      <div class="fr-grid-row fr-grid-row--center">
        <div class="fr-col-12 fr-col-md-6 fr-col-lg-4">
          <div class="fr-mt-6w fr-text--center">
            <h1 class="fr-h3">{{ 'auth.login' | translate }}</h1>

            <p class="fr-text--lg fr-mt-4w">
              {{ 'auth.redirect_message' | translate }}
            </p>

            <div class="fr-mt-4w">
              <button class="fr-btn" type="button" (click)="login()">
                {{ 'auth.sign_in' | translate }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [],
})
export class LoginComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  async ngOnInit(): Promise<void> {
    // If already authenticated, redirect to dashboard
    if (await this.authService.isAuthenticatedAsync()) {
      this.router.navigate(['/dashboard']);
    }
  }

  login(): void {
    this.authService.login();
  }
}
