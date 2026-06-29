import { Component, inject, signal, OnInit, OnDestroy, ViewChild, ElementRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService, GOOGLE_SUPPRESS_KEY } from '../../../services/api/auth.service';
import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    GoogleSigninButtonModule
  ],
  templateUrl: './login.html'
})
export class LoginComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private socialAuthService = inject(SocialAuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private ngZone = inject(NgZone);
  private fb = inject(FormBuilder);
  private destroy$ = new Subject<void>();
  private popupPollTimer: ReturnType<typeof setInterval> | null = null;

  @ViewChild('googleBtnContainer', { static: false }) googleBtnContainer!: ElementRef;

  // Formulaire réactif
  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    agreement: [false, [Validators.requiredTrue]]
  });

  // État
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showPassword = signal(false);

  ngOnInit() {
    // Redirige si déjà connecté
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // Vérifie si l'utilisateur vient de s'inscrire
    const registered = this.route.snapshot.queryParams['registered'];
    if (registered === 'true') {
      this.successMessage.set("Inscription réussie ! Vous pouvez maintenant vous connecter.");
    }

    // Souscrit aux changements d'état Google Auth
    this.socialAuthService.authState
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.ngZone.run(() => {
            // Évite la reconnexion auto Google après déconnexion
            if (sessionStorage.getItem(GOOGLE_SUPPRESS_KEY)) {
              sessionStorage.removeItem(GOOGLE_SUPPRESS_KEY);
              return;
            }

            if (user) {
              this.isLoading.set(true);
              this.errorMessage.set(null);
              this.successMessage.set(null);
              this.stopPopupPoll();
              
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
              this.stopPopupPoll();
            }
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            console.error('Google Auth Error:', err);
            this.isLoading.set(false);
            this.stopPopupPoll();
          });
        }
      });
  }

  // Helpers pour la popup Google

  /** Vérifie la fermeture de la popup toutes les 300ms */
  private startPopupPoll(popup: Window | null) {
    this.stopPopupPoll();
    if (!popup) return;
    this.popupPollTimer = setInterval(() => {
      if (popup.closed) {
        this.ngZone.run(() => {
          this.isLoading.set(false);
          this.stopPopupPoll();
        });
      }
    }, 300);
  }

  private stopPopupPoll() {
    if (this.popupPollTimer !== null) {
      clearInterval(this.popupPollTimer);
      this.popupPollTimer = null;
    }
  }

  triggerGoogleLogin() {
    if (!this.loginForm.get('agreement')?.value) {
      this.errorMessage.set("Vous devez accepter les conditions d'utilisation.");
      return;
    }

    const nativeEl = this.googleBtnContainer.nativeElement as HTMLElement;
    const googleButton = nativeEl.querySelector('div[role="button"]') || nativeEl.querySelector('iframe');

    if (googleButton) {
      this.isLoading.set(true);
      this.errorMessage.set(null);
      (googleButton as HTMLElement).click();
      // Watch de la fermeture de la popup
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

  /** Détecte quand l'utilisateur ferme la popup sans se connecter */
  private watchForGooglePopupClose() {
    // Laisse 800ms à Google pour ouvrir la popup
    const focusHandler = () => {
      // Retour du focus sur la fenêtre principale = popup fermée
      // Laisse 1.5s à l'authentification pour se lancer
      setTimeout(() => {
        this.ngZone.run(() => {
          if (this.isLoading()) {
            this.isLoading.set(false);
          }
        });
        window.removeEventListener('focus', focusHandler);
      }, 1500);
    };

    // Délai pour éviter de capter le focus initial
    setTimeout(() => {
      window.addEventListener('focus', focusHandler);
    }, 800);
  }

  ngOnDestroy() {
    this.stopPopupPoll();
    this.destroy$.next();
    this.destroy$.complete();
  }

  togglePasswordVisibility() {
    this.showPassword.update(v => !v);
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      if (this.loginForm.get('agreement')?.invalid) {
        this.errorMessage.set("Vous devez accepter les conditions d'utilisation.");
      } else {
        this.errorMessage.set("Veuillez remplir correctement tous les champs.");
      }
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const emailVal = this.loginForm.value.email || '';
    const passwordVal = this.loginForm.value.password || '';

    this.authService.login({
      username: emailVal,
      password: passwordVal
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || "Identifiants invalides.");
      }
    });
  }
}
