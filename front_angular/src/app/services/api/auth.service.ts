import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, switchMap } from 'rxjs';
import { UserMeResponse } from '../../models/user.model';
import { RegisterRequest, RegisterResponse, LoginRequest, LoginResponse, GoogleLoginRequest } from '../../models/auth.model';
import { SocialAuthService } from '@abacritt/angularx-social-login';
import { UserStore } from '../common/user.store';

// Clé pour signaler après loggout que l'auth Google soit ignoré (éviter une reconnexion instantané)
export const GOOGLE_SUPPRESS_KEY = 'google_suppress_autoselect';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private http = inject(HttpClient);
    private socialAuthService = inject(SocialAuthService);
    private userStore = inject(UserStore);

    // Délégation de l'état réactif au UserStore
    currentUser = this.userStore.currentUser;
    isAuthenticated = this.userStore.isAuthenticated;

    // Appel au démarrage pour vérifier la session                         
    checkSession(): Observable<UserMeResponse> {
        return this.http.get<UserMeResponse>('/api/users/me').pipe(
            tap({
                next: (user) => {
                    localStorage.setItem('organ_has_session', 'true');
                    this.userStore.setUser(user);
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

    login(req: LoginRequest): Observable<UserMeResponse> {
        return this.http.post<LoginResponse>('/api/auth/login', req).pipe(
            switchMap(() => this.checkSession())
        );
    }

    loginWithGoogle(req: GoogleLoginRequest): Observable<UserMeResponse> {
        return this.http.post<LoginResponse>('/api/auth/login/google', req).pipe(
            switchMap(() => this.checkSession())
        );
    }

    refreshToken(): Observable<void> {
        // Le refresh_token est envoyé automatiquement par le navigateur grâce au cookie httpOnly
        return this.http.post<void>('/api/auth/refresh', {});
    }

    clearSessionState(): void {
        localStorage.removeItem('organ_has_session');
        this.userStore.clear();
    }

    logout(): Observable<void> {
        return this.http.post<void>('/api/auth/logout', {}).pipe(
            tap(() => {
                this.clearSessionState();
                // Après Loggout, pas de reconnexion Google
                sessionStorage.setItem(GOOGLE_SUPPRESS_KEY, '1');
                try {
                    const google = (window as any).google;
                    if (google?.accounts?.id) {
                        google.accounts.id.disableAutoSelect();
                        // Annule la session
                        google.accounts.id.cancel();
                    }
                } catch (_) {}
                // Signaler le loggout
                this.socialAuthService.signOut(true).catch(() => {});
            })
        );
    }
}