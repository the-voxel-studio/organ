import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../../../../services/auth.service';
import { UserService } from '../../../../../services/user.service';

@Component({
  selector: 'app-settings-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './settings-profile.html'
})
export class SettingsProfileComponent implements OnInit {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private fb = inject(FormBuilder);

  currentUser = this.authService.currentUser;

  profileForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]]
  });

  isProfileLoading = false;
  profileSuccess: string | null = null;
  profileError: string | null = null;

  constructor() {
    effect(() => {
      const user = this.currentUser();
      if (user) {
        this.profileForm.patchValue({
          firstName: user.firstName || '',
          lastName: user.lastName || '',
          email: user.email || ''
        });
      }
    });
  }

  ngOnInit() {}

  onSubmitProfile() {
    if (this.profileForm.invalid) return;
    this.profileError = null;
    this.profileSuccess = null;
    this.isProfileLoading = true;

    this.userService.updateProfile({
      firstName: this.profileForm.value.firstName || '',
      lastName: this.profileForm.value.lastName || '',
      email: this.profileForm.value.email || ''
    }).subscribe({
      next: (res) => {
        this.profileSuccess = "Profil mis à jour avec succès.";
        this.authService.checkSession().subscribe({
          complete: () => {
            this.isProfileLoading = false;
          }
        });
      },
      error: (err) => {
        this.isProfileLoading = false;
        const msg = err.error?.message || err.error?.[0]?.message || "Une erreur est survenue.";
        this.profileError = this.mapErrorMessage(msg);
      }
    });
  }

  private mapErrorMessage(message: string): string {
    if (!message) return "Une erreur est survenue.";
    const map: { [key: string]: string } = {
      'Not authenticated': 'Session expirée. Veuillez vous reconnecter.',
      'The email address provided by Google is already used by another account.': 'L\'adresse e-mail fournie par Google est déjà utilisée par un autre compte.'
    };
    return map[message] || message;
  }
}
