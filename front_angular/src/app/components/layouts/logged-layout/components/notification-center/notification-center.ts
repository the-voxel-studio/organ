import { Component, OnInit, OnDestroy, inject, signal, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../../../services/api/notification.service';
import { NotificationResponse } from '../../../../../models/notification.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-center.html',
  styleUrl: './notification-center.css'
})
export class NotificationCenterComponent implements OnInit, OnDestroy {
  private notificationService = inject(NotificationService);
  private elementRef = inject(ElementRef);
  private destroy$ = new Subject<void>();

  isNotificationsOpen = signal(false);
  hoveredNotificationUuid = signal<string | null>(null);
  private hoverTimeouts = new Map<string, any>();

  notifications = this.notificationService.notifications;
  unreadNotificationsCount = this.notificationService.unreadNotificationsCount;

  ngOnInit() {
    this.notificationService.initialize();
  }

  ngOnDestroy() {
    this.notificationService.disconnect();
    this.clearAllHoverTimeouts();
  }

  toggleNotifications() {
    this.isNotificationsOpen.update(v => !v);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isNotificationsOpen.set(false);
    }
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
    }, 450);

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
}
