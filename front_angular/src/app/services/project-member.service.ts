import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProjectMemberSummary,
  InviteMemberRequest,
  InviteMemberResponse,
  UpdateMemberRequest,
  UpdateMemberResponse,
  RestoreMemberResponse
} from '../models/project-member.model';
import { ProjectInvitationSummary } from '../models/project-invitation.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectMemberService {
  private http = inject(HttpClient);

  getMembers(projectUuid: string): Observable<ProjectMemberSummary[]> {
    return this.http.get<ProjectMemberSummary[]>(`/api/projects/${projectUuid}/members`);
  }

  getTrashedMembers(projectUuid: string): Observable<ProjectMemberSummary[]> {
    return this.http.get<ProjectMemberSummary[]>(`/api/projects/${projectUuid}/members/trash`);
  }

  getInvitations(projectUuid: string): Observable<ProjectInvitationSummary[]> {
    return this.http.get<ProjectInvitationSummary[]>(`/api/projects/${projectUuid}/members/invitations`);
  }

  inviteMember(projectUuid: string, req: InviteMemberRequest): Observable<InviteMemberResponse> {
    return this.http.post<InviteMemberResponse>(`/api/projects/${projectUuid}/members/invite`, req);
  }

  updateMemberRole(projectUuid: string, memberUuid: string, req: UpdateMemberRequest): Observable<UpdateMemberResponse> {
    return this.http.put<UpdateMemberResponse>(`/api/projects/${projectUuid}/members/${memberUuid}`, req);
  }

  removeMember(projectUuid: string, memberUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/members/${memberUuid}`, {
      params: { permanent: String(permanent) }
    });
  }

  restoreMember(projectUuid: string, memberUuid: string): Observable<RestoreMemberResponse> {
    return this.http.post<RestoreMemberResponse>(`/api/projects/${projectUuid}/members/${memberUuid}/restore`, {});
  }
}
