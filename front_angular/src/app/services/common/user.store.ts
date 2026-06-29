import { Injectable, computed, signal } from '@angular/core';
import { UserMeResponse } from '../../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserStore {
  // État privé encapsulé sous forme de signal
  private state = signal<UserMeResponse | null>(null);

  // Sélections publiques en lecture seule
  readonly currentUser = computed(() => this.state());
  readonly isAuthenticated = computed(() => !!this.state());

  /**
   * Initialise ou remplace l'utilisateur dans le store.
   */
  setUser(user: UserMeResponse | null): void {
    this.state.set(user);
  }

  /**
   * Met à jour localement les informations de profil de l'utilisateur.
   */
  updateProfile(firstName: string, lastName: string, email: string): void {
    this.state.update((current) => {
      if (!current) return null;
      return {
        ...current,
        firstName,
        lastName,
        email
      };
    });
  }

  /**
   * Met à jour localement l'état de liaison Google de l'utilisateur.
   */
  linkGoogle(email: string, authWithGoogle: boolean): void {
    this.state.update((current) => {
      if (!current) return null;
      return {
        ...current,
        email,
        authWithGoogle
      };
    });
  }

  /**
   * Réinitialise l'état utilisateur (ex: lors de la déconnexion).
   */
  clear(): void {
    this.state.set(null);
  }
}
