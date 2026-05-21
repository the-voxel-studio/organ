import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-organ',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-8">
      <h1 class="text-2xl font-bold mb-4">Organe (Placeholder)</h1>
      <p class="text-gray-600">UUID Projet : {{ projectUuid }}</p>
      <p class="text-gray-600">UUID Organe : {{ organUuid }}</p>
    </div>
  `
})
export class OrganComponent implements OnInit {
  private route = inject(ActivatedRoute);
  projectUuid: string | null = null;
  organUuid: string | null = null;

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.projectUuid = params.get('projectUuid');
      this.organUuid = params.get('organUuid');
    });
  }
}
