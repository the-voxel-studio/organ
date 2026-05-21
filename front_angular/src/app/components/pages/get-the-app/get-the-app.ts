import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-get-the-app',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './get-the-app.html'
})
export class GetTheAppComponent {}
