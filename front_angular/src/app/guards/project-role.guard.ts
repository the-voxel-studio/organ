import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { ProjectService } from '../services/api/project.service';
import { ToastService } from '../services/common/toast.service';
import { catchError, map, of } from 'rxjs';

export const projectRoleGuard: CanActivateFn = (route) => {
  const projectService = inject(ProjectService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  const projectUuid = route.paramMap.get('projectUuid');
  const allowedRoles = (route.data['roles'] as string[]) || ['ADMIN', 'MANAGER'];

  if (!projectUuid) {
    return of(true);
  }

  return projectService.getProjectDetailed(projectUuid).pipe(
    map((projData) => {
      const role = projData?.project?.role;
      if (role && allowedRoles.includes(role)) {
        return true;
      }
      
      toastService.error('Accès refusé', "Vous n'avez pas l'autorisation requise pour accéder à cette page.");
      router.navigate(['/project', projectUuid]);
      return false;
    }),
    catchError((err) => {
      console.error('Project Role Guard error:', err);
      toastService.error('Accès refusé', 'Projet non trouvé ou accès refusé.');
      router.navigate(['/dashboard']);
      return of(false);
    })
  );
};
