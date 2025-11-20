import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@app/core/services/auth.service';

/**
 * Login component.
 *
 * Simple login page using DSFR design system with internationalization.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  template: `
    <div class="fr-container">
      <div class="fr-grid-row fr-grid-row--center">
        <div class="fr-col-12 fr-col-md-6 fr-col-lg-4">
          <div class="fr-mt-6w">
            <h1 class="fr-h3">{{ 'auth.login' | translate }}</h1>

            <form (ngSubmit)="onSubmit()" class="fr-mt-4w">
              <div class="fr-input-group">
                <label class="fr-label" for="username">
                  {{ 'auth.username' | translate }}
                </label>
                <input
                  class="fr-input"
                  type="text"
                  id="username"
                  name="username"
                  [(ngModel)]="username"
                  required
                  autocomplete="username"
                />
              </div>

              <div class="fr-input-group fr-mt-2w">
                <label class="fr-label" for="password">
                  {{ 'auth.password' | translate }}
                </label>
                <input
                  class="fr-input"
                  type="password"
                  id="password"
                  name="password"
                  [(ngModel)]="password"
                  required
                  autocomplete="current-password"
                />
              </div>

              <div class="fr-mt-4w">
                <button class="fr-btn" type="submit" [disabled]="loading">
                  {{ (loading ? 'auth.signing_in' : 'auth.sign_in') | translate }}
                </button>
              </div>

              <div class="fr-mt-2w" *ngIf="error">
                <div class="fr-alert fr-alert--error">
                  <p>{{ error }}</p>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [],
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  error: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit() {
    this.loading = true;
    this.error = null;

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading = false;
        // Error message comes from backend (already internationalized)
        this.error = err.message || 'auth.invalid_credentials';
        console.error('Login error:', err);
      },
    });
  }
}
