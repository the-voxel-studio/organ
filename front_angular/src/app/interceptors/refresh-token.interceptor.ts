import {
    HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse
} from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';

export const refreshTokenInterceptor: HttpInterceptorFn = (req:
    HttpRequest<unknown>, next: HttpHandlerFn) => {
    const authService = inject(AuthService);

    return next(req).pipe(
        catchError((error) => {
            // Si l'API retourne 401 Unauthorized                              
            if (error instanceof HttpErrorResponse && error.status === 401) {

                // Sécurité pour éviter de boucler si la requête de refresh elle-même échoue                                                                
                if (req.url.includes('/api/auth/refresh')) {
                    authService.clearSessionState();
                    return throwError(() => error);
                }

                // On appelle l'endpoint de refresh                              
                return authService.refreshToken().pipe(
                    switchMap(() => {                                                              
                        // Le navigateur les a mis à jour. On peut rejouer la requête d'origine.                                                                 
                        return next(req);
                    }),
                    catchError((refreshErr) => {
                        // Le refresh a échoué (refresh token expiré). On déconnecte l'utilisateur.                                                             
                        authService.clearSessionState();
                        return throwError(() => refreshErr);
                    })
                );
            }

            return throwError(() => error);
        })
    );
};     