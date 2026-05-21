import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-8">
      <h1 class="text-2xl font-bold mb-4">Paramètres (Placeholder)</h1>
      <p class="text-gray-600">Gérez vos préférences et options de compte ici.</p>
    </div>
  `
})
export class SettingsComponent {}
