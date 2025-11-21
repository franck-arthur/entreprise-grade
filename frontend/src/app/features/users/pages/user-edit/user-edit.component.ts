import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-user-edit',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="fr-container fr-my-4w">
      <h1>Modifier utilisateur</h1>
      <p>Formulaire de modification à implémenter.</p>
      <a routerLink="/users" class="fr-btn fr-btn--secondary">Retour</a>
    </div>
  `
})
export class UserEditComponent {}
