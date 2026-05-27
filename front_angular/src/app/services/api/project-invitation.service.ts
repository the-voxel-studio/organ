import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  UserInvitationResponse,
  AcceptInvitationResponse,
  RefuseInvitationResponse
} from '../../models/project-invitation.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectInvitationService {
  private http = inject(HttpClient);

  getInvitations(): Observable<UserInvitationResponse[]> {
    return this.http.get<UserInvitationResponse[]>('/api/invitations');
  }

  acceptInvitation(uuid: string): Observable<AcceptInvitationResponse> {
    return this.http.post<AcceptInvitationResponse>(`/api/invitations/${uuid}/accept`, {});
  }

  refuseInvitation(uuid: string): Observable<RefuseInvitationResponse> {
    return this.http.post<RefuseInvitationResponse>(`/api/invitations/${uuid}/refuse`, {});
  }
}
