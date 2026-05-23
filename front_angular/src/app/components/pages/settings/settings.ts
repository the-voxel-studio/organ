import { Component, OnInit, OnDestroy, ViewChild, ElementRef, inject, NgZone, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { ConfirmationModalComponent } from '../../confirmation-modal/confirmation-modal';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { UserConnection } from '../../../models/user.model';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    GoogleSigninButtonModule,
    ConfirmationModalComponent
  ],
  templateUrl: './settings.html'
})
export class SettingsComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private socialAuthService = inject(SocialAuthService);
  private router = inject(Router);
  private ngZone = inject(NgZone);
  private destroy$ = new Subject<void>();

  @ViewChild('googleBtnContainer', { static: false }) googleBtnContainer!: ElementRef;

  // Bound user me response from auth service signal
  currentUser = this.authService.currentUser;

  // Profile Form State
  firstName = '';
  lastName = '';
  email = '';
  isProfileLoading = false;
  profileSuccess: string | null = null;
  profileError: string | null = null;

  // Password Form State
  currentPassword = '';
  newPassword = '';
  showCurrentPassword = false;
  showNewPassword = false;
  isPasswordLoading = false;
  passwordSuccess: string | null = null;
  passwordError: string | null = null;

  // Google account linking state
  isGoogleLinking = false;
  googleSuccess: string | null = null;
  googleError: string | null = null;

  // Connections List State
  connections = signal<UserConnection[]>([]);
  isConnectionsLoading = signal(false);

  // Invalidate Session Modal State
  isDisconnectModalOpen = false;
  isDisconnectLoading = false;
  connectionToInvalidate: UserConnection | null = null;

  // Delete Account Modal State
  isDeleteModalOpen = false;
  isDeleteLoading = false;

  ngOnInit() {
    // Populate form fields from current user
    const user = this.currentUser();
    if (user) {
      this.firstName = user.firstName || '';
      this.lastName = user.lastName || '';
      this.email = user.email || '';
    }

    // Load active connections
    this.loadConnections();

    // Subscribe to Google auth state changes for linking
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

  loadConnections() {
    this.isConnectionsLoading.set(true);
    this.userService.getConnections().subscribe({
      next: (conns) => {
        this.connections.set(conns);
        this.isConnectionsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load connections:', err);
        this.isConnectionsLoading.set(false);
      }
    });
  }

  onSubmitProfile(event: Event) {
    event.preventDefault();
    this.profileError = null;
    this.profileSuccess = null;
    this.isProfileLoading = true;

    this.userService.updateProfile({
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email
    }).subscribe({
      next: (res) => {
        this.profileSuccess = "Profil mis à jour avec succès.";
        // Refresh Auth session state
        this.authService.checkSession().subscribe({
          complete: () => {
            this.isProfileLoading = false;
          }
        });
      },
      error: (err) => {
        this.isProfileLoading = false;
        const msg = err.error?.message || err.error?.[0]?.message;
        this.profileError = this.mapErrorMessage(msg);
      }
    });
  }

  onSubmitPassword(event: Event) {
    event.preventDefault();
    this.passwordError = null;
    this.passwordSuccess = null;
    this.isPasswordLoading = true;

    this.userService.updatePassword({
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.passwordSuccess = "Mot de passe mis à jour avec succès.";
        this.currentPassword = '';
        this.newPassword = '';
        this.isPasswordLoading = false;
      },
      error: (err) => {
        this.isPasswordLoading = false;
        const msg = err.error?.message || err.error?.[0]?.message;
        this.passwordError = this.mapErrorMessage(msg);
      }
    });
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
        // Refresh Auth session state
        this.authService.checkSession().subscribe({
          complete: () => {
            this.isGoogleLinking = false;
          }
        });
      },
      error: (err) => {
        this.isGoogleLinking = false;
        const msg = err.error?.message || err.error?.[0]?.message;
        this.googleError = this.mapErrorMessage(msg);
      }
    });
  }

  onRevokeConnection(conn: UserConnection) {
    this.connectionToInvalidate = conn;
    this.isDisconnectModalOpen = true;
  }

  confirmRevokeConnection() {
    if (!this.connectionToInvalidate) return;
    this.isDisconnectLoading = true;

    this.userService.revokeConnection(this.connectionToInvalidate.uuid).subscribe({
      next: () => {
        this.isDisconnectLoading = false;
        this.isDisconnectModalOpen = false;
        
        // If it was the current session, reload to trigger auth check/redirect
        if (this.connectionToInvalidate?.isCurrent) {
          window.location.reload();
        } else {
          this.loadConnections();
        }
        this.connectionToInvalidate = null;
      },
      error: (err) => {
        this.isDisconnectLoading = false;
        this.isDisconnectModalOpen = false;
        this.connectionToInvalidate = null;
        alert("Une erreur est survenue lors de la déconnexion de l'appareil.");
      }
    });
  }

  onOpenDeleteModal() {
    this.isDeleteModalOpen = true;
  }

  confirmDeleteAccount() {
    this.isDeleteLoading = true;
    this.userService.deleteAccount().subscribe({
      next: () => {
        this.isDeleteLoading = false;
        this.isDeleteModalOpen = false;
        this.authService.clearSessionState();
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isDeleteLoading = false;
        this.isDeleteModalOpen = false;
        alert(err.error?.message || "Une erreur est survenue lors de la suppression de votre compte.");
      }
    });
  }

  formatRelativeDate(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);
    
    if (diffInSeconds < 60) return "À l'instant";
    if (diffInSeconds < 3600) return `Il y a ${Math.floor(diffInSeconds / 60)} min`;
    if (diffInSeconds < 86400) return `Il y a ${Math.floor(diffInSeconds / 3600)} h`;
    if (diffInSeconds < 604800) return `Il y a ${Math.floor(diffInSeconds / 86400)} j`;
    
    return date.toLocaleDateString();
  }

  private mapErrorMessage(message: string): string {
    if (!message) return "Une erreur est survenue.";
    
    const map: { [key: string]: string } = {
      'Invalid current password': 'Le mot de passe actuel est incorrect.',
      'New password must be at least 8 characters': 'Le nouveau mot de passe doit faire au moins 8 caractères.',
      'Current and new passwords are required': 'Le mot de passe actuel et le nouveau mot de passe sont requis.',
      'Not authenticated': 'Session expirée. Veuillez vous reconnecter.',
      'This Google account is already linked to another profile': 'Ce compte Google est déjà lié à un autre profil.',
      'The email address provided by Google is already used by another account.': 'L\'adresse e-mail fournie par Google est déjà utilisée par un autre compte.'
    };

    return map[message] || message;
  }
}
