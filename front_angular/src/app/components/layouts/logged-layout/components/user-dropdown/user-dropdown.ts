import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../../services/api/auth.service';

@Component({
  selector: 'app-user-dropdown',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './user-dropdown.html'
})
export class UserDropdownComponent {
  protected authService = inject(AuthService);
  private router = inject(Router);

  isProfileOpen = signal(false);

  currentUserInitials = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return 'U';
    const first = user.firstName ? user.firstName.charAt(0).toUpperCase() : '';
    const last = user.lastName ? user.lastName.charAt(0).toUpperCase() : '';
    return first + last || 'U';
  });

  toggleProfile(event: MouseEvent) {
    event.stopPropagation();
    this.isProfileOpen.update(v => !v);
  }

  onLogout() {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Logout failed', err);
        this.router.navigate(['/login']);
      }
    });
  }
}
