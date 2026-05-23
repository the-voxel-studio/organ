import { Component, inject, signal, computed, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet, NavigationEnd } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { AuthService } from '../../../services/auth.service';
import { ProjectService } from '../../../services/project.service';
import { NotificationService } from '../../../services/notification.service';
import { DynamicLogoComponent } from '../../dynamic-logo/dynamic-logo';
import { ProjectSummary } from '../../../models/project.model';
import { NotificationResponse } from '../../../models/notification.model';
import { Subject, takeUntil, filter } from 'rxjs';

@Component({
  selector: 'app-logged-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, DynamicLogoComponent],
  templateUrl: './logged-layout.html',
  styleUrl: './logged-layout.css'
})
export class LoggedLayoutComponent implements OnInit, OnDestroy {
  protected authService = inject(AuthService);
  private projectService = inject(ProjectService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  private destroy$ = new Subject<void>();

  // State signals
  projects = signal<ProjectSummary[]>([]);
  isNotificationsOpen = signal(false);
  isProfileOpen = signal(false);
  activeRoute = signal('');
  hoveredNotificationUuid = signal<string | null>(null);
  private hoverTimeouts = new Map<string, any>();

  // Sidebar resizing properties
  sidebarWidth = 368; // default w-92 is 368px
  isResizing = false;

  // Bound properties from service
  notifications = this.notificationService.notifications;
  unreadNotificationsCount = this.notificationService.unreadNotificationsCount;

  currentUserInitials = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return 'U';
    const first = user.firstName ? user.firstName.charAt(0).toUpperCase() : '';
    const last = user.lastName ? user.lastName.charAt(0).toUpperCase() : '';
    return first + last || 'U';
  });

  ngOnInit() {
    // Load saved sidebar width
    const savedWidth = localStorage.getItem('sidebar_width');
    if (savedWidth) {
      this.sidebarWidth = parseInt(savedWidth, 10);
    }

    // Set initial route
    this.activeRoute.set(this.router.url);

    // Track active route changes
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.activeRoute.set(this.router.url);
        // Auto-close menus on navigate
        this.isNotificationsOpen.set(false);
        this.isProfileOpen.set(false);
      });

    // Track projects change to reload sidebar data
    this.projectService.projectsChanged$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadSidebarData();
      });

    // Initialize notification stream (loads notifications + connects Mercure)
    this.notificationService.initialize();

    // Load dynamic data
    this.loadSidebarData();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    // Disconnect Mercure EventSource when layout is destroyed
    this.notificationService.disconnect();
    this.clearAllHoverTimeouts();
  }

  loadSidebarData() {
    // Get projects list
    this.projectService.getProjects().subscribe({
      next: (data) => {
        this.projects.set(data);
      },
      error: (err) => {
        console.error('Failed to load projects', err);
      }
    });
  }

  toggleNotifications(event: MouseEvent) {
    event.stopPropagation();
    this.isNotificationsOpen.update(v => !v);
    if (this.isNotificationsOpen()) {
      this.isProfileOpen.set(false);
    }
  }

  toggleProfile(event: MouseEvent) {
    event.stopPropagation();
    this.isProfileOpen.update(v => !v);
    if (this.isProfileOpen()) {
      this.isNotificationsOpen.set(false);
    }
  }

  closeDropdowns() {
    this.isNotificationsOpen.set(false);
    this.isProfileOpen.set(false);
    this.clearAllHoverTimeouts();
  }

  markAllNotificationsAsRead() {
    const unread = this.notifications().filter(n => !n.isRead && !n.isRealInvite);
    if (unread.length === 0) return;

    unread.forEach(notification => {
      this.notificationService.markAsRead(notification.uuid).subscribe();
    });
  }

  markNotificationAsRead(uuid: string, event: MouseEvent) {
    event.stopPropagation();
    this.notificationService.markAsRead(uuid).subscribe();
  }

  onMouseEnterNotification(notif: NotificationResponse) {
    if (notif.isRead || notif.isRealInvite) return;

    this.hoveredNotificationUuid.set(notif.uuid);

    const timeout = setTimeout(() => {
      this.notificationService.markAsRead(notif.uuid).subscribe({
        next: () => {
          if (this.hoveredNotificationUuid() === notif.uuid) {
            this.hoveredNotificationUuid.set(null);
          }
        }
      });
      this.hoverTimeouts.delete(notif.uuid);
    }, 450); // 450ms hover to read

    this.hoverTimeouts.set(notif.uuid, timeout);
  }

  onMouseLeaveNotification(notif: NotificationResponse) {
    if (this.hoveredNotificationUuid() === notif.uuid) {
      this.hoveredNotificationUuid.set(null);
    }
    const timeout = this.hoverTimeouts.get(notif.uuid);
    if (timeout) {
      clearTimeout(timeout);
      this.hoverTimeouts.delete(notif.uuid);
    }
  }

  private clearAllHoverTimeouts() {
    this.hoverTimeouts.forEach(timeout => clearTimeout(timeout));
    this.hoverTimeouts.clear();
    this.hoveredNotificationUuid.set(null);
  }

  acceptInvite(uuid: string, event: MouseEvent) {
    event.stopPropagation();
    this.notificationService.acceptInvitation(uuid).subscribe({
      next: () => {
        // Refresh the whole page to load new projects and update navigation
        window.location.reload();
      },
      error: (err) => {
        console.error('Failed to accept invitation:', err);
        alert(err?.error?.message || 'Erreur lors de l\'acceptation de l\'invitation.');
      }
    });
  }

  refuseInvite(uuid: string, event: MouseEvent) {
    event.stopPropagation();
    this.notificationService.refuseInvitation(uuid).subscribe({
      error: (err) => {
        console.error('Failed to refuse invitation:', err);
      }
    });
  }

  onLogout() {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Logout failed', err);
        this.router.navigate(['/login']);
      }
    });
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
    
    // Bounds check
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
