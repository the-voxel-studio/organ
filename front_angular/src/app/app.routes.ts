import { Routes } from '@angular/router';
import { LandingLayoutComponent } from './components/layouts/landing-layout/landing-layout';
import { LoggedLayoutComponent } from './components/layouts/logged-layout/logged-layout';
import { AuthLayoutComponent } from './components/layouts/auth-layout/auth-layout';
import { NavbarLayoutComponent } from './components/layouts/navbar-layout/navbar-layout';
import { LandingPageComponent } from './components/landing/landing-page/landing-page';
import { LoginComponent } from './components/pages/login/login';
import { RegisterComponent } from './components/pages/register/register';
import { GetTheAppComponent } from './components/pages/get-the-app/get-the-app';
import { DashboardComponent } from './components/pages/dashboard/dashboard';
import { ProjectComponent } from './components/pages/project/project';
import { ProjectEditComponent } from './components/pages/project-edit/project-edit';
import { ProjectTrashComponent } from './components/pages/project-trash/project-trash';
import { OrganComponent } from './components/pages/organ/organ';
import { OrganEditComponent } from './components/pages/organ-edit/organ-edit';
import { OrganTrashComponent } from './components/pages/organ/components/organ-trash/organ-trash';
import { SettingsComponent } from './components/pages/settings/settings';
import { TrashComponent } from './components/pages/trash/trash';
import { LegalNoticeComponent } from './components/pages/legal/legal-notice/legal-notice';
import { PrivacyPolicyComponent } from './components/pages/legal/privacy-policy/privacy-policy';
import { TermsComponent } from './components/pages/legal/terms/terms';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  // Routes publiques (LandingLayoutComponent)
  {
    path: '',
    component: LandingLayoutComponent,
    children: [
      { path: '', component: LandingPageComponent },
      { path: 'get-the-app', component: GetTheAppComponent },
      { path: 'legal-notice', component: LegalNoticeComponent },
      { path: 'privacy-policy', component: PrivacyPolicyComponent },
      { path: 'terms', component: TermsComponent }
    ]
  },

  // Routes d'authentification (AuthLayoutComponent)
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      { 
        path: 'login', 
        component: LoginComponent, 
        data: { slogan: "L'organisation devient <span class=\"text-bubblegum italic text-balance\">un réflexe.</span>" } 
      },
      { 
        path: 'register', 
        component: RegisterComponent, 
        data: { slogan: "Reprenez le contrôle <span class=\"text-bubblegum italic text-balance\">de votre temps.</span>" } 
      }
    ]
  },
  
  // Routes protégées avec NavbarLayout (aucun menu ou sidebar)
  {
    path: '',
    component: NavbarLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'project/new', component: ProjectEditComponent },
      { path: 'project/:uuid/settings', component: ProjectEditComponent },
      { path: 'project/:projectUuid/organ/new', component: OrganEditComponent },
      { path: 'project/:projectUuid/organ/:organUuid/settings', component: OrganEditComponent }
    ]
  },

  // Routes protégées avec Sidebar (LoggedLayoutComponent)
  {
    path: '',
    component: LoggedLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'project/:uuid', component: ProjectComponent },
      { path: 'project/:uuid/trash', component: ProjectTrashComponent },
      { path: 'organ/:projectUuid/:organUuid', component: OrganComponent },
      { path: 'organ/:projectUuid/:organUuid/trash', component: OrganTrashComponent },
      { path: 'settings', component: SettingsComponent },
      { path: 'trash', component: TrashComponent }
    ]
  },

  // Redirection par défaut vers la Landing Page
  { path: '**', redirectTo: '' }
];
