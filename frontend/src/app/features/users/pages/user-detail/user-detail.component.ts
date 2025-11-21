import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="fr-container fr-my-4w">
      <h1>Détail utilisateur</h1>
      <p>Détails de l'utilisateur à implémenter.</p>
      <a routerLink="/users" class="fr-btn fr-btn--secondary">Retour</a>
    </div>
  `
})
export class UserDetailComponent {}
