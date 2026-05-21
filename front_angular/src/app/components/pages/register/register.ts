import { Component, inject, signal, OnInit, OnDestroy, ViewChild, ElementRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService, GOOGLE_SUPPRESS_KEY } from '../../../services/auth.service';
import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    GoogleSigninButtonModule
  ],
  templateUrl: './register.html'
})
export class RegisterComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private socialAuthService = inject(SocialAuthService);
  private router = inject(Router);
  private ngZone = inject(NgZone);
  private destroy$ = new Subject<void>();

  @ViewChild('googleBtnContainer', { static: false }) googleBtnContainer!: ElementRef;

  // Form Fields
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  confirmPassword = '';
  agreement = false;

  // Status State
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);
  showConfirmPassword = signal(false);

  ngOnInit() {
    // Check if already authenticated
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // Google Sign-In redirect state subscription
    this.socialAuthService.authState
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          // If the user just logged out, suppress the stale Google authState re-emit
          if (sessionStorage.getItem(GOOGLE_SUPPRESS_KEY)) {
            sessionStorage.removeItem(GOOGLE_SUPPRESS_KEY);
            return;
          }

          if (user) {
            this.isLoading.set(true);
            this.errorMessage.set(null);
            
            this.authService.loginWithGoogle({ token: user.idToken, idToken: user.idToken }).subscribe({
              next: () => {
                this.isLoading.set(false);
                this.router.navigate(['/dashboard']);
              },
              error: (err) => {
                this.isLoading.set(false);
                this.errorMessage.set(err.error?.message || "Une erreur est survenue lors de la connexion Google.");
              }
            });
          } else {
            this.isLoading.set(false);
          }
        },
        error: (err) => {
          console.error('Google Auth Error:', err);
          this.isLoading.set(false);
        }
      });
  }

  // ── Google popup helpers ────────────────────────────────────────────────

  private watchForGooglePopupClose() {
    const focusHandler = () => {
      setTimeout(() => {
        this.ngZone.run(() => {
          if (this.isLoading()) {
            this.isLoading.set(false);
          }
        });
        window.removeEventListener('focus', focusHandler);
      }, 1500);
    };
    setTimeout(() => {
      window.addEventListener('focus', focusHandler);
    }, 800);
  }

  triggerGoogleLogin() {
    if (!this.agreement) {
      this.errorMessage.set("Vous devez accepter les conditions d'utilisation.");
      return;
    }

    const nativeEl = this.googleBtnContainer.nativeElement as HTMLElement;
    const googleButton = nativeEl.querySelector('div[role="button"]') || nativeEl.querySelector('iframe');

    if (googleButton) {
      this.isLoading.set(true);
      this.errorMessage.set(null);
      (googleButton as HTMLElement).click();
      this.watchForGooglePopupClose();
    } else {
      const google = (window as any).google;
      if (google?.accounts?.id) {
        this.isLoading.set(true);
        this.errorMessage.set(null);
        google.accounts.id.prompt((notification: any) => {
          this.ngZone.run(() => {
            if (notification.isNotDisplayed() || notification.isSkippedMoment() || notification.isDismissedMoment()) {
              this.isLoading.set(false);
            }
          });
        });
      } else {
        this.errorMessage.set("Le service de connexion Google n'est pas encore disponible. Veuillez réessayer.");
      }
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  togglePasswordVisibility() {
    this.showPassword.update(v => !v);
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword.update(v => !v);
  }

  onSubmit() {
    if (!this.agreement) {
      this.errorMessage.set("Vous devez accepter les conditions d'utilisation.");
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage.set("Les deux mots de passe ne correspondent pas.");
      return;
    }

    if (this.password.length < 8) {
      this.errorMessage.set("Le mot de passe doit faire au moins 8 caractères.");
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.register({
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        // Navigate to login with success indicator
        this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || "Une erreur est survenue lors de l'inscription.");
      }
    });
  }
}
