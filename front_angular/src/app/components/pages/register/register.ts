import { Component, inject, signal, OnInit, OnDestroy, ViewChild, ElementRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService, GOOGLE_SUPPRESS_KEY } from '../../../services/api/auth.service';
import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    GoogleSigninButtonModule
  ],
  templateUrl: './register.html'
})
export class RegisterComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private socialAuthService = inject(SocialAuthService);
  private router = inject(Router);
  private ngZone = inject(NgZone);
  private fb = inject(FormBuilder);
  private destroy$ = new Subject<void>();

  @ViewChild('googleBtnContainer', { static: false }) googleBtnContainer!: ElementRef;

  // Form Group
  registerForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
    agreement: [false, [Validators.requiredTrue]]
  });

  // État du statut
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);
  showConfirmPassword = signal(false);

  ngOnInit() {
    // Vérifie si déjà authentifié
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // Souscription à la redirection Google Sign-In
    this.socialAuthService.authState
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.ngZone.run(() => {
            // Évite le ré-émetting d'un authState Google expiré après déconnexion
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
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            console.error('Google Auth Error:', err);
            this.isLoading.set(false);
          });
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
    if (!this.registerForm.get('agreement')?.value) {
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
    if (this.registerForm.invalid) {
      if (this.registerForm.get('agreement')?.invalid) {
        this.errorMessage.set("Vous devez accepter les conditions d'utilisation.");
      } else {
        this.errorMessage.set("Veuillez remplir correctement tous les champs.");
      }
      return;
    }
 
    const formVals = this.registerForm.value;
 
    if (formVals.password !== formVals.confirmPassword) {
      this.errorMessage.set("Les deux mots de passe ne correspondent pas.");
      return;
    }
 
    this.isLoading.set(true);
    this.errorMessage.set(null);
 
    this.authService.register({
      firstName: formVals.firstName || '',
      lastName: formVals.lastName || '',
      email: formVals.email || '',
      password: formVals.password || ''
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        // Redirige vers login avec indicateur de succès
        this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 400 && (err.error?.error === 'User already exists' || err.error?.message === 'User already exists')) {
          this.errorMessage.set("Un utilisateur existe déjà avec cette adresse email.");
        } else {
          this.errorMessage.set(err.error?.message || "Une erreur est survenue lors de l'inscription.");
        }
      }
    });
  }
}
