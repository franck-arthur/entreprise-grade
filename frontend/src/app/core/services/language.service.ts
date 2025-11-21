import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Language service for managing application language/locale.
 *
 * Handles language switching and persistence.
 */
@Injectable({
  providedIn: 'root',
})
export class LanguageService {
  private readonly STORAGE_KEY = 'app_language';
  private readonly DEFAULT_LANGUAGE = 'en';
  private readonly SUPPORTED_LANGUAGES = ['en', 'fr'];

  private currentLanguageSubject: BehaviorSubject<string>;
  public currentLanguage$: Observable<string>;

  constructor(private translate: TranslateService) {
    // Initialize with stored language or default
    const storedLanguage = this.getStoredLanguage();
    const initialLanguage: string = storedLanguage && this.isLanguageSupported(storedLanguage)
      ? storedLanguage
      : this.DEFAULT_LANGUAGE;

    this.currentLanguageSubject = new BehaviorSubject<string>(initialLanguage);
    this.currentLanguage$ = this.currentLanguageSubject.asObservable();

    // Configure TranslateService
    this.translate.addLangs(this.SUPPORTED_LANGUAGES);
    this.translate.setDefaultLang(this.DEFAULT_LANGUAGE);
    this.translate.use(initialLanguage);
  }

  /**
   * Get current language.
   */
  getCurrentLanguage(): string {
    return this.currentLanguageSubject.value;
  }

  /**
   * Get supported languages.
   */
  getSupportedLanguages(): string[] {
    return this.SUPPORTED_LANGUAGES;
  }

  /**
   * Set language.
   */
  setLanguage(language: string): void {
    if (this.isLanguageSupported(language)) {
      this.translate.use(language);
      this.storeLanguage(language);
      this.currentLanguageSubject.next(language);
    }
  }

  /**
   * Get language for HTTP header (Accept-Language).
   */
  getLanguageHeader(): string {
    const lang = this.getCurrentLanguage();
    return lang === 'fr' ? 'fr-FR' : 'en-US';
  }

  /**
   * Check if language is supported.
   */
  private isLanguageSupported(language: string | null): boolean {
    return language !== null && this.SUPPORTED_LANGUAGES.includes(language);
  }

  /**
   * Store language in localStorage.
   */
  private storeLanguage(language: string): void {
    localStorage.setItem(this.STORAGE_KEY, language);
  }

  /**
   * Get stored language from localStorage.
   */
  private getStoredLanguage(): string | null {
    return localStorage.getItem(this.STORAGE_KEY);
  }
}
