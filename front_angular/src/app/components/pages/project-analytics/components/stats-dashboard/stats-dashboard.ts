import { Component, Input, Output, EventEmitter, ElementRef, ViewChild, OnChanges, SimpleChanges, OnDestroy, AfterViewInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, registerables } from 'chart.js';
import { ProjectStatsResponse } from '../../../../../models/project.model';
import { TranslationService } from '../../../../../services/common/translation.service';
import { ShimmerComponent } from '../../../../shimmer/shimmer';

Chart.register(...registerables);

@Component({
  selector: 'app-stats-dashboard',
  standalone: true,
  imports: [CommonModule, ShimmerComponent],
  templateUrl: './stats-dashboard.html'
})
export class StatsDashboardComponent implements OnChanges, OnDestroy, AfterViewInit {
  private translationService = inject(TranslationService);

  @Input() statsData: ProjectStatsResponse | null = null;
  @Input() projectColor = '#FF7EB6';
  @Input() statsDays = 7;
  @Input() isLoading = false;
  @Output() daysChanged = new EventEmitter<number>();

  @ViewChild('activityChart') activityChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('organChart') organChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('taskStatusChart') taskStatusChartCanvas!: ElementRef<HTMLCanvasElement>;

  private activityChart: Chart | null = null;
  private organChart: Chart | null = null;
  private statusChart: Chart | null = null;

  // KPI computations from stats history
  get totalCreated(): number {
    return this.statsData?.history.reduce((sum, h) => sum + h.tasksCreated, 0) || 0;
  }

  get totalCompleted(): number {
    return this.statsData?.history.reduce((sum, h) => sum + h.tasksCompleted, 0) || 0;
  }

  get completionRate(): number {
    const created = this.totalCreated;
    if (created === 0) return 0;
    return Math.round((this.totalCompleted / created) * 100);
  }

  get totalComments(): number {
    return this.statsData?.history.reduce((sum, h) => sum + h.commentsAdded, 0) || 0;
  }

  get totalAttachments(): number {
    return this.statsData?.history.reduce((sum, h) => sum + h.attachmentsAdded, 0) || 0;
  }

  get totalConsultations(): number {
    return this.statsData?.history.reduce((sum, h) => sum + h.consultations, 0) || 0;
  }

  ngOnChanges(changes: SimpleChanges) {
    if ((changes['statsData'] || changes['projectColor'] || changes['statsDays']) && this.statsData) {
      setTimeout(() => this.updateCharts(), 50);
    }
  }

  ngAfterViewInit() {
    if (this.statsData) {
      setTimeout(() => this.updateCharts(), 50);
    }
  }

  ngOnDestroy() {
    this.destroyCharts();
  }

  private destroyCharts() {
    if (this.activityChart) { this.activityChart.destroy(); this.activityChart = null; }
    if (this.organChart) { this.organChart.destroy(); this.organChart = null; }
    if (this.statusChart) { this.statusChart.destroy(); this.statusChart = null; }
  }

  getProjectColorHex(): string {
    return this.projectColor || '#FF7EB6';
  }

