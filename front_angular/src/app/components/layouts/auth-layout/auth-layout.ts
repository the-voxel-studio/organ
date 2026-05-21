import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { DynamicLogoComponent } from '../../dynamic-logo/dynamic-logo';
import { filter, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, DynamicLogoComponent],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.css'
})
export class AuthLayoutComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroy$ = new Subject<void>();

  slogan = signal<string>("L'organisation devient <span class=\"text-bubblegum italic text-balance\">un réflexe.</span>");

  ngOnInit() {
    this.updateSlogan();
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateSlogan();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateSlogan() {
    let currentRoute = this.route.root;
    while (currentRoute.firstChild) {
      currentRoute = currentRoute.firstChild;
    }
    currentRoute.data.pipe(takeUntil(this.destroy$)).subscribe(data => {
      this.slogan.set(data['slogan'] || "L'organisation devient <span class=\"text-bubblegum italic text-balance\">un réflexe.</span>");
    });
  }
}
