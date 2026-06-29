import { Component, Input, ElementRef, ViewChild, OnChanges, SimpleChanges, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, registerables } from 'chart.js';
import { MemberActivityStats } from '../../../../../models/project.model';

Chart.register(...registerables);

@Component({
  selector: 'app-audit-ranking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-ranking.html'
})
export class AuditRankingComponent implements OnChanges, OnDestroy, AfterViewInit {
  @Input() memberStats: MemberActivityStats[] = [];
  @Input() totalLogsCount = 0;
  @Input() mostActiveMember: MemberActivityStats | null = null;
  @Input() projectColor = '#FF7EB6';

  @ViewChild('memberActivityChart') memberActivityChartCanvas!: ElementRef<HTMLCanvasElement>;

  private memberActivityChart: Chart | null = null;

  ngOnChanges(changes: SimpleChanges) {
    if ((changes['memberStats'] || changes['projectColor']) && this.memberStats.length > 0) {
      setTimeout(() => this.updateMemberChart(), 50);
    }
  }

  ngAfterViewInit() {
    if (this.memberStats.length > 0) {
      setTimeout(() => this.updateMemberChart(), 50);
    }
  }

  ngOnDestroy() {
    if (this.memberActivityChart) {
      this.memberActivityChart.destroy();
      this.memberActivityChart = null;
    }
  }

  getProjectColorHex(): string {
    return this.projectColor || '#FF7EB6';
  }

  private updateMemberChart() {
    if (!this.memberActivityChartCanvas) return;

    if (this.memberActivityChart) {
      this.memberActivityChart.destroy();
      this.memberActivityChart = null;
    }

    // Filtre le top 5 des membres actifs selon les types d'actions cochés
    const topMembers = this.memberStats.slice(0, 5).filter(m => m.filteredActionsCount > 0);
    const labels = topMembers.map(m => `${m.firstName} ${m.lastName}`);
    const data = topMembers.map(m => m.filteredActionsCount);
    const highlightColor = this.getProjectColorHex();

    const ctx = this.memberActivityChartCanvas.nativeElement.getContext('2d');
    if (ctx) {
      this.memberActivityChart = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [
            {
              label: 'Actions sélectionnées',
              data: data,
              backgroundColor: highlightColor + 'd0',
              borderRadius: 8,
              barThickness: 16
            }
          ]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            x: { 
              beginAtZero: true, 
              ticks: { precision: 0, color: '#9ca3af' },
              grid: { color: 'rgba(243, 244, 246, 1)' }
            },
            y: {
              ticks: { color: '#374151', font: { family: 'Outfit, sans-serif', weight: 'bold', size: 11 } },
              grid: { display: false }
            }
          }
        }
      });
    }
  }
}
