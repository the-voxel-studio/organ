import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
  state?: 'entering' | 'active' | 'exiting';
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  toasts = signal<ToastMessage[]>([]);

  show(type: 'success' | 'error' | 'warning' | 'info', title: string, message: string, duration = 4000) {
    const id = 'toast-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
    const newToast: ToastMessage = { id, type, title, message, duration, state: 'entering' };
    this.toasts.update(list => [...list, newToast]);

    // Transition to active state
    setTimeout(() => {
      this.toasts.update(list => 
        list.map(t => t.id === id ? { ...t, state: 'active' } : t)
      );
    }, 50);

    setTimeout(() => {
      this.clear(id);
    }, duration);
  }

  success(title: string, message: string, duration = 4000) {
    this.show('success', title, message, duration);
  }

  error(title: string, message: string, duration = 4000) {
    this.show('error', title, message, duration);
  }

  warning(title: string, message: string, duration = 4000) {
    this.show('warning', title, message, duration);
  }

  info(title: string, message: string, duration = 4000) {
    this.show('info', title, message, duration);
  }

  clear(id: string) {
    const list = this.toasts();
    const toast = list.find(t => t.id === id);
    if (toast && toast.state !== 'exiting') {
      this.toasts.update(currentList =>
        currentList.map(t => t.id === id ? { ...t, state: 'exiting' } : t)
      );
      setTimeout(() => {
        this.toasts.update(currentList => currentList.filter(t => t.id !== id));
      }, 300);
    }
  }
}
