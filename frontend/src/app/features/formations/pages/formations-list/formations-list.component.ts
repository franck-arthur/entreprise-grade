import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { FormationService } from '@app/core/services/formation.service';
import { AuthService } from '@app/core/services/auth.service';
import {
  Formation,
  FormationFilters,
  PagedResponse,
  ModaliteFormation,
  FormationStatut,
} from '@app/core/models/formation.model';

/**
 * Formations list page component.
 *
 * Displays a paginated list of formations with filtering capabilities.
 */
@Component({
  selector: 'app-formations-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule],
  template: `
    <div class="formations-list">
      <div class="fr-container">
        <div class="fr-grid-row">
          <div class="fr-col-12">
            <h1 class="fr-h1">Gestion des Formations</h1>

            <!-- Actions bar -->
            <div class="fr-mb-4w">
              <button
                class="fr-btn fr-btn--primary"
                [routerLink]="['/formations/new']">
                Créer une formation
              </button>
            </div>

            <!-- Filters -->
            <div class="fr-card fr-p-2w fr-mb-4w">
              <h3 class="fr-h6">Filtres</h3>
              <div class="fr-grid-row fr-grid-row--gutters">
                <div class="fr-col-12 fr-col-md-3">
                  <div class="fr-select-group">
                    <label class="fr-label" for="modalite-filter">Modalité</label>
                    <select
                      class="fr-select"
                      id="modalite-filter"
                      [(ngModel)]="filters.modalite"
                      (change)="onFiltersChange()">
                      <option value="">Toutes les modalités</option>
                      <option *ngFor="let modalite of modalites" [value]="modalite">
                        {{ getModaliteLabel(modalite) }}
                      </option>
                    </select>
                  </div>
                </div>
                <div class="fr-col-12 fr-col-md-3">
                  <div class="fr-select-group">
                    <label class="fr-label" for="statut-filter">Statut</label>
                    <select
                      class="fr-select"
                      id="statut-filter"
                      [(ngModel)]="filters.statut"
                      (change)="onFiltersChange()">
                      <option value="">Tous les statuts</option>
                      <option *ngFor="let statut of statuts" [value]="statut">
                        {{ getStatutLabel(statut) }}
                      </option>
                    </select>
                  </div>
                </div>
                <div class="fr-col-12 fr-col-md-6">
                  <button
                    class="fr-btn fr-btn--secondary"
                    (click)="clearFilters()">
                    Effacer les filtres
                  </button>
                </div>
              </div>
            </div>

            <!-- Loading state -->
            <div *ngIf="loading" class="fr-container fr-py-4w">
              <div class="fr-grid-row fr-grid-row--center">
                <div class="fr-col-auto">
                  Chargement...
                </div>
              </div>
            </div>

            <!-- Error state -->
            <div *ngIf="error" class="fr-alert fr-alert--error">
              <h3 class="fr-alert__title">Erreur</h3>
              <p>{{ error }}</p>
            </div>

            <!-- Formations list -->
            <div *ngIf="!loading && !error && formations.length > 0" class="formations-grid">
              <div *ngFor="let formation of formations" class="fr-card fr-mb-3w">
                <div class="fr-card__body">
                  <div class="fr-card__content">
                    <h3 class="fr-card__title">{{ formation.libelle }}</h3>
                    <p class="fr-card__desc">{{ formation.description }}</p>

                    <div class="formation-info">
                      <p class="fr-badge fr-badge--sm" [class]="getStatutBadgeClass(formation.statut)">
                        {{ getStatutLabel(formation.statut) }}
                      </p>
                      <p class="fr-badge fr-badge--sm fr-badge--blue-cumulus">
                        {{ getModaliteLabel(formation.modalite) }}
                      </p>
                      <p><strong>Date :</strong> {{ formatDate(formation.dateFormation) }}</p>
                      <p><strong>Formateurs :</strong> {{ formation.formateurs }}</p>
                      <p><strong>Lieu :</strong> {{ formation.lieu }}, {{ formation.ville }}</p>
                      <p><strong>Participants :</strong> {{ formation.nbParticipantsInscrits || 0 }}/{{ formation.nbParticipants }}</p>
                    </div>
                  </div>

                  <div class="fr-card__footer">
                    <div class="fr-btns-group fr-btns-group--inline-reverse fr-btns-group--inline-sm">
                      <button
                        class="fr-btn fr-btn--secondary fr-btn--sm"
                        [routerLink]="['/formations', formation.id]">
                        Voir détails
                      </button>
                      <button
                        class="fr-btn fr-btn--sm"
                        [routerLink]="['/formations', formation.id, 'edit']">
                        Modifier
                      </button>
                      <button
                        class="fr-btn fr-btn--secondary fr-btn--sm fr-icon-delete-line"
                        (click)="deleteFormation(formation.id)"
                        [attr.aria-label]="'Supprimer ' + formation.libelle">
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Empty state -->
            <div *ngIf="!loading && !error && formations.length === 0" class="fr-container fr-py-4w">
              <div class="fr-grid-row fr-grid-row--center">
                <div class="fr-col-auto">
                  <p>Aucune formation trouvée.</p>
                </div>
              </div>
            </div>

            <!-- Pagination -->
            <div *ngIf="totalPages > 1" class="fr-pagination" role="navigation" aria-label="Pagination">
              <ul class="fr-pagination__list">
                <li>
                  <button
                    class="fr-pagination__link fr-pagination__link--prev"
                    [disabled]="currentPage === 0"
                    (click)="goToPage(currentPage - 1)">
                    Précédent
                  </button>
                </li>
                <li *ngFor="let page of getPageNumbers()" [class.fr-pagination__link--current]="page === currentPage + 1">
                  <button
                    class="fr-pagination__link"
                    (click)="goToPage(page - 1)">
                    {{ page }}
                  </button>
                </li>
                <li>
                  <button
                    class="fr-pagination__link fr-pagination__link--next"
                    [disabled]="currentPage === totalPages - 1"
                    (click)="goToPage(currentPage + 1)">
                    Suivant
                  </button>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .formations-grid {
        display: grid;
        gap: 1rem;
      }

      .formation-info {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        margin-top: 1rem;
      }

      .formation-info p {
        margin: 0;
        font-size: 0.875rem;
      }

      .fr-pagination {
        margin-top: 2rem;
      }
    `,
  ],
})
export class FormationsListComponent implements OnInit, OnDestroy {
  formations: Formation[] = [];
  loading = false;
  error: string | null = null;

  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;

