import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet, NavigationEnd } from '@angular/router';
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
  private destroy$ = new Subject<void>();

  // State signals
  projects = signal<ProjectSummary[]>([]);
  notifications = signal<NotificationResponse[]>([]);
  isNotificationsOpen = signal(false);
  isProfileOpen = signal(false);
  activeRoute = signal('');

  // Computed properties
  unreadNotificationsCount = computed(() => {
    return this.notifications().filter(n => !n.isRead).length;
  });

  currentUserInitials = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return 'U';
    const first = user.firstName ? user.firstName.charAt(0).toUpperCase() : '';
    const last = user.lastName ? user.lastName.charAt(0).toUpperCase() : '';
    return first + last || 'U';
  });

  ngOnInit() {
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

    // Load dynamic data
    this.loadSidebarData();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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

    // Get notifications list
    this.notificationService.getNotifications().subscribe({
      next: (data) => {
        this.notifications.set(data);
      },
      error: (err) => {
        console.error('Failed to load notifications', err);
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
  }

  markAllNotificationsAsRead() {
    const unread = this.notifications().filter(n => !n.isRead);
    if (unread.length === 0) return;

    unread.forEach(notification => {
      this.notificationService.markAsRead(notification.uuid).subscribe({
        next: () => {
          // Update notification item in list
          this.notifications.update(list =>
            list.map(n => n.uuid === notification.uuid ? { ...n, isRead: true } : n)
          );
        }
      });
    });
  }

  markNotificationAsRead(uuid: string, event: MouseEvent) {
    event.stopPropagation();
    this.notificationService.markAsRead(uuid).subscribe({
      next: () => {
        this.notifications.update(list =>
          list.map(n => n.uuid === uuid ? { ...n, isRead: true } : n)
        );
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
}
