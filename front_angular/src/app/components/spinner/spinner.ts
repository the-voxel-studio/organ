import { Component, Input, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-spinner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './spinner.html',
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      vertical-align: middle;
    }
    :host.block-container {
      display: flex;
      flex-direction: column;
      width: 100%;
      padding-top: 1.5rem; /* py-6 */
      padding-bottom: 1.5rem;
    }
    :host.fullscreen-container {
      display: flex;
      flex-direction: column;
      width: 100%;
      min-height: 300px;
      padding-top: 3rem; /* py-12 */
      padding-bottom: 3rem;
    }
  `]
})
export class SpinnerComponent {
  @Input() size: 'xs' | 'sm' | 'md' | 'lg' | 'xl' = 'md';
  @Input() color: string | null = null; // Hex color or SVG fill/stroke color (defaults to currentColor)
  @Input() text: string | null = null;
  @Input() fullscreen: boolean = false;
  @Input() inline: boolean = false;
  @Input() className: string = ''; // Additional custom classes for spinner SVG

  @HostBinding('class')
  get hostClass(): string {
    if (this.inline) {
      return '';
    }
    if (this.fullscreen) {
      return 'fullscreen-container';
    }
    return 'block-container';
  }

  get spinnerClass(): string {
    let sizeClass = '';
    switch (this.size) {
      case 'xs':
        sizeClass = 'w-3 h-3';
        break;
      case 'sm':
        sizeClass = 'w-5 h-5';
        break;
      case 'lg':
        sizeClass = 'w-12 h-12';
        break;
      case 'xl':
        sizeClass = 'w-16 h-16';
        break;
      case 'md':
      default:
        sizeClass = 'w-8 h-8';
        break;
    }
    return `${sizeClass} ${this.className}`;
  }
}
