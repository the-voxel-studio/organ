import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OrganLinkSummary,
  TrashedOrganLinkSummary,
  CreateOrganLinkRequest,
  UpdateOrganLinkRequest
} from '../models/organ-link.model';

@Injectable({
  providedIn: 'root'
})
export class OrganLinkService {
  private http = inject(HttpClient);

  getLinks(projectUuid: string, organUuid: string): Observable<OrganLinkSummary[]> {
    return this.http.get<OrganLinkSummary[]>(`/api/projects/${projectUuid}/organs/${organUuid}/links`);
  }

  getTrashedLinks(projectUuid: string, organUuid: string): Observable<TrashedOrganLinkSummary[]> {
    return this.http.get<TrashedOrganLinkSummary[]>(`/api/projects/${projectUuid}/organs/${organUuid}/links/trash`);
  }

  createLink(projectUuid: string, organUuid: string, req: CreateOrganLinkRequest): Observable<OrganLinkSummary> {
    return this.http.post<OrganLinkSummary>(`/api/projects/${projectUuid}/organs/${organUuid}/links`, req);
  }

  updateLink(projectUuid: string, organUuid: string, linkUuid: string, req: UpdateOrganLinkRequest): Observable<OrganLinkSummary> {
    return this.http.put<OrganLinkSummary>(`/api/projects/${projectUuid}/organs/${organUuid}/links/${linkUuid}`, req);
  }

  restoreLink(projectUuid: string, organUuid: string, linkUuid: string): Observable<OrganLinkSummary> {
    return this.http.post<OrganLinkSummary>(`/api/projects/${projectUuid}/organs/${organUuid}/links/${linkUuid}/restore`, {});
  }

  deleteLink(projectUuid: string, organUuid: string, linkUuid: string, permanent: boolean = false): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/organs/${organUuid}/links/${linkUuid}`, {
      params: { permanent: String(permanent) }
    });
  }
}
