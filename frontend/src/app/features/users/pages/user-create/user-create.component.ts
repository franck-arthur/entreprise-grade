import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { UserService } from '@app/core/services/user.service';
import { UserRole, CreateUserRequest } from '@app/core/models/user.model';

/**
 * Component for creating a new user.
 * Includes form validation and role selection.
 */
@Component({
  selector: 'app-user-create',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, TranslateModule],
  template: `
    <div class="fr-container fr-my-4w">
      <div class="fr-grid-row">
        <div class="fr-col-12 fr-col-md-8 fr-col-lg-6">
          <h1>{{ 'users.create_user' | translate }}</h1>

          <form [formGroup]="userForm" (ngSubmit)="onSubmit()" class="fr-mt-4w">
            <!-- Username -->
            <div class="fr-input-group" [class.fr-input-group--error]="isFieldInvalid('username')">
              <label class="fr-label" for="username">
                {{ 'users.username' | translate }}
                <span class="fr-hint-text">{{ 'common.required' | translate }}</span>
              </label>
              <input
                class="fr-input"
                [class.fr-input--error]="isFieldInvalid('username')"
                type="text"
                id="username"
                formControlName="username"
                placeholder="john.doe"
              />
              <p class="fr-error-text" *ngIf="isFieldInvalid('username')">
                <span *ngIf="userForm.get('username')?.errors?.['required']">
                  {{ 'errors.required_field' | translate }}
                </span>
                <span *ngIf="userForm.get('username')?.errors?.['minlength']">
                  Minimum 3 caractères requis
                </span>
                <span *ngIf="userForm.get('username')?.errors?.['maxlength']">
                  Maximum 50 caractères autorisés
                </span>
              </p>
            </div>

            <!-- Email -->
            <div class="fr-input-group fr-mt-3w" [class.fr-input-group--error]="isFieldInvalid('email')">
              <label class="fr-label" for="email">
                {{ 'users.email' | translate }}
                <span class="fr-hint-text">{{ 'common.required' | translate }}</span>
              </label>
              <input
                class="fr-input"
                [class.fr-input--error]="isFieldInvalid('email')"
                type="email"
                id="email"
                formControlName="email"
                placeholder="john.doe@example.com"
              />
              <p class="fr-error-text" *ngIf="isFieldInvalid('email')">
                <span *ngIf="userForm.get('email')?.errors?.['required']">
                  {{ 'errors.required_field' | translate }}
                </span>
                <span *ngIf="userForm.get('email')?.errors?.['email']">
                  {{ 'errors.invalid_email' | translate }}
                </span>
              </p>
            </div>

            <!-- Password -->
            <div class="fr-input-group fr-mt-3w" [class.fr-input-group--error]="isFieldInvalid('password')">
              <label class="fr-label" for="password">
                {{ 'auth.password' | translate }}
                <span class="fr-hint-text">{{ 'common.required' | translate }} - Minimum 8 caractères</span>
              </label>
              <input
                class="fr-input"
                [class.fr-input--error]="isFieldInvalid('password')"
                type="password"
                id="password"
                formControlName="password"
                placeholder="••••••••"
              />
              <p class="fr-error-text" *ngIf="isFieldInvalid('password')">
                <span *ngIf="userForm.get('password')?.errors?.['required']">
                  {{ 'errors.required_field' | translate }}
                </span>
                <span *ngIf="userForm.get('password')?.errors?.['minlength']">
                  Minimum 8 caractères requis
                </span>
              </p>
            </div>

            <!-- First Name -->
            <div class="fr-input-group fr-mt-3w">
              <label class="fr-label" for="firstName">
                {{ 'users.first_name' | translate }}
                <span class="fr-hint-text">{{ 'common.optional' | translate }}</span>
              </label>
              <input
                class="fr-input"
                type="text"
                id="firstName"
                formControlName="firstName"
                placeholder="John"
              />
            </div>

            <!-- Last Name -->
            <div class="fr-input-group fr-mt-3w">
              <label class="fr-label" for="lastName">
                {{ 'users.last_name' | translate }}
                <span class="fr-hint-text">{{ 'common.optional' | translate }}</span>
              </label>
              <input
                class="fr-input"
                type="text"
                id="lastName"
                formControlName="lastName"
                placeholder="Doe"
              />
            </div>

            <!-- Phone Number -->
            <div class="fr-input-group fr-mt-3w">
              <label class="fr-label" for="phoneNumber">
                {{ 'users.phone_number' | translate }}
                <span class="fr-hint-text">{{ 'common.optional' | translate }}</span>
              </label>
              <input
                class="fr-input"
                type="tel"
                id="phoneNumber"
                formControlName="phoneNumber"
                placeholder="+33 1 23 45 67 89"
              />
            </div>

            <!-- Roles -->
            <div class="fr-form-group fr-mt-3w">
              <fieldset class="fr-fieldset">
                <legend class="fr-fieldset__legend">
                  {{ 'users.roles' | translate }}
                  <span class="fr-hint-text">{{ 'common.optional' | translate }}</span>
                </legend>
                <div class="fr-fieldset__content">
                  <div class="fr-checkbox-group" *ngFor="let role of availableRoles">
                    <input
                      type="checkbox"
                      [id]="'role-' + role"
                      [value]="role"
                      (change)="onRoleChange(role, $event)"
                    />
                    <label class="fr-label" [for]="'role-' + role">
                      {{ role }}
                    </label>
                  </div>
                </div>
              </fieldset>
            </div>

            <!-- Error Message -->
            <div class="fr-alert fr-alert--error fr-mt-3w" *ngIf="errorMessage">
              <p>{{ errorMessage }}</p>
            </div>

            <!-- Success Message -->
            <div class="fr-alert fr-alert--success fr-mt-3w" *ngIf="successMessage">
              <p>{{ successMessage }}</p>
            </div>

            <!-- Action Buttons -->
            <div class="fr-btns-group fr-btns-group--inline fr-mt-4w">
              <button
                type="submit"
                class="fr-btn"
                [disabled]="userForm.invalid || isSubmitting"
              >
                <span *ngIf="!isSubmitting">{{ 'common.create' | translate }}</span>
                <span *ngIf="isSubmitting">{{ 'common.loading' | translate }}</span>
              </button>
              <a routerLink="/users" class="fr-btn fr-btn--secondary">
                {{ 'common.cancel' | translate }}
              </a>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class UserCreateComponent implements OnInit {
  userForm!: FormGroup;
  availableRoles = Object.values(UserRole);
  selectedRoles: UserRole[] = [UserRole.USER];
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
  }

  /**
   * Initialize the form with validators.
   */
  private initForm(): void {
    this.userForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      firstName: [''],
      lastName: [''],
      phoneNumber: ['']
    });
  }

  /**
   * Check if a field is invalid and touched.
   */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.userForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  /**
   * Handle role checkbox changes.
   */
  onRoleChange(role: UserRole, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      if (!this.selectedRoles.includes(role)) {
        this.selectedRoles.push(role);
      }
    } else {
      this.selectedRoles = this.selectedRoles.filter(r => r !== role);
    }
  }

  /**
   * Submit the form to create a new user.
   */
  onSubmit(): void {
    if (this.userForm.invalid) {
      Object.keys(this.userForm.controls).forEach(key => {
        this.userForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request: CreateUserRequest = {
      username: this.userForm.value.username,
      email: this.userForm.value.email,
      password: this.userForm.value.password,
      firstName: this.userForm.value.firstName || undefined,
      lastName: this.userForm.value.lastName || undefined,
      phoneNumber: this.userForm.value.phoneNumber || undefined,
      roles: this.selectedRoles.length > 0 ? this.selectedRoles : undefined
    };

    this.userService.createUser(request).subscribe({
      next: (user) => {
        this.successMessage = 'Utilisateur créé avec succès';
        this.isSubmitting = false;

        // Redirect to user list after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/users']);
        }, 2000);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Erreur lors de la création de l\'utilisateur';
        this.isSubmitting = false;
      }
    });
  }
}
