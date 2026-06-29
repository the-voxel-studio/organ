import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { UserService } from '../../../../../services/api/user.service';
import { AuthService } from '../../../../../services/api/auth.service';

@Component({
  selector: 'app-settings-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './settings-password.html'
})
export class SettingsPasswordComponent {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  currentUser = this.authService.currentUser;

  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  showCurrentPassword = false;
  showNewPassword = false;
  isPasswordLoading = false;
  passwordSuccess: string | null = null;
  passwordError: string | null = null;

  onSubmitPassword() {
    if (this.passwordForm.invalid) return;
    this.passwordError = null;
    this.passwordSuccess = null;
    this.isPasswordLoading = true;

    this.userService.updatePassword({
      currentPassword: this.passwordForm.value.currentPassword || '',
      newPassword: this.passwordForm.value.newPassword || ''
    }).subscribe({
      next: () => {
        this.passwordSuccess = "Mot de passe mis à jour avec succès.";
        this.passwordForm.reset();
        this.isPasswordLoading = false;
      },
      error: (err) => {
        this.isPasswordLoading = false;
        const msg = err.error?.message || err.error?.[0]?.message || "Une erreur est survenue.";
        this.passwordError = this.mapErrorMessage(msg);
      }
    });
  }

  private mapErrorMessage(message: string): string {
    if (!message) return "Une erreur est survenue.";
    const map: { [key: string]: string } = {
      'Invalid current password': 'Le mot de passe actuel est incorrect.',
      'New password must be at least 8 characters': 'Le nouveau mot de passe doit faire au moins 8 caractères.',
      'Current and new passwords are required': 'Le mot de passe actuel et le nouveau mot de passe sont requis.',
      'Not authenticated': 'Session expirée. Veuillez vous reconnecter.'
    };
    return map[message] || message;
  }
}
