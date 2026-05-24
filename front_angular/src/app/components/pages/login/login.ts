import { Component, inject, signal, OnInit, OnDestroy, ViewChild, ElementRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService, GOOGLE_SUPPRESS_KEY } from '../../../services/auth.service';
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

  // Reactive Form
  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    agreement: [false, [Validators.requiredTrue]]
  });

  // State
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showPassword = signal(false);

  ngOnInit() {
    // Check if already authenticated
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // Check for registered query parameter
    const registered = this.route.snapshot.queryParams['registered'];
    if (registered === 'true') {
      this.successMessage.set("Inscription réussie ! Vous pouvez maintenant vous connecter.");
    }

    // Subscribe to Google auth state changes
    this.socialAuthService.authState
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.ngZone.run(() => {
            // If the user just logged out, suppress the stale Google authState re-emit
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

  // ── Google popup helpers ────────────────────────────────────────────────

  /** Poll for popup closure every 300 ms and reset loading state when it closes. */
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
      // Poll for popup window closure — no stale 30s timeout
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

  /**
   * After the Google button is clicked, poll the open windows list to detect
   * when the user dismisses the Google popup without signing in.
   * We check window.open references indirectly by watching a brief delay then
   * looking for a focused state change — the simplest reliable approach is to
   * watch for the main window to regain focus (Google popup closed).
   */
  private watchForGooglePopupClose() {
    // Give Google SDK 800 ms to open its popup before we start watching
    const focusHandler = () => {
      // Main window regained focus → popup was closed
      // Give authState 1.5 s to fire; if it doesn't, reset loading
      setTimeout(() => {
        this.ngZone.run(() => {
          if (this.isLoading()) {
            this.isLoading.set(false);
          }
        });
        window.removeEventListener('focus', focusHandler);
      }, 1500);
    };

    // Small delay so the window doesn't immediately detect its own refocus
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
