import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DynamicLogoComponent } from '../../dynamic-logo/dynamic-logo';

@Component({
  selector: 'app-landing-footer',
  standalone: true,
  imports: [CommonModule, RouterLink, DynamicLogoComponent],
  templateUrl: './landing-footer.html',
  styleUrl: './landing-footer.css'
})
export class LandingFooterComponent {
  protected readonly year = new Date().getFullYear();
}

