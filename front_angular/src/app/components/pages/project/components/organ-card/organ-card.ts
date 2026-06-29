import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-organ-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './organ-card.html'
})
export class OrganCardComponent {
  @Input({ required: true }) organ!: any;
  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) projectColor!: string;

  private sanitizer = inject(DomSanitizer);

  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }
}
