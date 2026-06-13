import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-shimmer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shimmer.html'
})
export class ShimmerComponent {
  @Input() width: string = '100%';
  @Input() height: string = '20px';
  @Input() className: string = 'rounded-md';
}
