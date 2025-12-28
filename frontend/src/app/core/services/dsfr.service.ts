import { Injectable } from '@angular/core';

/**
 * Service for managing DSFR (Design System of the French Republic) components.
 *
 * This service handles the proper initialization and configuration of DSFR
 * JavaScript components to avoid timing issues with Angular's lifecycle.
 */
@Injectable({
  providedIn: 'root',
})
export class DsfrService {
  private isInitialized = false;
  private scriptLoaded = false;

  constructor() {
    // Check if DSFR script is already loaded (via angular.json scripts)
    this.checkScriptReady();
  }

  /**
   * Check if DSFR script is ready (loaded via angular.json).
   */
  private checkScriptReady(): void {
    if (typeof window !== 'undefined') {
      // Check for both DSFR and our custom loader functions
      if ((window as any).dsfr && (window as any).safeDsfrInit) {
        this.scriptLoaded = true;
        console.log('DSFR script and safe init function available');
      } else {
        // Wait for scripts to load
        setTimeout(() => {
          if ((window as any).dsfr && (window as any).safeDsfrInit) {
            this.scriptLoaded = true;
            console.log('DSFR script and safe init ready');
          } else {
            console.log('Still waiting for DSFR scripts...', {
              dsfr: !!(window as any).dsfr,
              safeDsfrInit: !!(window as any).safeDsfrInit
            });
          }
        }, 300);
      }
    }
  }

  /**
   * Load DSFR script dynamically as fallback.
   */
  private loadDsfrScript(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.scriptLoaded || (typeof window !== 'undefined' && (window as any).dsfr)) {
        this.scriptLoaded = true;
        resolve();
        return;
      }

      const script = document.createElement('script');
      script.src = '/dsfr/dsfr.module.min.js';
      script.async = true;
      script.onload = () => {
        this.scriptLoaded = true;
        console.log('DSFR script loaded dynamically');
        resolve();
      };
      script.onerror = (error) => {
        console.error('Error loading DSFR script:', error);
        reject(error);
      };

      document.head.appendChild(script);
    });
  }

  /**
   * Initialize DSFR components.
   * This should be called after Angular components have been rendered.
   */
  async initializeDsfr(): Promise<void> {
    if (this.isInitialized) {
      return;
    }

    try {
      // Wait for scripts to be available
      let attempts = 0;
      const maxAttempts = 10;

      while (attempts < maxAttempts) {
        if (typeof window !== 'undefined' && (window as any).safeDsfrInit) {
          this.scriptLoaded = true;
          break;
        }
        await new Promise(resolve => setTimeout(resolve, 200));
        attempts++;
      }

      if (!this.scriptLoaded && attempts >= maxAttempts) {
        console.warn('DSFR scripts not loaded after waiting, trying fallback...');
        await this.loadDsfrScript();
      }

      // Use our safe initialization function
      if (typeof window !== 'undefined' && (window as any).safeDsfrInit) {
        try {
          console.log('Using safe DSFR initialization...');
          const success = (window as any).safeDsfrInit();
          if (success) {
            this.isInitialized = true;
            console.log('DSFR initialized successfully via safeDsfrInit');
          } else {
            console.log('Safe init returned false, will retry...');
            // The safe init function handles its own retries
            setTimeout(() => {
              this.isInitialized = true; // Prevent infinite attempts
            }, 2000);
          }
        } catch (error) {
          console.warn('Safe DSFR initialization error:', error);
          this.isInitialized = true;
        }
      } else {
        console.warn('Safe DSFR init function not available, using fallback');
        if (typeof window !== 'undefined' && (window as any).dsfr) {
          this.initializeDsfrCore();
        }
      }
    } catch (error) {
      console.error('Failed to initialize DSFR:', error);
      this.isInitialized = true;
    }
  }

  /**
   * Core DSFR initialization method.
   */
  private initializeDsfrCore(): void {
    try {
      if (typeof window !== 'undefined' && !this.isInitialized) {
        // Try our safe manual initialization first
        if ((window as any).safeDsfrInit) {
          const success = (window as any).safeDsfrInit();
          this.isInitialized = true;
          console.log('DSFR core initialized via safe init:', success);
        } else if ((window as any).initDsfrManually) {
          (window as any).initDsfrManually();
          this.isInitialized = true;
          console.log('DSFR core initialized via manual init');
        } else if ((window as any).dsfr) {
          // Fallback to direct DSFR initialization - avoid this if possible
          console.warn('Using direct DSFR initialization as last resort');
          if ((window as any).dsfr.start) {
            (window as any).dsfr.start();
          } else if ((window as any).dsfr.core && (window as any).dsfr.core.start) {
            (window as any).dsfr.core.start();
          }
          this.isInitialized = true;
          console.log('DSFR core initialized directly (last resort)');
        }
      }
    } catch (error) {
      console.warn('DSFR core initialization failed:', error);
      this.isInitialized = true; // Prevent infinite retries
    }
  }

  /**
   * Re-initialize DSFR for dynamically added components.
   */
  async refreshDsfr(): Promise<void> {
    if (typeof window !== 'undefined' && (window as any).dsfr) {
      try {
        (window as any).dsfr.core.refresh();
      } catch (error) {
        console.warn('DSFR refresh warning:', error);
      }
    }
  }

  /**
   * Check if DSFR is available and initialized.
   */
  isDsfrReady(): boolean {
    return this.scriptLoaded && this.isInitialized &&
           typeof window !== 'undefined' && !!(window as any).dsfr;
  }
}