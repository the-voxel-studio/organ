import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { DashboardService } from '../../../services/dashboard.service';
import { DashboardResponse } from '../../../models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private sanitizer = inject(DomSanitizer);

  // State signals
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

  getProjectColorHex(color: string | null | undefined): string {
    return color || '#FF7EB6';
  }

  formatDate(dateString: string | null): string {
    if (!dateString) return '-';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch (e) {
      return '-';
    }
  }

  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }
}
