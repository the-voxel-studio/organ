import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { UserMeResponse } from '../models/user.model';
import { RegisterRequest, RegisterResponse, LoginRequest, LoginResponse, GoogleLoginRequest } from '../models/auth.model';
import { SocialAuthService } from '@abacritt/angularx-social-login';

// Key used to signal that we just logged out and Google's authState should be ignored
export const GOOGLE_SUPPRESS_KEY = 'google_suppress_autoselect';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private http = inject(HttpClient);
    private socialAuthService = inject(SocialAuthService);

    // Signal réactif contenant l'utilisateur connecté ou null             
    currentUser = signal<UserMeResponse | null>(null);

    // Signal dérivé pour savoir s'il est authentifié                      
    isAuthenticated = signal<boolean>(false);

    // Appel au démarrage pour vérifier la session                         
    checkSession(): Observable<UserMeResponse> {
        return this.http.get<UserMeResponse>('/api/users/me').pipe(
            tap({
                next: (user) => {
                    this.currentUser.set(user);
                    this.isAuthenticated.set(true);
                },
                error: () => {
                    this.clearSessionState();
                }
            })
        );
    }

    register(req: RegisterRequest): Observable<RegisterResponse> {
        return this.http.post<RegisterResponse>('/api/auth/register', req);
    }

    login(req: LoginRequest): Observable<LoginResponse> {
        return this.http.post<LoginResponse>('/api/auth/login', req).pipe(
            tap(() => {
                this.checkSession().subscribe();
            })
        );
    }

    loginWithGoogle(req: GoogleLoginRequest): Observable<LoginResponse> {
        return this.http.post<LoginResponse>('/api/auth/login/google', req).pipe(
            tap(() => {
                this.checkSession().subscribe();
            })
        );
    }

    refreshToken(): Observable<void> {
        // Le refresh_token est envoyé automatiquement par le navigateur grâce au cookie httpOnly
        return this.http.post<void>('/api/auth/refresh', {});
    }

    clearSessionState(): void {
        this.currentUser.set(null);
        this.isAuthenticated.set(false);
    }

    logout(): Observable<void> {
        return this.http.post<void>('/api/auth/logout', {}).pipe(
            tap(() => {
                this.clearSessionState();
                // Prevent Google from auto-logging back in after explicit logout
                sessionStorage.setItem(GOOGLE_SUPPRESS_KEY, '1');
                try {
                    const google = (window as any).google;
                    if (google?.accounts?.id) {
                        google.accounts.id.disableAutoSelect();
                        // Revoke the session hint so One-Tap won't re-fire
                        google.accounts.id.cancel();
                    }
                } catch (_) {}
                // Sign out from the Angular Social Auth service too
                this.socialAuthService.signOut(true).catch(() => {});
            })
        );
    }
}