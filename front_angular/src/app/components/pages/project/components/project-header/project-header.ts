import { Component, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-project-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './project-header.html'
})
export class ProjectHeaderComponent {
  @Input({ required: true }) project!: any;
  @Input({ required: true }) admin!: any;
  @Input({ required: true }) canManage!: boolean;

  private sanitizer = inject(DomSanitizer);

  showNotImplementedAlert = signal(false);
  notImplementedFeatureName = '';

  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }

  openNotImplementedAlert(featureName: string) {
    this.notImplementedFeatureName = featureName;
    this.showNotImplementedAlert.set(true);
  }

  closeNotImplementedAlert() {
    this.showNotImplementedAlert.set(false);
  }
}
