import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { OrganService } from '../services/organ.service';
import { ProjectService } from '../services/project.service';
import { ToastService } from '../services/toast.service';
import { catchError, map, of, switchMap } from 'rxjs';

export const organPermissionGuard: CanActivateFn = (route) => {
  const organService = inject(OrganService);
  const projectService = inject(ProjectService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  const projectUuid = route.paramMap.get('projectUuid');
  const organUuid = route.paramMap.get('organUuid');
  const requiredPermission = route.data['permission'] as string;

  if (!projectUuid || !organUuid) {
    return of(true);
  }

  return projectService.getProjectDetailed(projectUuid).pipe(
    switchMap((projData) => {
      const role = projData?.project?.role;
      if (role === 'ADMIN' || role === 'MANAGER') {
        return of(true);
      }
      return organService.getOrganPermissions(projectUuid, organUuid).pipe(
        map((res) => {
          const perms = res.permissions || [];
          if (perms.includes('ALL') || perms.includes(requiredPermission)) {
            return true;
          }
          toastService.error('Accès refusé', "Vous n'avez pas la permission requise pour accéder à cette page.");
          router.navigate(['/organ', projectUuid, organUuid]);
          return false;
        })
      );
    }),
    catchError((err) => {
      console.error('Permission Guard error:', err);
      toastService.error('Accès refusé', 'Projet non trouvé ou accès refusé.');
      router.navigate(['/dashboard']);
      return of(false);
    })
  );
};
