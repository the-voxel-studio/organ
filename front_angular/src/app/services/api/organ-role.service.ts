import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OrganRoleDefinition,
  TrashedOrganRoleSummary,
  CreateOrganRoleRequest,
  CreateOrganRoleResponse,
  UpdateOrganRoleRequest,
  AssignRoleRequest,
  AssignRoleResponse,
  TrashedRoleMember,
  RestoreRoleMemberResponse,
  RestoreOrganRoleResponse
} from '../../models/organ-role.model';
import { OrganMember } from '../../models/organ.model';

@Injectable({
  providedIn: 'root'
})
export class OrganRoleService {
  private http = inject(HttpClient);

  getRoles(projectUuid: string, organUuid: string): Observable<OrganRoleDefinition[]> {
    return this.http.get<OrganRoleDefinition[]>(`/api/projects/${projectUuid}/organs/${organUuid}/roles`);
  }

  getRoleMembers(projectUuid: string, organUuid: string, roleUuid: string): Observable<OrganMember[]> {
    return this.http.get<OrganMember[]>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/${roleUuid}/members`);
  }

  getTrashedRoles(projectUuid: string, organUuid: string): Observable<TrashedOrganRoleSummary[]> {
    return this.http.get<TrashedOrganRoleSummary[]>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/trash`);
  }

  createRole(projectUuid: string, organUuid: string, req: CreateOrganRoleRequest): Observable<CreateOrganRoleResponse> {
    return this.http.post<CreateOrganRoleResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/roles`, req);
  }

  updateRole(projectUuid: string, organUuid: string, roleUuid: string, req: UpdateOrganRoleRequest): Observable<OrganRoleDefinition> {
    return this.http.put<OrganRoleDefinition>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/${roleUuid}`, req);
  }

  assignRole(projectUuid: string, organUuid: string, roleUuid: string, req: AssignRoleRequest): Observable<AssignRoleResponse> {
    return this.http.post<AssignRoleResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/${roleUuid}/assign`, req);
  }

  getTrashedMembers(projectUuid: string, organUuid: string): Observable<TrashedRoleMember[]> {
    return this.http.get<TrashedRoleMember[]>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/members/trash`);
  }

  restoreMember(projectUuid: string, organUuid: string, uorId: number): Observable<RestoreRoleMemberResponse> {
    return this.http.post<RestoreRoleMemberResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/members/${uorId}/restore`, {});
  }

  unassignRole(projectUuid: string, organUuid: string, roleUuid: string, userUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/${roleUuid}/unassign/${userUuid}`, {
      params: { permanent: String(permanent) }
    });
  }

  deleteRole(projectUuid: string, organUuid: string, roleUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/${roleUuid}`, {
      params: { permanent: String(permanent) }
    });
  }

  restoreRole(projectUuid: string, organUuid: string, roleUuid: string): Observable<RestoreOrganRoleResponse> {
    return this.http.post<RestoreOrganRoleResponse>(`/api/projects/${projectUuid}/organs/${organUuid}/roles/${roleUuid}/restore`, {});
  }
}
