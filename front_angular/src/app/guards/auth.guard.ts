import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/api/auth.service';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Redirection systématique des smartphones vers /get-the-app
  const isMobile = /Android|webOS|iPhone|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  if (isMobile) {
    router.navigate(['/get-the-app']);
    return false;
  }

  if (authService.isAuthenticated()) {
    return true;
  }

  if (localStorage.getItem('organ_has_session') !== 'true') {
    router.navigate(['/login']);
    return of(false);
  }

  return authService.checkSession().pipe(
    map(() => true),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};
