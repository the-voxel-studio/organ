import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-project',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-8">
      <h1 class="text-2xl font-bold mb-4">Projet (Placeholder)</h1>
      <p class="text-gray-600">UUID du projet : {{ projectUuid }}</p>
    </div>
  `
})
export class ProjectComponent implements OnInit {
  private route = inject(ActivatedRoute);
  projectUuid: string | null = null;

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.projectUuid = params.get('uuid');
    });
  }
}
