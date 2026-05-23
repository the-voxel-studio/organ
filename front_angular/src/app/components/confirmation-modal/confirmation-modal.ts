import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirmation-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirmation-modal.html'
})
export class ConfirmationModalComponent {
  @Input() isOpen: boolean = false;
  @Input() title: string = '';
  @Input() message: string = '';
  @Input() confirmLabel: string = 'Confirmer';
  @Input() cancelLabel: string = 'Annuler';
  @Input() type: 'danger' | 'warning' | 'info' | 'primary' = 'primary';
  @Input() isLoading: boolean = false;
  @Input() infoTitle?: string;
  @Input() infoText?: string;

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm() {
    this.confirm.emit();
  }

  onCancel() {
    this.cancel.emit();
  }
}
