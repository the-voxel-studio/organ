import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { DynamicLogoComponent } from '../../dynamic-logo/dynamic-logo';

@Component({
  selector: 'app-navbar-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, DynamicLogoComponent],
  templateUrl: './navbar-layout.html',
  styleUrl: './navbar-layout.css'
})
export class NavbarLayoutComponent {}
