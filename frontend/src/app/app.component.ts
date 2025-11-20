import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { HeaderComponent } from './shared/components/header/header.component';
import { LanguageService } from './core/services/language.service';

/**
 * Root application component.
 *
 * This is a standalone component that serves as the entry point for the application.
 * It includes the router outlet for navigation between routes and the main header.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TranslateModule, HeaderComponent],
  template: `
    <app-header></app-header>
    <main role="main">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [
    `
      main {
        min-height: calc(100vh - 160px);
      }
    `,
  ],
})
export class AppComponent implements OnInit {
  title = 'Application Enterprise';

  constructor(private languageService: LanguageService) {}

  ngOnInit() {
    // Language service is initialized in its constructor
    // This ensures the language is set before the app loads
  }
}
