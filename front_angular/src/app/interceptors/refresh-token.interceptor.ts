import {
    HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse
} from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/api/auth.service';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

export const refreshTokenInterceptor: HttpInterceptorFn = (req:
    HttpRequest<unknown>, next: HttpHandlerFn) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const handleLogoutAndRedirect = (err: any) => {
        return authService.logout().pipe(
            catchError(() => {
                // Même si le logout API échoue (ex: token expiré), on nettoie l'état local et on redirige
                authService.clearSessionState();
                router.navigate(['/login']);
                return throwError(() => err);
            }),
            switchMap(() => {
                router.navigate(['/login']);
                return throwError(() => err);
            })
        );
    };

    return next(req).pipe(
        catchError((error) => {
            // Si l'API retourne 401 Unauthorized                              
            if (error instanceof HttpErrorResponse && error.status === 401) {

                // Si c'est l'URL de refresh qui échoue avec un 401, on fait le logout et redirige
                if (req.url.includes('/api/auth/refresh')) {
                    return handleLogoutAndRedirect(error);
                }

                // Pour les autres endpoints d'authentification (login, register, logout), pas de refresh
                if (req.url.includes('/api/auth')) {
                    return throwError(() => error);
                }


                // On appelle l'endpoint de refresh                              
                return authService.refreshToken().pipe(
                    switchMap(() => {                                                              
                        // Le navigateur les a mis à jour. On peut rejouer la requête d'origine.                                                                 
                        return next(req);
                    }),
                    catchError((refreshErr) => {
                        // Le refresh a échoué.
                        // Si c'était un 401 sur le refresh, il a déjà été intercepté et géré par le cas ci-dessus.
                        // Sinon (ex: erreur réseau ou 500), on déconnecte au niveau API et on redirige.
                        if (refreshErr instanceof HttpErrorResponse && refreshErr.status === 401) {
                            return throwError(() => refreshErr);
                        }
                        return handleLogoutAndRedirect(refreshErr);
                    })
                );
            }

            return throwError(() => error);
        })
    );
};