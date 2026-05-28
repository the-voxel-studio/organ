import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-spinner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './spinner.html'
})
export class SpinnerComponent {
  @Input() color: string | null = null;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() text: string | null = null;
  @Input() fullscreen: boolean = false;

  get spinnerClass(): string {
    switch (this.size) {
      case 'sm':
        return 'w-6 h-6 border-2';
      case 'lg':
        return 'w-12 h-12 border-4';
      case 'md':
      default:
        return 'w-8 h-8 border-4';
    }
  }

  get containerClass(): string {
    if (this.fullscreen) {
      return 'flex flex-col items-center justify-center min-h-[300px] w-full py-12';
    }
    return 'flex flex-col items-center justify-center py-6 w-full';
  }
}
