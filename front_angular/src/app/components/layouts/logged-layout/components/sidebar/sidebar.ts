import { Component, OnInit, OnDestroy, inject, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ProjectService } from '../../../../../services/project.service';
import { DynamicLogoComponent } from '../../../../dynamic-logo/dynamic-logo';
import { ProjectSummary } from '../../../../../models/project.model';
import { Subject, takeUntil, filter } from 'rxjs';
import { NotificationCenterComponent } from '../notification-center/notification-center';
import { UserDropdownComponent } from '../user-dropdown/user-dropdown';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    DynamicLogoComponent,
    NotificationCenterComponent,
    UserDropdownComponent
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent implements OnInit, OnDestroy {
  private projectService = inject(ProjectService);
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  private destroy$ = new Subject<void>();

  projects = signal<ProjectSummary[]>([]);
  activeRoute = signal('');
  sidebarWidth = 368;
  isResizing = false;

  ngOnInit() {
    const savedWidth = localStorage.getItem('sidebar_width');
    if (savedWidth) {
      this.sidebarWidth = parseInt(savedWidth, 10);
    }

    this.activeRoute.set(this.router.url);

    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.activeRoute.set(this.router.url);
      });

    this.projectService.projectsChanged$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadSidebarData();
      });

    this.loadSidebarData();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadSidebarData() {
    this.projectService.getProjects().subscribe({
      next: (data) => {
        this.projects.set(data);
      },
      error: (err) => {
        console.error('Failed to load projects', err);
      }
    });
  }

  isProjectActive(projectUuid: string): boolean {
    const url = this.activeRoute();
    return url.startsWith(`/project/${projectUuid}`) || url.includes(`/organ/${projectUuid}/`);
  }

  getProjectColorHex(color: string): string {
    return color || '#FF7EB6';
  }

  startResizing(event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isResizing = true;
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(event: MouseEvent) {
    if (!this.isResizing) return;
    let newWidth = event.clientX;
    if (newWidth < 240) newWidth = 240;
    if (newWidth > 480) newWidth = 480;
    this.sidebarWidth = newWidth;
  }

  @HostListener('document:mouseup')
  onMouseUp() {
    if (this.isResizing) {
      this.isResizing = false;
      localStorage.setItem('sidebar_width', String(this.sidebarWidth));
    }
  }

  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }
}
