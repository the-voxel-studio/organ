import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProjectDriveConfigResponse,
  ConnectGoogleRequest,
  ConnectGoogleResponse,
  CreateFolderResponse,
  GoogleDriveFolderInfo,
  SelectFolderRequest,
  SelectFolderResponse,
  UpdateDriveConfigRequest,
  UpdateDriveConfigResponse
} from '../models/project-drive.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectDriveService {
  private http = inject(HttpClient);

  getConfig(projectUuid: string): Observable<ProjectDriveConfigResponse> {
    return this.http.get<ProjectDriveConfigResponse>(`/api/projects/${projectUuid}/drive-config`);
  }

  connectGoogle(projectUuid: string, req: ConnectGoogleRequest): Observable<ConnectGoogleResponse> {
    return this.http.post<ConnectGoogleResponse>(`/api/projects/${projectUuid}/drive-config/connect-google`, req);
  }

  createFolder(projectUuid: string): Observable<CreateFolderResponse> {
    return this.http.post<CreateFolderResponse>(`/api/projects/${projectUuid}/drive-config/create-folder`, {});
  }

  listFolders(projectUuid: string): Observable<GoogleDriveFolderInfo[]> {
    return this.http.get<GoogleDriveFolderInfo[]>(`/api/projects/${projectUuid}/drive-config/list-folders`);
  }

  selectFolder(projectUuid: string, req: SelectFolderRequest): Observable<SelectFolderResponse> {
    return this.http.post<SelectFolderResponse>(`/api/projects/${projectUuid}/drive-config/select-folder`, req);
  }

  updateConfig(projectUuid: string, req: UpdateDriveConfigRequest): Observable<UpdateDriveConfigResponse> {
    return this.http.put<UpdateDriveConfigResponse>(`/api/projects/${projectUuid}/drive-config`, req);
  }

  deleteConfig(projectUuid: string): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectUuid}/drive-config`);
  }
}
