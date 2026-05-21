import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { filter, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
import { DynamicLogoComponent } from '../../dynamic-logo/dynamic-logo';

@Component({
  selector: 'app-landing-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, DynamicLogoComponent],
  templateUrl: './landing-navbar.html',
  styleUrl: './landing-navbar.css'
})
export class LandingNavbarComponent implements OnInit, OnDestroy {
  protected authService = inject(AuthService);
  private router = inject(Router);
  private destroy$ = new Subject<void>();
  
  // Mobile menu open state
  isMobileMenuOpen = signal(false);
  
  // Active route state
  isHome = signal(true);

  links = [
    { id: 'features', label: 'Fonctionnalités' },
    { id: 'collaboration', label: 'Collaboration' },
    { id: 'use-cases', "label": "Cas d'usage" },
    { id: 'about', label: 'Notre Histoire' },
    { id: 'social-proof', label: 'Le mot de la fin' }
  ];

  ngOnInit() {
    // Check if already logged in silently
    this.authService.checkSession().subscribe({
      error: () => {}
    });

    this.updateIsHome();
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateIsHome();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateIsHome() {
    const urlPath = this.router.url.split('#')[0].split('?')[0];
    this.isHome.set(urlPath === '/' || urlPath === '');
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen.update(v => !v);
  }

  closeMobileMenu() {
    this.isMobileMenuOpen.set(false);
  }
}