  private updateCharts() {
    if (!this.activityChartCanvas || !this.organChartCanvas || !this.taskStatusChartCanvas || !this.statsData) {
      return;
    }

    this.destroyCharts();

    const history = this.statsData.history || [];
    const current = this.statsData.current || [];

    // --- 1. Activity Chart ---
    const dates = history.map(h => this.formatDateLabel(h.date));
    const created = history.map(h => h.tasksCreated);
    const completed = history.map(h => h.tasksCompleted);
    const consultations = history.map(h => h.consultations);

    const highlightColor = this.getProjectColorHex();

    const ctx = this.activityChartCanvas.nativeElement.getContext('2d');
    if (ctx) {
      this.activityChart = new Chart(ctx, {
        type: 'line',
        data: {
          labels: dates,
          datasets: [
            {
              label: 'Tâches créées',
              data: created,
              borderColor: '#FF7DD4', // Secondary bubblegum color
              backgroundColor: 'rgba(255, 125, 212, 0.05)',
              tension: 0.35,
              fill: true,
              borderWidth: 3,
              pointBackgroundColor: '#FF7DD4'
            },
            {
              label: 'Tâches terminées',
              data: completed,
              borderColor: highlightColor, // Couleur primaire du projet
              backgroundColor: highlightColor + '0d',
              tension: 0.35,
              fill: true,
              borderWidth: 3,
              pointBackgroundColor: highlightColor
            },
            {
              label: 'Consultations',
              data: consultations,
              borderColor: '#94A3B8', // Third color: gray
              backgroundColor: 'transparent',
              borderDash: [5, 5],
              tension: 0.35,
              borderWidth: 2,
              pointBackgroundColor: '#94A3B8'
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { 
              position: 'top',
              labels: { 
                font: { family: 'Outfit, sans-serif', weight: 'bold', size: 12 },
                color: '#1f2937'
              } 
            }
          },
          scales: {
            y: { 
              beginAtZero: true, 
              ticks: { precision: 0, color: '#9ca3af' },
              grid: { color: 'rgba(243, 244, 246, 1)' }
            },
            x: {
              ticks: { color: '#9ca3af' },
              grid: { display: false }
            }
          }
        }
      });
    }

    // --- 2. Organ Distribution Chart ---
    const organMap = new Map<string, { tasks: number; hours: number }>();
    current.forEach(item => {
      const name = item.organ_name || 'Sans Organe';
      const tasks = Number(item.task_count || 0);
      const hours = Number(item.total_hours || 0);
      if (!organMap.has(name)) {
        organMap.set(name, { tasks: 0, hours: 0 });
      }
      const val = organMap.get(name)!;
      val.tasks += tasks;
      val.hours += hours;
    });

    const organLabels = Array.from(organMap.keys());
    const organTasks = organLabels.map(l => organMap.get(l)!.tasks);
    const organHours = organLabels.map(l => organMap.get(l)!.hours);

    const organCtx = this.organChartCanvas.nativeElement.getContext('2d');
    if (organCtx) {
      this.organChart = new Chart(organCtx, {
        type: 'bar',
        data: {
          labels: organLabels,
          datasets: [
            {
              label: 'Tâches actives',
              data: organTasks,
              backgroundColor: highlightColor, // Couleur primaire du projet
              borderRadius: 8
            },
            {
              label: 'Heures estimées',
              data: organHours,
              backgroundColor: '#FF7DD4', // Secondary bubblegum color
              borderRadius: 8
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { 
              position: 'top',
              labels: { 
                font: { family: 'Outfit, sans-serif', weight: 'bold', size: 12 },
                color: '#1f2937'
              } 
            }
          },
          scales: {
            y: { 
              beginAtZero: true, 
              ticks: { precision: 0, color: '#9ca3af' },
              grid: { color: 'rgba(243, 244, 246, 1)' }
            },
            x: {
              ticks: { color: '#9ca3af' },
              grid: { display: false }
            }
          }
        }
      });
    }

    // --- 3. Status Doughnut Chart ---
    const statusMap = new Map<string, number>();
    current.forEach(item => {
      const count = Number(item.task_count || 0);
      if (count > 0) {
        const status = item.status || 'UNKNOWN';
        statusMap.set(status, (statusMap.get(status) || 0) + count);
      }
    });

    const statusKeys = Array.from(statusMap.keys());
    const statusLabels = statusKeys.map(k => this.translationService.translateStatus(k));
    const statusCounts = statusKeys.map(k => statusMap.get(k)!);

    const statusColors: { [key: string]: string } = {
      'TODO': '#cad5e2', // Slate gray (Third color)
      'IN_PROGRESS': highlightColor, // Couleur primaire du projet
      'WAITING': '#ffba00', // Secondary bubblegum color
      'DONE': '#05df72', // Slate gray (Third color)
      'CANCELED': '#ff637e' // Dark slate gray (Third color)
    };
    const colors = statusKeys.map(k => statusColors[k.toUpperCase()] || '#CBD5E1');

    const statusCtx = this.taskStatusChartCanvas.nativeElement.getContext('2d');
    if (statusCtx) {
      this.statusChart = new Chart(statusCtx, {
        type: 'doughnut',
        data: {
          labels: statusLabels,
          datasets: [
            {
              data: statusCounts,
              backgroundColor: colors,
              borderWidth: 3,
              borderColor: '#ffffff'
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { 
              position: 'right',
              labels: { 
                font: { family: 'Outfit, sans-serif', weight: 'bold', size: 12 },
                color: '#1f2937'
              } 
            }
          },
          cutout: '65%'
        }
      });
    }
  }


  formatDateLabel(dateStr: string): string {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
    } catch {
      return dateStr;
    }
  }
}
