import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  toasts = signal<ToastMessage[]>([]);

  show(type: 'success' | 'error' | 'warning' | 'info', title: string, message: string, duration = 4000) {
    const id = 'toast-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
    const newToast: ToastMessage = { id, type, title, message, duration };
    this.toasts.update(list => [...list, newToast]);

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
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}
