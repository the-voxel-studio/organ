import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastMessage, ToastService } from '../../services/common/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.html',
  styleUrl: './toast.css'
})
export class ToastComponent {
  private toastService = inject(ToastService);

  @Input({ required: true }) toast!: ToastMessage;

  close() {
    this.toastService.clear(this.toast.id);
  }
}
