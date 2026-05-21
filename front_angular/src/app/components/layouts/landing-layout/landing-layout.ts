import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { LandingNavbarComponent } from '../../landing/landing-navbar/landing-navbar';
import { LandingFooterComponent } from '../../landing/landing-footer/landing-footer';

@Component({
  selector: 'app-landing-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, LandingNavbarComponent, LandingFooterComponent],
  templateUrl: './landing-layout.html',
  styleUrl: './landing-layout.css'
})
export class LandingLayoutComponent {}
