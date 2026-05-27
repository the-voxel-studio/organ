import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  UserMeResponse,
  UpdateUserRequest,
  UpdateUserResponse,
  UserConnection,
  UpdatePasswordRequest,
  LinkGoogleRequest,
  LinkGoogleResponse,
  DeleteUserResponse
} from '../../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);

  getUserMe(): Observable<UserMeResponse> {
    return this.http.get<UserMeResponse>('/api/users/me');
  }

  updateProfile(req: UpdateUserRequest): Observable<UpdateUserResponse> {
    return this.http.put<UpdateUserResponse>('/api/users/me', req);
  }

  getConnections(): Observable<UserConnection[]> {
    return this.http.get<UserConnection[]>('/api/users/me/connections');
  }

  revokeConnection(uuid: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`/api/users/me/connections/${uuid}`);
  }

  updatePassword(req: UpdatePasswordRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>('/api/users/me/password', req);
  }

  linkGoogleAccount(req: LinkGoogleRequest): Observable<LinkGoogleResponse> {
    return this.http.post<LinkGoogleResponse>('/api/users/me/link-google', req);
  }

  deleteAccount(): Observable<DeleteUserResponse> {
    return this.http.delete<DeleteUserResponse>('/api/users/me');
  }
}