  filters: FormationFilters = {};
  modalites = Object.values(ModaliteFormation);
  statuts = Object.values(FormationStatut);

  private destroy$ = new Subject<void>();

  constructor(
    private formationService: FormationService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.loadFormations();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadFormations() {
    this.loading = true;
    this.error = null;

    // Check authentication before making API call
    if (!this.authService.isAuthenticated()) {
      console.warn('User not authenticated, redirecting to login');
      this.authService.login();
      return;
    }

    this.formationService
      .getFormations(this.currentPage, this.pageSize, this.filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PagedResponse<Formation>) => {
          this.formations = response.content;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading formations:', error);

          // Handle specific authentication errors
          if (error.message?.includes('401')) {
            console.warn('Authentication failed, redirecting to login');
            this.authService.login();
            return;
          }

          this.error = 'Erreur lors du chargement des formations';
          this.loading = false;
        },
      });
  }

  onFiltersChange() {
    this.currentPage = 0;
    this.loadFormations();
  }

  clearFilters() {
    this.filters = {};
    this.currentPage = 0;
    this.loadFormations();
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadFormations();
    }
  }

  getPageNumbers(): number[] {
    const pages = [];
    const start = Math.max(1, this.currentPage - 1);
    const end = Math.min(this.totalPages, this.currentPage + 3);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  deleteFormation(id: number) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette formation ?')) {
      this.formationService
        .deleteFormation(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.loadFormations();
          },
          error: (error) => {
            console.error('Error deleting formation:', error);
            this.error = 'Erreur lors de la suppression de la formation';
          },
        });
    }
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR');
  }

  getModaliteLabel(modalite: ModaliteFormation): string {
    const labels = {
      [ModaliteFormation.PRESENTIEL]: 'Présentiel',
      [ModaliteFormation.DISTANCIEL]: 'Distanciel',
      [ModaliteFormation.MIXTE]: 'Mixte',
    };
    return labels[modalite] || modalite;
  }

  getStatutLabel(statut: FormationStatut): string {
    const labels = {
      [FormationStatut.PLANIFIE]: 'Planifié',
      [FormationStatut.EN_COURS]: 'En cours',
      [FormationStatut.TERMINE]: 'Terminé',
      [FormationStatut.ANNULE]: 'Annulé',
    };
    return labels[statut] || statut;
  }

  getStatutBadgeClass(statut: FormationStatut): string {
    const classes = {
      [FormationStatut.PLANIFIE]: 'fr-badge--blue-france',
      [FormationStatut.EN_COURS]: 'fr-badge--yellow-moutarde',
      [FormationStatut.TERMINE]: 'fr-badge--green-emeraude',
      [FormationStatut.ANNULE]: 'fr-badge--red-marianne',
    };
    return classes[statut] || '';
  }
}