import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '@app/core/services/language.service';

/**
 * Language selector component.
 *
 * Displays a dropdown to switch between supported languages.
 * Uses DSFR select component for consistent styling.
 */
@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  template: `
    <div class="fr-select-group">
      <label class="fr-label" for="language-select">
        {{ 'language.select' | translate }}
      </label>
      <select
        class="fr-select"
        id="language-select"
        [(ngModel)]="currentLanguage"
        (change)="onLanguageChange()"
      >
        <option *ngFor="let lang of supportedLanguages" [value]="lang">
          {{ 'language.' + lang | translate }}
        </option>
      </select>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
})
export class LanguageSelectorComponent {
  currentLanguage: string;
  supportedLanguages: string[];

  constructor(private languageService: LanguageService) {
    this.currentLanguage = this.languageService.getCurrentLanguage();
    this.supportedLanguages = this.languageService.getSupportedLanguages();
  }

  onLanguageChange(): void {
    this.languageService.setLanguage(this.currentLanguage);
  }
}
