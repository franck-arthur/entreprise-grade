import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-user-create',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="fr-container fr-my-4w">
      <h1>Créer un utilisateur</h1>
      <p>Formulaire de création à implémenter.</p>
      <a routerLink="/users" class="fr-btn fr-btn--secondary">Retour</a>
    </div>
  `
})
export class UserCreateComponent {}
