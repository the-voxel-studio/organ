import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { DashboardService } from '../../../services/api/dashboard.service';
import { DashboardResponse } from '../../../models/dashboard.model';
import { RefreshService } from '../../../services/common/refresh.service';


import { PriorityTasksComponent } from './components/priority-tasks/priority-tasks';
import { ProjectGridComponent } from './components/project-grid/project-grid';
import { ShimmerComponent } from '../../shimmer/shimmer';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    PriorityTasksComponent,
    ProjectGridComponent,
    ShimmerComponent
  ],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private dashboardService = inject(DashboardService);
  private refreshService = inject(RefreshService);
  private destroy$ = new Subject<void>();

  // Signaux d'état
  dashboardData = signal<DashboardResponse | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);


  ngOnInit() {
    this.loadDashboardData();

    this.refreshService.refresh$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadDashboardData();
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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
