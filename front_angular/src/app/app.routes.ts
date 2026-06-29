import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { organPermissionGuard } from './guards/organ-permission.guard';
import { projectRoleGuard } from './guards/project-role.guard';

export const routes: Routes = [
  // Routes publiques (LandingLayoutComponent)
  {
    path: '',
    loadComponent: () => import('./components/layouts/landing-layout/landing-layout').then(m => m.LandingLayoutComponent),
    children: [
      { 
        path: '', 
        loadComponent: () => import('./components/landing/landing-page/landing-page').then(m => m.LandingPageComponent) 
      },
      { 
        path: 'get-the-app', 
        loadComponent: () => import('./components/pages/get-the-app/get-the-app').then(m => m.GetTheAppComponent) 
      },
      { 
        path: 'legal-notice', 
        loadComponent: () => import('./components/pages/legal-notice/legal-notice').then(m => m.LegalNoticeComponent) 
      },
      { 
        path: 'privacy-policy', 
        loadComponent: () => import('./components/pages/privacy-policy/privacy-policy').then(m => m.PrivacyPolicyComponent) 
      },
      { 
        path: 'terms', 
        loadComponent: () => import('./components/pages/terms/terms').then(m => m.TermsComponent) 
      }
    ]
  },

  // Routes d'authentification (AuthLayoutComponent)
  {
    path: '',
    loadComponent: () => import('./components/layouts/auth-layout/auth-layout').then(m => m.AuthLayoutComponent),
    children: [
      { 
        path: 'login', 
        loadComponent: () => import('./components/pages/login/login').then(m => m.LoginComponent), 
        data: { slogan: "L'organisation devient <span class=\"text-bubblegum italic text-balance\">un réflexe.</span>" } 
      },
      { 
        path: 'register', 
        loadComponent: () => import('./components/pages/register/register').then(m => m.RegisterComponent), 
        data: { slogan: "Reprenez le contrôle <span class=\"text-bubblegum italic text-balance\">de votre temps.</span>" } 
      }
    ]
  },
  
  // Routes protégées avec NavbarLayout (aucun menu ou sidebar)
  {
    path: '',
    loadComponent: () => import('./components/layouts/navbar-layout/navbar-layout').then(m => m.NavbarLayoutComponent),
    canActivate: [authGuard],
    children: [
      { 
        path: 'project/new', 
        loadComponent: () => import('./components/pages/project-create/project-create').then(m => m.ProjectCreateComponent) 
      },
      { 
        path: 'project/:projectUuid/settings', 
        loadComponent: () => import('./components/pages/project-settings/project-settings').then(m => m.ProjectSettingsComponent),
        canActivate: [projectRoleGuard],
        data: { roles: ['ADMIN'] }
      },
      { 
        path: 'project/:projectUuid/organ/new', 
        loadComponent: () => import('./components/pages/organ-create/organ-create').then(m => m.OrganCreateComponent) 
      },
      { 
        path: 'project/:projectUuid/organ/:organUuid/settings', 
        loadComponent: () => import('./components/pages/organ-settings/organ-settings').then(m => m.OrganSettingsComponent),
        canActivate: [organPermissionGuard],
        data: { permission: 'ORGAN_EDIT' }
      }
    ]
  },

  // Routes protégées avec Sidebar (LoggedLayoutComponent)
  {
    path: '',
    loadComponent: () => import('./components/layouts/logged-layout/logged-layout').then(m => m.LoggedLayoutComponent),
    canActivate: [authGuard],
    children: [
      { 
        path: 'dashboard', 
        loadComponent: () => import('./components/pages/dashboard/dashboard').then(m => m.DashboardComponent) 
      },
      { 
        path: 'project/:projectUuid', 
        loadComponent: () => import('./components/pages/project/project').then(m => m.ProjectComponent) 
      },
      { 
        path: 'project/:projectUuid/trash', 
        loadComponent: () => import('./components/pages/project-trash/project-trash').then(m => m.ProjectTrashComponent),
        canActivate: [projectRoleGuard],
        data: { roles: ['ADMIN', 'MANAGER'] }
      },
      {
        path: 'project/:projectUuid/analytics',
        loadComponent: () => import('./components/pages/project-analytics/project-analytics').then(m => m.ProjectAnalyticsComponent),
        canActivate: [projectRoleGuard],
        data: { roles: ['ADMIN', 'MANAGER'] }
      },
      { 
        path: 'organ/:projectUuid/:organUuid', 
        loadComponent: () => import('./components/pages/organ/organ').then(m => m.OrganComponent) 
      },
      { 
        path: 'organ/:projectUuid/:organUuid/trash', 
        loadComponent: () => import('./components/pages/organ-trash/organ-trash').then(m => m.OrganTrashComponent) 
      },
      { 
        path: 'settings', 
        loadComponent: () => import('./components/pages/settings/settings').then(m => m.SettingsComponent) 
      },
      { 
        path: 'trash', 
        loadComponent: () => import('./components/pages/trash/trash').then(m => m.TrashComponent) 
      }
    ]
  },

  // Redirection par défaut vers la Landing Page
  { path: '**', redirectTo: '' }
];
