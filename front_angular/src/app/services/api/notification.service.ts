import { Injectable, inject, signal, computed, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, tap } from 'rxjs';
import {
  NotificationResponse,
  SubscribeUrlResponse,
  NotificationMessageResponse
} from '../../models/notification.model';
import { ProjectInvitationService } from './project-invitation.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private http = inject(HttpClient);
  private projectInvitationService = inject(ProjectInvitationService);

  // État réactif
  notifications = signal<NotificationResponse[]>([]);
  unreadNotificationsCount = computed(() => this.notifications().filter(n => !n.isRead).length);

  private eventSource: EventSource | null = null;

  ngOnDestroy(): void {
    this.disconnect();
  }

  initialize(): void {
    this.loadNotifications();
    this.setupMercure();
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  loadNotifications(): void {
    forkJoin({
      notifications: this.getNotifications(),
      invitations: this.projectInvitationService.getInvitations()
    }).subscribe({
      next: ({ notifications, invitations }) => {
        const inviteNotifications: NotificationResponse[] = invitations.map(inv => ({
          uuid: inv.uuid,
          type: 'PROJECT_INVITATION',
          message: `Vous avez été invité à rejoindre le projet "${inv.projectName}" par ${inv.invitedBy}`,
          isRead: false,
          createdAt: inv.createdAt,
          isRealInvite: true
        }));

        const all = [...inviteNotifications, ...notifications];
        this.sortAndSetNotifications(all);
      },
      error: (err) => {
        console.error('Failed to load notifications and invitations:', err);
      }
    });
  }

  private sortAndSetNotifications(list: NotificationResponse[]): void {
    list.sort((a, b) => {
      // 1. Invitations de projet d'abord
      if (a.isRealInvite && !b.isRealInvite) return -1;
      if (!a.isRealInvite && b.isRealInvite) return 1;

      // 2. Unread before read
      if (!a.isRead && b.isRead) return -1;
      if (a.isRead && !b.isRead) return 1;

      // 3. By Date DESC
      const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return dateB - dateA;
    });

    this.notifications.set(list);
  }

  setupMercure(): void {
    this.getSubscribeUrl().subscribe({
      next: (data) => {
        this.disconnect();

        // Use the direct Mercure docker service link exposed on port 9090
        const hubUrl = new URL('http://localhost:9090/.well-known/mercure');
        hubUrl.searchParams.set('topic', data.topic);
        hubUrl.searchParams.set('authorization', data.token);

        console.log('Connecting directly to Mercure hub:', hubUrl.toString());
        this.eventSource = new EventSource(hubUrl.toString());

        this.eventSource.onmessage = (event) => {
          try {
            const notification = JSON.parse(event.data);
            if (notification.type === 'PROJECT_INVITATION') {
              this.loadNotifications();
            } else {
              this.notifications.update(list => {
                // If it already exists, replace it, otherwise prepend
                const filtered = list.filter(n => n.uuid !== notification.uuid);
                const updated = [notification, ...filtered];
                
                // Re-sort
                updated.sort((a, b) => {
                  if (a.isRealInvite && !b.isRealInvite) return -1;
                  if (!a.isRealInvite && b.isRealInvite) return 1;

                  if (!a.isRead && b.isRead) return -1;
                  if (a.isRead && !b.isRead) return 1;

                  const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
                  const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
                  return dateB - dateA;
                });
                return updated;
              });
            }
          } catch (e) {
            console.error('Error parsing Mercure message:', e);
          }
        };
      },
      error: (err) => {
        console.error('Failed to setup Mercure stream:', err);
      }
    });
  }

  getNotifications(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>('/api/notifications');
  }

  getSubscribeUrl(): Observable<SubscribeUrlResponse> {
    return this.http.get<SubscribeUrlResponse>('/api/notifications/subscribe');
  }

  getTrashedNotifications(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>('/api/notifications/trash');
  }

  restoreNotification(uuid: string): Observable<NotificationMessageResponse> {
    return this.http.post<NotificationMessageResponse>(`/api/notifications/${uuid}/restore`, {}).pipe(
      tap(() => this.loadNotifications())
    );
  }

  markAsRead(uuid: string): Observable<NotificationMessageResponse> {
    return this.http.patch<NotificationMessageResponse>(`/api/notifications/${uuid}/read`, {}).pipe(
      tap(() => {
        this.notifications.update(list =>
          list.map(n => n.uuid === uuid ? { ...n, isRead: true } : n)
        );
      })
    );
  }

  deleteNotification(uuid: string): Observable<void> {
    return this.http.delete<void>(`/api/notifications/${uuid}`).pipe(
      tap(() => this.loadNotifications())
    );
  }

  acceptInvitation(uuid: string): Observable<any> {
    return this.projectInvitationService.acceptInvitation(uuid).pipe(
      tap(() => this.loadNotifications())
    );
  }

  refuseInvitation(uuid: string): Observable<any> {
    return this.projectInvitationService.refuseInvitation(uuid).pipe(
      tap(() => this.loadNotifications())
    );
  }
}
