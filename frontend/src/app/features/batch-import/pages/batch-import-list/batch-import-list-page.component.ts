import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BatchImportListComponent } from '../../components/batch-import-list/batch-import-list.component';
import { BatchImportUploadComponent } from '../../components/batch-import-upload/batch-import-upload.component';

/**
 * Page component for batch import management.
 * Combines upload and list functionality.
 */
@Component({
  selector: 'app-batch-import-list-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    BatchImportListComponent,
    BatchImportUploadComponent
  ],
  template: `
    <div class="fr-container fr-my-4w">
      <div class="fr-grid-row">
        <div class="fr-col-12">
          <h1>Imports par lot</h1>
          <p class="fr-text--lg">
            Importez des utilisateurs en masse via des fichiers CSV.
          </p>
        </div>
      </div>

      <!-- Upload Section -->
      <div class="fr-grid-row fr-mt-4w">
        <div class="fr-col-12">
          <div class="fr-card">
            <div class="fr-card__body">
              <div class="fr-card__content">
                <h2 class="fr-card__title">Nouvel import</h2>
                <app-batch-import-upload></app-batch-import-upload>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- List Section -->
      <div class="fr-grid-row fr-mt-4w">
        <div class="fr-col-12">
          <div class="fr-card">
            <div class="fr-card__body">
              <div class="fr-card__content">
                <h2 class="fr-card__title">Historique des imports</h2>
                <app-batch-import-list></app-batch-import-list>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class BatchImportListPageComponent {}
