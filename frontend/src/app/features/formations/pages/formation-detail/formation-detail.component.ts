import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FormationService } from '@app/core/services/formation.service';
import { Formation, FormationParticipation, ModaliteFormation, FormationStatut } from '@app/core/models/formation.model';

/**
 * Formation detail page component.
 *
 * Displays detailed information about a specific formation.
 */
@Component({
  selector: 'app-formation-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  template: `
    <div class="formation-detail">
      <div class="fr-container">
        <div class="fr-grid-row">
          <div class="fr-col-12">
            <!-- Breadcrumb -->
            <nav role="navigation" class="fr-breadcrumb" aria-label="Fil d'Ariane">
              <ol class="fr-breadcrumb__list">
                <li><a class="fr-breadcrumb__link" [routerLink]="['/formations']">Formations</a></li>
                <li><span class="fr-breadcrumb__link" aria-current="page">{{ formation?.libelle || 'Détail' }}</span></li>
              </ol>
            </nav>

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

            <!-- Formation detail -->
            <div *ngIf="formation && !loading && !error">
              <!-- Header -->
              <div class="fr-grid-row fr-grid-row--gutters fr-mb-3w">
                <div class="fr-col-12 fr-col-md-8">
                  <h1 class="fr-h1">{{ formation.libelle }}</h1>
                  <div class="formation-badges fr-mb-2w">
                    <p class="fr-badge fr-badge--sm" [class]="getStatutBadgeClass(formation.statut)">
                      {{ getStatutLabel(formation.statut) }}
                    </p>
                    <p class="fr-badge fr-badge--sm fr-badge--blue-cumulus">
                      {{ getModaliteLabel(formation.modalite) }}
                    </p>
                    <p *ngIf="formation.complet" class="fr-badge fr-badge--sm fr-badge--red-marianne">
                      Complet
                    </p>
                  </div>
                </div>
                <div class="fr-col-12 fr-col-md-4">
                  <div class="fr-btns-group fr-btns-group--inline-reverse">
                    <button
                      class="fr-btn fr-btn--secondary"
                      [routerLink]="['/formations', formation.id, 'edit']">
                      Modifier
                    </button>
                    <button
                      class="fr-btn fr-btn--secondary fr-icon-delete-line"
                      (click)="deleteFormation()">
                      Supprimer
                    </button>
                  </div>
                </div>
              </div>

              <!-- Information generale -->
              <div class="fr-grid-row fr-grid-row--gutters">
                <div class="fr-col-12 fr-col-md-8">
                  <div class="fr-card">
                    <div class="fr-card__body">
                      <div class="fr-card__content">
                        <h3 class="fr-card__title">Informations générales</h3>

                        <div class="formation-info">
                          <div class="info-item">
                            <strong>Description :</strong>
                            <p>{{ formation.description }}</p>
                          </div>

                          <div class="info-item">
                            <strong>Formateurs :</strong>
                            <p>{{ formation.formateurs }}</p>
                          </div>

                          <div class="info-item">
                            <strong>Date de formation :</strong>
                            <p>{{ formatDate(formation.dateFormation) }}</p>
                          </div>

                          <div class="info-item">
                            <strong>Horaires :</strong>
                            <p>{{ formation.heureDebut }} - {{ formation.heureFin }}</p>
                          </div>

                          <div class="info-item">
                            <strong>Lieu :</strong>
                            <p>{{ formation.lieu }}, {{ formation.ville }}</p>
                          </div>

                          <div class="info-item" *ngIf="formation.lienParticipation">
                            <strong>Lien de participation :</strong>
                            <p><a [href]="formation.lienParticipation" target="_blank" rel="noopener">{{ formation.lienParticipation }}</a></p>
                          </div>

                          <div class="info-item">
                            <strong>Secteur :</strong>
                            <p>{{ formation.secteur.libelle }}</p>
                          </div>

                          <div class="info-item">
                            <strong>Région :</strong>
                            <p>{{ formation.region.libelle }}</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Participants info -->
                <div class="fr-col-12 fr-col-md-4">
                  <div class="fr-card">
                    <div class="fr-card__body">
                      <div class="fr-card__content">
                        <h3 class="fr-card__title">Participants</h3>

                        <div class="participants-stats">
                          <div class="stat-item">
                            <span class="stat-number">{{ formation.nbParticipantsInscrits || 0 }}</span>
                            <span class="stat-label">Inscrits</span>
                          </div>
                          <div class="stat-item">
                            <span class="stat-number">{{ formation.nbParticipants }}</span>
                            <span class="stat-label">Places disponibles</span>
                          </div>
                          <div class="stat-item">
                            <span class="stat-number">{{ getPlacesRestantes() }}</span>
                            <span class="stat-label">Places restantes</span>
                          </div>
                        </div>

                        <button
                          class="fr-btn fr-btn--sm fr-btn--secondary fr-mt-2w"
                          (click)="loadParticipants()"
                          [disabled]="loadingParticipants">
                          {{ loadingParticipants ? 'Chargement...' : 'Voir les participants' }}
                        </button>
                      </div>
                    </div>
                  </div>

                  <!-- Metadata -->
                  <div class="fr-card fr-mt-2w">
                    <div class="fr-card__body">
                      <div class="fr-card__content">
                        <h3 class="fr-card__title">Métadonnées</h3>

                        <div class="metadata">
                          <p><strong>Créé le :</strong> {{ formatDateTime(formation.createdAt) }}</p>
                          <p><strong>Modifié le :</strong> {{ formatDateTime(formation.updatedAt) }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Participants list -->
              <div *ngIf="showParticipants" class="fr-mt-4w">
                <div class="fr-card">
                  <div class="fr-card__body">
                    <div class="fr-card__content">
                      <h3 class="fr-card__title">Liste des participants</h3>

                      <div *ngIf="loadingParticipants" class="fr-container fr-py-2w">
                        <div class="fr-grid-row fr-grid-row--center">
                          <div class="fr-col-auto">
                            Chargement des participants...
                          </div>
                        </div>
                      </div>

                      <div *ngIf="participantsError" class="fr-alert fr-alert--error fr-alert--sm">
                        <p>{{ participantsError }}</p>
                      </div>

                      <div *ngIf="participants.length > 0 && !loadingParticipants">
                        <div class="fr-table">
                          <table>
                            <thead>
                              <tr>
                                <th scope="col">ID Utilisateur</th>
                                <th scope="col">Date d'inscription</th>
                                <th scope="col">Statut</th>
                                <th scope="col">Remarques</th>
                              </tr>
                            </thead>
                            <tbody>
                              <tr *ngFor="let participant of participants">
                                <td>{{ participant.userId }}</td>
                                <td>{{ formatDate(participant.dateInscription) }}</td>
                                <td>{{ participant.statut }}</td>
                                <td>{{ participant.remarques || '-' }}</td>
                              </tr>
                            </tbody>
                          </table>
                        </div>
                      </div>

                      <div *ngIf="participants.length === 0 && !loadingParticipants && !participantsError">
                        <p>Aucun participant inscrit.</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .formation-badges {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
      }

      .formation-info {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .info-item strong {
        display: block;
        margin-bottom: 0.25rem;
        color: var(--text-title-grey);
      }

      .info-item p {
        margin: 0;
      }

      .participants-stats {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .stat-item {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .stat-number {
        font-size: 1.5rem;
        font-weight: bold;
        color: var(--text-title-blue-france);
      }

      .stat-label {
        color: var(--text-mention-grey);
        font-size: 0.875rem;
      }

      .metadata p {
        margin: 0;
        font-size: 0.875rem;
        color: var(--text-mention-grey);
      }

      .fr-table {
        margin-top: 1rem;
      }

      .fr-table table {
        width: 100%;
      }
    `,
  ],
})
export class FormationDetailComponent implements OnInit, OnDestroy {
  formation: Formation | null = null;
  participants: FormationParticipation[] = [];
  loading = false;
  loadingParticipants = false;
  showParticipants = false;
  error: string | null = null;
  participantsError: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private formationService: FormationService
  ) {}

  ngOnInit() {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.loadFormation(id);
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadFormation(id: number) {
    this.loading = true;
    this.error = null;

    this.formationService
      .getFormationById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (formation: Formation) => {
          this.formation = formation;
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading formation:', error);
          this.error = 'Erreur lors du chargement de la formation';
          this.loading = false;
        },
      });
  }

  loadParticipants() {
    if (!this.formation || this.loadingParticipants) return;

    this.loadingParticipants = true;
    this.participantsError = null;
    this.showParticipants = true;

    this.formationService
      .getFormationParticipants(this.formation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (participants: FormationParticipation[]) => {
          this.participants = participants;
          this.loadingParticipants = false;
        },
        error: (error) => {
          console.error('Error loading participants:', error);
          this.participantsError = 'Erreur lors du chargement des participants';
          this.loadingParticipants = false;
        },
      });
  }

  deleteFormation() {
    if (!this.formation) return;

    if (confirm(`Êtes-vous sûr de vouloir supprimer la formation "${this.formation.libelle}" ?`)) {
      this.formationService
        .deleteFormation(this.formation.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.router.navigate(['/formations']);
          },
          error: (error) => {
            console.error('Error deleting formation:', error);
            this.error = 'Erreur lors de la suppression de la formation';
          },
        });
    }
  }

  getPlacesRestantes(): number {
    if (!this.formation) return 0;
    return Math.max(0, this.formation.nbParticipants - (this.formation.nbParticipantsInscrits || 0));
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR');
  }

  formatDateTime(dateTimeStr: string): string {
    const date = new Date(dateTimeStr);
    return date.toLocaleDateString('fr-FR') + ' à ' + date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
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