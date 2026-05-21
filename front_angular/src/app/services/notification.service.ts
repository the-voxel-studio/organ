import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  NotificationResponse,
  SubscribeUrlResponse,
  NotificationMessageResponse
} from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private http = inject(HttpClient);

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
    return this.http.post<NotificationMessageResponse>(`/api/notifications/${uuid}/restore`, {});
  }

  markAsRead(uuid: string): Observable<NotificationMessageResponse> {
    return this.http.patch<NotificationMessageResponse>(`/api/notifications/${uuid}/read`, {});
  }

  deleteNotification(uuid: string): Observable<void> {
    return this.http.delete<void>(`/api/notifications/${uuid}`);
  }
}
