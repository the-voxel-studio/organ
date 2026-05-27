import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../../../services/api/dashboard.service';
import { DashboardResponse } from '../../../models/dashboard.model';

// Sous-composants
import { PriorityTasksComponent } from './components/priority-tasks/priority-tasks';
import { ProjectGridComponent } from './components/project-grid/project-grid';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    PriorityTasksComponent,
    ProjectGridComponent
  ],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  // Signaux d'état
  dashboardData = signal<DashboardResponse | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  ngOnInit() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.dashboardService.getDashboardData().subscribe({
      next: (data) => {
        this.dashboardData.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load dashboard data', err);
        this.errorMessage.set('Impossible de charger les données du tableau de bord.');
        this.isLoading.set(false);
      }
    });
  }
}
