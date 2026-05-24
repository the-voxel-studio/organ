import { Component, OnInit, OnDestroy, ViewChild, ElementRef, inject, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { AuthService } from '../../../../../services/auth.service';
import { UserService } from '../../../../../services/user.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-settings-google-link',
  standalone: true,
  imports: [CommonModule, GoogleSigninButtonModule],
  templateUrl: './settings-google-link.html'
})
export class SettingsGoogleLinkComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private socialAuthService = inject(SocialAuthService);
  private ngZone = inject(NgZone);
  private destroy$ = new Subject<void>();

  @ViewChild('googleBtnContainer', { static: false }) googleBtnContainer!: ElementRef;

  currentUser = this.authService.currentUser;

  isGoogleLinking = false;
  googleSuccess: string | null = null;
  googleError: string | null = null;

  ngOnInit() {
    this.socialAuthService.authState
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.ngZone.run(() => {
            if (this.isGoogleLinking && user && user.idToken) {
              this.handleGoogleLink(user.idToken);
            }
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            if (this.isGoogleLinking) {
              console.error('Google Auth Error:', err);
              this.googleError = "Une erreur est survenue lors de l'authentification Google.";
              this.isGoogleLinking = false;
            }
          });
        }
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  triggerGoogleLink() {
    const nativeEl = this.googleBtnContainer.nativeElement as HTMLElement;
    const googleButton = nativeEl.querySelector('div[role="button"]') || nativeEl.querySelector('iframe');

    if (googleButton) {
      this.isGoogleLinking = true;
      this.googleError = null;
      this.googleSuccess = null;
      (googleButton as HTMLElement).click();
      this.watchForGooglePopupClose();
    } else {
      const google = (window as any).google;
      if (google?.accounts?.id) {
        this.isGoogleLinking = true;
        this.googleError = null;
        this.googleSuccess = null;
        google.accounts.id.prompt((notification: any) => {
          this.ngZone.run(() => {
            if (notification.isNotDisplayed() || notification.isSkippedMoment() || notification.isDismissedMoment()) {
              this.isGoogleLinking = false;
            }
          });
        });
      } else {
        this.googleError = "Le service de connexion Google n'est pas encore disponible. Veuillez réessayer.";
      }
    }
  }

  private watchForGooglePopupClose() {
    const focusHandler = () => {
      setTimeout(() => {
        this.ngZone.run(() => {
          if (this.isGoogleLinking) {
            this.isGoogleLinking = false;
          }
        });
        window.removeEventListener('focus', focusHandler);
      }, 1500);
    };

    setTimeout(() => {
      window.addEventListener('focus', focusHandler);
    }, 800);
  }

  private handleGoogleLink(idToken: string) {
    this.userService.linkGoogleAccount({ token: idToken, idToken: idToken }).subscribe({
      next: (res) => {
        this.googleSuccess = "Compte lié avec succès.";
        this.authService.checkSession().subscribe({
          complete: () => {
            this.isGoogleLinking = false;
          }
        });
      },
      error: (err) => {
        this.isGoogleLinking = false;
        const msg = err.error?.message || err.error?.[0]?.message || "Une erreur est survenue.";
        this.googleError = this.mapErrorMessage(msg);
      }
    });
  }

  private mapErrorMessage(message: string): string {
    if (!message) return "Une erreur est survenue.";
    const map: { [key: string]: string } = {
      'This Google account is already linked to another profile': 'Ce compte Google est déjà lié à un autre profil.',
      'The email address provided by Google is already used by another account.': 'L\'adresse e-mail fournie par Google est déjà utilisée par un autre compte.'
    };
    return map[message] || message;
  }
}
