import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-trash',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-8">
      <h1 class="text-2xl font-bold mb-4">Corbeille (Placeholder)</h1>
      <p class="text-gray-600">Retrouvez vos éléments supprimés ici.</p>
    </div>
  `
})
export class TrashComponent {}
