import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from './components/sidebar/sidebar';

@Component({
  selector: 'app-logged-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent],
  templateUrl: './logged-layout.html',
  styleUrl: './logged-layout.css'
})
export class LoggedLayoutComponent {}

